package quiz;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import quiz.ui.*;

public final class Client {
 final JFrame frame=new JFrame("Quiz Arena — Đối Kháng 1v1");
 final AuthScreen authScreen=new AuthScreen();
 final LobbyScreen lobbyScreen=new LobbyScreen(Model.LEVELS);
    WaitingChallengeDialog waitingDialog;
    InviteDialog currentInviteDialog;
    ResultDialog currentResultDialog;
    MatchScreen currentMatchScreen;
    // --- SESSION FIELDS (reset on logout) ---
    volatile DataOutputStream out;
    volatile String name;
    volatile String lastLoginUser;
    volatile String lastLoginPass;
    volatile boolean reconnecting = false;
    volatile String myAvatarId="avatar_01";
    String matchId;
    String currentMatchLeft;
    String currentOpponent;
    final Map<String, String> userAvatars = new java.util.concurrent.ConcurrentHashMap<>();
    final java.util.concurrent.BlockingQueue<String[]> sendQueue = new java.util.concurrent.LinkedBlockingQueue<>();

    public String getAvatarFor(String username) {
        if (username == null || username.isBlank()) return "avatar_01";
        if (username.equalsIgnoreCase(name)) return myAvatarId != null ? myAvatarId : "avatar_01";
        if (username.equalsIgnoreCase("Bot Luyện Tập")) return "avatar_02";
        return userAvatars.getOrDefault(username, "avatar_01");
    }
    volatile boolean userLeftMatch = false;
    final Set<String> abandonedMatchIds = Collections.synchronizedSet(new HashSet<>());
    java.util.List<MatchQuestion> currentQuestions;
    int[] currentSubmittedAnswers;
    // --- CONNECTION FIELDS ---
    final String host;
    final int port;
    volatile Thread readerThread;
    final javax.swing.Timer pingTimer;

 Client(String host,int port)throws Exception{
  this.host=host;this.port=port;
  startAsyncWriterThread();
  frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
  frame.addWindowListener(new java.awt.event.WindowAdapter() {
   @Override public void windowClosing(java.awt.event.WindowEvent e) {
    MusicManager.shutdown();
    SoundManager.shutdown();
    System.exit(0);
   }
  });
  frame.setLayout(new BorderLayout());
  frame.setSize(1080, 760);
  frame.setMinimumSize(new Dimension(900, 640));
  frame.setLocationRelativeTo(null);
  frame.getContentPane().setBackground(Theme.OFF_WHITE);

  // Cấu hình AuthScreen
  authScreen.setOnLogin((u, p) -> {
   lastLoginUser = u;
   lastLoginPass = p;
   send("LOGIN", u, p);
  });
  authScreen.setOnRegister((u, p) -> send("REGISTER", u, p));
  frame.add(authScreen, BorderLayout.CENTER);

  // Cấu hình LobbyScreen
  lobbyScreen.setOnChallenge((target, topic, level) -> {
   send("CHALLENGE", target, topic, level);
   if(waitingDialog!=null) waitingDialog.closeDialog();
   waitingDialog=new WaitingChallengeDialog(frame, target, topic, level, () -> send("CANCEL_CHALLENGE", target));
   waitingDialog.setVisible(true);
  });
  lobbyScreen.setOnTabChanged(tabName -> {
   if (tabName.equals("Bảng Xếp Hạng")) send("LEADERBOARD");
   else if (tabName.equals("Lịch Sử")) send("HISTORY");
   else if (tabName.equals("Quản Lý Người Chơi")) send("USER_LIST");
   else if (tabName.equals("Ngân Hàng Câu Hỏi")) send("QUESTION_LIST");
  });
  lobbyScreen.getPlayerManagementPanel().setUserActionListener(new PlayerManagementPanel.UserActionListener() {
   @Override
   public void onRefresh() {
    send("USER_LIST");
   }
   @Override
   public void onAddUser(String username, String password, int score, String avatarId, String fullName, String email, String bio) {
    send("ADD_USER", username, password, String.valueOf(score), avatarId, fullName, email, bio);
   }
   @Override
   public void onUpdateUser(String username, String newPassword, int score, String avatarId, String fullName, String email, String bio) {
    send("UPDATE_USER", username, newPassword, String.valueOf(score), avatarId, fullName, email, bio);
   }
   @Override
   public void onDeleteUser(String targetUsername) {
    send("DELETE_USER", targetUsername);
   }
  });
  lobbyScreen.getQuestionManagementPanel().setQuestionActionListener(new QuestionManagementPanel.QuestionActionListener() {
   @Override
   public void onRefresh() {
    send("QUESTION_LIST");
   }
   @Override
   public void onAddQuestion(String topic, String level, String type, String text, String choicesRaw, String answerRaw, String explanation, String blanksRaw, String fillMapRaw) {
    send("ADD_QUESTION", topic, level, type, text, choicesRaw, answerRaw, explanation, blanksRaw, fillMapRaw);
   }
   @Override
   public void onUpdateQuestion(String id, String topic, String level, String type, String text, String choicesRaw, String answerRaw, String explanation, String blanksRaw, String fillMapRaw) {
    send("UPDATE_QUESTION", id, topic, level, type, text, choicesRaw, answerRaw, explanation, blanksRaw, fillMapRaw);
   }
   @Override
   public void onDeleteQuestion(String id) {
    send("DELETE_QUESTION", id);
   }
  });
  lobbyScreen.setOnLogout(this::doLogout);
  lobbyScreen.setOnSoloPractice(() -> {
   String topic = lobbyScreen.getSelectedTopic();
   String level = lobbyScreen.getSelectedLevel();
   startSoloPractice(topic, level);
  });
  lobbyScreen.setOnQuickMatch(() -> send("QUICK_MATCH"));
  lobbyScreen.setOnCancelMatchmaking(() -> send("CANCEL_MATCHMAKING"));
  lobbyScreen.setOnUpdateAvatar(avatarId -> send("UPDATE_AVATAR", avatarId));

  pingTimer = new javax.swing.Timer(3000, e -> {
   if (out != null && name != null && !reconnecting) {
    send("PING", String.valueOf(System.nanoTime()));
   }
  });
  pingTimer.start();

  frame.setVisible(true);
  frame.setExtendedState(JFrame.NORMAL);
  frame.setAlwaysOnTop(true);
  frame.toFront();
  frame.requestFocus();
  SwingUtilities.invokeLater(() -> frame.setAlwaysOnTop(false));
  connectAndStartReader();
 }

 /** Luồng gửi gói tin bất đồng bộ (P2 - Item 10) tránh chặn Swing EDT */
 void startAsyncWriterThread() {
  Thread writer = new Thread(() -> {
   while (true) {
    try {
     String[] pkt = sendQueue.take();
     DataOutputStream currentOut = out;
     if (currentOut != null) {
      Wire.send(currentOut, pkt);
     }
    } catch (InterruptedException ie) {
     Thread.currentThread().interrupt();
     break;
    } catch (IOException ioe) {
     if (!"PING".equals( ioe.getMessage() )) {
      System.err.println("[Client] Lỗi gửi gói tin: " + ioe.getMessage());
     }
    }
   }
  }, "Client-Async-Writer");
  writer.setDaemon(true);
  writer.start();
 }

 /** Tạo socket mới và khởi động reader thread. Gọi khi khởi động, sau logout, hoặc khi reconnect. */
 void connectAndStartReader() throws Exception {
  Socket socket = new Socket(host, port);
  sendQueue.clear();
  out = new DataOutputStream(socket.getOutputStream());
  DataInputStream in = new DataInputStream(socket.getInputStream());
  Thread old = readerThread;
  if (old != null && old != Thread.currentThread()) old.interrupt();
  Thread reader = new Thread(() -> {
   try {
    while (true) {
     String[] a = Wire.read(in);
     SwingUtilities.invokeLater(() -> handle(a));
    }
   } catch (Exception e) {
    SwingUtilities.invokeLater(() -> {
     if (name != null) {
      attemptAutoReconnect();
     } else {
      authScreen.showError("Mất kết nối server");
     }
    });
   }
  }, "Client-Reader");
  reader.setDaemon(true);
  reader.start();
  readerThread = reader;
 }

 /** Tự động kết nối lại trong 25 giây khi rớt mạng (P2 - Item 9) */
 void attemptAutoReconnect() {
  if (reconnecting || name == null) return;
  reconnecting = true;
  out = null;
  ToastManager.warning(frame, "Mất kết nối mạng! Đang tự động kết nối lại (25s)...");
  Thread reconThread = new Thread(() -> {
   long deadlineMs = System.currentTimeMillis() + 25_000L;
   int attempt = 0;
   while (System.currentTimeMillis() < deadlineMs && name != null) {
    attempt++;
    try {
     Thread.sleep(2000L);
     if (name == null) return;
     connectAndStartReader();
     if (lastLoginUser != null && lastLoginPass != null) {
      send("LOGIN", lastLoginUser, lastLoginPass, "RECONNECT");
     }
     return;
    } catch (Exception ignored) {
     final int att = attempt;
     SwingUtilities.invokeLater(() -> {
      if (name != null) {
       ToastManager.info(frame, "Đang thử kết nối lại lần " + att + "...");
      }
     });
    }
   }
   SwingUtilities.invokeLater(() -> {
    reconnecting = false;
    if (name != null) {
     if (frame.getGlassPane() instanceof DisconnectOverlay) return;
     frame.setGlassPane(new DisconnectOverlay(() -> System.exit(0), this::doLogout));
     frame.getGlassPane().setVisible(true);
    }
   });
  }, "Client-Reconnect");
  reconThread.setDaemon(true);
  reconThread.start();
 }

 /** Logout: đóng socket hiện tại, reset session, tạo socket mới, quay về AuthScreen */
 void doLogout() {
  MusicManager.stopMusic();
  SoundManager.stopCountdown();
  name = null;
  lastLoginUser = null;
  lastLoginPass = null;
  reconnecting = false;
  myAvatarId = "avatar_01";
  matchId = null;
  currentQuestions = null;
  currentSubmittedAnswers = null;
  sendQueue.clear();
  DataOutputStream oldOut = out;
  out = null;
  if (oldOut != null) {
   try { oldOut.close(); } catch (Exception ignored) {}
  }
  if (currentMatchScreen != null) { currentMatchScreen.stopTimer(); currentMatchScreen = null; }
  if (currentResultDialog != null) { currentResultDialog.dispose(); currentResultDialog = null; }
  abandonedMatchIds.clear();
  if (waitingDialog != null) { waitingDialog.closeDialog(); waitingDialog = null; }
  if (currentInviteDialog != null) { currentInviteDialog.dispose(); currentInviteDialog = null; }
  frame.setGlassPane(new JPanel());
  frame.getGlassPane().setVisible(false);
  frame.getContentPane().removeAll();
  frame.getContentPane().setLayout(new BorderLayout());
  frame.getContentPane().add(authScreen, BorderLayout.CENTER);
  authScreen.resetForReuse();
  refresh();
  SwingUtilities.invokeLater(() -> {
   try {
    connectAndStartReader();
   } catch (Exception ex) {
    authScreen.showError("Không thể kết nối lại server: " + ex.getMessage());
   }
  });
 }

 void send(String... a){
  if (a == null || a.length == 0) return;
  sendQueue.offer(a);
 }

 void log(String s){System.out.println("[Client] "+s);}

 void button(JPanel p,String label,Runnable action){
  ModernButton b=new ModernButton(label, ModernButton.Style.PRIMARY);
  b.addActionListener(e->action.run());
  p.add(b);
 }

 void leaveCurrentMatch() {
  userLeftMatch = true;
  String leavingMatchId = matchId;
  if (leavingMatchId != null) {
   abandonedMatchIds.add(leavingMatchId);
  }
  matchId = null;
  currentMatchLeft = null;
  currentOpponent = null;
  currentQuestions = null;
  currentSubmittedAnswers = null;
  SoundManager.stopCountdown();
  MusicManager.stopMusic();
  if (currentMatchScreen != null) {
   currentMatchScreen.cleanupAndStop();
   currentMatchScreen = null;
  }
  if (currentResultDialog != null) {
   currentResultDialog.dispose();
   currentResultDialog = null;
  }
  send("LEAVE", leavingMatchId != null ? leavingMatchId : "");
  showLobby();
  ToastManager.info(frame, "Bạn đã rời khỏi trận đấu.");
 }

 void showLobby(){
  if(currentMatchScreen!=null){currentMatchScreen.stopTimer();currentMatchScreen=null;}
  if(currentResultDialog!=null){currentResultDialog.dispose();currentResultDialog=null;}
  frame.getContentPane().removeAll();
  frame.getContentPane().setLayout(new BorderLayout());
  frame.getContentPane().add(lobbyScreen, BorderLayout.CENTER);
  refresh();
  MusicManager.playLobbyMusic();
 }

 void refresh(){frame.getContentPane().revalidate();frame.getContentPane().repaint();}

 void handle(String[] a){
  try{
  switch(a[0]){
   case "PONG"->{
    try{
     long sentNano=Long.parseLong(Wire.field(a,1));
     long rttMs=Math.max(1L,java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-sentNano));
     lobbyScreen.updatePing(rttMs);
     if(currentMatchScreen!=null)currentMatchScreen.updatePing(rttMs);
    }catch(Exception ignored){}
   }
   case "WELCOME"->{
    boolean wasReconnecting=reconnecting;
    reconnecting=false;
    if(frame.getGlassPane() instanceof DisconnectOverlay){
     frame.setGlassPane(new JPanel());
     frame.getGlassPane().setVisible(false);
    }
    name=Wire.field(a,1);
    String rawTopics=Wire.field(a,2);
    myAvatarId=Wire.field(a,3);
    if(myAvatarId==null||myAvatarId.isBlank())myAvatarId="avatar_01";
    userAvatars.put(name,myAvatarId);
    String scoreStr=Wire.field(a,4);
    int score=0;try{score=Integer.parseInt(scoreStr);}catch(Exception ignored){}
    String fullName=Wire.field(a,5);
    String email=Wire.field(a,6);
    String bio=Wire.field(a,7);
    String role=Wire.field(a,8);
    if(role==null||role.isBlank())role="admin".equalsIgnoreCase(name)?"ADMIN":"PLAYER";
    lobbyScreen.setCurrentUser(name,score,myAvatarId,fullName,email,bio,role);
    if(!rawTopics.isBlank()){lobbyScreen.setTopics(Arrays.asList(rawTopics.split("\\|")));}
    if(wasReconnecting){
     ToastManager.success(frame, "Đã kết nối lại với máy chủ!");
     if(matchId==null)showLobby();
    }else{
     ToastManager.success(frame, "Đăng nhập thành công! Chào mừng "+name);
     showLobby();
    }
    send("USER_LIST");
    send("PING",String.valueOf(System.nanoTime()));
   }
   case "TOPICS"->{
    String rawTopics=Wire.field(a,1);
    if(!rawTopics.isBlank()){lobbyScreen.setTopics(Arrays.asList(rawTopics.split("\\|")));}
   }
   case "INFO"->{
    String msg=Wire.field(a,1);
    if(name==null){authScreen.showSuccess(msg);if(msg.contains("thành công")){authScreen.setMode(AuthScreen.Mode.LOGIN);authScreen.clearPassword();}}
    ToastManager.info(frame, msg);
   }
   case "ERROR"->{
    if(waitingDialog!=null){waitingDialog.closeDialog();waitingDialog=null;}
    String msg=Wire.field(a,1);
    if(name==null){authScreen.showError(msg);}
    ToastManager.error(frame, msg);
   }
   case "LOBBY"->{
     String lobbyData=Wire.field(a,1);
     if(lobbyData!=null&&!lobbyData.isBlank()){
      for(String row:lobbyData.split("\\|")){
       if(row.isBlank())continue;
       String[] f=row.split(";");
       if(f.length>=4&&!f[0].isBlank()){
        userAvatars.put(f[0],f[3]);
       }
      }
     }
     lobbyScreen.updatePlayersFromLobbyData(lobbyData,name);
    }
   case "LEADERBOARD"->{lobbyScreen.getLeaderboardPanel().updateData(Wire.field(a,1), name);}
   case "HISTORY"->{lobbyScreen.getHistoryPanel().updateData(Wire.field(a,1), name);}
   case "USER_LIST"->{lobbyScreen.getPlayerManagementPanel().updateData(Wire.field(a,1), name);}
   case "USER_SAVED"->{ToastManager.success(frame, Wire.field(a,1));}
   case "QUESTION_LIST"->{lobbyScreen.getQuestionManagementPanel().updateFromWire(a);}
   case "QUESTION_SAVED"->{ToastManager.success(frame, Wire.field(a,1));}
   case "PROFILE_SYNC"->{
    String newName=Wire.field(a,1);
    int newScore=0;try{newScore=Integer.parseInt(Wire.field(a,2));}catch(Exception ignored){}
    String newAvatar=Wire.field(a,3);
    String uFull=Wire.field(a,4);
    String uEmail=Wire.field(a,5);
    String uBio=Wire.field(a,6);
    String role=Wire.field(a,7);
    if(newName!=null&&!newName.isBlank())name=newName;
    if(newAvatar!=null&&!newAvatar.isBlank()){myAvatarId=newAvatar;userAvatars.put(name,myAvatarId);}
    if(role==null||role.isBlank())role="admin".equalsIgnoreCase(name)?"ADMIN":"PLAYER";
    lobbyScreen.setCurrentUser(name,newScore,myAvatarId,uFull,uEmail,uBio,role);
   }
   case "ACCOUNT_DELETED"->{
    String msg=Wire.field(a,1);
    ToastManager.warning(frame, msg.isBlank()?"Tài khoản đã bị xóa.":msg);
    doLogout();
   }
   case "DECLINED"->{
    if(waitingDialog!=null){waitingDialog.closeDialog();waitingDialog=null;}
    if(currentInviteDialog!=null){currentInviteDialog.dispose();currentInviteDialog=null;}
    ToastManager.warning(frame, Wire.field(a,1));
   }
   case "INVITE"->{
    SoundManager.playInviteAlert();
    String from=Wire.field(a,1),topic=Wire.field(a,2),level=Wire.field(a,3);
    String duration=Wire.field(a,5);
    if(currentInviteDialog!=null){currentInviteDialog.dispose();}
    currentInviteDialog=new InviteDialog(frame, from, topic, level, duration, accept->{
     send(accept?"ACCEPT":"REJECT",from);
    });
    currentInviteDialog.setVisible(true);
   }
   case "EMOTE"->{
    String from=Wire.field(a,1);String emote=Wire.field(a,2);
    if(currentMatchScreen!=null){currentMatchScreen.showOpponentEmote(from, emote);}
   }
   case "MATCH_PHASE"->{
    if(userLeftMatch || currentMatchScreen==null) return;
    currentMatchScreen.handleMatchPhase(a);
   }
   case "READY_STATUS"->{
    if(userLeftMatch || currentMatchScreen==null) return;
    String p1=Wire.field(a,1);
    boolean r1=Boolean.parseBoolean(Wire.field(a,2));
    String p2=Wire.field(a,3);
    boolean r2=Boolean.parseBoolean(Wire.field(a,4));
    currentMatchScreen.updateReadyStatus(p1, r1, p2, r2);
   }
   case "MATCHMAKING"->{
    lobbyScreen.onMatchmakingStarted(Wire.field(a,1));
   }
   case "MATCH_FOUND"->{
    String opponent=Wire.field(a,1);
    int myScore=0,oppScore=0;
    try{myScore=Integer.parseInt(Wire.field(a,2));}catch(Exception ignored){}
    try{oppScore=Integer.parseInt(Wire.field(a,3));}catch(Exception ignored){}
    lobbyScreen.onMatchFound(opponent,myScore,oppScore);
    SoundManager.playMatchFound();
   }
   case "MATCHMAKING_CANCELLED"->{
    lobbyScreen.onMatchmakingCancelled(Wire.field(a,1));
    ToastManager.info(frame, Wire.field(a,1));
   }
   case "AVATAR_UPDATED"->{
    myAvatarId=Wire.field(a,1);if(name!=null)userAvatars.put(name,myAvatarId);lobbyScreen.setMyAvatar(myAvatarId);
    ToastManager.success(frame, "Đã cập nhật avatar!");
   }
   case "MATCH"->{
    userLeftMatch = false;
    if(waitingDialog!=null){waitingDialog.closeDialog();waitingDialog=null;}
    if(currentInviteDialog!=null){currentInviteDialog.dispose();currentInviteDialog=null;}
    if(currentResultDialog!=null){currentResultDialog.dispose();currentResultDialog=null;}
    lobbyScreen.onMatchmakingCancelled(""); // reset quick match UI
    MusicManager.playBattleMusic();
    match(a);
   }
   case "MATCH_RESUME"->{
    if(userLeftMatch || currentMatchScreen==null) return;
    String resumeId=Wire.field(a,1);
    if(resumeId.equals(matchId)){
     int curQ=0;try{curQ=Integer.parseInt(Wire.field(a,2));}catch(Exception ignored){}
     int remSec=60;try{remSec=Integer.parseInt(Wire.field(a,3));}catch(Exception ignored){}
     boolean isSub=Boolean.parseBoolean(Wire.field(a,4));
     int[] savedAns=new int[10];
     Arrays.fill(savedAns,-1);
     for(int k=0;k<10;k++){
      try{savedAns[k]=Integer.parseInt(Wire.field(a,5+k));}catch(Exception ignored){}
     }
     currentSubmittedAnswers=savedAns.clone();
     currentMatchScreen.restoreState(curQ,savedAns,isSub);
     ToastManager.success(frame,"Đã khôi phục trạng thái trận đấu (Câu "+(curQ+1)+"/10)!");
    }
   }
   case "SCORE"->{
    if(userLeftMatch || currentMatchScreen==null) return;
    int s0=0,s1=0,pts0=0,pts1=0,strk0=0,strk1=0;
    try{s0=Integer.parseInt(Wire.field(a,1));}catch(Exception ignored){}
    try{s1=Integer.parseInt(Wire.field(a,2));}catch(Exception ignored){}
    try{pts0=Integer.parseInt(Wire.field(a,3));}catch(Exception ignored){}
    try{pts1=Integer.parseInt(Wire.field(a,4));}catch(Exception ignored){}
    try{strk0=Integer.parseInt(Wire.field(a,5));}catch(Exception ignored){}
    try{strk1=Integer.parseInt(Wire.field(a,6));}catch(Exception ignored){}
    if(currentMatchScreen!=null){
     boolean isLeft=name!=null&&name.equals(currentMatchLeft);
     int myScore=isLeft?s0:s1;
     int oppScore=isLeft?s1:s0;
     int myPts=isLeft?pts0:pts1;
     int oppPts=isLeft?pts1:pts0;
     int myStrk=isLeft?strk0:strk1;
     int oppStrk=isLeft?strk1:strk0;
     currentMatchScreen.updateScores(myScore,oppScore,myPts,oppPts,myStrk,oppStrk);
    }
   }
   case "PROGRESS"->{
    if(userLeftMatch || currentMatchScreen==null) return;
    int q0=0,q1=0;
    try{q0=Integer.parseInt(Wire.field(a,1));}catch(Exception ignored){}
    try{q1=Integer.parseInt(Wire.field(a,2));}catch(Exception ignored){}
    String st0=Wire.field(a,3);
    String st1=Wire.field(a,4);
    if(currentMatchScreen!=null){
     boolean isLeft=name!=null&&name.equals(currentMatchLeft);
     int myCurQ=isLeft?q0:q1;
     int oppCurQ=isLeft?q1:q0;
     String mySt=isLeft?st0:st1;
     String oppSt=isLeft?st1:st0;
     currentMatchScreen.updateProgress(myCurQ,oppCurQ,mySt,oppSt);
    }
   }
   case "SUBMITTED"->{
    if(userLeftMatch || currentMatchScreen==null) return;
    if(currentMatchScreen!=null){currentMatchScreen.onSubmittedAcknowledged();}
    ToastManager.success(frame, Wire.field(a,1));
   }
   case "RESULT"->{
    String resultMatchId=(a.length>=26)?Wire.field(a,25):null;
    if(resultMatchId!=null&&abandonedMatchIds.contains(resultMatchId)){
     log("Bỏ qua gói RESULT từ trận đã thoát: "+resultMatchId);
     return;
    }
    if(resultMatchId!=null&&matchId!=null&&!resultMatchId.equals(matchId)){
     log("Bỏ qua gói RESULT không khớp trận hiện tại (packet: "+resultMatchId+", current: "+matchId+")");
     return;
    }
    if(userLeftMatch || currentMatchScreen==null || matchId==null){
     return;
    }
    MusicManager.stopMusic();
    String[] fillInInputs = null;
    if(currentMatchScreen!=null){
     currentMatchScreen.onMatchFinished();
     fillInInputs = currentMatchScreen.getUserFillInAnswers();
     currentSubmittedAnswers=currentMatchScreen.getSelectedAnswers();
     if(currentQuestions==null){currentQuestions=currentMatchScreen.getQuestions();}
    }
    String winner=Wire.field(a,1);String leftInfo=Wire.field(a,2);String rightInfo=Wire.field(a,3);
    log("Kết quả: "+winner+" | "+leftInfo+" | "+rightInfo);
    int[] correctAnswers=null;
    if(a.length>=15){correctAnswers=new int[10];for(int i=0;i<10;i++){try{correctAnswers[i]=Integer.parseInt(Wire.field(a,5+i));}catch(Exception ignored){correctAnswers[i]=-1;}}}
    if(a.length>=25&&currentQuestions!=null){
     java.util.List<quiz.ui.MatchQuestion> enriched=new ArrayList<>();
     for(int i=0;i<currentQuestions.size()&&i<10;i++){
      quiz.ui.MatchQuestion oldQ=currentQuestions.get(i);
      String exp=Wire.field(a,15+i);
      int cAns=(correctAnswers!=null&&i<correctAnswers.length)?correctAnswers[i]:oldQ.correctAnswer();
      enriched.add(new quiz.ui.MatchQuestion(oldQ.text(),oldQ.choices(),exp,oldQ.type(),oldQ.blanks(),oldQ.fillMap(),cAns));
     }
     currentQuestions=enriched;
    }
    if(currentResultDialog!=null){currentResultDialog.dispose();}
    String oppAvatar=(currentMatchScreen!=null&&currentMatchScreen.getOpponentAvatarId()!=null)?currentMatchScreen.getOpponentAvatarId():getAvatarFor(currentOpponent);
    currentResultDialog=new ResultDialog(frame,name,winner,leftInfo,rightInfo,()->send("REMATCH","YES"),()->{send("REMATCH","NO");showLobby();},currentQuestions,currentSubmittedAnswers,correctAnswers,myAvatarId,oppAvatar,fillInInputs);
    currentResultDialog.setVisible(true);
   }
   case "CLOSED"->{
    if(userLeftMatch){
     return;
    }
    String closeMsg = Wire.field(a,1);
    SoundManager.stopCountdown();
    matchId=null;
    if(currentResultDialog!=null && currentResultDialog.isDisplayable()){
     currentResultDialog.onOpponentLeft(closeMsg);
     ToastManager.info(frame, closeMsg);
    } else {
     if(currentMatchScreen!=null){currentMatchScreen.cleanupAndStop();currentMatchScreen=null;}
     if(currentResultDialog!=null){currentResultDialog.dispose();currentResultDialog=null;}
     ToastManager.info(frame, closeMsg);
     showLobby();
    }
   }
   default->log(a[0]+": "+Wire.field(a,1));
  }
  }catch(Exception e){
   System.err.println("[Client] Lỗi xử lý gói tin: " + (a != null && a.length > 0 ? a[0] : "empty"));
   e.printStackTrace();
  }
 }

 void match(String[] a){
  if(currentResultDialog!=null){currentResultDialog.dispose();currentResultDialog=null;}
  matchId=a[1];String left=a[2];String right=a[3];currentMatchLeft=left;String topic=a[4];String level=a[5];String setId=a[6];int totalSec=Integer.parseInt(a[7]);String opponent=name.equals(left)?right:left;currentOpponent=opponent;String oppAvatar=getAvatarFor(opponent);
  java.util.List<MatchQuestion> qList=new ArrayList<>();
  int idx=8;for(int i=0;i<10&&idx<a.length;i++){String qType=a[idx++];String qText=a[idx++];int chCount=Integer.parseInt(a[idx++]);String[] choices=new String[chCount];for(int c=0;c<chCount;c++)choices[c]=a[idx++];int correctAns=Integer.parseInt(a[idx++]);int blanksCount=Integer.parseInt(a[idx++]);String[] blanks=null;if(blanksCount>0){blanks=new String[blanksCount];for(int b=0;b<blanksCount;b++)blanks[b]=a[idx++];}int fmCount=Integer.parseInt(a[idx++]);java.util.Map<String,String> fillMap=null;if(fmCount>0){fillMap=new java.util.LinkedHashMap<>();for(int f=0;f<fmCount;f++){String pair=a[idx++];int eq=pair.indexOf('=');if(eq>=0)fillMap.put(pair.substring(0,eq),pair.substring(eq+1));}}qList.add(new MatchQuestion(qText,choices,"",qType,blanks,fillMap,correctAns));}
  currentQuestions=qList;currentSubmittedAnswers=new int[10];Arrays.fill(currentSubmittedAnswers,-1);
  if(currentMatchScreen!=null){currentMatchScreen.cleanupAndStop();}
  currentMatchScreen=new MatchScreen(matchId,name,opponent,myAvatarId,oppAvatar,topic,level,setId,totalSec,qList,answers->{currentSubmittedAnswers=answers.clone();String[] payload=new String[12];payload[0]="SUBMIT";payload[1]=matchId;for(int i=0;i<10;i++){payload[i+2]=""+answers[i];}send(payload);},this::leaveCurrentMatch);
  currentMatchScreen.setOnEmoteListener(emote->{
   send("EMOTE",emote);
   if("Bot Luyện Tập".equalsIgnoreCase(opponent)){
    String[] botReplies={"👏","🔥","💪","😎","👍"};
    String botEmote=botReplies[new Random().nextInt(botReplies.length)];
    javax.swing.Timer botTimer=new javax.swing.Timer(800,ev->{
     if(currentMatchScreen!=null)currentMatchScreen.showOpponentEmote(opponent,botEmote);
    });
    botTimer.setRepeats(false);
    botTimer.start();
   }
  });
  currentMatchScreen.setOnAnswerListener((qIndex,choice)->{
      if(currentSubmittedAnswers!=null&&qIndex>=0&&qIndex<currentSubmittedAnswers.length){
          currentSubmittedAnswers[qIndex]=choice;
      }
      send("ANSWER",matchId,""+qIndex,""+choice);
  });
  currentMatchScreen.setOnReadyListener(()->send("READY",matchId));
  currentMatchScreen.setOnSkipRevealListener(qIdx->send("SKIP_REVEAL",matchId,String.valueOf(qIdx)));
  frame.getContentPane().removeAll();frame.getContentPane().setLayout(new BorderLayout());frame.getContentPane().add(currentMatchScreen,BorderLayout.CENTER);refresh();
 }

 /** Solo Practice chạy trên Server để chống gian lận (P1 - Item 8) */
 void startSoloPractice(String topic, String level) {
  if (currentResultDialog != null) {
   currentResultDialog.dispose();
   currentResultDialog = null;
  }
  if (topic == null || topic.isBlank()) {
   ToastManager.warning(frame, "Vui lòng chọn chủ đề luyện tập!");
   return;
  }
  if (level == null || level.isBlank()) level = "Dễ";
  send("SOLO_MATCH", topic, level);
 }

 public static void main(String[] args)throws Exception{
  String host=args.length>0?args[0].trim():"127.0.0.1";int port=args.length>1?Integer.parseInt(args[1].trim()):5050;
  if(host.contains(":")){String[] parts=host.split(":");host=parts[0].trim();if(parts.length>1&&!parts[1].trim().isEmpty()){try{port=Integer.parseInt(parts[1].trim());}catch(Exception ignored){}}}
  if(host.isEmpty()) host="127.0.0.1";
  final String finalHost=host;final int finalPort=port;
  SwingUtilities.invokeLater(()->{
   try{new Client(finalHost,finalPort);}catch(Exception e){JOptionPane.showMessageDialog(null,"Không kết nối được server ("+finalHost+":"+finalPort+"): "+e.getMessage());}
  });
 }
}
