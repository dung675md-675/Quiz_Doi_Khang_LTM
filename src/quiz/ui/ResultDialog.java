package quiz.ui;

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * ResultDialog - Hộp thoại hiển thị kết quả trận đấu 1v1 hiện đại:
 * - Banner vinh danh kết quả cá nhân với cúp vàng đồ họa Java2D / huân chương hào quang.
 * - Bảng so sánh trực tiếp Head-to-Head 5 chỉ số đối xứng với thanh đo thị giác (Comparison Bar):
 *   + Số câu trả lời đúng (X/10)
 *   + Tổng điểm (pts)
 *   + Điểm thưởng tốc độ (+speed)
 *   + Chuỗi trả lời đúng liên tiếp (Streak)
 *   + Thời gian hoàn thành (giây)
 * - Tùy chọn Tái đấu (REMATCH) với cập nhật trạng thái chờ đối thủ thời gian thực.
 * - Nút "Xem Đáp Án" nổi bật chuyển sang ReviewAnswersDialog đối chiếu kết quả.
 */
public class ResultDialog extends JDialog {

    public record PlayerStat(String name, int correct, long durationMs, boolean isWinner, boolean isMe, String avatarId, int points, int maxStreak, int attackWins, int counterWins, int pointsStolen) {
        public PlayerStat(String name, int correct, long durationMs, boolean isWinner, boolean isMe) {
            this(name, correct, durationMs, isWinner, isMe, "avatar_01", correct * 100, 0, 0, 0, 0);
        }
        public PlayerStat(String name, int correct, long durationMs, boolean isWinner, boolean isMe, String avatarId) {
            this(name, correct, durationMs, isWinner, isMe, avatarId, correct * 100, 0, 0, 0, 0);
        }
        public PlayerStat(String name, int correct, long durationMs, boolean isWinner, boolean isMe, String avatarId, int points, int maxStreak) {
            this(name, correct, durationMs, isWinner, isMe, avatarId, points, maxStreak, 0, 0, 0);
        }
    }

    private final Frame owner;
    private final String currentUsername;
    private final String winner;
    private final String myAvatarId;
    private final String opponentAvatarId;
    private final PlayerStat player1;
    private final PlayerStat player2;
    private final Runnable onRematchYes;
    private final Runnable onRematchNo;

    private List<MatchQuestion> questions;
    private int[] userAnswers;
    private int[] correctAnswers;
    private String[] userFillInAnswers;
    private String[] myOutcomeHistory;
    private String[] oppOutcomeHistory;
    private int[] myDeltaHistory;
    private int[] oppDeltaHistory;
    private boolean actionTaken = false;

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
        this(owner, currentUsername, winner, leftRaw, rightRaw, onRematchYes, onRematchNo, null, null, null, "avatar_01", "avatar_01", null);
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
        this(owner, currentUsername, winner, leftRaw, rightRaw, onRematchYes, onRematchNo, questions, userAnswers, correctAnswers, "avatar_01", "avatar_01", null);
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
            int[] correctAnswers,
            String myAvatarId,
            String opponentAvatarId) {
        this(owner, currentUsername, winner, leftRaw, rightRaw, onRematchYes, onRematchNo, questions, userAnswers, correctAnswers, myAvatarId, opponentAvatarId, null);
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
            int[] correctAnswers,
            String myAvatarId,
            String opponentAvatarId,
            String[] userFillInAnswers) {
        this(owner, currentUsername, winner, leftRaw, rightRaw, onRematchYes, onRematchNo, questions, userAnswers, correctAnswers, myAvatarId, opponentAvatarId, userFillInAnswers, null, null, null, null);
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
            int[] correctAnswers,
            String myAvatarId,
            String opponentAvatarId,
            String[] userFillInAnswers,
            String[] myOutcomeHistory,
            String[] oppOutcomeHistory,
            int[] myDeltaHistory,
            int[] oppDeltaHistory) {

        super(owner, "Kết Quả Trận Đấu — Quiz Arena", true);
        this.owner = owner;
        this.currentUsername = currentUsername != null ? currentUsername : "";
        this.winner = winner != null ? winner.trim() : "";
        this.myAvatarId = myAvatarId != null ? myAvatarId : "avatar_01";
        this.opponentAvatarId = opponentAvatarId != null ? opponentAvatarId : "avatar_01";
        this.onRematchYes = onRematchYes;
        this.onRematchNo = onRematchNo;
        this.questions = questions;
        this.userAnswers = userAnswers;
        this.correctAnswers = correctAnswers;
        this.userFillInAnswers = userFillInAnswers;
        this.myOutcomeHistory = myOutcomeHistory;
        this.oppOutcomeHistory = oppOutcomeHistory;
        this.myDeltaHistory = myDeltaHistory;
        this.oppDeltaHistory = oppDeltaHistory;

        PlayerStat p1 = parseStat(leftRaw, this.winner, this.currentUsername, this.myAvatarId, this.opponentAvatarId);
        PlayerStat p2 = parseStat(rightRaw, this.winner, this.currentUsername, this.myAvatarId, this.opponentAvatarId);

        // Bổ sung dữ liệu đối đầu chiến thuật từ lịch sử câu hỏi nếu server chưa gửi đủ trong raw string
        if (myOutcomeHistory != null) {
            int calcAtk0 = 0, calcCount0 = 0, calcStolen0 = 0;
            int calcAtk1 = 0, calcCount1 = 0, calcStolen1 = 0;
            for (int i = 0; i < myOutcomeHistory.length && i < 10; i++) {
                if ("STEAL_SUCCESS".equals(myOutcomeHistory[i])) {
                    calcAtk0++;
                    calcStolen0 += (myDeltaHistory != null && i < myDeltaHistory.length && myDeltaHistory[i] > 0) ? myDeltaHistory[i] : 50;
                }
                if ("COUNTER_SUCCESS".equals(myOutcomeHistory[i])) {
                    calcCount0++;
                    calcStolen0 += (myDeltaHistory != null && i < myDeltaHistory.length && myDeltaHistory[i] > 0) ? myDeltaHistory[i] : 50;
                }
                if (oppOutcomeHistory != null && i < oppOutcomeHistory.length) {
                    if ("STEAL_SUCCESS".equals(oppOutcomeHistory[i])) {
                        calcAtk1++;
                        calcStolen1 += (oppDeltaHistory != null && i < oppDeltaHistory.length && oppDeltaHistory[i] > 0) ? oppDeltaHistory[i] : 50;
                    }
                    if ("COUNTER_SUCCESS".equals(oppOutcomeHistory[i])) {
                        calcCount1++;
                        calcStolen1 += (oppDeltaHistory != null && i < oppDeltaHistory.length && oppDeltaHistory[i] > 0) ? oppDeltaHistory[i] : 50;
                    }
                }
            }
            if (p1.attackWins() == 0 && p1.counterWins() == 0 && p1.pointsStolen() == 0) {
                if (p1.isMe()) {
                    p1 = new PlayerStat(p1.name(), p1.correct(), p1.durationMs(), p1.isWinner(), p1.isMe(), p1.avatarId(), p1.points(), p1.maxStreak(), calcAtk0, calcCount0, calcStolen0);
                    p2 = new PlayerStat(p2.name(), p2.correct(), p2.durationMs(), p2.isWinner(), p2.isMe(), p2.avatarId(), p2.points(), p2.maxStreak(), calcAtk1, calcCount1, calcStolen1);
                } else {
                    p1 = new PlayerStat(p1.name(), p1.correct(), p1.durationMs(), p1.isWinner(), p1.isMe(), p1.avatarId(), p1.points(), p1.maxStreak(), calcAtk1, calcCount1, calcStolen1);
                    p2 = new PlayerStat(p2.name(), p2.correct(), p2.durationMs(), p2.isWinner(), p2.isMe(), p2.avatarId(), p2.points(), p2.maxStreak(), calcAtk0, calcCount0, calcStolen0);
                }
            }
        }

        this.player1 = p1;
        this.player2 = p2;

        long myMs = this.player1.isMe() ? this.player1.durationMs() : this.player2.durationMs();
        String key1 = this.player1.name() + " " + this.player1.correct() + "–" + this.player2.correct() + " " + this.player2.name();
        String key2 = this.player2.name() + " " + this.player2.correct() + "–" + this.player1.correct() + " " + this.player1.name();
        HistoryCard.recordClientDuration(key1, myMs);
        HistoryCard.recordClientDuration(key2, myMs);

        PlayerStat myStatObj = this.player1.isMe() ? this.player1 : this.player2;
        PlayerStat oppStatObj = this.player1.isMe() ? this.player2 : this.player1;
        MatchHistoryCache.cacheMatch(key1, this.questions, this.userAnswers, this.correctAnswers, this.userFillInAnswers, myStatObj, oppStatObj, this.myOutcomeHistory, this.oppOutcomeHistory, this.myDeltaHistory, this.oppDeltaHistory);
        MatchHistoryCache.cacheMatch(key2, this.questions, this.userAnswers, this.correctAnswers, this.userFillInAnswers, myStatObj, oppStatObj, this.myOutcomeHistory, this.oppOutcomeHistory, this.myDeltaHistory, this.oppDeltaHistory);

        setSize(640, 770);
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

        Color bannerStart = isWin ? Theme.EMERALD : (isDraw ? Theme.INDIGO : Theme.RED);
        Color bannerEnd = isWin ? Theme.EMERALD_DARK : (isDraw ? Theme.PURPLE : new Color(0x9F, 0x12, 0x39));
        String bannerTitle = isWin ? "CHIẾN THẮNG!" : (isDraw ? "TRẬN ĐẤU HÒA!" : "THẤT BẠI!");
        boolean isTieCorrect = (this.player1.correct() == this.player2.correct());
        boolean isPointsTieBreaker = isTieCorrect && !isDraw && (this.player1.points() != this.player2.points());
        int myPts = this.player1.isMe() ? this.player1.points() : this.player2.points();
        int oppPts = this.player1.isMe() ? this.player2.points() : this.player1.points();
        String bannerSubtitle;
        if (isPointsTieBreaker) {
            bannerSubtitle = isWin
                    ? "+2 Điểm Xếp Hạng • Thắng nhờ Điểm Tốc Độ câu đúng (" + myPts + "đ > " + oppPts + "đ)!"
                    : "-1 Điểm Xếp Hạng • Cùng đúng " + this.player1.correct() + " câu, nhưng thua về Điểm Tốc Độ (" + myPts + "đ < " + oppPts + "đ)!";
        } else {
            bannerSubtitle = isWin
                    ? "+2 Điểm Xếp Hạng • Bạn đã thể hiện rất xuất sắc!"
                    : (isDraw ? "0 Điểm Xếp Hạng • Hai đấu thủ ngang tài ngang sức!"
                            : "-1 Điểm Xếp Hạng • Chúc bạn may mắn ở ván sau!");
        }

        JPanel bannerPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setupAntiAliasing(g2);
                int w = getWidth();
                int h = getHeight();
                GradientPaint gp = new GradientPaint(0, 0, bannerStart, w, h, bannerEnd);
                g2.setPaint(gp);
                g2.fillRect(0, 0, w, h);

                // Subtle celebratory background circles
                g2.setColor(new Color(255, 255, 255, 18));
                g2.fillOval(-25, -25, 120, 120);
                g2.fillOval(w - 85, h - 60, 130, 130);
                g2.fillOval(w / 2 - 160, -40, 90, 90);
                g2.dispose();
            }
        };
        bannerPanel.setLayout(new BoxLayout(bannerPanel, BoxLayout.Y_AXIS));
        bannerPanel.setBorder(new EmptyBorder(14, 24, 16, 24));

        // Trophy / Result Emblem Panel (Java2D Vector)
        JPanel emblemPanel = createResultEmblemPanel(isWin, isDraw);
        emblemPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblTitle = new JLabel(bannerTitle, SwingConstants.CENTER);
        lblTitle.setFont(Theme.TITLE_XL);
        lblTitle.setForeground(Theme.WHITE);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblSub = new JLabel(bannerSubtitle, SwingConstants.CENTER);
        lblSub.setFont(Theme.BODY_BOLD);
        lblSub.setForeground(new Color(255, 255, 255, 230));
        lblSub.setAlignmentX(Component.CENTER_ALIGNMENT);

        bannerPanel.add(emblemPanel);
        bannerPanel.add(Box.createVerticalStrut(4));
        bannerPanel.add(lblTitle);
        bannerPanel.add(Box.createVerticalStrut(4));
        bannerPanel.add(lblSub);
        add(bannerPanel, BorderLayout.NORTH);

        // ==========================================
        // 2. CENTER: HEAD-TO-HEAD COMPARISON
        // ==========================================
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);
        centerPanel.setBorder(new EmptyBorder(12, 22, 10, 22));

        ModernCard vsCard = createHeadToHeadCard();
        centerPanel.add(vsCard);
        centerPanel.add(Box.createVerticalStrut(10));

        // Rematch Prompt Box
        ModernCard rematchBox = new ModernCard(new BorderLayout(8, 8));
        rematchBox.setCardBackground(Theme.BG_LIGHT);
        rematchBox.setBorderColor(Theme.BORDER_LIGHT);
        rematchBox.setCardPadding(10, 16, 10, 16);

        lblRematchStatus = new JLabel("Bạn có muốn tái đấu ngay với đối thủ này không?", SwingConstants.CENTER);
        lblRematchStatus.setFont(Theme.BODY_BOLD);
        lblRematchStatus.setForeground(Theme.DARK_TEXT);
        rematchBox.add(lblRematchStatus, BorderLayout.CENTER);

        centerPanel.add(rematchBox);
        add(centerPanel, BorderLayout.CENTER);

        // ==========================================
        // 3. BOTTOM: ACTIONS
        // ==========================================
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 12));
        bottomBar.setBackground(Theme.WHITE);
        bottomBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER_LIGHT));

        // Nút Xem Đáp Án thiết kế nổi bật (Style PRIMARY)
        btnReview = new ModernButton("Xem Đáp Án", ModernButton.Style.PRIMARY);
        btnReview.setPreferredSize(new Dimension(150, 42));
        btnReview.setEnabled(this.questions != null && !this.questions.isEmpty());
        btnReview.addActionListener(e -> {
            SoundManager.playClick();
            openReviewDialog();
        });

        btnLobby = new ModernButton("Về Sảnh Chờ", ModernButton.Style.SECONDARY);
        btnLobby.setPreferredSize(new Dimension(145, 42));
        btnLobby.addActionListener(e -> {
            SoundManager.playClick();
            actionTaken = true;
            dispose();
            if (this.onRematchNo != null) {
                this.onRematchNo.run();
            }
        });

        btnRematch = new ModernButton("Tái Đấu Ngay", ModernButton.Style.SUCCESS);
        btnRematch.setPreferredSize(new Dimension(155, 42));
        btnRematch.addActionListener(e -> {
            SoundManager.playClick();
            btnRematch.setEnabled(false);
            btnRematch.setText("Đã Yêu Cầu...");
            lblRematchStatus.setText("Đã gửi yêu cầu tái đấu! Đang chờ đối thủ đồng ý...");
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
                if (!actionTaken && onRematchNo != null) {
                    actionTaken = true;
                    onRematchNo.run();
                }
            }
        });
    }

    /**
     * Tạo biểu tượng đồ họa Java2D cho banner kết quả:
     * - Thắng: Cúp vàng lớn (Golden Trophy) với hiệu ứng ánh sáng.
     * - Hòa: Biểu tượng Cân bằng / Bắt tay hiện đại.
     * - Thua: Biểu tượng Khiên bạc / Ngôi sao kiên cường.
     */
    private JPanel createResultEmblemPanel(boolean isWin, boolean isDraw) {
        JPanel emblem = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setupAntiAliasing(g2);
                int w = getWidth();
                int h = getHeight();
                int cx = w / 2;
                int cy = h / 2;

                if (isWin) {
                    // 1. Glowing outer halo
                    g2.setColor(new Color(255, 255, 255, 40));
                    g2.fillOval(cx - 26, cy - 25, 52, 52);
                    g2.setColor(new Color(255, 235, 120, 65));
                    g2.fillOval(cx - 20, cy - 19, 40, 40);

                    // 2. Trophy Handles
                    g2.setColor(new Color(0xD9, 0x77, 0x06));
                    g2.setStroke(new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawArc(cx - 19, cy - 14, 11, 13, 90, 180);
                    g2.drawArc(cx + 8, cy - 14, 11, 13, 270, 180);

                    // 3. Cup Bowl
                    int cupW = 22;
                    int cupH = 17;
                    int cupX = cx - cupW / 2;
                    int cupY = cy - 16;
                    RoundRectangle2D cup = new RoundRectangle2D.Float(cupX, cupY, cupW, cupH, 8, 8);
                    GradientPaint goldGp = new GradientPaint(cupX, cupY, new Color(0xFF, 0xEE, 0x55), cupX + cupW, cupY + cupH, new Color(0xF5, 0x9E, 0x0B));
                    g2.setPaint(goldGp);
                    g2.fill(cup);
                    g2.setColor(new Color(0xD9, 0x77, 0x06));
                    g2.setStroke(new BasicStroke(1.2f));
                    g2.draw(cup);

                    // 4. Cup Rim Top
                    g2.setColor(new Color(0xFF, 0xF9, 0xC4));
                    g2.fillRoundRect(cupX - 2, cupY - 2, cupW + 4, 3, 2, 2);

                    // 5. Stem
                    int stemW = 5;
                    int stemH = 6;
                    g2.setColor(new Color(0xD9, 0x77, 0x06));
                    g2.fillRect(cx - stemW / 2, cupY + cupH, stemW, stemH);

                    // 6. Base / Pedestal
                    int baseW = 18;
                    int baseH = 6;
                    int baseX = cx - baseW / 2;
                    int baseY = cupY + cupH + stemH;
                    RoundRectangle2D base = new RoundRectangle2D.Float(baseX, baseY, baseW, baseH, 3, 3);
                    g2.setPaint(new GradientPaint(baseX, baseY, new Color(0xF5, 0x9E, 0x0B), baseX, baseY + baseH, new Color(0xB4, 0x53, 0x09)));
                    g2.fill(base);
                    g2.setColor(new Color(0x92, 0x40, 0x0E));
                    g2.draw(base);

                    // 7. Mini 5-point star on cup bowl
                    drawStar(g2, cx, cupY + cupH / 2, 4.0, 2.0, new Color(255, 255, 255, 240));

                } else if (isDraw) {
                    // Symmetrical balance scales for Draw
                    g2.setColor(new Color(255, 255, 255, 50));
                    g2.fillOval(cx - 20, cy - 20, 40, 40);
                    g2.setColor(Theme.WHITE);
                    g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawLine(cx - 12, cy - 2, cx + 12, cy - 2);
                    g2.drawLine(cx, cy - 12, cx, cy + 10);
                    g2.drawLine(cx - 8, cy + 10, cx + 8, cy + 10);
                    // Two pans
                    g2.drawArc(cx - 15, cy + 2, 6, 6, 0, -180);
                    g2.drawArc(cx + 9, cy + 2, 6, 6, 0, -180);
                } else {
                    // Resilient Shield for Defeat
                    g2.setColor(new Color(255, 255, 255, 45));
                    g2.fillOval(cx - 20, cy - 20, 40, 40);

                    Path2D.Float shield = new Path2D.Float();
                    shield.moveTo(cx - 12, cy - 12);
                    shield.lineTo(cx + 12, cy - 12);
                    shield.lineTo(cx + 12, cy + 2);
                    shield.quadTo(cx, cy + 14, cx, cy + 16);
                    shield.quadTo(cx, cy + 14, cx - 12, cy + 2);
                    shield.closePath();

                    GradientPaint shieldGp = new GradientPaint(cx - 12, cy - 12, new Color(0xE2, 0xE8, 0xF0), cx + 12, cy + 16, new Color(0x94, 0xA3, 0xB8));
                    g2.setPaint(shieldGp);
                    g2.fill(shield);
                    g2.setColor(new Color(0x47, 0x55, 0x69));
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.draw(shield);

                    // Inner star
                    drawStar(g2, cx, cy, 4.5, 2.2, new Color(0xFF, 0xFF, 0xFF, 240));
                }
                g2.dispose();
            }
        };
        emblem.setPreferredSize(new Dimension(64, 46));
        emblem.setMaximumSize(new Dimension(64, 46));
        emblem.setOpaque(false);
        return emblem;
    }

    private static void drawStar(Graphics2D g2, double centerX, double centerY, double outerRadius, double innerRadius, Color color) {
        Path2D.Double path = new Path2D.Double();
        double angle = -Math.PI / 2.0;
        double step = Math.PI / 5.0;
        for (int i = 0; i < 10; i++) {
            double r = (i % 2 == 0) ? outerRadius : innerRadius;
            double x = centerX + Math.cos(angle) * r;
            double y = centerY + Math.sin(angle) * r;
            if (i == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
            angle += step;
        }
        path.closePath();
        g2.setColor(color);
        g2.fill(path);
    }

    /**
     * Bảng đối đầu Head-to-Head hiện đại:
     * - Hàng trên: 2 người chơi (Avatar, Tên, Pill Thắng/Thua) đối xứng qua huy hiệu VS.
     * - Hàng dưới: 5 chỉ số đối đầu trực quan kèm thanh đo thị giác (Comparison Bar) hai chiều:
     *   1. Số câu trả lời đúng (0-10)
     *   2. Tổng điểm trận đấu (pts)
     *   3. Điểm thưởng tốc độ (+pts)
     *   4. Chuỗi đúng lớn nhất (streak)
     *   5. Thời gian hoàn thành (giây)
     */
    private ModernCard createHeadToHeadCard() {
        ModernCard card = new ModernCard(new BorderLayout(0, 10));
        card.setCardBackground(Theme.WHITE);
        card.setBorderColor(Theme.BORDER_LIGHT);
        card.setCardPadding(14, 16, 12, 16);

        // Top: Players Overview with VS in the center
        JPanel playersRow = new JPanel(new BorderLayout(12, 0));
        playersRow.setOpaque(false);

        JPanel p1Summary = createPlayerHeader(player1, true);
        JPanel p2Summary = createPlayerHeader(player2, false);

        // Center VS Badge
        JPanel vsBadgeWrapper = new JPanel(new GridBagLayout());
        vsBadgeWrapper.setOpaque(false);
        JPanel vsCircle = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setupAntiAliasing(g2);
                int size = Math.min(getWidth(), getHeight()) - 2;
                int ox = (getWidth() - size) / 2;
                int oy = (getHeight() - size) / 2;
                GradientPaint gp = new GradientPaint(ox, oy, new Color(0x63, 0x66, 0xF1), ox + size, oy + size, new Color(0x3B, 0x82, 0xF6));
                g2.setPaint(gp);
                g2.fillOval(ox, oy, size, size);
                g2.setColor(Theme.WHITE);
                g2.setFont(Theme.SMALL_BOLD);
                FontMetrics fm = g2.getFontMetrics();
                String text = "VS";
                int tx = ox + (size - fm.stringWidth(text)) / 2;
                int ty = oy + (size - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(text, tx, ty);
                g2.dispose();
            }
        };
        vsCircle.setPreferredSize(new Dimension(32, 32));
        vsCircle.setOpaque(false);
        vsBadgeWrapper.add(vsCircle);

        playersRow.add(p1Summary, BorderLayout.WEST);
        playersRow.add(vsBadgeWrapper, BorderLayout.CENTER);
        playersRow.add(p2Summary, BorderLayout.EAST);
        card.add(playersRow, BorderLayout.NORTH);

        // Metrics Table with Comparison Bars
        JPanel metricsPanel = new JPanel();
        metricsPanel.setLayout(new BoxLayout(metricsPanel, BoxLayout.Y_AXIS));
        metricsPanel.setOpaque(false);
        metricsPanel.setBorder(new EmptyBorder(6, 4, 4, 4));

        // Tính toán các chỉ số
        int spd1 = Math.max(0, player1.points() - player1.correct() * 100);
        int spd2 = Math.max(0, player2.points() - player2.correct() * 100);

        // 1. Số câu trả lời đúng (Correct answers)
        metricsPanel.add(createMetricRow(
                "SỐ CÂU ĐÚNG",
                player1.correct() + " / 10",
                player2.correct() + " / 10",
                player1.correct(),
                player2.correct(),
                10.0,
                true // higher is better
        ));
        metricsPanel.add(Box.createVerticalStrut(6));

        // 2. Tổng điểm (Total points)
        double maxPts = Math.max(100.0, Math.max(player1.points(), player2.points()));
        metricsPanel.add(createMetricRow(
                "TỔNG ĐIỂM",
                player1.points() + " đ",
                player2.points() + " đ",
                player1.points(),
                player2.points(),
                maxPts,
                true
        ));
        metricsPanel.add(Box.createVerticalStrut(6));

        // 3. Điểm thưởng tốc độ (Speed bonus)
        double maxSpd = Math.max(10.0, Math.max(spd1, spd2));
        metricsPanel.add(createMetricRow(
                "ĐIỂM TỐC ĐỘ",
                "+" + spd1 + " đ",
                "+" + spd2 + " đ",
                spd1,
                spd2,
                maxSpd,
                true
        ));
        metricsPanel.add(Box.createVerticalStrut(6));

        // 4. Chuỗi đúng liên tiếp (Streak)
        double maxStrk = Math.max(3.0, Math.max(player1.maxStreak(), player2.maxStreak()));
        metricsPanel.add(createMetricRow(
                "CHUỖI ĐÚNG",
                "Chuỗi " + player1.maxStreak(),
                "Chuỗi " + player2.maxStreak(),
                player1.maxStreak(),
                player2.maxStreak(),
                maxStrk,
                true
        ));
        metricsPanel.add(Box.createVerticalStrut(6));

        // 5. Thời gian hoàn thành (Duration)
        String dur1Text = String.format("%.2fs", player1.durationMs() / 1000.0);
        String dur2Text = String.format("%.2fs", player2.durationMs() / 1000.0);
        double maxDur = Math.max(1000.0, Math.max(player1.durationMs(), player2.durationMs()));
        // Với thời gian: thời gian nhỏ hơn thì thanh đầy hơn
        double invDur1 = (player1.durationMs() > 0) ? Math.max(0, maxDur * 1.15 - player1.durationMs()) : 0;
        double invDur2 = (player2.durationMs() > 0) ? Math.max(0, maxDur * 1.15 - player2.durationMs()) : 0;
        double maxInv = Math.max(1.0, Math.max(invDur1, invDur2));
        metricsPanel.add(createMetricRow(
                "THỜI GIAN",
                dur1Text,
                dur2Text,
                invDur1,
                invDur2,
                maxInv,
                true // invDur1 cao hơn nghĩa là thời gian ít hơn (tốt hơn)
        ));
        metricsPanel.add(Box.createVerticalStrut(6));

        // 6. Số lần Tấn công thành công (Attack wins)
        double maxAtk = Math.max(2.0, Math.max(player1.attackWins(), player2.attackWins()));
        metricsPanel.add(createMetricRow(
                "TẤN CÔNG THÀNH CÔNG",
                player1.attackWins() + " lần",
                player2.attackWins() + " lần",
                player1.attackWins(),
                player2.attackWins(),
                maxAtk,
                true
        ));
        metricsPanel.add(Box.createVerticalStrut(6));

        // 7. Số lần Phản đòn (Counter wins)
        double maxCounter = Math.max(2.0, Math.max(player1.counterWins(), player2.counterWins()));
        metricsPanel.add(createMetricRow(
                "PHẢN ĐÒN",
                player1.counterWins() + " lần",
                player2.counterWins() + " lần",
                player1.counterWins(),
                player2.counterWins(),
                maxCounter,
                true
        ));
        metricsPanel.add(Box.createVerticalStrut(6));

        // 8. Tổng điểm cướp được từ đối thủ (Points stolen)
        double maxStolen = Math.max(50.0, Math.max(player1.pointsStolen(), player2.pointsStolen()));
        metricsPanel.add(createMetricRow(
                "ĐIỂM CƯỚP ĐƯỢC",
                "+" + player1.pointsStolen() + " đ",
                "+" + player2.pointsStolen() + " đ",
                player1.pointsStolen(),
                player2.pointsStolen(),
                maxStolen,
                true
        ));

        card.add(metricsPanel, BorderLayout.CENTER);
        return card;
    }

    private JPanel createPlayerHeader(PlayerStat stat, boolean isLeft) {
        JPanel panel = new JPanel(new FlowLayout(isLeft ? FlowLayout.LEFT : FlowLayout.RIGHT, 10, 0));
        panel.setOpaque(false);
        panel.setPreferredSize(new Dimension(230, 48));

        JPanel avatar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setupAntiAliasing(g2);
                int size = Math.min(getWidth(), getHeight());
                int ox = (getWidth() - size) / 2;
                int oy = (getHeight() - size) / 2;
                AvatarPicker.paintAvatar(g2, stat.avatarId(), ox, oy, size);
                g2.dispose();
            }
        };
        avatar.setPreferredSize(new Dimension(42, 42));
        avatar.setOpaque(false);

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);

        JLabel lblName = new JLabel(stat.name() + (stat.isMe() ? " (Bạn)" : ""));
        lblName.setFont(Theme.SUBTITLE);
        lblName.setForeground(Theme.DARK_TEXT);
        lblName.setAlignmentX(isLeft ? Component.LEFT_ALIGNMENT : Component.RIGHT_ALIGNMENT);

        ModernBadge badge;
        if (stat.isWinner()) {
            badge = new ModernBadge("Thắng Cuộc", ModernBadge.Type.READY);
        } else if ("HÒA".equalsIgnoreCase(winner)) {
            badge = new ModernBadge("Hòa Trận", ModernBadge.Type.INFO);
        } else {
            badge = new ModernBadge("Thua Cuộc", ModernBadge.Type.BUSY);
        }
        badge.setAlignmentX(isLeft ? Component.LEFT_ALIGNMENT : Component.RIGHT_ALIGNMENT);

        info.add(lblName);
        info.add(Box.createVerticalStrut(2));
        info.add(badge);

        if (isLeft) {
            panel.add(avatar);
            panel.add(info);
        } else {
            panel.add(info);
            panel.add(avatar);
        }
        return panel;
    }

    /**
     * Tạo một hàng chỉ số đối đầu đối xứng với:
     * - Trị số bên trái (Player 1)
     * - Nhãn chỉ số ở giữa + Thanh so sánh 2 chiều (Symmetrical Visual Comparison Bar)
     * - Trị số bên phải (Player 2)
     * - Tự động làm nổi bật bên có thành tích tốt hơn
     */
    private JPanel createMetricRow(String title, String valLeft, String valRight, double numLeft, double numRight, double maxVal, boolean higherIsBetter) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setPreferredSize(new Dimension(540, 36));

        boolean isLeftBetter = higherIsBetter ? (numLeft > numRight) : (numLeft < numRight);
        boolean isRightBetter = higherIsBetter ? (numRight > numLeft) : (numRight < numLeft);
        boolean isTie = (numLeft == numRight);

        // 1. Trị số bên trái
        JLabel lblLeft = new JLabel(valLeft, SwingConstants.RIGHT);
        lblLeft.setPreferredSize(new Dimension(105, 34));
        lblLeft.setFont(isLeftBetter ? Theme.BODY_BOLD : Theme.BODY);
        lblLeft.setForeground(isLeftBetter ? Theme.EMERALD_DARK : Theme.TEXT_BODY);
        row.add(lblLeft, BorderLayout.WEST);

        // 2. Trung tâm: Tên chỉ số + Thanh đối đầu trực quan
        JPanel centerCol = new JPanel();
        centerCol.setLayout(new BoxLayout(centerCol, BoxLayout.Y_AXIS));
        centerCol.setOpaque(false);

        JLabel lblTitle = new JLabel(title, SwingConstants.CENTER);
        lblTitle.setFont(Theme.SMALL_BOLD);
        lblTitle.setForeground(Theme.MUTED_GRAY);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Symmetrical Visual Dual Bar
        double rLeft = Math.max(0.08, Math.min(1.0, numLeft / Math.max(0.001, maxVal)));
        double rRight = Math.max(0.08, Math.min(1.0, numRight / Math.max(0.001, maxVal)));
        if (numLeft == 0 && numRight == 0) {
            rLeft = 0.05;
            rRight = 0.05;
        }

        final double finalRLeft = rLeft;
        final double finalRRight = rRight;
        final boolean finalLeftBetter = isLeftBetter;
        final boolean finalRightBetter = isRightBetter;
        final boolean finalTie = isTie;

        JPanel comparisonBar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setupAntiAliasing(g2);
                int w = getWidth();
                int h = getHeight();
                int barH = 7;
                int y = (h - barH) / 2;
                int midX = w / 2;
                int halfW = midX - 8;

                // Trough background
                g2.setColor(new Color(0xF1, 0xF5, 0xF9));
                g2.fillRoundRect(8, y, w - 16, barH, 4, 4);

                // Left Fill (grows from center to left)
                int leftFillW = (int) Math.round(halfW * finalRLeft);
                int leftX = midX - leftFillW;
                Color leftColor = finalLeftBetter ? Theme.EMERALD : (finalTie ? new Color(0x38, 0xBD, 0xF8) : new Color(0xCB, 0xD5, 0xE1));
                g2.setColor(leftColor);
                g2.fillRoundRect(leftX, y, leftFillW, barH, 3, 3);

                // Right Fill (grows from center to right)
                int rightFillW = (int) Math.round(halfW * finalRRight);
                Color rightColor = finalRightBetter ? Theme.PURPLE : (finalTie ? new Color(0x38, 0xBD, 0xF8) : new Color(0xCB, 0xD5, 0xE1));
                g2.setColor(rightColor);
                g2.fillRoundRect(midX, y, rightFillW, barH, 3, 3);

                // Center dividing notch
                g2.setColor(new Color(0x94, 0xA3, 0xB8));
                g2.fillRect(midX - 1, y - 1, 2, barH + 2);

                g2.dispose();
            }
        };
        comparisonBar.setPreferredSize(new Dimension(280, 12));
        comparisonBar.setMaximumSize(new Dimension(320, 12));
        comparisonBar.setAlignmentX(Component.CENTER_ALIGNMENT);
        comparisonBar.setOpaque(false);

        centerCol.add(lblTitle);
        centerCol.add(Box.createVerticalStrut(2));
        centerCol.add(comparisonBar);
        row.add(centerCol, BorderLayout.CENTER);

        // 3. Trị số bên phải
        JLabel lblRight = new JLabel(valRight, SwingConstants.LEFT);
        lblRight.setPreferredSize(new Dimension(105, 34));
        lblRight.setFont(isRightBetter ? Theme.BODY_BOLD : Theme.BODY);
        lblRight.setForeground(isRightBetter ? Theme.PURPLE : Theme.TEXT_BODY);
        row.add(lblRight, BorderLayout.EAST);

        return row;
    }

    private PlayerStat parseStat(String raw, String winner, String currentUsername, String myAvatar, String oppAvatar) {
        if (raw == null || raw.isBlank()) {
            return new PlayerStat("Unknown", 0, 0, false, false, "avatar_01");
        }
        try {
            String[] parts = raw.split(":");
            String name = parts[0].trim();
            String details = parts.length > 1 ? parts[1].trim() : "";

            int correct = 0;
            long durationMs = 0;
            int points = -1;
            int maxStreak = 0;

            int cauIdx = details.indexOf("câu");
            if (cauIdx != -1) {
                correct = Integer.parseInt(details.substring(0, cauIdx).replaceAll("[^0-9]", ""));
            }

            int commaIdx = details.indexOf(",");
            int msIdx = details.indexOf("ms");
            if (commaIdx != -1 && msIdx != -1 && msIdx > commaIdx) {
                durationMs = Long.parseLong(details.substring(commaIdx + 1, msIdx).replaceAll("[^0-9]", ""));
            }

            int ptsIdx = details.indexOf("pts=");
            if (ptsIdx != -1) {
                int endPts = details.indexOf(";", ptsIdx);
                String sub = endPts != -1 ? details.substring(ptsIdx + 4, endPts) : details.substring(ptsIdx + 4);
                try { points = Integer.parseInt(sub.replaceAll("[^0-9]", "")); } catch (Exception ignored) {}
            }

            int strkIdx = details.indexOf("streak=");
            if (strkIdx != -1) {
                int endStrk = details.indexOf(";", strkIdx);
                String sub = endStrk != -1 ? details.substring(strkIdx + 7, endStrk) : details.substring(strkIdx + 7);
                try { maxStreak = Integer.parseInt(sub.replaceAll("[^0-9]", "")); } catch (Exception ignored) {}
            }

            if (points < 0) points = correct * 100;
            if (durationMs <= 0) {
                if (correct > 0) {
                    int estStreakBonus = maxStreak >= 2 ? (maxStreak - 1) * 15 : 0;
                    int speedPts = Math.max(0, points - correct * 100 - estStreakBonus);
                    double avgSpeed = Math.min(48.0, speedPts / (double) correct);
                    double ratio = avgSpeed / 50.0;
                    long avgMsPerQ = Math.round((1.0 - ratio * 0.75) * 4800.0) + 1200L;
                    durationMs = avgMsPerQ * 10L + ((Math.abs(name.hashCode() + points) % 900));
                } else {
                    durationMs = 35000L + (Math.abs(name.hashCode()) % 15000);
                }
            }

            int atkWins = 0;
            int atkWinsIdx = details.indexOf("atkWins=");
            if (atkWinsIdx != -1) {
                int end = details.indexOf(";", atkWinsIdx);
                String sub = end != -1 ? details.substring(atkWinsIdx + 8, end) : details.substring(atkWinsIdx + 8);
                try { atkWins = Integer.parseInt(sub.replaceAll("[^0-9]", "")); } catch (Exception ignored) {}
            }

            int counters = 0;
            int cntIdx = details.indexOf("counters=");
            if (cntIdx != -1) {
                int end = details.indexOf(";", cntIdx);
                String sub = end != -1 ? details.substring(cntIdx + 9, end) : details.substring(cntIdx + 9);
                try { counters = Integer.parseInt(sub.replaceAll("[^0-9]", "")); } catch (Exception ignored) {}
            }

            int stolen = 0;
            int stlIdx = details.indexOf("stolen=");
            if (stlIdx != -1) {
                int end = details.indexOf(";", stlIdx);
                String sub = end != -1 ? details.substring(stlIdx + 7, end) : details.substring(stlIdx + 7);
                try { stolen = Integer.parseInt(sub.replaceAll("[^0-9]", "")); } catch (Exception ignored) {}
            }

            boolean isWin = name.equalsIgnoreCase(winner);
            boolean isMe = name.equalsIgnoreCase(currentUsername);
            String avatarId = isMe ? (myAvatar != null ? myAvatar : "avatar_01") : (oppAvatar != null ? oppAvatar : "avatar_01");
            return new PlayerStat(name, correct, durationMs, isWin, isMe, avatarId, points, maxStreak, atkWins, counters, stolen);
        } catch (Exception e) {
            return new PlayerStat(raw, 0, 0, false, false, "avatar_01");
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

    /**
     * Khi đối thủ rời phòng hoặc từ chối tái đấu trong lúc người chơi đang xem bảng kết quả / đáp án:
     * Không đóng cưỡng bức hộp thoại, chỉ khóa nút Tái Đấu và hiển thị trạng thái.
     */
    public void onOpponentLeft(String message) {
        if (btnRematch != null) {
            btnRematch.setEnabled(false);
            btnRematch.setText("Đối Thủ Đã Rời");
        }
        if (lblRematchStatus != null) {
            lblRematchStatus.setText(message != null && !message.isBlank() ? message : "Đối thủ đã về sảnh chờ.");
            lblRematchStatus.setForeground(Theme.RED);
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
            PlayerStat myStat = this.player1.isMe() ? this.player1 : this.player2;
            PlayerStat oppStat = this.player1.isMe() ? this.player2 : this.player1;
            ReviewAnswersDialog dlg = new ReviewAnswersDialog(this, this.questions, this.userAnswers,
                    this.correctAnswers, this.userFillInAnswers, myStat, oppStat,
                    this.myOutcomeHistory, this.oppOutcomeHistory, this.myDeltaHistory, this.oppDeltaHistory);
            dlg.setVisible(true);
            return dlg;
        }
        return null;
    }

    public void setTacticalData(String[] myOutcomes, String[] oppOutcomes, int[] myDeltas, int[] oppDeltas) {
        this.myOutcomeHistory = myOutcomes;
        this.oppOutcomeHistory = oppOutcomes;
        this.myDeltaHistory = myDeltas;
        this.oppDeltaHistory = oppDeltas;
    }

    public PlayerStat getPlayer1() {
        return player1;
    }

    public PlayerStat getPlayer2() {
        return player2;
    }
}
