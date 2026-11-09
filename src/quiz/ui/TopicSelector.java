package quiz.ui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.*;

/** A two-row photo explorer; every topic remains available in a compact window. */
public class TopicSelector extends JPanel {
    private final JPanel cardsPanel;
    private final JScrollPane scrollPane;
    private final List<TopicCard> cardList = new ArrayList<>();
    private final JLabel lblSelectedInfo;
    private final ModernBadge badgeCount;
    private final JButton btnPrev;
    private final JButton btnNext;
    private String selectedTopic = "Tổng hợp";
    private Consumer<String> onTopicChanged;

    public TopicSelector() { this(List.of()); }

    public TopicSelector(List<String> initialTopics) {
        super(new BorderLayout(0, 16));
        setOpaque(false);

        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setOpaque(false);
        JPanel copy = new JPanel(new BorderLayout(0, 5));
        copy.setOpaque(false);
        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        titleRow.setOpaque(false);
        JLabel title = new JLabel("Khám phá chủ đề");
        title.setFont(ArenaPalette.font(22, Font.BOLD));
        title.setForeground(ArenaPalette.TEXT);
        titleRow.add(title);
        badgeCount = new ModernBadge("0 chủ đề", ModernBadge.Type.INFO);
        badgeCount.setArenaStyle(true);
        titleRow.add(badgeCount);
        copy.add(titleRow, BorderLayout.NORTH);
        lblSelectedInfo = new JLabel("Đang chọn: Tổng hợp");
        lblSelectedInfo.putClientProperty("html.disable", Boolean.TRUE);
        lblSelectedInfo.setFont(ArenaPalette.font(12, Font.PLAIN));
        lblSelectedInfo.setForeground(ArenaPalette.MUTED);
        copy.add(lblSelectedInfo, BorderLayout.CENTER);
        header.add(copy, BorderLayout.CENTER);

        JPanel arrows = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        arrows.setOpaque(false);
        btnPrev = navigation(true);
        btnNext = navigation(false);
        arrows.add(btnPrev);
        arrows.add(btnNext);
        header.add(arrows, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        cardsPanel = new JPanel(new GridLayout(2, 0, 12, 12));
        cardsPanel.setOpaque(false);
        scrollPane = new JScrollPane(cardsPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setWheelScrollingEnabled(false);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(36);
        scrollPane.setPreferredSize(new Dimension(1000, 292));
        scrollPane.setMinimumSize(new Dimension(260, 292));
        scrollPane.addMouseWheelListener(event -> {
            scrollBy(event.getUnitsToScroll() * 28);
            event.consume();
        });
        scrollPane.getViewport().addChangeListener(event -> updateArrows());
        btnPrev.addActionListener(event -> {
            SoundManager.playClick();
            scrollBy(-Math.max(222, scrollPane.getViewport().getWidth() - 60));
        });
        btnNext.addActionListener(event -> {
            SoundManager.playClick();
            scrollBy(Math.max(222, scrollPane.getViewport().getWidth() - 60));
        });
        add(scrollPane, BorderLayout.CENTER);
        setTopics(initialTopics != null && !initialTopics.isEmpty() ? initialTopics : quiz.Bank.STANDARD_TOPICS);
    }

    private void scrollBy(int distance) {
        JViewport viewport = scrollPane.getViewport();
        int limit = Math.max(0, viewport.getViewSize().width - viewport.getExtentSize().width);
        int next = Math.max(0, Math.min(limit, viewport.getViewPosition().x + distance));
        viewport.setViewPosition(new Point(next, 0));
        updateArrows();
    }

    private void updateArrows() {
        if (scrollPane == null || btnPrev == null || btnNext == null) return;
        JViewport viewport = scrollPane.getViewport();
        int limit = Math.max(0, viewport.getViewSize().width - viewport.getExtentSize().width);
        btnPrev.setEnabled(viewport.getViewPosition().x > 0);
        btnNext.setEnabled(viewport.getViewPosition().x < limit);
    }

    private JButton navigation(boolean previous) {
        JButton button = new JButton() {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                Theme.setupAntiAliasing(g);
                g.setColor(getModel().isRollover() && isEnabled()
                        ? ArenaPalette.SURFACE_RAISED : ArenaPalette.SURFACE);
                g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g.setColor(hasFocus() ? ArenaPalette.ACCENT : ArenaPalette.BORDER);
                g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g.setColor(isEnabled() ? ArenaPalette.TEXT : new Color(70, 85, 112));
                g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int x = getWidth() / 2;
                int y = getHeight() / 2;
                int direction = previous ? -1 : 1;
                g.drawLine(x - direction * 3, y - 5, x + direction * 3, y);
                g.drawLine(x + direction * 3, y, x - direction * 3, y + 5);
                g.dispose();
            }
        };
        button.setPreferredSize(new Dimension(38, 38));
        button.setToolTipText(previous ? "Chủ đề phía trước" : "Chủ đề tiếp theo");
        button.getAccessibleContext().setAccessibleName(button.getToolTipText());
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    public void setTopics(List<String> topics) {
        cardsPanel.removeAll();
        cardList.clear();
        List<String> all = new ArrayList<>();
        all.add("Tổng hợp");
        if (topics != null) {
            for (String topic : topics) {
                if (topic != null && !topic.isBlank() && !topic.equalsIgnoreCase("Tổng hợp")
                        && !all.contains(topic)) all.add(topic);
            }
        }
        if (all.stream().noneMatch(topic -> topic.equalsIgnoreCase(selectedTopic))) selectedTopic = "Tổng hợp";
        badgeCount.setText(all.size() + " chủ đề");
        for (int index = 0; index < all.size(); index++) {
            String topic = all.get(index);
            TopicCard card = new TopicCard(topic, topic.equalsIgnoreCase(selectedTopic), this::onCardSelected);
            int position = index;
            card.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "previousTopic");
            card.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "nextTopic");
            card.getActionMap().put("previousTopic", new AbstractAction() {
                @Override public void actionPerformed(ActionEvent event) { focusTopic(Math.max(0, position - 1)); }
            });
            card.getActionMap().put("nextTopic", new AbstractAction() {
                @Override public void actionPerformed(ActionEvent event) { focusTopic(Math.min(cardList.size() - 1, position + 1)); }
            });
            cardList.add(card);
            cardsPanel.add(card);
        }
        cardsPanel.setLayout(new GridLayout(all.size() > 1 ? 2 : 1, 0, 12, 12));
        updateSelectedLabel();
        cardsPanel.revalidate();
        cardsPanel.repaint();
        SwingUtilities.invokeLater(this::updateArrows);
    }

    private void focusTopic(int index) {
        if (index < 0 || index >= cardList.size()) return;
        TopicCard card = cardList.get(index);
        card.scrollRectToVisible(new Rectangle(0, 0, card.getWidth(), card.getHeight()));
        card.requestFocusInWindow();
    }

    private void onCardSelected(String topic) {
        setSelectedTopic(topic);
        if (onTopicChanged != null) onTopicChanged.accept(topic);
    }

    public void setSelectedTopic(String topic) {
        selectedTopic = topic == null || topic.isBlank() ? "Tổng hợp" : topic;
        for (TopicCard card : cardList) {
            boolean selected = card.getTopicName().equalsIgnoreCase(selectedTopic);
            card.setSelected(selected);
            if (selected) card.scrollRectToVisible(new Rectangle(0, 0, card.getWidth(), card.getHeight()));
        }
        updateSelectedLabel();
    }

    private void updateSelectedLabel() {
        lblSelectedInfo.setText("Đang chọn: " + selectedTopic);
        lblSelectedInfo.setToolTipText(selectedTopic);
    }

    public String getSelectedTopic() { return selectedTopic != null ? selectedTopic : "Tổng hợp"; }
    public void setOnTopicChanged(Consumer<String> listener) { onTopicChanged = listener; }
}
