package quiz.ui;

import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * PlayerCard — Card người chơi trực tuyến được thiết kế hiện đại với bo góc mềm mại,
 * chấm trạng thái Java2D xanh lá (Rảnh) / cam (Đang thi đấu) với hiệu ứng nhấp nháy (breathing pulse)
 * nhẹ nhàng, hiệu ứng hover mượt mà và nút thách đấu nổi bật.
 */
public class PlayerCard extends ModernCard {

    private final String username;
    private final int score;
    private final boolean isReady;
    private final boolean isInQueue;
    private boolean isSelected = false;
    private boolean isHovered = false;

    private final ModernButton btnChallenge;
    private final StatusIndicatorBadge statusBadge;
    private Consumer<String> onChallengeCallback;
    private Consumer<PlayerCard> onSelectCallback;

    // Bộ điều khiển nhấp nháy trạng thái dùng chung (Shared Pulse Timer)
    private static final Set<PlayerCard> ACTIVE_CARDS = Collections.newSetFromMap(new WeakHashMap<>());
    private static Timer sharedPulseTimer;
    private static float currentPulse = 0.0f; // 0.0f -> 1.0f (dao động theo hàm sin mượt mà)

    public PlayerCard(String username, int score, String rankLabel, String avatarId, boolean isReady, boolean isInQueue) {
        super(new BorderLayout(16, 0));
        this.username = username;
        this.score = score;
        this.isReady = isReady;
        this.isInQueue = isInQueue;

        setCardBackground(Theme.WHITE);
        setBorderColor(Theme.BORDER_LIGHT);
        setCornerRadius(14);
        setCardPadding(12, 16, 12, 16);
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 74));
        setPreferredSize(new Dimension(520, 72));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // ==========================================
        // LEFT: Avatar + Tên + Rank + Điểm số
        // ==========================================
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        leftPanel.setOpaque(false);

        // Avatar icon tròn
        AvatarCircleIcon avatar = new AvatarCircleIcon(avatarId);
        leftPanel.add(avatar);

        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);

        // Hàng tên người chơi
        JPanel nameRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        nameRow.setOpaque(false);

        JLabel lblName = new JLabel(username);
        lblName.setFont(Theme.SUBTITLE);
        lblName.setForeground(Theme.DARK_TEXT);
        nameRow.add(lblName);
        infoPanel.add(nameRow);

        // Hàng Rank + Điểm số
        JPanel scoreRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
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

        // ==========================================
        // CENTER: Chấm trạng thái Java2D & Nhãn trạng thái
        // ==========================================
        JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 10));
        centerPanel.setOpaque(false);

        statusBadge = new StatusIndicatorBadge(isReady, isInQueue);
        centerPanel.add(statusBadge);
        add(centerPanel, BorderLayout.CENTER);

        // ==========================================
        // RIGHT: Nút Thách Đấu
        // ==========================================
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 6));
        rightPanel.setOpaque(false);

        btnChallenge = new ModernButton(
            isReady ? "Thách đấu" : (isInQueue ? "Đang tìm" : "Đang bận"),
            isReady ? ModernButton.Style.EMERALD : ModernButton.Style.OUTLINE_GRAY
        );
        btnChallenge.setPreferredSize(new Dimension(116, 38));
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

        // ==========================================
        // HOVER & CLICK EFFECTS
        // ==========================================
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (onSelectCallback != null) {
                    onSelectCallback.accept(PlayerCard.this);
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                isHovered = true;
                if (!isSelected) {
                    setCardBackground(new Color(0xF8, 0xFA, 0xFC));
                    setBorderColor(new Color(0xC7, 0xD2, 0xFE)); // Viền tím pastel nhẹ
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                if (!isSelected) {
                    setCardBackground(Theme.WHITE);
                    setBorderColor(Theme.BORDER_LIGHT);
                    repaint();
                }
            }
        });

        // Đăng ký thẻ với Shared Pulse Timer khi được gắn vào cây giao diện
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                if (isShowing()) {
                    registerCard(this);
                } else {
                    unregisterCard(this);
                }
            }
        });
    }

    // Constructor tương thích ngược
    public PlayerCard(String username, int score, boolean isReady) {
        this(username, score, quiz.Rank.fromScore(score).getLabel(), "avatar_01", isReady, false);
    }

    public void setSelected(boolean selected) {
        this.isSelected = selected;
        setBorderColor(selected ? Theme.PURPLE : (isHovered ? new Color(0xC7, 0xD2, 0xFE) : Theme.BORDER_LIGHT));
        setCardBackground(selected ? new Color(0xF5, 0xF3, 0xFF) : (isHovered ? new Color(0xF8, 0xFA, 0xFC) : Theme.WHITE));
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

    public boolean isInQueue() {
        return isInQueue;
    }

    public void setOnChallenge(Consumer<String> callback) {
        this.onChallengeCallback = callback;
    }

    public void setOnSelect(Consumer<PlayerCard> callback) {
        this.onSelectCallback = callback;
    }

    // ==========================================
    // QUẢN LÝ SHARED PULSE TIMER (Chạy an toàn trên Swing EDT)
    // ==========================================
    private static synchronized void registerCard(PlayerCard card) {
        ACTIVE_CARDS.add(card);
        if (sharedPulseTimer == null) {
            // Chu kỳ 40ms (~25 FPS), nhẹ nhàng và mượt mà
            sharedPulseTimer = new Timer(40, e -> {
                if (ACTIVE_CARDS.isEmpty()) {
                    sharedPulseTimer.stop();
                    sharedPulseTimer = null;
                    return;
                }
                // Dao động hàm sin tuần hoàn từ 0.0f đến 1.0f (chu kỳ ~1.8s)
                long now = System.currentTimeMillis();
                currentPulse = (float) ((Math.sin(now * 0.0035) + 1.0) / 2.0);

                for (PlayerCard c : ACTIVE_CARDS) {
                    if (c.isShowing()) {
                        c.statusBadge.repaint();
                    }
                }
            });
            sharedPulseTimer.start();
        }
    }

    private static synchronized void unregisterCard(PlayerCard card) {
        ACTIVE_CARDS.remove(card);
        if (ACTIVE_CARDS.isEmpty() && sharedPulseTimer != null) {
            sharedPulseTimer.stop();
            sharedPulseTimer = null;
        }
    }

    /**
     * StatusIndicatorBadge — Vẽ bằng Java2D hình tròn trạng thái:
     * - Xanh lá: Rảnh (isReady = true)
     * - Cam: Đang thi đấu (isReady = false, !isInQueue)
     * - Vàng: Đang tìm trận (isInQueue = true)
     * Có hiệu ứng nhấp nháy nhẹ (breathing halo pulse), không dùng emoji.
     */
    private static class StatusIndicatorBadge extends JComponent {

        private final boolean isReady;
        private final boolean isInQueue;
        private final String labelText;
        private final Color statusColor;
        private final Color bgColor;
        private final Color borderColor;
        private final Color textColor;

        StatusIndicatorBadge(boolean isReady, boolean isInQueue) {
            this.isReady = isReady;
            this.isInQueue = isInQueue;

            if (isReady) {
                // Xanh lá tươi (Emerald) cho trạng thái Rảnh
                this.labelText = "Rảnh";
                this.statusColor = new Color(0x22, 0xC5, 0x5E);
                this.bgColor = new Color(0xEC, 0xFD, 0xF5);
                this.borderColor = new Color(0xA7, 0xF3, 0xD0);
                this.textColor = new Color(0x15, 0x80, 0x3D);
            } else if (isInQueue) {
                // Vàng cam cho trạng thái Đang tìm
                this.labelText = "Đang tìm";
                this.statusColor = new Color(0xF5, 0x9E, 0x0B);
                this.bgColor = new Color(0xFF, 0xFB, 0xEB);
                this.borderColor = new Color(0xFD, 0xE6, 0x8A);
                this.textColor = new Color(0xB4, 0x53, 0x09);
            } else {
                // Cam rực rỡ cho trạng thái Đang thi đấu
                this.labelText = "Đang đấu";
                this.statusColor = new Color(0xF9, 0x73, 0x16);
                this.bgColor = new Color(0xFF, 0xF7, 0xED);
                this.borderColor = new Color(0xFE, 0xD7, 0xAA);
                this.textColor = new Color(0xC2, 0x41, 0x0C);
            }

            setFont(Theme.SMALL_BOLD);
            setPreferredSize(new Dimension(108, 28));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.setupAntiAliasing(g2);

            int w = getWidth();
            int h = getHeight();

            // 1. Nền pill bo góc mềm mại
            g2.setColor(bgColor);
            g2.fill(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1f, h - 1f, 14, 14));

            // Viền pill tinh tế
            g2.setColor(borderColor);
            g2.setStroke(new BasicStroke(1.0f));
            g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1f, h - 1f, 14, 14));

            // 2. VẼ CHẤM TRÒN TRẠNG THÁI JAVA2D VỚI HIỆU ỨNG NHẤP NHÁY (BREATHING PULSE)
            int dotCenterX = 15;
            int dotCenterY = h / 2;
            float coreRadius = 4.0f; // đường kính lõi 8px

            // Hào quang tỏa sáng nhấp nháy êm dịu quanh chấm (không gây chói hay giật)
            float pulse = currentPulse;
            float haloRadius = coreRadius + 1.2f + 2.2f * pulse;
            int haloAlpha = (int) (30 + 55 * pulse);
            Color haloColor = new Color(statusColor.getRed(), statusColor.getGreen(), statusColor.getBlue(), haloAlpha);

            g2.setColor(haloColor);
            g2.fill(new Ellipse2D.Float(dotCenterX - haloRadius, dotCenterY - haloRadius, haloRadius * 2, haloRadius * 2));

            // Chấm tròn lõi nguyên khối sắc nét
            g2.setColor(statusColor);
            g2.fill(new Ellipse2D.Float(dotCenterX - coreRadius, dotCenterY - coreRadius, coreRadius * 2, coreRadius * 2));

            // 3. Chữ trạng thái
            g2.setFont(Theme.SMALL_BOLD);
            g2.setColor(textColor);
            FontMetrics fm = g2.getFontMetrics();
            int textX = 26;
            int textY = dotCenterY + (fm.getAscent() - fm.getDescent()) / 2;
            g2.drawString(labelText, textX, textY);

            g2.dispose();
        }
    }

    /**
     * Avatar circle hiển thị ảnh đại diện bo tròn.
     */
    private static class AvatarCircleIcon extends JComponent {
        private final String avatarId;

        AvatarCircleIcon(String avatarId) {
            this.avatarId = avatarId;
            setPreferredSize(new Dimension(46, 46));
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
