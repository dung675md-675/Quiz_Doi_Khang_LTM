package quiz;
import java.io.*;
public final class Wire {
 public static void send(DataOutputStream out,String... fields)throws IOException{synchronized(out){out.writeInt(fields.length);for(String f:fields)out.writeUTF(f==null?"":f);out.flush();}}
 public static String[] read(DataInputStream in)throws IOException{int count=in.readInt();if(count<1||count>1000)throw new IOException("Gói tin không hợp lệ");String[] a=new String[count];for(int i=0;i<count;i++)a[i]=in.readUTF();return a;}
 public static String field(String[] a,int i){return (a!=null&&i<a.length)?a[i]:"";}
}
