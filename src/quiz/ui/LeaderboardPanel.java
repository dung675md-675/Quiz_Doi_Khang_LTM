package quiz.ui;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

public class LeaderboardPanel extends JPanel {

    private final JTable table;
    private final DefaultTableModel model;
    private String currentUsername;

    public LeaderboardPanel() {
        setLayout(new BorderLayout());
        setOpaque(false);

        // HEADER
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(16, 24, 12, 24));

        JLabel title = new JLabel("BẢNG XẾP HẠNG CAO THỦ");
        title.setFont(Theme.TITLE);
        title.setForeground(Theme.BLUE);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel desc = new JLabel("Xếp theo: Điểm số giảm dần → Điểm TB đối thủ → Tốc độ thắng");
        desc.setFont(Theme.SMALL);
        desc.setForeground(Theme.MUTED_GRAY);
        desc.setAlignmentX(Component.LEFT_ALIGNMENT);

        header.add(title);
        header.add(Box.createVerticalStrut(4));
        header.add(desc);
        add(header, BorderLayout.NORTH);

        // TABLE
        String[] columnNames = {"Hạng", "Người chơi", "Rank", "Điểm số", "TB Đối thủ", "Tốc độ", "Tổng trận", "Tỉ lệ thắng"};
        model = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(model);
        table.setRowHeight(40);
        table.setFont(Theme.BODY);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFillsViewportHeight(true);
        
        table.getTableHeader().setFont(Theme.BODY_BOLD);
        table.getTableHeader().setBackground(Theme.OFF_WHITE);
        table.getTableHeader().setForeground(Theme.DARK_TEXT);
        table.getTableHeader().setPreferredSize(new Dimension(0, 36));

        // Center renderers
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(0).setPreferredWidth(60);
        table.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(5).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(6).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(7).setCellRenderer(centerRenderer);

        // Rank column styled purple
        DefaultTableCellRenderer rankRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                setForeground(Theme.PURPLE);
                setFont(Theme.BODY_BOLD);
                setHorizontalAlignment(SwingConstants.CENTER);
                return c;
            }
        };
        table.getColumnModel().getColumn(2).setCellRenderer(rankRenderer);

        DefaultTableCellRenderer scoreRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                setForeground(Theme.BLUE);
                setFont(Theme.BODY_BOLD);
                setHorizontalAlignment(SwingConstants.CENTER);
                return c;
            }
        };
        table.getColumnModel().getColumn(3).setCellRenderer(scoreRenderer);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER_LIGHT));
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void updateData(String rawData, String currentUsername) {
        this.currentUsername = currentUsername;
        model.setRowCount(0);

        if (rawData == null || rawData.isBlank()) return;

        String[] rows = rawData.split("\\|");
        int rank = 1;
        for (String r : rows) {
            if (r.isBlank()) continue;
            String[] f = r.split(";");
            if (f.length >= 6) {
                String rankText = switch (rank) {
                    case 1 -> "TOP 1";
                    case 2 -> "TOP 2";
                    case 3 -> "TOP 3";
                    default -> "#" + rank;
                };
                // Format: name;score;avgOpp;speed;totalMatches;winRate;rankLabel
                String rankLabel = f.length >= 7 ? f[6] : quiz.Rank.fromScore(parseScore(f[1])).getDisplayName();
                model.addRow(new Object[]{rankText, f[0], rankLabel, f[1] + " pts", f[2], f[3], f[4], f[5]});
                rank++;
            }
        }
        
        // Highlight current user
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String name = (String) model.getValueAt(row, 1);
                if (name != null && name.equals(currentUsername)) {
                    c.setBackground(new Color(230, 242, 255));
                    c.setFont(Theme.BODY_BOLD);
                } else {
                    c.setBackground(isSelected ? table.getSelectionBackground() : Theme.WHITE);
                    c.setFont(Theme.BODY);
                }
                return c;
            }
        });
    }

    private int parseScore(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return 0; }
    }
}
