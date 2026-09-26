package quiz.ui;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * PlayerCard - Card hiển thị thông tin một người chơi trong sảnh (Lobby):
 * Avatar chữ cái, Tên người dùng, Điểm số, Trạng thái Sẵn sàng / Đang đấu, và nút Thách đấu.
 */
public class PlayerCard extends ModernCard {

    private final String username;
    private final int score;
    private final boolean isReady;
    private boolean isSelected = false;

    private final ModernButton btnChallenge;
    private Consumer<String> onChallengeCallback;
    private Consumer<PlayerCard> onSelectCallback;

    public PlayerCard(String username, int score, boolean isReady) {
        super(new BorderLayout(14, 0));
        this.username = username;
        this.score = score;
        this.isReady = isReady;

        setCardBackground(Theme.WHITE);
        setBorderColor(Theme.BORDER_LIGHT);
        setCornerRadius(12);
        setCardPadding(12, 16, 12, 16);
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 74));
        setPreferredSize(new Dimension(500, 72));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // 1. LEFT: Avatar + Name + Score
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        leftPanel.setOpaque(false);

        // Avatar tròn chứa chữ cái đầu
        AvatarIcon avatar = new AvatarIcon(username);
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
        JLabel lblScore = new JLabel(score + " điểm");
        lblScore.setFont(Theme.SMALL_BOLD);
        lblScore.setForeground(Theme.PURPLE);
        scoreRow.add(lblScore);
        infoPanel.add(scoreRow);

        leftPanel.add(infoPanel);
        add(leftPanel, BorderLayout.WEST);

        // 2. CENTER: Status Badge
        JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 10));
        centerPanel.setOpaque(false);
        ModernBadge statusBadge = new ModernBadge(
            isReady ? "Sẵn sàng" : "Đang đấu",
            isReady ? ModernBadge.Type.READY : ModernBadge.Type.BUSY
        );
        centerPanel.add(statusBadge);
        add(centerPanel, BorderLayout.CENTER);

        // 3. RIGHT: Challenge Button
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

        // Click vào Card để chọn
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (onSelectCallback != null) {
                    onSelectCallback.accept(PlayerCard.this);
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                if (!isSelected) {
                    setBorderColor(Theme.BLUE);
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!isSelected) {
                    setBorderColor(Theme.BORDER_LIGHT);
                    repaint();
                }
            }
        });
    }

    public void setSelected(boolean selected) {
        this.isSelected = selected;
        if (selected) {
            setBorderColor(Theme.PURPLE);
            setCardBackground(new Color(0xF8, 0xF5, 0xFF)); // Màu tím pastel nhẹ khi được chọn
        } else {
            setBorderColor(Theme.BORDER_LIGHT);
            setCardBackground(Theme.WHITE);
        }
        repaint();
    }

    public boolean isSelected() {
        return isSelected;
    }

    public String getUsername() {
        return username;
    }

    public int getScore() {
        return score;
    }

    public boolean isReady() {
        return isReady;
    }

    public void setOnChallenge(Consumer<String> callback) {
        this.onChallengeCallback = callback;
    }

    public void setOnSelect(Consumer<PlayerCard> callback) {
        this.onSelectCallback = callback;
    }

    /**
     * Component vẽ Avatar tròn mang chữ cái đầu của Username
     */
    private static class AvatarIcon extends JComponent {
        private final String letter;
        private final Color bg;

        AvatarIcon(String name) {
            this.letter = (name != null && !name.isEmpty()) ? name.substring(0, 1).toUpperCase() : "?";
            // Chọn màu nền ngẫu nhiên hài hòa theo tên
            int hash = Math.abs(name != null ? name.hashCode() : 0);
            Color[] colors = {Theme.BLUE, Theme.PURPLE, new Color(0x08, 0x91, 0xB2), new Color(0x4F, 0x46, 0xE5)};
            this.bg = colors[hash % colors.length];
            setPreferredSize(new Dimension(42, 42));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.setupAntiAliasing(g2);
            int size = Math.min(getWidth(), getHeight()) - 2;

            g2.setColor(bg);
            g2.fillOval(1, 1, size, size);

            g2.setColor(Theme.WHITE);
            g2.setFont(Theme.TITLE);
            FontMetrics fm = g2.getFontMetrics();
            int tx = (getWidth() - fm.stringWidth(letter)) / 2;
            int ty = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(letter, tx, ty);
            g2.dispose();
        }
    }
}
