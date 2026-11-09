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
        super(new BorderLayout(8, 0));
        this.username = username;
        this.score = score;
        this.isReady = isReady;
        this.isInQueue = isInQueue;

        setCardBackground(ArenaPalette.SURFACE);
        setBorderColor(ArenaPalette.BORDER);
        setHasShadow(false);
        setCornerRadius(18);
        setCardPadding(12, 16, 12, 16);
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 74));
        setPreferredSize(new Dimension(520, 72));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // ==========================================
        // LEFT: Avatar + Tên + Rank + Điểm số
        // ==========================================
        JPanel leftPanel = new JPanel(new BorderLayout(10, 0));
        leftPanel.setOpaque(false);
        leftPanel.setPreferredSize(new Dimension(212, 46));

        // Avatar icon tròn
        AvatarCircleIcon avatar = new AvatarCircleIcon(avatarId);
        leftPanel.add(avatar, BorderLayout.WEST);

        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);

        // Hàng tên người chơi
        JPanel nameRow = new JPanel(new BorderLayout());
        nameRow.setOpaque(false);

        JLabel lblName = new JLabel(username);
        lblName.putClientProperty("html.disable", Boolean.TRUE);
        lblName.setToolTipText(username);
        lblName.setFont(Theme.SUBTITLE);
        lblName.setForeground(ArenaPalette.TEXT);
        nameRow.add(lblName);
        infoPanel.add(nameRow);
        nameRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));

        // Hàng Rank + Điểm số
        JPanel scoreRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        scoreRow.setOpaque(false);

        JLabel lblRank = new JLabel(rankLabel);
        lblRank.setFont(Theme.SMALL_BOLD);
        lblRank.setForeground(ArenaPalette.CYAN);
        scoreRow.add(lblRank);

        JLabel lblDot = new JLabel("•");
        lblDot.setFont(Theme.SMALL);
        lblDot.setForeground(ArenaPalette.MUTED);
        scoreRow.add(lblDot);

        JLabel lblScore = new JLabel(score + " điểm");
        lblScore.setFont(Theme.SMALL_BOLD);
        lblScore.setForeground(ArenaPalette.MUTED);
        scoreRow.add(lblScore);

        infoPanel.add(scoreRow);
        leftPanel.add(infoPanel, BorderLayout.CENTER);
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
        btnChallenge.setArenaStyle(true);
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
                    setCardBackground(ArenaPalette.SURFACE_RAISED);
                    setBorderColor(new Color(64, 87, 118));
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                if (!isSelected) {
                    setCardBackground(ArenaPalette.SURFACE);
                    setBorderColor(ArenaPalette.BORDER);
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
        setBorderColor(selected ? ArenaPalette.ACCENT : (isHovered ? new Color(64, 87, 118) : ArenaPalette.BORDER));
        setCardBackground(selected ? new Color(32, 47, 45) : (isHovered ? ArenaPalette.SURFACE_RAISED : ArenaPalette.SURFACE));
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
                this.bgColor = new Color(24, 57, 49);
                this.borderColor = new Color(45, 93, 74);
                this.textColor = new Color(105, 226, 178);
            } else if (isInQueue) {
                // Vàng cam cho trạng thái Đang tìm
                this.labelText = "Đang tìm";
                this.statusColor = new Color(0xF5, 0x9E, 0x0B);
                this.bgColor = new Color(66, 50, 31);
                this.borderColor = new Color(105, 80, 44);
                this.textColor = new Color(248, 205, 132);
            } else {
                // Cam rực rỡ cho trạng thái Đang thi đấu
                this.labelText = "Đang đấu";
                this.statusColor = new Color(0xF9, 0x73, 0x16);
                this.bgColor = new Color(64, 39, 31);
                this.borderColor = new Color(106, 65, 42);
                this.textColor = new Color(255, 181, 125);
            }

            setFont(Theme.SMALL_BOLD);
            setPreferredSize(new Dimension(90, 28));
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
