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
 Server(Path folder)throws IOException{Files.createDirectories(folder);db=new DB(folder);bank=new Bank(folder.resolve("questions.txt"),db);storeFile=folder.resolve("users.dat").toFile();store=Model.Store.read(storeFile);if(db.isAvailable()){store.users.putAll(db.getAllUsers());store.results.clear();store.results.addAll(db.getAllResults());}
 // Matchmaking scheduler: kiểm tra và ghép cặp mỗi 2 giây
 timer.scheduleAtFixedRate(this::runMatchmaking,2,2,TimeUnit.SECONDS);}
 static final class Peer {final Socket socket;final DataInputStream in;final DataOutputStream out;volatile String name;Peer(Socket s)throws IOException{socket=s;in=new DataInputStream(s.getInputStream());out=new DataOutputStream(s.getOutputStream());}void send(String... a){try{Wire.send(out,a);}catch(IOException e){try{socket.close();}catch(IOException ignored){}}}}
 final class Match {
  final String id=UUID.randomUUID().toString(),left,right,topic,level;final Set<String> used=new HashSet<>();Model.SetData set;long start,deadline;boolean done;int[] correct=new int[2];long[] duration=new long[2];boolean[] submitted=new boolean[2],rematch=new boolean[2];final int[][] liveAnswers=new int[2][10];
  Match(String a,String b,String t,String l){left=a;right=b;topic=t;level=l;}
  int index(String name){return name.equals(left)?0:name.equals(right)?1:-1;}
  synchronized void start(){
    try{
     set=bank.pick(topic,level,used);if(set==null){close("Không tìm thấy bộ đề");return;}used.add(set.id());Arrays.fill(submitted,false);Arrays.fill(rematch,false);Arrays.fill(correct,0);Arrays.fill(duration,0);Arrays.fill(liveAnswers[0],-1);Arrays.fill(liveAnswers[1],-1);done=false;start=System.nanoTime();deadline=start+TimeUnit.SECONDS.toNanos(set.duration()+15);
     ArrayList<String> msg=new ArrayList<>(List.of("MATCH",id,left,right,topic,level,set.id(),""+set.duration()));for(Model.Question q:set.questions()){msg.add(q.type()!=null?q.type().name():"MULTIPLE_CHOICE");msg.add(q.text());String[] ch=q.choices();msg.add(""+(ch!=null?ch.length:0));if(ch!=null)for(String c:ch)msg.add(c);msg.add(""+q.answer());String[] blanks=q.blanks();if(blanks!=null&&blanks.length>0){msg.add(""+blanks.length);for(String b:blanks)msg.add(b);}else{msg.add("0");}java.util.Map<String,String> fm=q.fillMap();if(fm!=null&&!fm.isEmpty()){msg.add(""+fm.size());for(var e:fm.entrySet()){msg.add(e.getKey()+"="+e.getValue());}}else{msg.add("0");}}
     send(left,msg.toArray(String[]::new));send(right,msg.toArray(String[]::new));send(left,"SCORE","0","0");send(right,"SCORE","0","0");timer.schedule(()->{synchronized(Match.this){finishExpired();}},set.duration()+15,TimeUnit.SECONDS);broadcast();
    }catch(Exception e){
     e.printStackTrace();
     close("Lỗi khi bắt đầu trận: "+e.getMessage());
    }
  }
  synchronized void answer(String name,int qIndex,int choice){int i=index(name);if(i<0||done||submitted[i])return;if(qIndex>=0&&qIndex<10&&set!=null){liveAnswers[i][qIndex]=choice;int p0=0,p1=0;for(int k=0;k<10;k++){if(liveAnswers[0][k]!=-1&&liveAnswers[0][k]==set.questions().get(k).answer())p0++;if(liveAnswers[1][k]!=-1&&liveAnswers[1][k]==set.questions().get(k).answer())p1++;}correct[0]=p0;correct[1]=p1;send(left,"SCORE",String.valueOf(p0),String.valueOf(p1));send(right,"SCORE",String.valueOf(p0),String.valueOf(p1));}}
  synchronized void submit(String name,String[] a){int i=index(name);if(i<0||done||submitted[i])return;long now=System.nanoTime();if(now>=deadline){finishExpired();return;}if(a.length!=12){send(name,"ERROR","Phải gửi đúng 10 đáp án");return;}int points=0;for(int k=0;k<10;k++){try{int v=Integer.parseInt(a[k+2]);if(v!= -1 &&(v<0||v>15)){send(name,"ERROR","Đáp án không hợp lệ");return;}if(v==set.questions().get(k).answer())points++;}catch(NumberFormatException e){send(name,"ERROR","Đáp án không hợp lệ");return;}}
   correct[i]=points;duration[i]=TimeUnit.NANOSECONDS.toMillis(now-start);submitted[i]=true;send(name,"SUBMITTED","Đã nhận bài");send(left,"SCORE",String.valueOf(correct[0]),String.valueOf(correct[1]));send(right,"SCORE",String.valueOf(correct[0]),String.valueOf(correct[1]));if(submitted[0]&&submitted[1])finish();}
  void finishExpired(){if(done)return;for(int i=0;i<2;i++)if(!submitted[i]){submitted[i]=true;duration[i]=set.duration()*1000L;correct[i]=0;}finish();}
  void finish(){if(done)return;done=true;String winner="HÒA";if(correct[0]!=correct[1])winner=correct[0]>correct[1]?left:right;else if(Math.abs(duration[0]-duration[1])>1000)winner=duration[0]<duration[1]?left:right;
   synchronized(store){if(!winner.equals("HÒA")){String loser=winner.equals(left)?right:left;store.users.get(winner).score+=2;store.users.get(loser).score=Math.max(0,store.users.get(loser).score-1);if(db.isAvailable())db.updateMatchScores(winner,loser);}
   Model.Result res=new Model.Result(id,left,right,topic,level,set.id(),correct[0],correct[1],duration[0],duration[1],set.duration(),winner,System.currentTimeMillis());
   store.results.add(res);if(db.isAvailable())db.saveResult(res);store.save(storeFile);}
   ArrayList<String> msg=new ArrayList<>(List.of("RESULT",winner,left+": "+correct[0]+" câu, "+duration[0]+" ms",right+": "+correct[1]+" câu, "+duration[1]+" ms","Chơi tiếp? Chọn Có hoặc Không"));for(Model.Question q:set.questions())msg.add(""+q.answer());for(Model.Question q:set.questions())msg.add(q.explanation()!=null?q.explanation():"");String[] resPacket=msg.toArray(String[]::new);send(left,resPacket);send(right,resPacket);broadcast();}
  synchronized void vote(String name,boolean yes){int i=index(name);if(i<0||!done)return;if(!yes){close(name+" không chơi tiếp");return;}rematch[i]=true;send(name,"INFO","Đang chờ đối thủ đồng ý tái đấu");if(rematch[0]&&rematch[1])start();}
  synchronized void disconnected(String name){if(!done){int i=index(name);if(i>=0){submitted[i]=true;correct[i]=0;duration[i]=set.duration()*1000L;if(submitted[0]&&submitted[1])finish();}}close(name+" đã rời phòng");}
  void close(String reason){matches.remove(left,this);matches.remove(right,this);send(left,"CLOSED",reason);send(right,"CLOSED",reason);broadcast();}
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
  // Tạo trận với topic và level mặc định "Ngẫu nhiên"
  // Chọn topic ngẫu nhiên từ danh sách hiện có
  List<String> topics=bank.topics();
  if(topics.isEmpty()){matchmakingQueue.putIfAbsent(nameA,peerA);matchmakingQueue.putIfAbsent(nameB,peerB);return;}
  String topic=topics.get(new Random().nextInt(topics.size()));
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
   // Xóa khỏi matchmaking queue trước
   matchmakingQueue.remove(n);
   if(online.remove(n,p)){
    Invite inv=invites.remove(n);if(inv!=null)send(inv.from(),"DECLINED",n+" đã thoát");
    invites.entrySet().removeIf(e->e.getValue().from().equals(n));
    Match m=matches.get(n);if(m!=null)m.disconnected(n);
    broadcast();
   }
  }
 }
 void handle(Peer p,String[] a){try{String type=a[0];
  if(type.equals("REGISTER")){String n=Wire.field(a,1).trim(),pass=Wire.field(a,2);if(!n.matches("[A-Za-z0-9_]{3,24}")||pass.length()<4){p.send("ERROR","Tên 3-24 ký tự chữ/số/_; mật khẩu từ 4 ký tự");return;}synchronized(store){if(store.users.containsKey(n)||(db.isAvailable()&&db.getUser(n)!=null)){p.send("ERROR","Tên đã tồn tại");return;}String hashed=Security.hashPassword(pass);if(db.isAvailable())db.registerUser(n,hashed);store.users.put(n,new Model.User(n,hashed));store.save(storeFile);}p.send("INFO","Đăng ký thành công");return;}
  if(type.equals("LOGIN")){if(p.name!=null)return;String n=Wire.field(a,1);synchronized(store){Model.User u=store.users.get(n);if(u==null&&db.isAvailable()){u=db.getUser(n);if(u!=null)store.users.put(n,u);}if(u==null||!Security.verifyPassword(Wire.field(a,2),u.password)){p.send("ERROR","Sai tài khoản hoặc mật khẩu");return;}if(Security.isLegacy(u.password)){u.password=Security.hashPassword(Wire.field(a,2));if(db.isAvailable())db.updatePassword(n,u.password);store.save(storeFile);}if(online.putIfAbsent(n,p)!=null){p.send("ERROR","Tài khoản đang đăng nhập");return;}p.name=n;}
   String avatarId;int score;synchronized(store){Model.User u=store.users.get(n);avatarId=u!=null&&u.avatarId!=null?u.avatarId:"avatar_01";score=u!=null?u.score:0;}
   p.send("WELCOME",n,String.join("|",bank.topics()),avatarId,String.valueOf(score));broadcast();return;}
  if(p.name==null){p.send("ERROR","Cần đăng nhập");return;}
  switch(type){
   case "CHALLENGE"->{String to=Wire.field(a,1),topic=Wire.field(a,2),level=Wire.field(a,3);boolean validTopic=bank.topics().contains(topic)||topic.equalsIgnoreCase("Tổng hợp");boolean validLevel=Arrays.asList(Model.LEVELS).contains(level)||level.contains("Tiêu chuẩn")||level.equalsIgnoreCase("Hỗn hợp");if(to.equals(p.name)||!online.containsKey(to)||matches.containsKey(to)||matches.containsKey(p.name)||invites.containsKey(to)||matchmakingQueue.containsKey(to)||matchmakingQueue.containsKey(p.name)||!validTopic||!validLevel){p.send("ERROR","Không thể gửi lời mời");return;}Model.SetData sample=bank.generateMatchSet(topic,level,new HashSet<>(),Model.ExamConfig.DEFAULT);if(sample==null){p.send("ERROR","Chưa có bộ đề");return;}invites.put(to,new Invite(p.name,to,topic,level));send(to,"INVITE",p.name,topic,level,"10",""+sample.duration());p.send("INFO","Đã gửi lời mời đến "+to);timer.schedule(()->{Invite inv=invites.get(to);if(inv!=null&&inv.from().equals(p.name)&&invites.remove(to,inv))send(p.name,"DECLINED","Lời mời hết hạn");},30,TimeUnit.SECONDS);}
   case "CANCEL_CHALLENGE"->{String target=Wire.field(a,1);Invite inv=invites.get(target);if(inv!=null&&inv.from().equals(p.name)){invites.remove(target);p.send("INFO","Đã hủy lời mời");send(target,"DECLINED","Người thách đấu đã hủy lời mời");}}
   case "ACCEPT","REJECT"->{Invite inv=invites.remove(p.name);if(inv==null||!inv.from().equals(Wire.field(a,1))){p.send("ERROR","Lời mời không còn hiệu lực");return;}if(type.equals("REJECT")||!online.containsKey(inv.from())||matches.containsKey(inv.from())||matches.containsKey(p.name)){send(inv.from(),"DECLINED",p.name+" từ chối hoặc không còn sẵn sàng");return;}Match m=new Match(inv.from(),p.name,inv.topic(),inv.level());matches.put(m.left,m);matches.put(m.right,m);m.start();}
   case "ANSWER"->{Match m=matches.get(p.name);if(m!=null&&m.id.equals(Wire.field(a,1))){try{int qIdx=Integer.parseInt(Wire.field(a,2));int ch=Integer.parseInt(Wire.field(a,3));m.answer(p.name,qIdx,ch);}catch(Exception ignored){}}}
   case "SUBMIT"->{Match m=matches.get(p.name);if(m!=null&&m.id.equals(Wire.field(a,1)))m.submit(p.name,a);}
   case "REMATCH"->{Match m=matches.get(p.name);if(m!=null)m.vote(p.name,Wire.field(a,1).equals("YES"));}
   case "LEAVE"->{Match m=matches.get(p.name);if(m!=null)m.disconnected(p.name);}
   case "EMOTE"->{Match m=matches.get(p.name);if(m!=null){String to=p.name.equals(m.left)?m.right:m.left;send(to,"EMOTE",p.name,Wire.field(a,1));}}
   case "QUICK_MATCH"->{
    if(matches.containsKey(p.name)){p.send("ERROR","Bạn đang trong trận đấu");return;}
    if(invites.containsKey(p.name)){p.send("ERROR","Bạn đang có lời mời đến");return;}
    // Không cho phép duplicate trong queue
    if(matchmakingQueue.putIfAbsent(p.name,p)!=null){p.send("MATCHMAKING","Bạn đã trong hàng chờ");return;}
    p.send("MATCHMAKING","Đã vào hàng chờ ghép trận");
    broadcast();
    // Timeout 30 giây
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
    // Validate avatar ID
    if(newAvatar==null||!newAvatar.matches("avatar_(0[1-9]|1[0-2])")){p.send("ERROR","Avatar không hợp lệ");return;}
    synchronized(store){Model.User u=store.users.get(p.name);if(u!=null){u.avatarId=newAvatar;store.save(storeFile);}}
    if(db.isAvailable())db.updateAvatar(p.name,newAvatar);
    p.send("AVATAR_UPDATED",newAvatar);
    broadcast();
   }
   case "LEADERBOARD"->leaderboard(p);
   case "HISTORY"->history(p);
   default->p.send("ERROR","Lệnh không hợp lệ");}
 }catch(Exception e){p.send("ERROR",e.getMessage()==null?"Lỗi xử lý":e.getMessage());}}
 void leaderboard(Peer p){StringBuilder b=new StringBuilder();synchronized(store){List<Model.User> users=new ArrayList<>(store.users.values());Map<String,Double> opponent=new HashMap<>(),speed=new HashMap<>();Map<String,Integer> totalMatches=new HashMap<>();Map<String,Double> winRate=new HashMap<>();for(Model.User u:users){List<Model.Result> rs=store.results.stream().filter(r->r.left().equals(u.name)||r.right().equals(u.name)).toList();int matchesCount=rs.size();totalMatches.put(u.name,matchesCount);long wins=rs.stream().filter(r->r.winner().equals(u.name)).count();winRate.put(u.name,matchesCount>0?(wins*100.0/matchesCount):0.0);opponent.put(u.name,rs.stream().mapToInt(r->{Model.User other=store.users.get(r.left().equals(u.name)?r.right():r.left());return other==null?0:other.score;}).average().orElse(0));speed.put(u.name,rs.stream().filter(r->r.winner().equals(u.name)).mapToDouble(r->(r.left().equals(u.name)?r.leftMs():r.rightMs())/(r.duration()*1000.0)).average().orElse(Double.POSITIVE_INFINITY));}
  users.sort(Comparator.comparingInt((Model.User u)->u.score).reversed().thenComparing(Comparator.comparingDouble((Model.User u)->opponent.get(u.name)).reversed()).thenComparingDouble(u->speed.get(u.name)).thenComparing(u->u.name));
  for(Model.User u:users){String rankLabel=Rank.fromScore(u.score).getDisplayName();b.append(u.name).append(';').append(u.score).append(';').append(String.format(Locale.ROOT,"%.2f",opponent.get(u.name))).append(';').append(Double.isInfinite(speed.get(u.name))?"—":String.format(Locale.ROOT,"%.3f",speed.get(u.name))).append(';').append(totalMatches.get(u.name)).append(';').append(String.format(Locale.ROOT,"%.1f%%",winRate.get(u.name))).append(';').append(rankLabel).append('|');}}p.send("LEADERBOARD",b.toString());}
 void history(Peer p){StringBuilder b=new StringBuilder();synchronized(store){List<Model.Result> rs=db.isAvailable()?db.getUserHistory(p.name):store.results.stream().filter(r->r.left().equals(p.name)||r.right().equals(p.name)).toList();for(Model.Result r:rs)b.append(r.topic()).append(" / ").append(r.level()).append(" / ").append(r.setId()).append(": ").append(r.left()).append(" ").append(r.leftCorrect()).append("–").append(r.rightCorrect()).append(" ").append(r.right()).append(" → ").append(r.winner()).append(" | ").append(r.duration()).append(" | ").append(r.timestamp()).append('\n');}p.send("HISTORY",b.toString());}
 public static void main(String[] args)throws Exception{int port=args.length>0?Integer.parseInt(args[0]):5050;Path folder=Path.of(args.length>1?args[1]:"data");Server app=new Server(folder);try(ServerSocket socket=new ServerSocket(port)){System.out.println("Quiz server: port "+port+", data "+folder.toAbsolutePath());try{String ip=InetAddress.getLocalHost().getHostAddress();System.out.println("Dia chi IP may chu (de cac may khac ket noi): "+ip+":"+port);}catch(Exception ignored){}while(true){Peer p=new Peer(socket.accept());app.workers.submit(()->app.serve(p));}}}
}
