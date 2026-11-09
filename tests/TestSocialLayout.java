package quiz;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;
import quiz.ui.*;

/** Render the requested friend and chat entry points at actual supported window sizes. */
public final class TestSocialLayout {
    private static int checks;
    private static <T>T field(Object owner,String name,Class<T>type)throws Exception {
        Field f=owner.getClass().getDeclaredField(name);f.setAccessible(true);return type.cast(f.get(owner));
    }
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);checks++;}
    private static void layout(Container parent){for(int i=0;i<6;i++)once(parent);}
    private static void once(Container parent){parent.doLayout();for(Component c:parent.getComponents())if(c instanceof Container p&&c.isVisible())once(p);}
    private static void inside(Component c,Component root,String label){
        check(c.getWidth()>0&&c.getHeight()>0,label+" has space");
        for(Component n=c;n!=root&&n.getParent()!=null;n=n.getParent()) {
            Rectangle bounds=n.getBounds();Container p=n.getParent();
            check(bounds.x>=0&&bounds.y>=0&&bounds.x+bounds.width<=p.getWidth()&&bounds.y+bounds.height<=p.getHeight(),label+" fits "+p.getClass().getSimpleName()+" "+bounds+" / "+p.getSize());
        }
    }
    private static void snapshot(JComponent panel,String name)throws Exception{
        BufferedImage image=new BufferedImage(panel.getWidth(),panel.getHeight(),BufferedImage.TYPE_INT_RGB);
        Graphics2D g=image.createGraphics();panel.printAll(g);g.dispose();
        new File("target").mkdirs();ImageIO.write(image,"png",new File("target",name));
    }
    public static void main(String[]args)throws Exception{
        System.setProperty("java.util.prefs.PreferencesFactory",TestArenaVisuals.MemoryFactory.class.getName());SoundManager.setMuted(true);
        try{SwingUtilities.invokeAndWait(()->{try{
            LobbyScreen lobby=new LobbyScreen(Model.LEVELS);
            lobby.setCurrentUser("alice",42,"avatar_01","Alice","","","ADMIN");
            lobby.setTopics(List.of("Tổng hợp","Kỳ quan thế giới","Vũ trụ và khám phá không gian"));
            FriendsPanel friends=lobby.getFriendsPanel();ChatPanel chat=friends.getChatPanel();chat.setCurrentUser("alice");
            String row="bob\tNguyễn Minh\tavatar_02\t50\tĐồng\tREADY\tFRIEND";
            friends.updateFriends(new String[]{row});friends.updateRequests(new String[]{"carol\tCarol\tavatar_03\t10\tSắt\tOFFLINE\tINCOMING"});
            lobby.updatePlayersFromLobbyData("alice;42;Đang rỗi;avatar_01;Đồng|bob;50;Đang rỗi;avatar_02;Đồng","alice");
            NavigationBar nav=field(lobby,"navBar",NavigationBar.class);
            for(int[]size:new int[][]{{744,541},{1080,760},{1920,1000}}){
                lobby.setSize(size[0],size[1]);nav.selectTab("Bạn Bè");
                field(friends,"tabs",JTabbedPane.class).setSelectedIndex(0);layout(lobby);
                snapshot(lobby,"social-friends-"+size[0]+".png");
                friends.openChat("bob");layout(lobby);
                chat.appendMessage(new String[]{"CHAT_MESSAGE","1","bob","alice","Chào bạn! Cùng thi đấu nhé?","1794096000000"});
                inside(field(chat,"composer",JTextArea.class),lobby,"Private composer");
                inside(field(chat,"sendButton",JButton.class),lobby,"Send button");
                snapshot(lobby,"social-chat-"+size[0]+".png");
                check(chat.getSelectedConversation().equals("bob"),"Friend action opens correct recipient");
                nav.selectTab("Người Chơi");layout(lobby);
                JPanel players=field(lobby,"playersListPanel",JPanel.class);
                for(Component c:players.getComponents())if(c instanceof PlayerCard card){
                    inside(field(card,"btnFriend",JButton.class),card,"Friend request action");
                    inside(field(card,"btnChallenge",JButton.class),card,"Challenge action");
                }
            }
            check(lobby.getHistoryPanel().getAllCards()!=null,"Latest history accessor remains available");
        }catch(Exception e){throw new AssertionError(e);}});
        System.out.println("PASS TestSocialLayout: "+checks+" checks");}finally{SoundManager.shutdown();}
    }
}
