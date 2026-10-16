package quiz.ui;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * PlayerCard — Card người chơi với Avatar emoji, Rank badge, điểm số.
 */
public class PlayerCard extends ModernCard {

    private final String username;
    private final int score;
    private final boolean isReady;
    private final boolean isInQueue;
    private boolean isSelected = false;

    private final ModernButton btnChallenge;
    private Consumer<String> onChallengeCallback;
    private Consumer<PlayerCard> onSelectCallback;

    public PlayerCard(String username, int score, String rankLabel, String avatarId, boolean isReady, boolean isInQueue) {
        super(new BorderLayout(14, 0));
        this.username = username;
        this.score = score;
        this.isReady = isReady;
        this.isInQueue = isInQueue;

        setCardBackground(Theme.WHITE);
        setBorderColor(Theme.BORDER_LIGHT);
        setCornerRadius(12);
        setCardPadding(10, 14, 10, 14);
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
        setPreferredSize(new Dimension(500, 70));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // LEFT: Avatar + Name + Rank + Score
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftPanel.setOpaque(false);

        // Avatar emoji circle
        AvatarEmojiIcon avatar = new AvatarEmojiIcon(avatarId);
        leftPanel.add(avatar);

        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);

        JLabel lblName = new JLabel(username);
        lblName.setFont(Theme.SUBTITLE);
        lblName.setForeground(Theme.DARK_TEXT);
        infoPanel.add(lblName);

        JPanel scoreRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        scoreRow.setOpaque(false);

        JLabel lblRank = new JLabel(rankLabel);
        lblRank.setFont(Theme.SMALL_BOLD);
        lblRank.setForeground(Theme.PURPLE);
        scoreRow.add(lblRank);

        JLabel lblDot = new JLabel("•");
        lblDot.setFont(Theme.SMALL);
        lblDot.setForeground(Theme.MUTED_GRAY);
        scoreRow.add(lblDot);

        JLabel lblScore = new JLabel(score + " điểm");
        lblScore.setFont(Theme.SMALL_BOLD);
        lblScore.setForeground(Theme.MUTED_GRAY);
        scoreRow.add(lblScore);
        infoPanel.add(scoreRow);

        leftPanel.add(infoPanel);
        add(leftPanel, BorderLayout.WEST);

        // CENTER: Status Badge
        JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 10));
        centerPanel.setOpaque(false);
        String statusText = isInQueue ? "Đang tìm" : (isReady ? "Sẵn sàng" : "Đang đấu");
        ModernBadge.Type statusType = isInQueue ? ModernBadge.Type.WARNING :
                                     (isReady ? ModernBadge.Type.READY : ModernBadge.Type.BUSY);
        ModernBadge statusBadge = new ModernBadge(statusText, statusType);
        centerPanel.add(statusBadge);
        add(centerPanel, BorderLayout.CENTER);

        // RIGHT: Challenge Button
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 8));
        rightPanel.setOpaque(false);
        btnChallenge = new ModernButton(isReady ? "Thách đấu" : "Đang bận", ModernButton.Style.PRIMARY);
        btnChallenge.setPreferredSize(new Dimension(110, 36));
        btnChallenge.setFont(Theme.BODY_BOLD);
        btnChallenge.setEnabled(isReady);
        btnChallenge.addActionListener(e -> {
            if (isReady && onChallengeCallback != null) {
                SoundManager.playClick();
                onChallengeCallback.accept(username);
            }
        });
        rightPanel.add(btnChallenge);
        add(rightPanel, BorderLayout.EAST);

        // Hover
        addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { if (onSelectCallback != null) onSelectCallback.accept(PlayerCard.this); }
            @Override public void mouseEntered(MouseEvent e) { if (!isSelected) { setBorderColor(Theme.BLUE); repaint(); } }
            @Override public void mouseExited(MouseEvent e) { if (!isSelected) { setBorderColor(Theme.BORDER_LIGHT); repaint(); } }
        });
    }

    // Backward-compatible constructor (old signature)
    public PlayerCard(String username, int score, boolean isReady) {
        this(username, score, quiz.Rank.fromScore(score).getLabel(), "avatar_01", isReady, false);
    }

    public void setSelected(boolean selected) {
        this.isSelected = selected;
        setBorderColor(selected ? Theme.PURPLE : Theme.BORDER_LIGHT);
        setCardBackground(selected ? new Color(0xF8, 0xF5, 0xFF) : Theme.WHITE);
        repaint();
    }

    public boolean isSelected() { return isSelected; }
    public String getUsername() { return username; }
    public int getScore() { return score; }
    public boolean isReady() { return isReady; }
    public void setOnChallenge(Consumer<String> callback) { this.onChallengeCallback = callback; }
    public void setOnSelect(Consumer<PlayerCard> callback) { this.onSelectCallback = callback; }

    /** Avatar icon hiển thị ảnh tròn */
    private static class AvatarEmojiIcon extends JComponent {
        private final String avatarId;

        AvatarEmojiIcon(String avatarId) {
            this.avatarId = avatarId;
            setPreferredSize(new Dimension(44, 44));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.setupAntiAliasing(g2);
            int size = Math.min(getWidth(), getHeight()) - 2;
            int ox = (getWidth() - size) / 2;
            int oy = (getHeight() - size) / 2;
            AvatarPicker.paintAvatar(g2, avatarId, ox, oy, size);
            g2.dispose();
        }
    }
}

