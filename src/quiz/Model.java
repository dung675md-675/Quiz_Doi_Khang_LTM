package quiz;
import java.io.*;
import java.util.*;

public final class Model {
 public static final String[] LEVELS={"Dễ","Trung bình","Khó"};
 public static final int[] DURATIONS={120,160,200};

 public enum QuestionType {
  MULTIPLE_CHOICE("Trắc nghiệm 4 đáp án"),
  MULTIPLE_SELECT("Chọn nhiều đáp án"),
  TRUE_FALSE("Đúng / Sai"),
  CLUE_GUESS("Đoán qua manh mối"),
  FILL_IN("Điền vào chỗ trống"),
  ORDERING("Sắp xếp thứ tự"),
  ODD_ONE_OUT("Tìm đối tượng khác biệt"),
  COMPARISON("So sánh / Ước lượng"),
  PHENOMENON_EXPLANATION("Giải thích hiện tượng"),
  ELIMINATION("Loại trừ phương án"),
  DRAG_AND_DROP("Kéo thả đáp án");

  private final String displayName;
  QuestionType(String displayName){this.displayName=displayName;}
  public String getDisplayName(){return displayName;}
  public static QuestionType fromString(String raw){
   if(raw==null||raw.isBlank()) return MULTIPLE_CHOICE;
   for(QuestionType t:values()){
    if(t.name().equalsIgnoreCase(raw)||t.displayName.equalsIgnoreCase(raw)) return t;
   }
   return MULTIPLE_CHOICE;
  }
 }

 public static record ExamConfig(int easyCount, int mediumCount, int hardCount, int durationSeconds) implements Serializable {
  public static final ExamConfig DEFAULT = new ExamConfig(3, 4, 3, 120);
  public int totalQuestions() { return easyCount + mediumCount + hardCount; }
 }

 public static record Question(
  String id,
  String topic,
  String level,
  QuestionType type,
  String text,
  String[] choices,
  int answer,
  String explanation,
  String[] blanks,
  java.util.Map<String,String> fillMap
 ) implements Serializable {
  public Question(String text, String[] choices, int answer) {
   this("", "", "Dễ", QuestionType.MULTIPLE_CHOICE, text, choices, answer, "", null, null);
  }
  public Question(String id, String topic, String level, QuestionType type, String text, String[] choices, int answer) {
   this(id, topic, level, type, text, choices, answer, "", null, null);
  }
  public Question(String id, String topic, String level, QuestionType type, String text, String[] choices, int answer, String explanation) {
   this(id, topic, level, type, text, choices, answer, explanation, null, null);
  }
 }

 public static record SetData(String id,String topic,String level,int duration,List<Question> questions) implements Serializable {}
 public static final class User implements Serializable {
  private static final long serialVersionUID = 1L;
  public String name,password;
  public int score;
  public String avatarId="avatar_01";
  public String fullName="";
  public String email="";
  public String bio="";
  public String role="PLAYER";
  public User(String n,String p){
   name=n;password=p;
   this.role="admin".equalsIgnoreCase(n)?"ADMIN":"PLAYER";
  }
  public User(String n,String p,int score,String avatarId,String fullName,String email,String bio){
   this(n,p,score,avatarId,fullName,email,bio,"admin".equalsIgnoreCase(n)?"ADMIN":"PLAYER");
  }
  public User(String n,String p,int score,String avatarId,String fullName,String email,String bio,String role){
   this.name=n;this.password=p;this.score=Math.max(0,score);
   this.avatarId=(avatarId!=null&&!avatarId.isBlank())?avatarId:"avatar_01";
   this.fullName=fullName!=null?fullName:"";
   this.email=email!=null?email:"";
   this.bio=bio!=null?bio:"";
   this.role=(role!=null&&!role.isBlank())?role.toUpperCase():("admin".equalsIgnoreCase(n)?"ADMIN":"PLAYER");
  }
  public boolean isAdmin(){
   return "ADMIN".equalsIgnoreCase(role)||"admin".equalsIgnoreCase(name);
  }
 }
 public static record Result(String match,String left,String right,String topic,String level,String setId,int leftCorrect,int rightCorrect,long leftMs,long rightMs,int duration,String winner,long timestamp) implements Serializable {}
 public static final class Store implements Serializable {
  private static final long serialVersionUID = -2459928414739261786L;
  public Map<String,User> users=new HashMap<>(); public List<Result> results=new ArrayList<>();
  public static Store read(File file){if(!file.exists())return new Store();try(ObjectInputStream in=new ObjectInputStream(new FileInputStream(file))){return (Store)in.readObject();}catch(Exception e){System.err.println("[STORE] Dữ liệu "+file.getName()+" không tương thích ("+e.getMessage()+"). Khởi tạo kho dữ liệu mới.");return new Store();}}
  public void save(File file){try{File parent=file.getAbsoluteFile().getParentFile();parent.mkdirs();File temp=new File(parent,file.getName()+".tmp");try(ObjectOutputStream out=new ObjectOutputStream(new FileOutputStream(temp))){out.writeObject(this);}try{java.nio.file.Files.move(temp.toPath(),file.toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING,java.nio.file.StandardCopyOption.ATOMIC_MOVE);}catch(java.nio.file.AtomicMoveNotSupportedException e){java.nio.file.Files.move(temp.toPath(),file.toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING);}}catch(IOException e){throw new IllegalStateException(e);}}
 }
}
