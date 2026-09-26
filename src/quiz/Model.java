package quiz;
import java.io.*;
import java.util.*;

final class Model {
 static final String[] LEVELS={"Dễ","Trung bình","Khó"};
 static final int[] DURATIONS={100,150,200};
 static record Question(String text,String[] choices,int answer) implements Serializable {}
 static record SetData(String id,String topic,String level,int duration,List<Question> questions) implements Serializable {}
 static final class User implements Serializable {String name,password;int score;User(String n,String p){name=n;password=p;}}
 static record Result(String match,String left,String right,String topic,String level,String setId,int leftCorrect,int rightCorrect,long leftMs,long rightMs,int duration,String winner) implements Serializable {}
 static final class Store implements Serializable {
  Map<String,User> users=new HashMap<>(); List<Result> results=new ArrayList<>();
  static Store read(File file){if(!file.exists())return new Store();try(ObjectInputStream in=new ObjectInputStream(new FileInputStream(file))){return (Store)in.readObject();}catch(Exception e){throw new IllegalStateException("Không đọc được dữ liệu: "+file,e);}}
  void save(File file){try{File parent=file.getAbsoluteFile().getParentFile();parent.mkdirs();File temp=new File(parent,file.getName()+".tmp");try(ObjectOutputStream out=new ObjectOutputStream(new FileOutputStream(temp))){out.writeObject(this);}try{java.nio.file.Files.move(temp.toPath(),file.toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING,java.nio.file.StandardCopyOption.ATOMIC_MOVE);}catch(java.nio.file.AtomicMoveNotSupportedException e){java.nio.file.Files.move(temp.toPath(),file.toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING);}}catch(IOException e){throw new IllegalStateException(e);}}
 }
}
