package quiz;

import java.awt.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.*;
import quiz.ui.*;

/** Private chat acknowledgements and session privacy without opening windows. */
public final class TestSocialPanels {
    private static int checks;
    private static final class VisibleChat extends ChatPanel {
        boolean visible;
        @Override protected boolean isConversationVisible() { return visible; }
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("java.util.prefs.PreferencesFactory", TestArenaVisuals.MemoryFactory.class.getName());
        SoundManager.setMuted(true);
        try {
            SwingUtilities.invokeAndWait(() -> {
                try { testChat(); testMatchChat(); }
                catch (Exception failure) { throw new AssertionError(failure); }
            });
            System.out.println("PASS TestSocialPanels: " + checks + " private chat panel checks");
        } finally { SoundManager.shutdown(); }
    }

    private static void testChat() throws Exception {
        VisibleChat chat = new VisibleChat();
        List<String[]> sends = new ArrayList<>();
        List<String> histories = new ArrayList<>();
        List<Long> reads = new ArrayList<>();
        chat.setOnSend((target, text, nonce) -> sends.add(new String[] {target, text, nonce}));
        chat.setOnHistory((target, cursor) -> histories.add(target + ":" + cursor));
        chat.setOnRead((target, cursor) -> reads.add(cursor));
        chat.setCurrentUser("alice");
        chat.updateContacts(new String[] {person("bob"), person("carol")});
        chat.openConversation("bob");
        JTextArea composer = field(chat, "composer", JTextArea.class);
        JTextArea transcript = field(chat, "transcript", JTextArea.class);
        JButton send = field(chat, "sendButton", JButton.class);
        composer.setText("<html>Xin chào 🙂</html>");
        String draft = composer.getText();
        send.doClick(0);
        check(sends.size() == 1 && sends.get(0)[0].equals("bob") && !sends.get(0)[2].isBlank(), "Send includes recipient and idempotency nonce");
        check(composer.getText().equals(draft) && !send.isEnabled(), "Pending send retains text and blocks double send");
        send.doClick(0);
        check(sends.size() == 1, "Double click cannot duplicate a pending message");
        String nonce = sends.get(0)[2];
        chat.showError("CHAT_SEND", "bob", "Thử lại", nonce);
        check(send.isEnabled() && composer.getText().equals(draft), "Failed send preserves editable draft");
        send.doClick(0);
        check(sends.size() == 2 && sends.get(1)[2].equals(nonce), "Retry keeps its nonce");
        chat.appendMessage(new String[] {"CHAT_MESSAGE", "1", "alice", "bob", draft, "1794096000000", nonce});
        check(composer.getText().isEmpty(), "Server acknowledgement clears the matching draft");
        chat.appendMessage(new String[] {"CHAT_MESSAGE", "1", "alice", "bob", draft, "1794096000000", nonce});
        check(transcript.getText().indexOf(draft) == transcript.getText().lastIndexOf(draft), "Duplicate message id is rendered once");
        chat.appendMessage(new String[] {"CHAT_MESSAGE", "2", "bob", "alice", "Tin mới <b>nguyên văn</b>", "1794096001000"});
        check(transcript.getText().contains("<b>nguyên văn</b>"), "Message markup remains literal plain text");
        check(reads.isEmpty(), "Hidden chat never marks incoming messages read");
        chat.updateHistory(new String[] {"CHAT_HISTORY", "bob", "0", "true", "2", "bob", "alice", "Tin mới <b>nguyên văn</b>", "1794096001000"});
        check(reads.isEmpty(), "Hidden history loading never marks messages read");
        chat.setSize(604, 330); layout(chat);
        JScrollPane scroll = field(chat, "messageScroll", JScrollPane.class);
        scroll.getVerticalScrollBar().setValues(0, 100, 0, 100);
        chat.visible = true;
        Method mark = ChatPanel.class.getDeclaredMethod("markVisibleConversation"); mark.setAccessible(true); mark.invoke(chat);
        check(reads.equals(List.of(2L)), "Visible chat acknowledges only displayed received ids");
        mark.invoke(chat);
        check(reads.size() == 1, "Read cursor is not repeatedly sent");
        chat.acknowledgeRead("bob", 2);
        composer.setText("Nháp dành cho Bob");
        chat.openConversation("carol");
        check(composer.getText().isEmpty(), "Switching recipients never carries a different friend's draft");
        composer.setText("Nháp dành cho Carol");
        chat.openConversation("bob");
        check(composer.getText().equals("Nháp dành cho Bob"), "Conversation drafts are restored independently");
        chat.updateContacts(new String[] {person("carol")});
        check(!send.isEnabled(), "Unfriending disables new private messages without hiding history");
        chat.openConversation("carol");
        chat.updateHistory(new String[] {"CHAT_HISTORY", "bob", "0", "false", "3", "bob", "alice", "CHỈ BOB", "1794096002000"});
        check(!transcript.getText().contains("CHỈ BOB"), "Stale history for another friend cannot replace the current conversation");
        composer.setText("x".repeat(2001));
        check(composer.getText().codePointCount(0, composer.getText().length()) <= 2000, "Composer enforces its Unicode message limit");
        chat.clearSession();
        check(transcript.getText().isEmpty() && composer.getText().isEmpty() && chat.getSelectedConversation().isEmpty(), "Logout clears messages and drafts");
        check(!send.isEnabled(), "Logged out chat cannot send");
    }

    private static String person(String name) { return name + "\t" + name + "\tavatar_01\t0\tSắt\tREADY\tFRIEND"; }

    private static void testMatchChat() throws Exception {
        MatchChatPanel panel = new MatchChatPanel("match-one", "alice", false);
        List<String> sends = new ArrayList<>();
        panel.setOnSend(sends::add);
        JTextField input = field(panel, "composer", JTextField.class);
        JTextArea transcript = field(panel, "transcript", JTextArea.class);
        input.setText("Xin chào <b>Bob</b>");
        input.postActionEvent(); input.postActionEvent();
        check(sends.size() == 1 && !input.getText().isEmpty(), "Match send waits for acknowledgement and blocks double enter");
        panel.showError("Thử lại"); input.postActionEvent();
        check(sends.size() == 2, "Match send error preserves draft and permits retry");
        panel.receive(new String[]{"MATCH_CHAT_MESSAGE", "another-match", "1", "bob", "WRONG MATCH", "1794096000000"});
        check(transcript.getText().isEmpty(), "Other matches cannot appear in this transcript");
        String[] own={"MATCH_CHAT_MESSAGE", "match-one", "1", "alice", "Xin chào <b>Bob</b>", "1794096000000"};
        panel.receive(own); panel.receive(own);
        check(input.getText().isEmpty() && transcript.getText().indexOf("<b>Bob</b>") == transcript.getText().lastIndexOf("<b>Bob</b>"), "Matching echo clears draft and renders each id once as plain text");
        panel.receive(new String[]{"MATCH_CHAT_MESSAGE", "match-one", "2", "bob", "Chúc may mắn!", "1794096000001"});
        check(panel.getToggleButton().getText().contains("1"), "Hidden incoming match chat increments notification");
        input.setText("x".repeat(501)); input.postActionEvent();
        check(sends.size() == 2, "Match composer rejects oversized text");
        panel.close(); input.setText("Cannot send"); input.postActionEvent();
        check(sends.size() == 2 && transcript.getText().isEmpty(), "Leaving a match closes chat and clears messages");
        check(!new MatchChatPanel("solo", "alice", true).getToggleButton().isEnabled(), "Solo practice exposes no opponent chat");
    }
    private static void layout(Container parent) { for (int i = 0; i < 4; i++) layoutOnce(parent); }
    private static void layoutOnce(Container parent) {
        parent.doLayout();
        for (Component child : parent.getComponents()) if (child instanceof Container nested && child.isVisible()) layoutOnce(nested);
    }
    private static <T> T field(Object instance, String name, Class<T> type) throws Exception {
        Class<?> owner = instance.getClass();
        while (owner != null) {
            try { Field field = owner.getDeclaredField(name); field.setAccessible(true); return type.cast(field.get(instance)); }
            catch (NoSuchFieldException missing) { owner = owner.getSuperclass(); }
        }
        throw new NoSuchFieldException(name);
    }
    private static <T> List<T> find(Container parent, Class<T> type) {
        List<T> values = new ArrayList<>();
        for (Component child : parent.getComponents()) {
            if (type.isInstance(child)) values.add(type.cast(child));
            if (child instanceof Container nested) values.addAll(find(nested, type));
        }
        return values;
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); checks++; }
}
