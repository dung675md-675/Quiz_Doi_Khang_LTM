package quiz.ui;

import java.awt.*;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * TopicSelector — Khu vực chọn chủ đề trực quan bằng Topic Cards.
 * Có tiêu đề "CHỌN CHỦ ĐỀ" nổi bật, cuộn ngang mượt mà, hỗ trợ nút điều hướng và con lăn chuột.
 */
public class TopicSelector extends JPanel {

    private final JPanel cardsPanel;
    private final JScrollPane scrollPane;
    private final List<TopicCard> cardList = new ArrayList<>();
    private final JLabel lblSelectedInfo;

    private String selectedTopic = "Tổng hợp";
    private Consumer<String> onTopicChanged;

    public TopicSelector() {
        this(List.of());
    }

    public TopicSelector(List<String> initialTopics) {
        setLayout(new BorderLayout(0, 10));
        setOpaque(false);

        // ==========================================
        // 1. HEADER KHU VỰC CHỌN CHỦ ĐỀ
        // ==========================================
        JPanel headerPanel = new JPanel(new BorderLayout(12, 0));
        headerPanel.setOpaque(false);

        // Bên trái: Tiêu đề "CHỌN CHỦ ĐỀ" và thông tin chủ đề đang chọn
        JPanel leftHeader = new JPanel();
        leftHeader.setLayout(new BoxLayout(leftHeader, BoxLayout.Y_AXIS));
        leftHeader.setOpaque(false);

        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        titleRow.setOpaque(false);

        // Chấm tròn điểm nhấn màu xanh cyan
        JPanel accentDot = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setupAntiAliasing(g2);
                g2.setColor(new Color(0x00, 0xE5, 0xFF));
                g2.fillOval(0, 2, 10, 10);
                g2.dispose();
            }
        };
        accentDot.setPreferredSize(new Dimension(10, 14));
        accentDot.setOpaque(false);
        titleRow.add(accentDot);

        JLabel lblTitle = new JLabel("CHỌN CHỦ ĐỀ");
        lblTitle.setFont(Theme.TITLE);
        lblTitle.setForeground(Theme.DARK_TEXT);
        titleRow.add(lblTitle);

        ModernBadge badgeCount = new ModernBadge("8 Chủ Đề", ModernBadge.Type.RANK);
        titleRow.add(badgeCount);
        leftHeader.add(titleRow);

        lblSelectedInfo = new JLabel("Đang chọn: Tổng hợp");
        lblSelectedInfo.setFont(Theme.SMALL);
        lblSelectedInfo.setForeground(Theme.MUTED_GRAY);
        lblSelectedInfo.setBorder(new EmptyBorder(2, 18, 0, 0));
        leftHeader.add(lblSelectedInfo);

        headerPanel.add(leftHeader, BorderLayout.WEST);

        // Bên phải: Nút cuộn trái / phải
        JPanel navButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        navButtons.setOpaque(false);

        JButton btnPrev = createNavButton(true, "Cuộn sang trái");
        JButton btnNext = createNavButton(false, "Cuộn sang phải");

        btnPrev.addActionListener(e -> {
            SoundManager.playClick();
            scrollBy(-200);
        });

        btnNext.addActionListener(e -> {
            SoundManager.playClick();
            scrollBy(200);
        });

        navButtons.add(btnPrev);
        navButtons.add(btnNext);
        headerPanel.add(navButtons, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);

        // ==========================================
        // 2. DANH SÁCH TOPIC CARDS CUỘN NGANG
        // ==========================================
        cardsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 6));
        cardsPanel.setOpaque(false);

        scrollPane = new JScrollPane(cardsPanel);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(24);

        // Hỗ trợ cuộn ngang bằng chuột
        scrollPane.addMouseWheelListener(e -> {
            int units = e.getUnitsToScroll() * 28;
            scrollBy(units);
        });

        // Thiết lập chiều cao vùng thẻ (~178px đủ cho card 160px + padding thoải mái)
        scrollPane.setPreferredSize(new Dimension(880, 178));
        scrollPane.setMinimumSize(new Dimension(600, 178));

        add(scrollPane, BorderLayout.CENTER);

        // Nạp danh sách chủ đề ban đầu nếu có
        if (initialTopics != null && !initialTopics.isEmpty()) {
            setTopics(initialTopics);
        } else {
            // Mặc định nạp 8 chủ đề chuẩn của game
            setTopics(quiz.Bank.STANDARD_TOPICS);
        }
    }

    private void scrollBy(int pixels) {
        JScrollBar hBar = scrollPane.getHorizontalScrollBar();
        int cur = hBar.getValue();
        int target = Math.max(0, Math.min(hBar.getMaximum() - hBar.getVisibleAmount(), cur + pixels));
        hBar.setValue(target);
    }

    private JButton createNavButton(boolean isLeft, String tooltip) {
        JButton btn = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setupAntiAliasing(g2);
                int w = getWidth();
                int h = getHeight();
                if (getModel().isPressed()) {
                    g2.setColor(new Color(0xDE, 0xE4, 0xEE));
                } else if (getModel().isRollover()) {
                    g2.setColor(new Color(0xEB, 0xF0, 0xF8));
                } else {
                    g2.setColor(Theme.WHITE);
                }
                g2.fillRoundRect(0, 0, w - 1, h - 1, 8, 8);
                g2.setColor(Theme.BORDER_LIGHT);
                g2.drawRoundRect(0, 0, w - 1, h - 1, 8, 8);

                // Mũi tên tam giác vector rõ ràng, không bao giờ bị cắt thành dấu ba chấm
                g2.setColor(Theme.DARK_TEXT);
                int cx = w / 2;
                int cy = h / 2;
                if (isLeft) {
                    int[] xPoints = {cx + 3, cx - 4, cx + 3};
                    int[] yPoints = {cy - 5, cy, cy + 5};
                    g2.fillPolygon(xPoints, yPoints, 3);
                } else {
                    int[] xPoints = {cx - 3, cx + 4, cx - 3};
                    int[] yPoints = {cy - 5, cy, cy + 5};
                    g2.fillPolygon(xPoints, yPoints, 3);
                }
                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(32, 28));
        btn.setToolTipText(tooltip);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    /**
     * Cập nhật danh sách các chủ đề hiển thị trên thanh chọn thẻ.
     */
    public void setTopics(List<String> topics) {
        cardsPanel.removeAll();
        cardList.clear();

        List<String> fullList = new ArrayList<>();
        fullList.add("Tổng hợp");
        for (String t : topics) {
            if (!t.equalsIgnoreCase("Tổng hợp") && !fullList.contains(t)) {
                fullList.add(t);
            }
        }

        // Kiểm tra xem selectedTopic hiện tại có nằm trong danh sách không
        boolean foundCurrent = false;
        for (String t : fullList) {
            if (t.equalsIgnoreCase(selectedTopic)) {
                foundCurrent = true;
                break;
            }
        }
        if (!foundCurrent && !fullList.isEmpty()) {
            selectedTopic = fullList.get(0);
        }

        for (String topic : fullList) {
            boolean isSel = topic.equalsIgnoreCase(selectedTopic);
            TopicCard card = new TopicCard(topic, isSel, this::onCardSelected);
            cardList.add(card);
            cardsPanel.add(card);
        }

        updateSelectedLabel();
        cardsPanel.revalidate();
        cardsPanel.repaint();
    }

    private void onCardSelected(String topic) {
        setSelectedTopic(topic);
        if (onTopicChanged != null) {
            onTopicChanged.accept(topic);
        }
    }

    public void setSelectedTopic(String topic) {
        if (topic == null || topic.isBlank()) {
            topic = "Tổng hợp";
        }
        this.selectedTopic = topic;

        for (TopicCard card : cardList) {
            card.setSelected(card.getTopicName().equalsIgnoreCase(selectedTopic));
        }

        updateSelectedLabel();
    }

    private void updateSelectedLabel() {
        if (lblSelectedInfo != null) {
            lblSelectedInfo.setText("Đang chọn: " + selectedTopic);
        }
    }

    public String getSelectedTopic() {
        return selectedTopic != null ? selectedTopic : "Tổng hợp";
    }

    public void setOnTopicChanged(Consumer<String> listener) {
        this.onTopicChanged = listener;
    }
}
