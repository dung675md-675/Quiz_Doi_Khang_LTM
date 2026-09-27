package quiz;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.io.*;
import java.util.*;
final class Bank {
 final List<Model.SetData> sets=new ArrayList<>();
 Bank(Path path)throws IOException{
  this(path,null);
 }
 Bank(Path path, DB db)throws IOException{
  if(db!=null&&db.isAvailable()){
   List<Model.SetData> dbSets=db.loadQuestionSets();
   if(!dbSets.isEmpty()){
    sets.addAll(dbSets);
    System.out.println("[BANK] Đã nạp "+sets.size()+" bộ đề từ PostgreSQL.");
    return;
   }
  }
  loadFromFile(path);
 }
 private void loadFromFile(Path path)throws IOException{
  List<String> lines=Files.readAllLines(path,StandardCharsets.UTF_8);Map<String,List<Model.Question>> questions=new LinkedHashMap<>();Map<String,String[]> metadata=new LinkedHashMap<>();
  for(String line:lines){if(line.isBlank()||line.startsWith("#"))continue;String[] a=line.split("\\|",7);if(a.length!=7)throw new IOException("Sai định dạng dòng: "+line);
   String id=a[0],topic=a[1],level=a[2];int duration=Integer.parseInt(a[3]);int answer=Integer.parseInt(a[4]);if(answer<0||answer>3)throw new IOException("Đáp án phải 0..3");
   if(!Arrays.asList(Model.LEVELS).contains(level))throw new IOException("Mức độ không hợp lệ: "+level);
   questions.computeIfAbsent(id,k->new ArrayList<>()).add(new Model.Question(a[5],a[6].split(";",-1),answer));
   if(questions.get(id).get(questions.get(id).size()-1).choices().length!=4)throw new IOException("Cần 4 đáp án: "+id);
   String[] old=metadata.putIfAbsent(id,new String[]{topic,level,""+duration});if(old!=null&&(!old[0].equals(topic)||!old[1].equals(level)||!old[2].equals(""+duration)))throw new IOException("Metadata bộ đề không nhất quán: "+id);
  }
  for(var e:questions.entrySet()){String[] m=metadata.get(e.getKey());if(e.getValue().size()!=10)throw new IOException("Mỗi bộ phải có đúng 10 câu: "+e.getKey());sets.add(new Model.SetData(e.getKey(),m[0],m[1],Integer.parseInt(m[2]),List.copyOf(e.getValue())));}
  System.out.println("[BANK] Đã nạp "+sets.size()+" bộ đề từ file "+path.getFileName()+".");
 }
 List<String> topics(){return sets.stream().map(Model.SetData::topic).distinct().toList();}
 Model.SetData pick(String topic,String level,Set<String> used){List<Model.SetData> eligible=sets.stream().filter(s->s.topic().equals(topic)&&s.level().equals(level)).toList();if(eligible.isEmpty())return null;List<Model.SetData> fresh=eligible.stream().filter(s->!used.contains(s.id())).toList();if(fresh.isEmpty()){used.clear();fresh=eligible;}return fresh.get(new Random().nextInt(fresh.size()));}
}
