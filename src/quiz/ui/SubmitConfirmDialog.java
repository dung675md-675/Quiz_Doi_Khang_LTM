package quiz.ui;

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * SubmitConfirmDialog - Hộp thoại xác nhận nộp bài thi đấu hiện đại, bắt mắt:
 * - Header gradient rực rỡ với icon tròn nổi bật và thông báo trạng thái.
 * - 3 thẻ thống kê chỉ số tươi tắn: Đã làm, Chưa làm, Tiến độ %.
 * - Thanh tiến độ Gradient bo tròn mượt mà.
 * - Lưới ma trận trực quan 10 câu hỏi (Xanh lá: đã làm, Cam: chưa làm).
 *   Người chơi có thể bấm trực tiếp vào ô câu hỏi để chuyển đến câu đó ngay!
 * - Hộp lưu ý / mẹo thi đấu nổi bật.
 * - Các nút thao tác màu sắc tươi sáng: Nộp bài ngay (Xanh ngọc) và Tiếp tục làm bài (Xanh da trời).
 */
public class SubmitConfirmDialog extends JDialog {

    public enum Choice {
        SUBMIT,     // Người chơi chọn nộp bài
        CANCEL,     // Người chơi chọn tiếp tục làm bài
        NAVIGATE    // Người chơi bấm vào một câu hỏi cụ thể để chuyển đến câu đó
    }

    private Choice userChoice = Choice.CANCEL;
    private int targetQuestionIndex = -1;

    public SubmitConfirmDialog(
            Window owner,
            List<MatchQuestion> questions,
            int[] selectedAnswers,
            int currentQuestionIndex) {

        super(owner, "Xác Nhận Nộp Bài — Quiz Arena", ModalityType.APPLICATION_MODAL);

        int totalCount = (questions != null && !questions.isEmpty()) ? questions.size() : 10;
        int answeredCount = 0;
        if (selectedAnswers != null) {
            for (int i = 0; i < Math.min(totalCount, selectedAnswers.length); i++) {
                if (selectedAnswers[i] != -1) {
                    answeredCount++;
                }
            }
        }
        int unansweredCount = Math.max(0, totalCount - answeredCount);
        boolean isAllAnswered = (unansweredCount == 0);
        int progressPercent = (totalCount > 0) ? (answeredCount * 100 / totalCount) : 0;

        setSize(530, 620);
        setLocationRelativeTo(owner);
        setResizable(false);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Theme.WHITE);

        // ==========================================
        // 1. TOP GRADIENT HEADER
        // ==========================================
        JPanel headerPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setupAntiAliasing(g2);

                int w = getWidth();
                int h = getHeight();

                // Gradient nền tươi sáng
                Color startColor = isAllAnswered ? Theme.EMERALD : Theme.AMBER;
                Color endColor = isAllAnswered ? Theme.EMERALD_DARK : Theme.ORANGE_DARK;
                GradientPaint gp = new GradientPaint(0, 0, startColor, 0, h, endColor);
                g2.setPaint(gp);
                g2.fillRect(0, 0, w, h);

                // Họa tiết vòng tròn trang trí bán trong suốt
                g2.setColor(new Color(255, 255, 255, 18));
                g2.fillOval(-20, -30, 120, 120);
                g2.fillOval(w - 70, h - 50, 110, 110);
                g2.fillOval(w - 110, -20, 90, 90);

                g2.dispose();
            }
        };
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBorder(new EmptyBorder(16, 20, 16, 20));
        headerPanel.setPreferredSize(new Dimension(530, 135));

        // 1.1 Icon tròn nổi bật ở trên đỉnh
        JPanel iconBadge = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setupAntiAliasing(g2);
                int size = Math.min(getWidth(), getHeight());

                // Vòng phát sáng mờ
                g2.setColor(new Color(255, 255, 255, 50));
                g2.fillOval(0, 0, size, size);

                // Vòng tròn trắng mờ trong
                g2.setColor(new Color(255, 255, 255, 220));
                g2.fillOval(3, 3, size - 6, size - 6);

                // Viền trắng
                g2.setColor(new Color(255, 255, 255, 255));
                g2.setStroke(new BasicStroke(2f));
                g2.drawOval(1, 1, size - 2, size - 2);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        iconBadge.setLayout(new GridBagLayout());
        iconBadge.setPreferredSize(new Dimension(48, 48));
        iconBadge.setMaximumSize(new Dimension(48, 48));
        iconBadge.setOpaque(false);
        iconBadge.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblIcon = new JLabel(isAllAnswered ? "🚀" : "⚠️");
        lblIcon.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 24));
        iconBadge.add(lblIcon);
        headerPanel.add(iconBadge);
        headerPanel.add(Box.createVerticalStrut(8));

        // 1.2 Tiêu đề header
        JLabel lblTitle = new JLabel(
                isAllAnswered ? "XÁC NHẬN NỘP BÀI" : "CHƯA HOÀN THÀNH TẤT CẢ CÂU!",
                SwingConstants.CENTER);
        lblTitle.setFont(Theme.TITLE);
        lblTitle.setForeground(Theme.WHITE);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerPanel.add(lblTitle);
        headerPanel.add(Box.createVerticalStrut(4));

        // 1.3 Phụ đề header
        JLabel lblSubtitle = new JLabel(
                isAllAnswered
                        ? "Bạn đã trả lời đầy đủ toàn bộ câu hỏi. Sẵn sàng chốt điểm!"
                        : "Bạn vẫn còn " + unansweredCount + " câu hỏi chưa chọn đáp án.",
                SwingConstants.CENTER);
        lblSubtitle.setFont(Theme.BODY);
        lblSubtitle.setForeground(isAllAnswered ? new Color(230, 255, 245) : new Color(255, 248, 225));
        lblSubtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerPanel.add(lblSubtitle);

        add(headerPanel, BorderLayout.NORTH);

        // ==========================================
        // 2. CENTER BODY PANEL
        // ==========================================
        JPanel bodyPanel = new JPanel();
        bodyPanel.setLayout(new BoxLayout(bodyPanel, BoxLayout.Y_AXIS));
        bodyPanel.setBackground(Theme.WHITE);
        bodyPanel.setBorder(new EmptyBorder(16, 22, 14, 22));

        // 2.1 3 THẺ CHỈ SỐ THỐNG KÊ (3 QUICK STAT CARDS)
        JPanel statsRow = new JPanel(new GridLayout(1, 3, 10, 0));
        statsRow.setOpaque(false);
        statsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 76));

        // Card 1: ĐÃ LÀM
        JPanel cardAnswered = createStatCard(
                "ĐÃ LÀM",
                answeredCount + "/" + totalCount,
                "câu hỏi",
                Theme.EMERALD_LIGHT,
                new Color(0xA7, 0xF3, 0xD0),
                new Color(0x04, 0x78, 0x57),
                new Color(0x05, 0x96, 0x69));

        // Card 2: CHƯA LÀM
        Color unansBg = (unansweredCount > 0) ? Theme.AMBER_LIGHT : new Color(0xF3, 0xF4, 0xF6);
        Color unansBorder = (unansweredCount > 0) ? new Color(0xFE, 0xD7, 0xAA) : new Color(0xE5, 0xE7, 0xEB);
        Color unansTitle = (unansweredCount > 0) ? new Color(0xC2, 0x41, 0x0C) : new Color(0x6B, 0x72, 0x80);
        Color unansVal = (unansweredCount > 0) ? Theme.ORANGE_DARK : Theme.EMERALD;
        JPanel cardUnanswered = createStatCard(
                "CHƯA LÀM",
                unansweredCount + " câu",
                (unansweredCount > 0 ? "cần trả lời" : "hoàn tất 100%"),
                unansBg,
                unansBorder,
                unansTitle,
                unansVal);

        // Card 3: TIẾN ĐỘ
        JPanel cardProgress = createStatCard(
                "TIẾN ĐỘ",
                progressPercent + "%",
                "toàn bộ bài thi",
                Theme.SKY_LIGHT,
                new Color(0xBF, 0xDB, 0xFE),
                new Color(0x1E, 0x40, 0xAF),
                new Color(0x25, 0x63, 0xEB));

        statsRow.add(cardAnswered);
        statsRow.add(cardUnanswered);
        statsRow.add(cardProgress);
        bodyPanel.add(statsRow);
        bodyPanel.add(Box.createVerticalStrut(14));

        // 2.2 THANH TIẾN ĐỘ GRADIENT (GRADIENT PROGRESS BAR)
        JPanel progressTrack = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setupAntiAliasing(g2);

                int w = getWidth();
                int h = getHeight();
                int radius = h;

                // Nền track xám nhạt
                g2.setColor(new Color(0xE2, 0xE8, 0xF0));
                g2.fill(new RoundRectangle2D.Float(0, 0, w, h, radius, radius));

                // Thanh fill gradient rực rỡ
                float fillW = (float) w * (Math.max(0, Math.min(100, progressPercent)) / 100.0f);
                if (fillW > 0) {
                    GradientPaint fillGp = new GradientPaint(
                            0, 0,
                            isAllAnswered ? Theme.CYAN : Theme.AMBER,
                            fillW, 0,
                            Theme.EMERALD);
                    g2.setPaint(fillGp);
                    g2.fill(new RoundRectangle2D.Float(0, 0, fillW, h, radius, radius));
                }

                g2.dispose();
            }
        };
        progressTrack.setPreferredSize(new Dimension(Integer.MAX_VALUE, 12));
        progressTrack.setMaximumSize(new Dimension(Integer.MAX_VALUE, 12));
        progressTrack.setOpaque(false);
        bodyPanel.add(progressTrack);
        bodyPanel.add(Box.createVerticalStrut(14));

        // 2.3 LƯỚI MA TRẬN 10 CÂU HỎI TRỰC QUAN (QUESTION STATUS MATRIX)
        JPanel matrixHeader = new JPanel(new BorderLayout());
        matrixHeader.setOpaque(false);
        matrixHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));

        JLabel lblMatrixTitle = new JLabel("Danh sách câu hỏi:");
        lblMatrixTitle.setFont(Theme.BODY_BOLD);
        lblMatrixTitle.setForeground(Theme.DARK_TEXT);
        matrixHeader.add(lblMatrixTitle, BorderLayout.WEST);

        JLabel lblMatrixHint = new JLabel("👆 Bấm vào câu để làm tiếp");
        lblMatrixHint.setFont(Theme.SMALL_BOLD);
        lblMatrixHint.setForeground(Theme.PURPLE);
        matrixHeader.add(lblMatrixHint, BorderLayout.EAST);

        bodyPanel.add(matrixHeader);
        bodyPanel.add(Box.createVerticalStrut(8));

        // Lưới các câu hỏi 2 hàng x 5 cột
        JPanel questionGrid = new JPanel(new GridLayout(2, 5, 8, 8));
        questionGrid.setOpaque(false);
        questionGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 86));

        for (int i = 0; i < totalCount; i++) {
            final int qIndex = i;
            boolean isAns = (selectedAnswers != null && i < selectedAnswers.length && selectedAnswers[i] != -1);
            boolean isCurrent = (i == currentQuestionIndex);

            QuestionChip chip = new QuestionChip(
                    qIndex + 1,
                    isAns,
                    isCurrent,
                    () -> {
                        SoundManager.playClick();
                        userChoice = Choice.NAVIGATE;
                        targetQuestionIndex = qIndex;
                        dispose();
                    });
            questionGrid.add(chip);
        }
        bodyPanel.add(questionGrid);
        bodyPanel.add(Box.createVerticalStrut(14));

        // 2.4 HỘP THÔNG BÁO / LỜI KHUYÊN (CALLOUT NOTICE CARD)
        JPanel noticeCard = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setupAntiAliasing(g2);
                int w = getWidth();
                int h = getHeight();

                Color bg = isAllAnswered ? Theme.EMERALD_LIGHT : Theme.AMBER_LIGHT;
                Color border = isAllAnswered ? new Color(0xA7, 0xF3, 0xD0) : new Color(0xFD, 0xE6, 0x8A);

                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, w - 1, h - 1, 12, 12));

                g2.setColor(border);
                g2.setStroke(new BasicStroke(1.2f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 2, h - 2, 12, 12));

                g2.dispose();
            }
        };
        noticeCard.setLayout(new BorderLayout(10, 0));
        noticeCard.setBorder(new EmptyBorder(10, 14, 10, 14));
        noticeCard.setOpaque(false);
        noticeCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 65));

        JLabel lblNoticeIcon = new JLabel(isAllAnswered ? "💡" : "⚠️");
        lblNoticeIcon.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 20));
        lblNoticeIcon.setVerticalAlignment(SwingConstants.TOP);
        noticeCard.add(lblNoticeIcon, BorderLayout.WEST);

        String noticeHtml = isAllAnswered
                ? "<html><b>Mẹo vàng:</b> Khi hai đấu thủ bằng điểm, hệ thống ưu tiên người có <i>tổng thời gian nộp bài nhanh hơn</i> để chiến thắng!</html>"
                : "<html><b>Lưu ý quan trọng:</b> Những câu chưa làm sẽ nhận <b>0 điểm</b>. Bạn có thể bấm vào các ô màu cam ở trên để trả lời nốt trước khi nộp.</html>";

        JLabel lblNoticeText = new JLabel(noticeHtml);
        lblNoticeText.setFont(Theme.SMALL);
        lblNoticeText.setForeground(isAllAnswered ? new Color(0x06, 0x5F, 0x46) : new Color(0x92, 0x40, 0x0E));
        noticeCard.add(lblNoticeText, BorderLayout.CENTER);

        bodyPanel.add(noticeCard);
        add(bodyPanel, BorderLayout.CENTER);

        // ==========================================
        // 3. BOTTOM ACTION FOOTER
        // ==========================================
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        footerPanel.setBackground(new Color(0xF8, 0xFA, 0xFC));
        footerPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0xE2, 0xE8, 0xF0)));

        ModernButton btnCancel = new ModernButton("✏️ TIẾP TỤC LÀM BÀI", ModernButton.Style.SECONDARY_SKY);
        btnCancel.setPreferredSize(new Dimension(185, 42));
        btnCancel.addActionListener(e -> {
            SoundManager.playClick();
            userChoice = Choice.CANCEL;
            dispose();
        });

        ModernButton btnSubmit = new ModernButton(
                isAllAnswered ? "🚀 NỘP BÀI NGAY" : "⚡ VẪN NỘP BÀI",
                ModernButton.Style.EMERALD);
        btnSubmit.setPreferredSize(new Dimension(165, 42));
        btnSubmit.addActionListener(e -> {
            SoundManager.playClick();
            userChoice = Choice.SUBMIT;
            dispose();
        });

        footerPanel.add(btnCancel);
        footerPanel.add(btnSubmit);
        add(footerPanel, BorderLayout.SOUTH);

        // Phím tắt bàn phím tiện lợi
        KeyAdapter escEnterAdapter = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    userChoice = Choice.CANCEL;
                    dispose();
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    userChoice = Choice.SUBMIT;
                    dispose();
                }
            }
        };
        addKeyListener(escEnterAdapter);
        btnCancel.addKeyListener(escEnterAdapter);
        btnSubmit.addKeyListener(escEnterAdapter);
    }

    public Choice getUserChoice() {
        return userChoice;
    }

    public boolean isConfirmed() {
        return userChoice == Choice.SUBMIT;
    }

    public int getTargetQuestionIndex() {
        return targetQuestionIndex;
    }

    // ==========================================
    // HELPER: TẠO THẺ THỐNG KÊ (STAT CARD)
    // ==========================================
    private JPanel createStatCard(
            String title,
            String value,
            String subtext,
            Color bg,
            Color borderColor,
            Color titleColor,
            Color valueColor) {

        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setupAntiAliasing(g2);
                int w = getWidth();
                int h = getHeight();

                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, w - 1, h - 1, 12, 12));

                g2.setColor(borderColor);
                g2.setStroke(new BasicStroke(1.2f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 2, h - 2, 12, 12));

                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(8, 10, 8, 10));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(Theme.SMALL_BOLD);
        lblTitle.setForeground(titleColor);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblValue = new JLabel(value);
        lblValue.setFont(new Font(Theme.BODY_BOLD.getFamily(), Font.BOLD, 18));
        lblValue.setForeground(valueColor);
        lblValue.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblSub = new JLabel(subtext);
        lblSub.setFont(new Font(Theme.SMALL.getFamily(), Font.PLAIN, 10));
        lblSub.setForeground(titleColor);
        lblSub.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(lblTitle);
        card.add(Box.createVerticalStrut(2));
        card.add(lblValue);
        card.add(Box.createVerticalStrut(1));
        card.add(lblSub);

        return card;
    }

    // ==========================================
    // HELPER: QUESTION CHIP (BUTTON CHO TỪNG CÂU)
    // ==========================================
    private static class QuestionChip extends JComponent {
        private final int questionNum;
        private final boolean isAnswered;
        private final boolean isCurrent;
        private final Runnable onClick;
        private boolean isHovered = false;

        public QuestionChip(int questionNum, boolean isAnswered, boolean isCurrent, Runnable onClick) {
            this.questionNum = questionNum;
            this.isAnswered = isAnswered;
            this.isCurrent = isCurrent;
            this.onClick = onClick;

            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setFont(Theme.BODY_BOLD);
            setToolTipText(isAnswered
                    ? "Câu " + questionNum + ": Đã trả lời. Bấm để xem lại."
                    : "Câu " + questionNum + ": CHƯA TRẢ LỜI! Bấm để chuyển tới câu này.");

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    isHovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    isHovered = false;
                    repaint();
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    if (onClick != null) {
                        onClick.run();
                    }
                }
            });
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(80, 36);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.setupAntiAliasing(g2);

            int w = getWidth();
            int h = getHeight();
            int radius = 8;

            // Xác định màu nền
            Color bg;
            if (isAnswered) {
                bg = isHovered ? Theme.EMERALD_DARK : Theme.EMERALD;
            } else {
                bg = isHovered ? Theme.ORANGE_DARK : Theme.ORANGE;
            }

            g2.setColor(bg);
            g2.fill(new RoundRectangle2D.Float(0, 0, w - 1, h - 1, radius, radius));

            // Nếu là câu hiện tại đang đứng, vẽ viền nổi bật (vàng kim / tím)
            if (isCurrent) {
                g2.setColor(new Color(0xFC, 0xD3, 0x4D));
                g2.setStroke(new BasicStroke(2.5f));
                g2.draw(new RoundRectangle2D.Float(1.5f, 1.5f, w - 3, h - 3, radius, radius));
            } else if (isHovered) {
                g2.setColor(new Color(255, 255, 255, 180));
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, radius, radius));
            }

            // Vẽ text nhãn: "✓ Câu 1" hoặc "✕ Câu 3"
            String label = (isAnswered ? "✓ C." : "✕ C.") + questionNum;
            g2.setFont(Theme.BODY_BOLD);
            g2.setColor(Theme.WHITE);
            FontMetrics fm = g2.getFontMetrics();
            int tx = (w - fm.stringWidth(label)) / 2;
            int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(label, tx, ty);

            g2.dispose();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            int[] demoAnswers = new int[]{0, 1, -1, 3, 2, -1, 0, 1, 2, -1};
            SubmitConfirmDialog dialog = new SubmitConfirmDialog(null, null, demoAnswers, 2);
            dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            dialog.setVisible(true);
            System.out.println("Choice: " + dialog.getUserChoice() + ", Target: " + dialog.getTargetQuestionIndex());
        });
    }
}
