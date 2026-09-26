package quiz;
import java.io.*;
final class Wire {
 static void send(DataOutputStream out,String... fields)throws IOException{synchronized(out){out.writeInt(fields.length);for(String f:fields)out.writeUTF(f==null?"":f);out.flush();}}
 static String[] read(DataInputStream in)throws IOException{int count=in.readInt();if(count<1||count>80)throw new IOException("Gói tin không hợp lệ");String[] a=new String[count];for(int i=0;i<count;i++)a[i]=in.readUTF();return a;}
 static String field(String[] a,int i){return i<a.length?a[i]:"";}
}
