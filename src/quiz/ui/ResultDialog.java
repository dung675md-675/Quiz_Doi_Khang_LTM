package quiz.ui;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * ResultDialog - Hộp thoại hiển thị kết quả trận đấu 1v1 hiện đại:
 * - Banner vinh danh kết quả cá nhân: CHIẾN THẮNG (+2 điểm) / THẤT BẠI (-1
 * điểm) / HÒA (0 điểm).
 * - Bảng so sánh chỉ số trực tiếp Head-to-Head: Số câu đúng (X/10), Thời gian
 * làm bài (giây, ms).
 * - Tùy chọn Tái đấu (REMATCH) với cập nhật trạng thái chờ đối thủ thời gian
 * thực hoặc Về sảnh chờ.
 * - Hỗ trợ tra cứu chi tiết đáp án từng câu (ReviewAnswersDialog).
 */
public class ResultDialog extends JDialog {

    public record PlayerStat(String name, int correct, long durationMs, boolean isWinner, boolean isMe) {
    }

    private final Frame owner;
    private final String currentUsername;
    private final String winner;
    private final PlayerStat player1;
    private final PlayerStat player2;
    private final Runnable onRematchYes;
    private final Runnable onRematchNo;

    private List<MatchQuestion> questions;
    private int[] userAnswers;
    private int[] correctAnswers;

    private final ModernButton btnReview;
    private final ModernButton btnRematch;
    private final ModernButton btnLobby;
    private final JLabel lblRematchStatus;

    public ResultDialog(
            Frame owner,
            String currentUsername,
            String winner,
            String leftRaw,
            String rightRaw,
            Runnable onRematchYes,
            Runnable onRematchNo) {
        this(owner, currentUsername, winner, leftRaw, rightRaw, onRematchYes, onRematchNo, null, null, null);
    }

    public ResultDialog(
            Frame owner,
            String currentUsername,
            String winner,
            String leftRaw,
            String rightRaw,
            Runnable onRematchYes,
            Runnable onRematchNo,
            List<MatchQuestion> questions,
            int[] userAnswers,
            int[] correctAnswers) {

        super(owner, "Kết Quả Trận Đấu — Quiz Arena", true);
        this.owner = owner;
        this.currentUsername = currentUsername != null ? currentUsername : "";
        this.winner = winner != null ? winner.trim() : "";
        this.onRematchYes = onRematchYes;
        this.onRematchNo = onRematchNo;
        this.questions = questions;
        this.userAnswers = userAnswers;
        this.correctAnswers = correctAnswers;

        this.player1 = parseStat(leftRaw, this.winner, this.currentUsername);
        this.player2 = parseStat(rightRaw, this.winner, this.currentUsername);

        setSize(560, 530);
        setLocationRelativeTo(owner);
        setResizable(false);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Theme.WHITE);

        // ==========================================
        // 1. TOP RESULT BANNER
        // ==========================================
        boolean isWin = this.currentUsername.equalsIgnoreCase(this.winner);
        boolean isDraw = "HÒA".equalsIgnoreCase(this.winner);

        if (isWin) {
            SoundManager.playVictory();
        } else if (!isDraw) {
            SoundManager.playDefeat();
        }

        Color bannerBg = isWin ? Theme.GREEN : (isDraw ? Theme.PURPLE : Theme.RED);
        String bannerTitle = isWin ? "🏆 CHIẾN THẮNG!" : (isDraw ? "🤝 TRẬN ĐẤU HÒA!" : "💔 THẤT BẠI!");
        String bannerSubtitle = isWin
                ? "+2 Điểm Xếp Hạng • Bạn đã thể hiện rất xuất sắc!"
                : (isDraw ? "0 Điểm Xếp Hạng • Hai đấu thủ ngang tài ngang sức!"
                        : "-1 Điểm Xếp Hạng • Chúc bạn may mắn ở ván sau!");

        JPanel bannerPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setupAntiAliasing(g2);
                g2.setColor(bannerBg);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        bannerPanel.setLayout(new BoxLayout(bannerPanel, BoxLayout.Y_AXIS));
        bannerPanel.setBorder(new EmptyBorder(20, 24, 20, 24));

        JLabel lblTitle = new JLabel(bannerTitle, SwingConstants.CENTER);
        lblTitle.setFont(Theme.TITLE_XL);
        lblTitle.setForeground(Theme.WHITE);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblSub = new JLabel(bannerSubtitle, SwingConstants.CENTER);
        lblSub.setFont(Theme.BODY_BOLD);
        lblSub.setForeground(new Color(255, 255, 255, 220));
        lblSub.setAlignmentX(Component.CENTER_ALIGNMENT);

        bannerPanel.add(lblTitle);
        bannerPanel.add(Box.createVerticalStrut(6));
        bannerPanel.add(lblSub);
        add(bannerPanel, BorderLayout.NORTH);

        // ==========================================
        // 2. CENTER: HEAD-TO-HEAD COMPARISON
        // ==========================================
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);
        centerPanel.setBorder(new EmptyBorder(20, 24, 16, 24));

        ModernCard vsCard = new ModernCard(new GridLayout(1, 2, 16, 0));
        vsCard.setCardBackground(Theme.OFF_WHITE);
        vsCard.setBorderColor(Theme.BORDER_LIGHT);
        vsCard.setCardPadding(16, 16, 16, 16);

        vsCard.add(createPlayerColumn(player1));
        vsCard.add(createPlayerColumn(player2));
        centerPanel.add(vsCard);
        centerPanel.add(Box.createVerticalStrut(16));

        // Rematch Prompt Box
        ModernCard rematchBox = new ModernCard(new BorderLayout(8, 8));
        rematchBox.setCardBackground(new Color(0xF3, 0xF4, 0xF6));
        rematchBox.setBorderColor(Theme.BORDER_LIGHT);
        rematchBox.setCardPadding(12, 16, 12, 16);

        lblRematchStatus = new JLabel("Bạn có muốn tái đấu ngay với đối thủ này không?", SwingConstants.CENTER);
        lblRematchStatus.setFont(Theme.BODY_BOLD);
        lblRematchStatus.setForeground(Theme.DARK_TEXT);
        rematchBox.add(lblRematchStatus, BorderLayout.CENTER);

        centerPanel.add(rematchBox);
        add(centerPanel, BorderLayout.CENTER);

        // ==========================================
        // 3. BOTTOM: ACTIONS
        // ==========================================
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 14));
        bottomBar.setBackground(Theme.WHITE);
        bottomBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER_LIGHT));

        btnReview = new ModernButton("📝 Xem Đáp Án", ModernButton.Style.SECONDARY);
        btnReview.setPreferredSize(new Dimension(150, 42));
        btnReview.setEnabled(this.questions != null && !this.questions.isEmpty());
        btnReview.addActionListener(e -> {
            SoundManager.playClick();
            openReviewDialog();
        });

        btnLobby = new ModernButton("🏠 Về Sảnh Chờ", ModernButton.Style.SECONDARY);
        btnLobby.setPreferredSize(new Dimension(150, 42));
        btnLobby.addActionListener(e -> {
            SoundManager.playClick();
            dispose();
            if (this.onRematchNo != null) {
                this.onRematchNo.run();
            }
        });

        btnRematch = new ModernButton("⚔️ Tái Đấu Ngay", ModernButton.Style.SUCCESS);
        btnRematch.setPreferredSize(new Dimension(160, 42));
        btnRematch.addActionListener(e -> {
            SoundManager.playClick();
            btnRematch.setEnabled(false);
            btnRematch.setText("⏳ Đã Yêu Cầu...");
            lblRematchStatus.setText("⏳ Đã gửi yêu cầu tái đấu! Đang chờ đối thủ đồng ý...");
            lblRematchStatus.setForeground(Theme.BLUE);
            if (this.onRematchYes != null) {
                this.onRematchYes.run();
            }
        });

        bottomBar.add(btnReview);
        bottomBar.add(btnLobby);
        bottomBar.add(btnRematch);
        add(bottomBar, BorderLayout.SOUTH);

        // Đóng dialog qua nút X thì mặc định coi như từ chối tái đấu
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                if (onRematchNo != null) {
                    onRematchNo.run();
                }
            }
        });
    }

    private JPanel createPlayerColumn(PlayerStat stat) {
        JPanel col = new JPanel();
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        col.setOpaque(false);

        // Avatar + Name
        JPanel top = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        top.setOpaque(false);

        String initial = (!stat.name().isEmpty()) ? stat.name().substring(0, 1).toUpperCase() : "?";
        JPanel avatar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setupAntiAliasing(g2);
                g2.setColor(stat.isMe() ? Theme.BLUE : Theme.PURPLE);
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.setColor(Theme.WHITE);
                g2.setFont(Theme.BODY_BOLD);
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(initial)) / 2;
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(initial, x, y);
                g2.dispose();
            }
        };
        avatar.setPreferredSize(new Dimension(36, 36));
        avatar.setOpaque(false);

        JLabel lblName = new JLabel(stat.name() + (stat.isMe() ? " (Bạn)" : ""));
        lblName.setFont(Theme.SUBTITLE);
        lblName.setForeground(Theme.DARK_TEXT);

        top.add(avatar);
        top.add(lblName);
        col.add(top);
        col.add(Box.createVerticalStrut(8));

        // Result Pill Badge
        JPanel badgeRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        badgeRow.setOpaque(false);
        if (stat.isWinner()) {
            badgeRow.add(new ModernBadge("Thắng Cuộc", ModernBadge.Type.READY));
        } else if ("HÒA".equalsIgnoreCase(winner)) {
            badgeRow.add(new ModernBadge("Hòa Trận", ModernBadge.Type.INFO));
        } else {
            badgeRow.add(new ModernBadge("Thua Cuộc", ModernBadge.Type.BUSY));
        }
        col.add(badgeRow);
        col.add(Box.createVerticalStrut(12));

        // Stat: Correct answers
        JLabel lblScore = new JLabel(stat.correct() + " / 10", SwingConstants.CENTER);
        lblScore.setFont(Theme.TITLE_XL);
        lblScore.setForeground(stat.isWinner() ? Theme.GREEN : Theme.DARK_TEXT);
        lblScore.setAlignmentX(Component.CENTER_ALIGNMENT);
        col.add(lblScore);

        JLabel lblScoreSub = new JLabel("câu trả lời đúng", SwingConstants.CENTER);
        lblScoreSub.setFont(Theme.SMALL);
        lblScoreSub.setForeground(Theme.MUTED_GRAY);
        lblScoreSub.setAlignmentX(Component.CENTER_ALIGNMENT);
        col.add(lblScoreSub);
        col.add(Box.createVerticalStrut(10));

        // Stat: Duration
        String durText = String.format("%.2f giây", stat.durationMs() / 1000.0);
        JLabel lblDuration = new JLabel("⏱️ " + durText, SwingConstants.CENTER);
        lblDuration.setFont(Theme.BODY_BOLD);
        lblDuration.setForeground(Theme.DARK_TEXT);
        lblDuration.setAlignmentX(Component.CENTER_ALIGNMENT);
        col.add(lblDuration);

        return col;
    }

    private PlayerStat parseStat(String raw, String winner, String currentUsername) {
        if (raw == null || raw.isBlank()) {
            return new PlayerStat("Unknown", 0, 0, false, false);
        }
        try {
            String[] parts = raw.split(":");
            String name = parts[0].trim();
            String details = parts.length > 1 ? parts[1].trim() : "";

            int correct = 0;
            long durationMs = 0;

            int cauIdx = details.indexOf("câu");
            if (cauIdx != -1) {
                correct = Integer.parseInt(details.substring(0, cauIdx).replaceAll("[^0-9]", ""));
            }

            int commaIdx = details.indexOf(",");
            int msIdx = details.indexOf("ms");
            if (commaIdx != -1 && msIdx != -1) {
                durationMs = Long.parseLong(details.substring(commaIdx + 1, msIdx).replaceAll("[^0-9]", ""));
            }

            boolean isWin = name.equalsIgnoreCase(winner);
            boolean isMe = name.equalsIgnoreCase(currentUsername);
            return new PlayerStat(name, correct, durationMs, isWin, isMe);
        } catch (Exception e) {
            return new PlayerStat(raw, 0, 0, false, false);
        }
    }

    public void updateWaitingForRematch() {
        if (btnRematch != null) {
            btnRematch.setEnabled(false);
            btnRematch.setText("Đã Yêu Cầu...");
        }
        if (lblRematchStatus != null) {
            lblRematchStatus.setText("Đã gửi yêu cầu tái đấu! Đang chờ đối thủ đồng ý...");
            lblRematchStatus.setForeground(Theme.BLUE);
        }
    }

    public void setReviewData(List<MatchQuestion> questions, int[] userAnswers, int[] correctAnswers) {
        this.questions = questions;
        this.userAnswers = userAnswers;
        this.correctAnswers = correctAnswers;
        if (btnReview != null) {
            btnReview.setEnabled(this.questions != null && !this.questions.isEmpty());
        }
    }

    public ReviewAnswersDialog openReviewDialog() {
        if (this.questions != null && !this.questions.isEmpty()) {
            ReviewAnswersDialog dlg = new ReviewAnswersDialog(this, this.questions, this.userAnswers,
                    this.correctAnswers);
            dlg.setVisible(true);
            return dlg;
        }
        return null;
    }
}
