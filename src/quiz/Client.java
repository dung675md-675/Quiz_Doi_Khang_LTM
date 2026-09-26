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
    DataOutputStream out;
    String name;
    String matchId;
    Bank localBank;
    java.util.List<MatchQuestion> currentQuestions;
    int[] currentSubmittedAnswers;

 Client(String host,int port)throws Exception{
  Socket socket=new Socket(host,port);
  out=new DataOutputStream(socket.getOutputStream());
  frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
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
   waitingDialog=new WaitingChallengeDialog(frame, target, topic, level);
   waitingDialog.setVisible(true);
  });
  lobbyScreen.setOnLeaderboard(() -> send("LEADERBOARD"));
  lobbyScreen.setOnHistory(() -> send("HISTORY"));
  lobbyScreen.setOnSoloPractice(() -> {
   String topic = lobbyScreen.getSelectedTopic();
   String level = lobbyScreen.getSelectedLevel();
   startSoloPractice(topic, level);
  });

  frame.setVisible(true);

  Thread reader=new Thread(()->{
   try{
    DataInputStream in=new DataInputStream(socket.getInputStream());
    while(true){
     String[] a=Wire.read(in);
     SwingUtilities.invokeLater(()->handle(a));
    }
   }catch(Exception e){
    SwingUtilities.invokeLater(()->{
     ToastManager.error(frame, "Mất kết nối đến server!");
     if(name==null) authScreen.showError("Mất kết nối server");
    });
   }
  });
  reader.setDaemon(true);
  reader.start();
 }

 void send(String... a){
  try{
   Wire.send(out,a);
  }catch(IOException e){
   ToastManager.error(frame, "Không gửi được: "+e.getMessage());
  }
 }

 void log(String s){
  System.out.println("[Client] "+s);
 }

 void button(JPanel p,String label,Runnable action){
  ModernButton b=new ModernButton(label, ModernButton.Style.PRIMARY);
  b.addActionListener(e->action.run());
  p.add(b);
 }

 void showLobby(){
  if(currentMatchScreen!=null){
   currentMatchScreen.stopTimer();
   currentMatchScreen=null;
  }
  if(currentResultDialog!=null){
   currentResultDialog.dispose();
   currentResultDialog=null;
  }
  frame.getContentPane().removeAll();
  frame.getContentPane().setLayout(new BorderLayout());
  frame.getContentPane().add(lobbyScreen, BorderLayout.CENTER);
  refresh();
 }

 void refresh(){
  frame.getContentPane().revalidate();
  frame.getContentPane().repaint();
 }

 void handle(String[] a){
  switch(a[0]){
   case "WELCOME"->{
    name=Wire.field(a,1);
    lobbyScreen.setCurrentUser(name);
    ToastManager.success(frame, "Đăng nhập thành công! Chào mừng "+name);
    String rawTopics=Wire.field(a,2);
    if(!rawTopics.isBlank()){
     lobbyScreen.setTopics(Arrays.asList(rawTopics.split("\\|")));
    }
    showLobby();
   }
   case "INFO"->{
    String msg=Wire.field(a,1);
    if(name==null){
     authScreen.showSuccess(msg);
     if(msg.contains("thành công")){
      authScreen.setMode(AuthScreen.Mode.LOGIN);
      authScreen.clearPassword();
     }
    }
    ToastManager.info(frame, msg);
   }
   case "ERROR"->{
    if(waitingDialog!=null){
     waitingDialog.closeDialog();
     waitingDialog=null;
    }
    String msg=Wire.field(a,1);
    if(name==null){
     authScreen.showError(msg);
    }
    ToastManager.error(frame, msg);
   }
   case "LOBBY"->{
    lobbyScreen.updatePlayersFromLobbyData(Wire.field(a,1));
   }
   case "LEADERBOARD"->{
    new LeaderboardDialog(frame, Wire.field(a,1)).setVisible(true);
   }
   case "HISTORY"->{
    new HistoryDialog(frame, name, Wire.field(a,1)).setVisible(true);
   }
   case "DECLINED"->{
    if(waitingDialog!=null){
     waitingDialog.closeDialog();
     waitingDialog=null;
    }
    if(currentInviteDialog!=null){
     currentInviteDialog.dispose();
     currentInviteDialog=null;
    }
    ToastManager.warning(frame, Wire.field(a,1));
   }
   case "INVITE"->{
    SoundManager.playInviteAlert();
    String from=Wire.field(a,1),topic=Wire.field(a,2),level=Wire.field(a,3);
    String duration=Wire.field(a,5);
    if(currentInviteDialog!=null){
     currentInviteDialog.dispose();
    }
    currentInviteDialog=new InviteDialog(frame, from, topic, level, duration, accept->{
     send(accept?"ACCEPT":"REJECT",from);
    });
    currentInviteDialog.setVisible(true);
   }
   case "EMOTE"->{
    String from=Wire.field(a,1);
    String emote=Wire.field(a,2);
    if(currentMatchScreen!=null){
     currentMatchScreen.showOpponentEmote(from, emote);
    }
   }
   case "MATCH"->{
    if(waitingDialog!=null){
     waitingDialog.closeDialog();
     waitingDialog=null;
    }
    if(currentInviteDialog!=null){
     currentInviteDialog.dispose();
     currentInviteDialog=null;
    }
    if(currentResultDialog!=null){
     currentResultDialog.dispose();
     currentResultDialog=null;
    }
    match(a);
   }
   case "SUBMITTED"->{
    if(currentMatchScreen!=null){
     currentMatchScreen.onSubmittedAcknowledged();
    }
    ToastManager.success(frame, Wire.field(a,1));
   }
   case "RESULT"->{
    if(currentMatchScreen!=null){
     currentMatchScreen.stopTimer();
     if(currentSubmittedAnswers==null){
      currentSubmittedAnswers=currentMatchScreen.getSelectedAnswers();
     }
     if(currentQuestions==null){
      currentQuestions=currentMatchScreen.getQuestions();
     }
    }
    String winner=Wire.field(a,1);
    String leftInfo=Wire.field(a,2);
    String rightInfo=Wire.field(a,3);
    log("Kết quả: "+winner+" | "+leftInfo+" | "+rightInfo);

    int[] correctAnswers=null;
    if(a.length>=15){
     correctAnswers=new int[10];
     for(int i=0;i<10;i++){
      try{
       correctAnswers[i]=Integer.parseInt(Wire.field(a,5+i));
      }catch(Exception ignored){
       correctAnswers[i]= -1;
      }
     }
    }

    if(currentResultDialog!=null){
     currentResultDialog.dispose();
    }
    currentResultDialog=new ResultDialog(
     frame,
     name,
     winner,
     leftInfo,
     rightInfo,
     ()->send("REMATCH","YES"),
     ()->{
      send("REMATCH","NO");
      showLobby();
     },
     currentQuestions,
     currentSubmittedAnswers,
     correctAnswers
    );
    currentResultDialog.setVisible(true);
   }
   case "CLOSED"->{
    if(currentMatchScreen!=null){
     currentMatchScreen.stopTimer();
     currentMatchScreen=null;
    }
    if(currentResultDialog!=null){
     currentResultDialog.dispose();
     currentResultDialog=null;
    }
    matchId=null;
    ToastManager.info(frame, Wire.field(a,1));
    showLobby();
   }
   default->log(a[0]+": "+Wire.field(a,1));
  }
 }

 void match(String[] a){
  matchId=a[1];
  String left=a[2];
  String right=a[3];
  String topic=a[4];
  String level=a[5];
  String setId=a[6];
  int totalSec=Integer.parseInt(a[7]);
  String opponent=name.equals(left)?right:left;

  java.util.List<MatchQuestion> qList=new ArrayList<>();
  for(int i=0;i<10;i++){
   int base=8+i*5;
   String qText=a[base];
   String[] choices=new String[]{a[base+1],a[base+2],a[base+3],a[base+4]};
   qList.add(new MatchQuestion(qText,choices));
  }
  currentQuestions=qList;
  currentSubmittedAnswers=new int[10];
  Arrays.fill(currentSubmittedAnswers, -1);

  if(currentMatchScreen!=null){
   currentMatchScreen.stopTimer();
  }

  currentMatchScreen=new MatchScreen(
   matchId,
   name,
   opponent,
   topic,
   level,
   setId,
   totalSec,
   qList,
   answers->{
    currentSubmittedAnswers=answers.clone();
    String[] payload=new String[12];
    payload[0]="SUBMIT";
    payload[1]=matchId;
    for(int i=0;i<10;i++){
     payload[i+2]=""+answers[i];
    }
    send(payload);
   },
   ()->send("LEAVE")
  );
  currentMatchScreen.setOnEmoteListener(emote->send("EMOTE",emote));

  frame.getContentPane().removeAll();
  frame.getContentPane().setLayout(new BorderLayout());
  frame.getContentPane().add(currentMatchScreen,BorderLayout.CENTER);
  refresh();
 }

 void startSoloPractice(String topic, String level) {
  if (topic == null || topic.isBlank()) {
   ToastManager.warning(frame, "Vui lòng chọn chủ đề luyện tập!");
   return;
  }
  if (level == null || level.isBlank()) {
   level = "Dễ";
  }
  if (localBank == null) {
   java.nio.file.Path p = java.nio.file.Path.of("data/questions.txt");
   if (!java.nio.file.Files.exists(p)) {
    p = java.nio.file.Path.of("../data/questions.txt");
   }
   if (java.nio.file.Files.exists(p)) {
    try {
     localBank = new Bank(p);
    } catch (Exception e) {
     log("Lỗi tải ngân hàng câu hỏi: " + e.getMessage());
    }
   }
  }

  if (localBank == null) {
   ToastManager.error(frame, "Không tìm thấy file ngân hàng câu hỏi data/questions.txt!");
   return;
  }

  Model.SetData set = localBank.pick(topic, level, new HashSet<>());
  if (set == null) {
   ToastManager.error(frame, "Không có bộ đề cho chủ đề: " + topic + " (" + level + ")");
   return;
  }

  matchId = "SOLO-" + System.currentTimeMillis();
  java.util.List<MatchQuestion> qList = new ArrayList<>();
  int[] soloCorrect = new int[10];
  for (int i = 0; i < set.questions().size() && i < 10; i++) {
   Model.Question q = set.questions().get(i);
   qList.add(new MatchQuestion(q.text(), q.choices()));
   soloCorrect[i] = q.answer();
  }

  currentQuestions = qList;
  currentSubmittedAnswers = new int[10];
  Arrays.fill(currentSubmittedAnswers, -1);
  long startTime = System.currentTimeMillis();
  final String chosenTopic = topic;
  final String chosenLevel = level;

  if (currentMatchScreen != null) {
   currentMatchScreen.stopTimer();
  }
  if (currentResultDialog != null) {
   currentResultDialog.dispose();
  }

  String playerName = (name != null && !name.isBlank()) ? name : "Học Viên";
  String botName = "🤖 Bot Luyện Tập";

  currentMatchScreen = new MatchScreen(
   matchId,
   playerName,
   botName,
   topic,
   level,
   set.id(),
   set.duration(),
   qList,
   answers -> {
    long elapsed = System.currentTimeMillis() - startTime;
    currentSubmittedAnswers = answers.clone();
    finishSoloMatch(playerName, botName, set, answers, elapsed, soloCorrect, chosenTopic, chosenLevel);
   },
   () -> showLobby()
  );

  currentMatchScreen.setOnEmoteListener(emote -> {
   String[] botReplies = {"👏", "🔥", "💪", "😎", "👍"};
   String botEmote = botReplies[new Random().nextInt(botReplies.length)];
   javax.swing.Timer botTimer = new javax.swing.Timer(800, ev -> {
    if (currentMatchScreen != null) {
     currentMatchScreen.showOpponentEmote(botName, botEmote);
    }
   });
   botTimer.setRepeats(false);
   botTimer.start();
  });

  frame.getContentPane().removeAll();
  frame.getContentPane().setLayout(new BorderLayout());
  frame.getContentPane().add(currentMatchScreen, BorderLayout.CENTER);
  refresh();
  ToastManager.info(frame, "Bắt đầu luyện tập đơn: " + topic + " - " + level);
 }

 void finishSoloMatch(String playerName, String botName, Model.SetData set, int[] answers, long elapsedMs, int[] correctAnswers, String topic, String level) {
  if (currentMatchScreen != null) {
   currentMatchScreen.stopTimer();
  }
  int userScore = 0;
  for (int i = 0; i < 10; i++) {
   if (answers[i] != -1 && answers[i] == correctAnswers[i]) {
    userScore++;
   }
  }

  int botScore;
  long botElapsed;
  if (level.equalsIgnoreCase("Dễ")) {
   botScore = 5 + new Random().nextInt(3);
   botElapsed = (set.duration() * 1000L * (45 + new Random().nextInt(30))) / 100;
  } else if (level.equalsIgnoreCase("Khó")) {
   botScore = 6 + new Random().nextInt(4);
   botElapsed = (set.duration() * 1000L * (55 + new Random().nextInt(35))) / 100;
  } else {
   botScore = 5 + new Random().nextInt(4);
   botElapsed = (set.duration() * 1000L * (50 + new Random().nextInt(35))) / 100;
  }

  String winner = "HÒA";
  if (userScore > botScore) winner = playerName;
  else if (userScore < botScore) winner = botName;
  else if (Math.abs(elapsedMs - botElapsed) > 1000) {
   winner = (elapsedMs < botElapsed) ? playerName : botName;
  }

  String leftRaw = playerName + ": " + userScore + " câu, " + elapsedMs + " ms";
  String rightRaw = botName + ": " + botScore + " câu, " + botElapsed + " ms";

  if (currentResultDialog != null) {
   currentResultDialog.dispose();
  }

  currentResultDialog = new ResultDialog(
   frame,
   playerName,
   winner,
   leftRaw,
   rightRaw,
   () -> startSoloPractice(topic, level),
   () -> showLobby(),
   currentQuestions,
   currentSubmittedAnswers,
   correctAnswers
  );
  currentResultDialog.setVisible(true);
 }

 public static void main(String[] args)throws Exception{
  String host=args.length>0?args[0]:"127.0.0.1";
  int port=args.length>1?Integer.parseInt(args[1]):5050;
  SwingUtilities.invokeLater(()->{
   try{
    new Client(host,port);
   }catch(Exception e){
    JOptionPane.showMessageDialog(null,"Không kết nối được server: "+e.getMessage());
   }
  });
 }
}
