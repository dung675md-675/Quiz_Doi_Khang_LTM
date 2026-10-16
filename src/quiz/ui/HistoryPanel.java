package quiz.ui;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class HistoryPanel extends JPanel {

    private final JPanel listPanel;
    private final JScrollPane scrollPane;
    private final EmptyStatePanel emptyState;
    private String currentUsername;

    public HistoryPanel() {
        setLayout(new BorderLayout());
        setOpaque(false);

        // HEADER
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(16, 24, 12, 24));

        JLabel title = new JLabel("LỊCH SỬ THI ĐẤU");
        title.setFont(Theme.TITLE);
        title.setForeground(Theme.BLUE);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel desc = new JLabel("Danh sách các trận đấu gần đây của bạn.");
        desc.setFont(Theme.SMALL);
        desc.setForeground(Theme.MUTED_GRAY);
        desc.setAlignmentX(Component.LEFT_ALIGNMENT);

        header.add(title);
        header.add(Box.createVerticalStrut(4));
        header.add(desc);
        add(header, BorderLayout.NORTH);

        // LIST
        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setOpaque(false);
        listPanel.setBorder(new EmptyBorder(0, 24, 24, 24));

        emptyState = new EmptyStatePanel("", "Chưa có lịch sử đấu", "Hãy tham gia các trận đấu để ghi danh vào lịch sử.");
        
        scrollPane = new JScrollPane(listPanel);
        scrollPane.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER_LIGHT));
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void updateData(String rawData, String currentUsername) {
        this.currentUsername = currentUsername;
        listPanel.removeAll();

        if (rawData == null || rawData.isBlank()) {
            listPanel.add(emptyState);
        } else {
            String[] rows = rawData.split("\n");
            for (String row : rows) {
                if (row.isBlank()) continue;
                HistoryCard card = new HistoryCard(row, currentUsername);
                listPanel.add(card);
                listPanel.add(Box.createVerticalStrut(10));
            }
        }
        
        listPanel.revalidate();
        listPanel.repaint();
    }
}
