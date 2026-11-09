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
 * SubmitConfirmDialog - Hộp thoại xác nhận nộp bài thi đấu phong cách Cyberpunk Dark hiện đại:
 * - Đồng bộ hoàn toàn với thẩm mỹ Dark Glassmorphism của MatchScreen và ResultDialog.
 * - Header gradient bóng bẩy với icon Vector Neon phát sáng.
 * - 3 thẻ thống kê chỉ số: Đã làm, Chưa làm, Tiến độ % dạng thẻ kính thủy tinh tối.
 * - Thanh tiến độ Gradient Neon bo tròn mượt mà.
 * - Lưới ma trận trực quan 10 câu hỏi (Xanh ngọc: đã làm, Vàng cam: chưa làm, Cyan: câu hiện tại).
 *   Người chơi có thể bấm trực tiếp vào ô câu hỏi để chuyển đến câu đó ngay!
 * - Hộp lưu ý / mẹo thi đấu nổi bật.
 * - Các nút thao tác bắt mắt: "TIẾP TỤC LÀM BÀI" và "NỘP BÀI NGAY".
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

        setSize(540, 640);
        setLocationRelativeTo(owner);
        setResizable(false);
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(0x0F, 0x17, 0x2A));

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

                // Nền gradient tối bóng bẩy
                GradientPaint gp = new GradientPaint(
                        0, 0, new Color(0x1E, 0x29, 0x3B),
                        0, h, new Color(0x0F, 0x17, 0x2A));
                g2.setPaint(gp);
                g2.fillRect(0, 0, w, h);

                // Viền Neon phát sáng ở trên đỉnh
                Color neonAccent = isAllAnswered ? new Color(0x10, 0xB9, 0x81) : new Color(0xF5, 0x9E, 0x0B);
                g2.setColor(neonAccent);
                g2.fillRect(0, 0, w, 3);

                // Họa tiết trang trí vòng tròn bán trong suốt
                g2.setColor(new Color(255, 255, 255, 6));
                g2.fillOval(-20, -30, 130, 130);
                g2.fillOval(w - 80, h - 50, 120, 120);

                g2.dispose();
            }
        };
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBorder(new EmptyBorder(16, 20, 16, 20));
        headerPanel.setPreferredSize(new Dimension(540, 138));
        headerPanel.setOpaque(false);

        // 1.1 Icon tròn vector nổi bật
        JPanel iconBadge = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setupAntiAliasing(g2);
                int size = Math.min(getWidth(), getHeight());

                Color accentColor = isAllAnswered ? new Color(0x10, 0xB9, 0x81) : new Color(0xF5, 0x9E, 0x0B);

                // Vầng hào quang phát sáng
                g2.setColor(new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 35));
                g2.fillOval(0, 0, size, size);

                g2.setColor(new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 70));
                g2.fillOval(4, 4, size - 8, size - 8);

                // Nền tròn tối bên trong
                g2.setColor(new Color(0x1E, 0x29, 0x3B));
                g2.fillOval(8, 8, size - 16, size - 16);

                // Viền Neon
                g2.setColor(accentColor);
                g2.setStroke(new BasicStroke(1.8f));
                g2.drawOval(8, 8, size - 16, size - 16);

                // Biểu tượng vector: Tick hoặc Dấu chấm than
                if (isAllAnswered) {
                    g2.setColor(new Color(0x34, 0xD3, 0x99));
                    g2.setStroke(new BasicStroke(3.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawLine(size * 32 / 100, size * 52 / 100, size * 45 / 100, size * 66 / 100);
                    g2.drawLine(size * 45 / 100, size * 66 / 100, size * 68 / 100, size * 36 / 100);
                } else {
                    g2.setColor(new Color(0xFB, 0xBF, 0x24));
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 24));
                    FontMetrics fm = g2.getFontMetrics();
                    String ex = "!";
                    int tx = (size - fm.stringWidth(ex)) / 2;
                    int ty = (size - fm.getHeight()) / 2 + fm.getAscent() - 1;
                    g2.drawString(ex, tx, ty);
                }

                g2.dispose();
            }
        };
        iconBadge.setPreferredSize(new Dimension(50, 50));
        iconBadge.setMaximumSize(new Dimension(50, 50));
        iconBadge.setOpaque(false);
        iconBadge.setAlignmentX(Component.CENTER_ALIGNMENT);

        headerPanel.add(iconBadge);
        headerPanel.add(Box.createVerticalStrut(8));

        // 1.2 Tiêu đề header
        JLabel lblTitle = new JLabel(
                isAllAnswered ? "XÁC NHẬN NỘP BÀI" : "CHƯA HOÀN THÀNH TẤT CẢ CÂU!",
                SwingConstants.CENTER);
        lblTitle.setFont(Theme.TITLE);
        lblTitle.setForeground(new Color(0xF8, 0xFA, 0xFC));
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerPanel.add(lblTitle);
        headerPanel.add(Box.createVerticalStrut(4));

        // 1.3 Phụ đề header
        JLabel lblSubtitle = new JLabel(
                isAllAnswered
                        ? "Bạn đã trả lời đầy đủ 10/10 câu hỏi. Sẵn sàng chốt điểm số!"
                        : "Bạn vẫn còn " + unansweredCount + " câu hỏi chưa chọn đáp án.",
                SwingConstants.CENTER);
        lblSubtitle.setFont(Theme.BODY);
        lblSubtitle.setForeground(new Color(0x94, 0xA3, 0xB8));
        lblSubtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerPanel.add(lblSubtitle);

        add(headerPanel, BorderLayout.NORTH);

        // ==========================================
        // 2. CENTER BODY PANEL (CYBERPUNK DARK)
        // ==========================================
        JPanel bodyPanel = new JPanel();
        bodyPanel.setLayout(new BoxLayout(bodyPanel, BoxLayout.Y_AXIS));
        bodyPanel.setBackground(new Color(0x0F, 0x17, 0x2A));
        bodyPanel.setBorder(new EmptyBorder(14, 22, 14, 22));

        // 2.1 3 THẺ CHỈ SỐ THỐNG KÊ (3 STAT CARDS)
        JPanel statsRow = new JPanel(new GridLayout(1, 3, 10, 0));
        statsRow.setOpaque(false);
        statsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 76));

        // Card 1: ĐÃ LÀM
        JPanel cardAnswered = createStatCard(
                "ĐÃ LÀM",
                answeredCount + "/" + totalCount,
                "câu hỏi",
                new Color(0x06, 0x4E, 0x3B, 140),
                new Color(0x05, 0x96, 0x69, 180),
                new Color(0x6E, 0xE7, 0xB7),
                new Color(0x34, 0xD3, 0x99));

        // Card 2: CHƯA LÀM
        Color unansBg = (unansweredCount > 0) ? new Color(0x45, 0x1A, 0x03, 140) : new Color(0x1E, 0x29, 0x3B, 180);
        Color unansBorder = (unansweredCount > 0) ? new Color(0xD9, 0x77, 0x06, 180) : new Color(0x33, 0x41, 0x55);
        Color unansTitle = (unansweredCount > 0) ? new Color(0xFD, 0xBA, 0x74) : new Color(0x94, 0xA3, 0xB8);
        Color unansVal = (unansweredCount > 0) ? new Color(0xFB, 0xBF, 0x24) : new Color(0x34, 0xD3, 0x99);
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
                new Color(0x0C, 0x4A, 0x6E, 140),
                new Color(0x02, 0x84, 0xC7, 180),
                new Color(0x7D, 0xD3, 0xFC),
                new Color(0x38, 0xBD, 0xF8));

        statsRow.add(cardAnswered);
        statsRow.add(cardUnanswered);
        statsRow.add(cardProgress);
        bodyPanel.add(statsRow);
        bodyPanel.add(Box.createVerticalStrut(14));

        // 2.2 THANH TIẾN ĐỘ GRADIENT NEON
        JPanel progressTrack = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setupAntiAliasing(g2);

                int w = getWidth();
                int h = getHeight();
                int radius = h;

                // Nền track tối
                g2.setColor(new Color(0x1E, 0x29, 0x3B));
                g2.fill(new RoundRectangle2D.Float(0, 0, w, h, radius, radius));
                g2.setColor(new Color(0x33, 0x41, 0x55));
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, radius, radius));

                // Thanh fill gradient rực rỡ
                float fillW = (float) w * (Math.max(0, Math.min(100, progressPercent)) / 100.0f);
                if (fillW > 0) {
                    GradientPaint fillGp = new GradientPaint(
                            0, 0, new Color(0x06, 0xB6, 0xD4),
                            fillW, 0, new Color(0x10, 0xB9, 0x81));
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

        // 2.3 LƯỚI MA TRẬN 10 CÂU HỎI TRỰC QUAN
        JPanel matrixHeader = new JPanel(new BorderLayout());
        matrixHeader.setOpaque(false);
        matrixHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));

        JLabel lblMatrixTitle = new JLabel("Danh sách câu hỏi:");
        lblMatrixTitle.setFont(Theme.BODY_BOLD);
        lblMatrixTitle.setForeground(new Color(0xF1, 0xF5, 0xF9));
        matrixHeader.add(lblMatrixTitle, BorderLayout.WEST);

        JLabel lblMatrixHint = new JLabel("Bấm vào câu để làm tiếp");
        lblMatrixHint.setFont(Theme.SMALL_BOLD);
        lblMatrixHint.setForeground(new Color(0xA7, 0x8B, 0xFA));
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

                Color bg = isAllAnswered ? new Color(0x06, 0x4E, 0x3B, 150) : new Color(0x45, 0x1A, 0x03, 150);
                Color border = isAllAnswered ? new Color(0x05, 0x96, 0x69) : new Color(0xD9, 0x77, 0x06);

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
        lblNoticeIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        lblNoticeIcon.setVerticalAlignment(SwingConstants.TOP);
        noticeCard.add(lblNoticeIcon, BorderLayout.WEST);

        String noticeHtml = isAllAnswered
                ? "<html><div style='line-height: 125%; color: #A7F3D0; font-family: Segoe UI, sans-serif; font-size: 11px;'>"
                + "<b>Mẹo vàng:</b> Khi hai đấu thủ bằng điểm, hệ thống ưu tiên người có <i>tổng thời gian làm bài nhanh hơn</i> để chiến thắng!"
                + "</div></html>"
                : "<html><div style='line-height: 125%; color: #FDE68A; font-family: Segoe UI, sans-serif; font-size: 11px;'>"
                + "<b>Lưu ý quan trọng:</b> Những câu chưa làm sẽ nhận <b>0 điểm</b>. Bạn có thể bấm vào các ô màu cam ở trên để hoàn thiện trước khi nộp."
                + "</div></html>";

        JLabel lblNoticeText = new JLabel(noticeHtml);
        noticeCard.add(lblNoticeText, BorderLayout.CENTER);

        bodyPanel.add(noticeCard);
        add(bodyPanel, BorderLayout.CENTER);

        // ==========================================
        // 3. BOTTOM ACTION FOOTER
        // ==========================================
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        footerPanel.setBackground(new Color(0x0B, 0x11, 0x20));
        footerPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0x1E, 0x29, 0x3B)));

        ModernButton btnCancel = new ModernButton("TIẾP TỤC LÀM BÀI", ModernButton.Style.SECONDARY_SKY);
        btnCancel.setPreferredSize(new Dimension(175, 42));
        btnCancel.setFont(Theme.BODY_BOLD);
        btnCancel.addActionListener(e -> {
            SoundManager.playClick();
            userChoice = Choice.CANCEL;
            dispose();
        });

        ModernButton btnSubmit = new ModernButton(
                isAllAnswered ? "NỘP BÀI NGAY" : "XÁC NHẬN NỘP BÀI",
                ModernButton.Style.EMERALD);
        btnSubmit.setPreferredSize(new Dimension(175, 42));
        btnSubmit.setFont(Theme.BODY_BOLD);
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

            // Nền tối Cyberpunk
            Color bg;
            Color border;
            Color textColor;
            if (isAnswered) {
                bg = isHovered ? new Color(0x06, 0x5F, 0x46) : new Color(0x06, 0x4E, 0x3B);
                border = new Color(0x10, 0xB9, 0x81);
                textColor = new Color(0xD1, 0xFA, 0xE5);
            } else {
                bg = isHovered ? new Color(0x78, 0x35, 0x0F) : new Color(0x45, 0x1A, 0x03);
                border = new Color(0xF5, 0x9E, 0x0B);
                textColor = new Color(0xFE, 0xF3, 0xC7);
            }

            g2.setColor(bg);
            g2.fill(new RoundRectangle2D.Float(0, 0, w - 1, h - 1, radius, radius));

            // Viền
            if (isCurrent) {
                // Viền Cyan Neon nổi bật cho câu hiện tại
                g2.setColor(new Color(0x06, 0xB6, 0xD4));
                g2.setStroke(new BasicStroke(2.2f));
                g2.draw(new RoundRectangle2D.Float(1.2f, 1.2f, w - 2.4f, h - 2.4f, radius, radius));
            } else {
                g2.setColor(border);
                g2.setStroke(new BasicStroke(isHovered ? 1.5f : 1.0f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1.5f, h - 1.5f, radius, radius));
            }

            // Vẽ text nhãn: "Câu 1", "Câu 2"...
            String label = "Câu " + questionNum;
            g2.setFont(Theme.BODY_BOLD);
            g2.setColor(textColor);
            FontMetrics fm = g2.getFontMetrics();
            int tx = (w - fm.stringWidth(label)) / 2;
            int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(label, tx, ty);

            g2.dispose();
        }
    }
}
