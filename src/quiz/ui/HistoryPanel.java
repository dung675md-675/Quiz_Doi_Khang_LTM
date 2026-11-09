package quiz.ui;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * HistoryPanel — Giao diện Lịch sử thi đấu (Match History) phong cách eSports hiện đại:
 * - 4 thẻ thống kê sự nghiệp rực rỡ (Tổng trận, Tỉ lệ thắng, Chuỗi kỷ lục, Điểm tích lũy)
 *   với hiệu ứng chuyển màu pastel sang trọng và biểu tượng Java2D vector.
 * - Thanh chuyển tab bộ lọc capsule: Tất cả, Thắng, Thua, Hòa (bỏ ô tìm kiếm theo yêu cầu).
 * - Danh sách thẻ trận đấu trực quan với tỉ số lớn, avatar hai đấu thủ, nhãn kết quả và xem chi tiết.
 * - Tương thích mượt mà với thanh cuộn ModernScrollBarUI.
 */
public class HistoryPanel extends JPanel {

    private enum FilterMode {
        ALL,
        WIN,
        LOSS,
        DRAW
    }

    private final JPanel listPanel;
    private final JScrollPane scrollPane;
    private final EmptyStatePanel emptyState;
    private final List<HistoryCard> allCards = new ArrayList<>();

    // 4 Stat Labels
    private final JLabel lblTotalMatchesVal = new JLabel("0");
    private final JLabel lblWinRateVal = new JLabel("0%");
    private final JLabel lblMaxStreakVal = new JLabel("0 trận");
    private final JLabel lblTotalPointsVal = new JLabel("0 đ");

    // Filter toolbar buttons
    private FilterMode currentFilter = FilterMode.ALL;
    private final FilterCapsuleButton btnFilterAll;
    private final FilterCapsuleButton btnFilterWin;
    private final FilterCapsuleButton btnFilterLoss;
    private final FilterCapsuleButton btnFilterDraw;

    private String currentUsername = "";

    public HistoryPanel() {
        setLayout(new BorderLayout(0, 12));
        setOpaque(false);
        setBorder(new EmptyBorder(14, 20, 16, 20));

        // ==========================================
        // 1. TOP CONTAINER: STATS + FILTER TOOLBAR
        // ==========================================
        JPanel topContainer = new JPanel();
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
        topContainer.setOpaque(false);

        // Header Title
        JPanel headerTitle = new JPanel(new BorderLayout());
        headerTitle.setOpaque(false);

        JLabel title = new JLabel("LỊCH SỬ THI ĐẤU ĐỐI KHÁNG 1V1");
        title.setFont(Theme.TITLE);
        title.setForeground(Theme.BLUE);

        JLabel subtitle = new JLabel("Thống kê phong độ và tra cứu chi tiết kết quả các trận đối kháng 1v1");
        subtitle.setFont(Theme.SMALL);
        subtitle.setForeground(Theme.MUTED_GRAY);

        headerTitle.add(title, BorderLayout.NORTH);
        headerTitle.add(subtitle, BorderLayout.SOUTH);

        topContainer.add(headerTitle);
        topContainer.add(Box.createVerticalStrut(12));

        // 4 Stat Cards: Tổng trận, Tỉ lệ thắng, Chuỗi kỷ lục, Điểm tích lũy
        JPanel statsRow = new JPanel(new GridLayout(1, 4, 12, 0));
        statsRow.setOpaque(false);

        statsRow.add(createModernStatCard(
                "TỔNG SỐ TRẬN",
                "Trận đấu xếp hạng",
                lblTotalMatchesVal,
                new Color(0xEE, 0xF2, 0xFF),
                new Color(0xDB, 0xEA, 0xFE),
                new Color(0xBF, 0xDB, 0xFE),
                new Color(0x1D, 0x4E, 0xD8),
                0 // Icon: Swords/Gamepad
        ));

        statsRow.add(createModernStatCard(
                "TỈ LỆ THẮNG",
                "Hiệu suất chiến thắng",
                lblWinRateVal,
                new Color(0xEC, 0xFD, 0xF5),
                new Color(0xD1, 0xFA, 0xE5),
                new Color(0xA7, 0xF3, 0xD0),
                new Color(0x04, 0x78, 0x57),
                1 // Icon: Trophy
        ));

        statsRow.add(createModernStatCard(
                "CHUỖI KỶ LỤC",
                "Chuỗi thắng dài nhất",
                lblMaxStreakVal,
                new Color(0xFF, 0xFB, 0xEB),
                new Color(0xFE, 0xF3, 0xC7),
                new Color(0xFD, 0xE6, 0x8A),
                new Color(0xB4, 0x53, 0x09),
                2 // Icon: Flame/Streak
        ));

        statsRow.add(createModernStatCard(
                "ĐIỂM TÍCH LŨY",
                "Điểm xếp hạng Rank",
                lblTotalPointsVal,
                new Color(0xFA, 0xF5, 0xFF),
                new Color(0xED, 0xE9, 0xFE),
                new Color(0xDD, 0xD6, 0xFE),
                new Color(0x6D, 0x28, 0xD9),
                3 // Icon: Star/Crown
        ));

        topContainer.add(statsRow);
        topContainer.add(Box.createVerticalStrut(14));

        // Filter Toolbar (Capsule tabs, không có ô tìm kiếm)
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        toolbar.setOpaque(false);

        btnFilterAll = new FilterCapsuleButton("Tất cả", new Color(0x4F, 0x46, 0xE5), () -> setFilter(FilterMode.ALL));
        btnFilterWin = new FilterCapsuleButton("Thắng", new Color(0x05, 0x96, 0x69), () -> setFilter(FilterMode.WIN));
        btnFilterLoss = new FilterCapsuleButton("Thua", new Color(0xDC, 0x26, 0x26), () -> setFilter(FilterMode.LOSS));
        btnFilterDraw = new FilterCapsuleButton("Hòa", new Color(0xD9, 0x77, 0x06), () -> setFilter(FilterMode.DRAW));

        btnFilterAll.setActive(true);

        toolbar.add(btnFilterAll);
        toolbar.add(btnFilterWin);
        toolbar.add(btnFilterLoss);
        toolbar.add(btnFilterDraw);

        topContainer.add(toolbar);
        topContainer.add(Box.createVerticalStrut(4));
        add(topContainer, BorderLayout.NORTH);

        // ==========================================
        // 2. LIST PANEL & MODERN SCROLLPANE
        // ==========================================
        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setOpaque(false);
        listPanel.setBorder(new EmptyBorder(4, 0, 10, 0));

        emptyState = new EmptyStatePanel("", "Chưa có lịch sử đấu", "Hãy tham gia các trận đối kháng 1v1 để ghi danh thành tích vào lịch sử.");

        scrollPane = new JScrollPane(listPanel);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUI(new ModernScrollBarUI());
        scrollPane.getVerticalScrollBar().setUnitIncrement(18);
        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * Tạo thẻ thống kê hiện đại với Gradient pastel sang trọng và Vector Java2D icon.
     */
    private JPanel createModernStatCard(String title, String subtitle, JLabel lblValue,
                                        Color bgTop, Color bgBottom, Color borderColor, Color primaryColor,
                                        int iconType) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setupAntiAliasing(g2);
                int w = getWidth();
                int h = getHeight();

                // Gradient pastel
                GradientPaint gp = new GradientPaint(0, 0, bgTop, 0, h, bgBottom);
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, w, h, 14, 14);

                // Border
                g2.setColor(borderColor);
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 14, 14);

                // Java2D Vector Icon Badge (28x28 ở góc trên bên phải)
                int icSize = 28;
                int icX = w - icSize - 12;
                int icY = 12;

                g2.setColor(new Color(primaryColor.getRed(), primaryColor.getGreen(), primaryColor.getBlue(), 28));
                g2.fillOval(icX, icY, icSize, icSize);

                g2.setColor(primaryColor);
                g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int cx = icX + icSize / 2;
                int cy = icY + icSize / 2;

                if (iconType == 0) {
                    // Swords / Gamepad
                    g2.drawLine(cx - 5, cy - 5, cx + 5, cy + 5);
                    g2.drawLine(cx + 5, cy - 5, cx - 5, cy + 5);
                    g2.drawOval(cx - 7, cy - 7, 14, 14);
                } else if (iconType == 1) {
                    // Trophy Cup
                    g2.drawArc(cx - 5, cy - 6, 10, 8, 0, -180);
                    g2.drawLine(cx - 5, cy - 6, cx + 5, cy - 6);
                    g2.drawLine(cx, cy + 2, cx, cy + 6);
                    g2.drawLine(cx - 4, cy + 6, cx + 4, cy + 6);
                } else if (iconType == 2) {
                    // Flame / Streak
                    int[] xf = {cx, cx + 5, cx + 2, cx + 6, cx - 1, cx - 6, cx - 3};
                    int[] yf = {cy - 7, cy - 1, cy + 1, cy + 6, cy + 7, cy + 3, cy - 2};
                    g2.drawPolygon(xf, yf, 7);
                } else {
                    // Star / Crown
                    g2.drawLine(cx - 6, cy + 4, cx - 6, cy - 2);
                    g2.drawLine(cx - 6, cy - 2, cx - 2, cy + 1);
                    g2.drawLine(cx - 2, cy + 1, cx, cy - 4);
                    g2.drawLine(cx, cy - 4, cx + 2, cy + 1);
                    g2.drawLine(cx + 2, cy + 1, cx + 6, cy - 2);
                    g2.drawLine(cx + 6, cy - 2, cx + 6, cy + 4);
                    g2.drawLine(cx - 6, cy + 4, cx + 6, cy + 4);
                }

                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(10, 14, 10, 14));
        card.setPreferredSize(new Dimension(170, 82));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(Theme.SMALL_BOLD);
        lblTitle.setForeground(primaryColor);
        lblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        lblValue.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        lblValue.setForeground(primaryColor);
        lblValue.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblSub = new JLabel(subtitle);
        lblSub.setFont(Theme.SMALL);
        lblSub.setForeground(new Color(primaryColor.getRed(), primaryColor.getGreen(), primaryColor.getBlue(), 180));
        lblSub.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(lblTitle);
        card.add(Box.createVerticalStrut(2));
        card.add(lblValue);
        card.add(Box.createVerticalStrut(2));
        card.add(lblSub);

        return card;
    }

    private void setFilter(FilterMode mode) {
        this.currentFilter = mode;
        btnFilterAll.setActive(mode == FilterMode.ALL);
        btnFilterWin.setActive(mode == FilterMode.WIN);
        btnFilterLoss.setActive(mode == FilterMode.LOSS);
        btnFilterDraw.setActive(mode == FilterMode.DRAW);
        applyFilters();
    }

    public void updateData(String rawData, String currentUsername) {
        this.currentUsername = currentUsername != null ? currentUsername : "";
        allCards.clear();
        listPanel.removeAll();

        if (rawData == null || rawData.isBlank()) {
            lblTotalMatchesVal.setText("0");
            lblWinRateVal.setText("0%");
            lblMaxStreakVal.setText("0 trận");
            lblTotalPointsVal.setText("0 đ");
            listPanel.add(emptyState);
            listPanel.revalidate();
            listPanel.repaint();
            return;
        }

        String[] rows = rawData.split("\n");
        int totalMatches = 0;
        int winCount = 0;
        int lossCount = 0;
        int drawCount = 0;
        int currentStreak = 0;
        int maxStreak = 0;

        List<HistoryCard> parsedCards = new ArrayList<>();
        for (String row : rows) {
            if (row.isBlank()) continue;
            HistoryCard card = new HistoryCard(row, this.currentUsername, SwingUtilities.getWindowAncestor(this));
            parsedCards.add(card);
            totalMatches++;

            if (card.getResult() == HistoryCard.MatchResult.WIN) {
                winCount++;
                currentStreak++;
                if (currentStreak > maxStreak) {
                    maxStreak = currentStreak;
                }
            } else if (card.getResult() == HistoryCard.MatchResult.LOSS) {
                lossCount++;
                currentStreak = 0;
            } else {
                drawCount++;
                currentStreak = 0;
            }
        }

        // Sắp xếp trận đấu gần nhất lên đầu tiên (timestamp giảm dần)
        parsedCards.sort((c1, c2) -> Long.compare(c2.getTimestamp(), c1.getTimestamp()));
        allCards.addAll(parsedCards);

        // Cập nhật thống kê 4 thẻ
        int winRate = totalMatches > 0 ? (winCount * 100 / totalMatches) : 0;
        int estimatedPoints = winCount * 2 - lossCount;
        lblTotalMatchesVal.setText(totalMatches + " trận");
        lblWinRateVal.setText(winRate + "%");
        lblMaxStreakVal.setText(maxStreak + " trận");
        lblTotalPointsVal.setText(Math.max(0, estimatedPoints) + " đ");

        // Cập nhật nhãn nút bộ lọc
        btnFilterAll.setLabelText("Tất cả (" + totalMatches + ")");
        btnFilterWin.setLabelText("Thắng (" + winCount + ")");
        btnFilterLoss.setLabelText("Thua (" + lossCount + ")");
        btnFilterDraw.setLabelText("Hòa (" + drawCount + ")");

        applyFilters();
    }

    private void applyFilters() {
        listPanel.removeAll();

        int visibleCount = 0;
        for (HistoryCard card : allCards) {
            boolean matchesFilter = switch (currentFilter) {
                case ALL -> true;
                case WIN -> card.getResult() == HistoryCard.MatchResult.WIN;
                case LOSS -> card.getResult() == HistoryCard.MatchResult.LOSS;
                case DRAW -> card.getResult() == HistoryCard.MatchResult.DRAW;
            };

            if (matchesFilter) {
                listPanel.add(card);
                listPanel.add(Box.createVerticalStrut(10));
                visibleCount++;
            }
        }

        if (visibleCount == 0) {
            listPanel.add(emptyState);
        }

        listPanel.revalidate();
        listPanel.repaint();
    }

    public List<HistoryCard> getAllCards() {
        return java.util.Collections.unmodifiableList(allCards);
    }

    /**
     * FilterCapsuleButton — Nút chọn bộ lọc dạng Capsule phong cách hiện đại.
     */
    private static class FilterCapsuleButton extends JPanel {
        private String text;
        private final Color activeColor;
        private boolean isActive = false;
        private boolean isHover = false;
        private final Runnable onClick;

        public FilterCapsuleButton(String text, Color activeColor, Runnable onClick) {
            this.text = text;
            this.activeColor = activeColor;
            this.onClick = onClick;
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(96, 32));

            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    isHover = true;
                    repaint();
                }

                @Override
                public void mouseExited(java.awt.event.MouseEvent e) {
                    isHover = false;
                    repaint();
                }

                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    SoundManager.playClick();
                    if (onClick != null) onClick.run();
                }
            });
        }

        public void setActive(boolean active) {
            this.isActive = active;
            repaint();
        }

        public void setLabelText(String text) {
            this.text = text;
            // Tự động điều chỉnh kích thước nếu text dài hơn
            FontMetrics fm = getFontMetrics(Theme.SMALL_BOLD);
            int width = Math.max(96, fm.stringWidth(text) + 24);
            setPreferredSize(new Dimension(width, 32));
            revalidate();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.setupAntiAliasing(g2);
            int w = getWidth();
            int h = getHeight();

            if (isActive) {
                // Active pill: Màu chủ đạo nổi bật + đổ bóng nhẹ
                g2.setColor(activeColor);
                g2.fillRoundRect(0, 0, w, h, 16, 16);

                g2.setColor(Theme.WHITE);
                g2.setFont(Theme.SMALL_BOLD);
                FontMetrics fm = g2.getFontMetrics();
                int tx = (w - fm.stringWidth(text)) / 2;
                int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(text, tx, ty);
            } else {
                // Inactive pill: Nền xám nhạt phẳng, chữ xám đậm
                Color bg = isHover ? new Color(0xE2, 0xE8, 0xF0) : new Color(0xF1, 0xF5, 0xF9);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, w, h, 16, 16);

                g2.setColor(isHover ? new Color(0x94, 0xA3, 0xB8) : new Color(0xCB, 0xD5, 0xE1));
                g2.setStroke(new BasicStroke(1.0f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 16, 16);

                g2.setColor(isHover ? Theme.DARK_TEXT : new Color(0x47, 0x55, 0x69));
                g2.setFont(Theme.SMALL);
                FontMetrics fm = g2.getFontMetrics();
                int tx = (w - fm.stringWidth(text)) / 2;
                int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(text, tx, ty);
            }
            g2.dispose();
        }
    }
}
