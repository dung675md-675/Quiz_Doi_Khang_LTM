package quiz.ui;

import java.awt.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.TreeMap;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/** Non-modal match conversation. Plain text only; the server assigns sender and match. */
public final class MatchChatPanel extends JPanel {
    private final String matchId;
    private final String username;
    private final JTextArea transcript = new JTextArea();
    private final JTextField composer = new JTextField();
    private final JLabel status = new JLabel("Tin nhắn chỉ hiển thị cho hai người trong trận.");
    private final ModernButton toggle = new ModernButton("Chat", ModernButton.Style.SECONDARY);
    private final TreeMap<Long, String> messages = new TreeMap<>();
    private Consumer<String> onSend;
    private Runnable onHistory;
    private JDialog dialog;
    private int unread;
    private boolean closed;
    private String pendingText;

    public MatchChatPanel(String matchId, String username, boolean solo) {
        super(new BorderLayout(0, 10));
        this.matchId = matchId;
        this.username = username;
        setBackground(ArenaPalette.BG);
        setBorder(new EmptyBorder(16, 16, 16, 16));
        transcript.setEditable(false);
        transcript.setLineWrap(true);
        transcript.setWrapStyleWord(true);
        transcript.setFont(ArenaPalette.font(14, Font.PLAIN));
        transcript.setBackground(ArenaPalette.SURFACE);
        transcript.setForeground(ArenaPalette.TEXT);
        transcript.setMargin(new Insets(12, 12, 12, 12));
        JScrollPane scroll = new JScrollPane(transcript);
        scroll.setBorder(BorderFactory.createLineBorder(ArenaPalette.BORDER));
        scroll.getVerticalScrollBar().setUI(new ModernScrollBarUI(true));
        add(scroll, BorderLayout.CENTER);
        status.setFont(ArenaPalette.font(12, Font.PLAIN));
        status.setForeground(ArenaPalette.MUTED);
        add(status, BorderLayout.NORTH);
        JPanel footer = new JPanel(new BorderLayout(8, 0));
        footer.setOpaque(false);
        composer.setFont(ArenaPalette.font(14, Font.PLAIN));
        composer.setBackground(ArenaPalette.SURFACE_RAISED);
        composer.setForeground(ArenaPalette.TEXT);
        composer.setCaretColor(ArenaPalette.ACCENT);
        composer.setBorder(new EmptyBorder(10, 10, 10, 10));
        composer.setToolTipText("Tối đa 500 ký tự · Enter để gửi");
        ModernButton send = new ModernButton("Gửi", ModernButton.Style.PRIMARY);
        send.setArenaStyle(true);
        send.addActionListener(e -> send());
        composer.addActionListener(e -> send());
        footer.add(composer, BorderLayout.CENTER);
        footer.add(send, BorderLayout.EAST);
        add(footer, BorderLayout.SOUTH);
        toggle.setArenaStyle(true);
        toggle.setPreferredSize(new Dimension(86, 38));
        toggle.setEnabled(!solo);
        toggle.setToolTipText(solo ? "Chat dành cho trận đấu với người chơi khác" : "Chat với đối thủ trong trận");
        toggle.addActionListener(e -> open(SwingUtilities.getWindowAncestor(toggle)));
    }

    public JButton getToggleButton() { return toggle; }
    public void setOnSend(Consumer<String> callback) { onSend = callback; }
    public void setOnHistory(Runnable callback) { onHistory = callback; }

    private void open(Window owner) {
        if (closed) return;
        if (dialog == null) {
            dialog = new JDialog(owner, "Trò chuyện trong trận", Dialog.ModalityType.MODELESS);
            dialog.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
            dialog.setContentPane(this);
            dialog.setSize(430, 420);
            dialog.setMinimumSize(new Dimension(340, 280));
            dialog.setLocationRelativeTo(owner);
        }
        dialog.setVisible(true);
        dialog.toFront();
        unread = 0;
        toggle.setText("Chat");
        if (onHistory != null) onHistory.run();
        composer.requestFocusInWindow();
    }

    private void send() {
        String text = composer.getText().strip();
        if (closed || pendingText != null || text.isEmpty() || onSend == null) return;
        if (text.codePointCount(0, text.length()) > 500) {
            showError("Tin nhắn tối đa 500 ký tự.");
            return;
        }
        pendingText = text;
        onSend.accept(text);
        status.setText("Đang gửi…");
    }

    public void receive(String[] packet) {
        if (closed || packet.length < 6 || !matchId.equals(packet[1])) return;
        try {
            long id = Long.parseLong(packet[2]);
            if (messages.containsKey(id)) return;
            String time = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
                    .format(Instant.ofEpochMilli(Long.parseLong(packet[5])));
            messages.put(id, (username.equals(packet[3]) ? "Bạn" : packet[3]) + " · " + time + "\n" + packet[4] + "\n\n");
            while (messages.size() > 100) messages.pollFirstEntry();
            transcript.setText(String.join("", messages.values()));
            transcript.setCaretPosition(transcript.getDocument().getLength());
            if (username.equals(packet[3])) {
                if (composer.getText().strip().equals(packet[4])) composer.setText("");
                if (packet[4].equals(pendingText)) pendingText = null;
                status.setText("Đã gửi.");
            } else if (dialog == null || !dialog.isVisible()) {
                toggle.setText("Chat (" + (++unread) + ")");
            }
        } catch (RuntimeException ignored) { }
    }

    public void showError(String message) { pendingText = null; status.setText(message); }
    public void close() {
        closed = true;
        pendingText = null;
        if (dialog != null) { dialog.dispose(); dialog = null; }
        messages.clear();
        transcript.setText("");
        composer.setText("");
    }
}
