package quiz.ui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.HierarchyEvent;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.IntConsumer;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;

/** Private, plain-text conversations. The server remains authoritative for sends and read cursors. */
public class ChatPanel extends JPanel {
    @FunctionalInterface public interface SendListener { void send(String target, String text, String nonce); }
    private record Message(long id, String sender, String recipient, String text, long timestamp) {}
    private record Summary(int unread, long lastId, long timestamp, String preview) {}
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd/MM HH:mm");
    private final JComboBox<String> contacts = new JComboBox<>();
    private final JLabel unreadLabel = label("0 tin chưa đọc", ArenaPalette.CYAN);
    private final JLabel status = label("Chọn một người bạn để bắt đầu trò chuyện.", ArenaPalette.MUTED);
    private final JTextArea transcript = new JTextArea();
    private final JTextArea composer = new JTextArea(2, 20);
    private final JScrollPane messageScroll = new JScrollPane(transcript);
    private final ModernButton sendButton = button("Gửi tin", ModernButton.Style.PRIMARY);
    private final ModernButton olderButton = button("Tin cũ hơn", ModernButton.Style.SECONDARY);
    private final Set<String> friends = new LinkedHashSet<>();
    private final Map<String, Summary> summaries = new LinkedHashMap<>();
    private final Map<String, TreeMap<Long, Message>> conversations = new HashMap<>();
    private final Map<String, Boolean> hasMore = new HashMap<>();
    private final Map<String, Long> historyRequests = new HashMap<>();
    private final Map<String, Long> readRequested = new HashMap<>();
    private final Map<String, Long> readAcknowledged = new HashMap<>();
    private final Map<String, String> drafts = new HashMap<>();
    private String currentUser = "";
    private String selected = "";
    private String pendingTarget = "";
    private String pendingText = "";
    private String pendingNonce = "";
    private String retryTarget = "";
    private String retryText = "";
    private String retryNonce = "";
    private boolean rebuildingContacts;
    private int totalUnread;
    private SendListener onSend;
    private BiConsumer<String, Long> onHistory;
    private BiConsumer<String, Long> onRead;
    private Runnable onRefresh;
    private IntConsumer onUnreadChanged;

    public ChatPanel() {
        super(new BorderLayout(0, 10));
        setOpaque(false);
        setBorder(new EmptyBorder(8, 0, 0, 0));
        JPanel heading = new JPanel(new BorderLayout(10, 0));
        heading.setOpaque(false);
        contacts.setFont(ArenaPalette.font(14, Font.BOLD));
        contacts.setBackground(ArenaPalette.SURFACE_RAISED);
        contacts.setForeground(ArenaPalette.TEXT);
        contacts.setPreferredSize(new Dimension(240, 38));
        contacts.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                putClientProperty("html.disable", Boolean.TRUE);
                String username = value == null ? "Chọn cuộc trò chuyện" : value.toString();
                Summary summary = summaries.get(username);
                setText(username + (summary != null && summary.unread() > 0 ? "  ·  " + summary.unread() + " mới" : ""));
                setBackground(selected ? ArenaPalette.SURFACE_RAISED : ArenaPalette.SURFACE);
                setForeground(ArenaPalette.TEXT);
                setBorder(new EmptyBorder(7, 10, 7, 10));
                return this;
            }
        });
        contacts.addActionListener(event -> {
            if (!rebuildingContacts && contacts.getSelectedItem() instanceof String username) openConversation(username);
        });
        heading.add(contacts, BorderLayout.CENTER);
        JPanel tools = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        tools.setOpaque(false);
        tools.add(unreadLabel);
        ModernButton refresh = button("Làm mới", ModernButton.Style.SECONDARY);
        refresh.addActionListener(event -> { if (onRefresh != null) onRefresh.run(); });
        tools.add(refresh);
        heading.add(tools, BorderLayout.EAST);
        add(heading, BorderLayout.NORTH);

        transcript.setEditable(false);
        transcript.setFocusable(true);
        transcript.setLineWrap(true);
        transcript.setWrapStyleWord(true);
        transcript.setFont(ArenaPalette.font(14, Font.PLAIN));
        transcript.setForeground(ArenaPalette.TEXT);
        transcript.setBackground(ArenaPalette.SURFACE);
        transcript.setBorder(new EmptyBorder(14, 16, 14, 16));
        messageScroll.setBorder(BorderFactory.createLineBorder(ArenaPalette.BORDER));
        messageScroll.getViewport().setBackground(ArenaPalette.SURFACE);
        messageScroll.getVerticalScrollBar().setUnitIncrement(24);
        ArenaPalette.styleScrollBar(messageScroll.getVerticalScrollBar());
        JPanel content = new JPanel(new BorderLayout(0, 8));
        content.setOpaque(false);
        JPanel historyTools = new JPanel(new BorderLayout(8, 0));
        historyTools.setOpaque(false);
        historyTools.add(status, BorderLayout.CENTER);
        olderButton.setPreferredSize(new Dimension(112, 32));
        olderButton.addActionListener(event -> {
            TreeMap<Long, Message> messages = conversations.get(selected);
            if (messages != null && !messages.isEmpty()) requestHistory(selected, messages.firstKey());
        });
        historyTools.add(olderButton, BorderLayout.EAST);
        content.add(historyTools, BorderLayout.NORTH);
        content.add(messageScroll, BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);

        composer.setFont(ArenaPalette.font(14, Font.PLAIN));
        composer.setLineWrap(true);
        composer.setWrapStyleWord(true);
        composer.setForeground(ArenaPalette.TEXT);
        composer.setBackground(ArenaPalette.SURFACE_RAISED);
        composer.setCaretColor(ArenaPalette.CYAN);
        composer.setDisabledTextColor(ArenaPalette.MUTED);
        composer.setBorder(new EmptyBorder(8, 10, 8, 10));
        ((AbstractDocument) composer.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override public void insertString(FilterBypass bypass, int offset, String value, AttributeSet attrs)
                    throws BadLocationException { replace(bypass, offset, 0, value, attrs); }
            @Override public void replace(FilterBypass bypass, int offset, int length, String value, AttributeSet attrs)
                    throws BadLocationException {
                String old = bypass.getDocument().getText(0, bypass.getDocument().getLength());
                String next = old.substring(0, offset) + (value == null ? "" : value) + old.substring(offset + length);
                if (next.codePointCount(0, next.length()) <= 2000) super.replace(bypass, offset, length, value, attrs);
                else status.setText("Mỗi tin nhắn tối đa 2000 ký tự.");
            }
        });
        composer.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent event) { updateControls(); }
            @Override public void removeUpdate(DocumentEvent event) { updateControls(); }
            @Override public void changedUpdate(DocumentEvent event) { updateControls(); }
        });
        composer.getInputMap().put(KeyStroke.getKeyStroke("control ENTER"), "sendPrivateMessage");
        composer.getActionMap().put("sendPrivateMessage", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent event) { sendCurrent(); }
        });
        JScrollPane composerScroll = new JScrollPane(composer);
        composerScroll.setBorder(BorderFactory.createLineBorder(ArenaPalette.BORDER));
        composerScroll.setPreferredSize(new Dimension(400, 64));
        ArenaPalette.styleScrollBar(composerScroll.getVerticalScrollBar());
        JPanel footer = new JPanel(new BorderLayout(10, 5));
        footer.setOpaque(false);
        footer.add(composerScroll, BorderLayout.CENTER);
        sendButton.setIcon(new ArenaIcon(ArenaIcon.Type.ARROW, 17, null));
        sendButton.setPreferredSize(new Dimension(104, 44));
        sendButton.addActionListener(event -> sendCurrent());
        footer.add(sendButton, BorderLayout.EAST);
        JLabel hint = label("Chỉ gửi được cho bạn bè · Ctrl+Enter để gửi", ArenaPalette.MUTED);
        hint.setFont(ArenaPalette.font(11, Font.PLAIN));
        footer.add(hint, BorderLayout.SOUTH);
        add(footer, BorderLayout.SOUTH);
        addHierarchyListener(event -> {
            if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) markVisibleConversation();
        });
        messageScroll.getVerticalScrollBar().addAdjustmentListener(event -> {
            if (!event.getValueIsAdjusting() && atBottom()) markVisibleConversation();
        });
        updateControls();
    }

    public void setOnSend(SendListener listener) { onSend = listener; }
    public void setOnHistory(BiConsumer<String, Long> listener) { onHistory = listener; }
    public void setOnRead(BiConsumer<String, Long> listener) { onRead = listener; }
    public void setOnRefresh(Runnable listener) { onRefresh = listener; }
    public void setOnUnreadChanged(IntConsumer listener) { onUnreadChanged = listener; }

    public void setCurrentUser(String username) {
        String next = username == null ? "" : username;
        if (!next.equals(currentUser)) clearSession();
        currentUser = next;
        updateControls();
    }

    public void updateContacts(String[] rows) {
        friends.clear();
        if (rows != null) for (String row : rows) {
            String[] fields = row == null ? new String[0] : row.split("\t", -1);
            if (fields.length >= 7 && "FRIEND".equals(fields[6]) && !fields[0].equals(currentUser)) friends.add(fields[0]);
        }
        rebuildContacts();
        updateControls();
    }

    public void openConversation(String target) {
        if (target == null || target.isBlank() || target.equals(currentUser)) return;
        if (!target.equals(selected)) {
            if (!selected.isEmpty()) drafts.put(selected, composer.getText());
            composer.setText(drafts.getOrDefault(target, ""));
        }
        selected = target;
        rebuildContacts();
        renderConversation(true);
        requestHistory(target, 0);
        updateControls();
    }

    public String getSelectedConversation() { return selected; }

    public void updateHistory(String[] packet) {
        if (packet == null || packet.length < 4) return;
        String target = packet[1];
        long requestedBefore = number(packet[2]);
        Long expected = historyRequests.get(target);
        if (expected == null || expected != requestedBefore) return;
        historyRequests.remove(target);
        TreeMap<Long, Message> messages = conversations.computeIfAbsent(target, ignored -> new TreeMap<>());
        for (int index = 4; index + 4 < packet.length; index += 5) {
            Message message = parseMessage(packet, index);
            if (message != null && belongs(message, target)) messages.put(message.id(), message);
        }
        hasMore.put(target, Boolean.parseBoolean(packet[3]));
        if (selected.equals(target)) {
            renderConversation(requestedBefore == 0);
            status.setText(friends.contains(target) ? "Trò chuyện riêng với " + target : "Cuộc trò chuyện cũ · Kết bạn lại để gửi tin.");
            updateControls();
            if (requestedBefore == 0) markVisibleConversation();
        }
    }

    public void appendMessage(String[] packet) {
        if (packet == null || packet.length < 6) return;
        Message message = parseMessage(packet, 1);
        if (message == null || (!message.sender().equals(currentUser) && !message.recipient().equals(currentUser))) return;
        String target = message.sender().equals(currentUser) ? message.recipient() : message.sender();
        conversations.computeIfAbsent(target, ignored -> new TreeMap<>()).put(message.id(), message);
        String nonce = packet.length > 6 ? packet[6] : "";
        if (message.sender().equals(currentUser) && target.equals(pendingTarget)
                && (pendingNonce.equals(nonce) || nonce.isEmpty() && pendingText.equals(message.text()))) {
            if (selected.equals(target) && composer.getText().equals(pendingText)) composer.setText("");
            pendingTarget = pendingText = pendingNonce = "";
            retryTarget = retryText = retryNonce = "";
            drafts.remove(target);
            if (selected.equals(target)) status.setText("Đã gửi tin nhắn.");
        }
        if (selected.equals(target)) {
            boolean follow = atBottom() || message.sender().equals(currentUser);
            renderConversation(follow);
            if (follow) markVisibleConversation();
        }
        updateControls();
    }

    public void updateSummary(String[] packet) {
        summaries.clear();
        if (packet != null) for (int index = 1; index + 4 < packet.length; index += 5) {
            String target = packet[index];
            summaries.put(target, new Summary((int) number(packet[index + 1]), number(packet[index + 2]),
                    number(packet[index + 3]), packet[index + 4]));
        }
        rebuildContacts();
    }

    public void updateUnread(int count) {
        totalUnread = Math.max(0, count);
        unreadLabel.setText(totalUnread + " tin chưa đọc");
        if (onUnreadChanged != null) onUnreadChanged.accept(totalUnread);
    }

    public void acknowledgeRead(String target, long throughId) {
        readAcknowledged.merge(target, throughId, Math::max);
        Summary summary = summaries.get(target);
        if (summary != null && summary.lastId() <= throughId)
            summaries.put(target, new Summary(0, summary.lastId(), summary.timestamp(), summary.preview()));
        contacts.repaint();
    }

    public void showError(String command, String target, String message, String nonce) {
        if ("CHAT_SEND".equals(command) && target.equals(pendingTarget)
                && (nonce == null || nonce.isEmpty() || nonce.equals(pendingNonce))) {
            pendingTarget = pendingText = pendingNonce = "";
        }
        if ("CHAT_HISTORY".equals(command)) historyRequests.remove(target);
        if ("CHAT_READ".equals(command)) readRequested.remove(target);
        if (target == null || target.isEmpty() || target.equals(selected)) status.setText(message);
        updateControls();
    }

    public void clearSession() {
        currentUser = selected = pendingTarget = pendingText = pendingNonce = "";
        retryTarget = retryText = retryNonce = "";
        friends.clear(); summaries.clear(); conversations.clear(); hasMore.clear(); historyRequests.clear();
        readRequested.clear(); readAcknowledged.clear();
        drafts.clear();
        composer.setText(""); transcript.setText("");
        rebuildingContacts = true;
        contacts.removeAllItems();
        rebuildingContacts = false;
        updateUnread(0);
        status.setText("Chọn một người bạn để bắt đầu trò chuyện.");
        updateControls();
    }

    /** A visible, open conversation may acknowledge only the received messages it displays. */
    protected boolean isConversationVisible() { return isShowing(); }

    private void markVisibleConversation() {
        if (!isConversationVisible() || selected.isEmpty() || onRead == null || !atBottom()) return;
        TreeMap<Long, Message> messages = conversations.get(selected);
        if (messages == null) return;
        long through = messages.values().stream().filter(message -> message.recipient().equals(currentUser))
                .mapToLong(Message::id).max().orElse(0);
        long already = Math.max(readRequested.getOrDefault(selected, 0L), readAcknowledged.getOrDefault(selected, 0L));
        if (through > already) {
            readRequested.put(selected, through);
            onRead.accept(selected, through);
        }
    }

    private void requestHistory(String target, long before) {
        if (currentUser.isEmpty() || onHistory == null || historyRequests.containsKey(target)) return;
        historyRequests.put(target, before);
        status.setText("Đang tải tin nhắn...");
        updateControls();
        onHistory.accept(target, before);
    }

    private void sendCurrent() {
        String text = composer.getText();
        if (!friends.contains(selected) || currentUser.isEmpty() || text.isBlank() || !pendingNonce.isEmpty() || onSend == null) return;
        if (text.codePointCount(0, text.length()) > 2000) return;
        if (!selected.equals(retryTarget) || !text.equals(retryText)) {
            retryTarget = selected; retryText = text; retryNonce = UUID.randomUUID().toString();
        }
        pendingTarget = selected; pendingText = text; pendingNonce = retryNonce;
        status.setText("Đang gửi tin nhắn...");
        updateControls();
        onSend.send(pendingTarget, pendingText, pendingNonce);
    }

    private void rebuildContacts() {
        rebuildingContacts = true;
        Set<String> names = new LinkedHashSet<>();
        summaries.entrySet().stream().sorted(Comparator.comparingLong((Map.Entry<String, Summary> entry) -> entry.getValue().timestamp()).reversed())
                .forEach(entry -> names.add(entry.getKey()));
        names.addAll(friends);
        if (!selected.isEmpty()) names.add(selected);
        contacts.removeAllItems();
        for (String username : names) contacts.addItem(username);
        contacts.setSelectedItem(selected.isEmpty() ? null : selected);
        rebuildingContacts = false;
    }

    private void renderConversation(boolean followBottom) {
        StringBuilder text = new StringBuilder();
        TreeMap<Long, Message> messages = conversations.get(selected);
        if (messages != null) for (Message message : messages.values()) {
            String time = TIME.format(Instant.ofEpochMilli(message.timestamp()).atZone(ZoneId.systemDefault()));
            text.append(message.sender().equals(currentUser) ? "Bạn" : message.sender()).append("  ·  ").append(time)
                    .append('\n').append(message.text()).append("\n\n");
        }
        int oldCaret = transcript.getCaretPosition();
        transcript.setText(text.toString());
        transcript.setCaretPosition(followBottom ? transcript.getDocument().getLength() : Math.min(oldCaret, transcript.getDocument().getLength()));
    }

    private void updateControls() {
        boolean writable = !currentUser.isEmpty() && friends.contains(selected) && pendingNonce.isEmpty();
        composer.setEnabled(writable);
        sendButton.setEnabled(writable && !composer.getText().isBlank());
        olderButton.setEnabled(!selected.isEmpty() && hasMore.getOrDefault(selected, false) && !historyRequests.containsKey(selected));
    }

    private boolean atBottom() {
        JScrollBar bar = messageScroll.getVerticalScrollBar();
        return bar.getValue() + bar.getVisibleAmount() >= bar.getMaximum() - 32;
    }
    private boolean belongs(Message message, String target) {
        return message.sender().equals(currentUser) && message.recipient().equals(target)
                || message.sender().equals(target) && message.recipient().equals(currentUser);
    }
    private static Message parseMessage(String[] fields, int index) {
        try {
            long id = Long.parseLong(fields[index]);
            long timestamp = Long.parseLong(fields[index + 4]);
            return id > 0 ? new Message(id, fields[index + 1], fields[index + 2], fields[index + 3], timestamp) : null;
        } catch (RuntimeException malformed) { return null; }
    }
    private static long number(String value) { try { return Math.max(0, Long.parseLong(value)); } catch (RuntimeException ignored) { return 0; } }
    private static JLabel label(String text, Color color) {
        JLabel label = new JLabel(text); label.putClientProperty("html.disable", Boolean.TRUE);
        label.setFont(ArenaPalette.font(12, Font.PLAIN)); label.setForeground(color); return label;
    }
    private static ModernButton button(String text, ModernButton.Style style) {
        ModernButton button = new ModernButton(text, style); button.setArenaStyle(true); return button;
    }
}
