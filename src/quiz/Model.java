package quiz;
import java.io.*;
import java.util.*;

public final class Model {
 public static final String[] LEVELS={"Dễ","Trung bình","Khó"};
 public static final int[] DURATIONS={100,150,200};
 public static record Question(String text,String[] choices,int answer) implements Serializable {}
 public static record SetData(String id,String topic,String level,int duration,List<Question> questions) implements Serializable {}
 public static final class User implements Serializable {public String name,password;public int score;public User(String n,String p){name=n;password=p;}}
 public static record Result(String match,String left,String right,String topic,String level,String setId,int leftCorrect,int rightCorrect,long leftMs,long rightMs,int duration,String winner) implements Serializable {}
 public static final class Store implements Serializable {
  public Map<String,User> users=new HashMap<>(); public List<Result> results=new ArrayList<>();
  public static Store read(File file){if(!file.exists())return new Store();try(ObjectInputStream in=new ObjectInputStream(new FileInputStream(file))){return (Store)in.readObject();}catch(Exception e){throw new IllegalStateException("Không đọc được dữ liệu: "+file,e);}}
  public void save(File file){try{File parent=file.getAbsoluteFile().getParentFile();parent.mkdirs();File temp=new File(parent,file.getName()+".tmp");try(ObjectOutputStream out=new ObjectOutputStream(new FileOutputStream(temp))){out.writeObject(this);}try{java.nio.file.Files.move(temp.toPath(),file.toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING,java.nio.file.StandardCopyOption.ATOMIC_MOVE);}catch(java.nio.file.AtomicMoveNotSupportedException e){java.nio.file.Files.move(temp.toPath(),file.toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING);}}catch(IOException e){throw new IllegalStateException(e);}}
 }
}
