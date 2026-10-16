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
    volatile String myAvatarId="avatar_01";
    String matchId;
    String currentMatchLeft;
    String currentOpponent;
    final Map<String, String> userAvatars = new java.util.concurrent.ConcurrentHashMap<>();

    public String getAvatarFor(String username) {
        if (username == null || username.isBlank()) return "avatar_01";
        if (username.equalsIgnoreCase(name)) return myAvatarId != null ? myAvatarId : "avatar_01";
        if (username.equalsIgnoreCase("Bot Luyện Tập")) return "avatar_02";
        return userAvatars.getOrDefault(username, "avatar_01");
    }
    Bank localBank;
    java.util.List<MatchQuestion> currentQuestions;
    int[] currentSubmittedAnswers;
    // --- CONNECTION FIELDS ---
    final String host;
    final int port;
    volatile Thread readerThread;

 Client(String host,int port)throws Exception{
  this.host=host;this.port=port;
  frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
  frame.addWindowListener(new java.awt.event.WindowAdapter() {
   @Override public void windowClosing(java.awt.event.WindowEvent e) {
    MusicManager.shutdown();
    SoundManager.shutdown();
    System.exit(0);
   }
  });
  frame.setLayout(new BorderLayout());
  frame.setSize(960, 720);
  frame.setMinimumSize(new Dimension(880, 640));
  frame.setLocationRelativeTo(null);
  frame.getContentPane().setBackground(Theme.OFF_WHITE);

  // Cấu hình AuthScreen
  authScreen.setOnLogin((u, p) -> send("LOGIN", u, p));
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

  frame.setVisible(true);
  connectAndStartReader();
 }

 /** Tạo socket mới và khởi động reader thread. Gọi khi khởi động hoặc sau logout. */
 void connectAndStartReader() throws Exception {
  Socket socket = new Socket(host, port);
  out = new DataOutputStream(socket.getOutputStream());
  DataInputStream in = new DataInputStream(socket.getInputStream());
  // Dừng reader thread cũ nếu có
  Thread old = readerThread;
  if (old != null) old.interrupt();
  Thread reader = new Thread(() -> {
   try {
    while (true) {
     String[] a = Wire.read(in);
     SwingUtilities.invokeLater(() -> handle(a));
    }
   } catch (Exception e) {
    e.printStackTrace();
    SwingUtilities.invokeLater(() -> {
     if (name != null) {
      // Chỉ hiện disconnect nếu đang trong session
      if (frame.getGlassPane() instanceof DisconnectOverlay) return;
      frame.setGlassPane(new DisconnectOverlay(() -> System.exit(0)));
      frame.getGlassPane().setVisible(true);
     }
     if (name == null) authScreen.showError("Mất kết nối server");
    });
   }
  });
  reader.setDaemon(true);
  reader.start();
  readerThread = reader;
 }

 /** Logout: đóng socket hiện tại, reset session, tạo socket mới, quay về AuthScreen */
 void doLogout() {
   MusicManager.stopMusic();
   SoundManager.stopCountdown();
  // 1. Gửi tín hiệu cho server (nếu còn kết nối)
  // Không cần gửi gì vì đóng socket sẽ trigger cleanup phía server
  // 2. Đặt name = null TRƯỚC khi đóng socket để reader thread không hiện DisconnectOverlay
  name = null;
  myAvatarId = "avatar_01";
  matchId = null;
  currentQuestions = null;
  currentSubmittedAnswers = null;
  // 3. Đóng socket cũ (sẽ khiến reader thread thoát)
  DataOutputStream oldOut = out;
  out = null;
  if (oldOut != null) {
   try { oldOut.close(); } catch (Exception ignored) {}
  }
  // 4. Dừng các dialog/screen đang mở
  if (currentMatchScreen != null) { currentMatchScreen.stopTimer(); currentMatchScreen = null; }
  if (currentResultDialog != null) { currentResultDialog.dispose(); currentResultDialog = null; }
  if (waitingDialog != null) { waitingDialog.closeDialog(); waitingDialog = null; }
  if (currentInviteDialog != null) { currentInviteDialog.dispose(); currentInviteDialog = null; }
  // 5. Quay về AuthScreen
  frame.setGlassPane(new JPanel()); // Xóa DisconnectOverlay nếu có
  frame.getGlassPane().setVisible(false);
  frame.getContentPane().removeAll();
  frame.getContentPane().setLayout(new BorderLayout());
  frame.getContentPane().add(authScreen, BorderLayout.CENTER);
  authScreen.resetForReuse();
  refresh();
  // 6. Tạo socket mới cho lần đăng nhập tiếp theo
  SwingUtilities.invokeLater(() -> {
   try {
    connectAndStartReader();
   } catch (Exception ex) {
    authScreen.showError("Không thể kết nối lại server: " + ex.getMessage());
   }
  });
 }

 void send(String... a){
  DataOutputStream currentOut = out;
  if (currentOut == null) return;
  try{
   Wire.send(currentOut,a);
  }catch(IOException e){
   ToastManager.error(frame, "Không gửi được: "+e.getMessage());
  }
 }

 void log(String s){System.out.println("[Client] "+s);}

 void button(JPanel p,String label,Runnable action){
  ModernButton b=new ModernButton(label, ModernButton.Style.PRIMARY);
  b.addActionListener(e->action.run());
  p.add(b);
 }

 void showLobby(){
  if(currentMatchScreen!=null){currentMatchScreen.stopTimer();currentMatchScreen=null;}
  if(currentResultDialog!=null){currentResultDialog.dispose();currentResultDialog=null;}
  frame.getContentPane().removeAll();
  frame.getContentPane().setLayout(new BorderLayout());
  frame.getContentPane().add(lobbyScreen, BorderLayout.CENTER);
  refresh();
  // Phát nhạc sảnh (không phát lại nếu đang phát rồi)
  MusicManager.playLobbyMusic();
 }

 void refresh(){frame.getContentPane().revalidate();frame.getContentPane().repaint();}

 void handle(String[] a){
  try{
  switch(a[0]){
   case "WELCOME"->{
    name=Wire.field(a,1);
    String rawTopics=Wire.field(a,2);
    myAvatarId=Wire.field(a,3);
    if(myAvatarId==null||myAvatarId.isBlank())myAvatarId="avatar_01";userAvatars.put(name,myAvatarId);
    String scoreStr=Wire.field(a,4);
    int score=0;try{score=Integer.parseInt(scoreStr);}catch(Exception ignored){}
    lobbyScreen.setCurrentUser(name,score,myAvatarId);
    ToastManager.success(frame, "Đăng nhập thành công! Chào mừng "+name);
    if(!rawTopics.isBlank()){lobbyScreen.setTopics(Arrays.asList(rawTopics.split("\\|")));}
    showLobby();
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
    if(waitingDialog!=null){waitingDialog.closeDialog();waitingDialog=null;}
    if(currentInviteDialog!=null){currentInviteDialog.dispose();currentInviteDialog=null;}
    if(currentResultDialog!=null){currentResultDialog.dispose();currentResultDialog=null;}
    lobbyScreen.onMatchmakingCancelled(""); // reset quick match UI
     MusicManager.playBattleMusic();
    match(a);
   }
   case "SCORE"->{
    int s0=0,s1=0;
    try{s0=Integer.parseInt(Wire.field(a,1));}catch(Exception ignored){}
    try{s1=Integer.parseInt(Wire.field(a,2));}catch(Exception ignored){}
    if(currentMatchScreen!=null){
     boolean isLeft=name!=null&&name.equals(currentMatchLeft);
     int myScore=isLeft?s0:s1;
     int oppScore=isLeft?s1:s0;
     currentMatchScreen.updateScores(myScore,oppScore);
    }
   }
   case "SUBMITTED"->{
    if(currentMatchScreen!=null){currentMatchScreen.onSubmittedAcknowledged();}
    ToastManager.success(frame, Wire.field(a,1));
   }
   case "RESULT"->{
    MusicManager.stopMusic();
    if(currentMatchScreen!=null){
     currentMatchScreen.stopTimer();
     if(currentSubmittedAnswers==null){currentSubmittedAnswers=currentMatchScreen.getSelectedAnswers();}
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
      enriched.add(new quiz.ui.MatchQuestion(oldQ.text(),oldQ.choices(),exp,oldQ.type()));
     }
     currentQuestions=enriched;
    }
    if(currentResultDialog!=null){currentResultDialog.dispose();}
    String oppAvatar=(currentMatchScreen!=null&&currentMatchScreen.getOpponentAvatarId()!=null)?currentMatchScreen.getOpponentAvatarId():getAvatarFor(currentOpponent);currentResultDialog=new ResultDialog(frame,name,winner,leftInfo,rightInfo,()->send("REMATCH","YES"),()->{send("REMATCH","NO");showLobby();},currentQuestions,currentSubmittedAnswers,correctAnswers,myAvatarId,oppAvatar);
    currentResultDialog.setVisible(true);
   }
   case "CLOSED"->{
    if(currentMatchScreen!=null){currentMatchScreen.stopTimer();currentMatchScreen=null;}
    if(currentResultDialog!=null){currentResultDialog.dispose();currentResultDialog=null;}
    matchId=null;
    SoundManager.stopCountdown();
    ToastManager.info(frame, Wire.field(a,1));
    showLobby();
   }
   default->log(a[0]+": "+Wire.field(a,1));
  }
  }catch(Exception e){
   System.err.println("[Client] Lỗi xử lý gói tin: " + (a != null && a.length > 0 ? a[0] : "empty"));
   e.printStackTrace();
  }
 }

 void match(String[] a){
  matchId=a[1];String left=a[2];String right=a[3];currentMatchLeft=left;String topic=a[4];String level=a[5];String setId=a[6];int totalSec=Integer.parseInt(a[7]);String opponent=name.equals(left)?right:left;currentOpponent=opponent;String oppAvatar=getAvatarFor(opponent);
  java.util.List<MatchQuestion> qList=new ArrayList<>();
  int idx=8;for(int i=0;i<10&&idx<a.length;i++){String qType=a[idx++];String qText=a[idx++];int chCount=Integer.parseInt(a[idx++]);String[] choices=new String[chCount];for(int c=0;c<chCount;c++)choices[c]=a[idx++];int correctAns=Integer.parseInt(a[idx++]);int blanksCount=Integer.parseInt(a[idx++]);String[] blanks=null;if(blanksCount>0){blanks=new String[blanksCount];for(int b=0;b<blanksCount;b++)blanks[b]=a[idx++];}int fmCount=Integer.parseInt(a[idx++]);java.util.Map<String,String> fillMap=null;if(fmCount>0){fillMap=new java.util.LinkedHashMap<>();for(int f=0;f<fmCount;f++){String pair=a[idx++];int eq=pair.indexOf('=');if(eq>=0)fillMap.put(pair.substring(0,eq),pair.substring(eq+1));}}qList.add(new MatchQuestion(qText,choices,"",qType,blanks,fillMap,correctAns));}
  currentQuestions=qList;currentSubmittedAnswers=new int[10];Arrays.fill(currentSubmittedAnswers,-1);
  if(currentMatchScreen!=null){currentMatchScreen.stopTimer();}
  currentMatchScreen=new MatchScreen(matchId,name,opponent,myAvatarId,oppAvatar,topic,level,setId,totalSec,qList,answers->{currentSubmittedAnswers=answers.clone();String[] payload=new String[12];payload[0]="SUBMIT";payload[1]=matchId;for(int i=0;i<10;i++){payload[i+2]=""+answers[i];}send(payload);},()->send("LEAVE"));
  currentMatchScreen.setOnEmoteListener(emote->send("EMOTE",emote));
  currentMatchScreen.setOnAnswerListener((qIndex,choice)->send("ANSWER",matchId,""+qIndex,""+choice));
  frame.getContentPane().removeAll();frame.getContentPane().setLayout(new BorderLayout());frame.getContentPane().add(currentMatchScreen,BorderLayout.CENTER);refresh();
 }

 void startSoloPractice(String topic, String level) {
  if (topic == null || topic.isBlank()) {ToastManager.warning(frame, "Vui lòng chọn chủ đề luyện tập!");return;}
  if (level == null || level.isBlank()) level = "Dễ";
  if (localBank == null) {
   java.nio.file.Path p = java.nio.file.Path.of("data/questions.txt");
   if (!java.nio.file.Files.exists(p)) p = java.nio.file.Path.of("../data/questions.txt");
   if (java.nio.file.Files.exists(p)) {try {localBank = new Bank(p);} catch (Exception e) {log("Lỗi tải ngân hàng câu hỏi: " + e.getMessage());}}
  }
  if (localBank == null) {ToastManager.error(frame, "Không tìm thấy file ngân hàng câu hỏi data/questions.txt!");return;}
  Model.SetData set = localBank.pick(topic, level, new HashSet<>());
  if (set == null) {ToastManager.error(frame, "Không có bộ đề cho chủ đề: " + topic + " (" + level + ")");return;}
  matchId = "SOLO-" + System.currentTimeMillis();
  java.util.List<MatchQuestion> qList = new ArrayList<>();int[] soloCorrect = new int[10];
  for (int i = 0; i < set.questions().size() && i < 10; i++) {Model.Question q = set.questions().get(i);qList.add(new MatchQuestion(q.text(), q.choices(), q.explanation() != null ? q.explanation() : "", q.type() != null ? q.type().name() : "MULTIPLE_CHOICE", q.blanks(), q.fillMap(), q.answer()));soloCorrect[i] = q.answer();}
  currentQuestions = qList;currentSubmittedAnswers = new int[10];Arrays.fill(currentSubmittedAnswers, -1);long startTime = System.currentTimeMillis();
  final String chosenTopic = topic;final String chosenLevel = level;
  if (currentMatchScreen != null) {currentMatchScreen.stopTimer();}
  if (currentResultDialog != null) {currentResultDialog.dispose();}
  String playerName = (name != null && !name.isBlank()) ? name : "Học Viên";String botName = "Bot Luyện Tập";
  final int[] botLiveScore = {0};
  currentOpponent = botName;
  MusicManager.playBattleMusic();
  currentMatchScreen = new MatchScreen(matchId,playerName,botName,myAvatarId,"avatar_02",topic,level,set.id(),set.duration(),qList,answers -> {long elapsed = System.currentTimeMillis() - startTime;currentSubmittedAnswers = answers.clone();finishSoloMatch(playerName, botName, set, answers, elapsed, soloCorrect, chosenTopic, chosenLevel, botLiveScore[0]);},() -> showLobby());
  currentMatchScreen.setOnAnswerListener((qIndex, choice) -> {
   if (qIndex >= 0 && qIndex < 10 && currentMatchScreen != null) {
    int userCount = 0;
    for (int i = 0; i < 10; i++) {
     if (currentMatchScreen.getSelectedAnswers()[i] != -1 && currentMatchScreen.getSelectedAnswers()[i] == soloCorrect[i]) userCount++;
    }
    currentMatchScreen.setMyScore(userCount);

    // Bot cũng cập nhật kết quả câu này sau khi kết thúc câu
    boolean botRight = new Random().nextInt(100) < (chosenLevel.equalsIgnoreCase("Dễ") ? 60 : chosenLevel.equalsIgnoreCase("Khó") ? 80 : 70);
    if (botRight) {
     botLiveScore[0]++;
     currentMatchScreen.setOpponentScore(botLiveScore[0]);
    }
   }
  });
  currentMatchScreen.setOnEmoteListener(emote -> {String[] botReplies = {"👏", "🔥", "💪", "😎", "👍"};String botEmote = botReplies[new Random().nextInt(botReplies.length)];javax.swing.Timer botTimer = new javax.swing.Timer(800, ev -> {if (currentMatchScreen != null) currentMatchScreen.showOpponentEmote(botName, botEmote);});botTimer.setRepeats(false);botTimer.start();});
  frame.getContentPane().removeAll();frame.getContentPane().setLayout(new BorderLayout());frame.getContentPane().add(currentMatchScreen, BorderLayout.CENTER);refresh();
  ToastManager.info(frame, "Bắt đầu luyện tập đơn: " + topic + " - " + level);
 }

 void finishSoloMatch(String playerName, String botName, Model.SetData set, int[] answers, long elapsedMs, int[] correctAnswers, String topic, String level, int botScore) {
  MusicManager.stopMusic();
  if (currentMatchScreen != null) {currentMatchScreen.stopTimer();}
  int userScore = 0;for (int i = 0; i < 10; i++) {if (answers[i] != -1 && answers[i] == correctAnswers[i]) userScore++;}
  long botElapsed;
  if (level.equalsIgnoreCase("Dễ")) {botElapsed = (set.duration() * 1000L * (45 + new Random().nextInt(30))) / 100;}
  else if (level.equalsIgnoreCase("Khó")) {botElapsed = (set.duration() * 1000L * (55 + new Random().nextInt(35))) / 100;}
  else {botElapsed = (set.duration() * 1000L * (50 + new Random().nextInt(35))) / 100;}
  String winner = "HÒA";
  if (userScore > botScore) winner = playerName;else if (userScore < botScore) winner = botName;else if (Math.abs(elapsedMs - botElapsed) > 1000) winner = (elapsedMs < botElapsed) ? playerName : botName;
  String leftRaw = playerName + ": " + userScore + " câu, " + elapsedMs + " ms";String rightRaw = botName + ": " + botScore + " câu, " + botElapsed + " ms";
  if (currentResultDialog != null) {currentResultDialog.dispose();}
  currentResultDialog = new ResultDialog(frame,playerName,winner,leftRaw,rightRaw,() -> startSoloPractice(topic, level),() -> showLobby(),currentQuestions,currentSubmittedAnswers,correctAnswers,myAvatarId,"avatar_02");
  currentResultDialog.setVisible(true);
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
