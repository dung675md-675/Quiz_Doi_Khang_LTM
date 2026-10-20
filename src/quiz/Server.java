package quiz;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;
public final class Server {
 final DB db;final Bank bank;final Model.Store store;final File storeFile;
 final Map<String,Peer> online=new ConcurrentHashMap<>();
 final Map<String,Match> matches=new ConcurrentHashMap<>();
 final Map<String,Invite> invites=new ConcurrentHashMap<>();
 // Matchmaking queue: dùng ConcurrentHashMap để O(1) check/remove và tránh duplicate
 final Map<String,Peer> matchmakingQueue=new ConcurrentHashMap<>();
 final ScheduledExecutorService timer=Executors.newScheduledThreadPool(2);
 final ExecutorService workers=Executors.newCachedThreadPool();
 record Invite(String from,String to,String topic,String level){}
 Server(Path folder)throws IOException{
  Files.createDirectories(folder);db=new DB(folder);bank=new Bank(folder.resolve("questions.txt"),db);storeFile=folder.resolve("users.dat").toFile();store=Model.Store.read(storeFile);
  if(db.isAvailable()){store.users.putAll(db.getAllUsers());store.results.clear();store.results.addAll(db.getAllResults());}
  synchronized(store){
   Model.User adminUser=store.users.get("admin");
   if(adminUser==null){
    String hashed=Security.hashPassword("admin123");
    adminUser=new Model.User("admin",hashed,100,"avatar_01","Quản Trị Viên","admin@quizarena.vn","Tài khoản Quản trị viên hệ thống","ADMIN");
    store.users.put("admin",adminUser);
    if(db.isAvailable())db.createUser("admin",hashed,100,"avatar_01","Quản Trị Viên","admin@quizarena.vn","Tài khoản Quản trị viên hệ thống","ADMIN");
    store.save(storeFile);
   }else if(!"ADMIN".equalsIgnoreCase(adminUser.role)){
    adminUser.role="ADMIN";
    if(db.isAvailable())db.updateUser("admin",null,adminUser.score,adminUser.avatarId,adminUser.fullName,adminUser.email,adminUser.bio,"ADMIN");
    store.save(storeFile);
   }
  }
  // Matchmaking scheduler: kiểm tra và ghép cặp mỗi 2 giây
  timer.scheduleAtFixedRate(this::runMatchmaking,2,2,TimeUnit.SECONDS);
 }
 boolean isAdmin(Peer p){
  if(p==null||p.name==null)return false;
  if("admin".equalsIgnoreCase(p.name))return true;
  synchronized(store){
   Model.User u=store.users.get(p.name);
   return u!=null&&u.isAdmin();
  }
 }
 static final class Peer {final Socket socket;final DataInputStream in;final DataOutputStream out;volatile String name;Peer(Socket s)throws IOException{socket=s;in=new DataInputStream(s.getInputStream());out=new DataOutputStream(s.getOutputStream());}void send(String... a){try{Wire.send(out,a);}catch(IOException e){try{socket.close();}catch(IOException ignored){}}}}
 final class Match {
  final String id=UUID.randomUUID().toString(),left,right,topic,level;
  final boolean isSolo;
  final Set<String> used=new HashSet<>();
  Model.SetData set;
  long start,deadline;
  boolean done;
  final int[] correct=new int[2];
  final int[] points=new int[2];
  final int[] streak=new int[2];
  final int[] maxStreak=new int[2];
  final long[] duration=new long[2];
  final long[] questionStartNano=new long[2];
  final boolean[] submitted=new boolean[2],rematch=new boolean[2];
  final int[][] liveAnswers=new int[2][10];
  final boolean[][] scoredQuestion=new boolean[2][10];
  final int[] currentQ=new int[2];
  final ScheduledFuture<?>[] disconnectFutures=new ScheduledFuture<?>[2];
  ScheduledFuture<?> expireFuture;
  enum MatchPhase { READY_CHECK, COUNTDOWN, QUESTION_INTRO, ACTIVE, REVEAL, FINISHED }
  MatchPhase currentPhase = MatchPhase.READY_CHECK;
  final boolean[] ready = new boolean[2];
  final boolean[] skipReveal = new boolean[2];
  int currentQuestionIndex = 0;
  int countdownStep = 3;
  ScheduledFuture<?> phaseFuture = null;
  Match(String a,String b,String t,String l){left=a;right=b;topic=t;level=l;isSolo="Bot Luyện Tập".equals(b);}
  synchronized void cancelPhaseTimer(){
   if(phaseFuture!=null){phaseFuture.cancel(false);phaseFuture=null;}
  }
  synchronized void cancelTimers(){
   cancelPhaseTimer();
   if(expireFuture!=null){expireFuture.cancel(false);expireFuture=null;}
   for(int k=0;k<2;k++){
    if(disconnectFutures[k]!=null){disconnectFutures[k].cancel(false);disconnectFutures[k]=null;}
   }
  }
  int index(String name){return name.equals(left)?0:name.equals(right)?1:-1;}
  String encodeStates(int pIdx){
   StringBuilder sb=new StringBuilder(10);
   for(int k=0;k<10;k++){
    int ans=liveAnswers[pIdx][k];
    if(ans==-1){
     sb.append(submitted[pIdx]?'3':'0');
    }else if(set!=null&&k<set.questions().size()&&ans==set.questions().get(k).answer()){
     sb.append('1');
    }else{
     sb.append('2');
    }
   }
   return sb.toString();
  }
  synchronized void sendProgress(){
   String s0=encodeStates(0);
   String s1=encodeStates(1);
   send(left,"PROGRESS",String.valueOf(currentQ[0]),String.valueOf(currentQ[1]),s0,s1);
   if(!isSolo)send(right,"PROGRESS",String.valueOf(currentQ[1]),String.valueOf(currentQ[0]),s1,s0);
  }
  synchronized void sendScores(){
   String[] pkt=new String[]{"SCORE",String.valueOf(correct[0]),String.valueOf(correct[1]),String.valueOf(points[0]),String.valueOf(points[1]),String.valueOf(streak[0]),String.valueOf(streak[1])};
   send(left,pkt);
   if(!isSolo)send(right,pkt);
  }
  void recordQuestionScore(int i,int qIndex,int choice,long elapsedMs){
   if(qIndex<0||qIndex>=10||scoredQuestion[i][qIndex]||set==null||qIndex>=set.questions().size())return;
   scoredQuestion[i][qIndex]=true;
   if(choice!=-1&&choice==set.questions().get(qIndex).answer()){
    streak[i]++;
    if(streak[i]>maxStreak[i])maxStreak[i]=streak[i];
    long maxMs=Math.max(5000L,(set.duration()*1000L)/10L);
    double ratio=Math.max(0.0,Math.min(1.0,1.0-(double)elapsedMs/maxMs));
    int speedBonus=(int)Math.round(ratio*50.0);
    int streakBonus=streak[i]>=2?Math.min(50,(streak[i]-1)*15):0;
    points[i]+=100+speedBonus+streakBonus;
   }else{
    streak[i]=0;
   }
   questionStartNano[i]=System.nanoTime();
  }
  void simulateBotStep(int qIndex){
   if(!isSolo||qIndex<0||qIndex>=10||scoredQuestion[1][qIndex]||set==null||qIndex>=set.questions().size())return;
   Random rnd=ThreadLocalRandom.current();
   double acc="Dễ".equalsIgnoreCase(level)?0.60:"Khó".equalsIgnoreCase(level)?0.80:0.70;
   int correctAns=set.questions().get(qIndex).answer();
   int botChoice;
   if(rnd.nextDouble()<acc){
    botChoice=correctAns;
   }else{
    botChoice=(correctAns+1+rnd.nextInt(3))%4;
   }
   liveAnswers[1][qIndex]=botChoice;
   currentQ[1]=Math.min(9,qIndex+1);
   long simElapsedMs=1800L+rnd.nextInt(4500);
   recordQuestionScore(1,qIndex,botChoice,simElapsedMs);
   int p1=0;
   for(int k=0;k<10;k++){
    if(liveAnswers[1][k]!=-1&&liveAnswers[1][k]==set.questions().get(k).answer())p1++;
   }
   correct[1]=p1;
  }
  synchronized void progress(String name,int qIndex){
   int i=index(name);if(i<0||done)return;
   if(qIndex>=0&&qIndex<10){
    if(qIndex!=currentQ[i])questionStartNano[i]=System.nanoTime();
    currentQ[i]=qIndex;
    sendProgress();
   }
  }
  ArrayList<String> buildMatchPacket(){
   ArrayList<String> msg=new ArrayList<>(List.of("MATCH",id,left,right,topic,level,set.id(),""+set.duration()));
   for(Model.Question q:set.questions()){
    msg.add(q.type()!=null?q.type().name():"MULTIPLE_CHOICE");
    msg.add(q.text());
    String[] ch=q.choices();
    msg.add(""+(ch!=null?ch.length:0));
    if(ch!=null)for(String c:ch)msg.add(c);
    msg.add(""+q.answer());
    String[] blanks=q.blanks();
    if(blanks!=null&&blanks.length>0){msg.add(""+blanks.length);for(String b:blanks)msg.add(b);}
    else{msg.add("0");}
    java.util.Map<String,String> fm=q.fillMap();
    if(fm!=null&&!fm.isEmpty()){msg.add(""+fm.size());for(var e:fm.entrySet()){msg.add(e.getKey()+"="+e.getValue());}}
    else{msg.add("0");}
   }
   return msg;
  }
  synchronized void start(){
   try{
    int validCount=bank.countValidQuestions(topic,level);
    if(validCount<10){
     close("Chủ đề '"+topic+"' hiện chỉ có "+validCount+"/10 câu hỏi hợp lệ. Không thể bắt đầu trận đấu!");
     return;
    }
    set=bank.pick(topic,level,used);
    if(set==null||set.questions()==null||set.questions().size()<10){
     close("Không tìm thấy đủ 10 câu hỏi hợp lệ cho chủ đề '"+topic+"'");
     return;
    }
    used.add(set.id());
    Arrays.fill(submitted,false);
    Arrays.fill(rematch,false);
    Arrays.fill(correct,0);
    Arrays.fill(points,0);
    Arrays.fill(streak,0);
    Arrays.fill(maxStreak,0);
    Arrays.fill(duration,0);
    Arrays.fill(liveAnswers[0],-1);
    Arrays.fill(liveAnswers[1],-1);
    Arrays.fill(scoredQuestion[0],false);
    Arrays.fill(scoredQuestion[1],false);
    Arrays.fill(currentQ,0);
    done=false;
    currentQuestionIndex=0;
    Arrays.fill(ready,false);
    Arrays.fill(skipReveal,false);
    start=System.nanoTime();
    Arrays.fill(questionStartNano,start);
    deadline=start+TimeUnit.SECONDS.toNanos(set.duration()+150);
    String[] matchPkt=buildMatchPacket().toArray(String[]::new);
    send(left,matchPkt);
    if(!isSolo)send(right,matchPkt);
    sendScores();
    sendProgress();
    startReadyCheckPhase();
    final String currentSetId=set.id();
    expireFuture=timer.schedule(()->{synchronized(Match.this){if(!done&&set!=null&&set.id().equals(currentSetId))finishExpired();}},set.duration()+150,TimeUnit.SECONDS);
    broadcast();
   }catch(Exception e){
    e.printStackTrace();
    close("Lỗi khi bắt đầu trận: "+e.getMessage());
   }
  }
  synchronized void startReadyCheckPhase(){
   cancelPhaseTimer();
   currentPhase=MatchPhase.READY_CHECK;
   Arrays.fill(ready,false);
   if(isSolo)ready[1]=true;
   send(left,"MATCH_PHASE","READY_CHECK",id,"10",String.valueOf(ready[0]),String.valueOf(ready[1]));
   if(!isSolo)send(right,"MATCH_PHASE","READY_CHECK",id,"10",String.valueOf(ready[1]),String.valueOf(ready[0]));
   phaseFuture=timer.schedule(()->{
    synchronized(Match.this){
     if(done||currentPhase!=MatchPhase.READY_CHECK)return;
     if(!ready[0]||!ready[1]){
      String notReady=!ready[0]?left:right;
      close("Trận đấu bị hủy do "+notReady+" không sẵn sàng kịp thời.");
     }
    }
   },10,TimeUnit.SECONDS);
  }
  synchronized void onPlayerReady(String name){
   if(done||currentPhase!=MatchPhase.READY_CHECK)return;
   int i=index(name);if(i<0)return;
   ready[i]=true;
   send(left,"READY_STATUS",left,String.valueOf(ready[0]),right,String.valueOf(ready[1]));
   if(!isSolo)send(right,"READY_STATUS",right,String.valueOf(ready[1]),left,String.valueOf(ready[0]));
   if(ready[0]&&ready[1]){
    cancelPhaseTimer();
    startCountdownPhase();
   }
  }
  synchronized void startCountdownPhase(){
   cancelPhaseTimer();
   currentPhase=MatchPhase.COUNTDOWN;
   countdownStep=3;
   runCountdownStep(3);
  }
  synchronized void runCountdownStep(int step){
   if(done||currentPhase!=MatchPhase.COUNTDOWN)return;
   countdownStep=step;
   String stepStr=(step==0)?"GO":String.valueOf(step);
   send(left,"MATCH_PHASE","COUNTDOWN",id,stepStr);
   if(!isSolo)send(right,"MATCH_PHASE","COUNTDOWN",id,stepStr);
   if(step>0){
    phaseFuture=timer.schedule(()->{
     synchronized(Match.this){
      runCountdownStep(step-1);
     }
    },1000,TimeUnit.MILLISECONDS);
   }else{
    phaseFuture=timer.schedule(()->{
     synchronized(Match.this){
      startQuestionIntroPhase(0);
     }
    },700,TimeUnit.MILLISECONDS);
   }
  }
  synchronized void startQuestionIntroPhase(int qIndex){
   if(done)return;
   cancelPhaseTimer();
   currentPhase=MatchPhase.QUESTION_INTRO;
   currentQuestionIndex=qIndex;
   currentQ[0]=qIndex;currentQ[1]=qIndex;
   sendProgress();
   Model.Question q=(set!=null&&qIndex<set.questions().size())?set.questions().get(qIndex):null;
   String qType=(q!=null&&q.type()!=null)?q.type().name():"MULTIPLE_CHOICE";
   String[] introPkt=new String[]{"MATCH_PHASE","QUESTION_INTRO",id,String.valueOf(qIndex),topic,qType,"800"};
   send(left,introPkt);
   if(!isSolo)send(right,introPkt);
   phaseFuture=timer.schedule(()->{
    synchronized(Match.this){
     startQuestionActivePhase(qIndex);
    }
   },800,TimeUnit.MILLISECONDS);
  }
  synchronized void startQuestionActivePhase(int qIndex){
   if(done)return;
   cancelPhaseTimer();
   currentPhase=MatchPhase.ACTIVE;
   currentQuestionIndex=qIndex;
   questionStartNano[0]=System.nanoTime();
   questionStartNano[1]=System.nanoTime();
   int perQSec=Math.max(10,Math.min(25,set.duration()/10));
   String[] activePkt=new String[]{"MATCH_PHASE","QUESTION_ACTIVE",id,String.valueOf(qIndex),String.valueOf(perQSec)};
   send(left,activePkt);
   if(!isSolo)send(right,activePkt);
   if(isSolo){
    int delay=1200+ThreadLocalRandom.current().nextInt(Math.min(3500,Math.max(1500,perQSec*1000-1500)));
    timer.schedule(()->{
     synchronized(Match.this){
      if(currentPhase==MatchPhase.ACTIVE&&currentQuestionIndex==qIndex&&liveAnswers[1][qIndex]==-1){
       simulateBotStep(qIndex);
       checkBothAnsweredAndReveal(qIndex);
      }
     }
    },delay,TimeUnit.MILLISECONDS);
   }
   phaseFuture=timer.schedule(()->{
    synchronized(Match.this){
     if(currentPhase==MatchPhase.ACTIVE&&currentQuestionIndex==qIndex){
      startQuestionRevealPhase(qIndex);
     }
    }
   },perQSec,TimeUnit.SECONDS);
  }
  synchronized void checkBothAnsweredAndReveal(int qIndex){
   if(currentPhase!=MatchPhase.ACTIVE||currentQuestionIndex!=qIndex)return;
   boolean p0Answered=(liveAnswers[0][qIndex]!=-1);
   boolean p1Answered=(liveAnswers[1][qIndex]!=-1);
   if(p0Answered&&p1Answered){
    cancelPhaseTimer();
    phaseFuture=timer.schedule(()->{
     synchronized(Match.this){
      if(currentPhase==MatchPhase.ACTIVE&&currentQuestionIndex==qIndex){
       startQuestionRevealPhase(qIndex);
      }
     }
    },300,TimeUnit.MILLISECONDS);
   }
  }
  synchronized void startQuestionRevealPhase(int qIndex){
   if(done)return;
   cancelPhaseTimer();
   currentPhase=MatchPhase.REVEAL;
   currentQuestionIndex=qIndex;
   Arrays.fill(skipReveal,false);
   if(isSolo)skipReveal[1]=true;
   Model.Question q=(set!=null&&qIndex<set.questions().size())?set.questions().get(qIndex):null;
   int correctAns=(q!=null)?q.answer():-1;
   String exp=(q!=null&&q.explanation()!=null)?q.explanation():"";
   String[] revealLeft=new String[]{
    "MATCH_PHASE","QUESTION_REVEAL",id,String.valueOf(qIndex),String.valueOf(correctAns),exp,
    String.valueOf(points[0]),String.valueOf(points[1]),
    String.valueOf(streak[0]),String.valueOf(streak[1]),
    String.valueOf(liveAnswers[0][qIndex]),String.valueOf(liveAnswers[1][qIndex])
   };
   send(left,revealLeft);
   if(!isSolo){
    String[] revealRight=new String[]{
     "MATCH_PHASE","QUESTION_REVEAL",id,String.valueOf(qIndex),String.valueOf(correctAns),exp,
     String.valueOf(points[1]),String.valueOf(points[0]),
     String.valueOf(streak[1]),String.valueOf(streak[0]),
     String.valueOf(liveAnswers[1][qIndex]),String.valueOf(liveAnswers[0][qIndex])
    };
    send(right,revealRight);
   }
   sendScores();
   sendProgress();
   phaseFuture=timer.schedule(()->{
    synchronized(Match.this){
     if(currentPhase==MatchPhase.REVEAL&&currentQuestionIndex==qIndex){
      advanceAfterReveal(qIndex);
     }
    }
   },1800,TimeUnit.MILLISECONDS);
  }
  synchronized void onPlayerSkipReveal(String name,int qIndex){
   if(done||currentPhase!=MatchPhase.REVEAL||currentQuestionIndex!=qIndex)return;
   int i=index(name);if(i<0)return;
   skipReveal[i]=true;
   if(skipReveal[0]&&skipReveal[1]){
    cancelPhaseTimer();
    advanceAfterReveal(qIndex);
   }
  }
  synchronized void advanceAfterReveal(int qIndex){
   cancelPhaseTimer();
   if(qIndex+1<10&&set!=null&&qIndex+1<set.questions().size()){
    startQuestionIntroPhase(qIndex+1);
   }else{
    for(int i=0;i<2;i++){
     submitted[i]=true;
     currentQ[i]=9;
    }
    finish();
   }
  }
  synchronized void answer(String name,int qIndex,int choice){
   int i=index(name);if(i<0||done||submitted[i])return;
   if(qIndex>=0&&qIndex<10&&set!=null){
    long elapsedMs=Math.max(100L,TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-questionStartNano[i]));
    currentQ[i]=qIndex;
    liveAnswers[i][qIndex]=choice;
    recordQuestionScore(i,qIndex,choice,elapsedMs);
    int p0=0,p1=0;
    for(int k=0;k<10;k++){
     if(liveAnswers[0][k]!=-1&&liveAnswers[0][k]==set.questions().get(k).answer())p0++;
     if(liveAnswers[1][k]!=-1&&liveAnswers[1][k]==set.questions().get(k).answer())p1++;
    }
    correct[0]=p0;correct[1]=p1;
    if(isSolo&&i==0){
     simulateBotStep(qIndex);
    }
    sendScores();
    sendProgress();
    checkBothAnsweredAndReveal(qIndex);
   }
  }
  synchronized void submit(String name,String[] a){
   int i=index(name);if(i<0||done||submitted[i])return;
   long now=System.nanoTime();
   if(now>=deadline){finishExpired();return;}
   if(a.length!=12){send(name,"ERROR","Phải gửi đúng 10 đáp án");return;}
   int cCount=0;
   long avgElapsed=Math.max(500L,TimeUnit.NANOSECONDS.toMillis(now-start)/10L);
   for(int k=0;k<10;k++){
    try{
     int v=Integer.parseInt(a[k+2]);
     if(v!=-1&&(v<0||v>15)){send(name,"ERROR","Đáp án không hợp lệ");return;}
     liveAnswers[i][k]=v;
     if(!scoredQuestion[i][k]){
      recordQuestionScore(i,k,v,avgElapsed);
     }
     if(v!=-1&&v==set.questions().get(k).answer())cCount++;
    }catch(NumberFormatException e){send(name,"ERROR","Đáp án không hợp lệ");return;}
   }
   correct[i]=cCount;
   duration[i]=TimeUnit.NANOSECONDS.toMillis(now-start);
   submitted[i]=true;
   currentQ[i]=9;
   send(name,"SUBMITTED","Đã nhận bài");
   if(isSolo&&i==0){
    for(int k=0;k<10;k++)simulateBotStep(k);
    submitted[1]=true;
    currentQ[1]=9;
    long botDelta=ThreadLocalRandom.current().nextLong(-2500L,3500L);
    duration[1]=Math.max(3000L,Math.min(set.duration()*1000L,duration[0]+botDelta));
   }
   sendScores();
   sendProgress();
   if(submitted[0]&&submitted[1])finish();
  }
  synchronized void finishExpired(){
   if(done)return;
   cancelTimers();
   for(int i=0;i<2;i++){
    if(!submitted[i]){
     submitted[i]=true;
     duration[i]=set!=null?set.duration()*1000L:60000L;
     int cCount=0;
     if(set!=null){
      for(int k=0;k<10&&k<set.questions().size();k++){
       if(!scoredQuestion[i][k])recordQuestionScore(i,k,liveAnswers[i][k],set.duration()*100L);
       if(liveAnswers[i][k]!=-1&&liveAnswers[i][k]==set.questions().get(k).answer())cCount++;
      }
     }
     correct[i]=cCount;
    }
   }
   finish();
  }
  synchronized void finish(){
   if(done)return;
   done=true;
   cancelTimers();
   String winner="HÒA";
   if(correct[0]!=correct[1])winner=correct[0]>correct[1]?left:right;
   else if(points[0]!=points[1])winner=points[0]>points[1]?left:right;
   else if(Math.abs(duration[0]-duration[1])>1000)winner=duration[0]<duration[1]?left:right;
   if(!isSolo){
    synchronized(store){
     if(!winner.equals("HÒA")){
      String loser=winner.equals(left)?right:left;
      Model.User uw=store.users.get(winner),ul=store.users.get(loser);
      if(uw!=null)uw.score+=2;
      if(ul!=null)ul.score=Math.max(0,ul.score-1);
      if(db.isAvailable())db.updateMatchScores(winner,loser);
     }
     Model.Result res=new Model.Result(id,left,right,topic,level,set.id(),correct[0],correct[1],duration[0],duration[1],set.duration(),winner,System.currentTimeMillis());
     store.results.add(res);
     if(db.isAvailable())db.saveResult(res);
     store.save(storeFile);
    }
   }
   String leftStat=left+": "+correct[0]+" câu, "+duration[0]+" ms;pts="+points[0]+";streak="+maxStreak[0];
   String rightStat=right+": "+correct[1]+" câu, "+duration[1]+" ms;pts="+points[1]+";streak="+maxStreak[1];
   ArrayList<String> msg=new ArrayList<>(List.of("RESULT",winner,leftStat,rightStat,"Chơi tiếp? Chọn Có hoặc Không"));
   for(Model.Question q:set.questions())msg.add(""+q.answer());
   for(Model.Question q:set.questions())msg.add(q.explanation()!=null?q.explanation():"");
   msg.add(id);
   String[] resPacket=msg.toArray(String[]::new);
   send(left,resPacket);
   if(!isSolo)send(right,resPacket);
   broadcast();
  }
  synchronized void vote(String name,boolean yes){
   int i=index(name);if(i<0||!done)return;
   if(isSolo){
    if(!yes){
     matches.remove(left,this);
     broadcast();
    }else{
     start();
    }
    return;
   }
   if(!yes){
    matches.remove(left,this);
    matches.remove(right,this);
    String other=name.equals(left)?right:left;
    send(other,"CLOSED",name+" đã về sảnh chờ");
    broadcast();
    return;
   }
   rematch[i]=true;
   send(name,"INFO","Đang chờ đối thủ đồng ý tái đấu");
   if(rematch[0]&&rematch[1])start();
  }
  synchronized void onNetworkDrop(String name){
   if(done||isSolo){
    disconnected(name);
    return;
   }
   int i=index(name);if(i<0)return;
   if(disconnectFutures[i]!=null)disconnectFutures[i].cancel(false);
   String other=(i==0)?right:left;
   send(other,"INFO","Đối thủ "+name+" bị mất kết nối mạng. Đang chờ kết nối lại (25 giây)...");
   disconnectFutures[i]=timer.schedule(()->{
    synchronized(Match.this){
     if(!done&&!online.containsKey(name)){
      disconnected(name);
     }
    }
   },25,TimeUnit.SECONDS);
  }
  synchronized void resumePlayer(String name){
   int i=index(name);if(i<0||done||set==null)return;
   if(disconnectFutures[i]!=null){
    disconnectFutures[i].cancel(false);
    disconnectFutures[i]=null;
   }
   String[] matchPkt=buildMatchPacket().toArray(String[]::new);
   send(name,matchPkt);
   long remNanos=Math.max(1_000_000_000L,deadline-System.nanoTime());
   int remSec=(int)Math.max(5L,Math.min(set.duration(),TimeUnit.NANOSECONDS.toSeconds(remNanos)-35L));
   ArrayList<String> resumeMsg=new ArrayList<>();
   resumeMsg.add("MATCH_RESUME");
   resumeMsg.add(id);
   resumeMsg.add(String.valueOf(currentQ[i]));
   resumeMsg.add(String.valueOf(remSec));
   resumeMsg.add(String.valueOf(submitted[i]));
   for(int k=0;k<10;k++)resumeMsg.add(String.valueOf(liveAnswers[i][k]));
   send(name,resumeMsg.toArray(String[]::new));
   if(currentPhase==MatchPhase.READY_CHECK){
    send(name,"MATCH_PHASE","READY_CHECK",id,"10",String.valueOf(ready[i]),String.valueOf(ready[1-i]));
   }else if(currentPhase==MatchPhase.COUNTDOWN){
    send(name,"MATCH_PHASE","COUNTDOWN",id,String.valueOf(countdownStep));
   }else if(currentPhase==MatchPhase.QUESTION_INTRO){
    Model.Question q=(currentQuestionIndex<set.questions().size())?set.questions().get(currentQuestionIndex):null;
    String qType=(q!=null&&q.type()!=null)?q.type().name():"MULTIPLE_CHOICE";
    send(name,"MATCH_PHASE","QUESTION_INTRO",id,String.valueOf(currentQuestionIndex),topic,qType,"400");
   }else if(currentPhase==MatchPhase.ACTIVE){
    send(name,"MATCH_PHASE","QUESTION_ACTIVE",id,String.valueOf(currentQuestionIndex),"12");
   }else if(currentPhase==MatchPhase.REVEAL){
    Model.Question q=(currentQuestionIndex<set.questions().size())?set.questions().get(currentQuestionIndex):null;
    int correctAns=(q!=null)?q.answer():-1;
    String exp=(q!=null&&q.explanation()!=null)?q.explanation():"";
    send(name,"MATCH_PHASE","QUESTION_REVEAL",id,String.valueOf(currentQuestionIndex),String.valueOf(correctAns),exp,
         String.valueOf(points[0]),String.valueOf(points[1]),
         String.valueOf(streak[0]),String.valueOf(streak[1]),
         String.valueOf(liveAnswers[0][currentQuestionIndex]),String.valueOf(liveAnswers[1][currentQuestionIndex]));
   }
   sendScores();
   sendProgress();
   if(!isSolo){
    String other=(i==0)?right:left;
    send(other,"INFO","Đối thủ "+name+" đã kết nối lại trận đấu!");
   }
  }
  /**
   * Xử lý khi người chơi tự nhấn Thoát trận giữa chừng:
   * - Người thoát bị xử THUA, trừ điểm và không nhận packet RESULT của trận đã bỏ.
   * - Đối thủ còn lại nhận thông báo THẮNG và packet RESULT cùng điểm cộng.
   */
  synchronized void forfeit(String leavingPlayer){
   int i=index(leavingPlayer);
   if(i<0)return;
   cancelTimers();
   matches.remove(leavingPlayer,this);
   if(done){
    broadcast();
    return;
   }
   done=true;
   if(isSolo){
    broadcast();
    return;
   }
   int otherIdx=1-i;
   String otherName=otherIdx==0?left:right;
   submitted[i]=true;
   correct[i]=0;
   points[i]=0;
   duration[i]=set!=null?set.duration()*1000L:60000L;
   submitted[otherIdx]=true;
   if(duration[otherIdx]==0){
    duration[otherIdx]=Math.max(1000L,TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start));
   }
   String winner=otherName;
   synchronized(store){
    Model.User uw=store.users.get(winner),ul=store.users.get(leavingPlayer);
    if(uw!=null)uw.score+=2;
    if(ul!=null)ul.score=Math.max(0,ul.score-1);
    if(db.isAvailable())db.updateMatchScores(winner,leavingPlayer);
    if(set!=null){
     Model.Result res=new Model.Result(id,left,right,topic,level,set.id(),correct[0],correct[1],duration[0],duration[1],set.duration(),winner,System.currentTimeMillis());
     store.results.add(res);
     if(db.isAvailable())db.saveResult(res);
     store.save(storeFile);
    }
   }
   if(set!=null){
    String leftStat=left+": "+correct[0]+" câu, "+duration[0]+" ms;pts="+points[0]+";streak="+maxStreak[0];
    String rightStat=right+": "+correct[1]+" câu, "+duration[1]+" ms;pts="+points[1]+";streak="+maxStreak[1];
    ArrayList<String> msg=new ArrayList<>(List.of("RESULT",winner,leftStat,rightStat,"Đối thủ đã thoát trận!"));
    for(Model.Question q:set.questions())msg.add(""+q.answer());
    for(Model.Question q:set.questions())msg.add(q.explanation()!=null?q.explanation():"");
    msg.add(id);
    send(otherName,msg.toArray(String[]::new));
   }
   send(otherName,"INFO","Đối thủ "+leavingPlayer+" đã rời trận đấu. Bạn được xử THẮNG!");
   matches.remove(otherName,this);
   broadcast();
  }
  synchronized void disconnected(String name){
   cancelTimers();
   if(!done){
    int i=index(name);
    if(i>=0){
     submitted[i]=true;
     correct[i]=0;
     points[i]=0;
     duration[i]=set!=null?set.duration()*1000L:90000L;
     if(!isSolo){
      submitted[1-i]=true;
      finish();
     }
    }
   }
   close(name+" đã rời phòng");
  }
  synchronized void close(String reason){
   cancelTimers();
   matches.remove(left,this);
   if(!isSolo)matches.remove(right,this);
   send(left,"CLOSED",reason);
   if(!isSolo)send(right,"CLOSED",reason);
   broadcast();
  }
 }
 void send(String name,String... a){Peer p=online.get(name);if(p!=null)p.send(a);}
 synchronized void broadcast(){
  StringBuilder b=new StringBuilder();
  synchronized(store){
   online.keySet().stream().sorted().forEach(n->{
    Model.User u=store.users.get(n);
    String score=u!=null?String.valueOf(u.score):"0";
    String avatar=u!=null&&u.avatarId!=null?u.avatarId:"avatar_01";
    String rankLabel=u!=null?Rank.fromScore(u.score).getDisplayName():"Sắt";
    String status=matches.containsKey(n)?"Đang bận":matchmakingQueue.containsKey(n)?"Đang tìm":"Đang rỗi";
    b.append(n).append(';').append(score).append(';').append(status).append(';').append(avatar).append(';').append(rankLabel).append('|');
   });
  }
  String s=b.toString();for(Peer p:online.values())p.send("LOBBY",s);
 }
 // Matchmaking engine: chạy trong timer thread, ghép cặp score gần nhau nhất
 void runMatchmaking(){
  if(matchmakingQueue.size()<2)return;
  List<Map.Entry<String,Peer>> candidates;
  synchronized(matchmakingQueue){candidates=new ArrayList<>(matchmakingQueue.entrySet());}
  // Lọc: chỉ lấy người thực sự online và không đang trong trận
  candidates.removeIf(e->!online.containsKey(e.getKey())||matches.containsKey(e.getKey()));
  if(candidates.size()<2)return;
  // Tìm cặp có score gần nhau nhất
  int bestI=-1,bestJ=-1;int bestDiff=Integer.MAX_VALUE;
  for(int i=0;i<candidates.size()-1;i++){
   for(int j=i+1;j<candidates.size();j++){
    String na=candidates.get(i).getKey(),nb=candidates.get(j).getKey();
    int sa,sb;
    synchronized(store){Model.User ua=store.users.get(na);Model.User ub=store.users.get(nb);sa=ua!=null?ua.score:0;sb=ub!=null?ub.score:0;}
    int diff=Math.abs(sa-sb);if(diff<bestDiff){bestDiff=diff;bestI=i;bestJ=j;}
   }
  }
  if(bestI<0)return;
  String nameA=candidates.get(bestI).getKey();
  String nameB=candidates.get(bestJ).getKey();
  // Atomically remove cả hai để tránh race condition
  Peer peerA=matchmakingQueue.remove(nameA);
  Peer peerB=matchmakingQueue.remove(nameB);
  if(peerA==null||peerB==null){if(peerA!=null)matchmakingQueue.putIfAbsent(nameA,peerA);if(peerB!=null)matchmakingQueue.putIfAbsent(nameB,peerB);return;}
  // Kiểm tra lại sau khi remove (tránh race với CHALLENGE đến cùng lúc)
  if(matches.containsKey(nameA)||matches.containsKey(nameB)){matchmakingQueue.putIfAbsent(nameA,peerA);matchmakingQueue.putIfAbsent(nameB,peerB);return;}
  // Chọn topic ngẫu nhiên có đủ >= 10 câu hỏi hợp lệ
  List<String> validTopics=bank.topics().stream().filter(t->bank.countValidQuestions(t,"Hỗn hợp")>=10).toList();
  if(validTopics.isEmpty()){
   peerA.send("MATCHMAKING_CANCELLED","Không có chủ đề nào đủ 10 câu hỏi hợp lệ để bắt đầu trận!");
   peerB.send("MATCHMAKING_CANCELLED","Không có chủ đề nào đủ 10 câu hỏi hợp lệ để bắt đầu trận!");
   broadcast();
   return;
  }
  String topic=validTopics.get(new Random().nextInt(validTopics.size()));
  String level=Model.LEVELS[new Random().nextInt(Model.LEVELS.length)];
  // Thông báo MATCH_FOUND trước khi bắt đầu trận
  int scoreA,scoreB;synchronized(store){Model.User ua=store.users.get(nameA);Model.User ub=store.users.get(nameB);scoreA=ua!=null?ua.score:0;scoreB=ub!=null?ub.score:0;}
  peerA.send("MATCH_FOUND",nameB,String.valueOf(scoreA),String.valueOf(scoreB));
  peerB.send("MATCH_FOUND",nameA,String.valueOf(scoreB),String.valueOf(scoreA));
  Match m=new Match(nameA,nameB,topic,level);
  matches.put(m.left,m);matches.put(m.right,m);
  broadcast();
  timer.schedule(()->{
   try{
    if(!online.containsKey(nameA)||!online.containsKey(nameB)){
     m.close("Người chơi đã mất kết nối trước khi vào trận");
     return;
    }
    m.start();
   }catch(Exception e){
    e.printStackTrace();
    m.close("Lỗi khi khởi tạo bộ đề: "+e.getMessage());
   }
  },1500,TimeUnit.MILLISECONDS);
 }
 void serve(Peer p){try(p.socket){while(true){String[] a=Wire.read(p.in);handle(p,a);}}catch(Exception ignored){}finally{cleanupPeer(p);}}
 void cleanupPeer(Peer p){
  String n=p.name;
  if(n!=null){
   matchmakingQueue.remove(n);
   if(online.remove(n,p)){
    Invite inv=invites.remove(n);if(inv!=null)send(inv.from(),"DECLINED",n+" đã thoát");
    invites.entrySet().removeIf(e->e.getValue().from().equals(n));
    Match m=matches.get(n);if(m!=null)m.onNetworkDrop(n);
    broadcast();
   }
  }
 }
 static String cleanField(String s){return s==null?"":s.replace(';',' ').replace('|',' ').replace('\n',' ').trim();}
 void handle(Peer p,String[] a){try{String type=a[0];
  if(type.equals("PING")){p.send("PONG",Wire.field(a,1));return;}
  if(type.equals("REGISTER")){String n=Wire.field(a,1).trim(),pass=Wire.field(a,2);if(!n.matches("[A-Za-z0-9_]{3,24}")||pass.length()<4){p.send("ERROR","Tên 3-24 ký tự chữ/số/_; mật khẩu từ 4 ký tự");return;}synchronized(store){if(store.users.containsKey(n)||(db.isAvailable()&&db.getUser(n)!=null)){p.send("ERROR","Tên đã tồn tại");return;}String hashed=Security.hashPassword(pass);if(db.isAvailable())db.registerUser(n,hashed);store.users.put(n,new Model.User(n,hashed));store.save(storeFile);}p.send("INFO","Đăng ký thành công");return;}
  if(type.equals("LOGIN")){
   if(p.name!=null)return;
   String n=Wire.field(a,1);
   boolean isReconnect="RECONNECT".equalsIgnoreCase(Wire.field(a,3));
   synchronized(store){
    Model.User u=store.users.get(n);
    if(u==null&&db.isAvailable()){u=db.getUser(n);if(u!=null)store.users.put(n,u);}
    if(u==null||!Security.verifyPassword(Wire.field(a,2),u.password)){p.send("ERROR","Sai tài khoản hoặc mật khẩu");return;}
    if(Security.isLegacy(u.password)){u.password=Security.hashPassword(Wire.field(a,2));if(db.isAvailable())db.updatePassword(n,u.password);store.save(storeFile);}
    Peer existing=online.putIfAbsent(n,p);
    if(existing!=null){
     if(isReconnect||existing.socket.isClosed()||matches.containsKey(n)){
      online.put(n,p);
      try{existing.socket.close();}catch(Exception ignored){}
     }else{
      p.send("ERROR","Tài khoản đang đăng nhập");
      return;
     }
    }
    p.name=n;
   }
   String avatarId,fullName,email,bio,role;int score;
   synchronized(store){
    Model.User u=store.users.get(n);
    avatarId=u!=null&&u.avatarId!=null?u.avatarId:"avatar_01";
    score=u!=null?u.score:0;
    fullName=u!=null&&u.fullName!=null?u.fullName:"";
    email=u!=null&&u.email!=null?u.email:"";
    bio=u!=null&&u.bio!=null?u.bio:"";
    role=(u!=null&&u.isAdmin())?"ADMIN":"PLAYER";
   }
   p.send("WELCOME",n,String.join("|",bank.topics()),avatarId,String.valueOf(score),fullName,email,bio,role);
   broadcast();
   Match activeMatch=matches.get(n);
   if(activeMatch!=null&&!activeMatch.done){
    activeMatch.resumePlayer(n);
   }
   return;
  }
  if(p.name==null){p.send("ERROR","Cần đăng nhập");return;}
  switch(type){
   case "CHALLENGE"->{
    String to=Wire.field(a,1),topic=Wire.field(a,2),level=Wire.field(a,3);
    boolean validTopic=bank.topics().contains(topic)||topic.equalsIgnoreCase("Tổng hợp");
    boolean validLevel=Arrays.asList(Model.LEVELS).contains(level)||level.contains("Tiêu chuẩn")||level.equalsIgnoreCase("Hỗn hợp");
    if(to.equals(p.name)||!online.containsKey(to)||matches.containsKey(to)||matches.containsKey(p.name)||invites.containsKey(to)||matchmakingQueue.containsKey(to)||matchmakingQueue.containsKey(p.name)||!validTopic||!validLevel){
     p.send("ERROR","Không thể gửi lời mời");
     return;
    }
    int validCnt=bank.countValidQuestions(topic,level);
    if(validCnt<10){
     p.send("ERROR","Chủ đề '"+topic+"' hiện chỉ có "+validCnt+"/10 câu hỏi hợp lệ. Không thể tạo trận đấu!");
     return;
    }
    Model.SetData sample=bank.generateMatchSet(topic,level,new HashSet<>(),Model.ExamConfig.DEFAULT);
    if(sample==null){p.send("ERROR","Chủ đề '"+topic+"' không đủ 10 câu hỏi hợp lệ!");return;}
    invites.put(to,new Invite(p.name,to,topic,level));
    send(to,"INVITE",p.name,topic,level,"10",""+sample.duration());
    p.send("INFO","Đã gửi lời mời đến "+to);
    timer.schedule(()->{Invite inv=invites.get(to);if(inv!=null&&inv.from().equals(p.name)&&invites.remove(to,inv))send(p.name,"DECLINED","Lời mời hết hạn");},30,TimeUnit.SECONDS);
   }
   case "CANCEL_CHALLENGE"->{String target=Wire.field(a,1);Invite inv=invites.get(target);if(inv!=null&&inv.from().equals(p.name)){invites.remove(target);p.send("INFO","Đã hủy lời mời");send(target,"DECLINED","Người thách đấu đã hủy lời mời");}}
   case "ACCEPT","REJECT"->{
    Invite inv=invites.remove(p.name);
    if(inv==null||!inv.from().equals(Wire.field(a,1))){p.send("ERROR","Lời mời không còn hiệu lực");return;}
    if(type.equals("REJECT")||!online.containsKey(inv.from())||matches.containsKey(inv.from())||matches.containsKey(p.name)){
     send(inv.from(),"DECLINED",p.name+" từ chối hoặc không còn sẵn sàng");
     return;
    }
    int validCnt=bank.countValidQuestions(inv.topic(),inv.level());
    if(validCnt<10){
     p.send("ERROR","Chủ đề '"+inv.topic()+"' hiện chỉ có "+validCnt+"/10 câu hỏi hợp lệ. Không thể bắt đầu trận!");
     send(inv.from(),"DECLINED","Chủ đề '"+inv.topic()+"' không đủ 10 câu hỏi hợp lệ");
     return;
    }
    Match m=new Match(inv.from(),p.name,inv.topic(),inv.level());
    matches.put(m.left,m);matches.put(m.right,m);
    m.start();
   }
   case "SOLO_MATCH"->{
    if(matches.containsKey(p.name)){p.send("ERROR","Bạn đang trong trận đấu");return;}
    matchmakingQueue.remove(p.name);
    String topic=Wire.field(a,1).trim();
    String level=Wire.field(a,2).trim();
    if(topic.isEmpty())topic="Tổng hợp";
    if(level.isEmpty())level="Hỗn hợp";
    int validCnt=bank.countValidQuestions(topic,level);
    if(validCnt<10){
     p.send("ERROR","Chủ đề '"+topic+"' hiện chỉ có "+validCnt+"/10 câu hỏi hợp lệ. Không thể bắt đầu luyện tập!");
     return;
    }
    Match m=new Match(p.name,"Bot Luyện Tập",topic,level);
    matches.put(p.name,m);
    m.start();
   }
   case "READY","MATCH_READY"->{Match m=matches.get(p.name);if(m!=null)m.onPlayerReady(p.name);}
   case "SKIP_REVEAL"->{Match m=matches.get(p.name);if(m!=null){try{int qIdx=Integer.parseInt(Wire.field(a,2));m.onPlayerSkipReveal(p.name,qIdx);}catch(Exception ignored){}}}
   case "ANSWER"->{Match m=matches.get(p.name);if(m!=null&&m.id.equals(Wire.field(a,1))){try{int qIdx=Integer.parseInt(Wire.field(a,2));int ch=Integer.parseInt(Wire.field(a,3));m.answer(p.name,qIdx,ch);}catch(Exception ignored){}}}
   case "PROGRESS"->{Match m=matches.get(p.name);if(m!=null&&m.id.equals(Wire.field(a,1))){try{int qIdx=Integer.parseInt(Wire.field(a,2));m.progress(p.name,qIdx);}catch(Exception ignored){}}}
   case "SUBMIT"->{Match m=matches.get(p.name);if(m!=null&&m.id.equals(Wire.field(a,1)))m.submit(p.name,a);}
   case "REMATCH"->{Match m=matches.get(p.name);if(m!=null)m.vote(p.name,Wire.field(a,1).equals("YES"));}
   case "LEAVE"->{Match m=matches.get(p.name);if(m!=null)m.forfeit(p.name);}
   case "EMOTE"->{Match m=matches.get(p.name);if(m!=null&&!m.isSolo){String to=p.name.equals(m.left)?m.right:m.left;send(to,"EMOTE",p.name,Wire.field(a,1));}}
   case "QUICK_MATCH"->{
    if(matches.containsKey(p.name)){p.send("ERROR","Bạn đang trong trận đấu");return;}
    if(invites.containsKey(p.name)){p.send("ERROR","Bạn đang có lời mời đến");return;}
    if(matchmakingQueue.putIfAbsent(p.name,p)!=null){p.send("MATCHMAKING","Bạn đã trong hàng chờ");return;}
    p.send("MATCHMAKING","Đã vào hàng chờ ghép trận");
    broadcast();
    timer.schedule(()->{
     Peer inQueue=matchmakingQueue.remove(p.name);
     if(inQueue!=null&&inQueue==p){
      p.send("MATCHMAKING_CANCELLED","Không tìm thấy đối thủ phù hợp sau 30 giây");
      broadcast();
     }
    },30,TimeUnit.SECONDS);
   }
   case "CANCEL_MATCHMAKING"->{
    Peer removed=matchmakingQueue.remove(p.name);
    if(removed!=null){p.send("MATCHMAKING_CANCELLED","Đã hủy tìm trận");broadcast();}
    else p.send("INFO","Bạn không trong hàng chờ");
   }
   case "UPDATE_AVATAR"->{
    String newAvatar=Wire.field(a,1);
    if(newAvatar==null||!newAvatar.matches("avatar_(0[1-9]|1[0-2])")){p.send("ERROR","Avatar không hợp lệ");return;}
    synchronized(store){Model.User u=store.users.get(p.name);if(u!=null){u.avatarId=newAvatar;store.save(storeFile);}}
    if(db.isAvailable())db.updateAvatar(p.name,newAvatar);
    p.send("AVATAR_UPDATED",newAvatar);
    broadcast();
   }
   case "USER_LIST"->sendUserList(p);
   case "ADD_USER"->{
    if(!isAdmin(p)){p.send("ERROR","Từ chối truy cập: Chỉ Quản trị viên (Admin) mới có quyền thêm người chơi!");return;}
    String uName=Wire.field(a,1).trim();
    String uPass=Wire.field(a,2);
    int uScore=0;try{uScore=Math.max(0,Integer.parseInt(Wire.field(a,3).trim()));}catch(Exception ignored){}
    String uAvatar=Wire.field(a,4).trim();
    if(!uAvatar.matches("avatar_(0[1-9]|1[0-2])"))uAvatar="avatar_01";
    String uFull=cleanField(Wire.field(a,5));
    String uEmail=cleanField(Wire.field(a,6));
    String uBio=cleanField(Wire.field(a,7));
    String uRole="ADMIN".equalsIgnoreCase(Wire.field(a,8).trim())?"ADMIN":("admin".equalsIgnoreCase(uName)?"ADMIN":"PLAYER");
    if(!uName.matches("[A-Za-z0-9_]{3,24}")){p.send("ERROR","Tên đăng nhập phải từ 3-24 ký tự (chữ, số, _)");return;}
    if(uPass.length()<4){p.send("ERROR","Mật khẩu phải từ 4 ký tự trở lên");return;}
    synchronized(store){
     if(store.users.containsKey(uName)||(db.isAvailable()&&db.getUser(uName)!=null)){
      p.send("ERROR","Tên người chơi '"+uName+"' đã tồn tại!");
      return;
     }
     String hashed=Security.hashPassword(uPass);
     Model.User newUser=new Model.User(uName,hashed,uScore,uAvatar,uFull,uEmail,uBio,uRole);
     store.users.put(uName,newUser);
     if(db.isAvailable())db.createUser(uName,hashed,uScore,uAvatar,uFull,uEmail,uBio,uRole);
     store.save(storeFile);
    }
    p.send("USER_SAVED","Đã thêm người chơi '"+uName+"' thành công!");
    sendUserList(p);
    broadcast();
   }
   case "UPDATE_USER"->{
    String uName=Wire.field(a,1).trim();
    boolean callerIsAdmin=isAdmin(p);
    if(!callerIsAdmin&&!uName.equals(p.name)){
     p.send("ERROR","Từ chối truy cập: Bạn không có quyền chỉnh sửa tài khoản của người chơi khác!");
     return;
    }
    String newPass=Wire.field(a,2);
    int requestedScore=0;try{requestedScore=Math.max(0,Integer.parseInt(Wire.field(a,3).trim()));}catch(Exception ignored){}
    String uAvatar=Wire.field(a,4).trim();
    if(!uAvatar.matches("avatar_(0[1-9]|1[0-2])"))uAvatar="avatar_01";
    String uFull=cleanField(Wire.field(a,5));
    String uEmail=cleanField(Wire.field(a,6));
    String uBio=cleanField(Wire.field(a,7));
    String requestedRole=Wire.field(a,8).trim();
    if(!newPass.isEmpty()&&newPass.length()<4){p.send("ERROR","Mật khẩu mới phải từ 4 ký tự trở lên (hoặc để trống nếu không đổi)");return;}
    String hashed=newPass.isEmpty()?null:Security.hashPassword(newPass);
    int finalScore;
    String finalRole;
    synchronized(store){
     Model.User u=store.users.get(uName);
     if(u==null){p.send("ERROR","Không tìm thấy người chơi '"+uName+"'");return;}
     if(hashed!=null)u.password=hashed;
     finalScore=callerIsAdmin?requestedScore:u.score;
     finalRole=(callerIsAdmin&&!requestedRole.isEmpty())?(requestedRole.equalsIgnoreCase("ADMIN")?"ADMIN":"PLAYER"):(u.isAdmin()?"ADMIN":"PLAYER");
     if("admin".equalsIgnoreCase(uName))finalRole="ADMIN";
     u.score=finalScore;
     u.avatarId=uAvatar;
     u.fullName=uFull;
     u.email=uEmail;
     u.bio=uBio;
     u.role=finalRole;
     if(db.isAvailable())db.updateUser(uName,hashed,finalScore,uAvatar,uFull,uEmail,uBio,finalRole);
     store.save(storeFile);
    }
    p.send("USER_SAVED",callerIsAdmin?"Đã cập nhật thông tin người chơi '"+uName+"'!":"Đã cập nhật hồ sơ cá nhân thành công!");
    Peer targetPeer=online.get(uName);
    if(targetPeer!=null){
     targetPeer.send("PROFILE_SYNC",uName,String.valueOf(finalScore),uAvatar,uFull,uEmail,uBio,finalRole);
    }
    sendUserList(p);
    broadcast();
   }
   case "DELETE_USER"->{
    if(!isAdmin(p)){
     p.send("ERROR","Từ chối truy cập: Chỉ Quản trị viên (Admin) mới có quyền xóa tài khoản người chơi!");
     return;
    }
    String uName=Wire.field(a,1).trim();
    if("admin".equalsIgnoreCase(uName)){
     p.send("ERROR","Không thể xóa tài khoản Quản trị viên gốc (admin)!");
     return;
    }
    if(matches.containsKey(uName)){p.send("ERROR","Không thể xóa người chơi đang trong trận đấu!");return;}
    synchronized(store){
     if(!store.users.containsKey(uName)){p.send("ERROR","Không tìm thấy người chơi '"+uName+"'");return;}
     store.users.remove(uName);
     if(db.isAvailable())db.deleteUser(uName);
     store.save(storeFile);
    }
    boolean isSelf=uName.equals(p.name);
    Peer targetPeer=online.get(uName);
    if(targetPeer!=null){
     targetPeer.send("ACCOUNT_DELETED","Tài khoản '"+uName+"' đã bị xóa khỏi hệ thống.");
    }
    if(!isSelf){
     p.send("USER_SAVED","Đã xóa người chơi '"+uName+"' thành công!");
     sendUserList(p);
    }
    broadcast();
   }
   case "QUESTION_LIST"->{
    if(!isAdmin(p)){p.send("ERROR","Từ chối truy cập: Chỉ Quản trị viên (Admin) mới có quyền quản lý ngân hàng câu hỏi!");return;}
    sendQuestionList(p);
   }
   case "ADD_QUESTION"->{
    if(!isAdmin(p)){p.send("ERROR","Từ chối truy cập: Chỉ Quản trị viên (Admin) mới có quyền thêm câu hỏi!");return;}
    String topic=cleanQField(Wire.field(a,1));
    String id=bank.nextQuestionId(topic);
    Model.Question q=parseQuestionFromWire(id,a,1);
    String err=Bank.validateForAdmin(q);
    if(err!=null){p.send("ERROR","Câu hỏi không hợp lệ: "+err);return;}
    if(!bank.addQuestion(q)){p.send("ERROR","Không thể thêm câu hỏi vào ngân hàng đề!");return;}
    p.send("QUESTION_SAVED","Đã thêm câu hỏi '"+id+"' vào chủ đề '"+topic+"' thành công!");
    sendQuestionList(p);
    broadcastTopics();
   }
   case "UPDATE_QUESTION"->{
    if(!isAdmin(p)){p.send("ERROR","Từ chối truy cập: Chỉ Quản trị viên (Admin) mới có quyền sửa câu hỏi!");return;}
    String id=Wire.field(a,1).trim();
    if(id.isEmpty()){p.send("ERROR","Thiếu mã câu hỏi cần sửa");return;}
    Model.Question q=parseQuestionFromWire(id,a,2);
    String err=Bank.validateForAdmin(q);
    if(err!=null){p.send("ERROR","Câu hỏi không hợp lệ: "+err);return;}
    if(!bank.updateQuestion(id,q)){p.send("ERROR","Không tìm thấy hoặc không thể cập nhật câu hỏi '"+id+"'!");return;}
    p.send("QUESTION_SAVED","Đã cập nhật câu hỏi '"+id+"' thành công!");
    sendQuestionList(p);
    broadcastTopics();
   }
   case "DELETE_QUESTION"->{
    if(!isAdmin(p)){p.send("ERROR","Từ chối truy cập: Chỉ Quản trị viên (Admin) mới có quyền xóa câu hỏi!");return;}
    String id=Wire.field(a,1).trim();
    if(!bank.deleteQuestion(id)){p.send("ERROR","Không tìm thấy câu hỏi '"+id+"' để xóa!");return;}
    p.send("QUESTION_SAVED","Đã xóa câu hỏi '"+id+"' khỏi ngân hàng đề!");
    sendQuestionList(p);
    broadcastTopics();
   }
   case "LEADERBOARD"->leaderboard(p);
   case "HISTORY"->history(p);
   default->p.send("ERROR","Lệnh không hợp lệ");}
 }catch(Exception e){p.send("ERROR",e.getMessage()==null?"Lỗi xử lý":e.getMessage());}}
 void broadcastTopics(){
  String tStr=String.join("|",bank.topics());
  for(Peer peer:online.values())peer.send("TOPICS",tStr);
 }
 static String cleanQField(String s){return s==null?"":s.replace('|',' ').replace('\t',' ').replace('\r',' ').replace('\n',' ').trim();}
 /** Trường từ client: topic, level, type, text, choicesRaw, answerRaw, explanation, blanksRaw, fillMapRaw */
 Model.Question parseQuestionFromWire(String id,String[] a,int offset){
  try{
   String[] f=new String[10];
   f[0]=id;
   for(int k=0;k<9;k++)f[k+1]=cleanQField(Wire.field(a,offset+k));
   return Bank.parseFields(f);
  }catch(Exception e){
   return null;
  }
 }
 /** Mỗi câu hỏi là 1 trường (tab-separated) để tránh giới hạn 64KB của writeUTF. */
 void sendQuestionList(Peer p){
  List<Model.Question> qs=bank.getPool();
  ArrayList<String> msg=new ArrayList<>(qs.size()+1);
  msg.add("QUESTION_LIST");
  for(Model.Question q:qs){
   if(msg.size()>=999)break;
   String[] raw=Bank.rawFields(q);
   msg.add(String.join("\t",
    cleanQField(q.id()),cleanQField(q.topic()),cleanQField(q.level()),
    q.type()!=null?q.type().name():"MULTIPLE_CHOICE",
    cleanQField(q.text()),cleanQField(raw[0]),cleanQField(raw[1]),cleanQField(q.explanation()),
    cleanQField(raw[2]),cleanQField(raw[3])));
  }
  p.send(msg.toArray(String[]::new));
 }
 void sendUserList(Peer p){
  StringBuilder b=new StringBuilder();
  boolean callerIsAdmin=isAdmin(p);
  synchronized(store){
   List<Model.User> users;
   if(callerIsAdmin){
    users=new ArrayList<>(store.users.values());
    users.sort(Comparator.comparingInt((Model.User u)->u.score).reversed().thenComparing(u->u.name));
   }else{
    Model.User self=store.users.get(p.name);
    users=self!=null?List.of(self):List.of();
   }
   for(Model.User u:users){
    String rankLabel=Rank.fromScore(u.score).getDisplayName();
    String status=matches.containsKey(u.name)?"Đang đấu":online.containsKey(u.name)?"Trực tuyến":"Ngoại tuyến";
    String avatar=u.avatarId!=null&&!u.avatarId.isBlank()?u.avatarId:"avatar_01";
    String roleStr=u.isAdmin()?"ADMIN":"PLAYER";
    b.append(cleanField(u.name)).append(';')
     .append(u.score).append(';')
     .append(cleanField(avatar)).append(';')
     .append(cleanField(rankLabel)).append(';')
     .append(cleanField(status)).append(';')
     .append(cleanField(u.fullName)).append(';')
     .append(cleanField(u.email)).append(';')
     .append(cleanField(u.bio)).append(';')
     .append(roleStr).append('|');
   }
  }
  p.send("USER_LIST",b.toString());
 }
 void leaderboard(Peer p){StringBuilder b=new StringBuilder();synchronized(store){List<Model.User> users=new ArrayList<>(store.users.values());Map<String,Double> opponent=new HashMap<>(),speed=new HashMap<>();Map<String,Integer> totalMatches=new HashMap<>();Map<String,Double> winRate=new HashMap<>();for(Model.User u:users){List<Model.Result> rs=store.results.stream().filter(r->r.left().equals(u.name)||r.right().equals(u.name)).toList();int matchesCount=rs.size();totalMatches.put(u.name,matchesCount);long wins=rs.stream().filter(r->r.winner().equals(u.name)).count();winRate.put(u.name,matchesCount>0?(wins*100.0/matchesCount):0.0);opponent.put(u.name,rs.stream().mapToInt(r->{Model.User other=store.users.get(r.left().equals(u.name)?r.right():r.left());return other==null?0:other.score;}).average().orElse(0));speed.put(u.name,rs.stream().filter(r->r.winner().equals(u.name)).mapToDouble(r->(r.left().equals(u.name)?r.leftMs():r.rightMs())/(r.duration()*1000.0)).average().orElse(Double.POSITIVE_INFINITY));}
  users.sort(Comparator.comparingInt((Model.User u)->u.score).reversed().thenComparing(Comparator.comparingDouble((Model.User u)->opponent.get(u.name)).reversed()).thenComparingDouble(u->speed.get(u.name)).thenComparing(u->u.name));
  for(Model.User u:users){String rankLabel=Rank.fromScore(u.score).getDisplayName();b.append(u.name).append(';').append(u.score).append(';').append(String.format(Locale.ROOT,"%.2f",opponent.get(u.name))).append(';').append(Double.isInfinite(speed.get(u.name))?"—":String.format(Locale.ROOT,"%.3f",speed.get(u.name))).append(';').append(totalMatches.get(u.name)).append(';').append(String.format(Locale.ROOT,"%.1f%%",winRate.get(u.name))).append(';').append(rankLabel).append('|');}}p.send("LEADERBOARD",b.toString());}
 void history(Peer p){StringBuilder b=new StringBuilder();synchronized(store){List<Model.Result> rs=db.isAvailable()?db.getUserHistory(p.name):store.results.stream().filter(r->r.left().equals(p.name)||r.right().equals(p.name)).toList();for(Model.Result r:rs)b.append(r.topic()).append(" / ").append(r.level()).append(" / ").append(r.setId()).append(": ").append(r.left()).append(" ").append(r.leftCorrect()).append("–").append(r.rightCorrect()).append(" ").append(r.right()).append(" → ").append(r.winner()).append(" | ").append(r.duration()).append(" | ").append(r.timestamp()).append('\n');}p.send("HISTORY",b.toString());}
 public static void main(String[] args)throws Exception{int port=args.length>0?Integer.parseInt(args[0]):5050;Path folder=Path.of(args.length>1?args[1]:"data");Server app=new Server(folder);try(ServerSocket socket=new ServerSocket(port)){System.out.println("Quiz server: port "+port+", data "+folder.toAbsolutePath());try{String ip=InetAddress.getLocalHost().getHostAddress();System.out.println("Dia chi IP may chu (de cac may khac ket noi): "+ip+":"+port);}catch(Exception ignored){}while(true){Peer p=new Peer(socket.accept());app.workers.submit(()->app.serve(p));}}}
}
