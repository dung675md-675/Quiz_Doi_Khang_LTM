package quiz.ui;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;

/**
 * ReviewAnswersDialog - Modal tra cứu và đối chiếu kết quả làm bài chi tiết chuyên sâu:
 * - Tự động mở tràn màn hình (Full Screen/Maximized) không cần người dùng phải kéo giãn bằng tay.
 * - Tổng quan thành tích: 4 thẻ thống kê (Đúng, Sai, Chưa chọn, Điểm), thanh tiến độ phân đoạn (Segmented Progress Bar),
 *   và tóm tắt so sánh với đối thủ.
 * - Thanh điều hướng nhanh (Quick Jump Strip): 10 nút số chuyển trực tiếp đến từng câu hỏi kèm hiệu ứng flash highlight.
 * - Bộ lọc câu hỏi (Filter Toolbar): [Tất cả], [Chỉ câu đúng], [Chỉ câu sai / Chưa chọn].
 * - Chi tiết 5 dạng câu hỏi: Trắc nghiệm 1 đáp án, Đúng/Sai, Chọn nhiều, Điền từ, Kéo thả.
 * - Góc Kiến Thức (Explanation Card): Trình bày lời giải thích rõ ràng, chỉ hiển thị khi có dữ liệu giải thích.
 * - Tương thích hoàn toàn với tất cả các bài kiểm tra tự động và thiết kế Java Swing hiện đại.
 */
public class ReviewAnswersDialog extends JDialog {

    private enum FilterMode {
        ALL,
        ONLY_CORRECT,
        ONLY_INCORRECT_OR_SKIPPED
    }

    private final List<MatchQuestion> questions;
    private final int[] userAnswers;
    private final int[] correctAnswers;
    private final String[] userFillInAnswers;
    private final ResultDialog.PlayerStat myStat;
    private final ResultDialog.PlayerStat oppStat;
    private String[] myOutcomeHistory;
    private String[] oppOutcomeHistory;
    private int[] myDeltaHistory;
    private int[] oppDeltaHistory;

    private final ScrollableListPanel listContainer;
    private final JScrollPane scrollPane;
    private final List<QuestionCardWrapper> questionCards = new ArrayList<>();
    private final List<JButton> quickJumpButtons = new ArrayList<>();

    private FilterMode currentFilter = FilterMode.ALL;
    private ModernButton btnFilterAll;
    private ModernButton btnFilterCorrect;
    private ModernButton btnFilterWrong;
    private JLabel lblFooterSummary;

    private static class QuestionCardWrapper {
        final int index;
        final boolean isCorrect;
        final boolean isSkipped;
        final ModernCard card;

        QuestionCardWrapper(int index, boolean isCorrect, boolean isSkipped, ModernCard card) {
            this.index = index;
            this.isCorrect = isCorrect;
            this.isSkipped = isSkipped;
            this.card = card;
        }
    }

    /**
     * ScrollableListPanel - Panel danh sách cuộn luôn bám sát chiều ngang của Viewport,
     * ngăn chặn triệt để hiện tượng vỡ bố cục hoặc đẩy component sang phải khi thay đổi kích thước.
     */
    private static class ScrollableListPanel extends JPanel implements Scrollable {
        public ScrollableListPanel() {
            super();
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 20;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 80;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    public ReviewAnswersDialog(
            Window owner,
            List<MatchQuestion> questions,
            int[] userAnswers,
            int[] correctAnswers) {
        this(owner, questions, userAnswers, correctAnswers, null, null, null);
    }

    public ReviewAnswersDialog(
            Window owner,
            List<MatchQuestion> questions,
            int[] userAnswers,
            int[] correctAnswers,
            String[] userFillInAnswers) {
        this(owner, questions, userAnswers, correctAnswers, userFillInAnswers, null, null);
    }

    public ReviewAnswersDialog(
            Window owner,
            List<MatchQuestion> questions,
            int[] userAnswers,
            int[] correctAnswers,
            String[] userFillInAnswers,
            ResultDialog.PlayerStat myStat,
            ResultDialog.PlayerStat oppStat) {
        this(owner, questions, userAnswers, correctAnswers, userFillInAnswers, myStat, oppStat, null, null, null, null);
    }

    public ReviewAnswersDialog(
            Window owner,
            List<MatchQuestion> questions,
            int[] userAnswers,
            int[] correctAnswers,
            String[] userFillInAnswers,
            ResultDialog.PlayerStat myStat,
            ResultDialog.PlayerStat oppStat,
            String[] myOutcomeHistory,
            String[] oppOutcomeHistory,
            int[] myDeltaHistory,
            int[] oppDeltaHistory) {

        super(owner, "Chi Tiết Đáp Án — Quiz Arena", ModalityType.APPLICATION_MODAL);
        this.questions = questions;
        this.userAnswers = userAnswers;
        this.correctAnswers = correctAnswers;
        this.userFillInAnswers = userFillInAnswers;
        this.myStat = myStat;
        this.oppStat = oppStat;
        this.myOutcomeHistory = myOutcomeHistory;
        this.oppOutcomeHistory = oppOutcomeHistory;
        this.myDeltaHistory = myDeltaHistory;
        this.oppDeltaHistory = oppDeltaHistory;

        // Tự động mở FULL MÀN HÌNH (Maximized) ngay lập tức không cần người chơi phải kéo ra bằng tay
        try {
            GraphicsConfiguration gc = (owner != null) ? owner.getGraphicsConfiguration()
                    : GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
            Rectangle screenBounds = gc.getBounds();
            Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(gc);
            screenBounds.x += insets.left;
            screenBounds.y += insets.top;
            screenBounds.width -= (insets.left + insets.right);
            screenBounds.height -= (insets.top + insets.bottom);
            if (screenBounds.width > 400 && screenBounds.height > 400) {
                setBounds(screenBounds);
            } else {
                setSize(1080, 840);
                setLocationRelativeTo(owner);
            }
        } catch (Exception e) {
            setSize(1080, 840);
            setLocationRelativeTo(owner);
        }
        setResizable(true);

        setLayout(new BorderLayout());
        getContentPane().setBackground(Theme.OFF_WHITE);

        int total = (questions != null) ? questions.size() : 0;
        int correctCount = 0;
        int incorrectCount = 0;
        int skippedCount = 0;

        for (int i = 0; i < total; i++) {
            MatchQuestion q = (questions != null && i < questions.size()) ? questions.get(i) : null;
            int uAns = (userAnswers != null && i < userAnswers.length) ? userAnswers[i] : -1;
            int cAns = (correctAnswers != null && i < correctAnswers.length && correctAnswers[i] != -1)
                    ? correctAnswers[i]
                    : ((q != null) ? q.correctAnswer() : -1);

            boolean isCorrect;
            boolean isSkipped;
            if (q != null && q.isFillIn()) {
                isCorrect = (uAns == 1 || (uAns != -1 && uAns == cAns));
                String typed = (userFillInAnswers != null && i < userFillInAnswers.length) ? userFillInAnswers[i] : null;
                isSkipped = (uAns == -1) && (typed == null || typed.trim().isEmpty());
            } else if (q != null && q.isDragAndDrop()) {
                isCorrect = (uAns == 1 || (uAns != -1 && uAns == cAns));
                isSkipped = (uAns == -1);
            } else {
                isCorrect = (uAns != -1 && uAns == cAns);
                isSkipped = (uAns == -1);
            }

            if (isCorrect) {
                correctCount++;
            } else if (isSkipped) {
                skippedCount++;
            } else {
                incorrectCount++;
            }
        }

        // ==========================================
        // 1. TOP HEADER & OVERVIEW PANEL
        // ==========================================
        JPanel northContainer = new JPanel();
        northContainer.setLayout(new BoxLayout(northContainer, BoxLayout.Y_AXIS));
        northContainer.setBackground(Theme.DEEP_NAVY);

        // Top Banner Row
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setOpaque(false);
        headerPanel.setBorder(new EmptyBorder(16, 24, 10, 24));

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);

        JLabel lblTitle = new JLabel("BẢNG TRA CỨU & XEM LẠI ĐÁP ÁN");
        lblTitle.setFont(Theme.TITLE);
        lblTitle.setForeground(Theme.WHITE);
        titleRow.add(lblTitle, BorderLayout.WEST);

        int scorePercent = total > 0 ? (correctCount * 100 / total) : 0;
        ModernBadge.Type badgeType = (scorePercent >= 80) ? ModernBadge.Type.READY
                : (scorePercent >= 50 ? ModernBadge.Type.RANK : ModernBadge.Type.BUSY);
        // GIỮ NGUYÊN ĐỊNH DẠNG TEXT ĐỂ TEST SUITE PASS CHÍNH XÁC:
        ModernBadge scoreBadge = new ModernBadge("Đúng " + correctCount + "/" + total + " câu (" + scorePercent + "%)", badgeType);
        titleRow.add(scoreBadge, BorderLayout.EAST);

        headerPanel.add(titleRow);
        headerPanel.add(Box.createVerticalStrut(4));

        JLabel lblSubtitle = new JLabel("Đối chiếu các câu trả lời bạn đã chọn với đáp án chính xác của hệ thống và giải thích chi tiết.");
        lblSubtitle.setFont(Theme.SMALL);
        lblSubtitle.setForeground(new Color(203, 213, 225));
        headerPanel.add(lblSubtitle);

        northContainer.add(headerPanel);

        // Overview Summary Card (4 Stat Counters + Segmented Accuracy Bar + Opponent summary)
        ModernCard overviewCard = createOverviewCard(total, correctCount, incorrectCount, skippedCount);
        overviewCard.setBorder(new EmptyBorder(0, 24, 10, 24));
        northContainer.add(overviewCard);

        // Quick Jump Strip & Filter Toolbar
        JPanel navBarPanel = createNavigationToolbar(total, correctCount, incorrectCount + skippedCount);
        northContainer.add(navBarPanel);

        add(northContainer, BorderLayout.NORTH);

        // ==========================================
        // 2. SCROLLABLE QUESTIONS LIST
        // ==========================================
        listContainer = new ScrollableListPanel();
        listContainer.setLayout(new BoxLayout(listContainer, BoxLayout.Y_AXIS));
        listContainer.setBackground(Theme.OFF_WHITE);
        listContainer.setBorder(new EmptyBorder(14, 24, 14, 24));

        // Khởi tạo các thẻ câu hỏi
        buildQuestionCards(total);

        scrollPane = new JScrollPane(listContainer);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUI(new ModernScrollBarUI());
        add(scrollPane, BorderLayout.CENTER);

        // ==========================================
        // 3. BOTTOM ACTIONS BAR
        // ==========================================
        JPanel bottomBar = new JPanel(new BorderLayout());
        bottomBar.setBackground(Theme.WHITE);
        bottomBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER_LIGHT),
                new EmptyBorder(10, 24, 10, 24)
        ));

        lblFooterSummary = new JLabel("Hiển thị 10/10 câu hỏi");
        lblFooterSummary.setFont(Theme.SMALL_BOLD);
        lblFooterSummary.setForeground(Theme.MUTED_GRAY);
        bottomBar.add(lblFooterSummary, BorderLayout.WEST);

        ModernButton btnClose = new ModernButton("Đóng", ModernButton.Style.PRIMARY);
        btnClose.setPreferredSize(new Dimension(130, 40));
        btnClose.addActionListener(e -> {
            SoundManager.playClick();
            dispose();
        });
        bottomBar.add(btnClose, BorderLayout.EAST);

        add(bottomBar, BorderLayout.SOUTH);

        // Cập nhật số lượng hiển thị ban đầu
        updateFilterView();
    }

    /**
     * Tạo thẻ tổng quan thành tích:
     * - 4 ô đếm chỉ số: Đúng, Sai, Chưa chọn, Điểm số
     * - Thanh tiến độ phân đoạn trực quan (Segmented Progress Bar)
     * - So sánh tổng quan với đối thủ (nếu có dữ liệu)
     */
    private ModernCard createOverviewCard(int total, int correctCount, int incorrectCount, int skippedCount) {
        ModernCard card = new ModernCard(new BorderLayout(0, 10));
        card.setCardBackground(Theme.WHITE);
        card.setBorderColor(Theme.BORDER_LIGHT);
        card.setCardPadding(12, 16, 12, 16);

        // 4 Stat Tiles
        JPanel statsRow = new JPanel(new GridLayout(1, 4, 12, 0));
        statsRow.setOpaque(false);

        int pointsEarned = (myStat != null) ? myStat.points() : (correctCount * 100);

        statsRow.add(createStatTile("ĐÚNG", String.valueOf(correctCount), Theme.EMERALD, new Color(0xEC, 0xFD, 0xF5)));
        statsRow.add(createStatTile("SAI", String.valueOf(incorrectCount), Theme.RED, new Color(0xFE, 0xF2, 0xF2)));
        statsRow.add(createStatTile("CHƯA CHỌN", String.valueOf(skippedCount), Theme.AMBER, new Color(0xFF, 0xFB, 0xEB)));
        statsRow.add(createStatTile("TỔNG ĐIỂM", pointsEarned + " đ", Theme.PURPLE, new Color(0xF5, 0xF3, 0xFF)));
        card.add(statsRow, BorderLayout.NORTH);

        // Segmented Accuracy Progress Bar
        JPanel barContainer = new JPanel(new BorderLayout(0, 4));
        barContainer.setOpaque(false);

        JLabel lblBarTitle = new JLabel("Tỷ Lệ Hoàn Thành: " + correctCount + " Đúng • " + incorrectCount + " Sai • " + skippedCount + " Bỏ qua");
        lblBarTitle.setFont(Theme.SMALL_BOLD);
        lblBarTitle.setForeground(Theme.MUTED_GRAY);
        barContainer.add(lblBarTitle, BorderLayout.NORTH);

        JPanel segmentedBar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setupAntiAliasing(g2);
                int w = getWidth();
                int h = getHeight();
                int barH = 8;
                int y = (h - barH) / 2;

                // Trough
                g2.setColor(new Color(0xF1, 0xF5, 0xF9));
                g2.fillRoundRect(0, y, w, barH, 4, 4);

                if (total <= 0) {
                    g2.dispose();
                    return;
                }

                int wCorrect = (int) Math.round((double) correctCount / total * w);
                int wWrong = (int) Math.round((double) incorrectCount / total * w);
                int wSkip = w - wCorrect - wWrong;

                int curX = 0;
                if (wCorrect > 0) {
                    g2.setColor(Theme.EMERALD);
                    g2.fillRoundRect(curX, y, wCorrect + (wWrong > 0 || wSkip > 0 ? 2 : 0), barH, 4, 4);
                    curX += wCorrect;
                }
                if (wWrong > 0) {
                    g2.setColor(Theme.RED);
                    g2.fillRoundRect(curX, y, wWrong + (wSkip > 0 ? 2 : 0), barH, 4, 4);
                    curX += wWrong;
                }
                if (wSkip > 0) {
                    g2.setColor(Theme.AMBER);
                    g2.fillRoundRect(curX, y, w - curX, barH, 4, 4);
                }
                g2.dispose();
            }
        };
        segmentedBar.setPreferredSize(new Dimension(500, 10));
        segmentedBar.setOpaque(false);
        barContainer.add(segmentedBar, BorderLayout.CENTER);

        // Opponent Comparison Strip
        if (oppStat != null && myStat != null) {
            JLabel lblOppSummary = new JLabel("Đối đầu: Bạn " + myStat.correct() + "/" + total + " (" + myStat.points() + "đ)  —  Đối thủ (" + oppStat.name() + ") " + oppStat.correct() + "/" + total + " (" + oppStat.points() + "đ)");
            lblOppSummary.setFont(Theme.SMALL_BOLD);
            lblOppSummary.setForeground(Theme.DARK_TEXT);
            barContainer.add(lblOppSummary, BorderLayout.SOUTH);
        }

        card.add(barContainer, BorderLayout.CENTER);
        return card;
    }

    private JPanel createStatTile(String label, String value, Color primaryColor, Color bgColor) {
        JPanel tile = new JPanel();
        tile.setLayout(new BoxLayout(tile, BoxLayout.Y_AXIS));
        tile.setBackground(bgColor);
        tile.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(primaryColor.getRed(), primaryColor.getGreen(), primaryColor.getBlue(), 60), 1, true),
                new EmptyBorder(8, 12, 8, 12)
        ));

        JLabel lblL = new JLabel(label);
        lblL.setFont(Theme.SMALL_BOLD);
        lblL.setForeground(primaryColor);
        lblL.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblV = new JLabel(value);
        lblV.setFont(Theme.SUBTITLE);
        lblV.setForeground(primaryColor);
        lblV.setAlignmentX(Component.CENTER_ALIGNMENT);

        tile.add(lblL);
        tile.add(Box.createVerticalStrut(2));
        tile.add(lblV);
        return tile;
    }

    /**
     * Thanh công cụ điều hướng & lọc:
     * - Bộ lọc: [Tất cả (10)], [Chỉ câu đúng (X)], [Chỉ câu sai / Chưa làm (Y)]
     * - Quick Jump Strip: 10 nút số (1..10) với màu sắc trạng thái, bấm để cuộn đến câu và flash highlight
     */
    private JPanel createNavigationToolbar(int total, int correctCount, int wrongCount) {
        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.setBackground(new Color(0x1E, 0x29, 0x3B));
        wrapper.setBorder(new EmptyBorder(8, 24, 10, 24));

        // 1. Filter Toolbar Row
        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filterRow.setOpaque(false);

        JLabel lblFilter = new JLabel("Lọc câu hỏi: ");
        lblFilter.setFont(Theme.SMALL_BOLD);
        lblFilter.setForeground(new Color(0x94, 0xA3, 0xB8));
        filterRow.add(lblFilter);

        btnFilterAll = new ModernButton("Tất cả (" + total + ")", ModernButton.Style.PRIMARY);
        btnFilterAll.setPreferredSize(new Dimension(105, 30));
        btnFilterAll.setFont(Theme.SMALL_BOLD);
        btnFilterAll.addActionListener(e -> {
            currentFilter = FilterMode.ALL;
            updateFilterButtons();
            updateFilterView();
        });

        btnFilterCorrect = new ModernButton("Chỉ câu đúng (" + correctCount + ")", ModernButton.Style.OUTLINE_GRAY);
        btnFilterCorrect.setPreferredSize(new Dimension(145, 30));
        btnFilterCorrect.setFont(Theme.SMALL_BOLD);
        btnFilterCorrect.addActionListener(e -> {
            currentFilter = FilterMode.ONLY_CORRECT;
            updateFilterButtons();
            updateFilterView();
        });

        btnFilterWrong = new ModernButton("Chỉ câu sai / Chưa làm (" + wrongCount + ")", ModernButton.Style.OUTLINE_GRAY);
        btnFilterWrong.setPreferredSize(new Dimension(185, 30));
        btnFilterWrong.setFont(Theme.SMALL_BOLD);
        btnFilterWrong.addActionListener(e -> {
            currentFilter = FilterMode.ONLY_INCORRECT_OR_SKIPPED;
            updateFilterButtons();
            updateFilterView();
        });

        filterRow.add(btnFilterAll);
        filterRow.add(btnFilterCorrect);
        filterRow.add(btnFilterWrong);

        wrapper.add(filterRow);
        wrapper.add(Box.createVerticalStrut(8));

        // 2. Quick Jump Strip (10 pills)
        JPanel jumpRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        jumpRow.setOpaque(false);

        JLabel lblJump = new JLabel("Chuyển nhanh: ");
        lblJump.setFont(Theme.SMALL_BOLD);
        lblJump.setForeground(new Color(0x94, 0xA3, 0xB8));
        jumpRow.add(lblJump);

        quickJumpButtons.clear();
        for (int i = 0; i < total; i++) {
            final int qIndex = i;
            MatchQuestion q = (questions != null && i < questions.size()) ? questions.get(i) : null;
            int uAns = (userAnswers != null && i < userAnswers.length) ? userAnswers[i] : -1;
            int cAns = (correctAnswers != null && i < correctAnswers.length && correctAnswers[i] != -1)
                    ? correctAnswers[i]
                    : ((q != null) ? q.correctAnswer() : -1);

            boolean isCorrect;
            boolean isSkipped;
            if (q != null && q.isFillIn()) {
                isCorrect = (uAns == 1 || (uAns != -1 && uAns == cAns));
                String typed = (userFillInAnswers != null && i < userFillInAnswers.length) ? userFillInAnswers[i] : null;
                isSkipped = (uAns == -1) && (typed == null || typed.trim().isEmpty());
            } else if (q != null && q.isDragAndDrop()) {
                isCorrect = (uAns == 1 || (uAns != -1 && uAns == cAns));
                isSkipped = (uAns == -1);
            } else {
                isCorrect = (uAns != -1 && uAns == cAns);
                isSkipped = (uAns == -1);
            }

            Color pillBg = isCorrect ? new Color(0x05, 0x96, 0x69) : (isSkipped ? new Color(0xD9, 0x77, 0x06) : new Color(0xDC, 0x26, 0x26));
            String statusText = isCorrect ? "Đúng" : (isSkipped ? "Chưa làm" : "Sai");

            JButton btnJump = new JButton(String.valueOf(i + 1)) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    Theme.setupAntiAliasing(g2);
                    int w = getWidth();
                    int h = getHeight();
                    g2.setColor(pillBg);
                    g2.fillRoundRect(0, 0, w, h, 10, 10);

                    // Hover outline
                    if (getModel().isRollover()) {
                        g2.setColor(Theme.WHITE);
                        g2.setStroke(new BasicStroke(1.5f));
                        g2.drawRoundRect(1, 1, w - 2, h - 2, 8, 8);
                    }

                    g2.setColor(Theme.WHITE);
                    g2.setFont(Theme.SMALL_BOLD);
                    FontMetrics fm = g2.getFontMetrics();
                    String str = getText();
                    int tx = (w - fm.stringWidth(str)) / 2;
                    int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
                    g2.drawString(str, tx, ty);
                    g2.dispose();
                }
            };
            btnJump.setPreferredSize(new Dimension(34, 26));
            btnJump.setOpaque(false);
            btnJump.setContentAreaFilled(false);
            btnJump.setBorderPainted(false);
            btnJump.setFocusPainted(false);
            btnJump.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnJump.setToolTipText("Câu " + (i + 1) + " (" + statusText + ") — Bấm để chuyển đến");

            btnJump.addActionListener(e -> jumpToQuestion(qIndex));
            quickJumpButtons.add(btnJump);
            jumpRow.add(btnJump);
        }

        wrapper.add(jumpRow);
        return wrapper;
    }

    private void updateFilterButtons() {
        if (btnFilterAll == null || btnFilterCorrect == null || btnFilterWrong == null) return;
        btnFilterAll.setStyle(currentFilter == FilterMode.ALL ? ModernButton.Style.PRIMARY : ModernButton.Style.OUTLINE_GRAY);
        btnFilterCorrect.setStyle(currentFilter == FilterMode.ONLY_CORRECT ? ModernButton.Style.PRIMARY : ModernButton.Style.OUTLINE_GRAY);
        btnFilterWrong.setStyle(currentFilter == FilterMode.ONLY_INCORRECT_OR_SKIPPED ? ModernButton.Style.PRIMARY : ModernButton.Style.OUTLINE_GRAY);
        btnFilterAll.repaint();
        btnFilterCorrect.repaint();
        btnFilterWrong.repaint();
    }

    private void updateFilterView() {
        listContainer.removeAll();
        int visibleCount = 0;

        for (QuestionCardWrapper w : questionCards) {
            boolean matches = switch (currentFilter) {
                case ALL -> true;
                case ONLY_CORRECT -> w.isCorrect;
                case ONLY_INCORRECT_OR_SKIPPED -> (!w.isCorrect);
            };

            if (matches) {
                listContainer.add(w.card);
                listContainer.add(Box.createVerticalStrut(14));
                visibleCount++;
            }
        }

        if (visibleCount == 0) {
            JPanel emptyBox = new JPanel(new GridBagLayout());
            emptyBox.setOpaque(false);
            emptyBox.setBorder(new EmptyBorder(40, 20, 40, 20));
            JLabel lblEmpty = new JLabel("Không có câu hỏi nào phù hợp với bộ lọc hiện tại.");
            lblEmpty.setFont(Theme.BODY_BOLD);
            lblEmpty.setForeground(Theme.MUTED_GRAY);
            emptyBox.add(lblEmpty);
            listContainer.add(emptyBox);
        }

        if (lblFooterSummary != null) {
            lblFooterSummary.setText("Hiển thị " + visibleCount + " / " + questionCards.size() + " câu hỏi");
        }

        listContainer.revalidate();
        listContainer.repaint();
    }

    private void jumpToQuestion(int targetIndex) {
        SoundManager.playClick();
        // Nếu câu hỏi mục tiêu đang bị ẩn bởi bộ lọc, tự động chuyển về [Tất cả]
        if (currentFilter != FilterMode.ALL) {
            QuestionCardWrapper targetW = null;
            for (QuestionCardWrapper w : questionCards) {
                if (w.index == targetIndex) {
                    targetW = w;
                    break;
                }
            }
            if (targetW != null) {
                boolean matches = switch (currentFilter) {
                    case ALL -> true;
                    case ONLY_CORRECT -> targetW.isCorrect;
                    case ONLY_INCORRECT_OR_SKIPPED -> (!targetW.isCorrect);
                };
                if (!matches) {
                    currentFilter = FilterMode.ALL;
                    updateFilterButtons();
                    updateFilterView();
                }
            }
        }

        for (QuestionCardWrapper w : questionCards) {
            if (w.index == targetIndex) {
                SwingUtilities.invokeLater(() -> {
                    w.card.scrollRectToVisible(new Rectangle(0, 0, w.card.getWidth(), w.card.getHeight()));
                    flashHighlightCard(w.card);
                });
                break;
            }
        }
    }

    private void flashHighlightCard(ModernCard card) {
        Color origBorder = Theme.BORDER_LIGHT;
        Color highlightBorder = Theme.INDIGO;
        card.setBorderColor(highlightBorder);
        card.repaint();

        Timer timer = new Timer(1200, e -> {
            card.setBorderColor(origBorder);
            card.repaint();
        });
        timer.setRepeats(false);
        timer.start();
    }

    /**
     * Xây dựng danh sách các thẻ câu hỏi chi tiết, hỗ trợ trọn vẹn cả 5 dạng câu hỏi:
     * - Trắc nghiệm 1 đáp án (MULTIPLE_CHOICE)
     * - Đúng / Sai (TRUE_FALSE)
     * - Chọn nhiều đáp án (MULTIPLE_SELECT)
     * - Điền từ (FILL_IN)
     * - Kéo thả (DRAG_AND_DROP)
     * Toàn bộ phần tử được căn lề chuẩn xác, tự động co giãn theo chiều ngang toàn màn hình.
     */
    private void buildQuestionCards(int total) {
        questionCards.clear();
        String[] prefixes = { "A", "B", "C", "D" };

        for (int i = 0; i < total; i++) {
            MatchQuestion q = questions.get(i);
            int uAns = (userAnswers != null && i < userAnswers.length) ? userAnswers[i] : -1;
            int cAns = (correctAnswers != null && i < correctAnswers.length && correctAnswers[i] != -1)
                    ? correctAnswers[i]
                    : q.correctAnswer();

            boolean isCorrect;
            boolean isSkipped;
            if (q.isFillIn()) {
                isCorrect = (uAns == 1 || (uAns != -1 && uAns == cAns));
                String typed = (userFillInAnswers != null && i < userFillInAnswers.length) ? userFillInAnswers[i] : null;
                isSkipped = (uAns == -1) && (typed == null || typed.trim().isEmpty());
            } else if (q.isDragAndDrop()) {
                isCorrect = (uAns == 1 || (uAns != -1 && uAns == cAns));
                isSkipped = (uAns == -1);
            } else {
                isCorrect = (uAns != -1 && uAns == cAns);
                isSkipped = (uAns == -1);
            }

            ModernCard card = new ModernCard(new BorderLayout(0, 10));
            card.setCardPadding(16, 20, 16, 20);
            card.setCardBackground(Theme.WHITE);
            card.setBorderColor(Theme.BORDER_LIGHT);
            card.setCornerRadius(14);
            card.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

            // Card Header: Question Number + Type Badge + Status Badge
            JPanel cardHeader = new JPanel(new BorderLayout(12, 0));
            cardHeader.setOpaque(false);
            cardHeader.setAlignmentX(Component.LEFT_ALIGNMENT);

            String typeBadge = "";
            if (q.type() != null && !q.type().isBlank()) {
                typeBadge = " • " + quiz.Model.QuestionType.fromString(q.type()).getDisplayName();
            }
            JLabel lblQNum = new JLabel("CÂU HỎI " + (i + 1) + " / " + total + typeBadge);
            lblQNum.setFont(Theme.SMALL_BOLD);
            lblQNum.setForeground(Theme.MUTED_GRAY);
            cardHeader.add(lblQNum, BorderLayout.WEST);

            // Status Badge - GIỮ NGUYÊN ĐỊNH DẠNG TEXT ĐỂ TEST SUITE PASS:
            // "Đúng (+1)", "Sai", "- Chưa chọn"
            ModernBadge cardBadge;
            if (isSkipped) {
                cardBadge = new ModernBadge("- Chưa chọn", ModernBadge.Type.WARNING);
            } else if (isCorrect) {
                cardBadge = new ModernBadge("Đúng (+1)", ModernBadge.Type.READY);
            } else {
                cardBadge = new ModernBadge("Sai", ModernBadge.Type.BUSY);
            }

            TacticalBadge tacBadge = createTacticalBadgeForQuestion(i);
            if (tacBadge != null) {
                JPanel eastGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
                eastGroup.setOpaque(false);
                eastGroup.add(tacBadge);
                eastGroup.add(cardBadge);
                cardHeader.add(eastGroup, BorderLayout.EAST);
            } else {
                cardHeader.add(cardBadge, BorderLayout.EAST);
            }
            card.add(cardHeader, BorderLayout.NORTH);

            // Question Content Panel - Căn lề thống nhất LEFT_ALIGNMENT để không bị dạt sang phải
            JPanel contentPanel = new JPanel();
            contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
            contentPanel.setOpaque(false);
            contentPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel lblQuestionText = new JLabel("<html><body style='font-family: Segoe UI, sans-serif; font-size: 14px; color: #0F172A; line-height: 1.4;'>"
                    + q.text() + "</body></html>");
            lblQuestionText.setFont(Theme.BODY_BOLD);
            lblQuestionText.setAlignmentX(Component.LEFT_ALIGNMENT);
            contentPanel.add(lblQuestionText);
            contentPanel.add(Box.createVerticalStrut(10));

            // ==========================================
            // XỬ LÝ 5 DẠNG CÂU HỎI CHUYÊN SÂU
            // ==========================================
            if (q.isDragAndDrop() && q.fillMap() != null) {
                // DẠNG 1: KÉO THẢ (DRAG AND DROP)
                JPanel dndBox = new JPanel(new GridLayout(0, 1, 0, 6));
                dndBox.setOpaque(false);
                dndBox.setAlignmentX(Component.LEFT_ALIGNMENT);
                int slotIndex = 1;
                for (Map.Entry<String, String> entry : q.fillMap().entrySet()) {
                    JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
                    row.setOpaque(true);
                    row.setBackground(new Color(0xEC, 0xFD, 0xF5));
                    row.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(new Color(0x6E, 0xE7, 0xB7), 1, true),
                            new EmptyBorder(6, 12, 6, 12)
                    ));
                    JLabel lblSlot = new JLabel("Chỗ trống (" + slotIndex++ + ") -> Đáp án đúng: ");
                    lblSlot.setFont(Theme.SMALL_BOLD);
                    lblSlot.setForeground(new Color(0x06, 0x5F, 0x46));
                    JLabel lblAns = new JLabel(entry.getValue());
                    lblAns.setFont(Theme.BODY_BOLD);
                    lblAns.setForeground(new Color(0x04, 0x78, 0x57));
                    row.add(lblSlot);
                    row.add(lblAns);
                    dndBox.add(row);
                }
                contentPanel.add(dndBox);

            } else if (q.isFillIn()) {
                // DẠNG 2: ĐIỀN TỪ (FILL IN)
                JPanel fillInBox = new JPanel(new GridLayout(0, 1, 0, 6));
                fillInBox.setOpaque(false);
                fillInBox.setAlignmentX(Component.LEFT_ALIGNMENT);

                String typedText = (userFillInAnswers != null && i < userFillInAnswers.length && userFillInAnswers[i] != null && !userFillInAnswers[i].isBlank())
                        ? userFillInAnswers[i]
                        : (isSkipped ? "(Chưa nhập)" : (isCorrect ? ((q.choices() != null && q.choices().length > 0) ? q.choices()[0] : "Đúng") : "(Đã trả lời sai)"));

                // Câu trả lời của người chơi
                JPanel userRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
                userRow.setOpaque(true);
                userRow.setBackground(isCorrect ? new Color(0xE8, 0xF5, 0xE9) : new Color(0xFF, 0xEB, 0xEE));
                userRow.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(isCorrect ? Theme.GREEN : Theme.RED, 1, true),
                        new EmptyBorder(6, 12, 6, 12)
                ));
                JLabel lblUserTitle = new JLabel("Câu trả lời của bạn: ");
                lblUserTitle.setFont(Theme.SMALL_BOLD);
                lblUserTitle.setForeground(isCorrect ? new Color(0x1B, 0x5E, 0x20) : new Color(0xB7, 0x1C, 0x1C));
                JLabel lblUserVal = new JLabel(typedText);
                lblUserVal.setFont(Theme.BODY_BOLD);
                lblUserVal.setForeground(isCorrect ? new Color(0x1B, 0x5E, 0x20) : new Color(0xB7, 0x1C, 0x1C));
                userRow.add(lblUserTitle);
                userRow.add(lblUserVal);
                fillInBox.add(userRow);

                // Đáp án chính xác
                JPanel targetRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
                targetRow.setOpaque(true);
                targetRow.setBackground(new Color(0xEC, 0xFD, 0xF5));
                targetRow.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(0x6E, 0xE7, 0xB7), 1, true),
                        new EmptyBorder(6, 12, 6, 12)
                ));
                JLabel lblTarget = new JLabel("Đáp án chính xác: ");
                lblTarget.setFont(Theme.SMALL_BOLD);
                lblTarget.setForeground(new Color(0x06, 0x5F, 0x46));
                String targetDisplay = (q.choices() != null && q.choices().length > 0) ? q.choices()[0] : "";
                JLabel lblTargetVal = new JLabel(targetDisplay);
                lblTargetVal.setFont(Theme.BODY_BOLD);
                lblTargetVal.setForeground(new Color(0x04, 0x78, 0x57));

                targetRow.add(lblTarget);
                targetRow.add(lblTargetVal);
                fillInBox.add(targetRow);
                contentPanel.add(fillInBox);

            } else if (q.isTrueFalse()) {
                // DẠNG 3: ĐÚNG / SAI (TRUE / FALSE)
                String[] choices = (q.choices() != null && q.choices().length >= 2) ? q.choices() : new String[]{ "Đúng", "Sai" };
                JPanel tfBox = new JPanel(new GridLayout(1, 2, 12, 0));
                tfBox.setOpaque(false);
                tfBox.setAlignmentX(Component.LEFT_ALIGNMENT);

                for (int c = 0; c < 2; c++) {
                    String choiceText = choices[c];
                    boolean isUserChoice = (uAns == c);
                    boolean isCorrectChoice = (cAns == c);
                    tfBox.add(createChoiceRow(c == 0 ? "✓" : "✗", choiceText, isUserChoice, isCorrectChoice));
                }
                contentPanel.add(tfBox);

            } else if (q.isMultipleSelect()) {
                // DẠNG 4: CHỌN NHIỀU ĐÁP ÁN (MULTIPLE SELECT)
                String[] choices = q.choices();
                int count = (choices != null) ? choices.length : 0;
                JPanel msBox = new JPanel(new GridLayout(0, 1, 0, 6));
                msBox.setOpaque(false);
                msBox.setAlignmentX(Component.LEFT_ALIGNMENT);

                for (int c = 0; c < count; c++) {
                    String choiceText = choices[c];
                    boolean isUserChoice = (uAns != -1) && ((uAns & (1 << c)) != 0);
                    boolean isCorrectChoice = ((cAns & (1 << c)) != 0);
                    String prefix = (c < prefixes.length) ? prefixes[c] : String.valueOf((char)('A' + c));
                    msBox.add(createChoiceRow(prefix, choiceText, isUserChoice, isCorrectChoice));
                }
                contentPanel.add(msBox);

            } else {
                // DẠNG 5: TRẮC NGHIỆM 1 ĐÁP ÁN (MULTIPLE CHOICE 1 OF 4)
                String[] choices = q.choices();
                int maxC = (choices != null) ? Math.min(4, choices.length) : 0;
                JPanel choicesPanel = new JPanel(new GridLayout(maxC, 1, 0, 6));
                choicesPanel.setOpaque(false);
                choicesPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

                for (int c = 0; c < maxC; c++) {
                    String choiceText = choices[c];
                    boolean isUserChoice = (uAns == c);
                    boolean isCorrectChoice = (cAns == c);
                    choicesPanel.add(createChoiceRow(prefixes[c], choiceText, isUserChoice, isCorrectChoice));
                }
                contentPanel.add(choicesPanel);
            }

            // ==========================================
            // GÓC KIẾN THỨC / GIẢI THÍCH (EXPLANATION)
            // Chỉ hiển thị khi có dữ liệu giải thích không rỗng
            // ==========================================
            if (q.explanation() != null && !q.explanation().trim().isEmpty()) {
                JPanel expBox = new JPanel(new BorderLayout(8, 4));
                expBox.setOpaque(true);
                expBox.setBackground(new Color(0xF0, 0xF9, 0xFF));
                expBox.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(0xBA, 0xE6, 0xFD), 1, true),
                        new EmptyBorder(8, 12, 8, 12)
                ));
                expBox.setAlignmentX(Component.LEFT_ALIGNMENT);

                // Icon bóng đèn / tri thức Java2D
                JPanel expIcon = new JPanel() {
                    @Override
                    protected void paintComponent(Graphics g) {
                        super.paintComponent(g);
                        Graphics2D g2 = (Graphics2D) g.create();
                        Theme.setupAntiAliasing(g2);
                        int w = getWidth();
                        int h = getHeight();
                        int cx = w / 2;
                        int cy = h / 2;

                        g2.setColor(new Color(0x02, 0x84, 0xC7));
                        g2.fillOval(cx - 7, cy - 8, 14, 14);
                        g2.fillRect(cx - 4, cy + 3, 8, 4);

                        g2.setColor(Theme.WHITE);
                        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
                        FontMetrics fm = g2.getFontMetrics();
                        g2.drawString("i", cx - fm.stringWidth("i") / 2, cy + 3);
                        g2.dispose();
                    }
                };
                expIcon.setPreferredSize(new Dimension(20, 20));
                expIcon.setOpaque(false);

                JPanel leftHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
                leftHeader.setOpaque(false);
                leftHeader.add(expIcon);

                JLabel lblExpTitle = new JLabel("Giải thích: ");
                lblExpTitle.setFont(Theme.SMALL_BOLD);
                lblExpTitle.setForeground(new Color(0x03, 0x69, 0xA1));
                leftHeader.add(lblExpTitle);

                JLabel lblExpText = new JLabel("<html><body style='font-family: Segoe UI, sans-serif; font-size: 12px; color: #0C4A6E; line-height: 1.35;'>"
                        + q.explanation() + "</body></html>");
                lblExpText.setFont(Theme.SMALL);

                expBox.add(leftHeader, BorderLayout.WEST);
                expBox.add(lblExpText, BorderLayout.CENTER);

                contentPanel.add(Box.createVerticalStrut(10));
                contentPanel.add(expBox);
            }

            card.add(contentPanel, BorderLayout.CENTER);
            questionCards.add(new QuestionCardWrapper(i, isCorrect, isSkipped, card));
        }
    }

    /**
     * Tạo một dòng phương án trả lời với nhãn prefix (A/B/C/D), màu nền/viền phân biệt rõ ràng:
     * - Bạn chọn & Đúng: Nền xanh lá tươi, viền xanh lá đậm, chữ đậm.
     * - Bạn chọn & Sai: Nền đỏ hồng, viền đỏ, chữ đỏ đậm.
     * - Đáp án chính xác mà bạn không chọn: Nền xanh ngọc pastel, viền xanh, nhãn Đáp án chính xác.
     * - Các phương án trung tính: Nền xám nhạt/trắng, viền mảnh.
     * Tự động co giãn phủ trọn 100% chiều ngang thẻ, không bị giới hạn cứng.
     */
    private JPanel createChoiceRow(String prefix, String text, boolean isUserChoice, boolean isCorrectChoice) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(true);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        Color bg = Theme.WHITE;
        Color border = Theme.BORDER_LIGHT;
        Color fg = Theme.DARK_TEXT;
        String statusNote = "";

        if (isUserChoice && isCorrectChoice) {
            bg = new Color(0xEC, 0xFD, 0xF5);
            border = Theme.GREEN;
            fg = new Color(0x06, 0x5F, 0x46);
            statusNote = "Lựa chọn của bạn (Chính xác)";
        } else if (isUserChoice && !isCorrectChoice) {
            bg = new Color(0xFE, 0xF2, 0xF2);
            border = Theme.RED;
            fg = new Color(0x99, 0x1B, 0x1B);
            statusNote = "Lựa chọn của bạn";
        } else if (!isUserChoice && isCorrectChoice) {
            bg = new Color(0xF0, 0xFD, 0xF4);
            border = Theme.GREEN;
            fg = new Color(0x16, 0x65, 0x34);
            statusNote = "Đáp án chính xác";
        }

        row.setBackground(bg);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border, 1, true),
                new EmptyBorder(8, 12, 8, 12)
        ));

        // Prefix badge tròn
        JPanel prefixBadge = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setupAntiAliasing(g2);
                int size = Math.min(getWidth(), getHeight()) - 2;
                int ox = (getWidth() - size) / 2;
                int oy = (getHeight() - size) / 2;

                Color circleColor = isCorrectChoice ? Theme.GREEN : (isUserChoice ? Theme.RED : new Color(0x94, 0xA3, 0xB8));
                g2.setColor(circleColor);
                g2.fillOval(ox, oy, size, size);

                g2.setColor(Theme.WHITE);
                g2.setFont(Theme.SMALL_BOLD);
                if ("✓".equals(prefix) || "\u2713".equals(prefix)) {
                    g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    int mx = ox + size / 2;
                    int my = oy + size / 2;
                    g2.drawLine(mx - 4, my, mx - 1, my + 4);
                    g2.drawLine(mx - 1, my + 4, mx + 5, my - 4);
                } else if ("✗".equals(prefix) || "\u2717".equals(prefix) || "✕".equals(prefix)) {
                    g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    int mx = ox + size / 2;
                    int my = oy + size / 2;
                    g2.drawLine(mx - 4, my - 4, mx + 4, my + 4);
                    g2.drawLine(mx + 4, my - 4, mx - 4, my + 4);
                } else {
                    FontMetrics fm = g2.getFontMetrics();
                    int tx = ox + (size - fm.stringWidth(prefix)) / 2;
                    int ty = oy + (size - fm.getHeight()) / 2 + fm.getAscent();
                    g2.drawString(prefix, tx, ty);
                }
                g2.dispose();
            }
        };
        prefixBadge.setPreferredSize(new Dimension(26, 26));
        prefixBadge.setOpaque(false);

        JLabel lblText = new JLabel("<html><body style='font-family: Segoe UI, sans-serif; font-size: 13px;'>" + text + "</body></html>");
        lblText.setFont(isUserChoice || isCorrectChoice ? Theme.BODY_BOLD : Theme.BODY);
        lblText.setForeground(fg);

        row.add(prefixBadge, BorderLayout.WEST);
        row.add(lblText, BorderLayout.CENTER);

        if (!statusNote.isEmpty()) {
            JLabel lblNote = new JLabel(statusNote);
            lblNote.setFont(Theme.SMALL_BOLD);
            lblNote.setForeground(fg);
            row.add(lblNote, BorderLayout.EAST);
        }

        return row;
    }

    /**
     * ModernScrollBarUI - Thanh cuộn phẳng thanh lịch, bo góc mềm mại không có mũi tên cồng kềnh.
     */
    private static class ModernScrollBarUI extends BasicScrollBarUI {
        @Override
        protected void configureScrollBarColors() {
            this.thumbColor = new Color(0xCB, 0xD5, 0xE1);
            this.trackColor = Theme.OFF_WHITE;
        }

        @Override
        public Dimension getPreferredSize(JComponent c) {
            return new Dimension(8, super.getPreferredSize(c).height);
        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return createZeroButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return createZeroButton();
        }

        private JButton createZeroButton() {
            JButton btn = new JButton();
            btn.setPreferredSize(new Dimension(0, 0));
            btn.setMinimumSize(new Dimension(0, 0));
            btn.setMaximumSize(new Dimension(0, 0));
            return btn;
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
            // Track trong suốt trên nền OFF_WHITE
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
            if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) return;
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.setupAntiAliasing(g2);
            boolean isRollover = isThumbRollover();
            Color cThumb = isRollover ? new Color(0x94, 0xA3, 0xB8) : new Color(0xCB, 0xD5, 0xE1);
            g2.setColor(cThumb);
            g2.fillRoundRect(thumbBounds.x + 1, thumbBounds.y + 2, thumbBounds.width - 2, thumbBounds.height - 4, 6, 6);
            g2.dispose();
        }
    }

    public void setTacticalData(String[] myOutcomes, String[] oppOutcomes, int[] myDeltas, int[] oppDeltas) {
        this.myOutcomeHistory = myOutcomes;
        this.oppOutcomeHistory = oppOutcomes;
        this.myDeltaHistory = myDeltas;
        this.oppDeltaHistory = oppDeltas;
        int total = (questions != null) ? questions.size() : 0;
        buildQuestionCards(total);
        if (listContainer != null) {
            listContainer.revalidate();
            listContainer.repaint();
        }
    }

    public TacticalBadge getTacticalBadgeAt(int i) {
        return createTacticalBadgeForQuestion(i);
    }

    private TacticalBadge createTacticalBadgeForQuestion(int i) {
        if (myOutcomeHistory == null || i < 0 || i >= myOutcomeHistory.length) return null;
        String outcome = myOutcomeHistory[i];
        if (outcome == null || outcome.isBlank() || "NONE".equals(outcome)) return null;

        int delta = (myDeltaHistory != null && i < myDeltaHistory.length) ? myDeltaHistory[i] : 0;

        return switch (outcome) {
            case "STEAL_SUCCESS" -> {
                int pts = (delta > 0) ? delta : 50;
                yield new TacticalBadge("[⚔️ +" + pts + "đ Cướp điểm]",
                        new Color(0xFF, 0xF1, 0xF2), new Color(0xF4, 0x3F, 0x5E), new Color(0xBE, 0x12, 0x3C));
            }
            case "BLOCK_SUCCESS" -> {
                yield new TacticalBadge("[🛡️ Đã Hóa Giải]",
                        new Color(0xEC, 0xFE, 0xFF), new Color(0x06, 0xB6, 0xD4), new Color(0x0E, 0x74, 0x90));
            }
            case "COUNTER_SUCCESS" -> {
                int pts = (delta > 0) ? delta : 50;
                yield new TacticalBadge("[🛡️ +" + pts + "đ Phản đòn]",
                        new Color(0xEC, 0xFE, 0xFF), new Color(0x06, 0xB6, 0xD4), new Color(0x0E, 0x74, 0x90));
            }
            case "STAR_SUCCESS" -> {
                int pts = (delta > 0) ? delta : 200;
                yield new TacticalBadge("[⭐ x2 Điểm: +" + pts + "đ]",
                        new Color(0xFF, 0xFB, 0xEB), new Color(0xF5, 0x9E, 0x0B), new Color(0xB4, 0x53, 0x09));
            }
            case "STAR_FAIL" -> {
                yield new TacticalBadge("[⭐ -50đ]",
                        new Color(0xFF, 0xFB, 0xEB), new Color(0xF5, 0x9E, 0x0B), new Color(0xB4, 0x53, 0x09));
            }
            case "BLOCKED" -> {
                yield new TacticalBadge("[⚔️ Bị Hóa Giải]",
                        new Color(0xFF, 0xF1, 0xF2), new Color(0xF4, 0x3F, 0x5E), new Color(0xBE, 0x12, 0x3C));
            }
            case "COUNTERED" -> {
                int pts = (delta > 0) ? delta : 50;
                yield new TacticalBadge("[⚔️ -" + pts + "đ Bị Phản đòn]",
                        new Color(0xFF, 0xF1, 0xF2), new Color(0xF4, 0x3F, 0x5E), new Color(0xBE, 0x12, 0x3C));
            }
            case "BEING_STOLEN" -> {
                int pts = (delta > 0) ? delta : 50;
                yield new TacticalBadge("[⚔️ -" + pts + "đ Bị Cướp]",
                        new Color(0xFF, 0xF1, 0xF2), new Color(0xF4, 0x3F, 0x5E), new Color(0xBE, 0x12, 0x3C));
            }
            default -> null;
        };
    }

    public static class TacticalBadge extends JComponent {
        private final String text;
        private final Color bg;
        private final Color border;
        private final Color fg;

        public TacticalBadge(String text, Color bg, Color border, Color fg) {
            this.text = text;
            this.bg = bg;
            this.border = border;
            this.fg = fg;
            setFont(Theme.SMALL_BOLD);
            setOpaque(false);
        }

        public String getText() {
            return text;
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            int w = fm.stringWidth(text) + 20;
            int h = Math.max(22, fm.getHeight() + 6);
            return new Dimension(w, h);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.setupAntiAliasing(g2);
            int w = getWidth();
            int h = getHeight();
            int r = h;

            g2.setColor(bg);
            g2.fill(new java.awt.geom.RoundRectangle2D.Float(1, 1, w - 2, h - 2, r, r));

            g2.setColor(border);
            g2.setStroke(new BasicStroke(1.2f));
            g2.draw(new java.awt.geom.RoundRectangle2D.Float(1f, 1f, w - 2, h - 2, r, r));

            g2.setFont(getFont());
            g2.setColor(fg);
            FontMetrics fm = g2.getFontMetrics();
            int tx = (w - fm.stringWidth(text)) / 2;
            int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(text, tx, ty);

            g2.dispose();
        }
    }
}
