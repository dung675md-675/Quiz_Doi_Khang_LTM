package quiz.ui;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * LeaderboardDialog - Hộp thoại hiển thị Bảng xếp hạng cao thủ với giao diện hiện đại,
 * làm nổi bật Top 1-2-3 và các tiêu chí xếp hạng (Điểm, TB đối thủ, Tốc độ).
 */
public class LeaderboardDialog extends JDialog {

    public LeaderboardDialog(Frame owner, String rawData) {
        super(owner, "Bảng Xếp Hạng — Quiz Arena", true);
        setSize(650, 480);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Theme.WHITE);

        // 1. HEADER
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(Theme.WHITE);
        header.setBorder(new EmptyBorder(20, 24, 12, 24));

        JLabel title = new JLabel("🏆 BẢNG XẾP HẠNG CAO THỦ");
        title.setFont(Theme.TITLE);
        title.setForeground(Theme.BLUE);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel desc = new JLabel("Xếp theo: Điểm số giảm dần → Điểm TB đối thủ → Tỉ lệ thời gian thắng");
        desc.setFont(Theme.SMALL);
        desc.setForeground(Theme.MUTED_GRAY);
        desc.setAlignmentX(Component.LEFT_ALIGNMENT);

        header.add(title);
        header.add(Box.createVerticalStrut(4));
        header.add(desc);
        add(header, BorderLayout.NORTH);

        // 2. TABLE
        String[] columnNames = {"Hạng", "Người chơi", "Điểm số", "TB Đối thủ", "Tốc độ thắng"};
        DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        if (rawData != null && !rawData.isBlank()) {
            String[] rows = rawData.split("\\|");
            int rank = 1;
            for (String r : rows) {
                if (r.isBlank()) continue;
                String[] f = r.split(";");
                if (f.length >= 4) {
                    String rankText = switch (rank) {
                        case 1 -> "🥇 #1";
                        case 2 -> "🥈 #2";
                        case 3 -> "🥉 #3";
                        default -> "    #" + rank;
                    };
                    model.addRow(new Object[]{rankText, f[0], f[1] + " pts", f[2], f[3]});
                    rank++;
                }
            }
        }

        JTable table = new JTable(model);
        table.setRowHeight(38);
        table.setFont(Theme.BODY);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(new Color(0xEE, 0xF2, 0xFF));
        table.getTableHeader().setFont(Theme.BODY_BOLD);
        table.getTableHeader().setBackground(Theme.OFF_WHITE);
        table.getTableHeader().setForeground(Theme.DARK_TEXT);
        table.getTableHeader().setPreferredSize(new Dimension(0, 36));

        // Center renderers
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(0).setPreferredWidth(80);

        DefaultTableCellRenderer scoreRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                setForeground(Theme.PURPLE);
                setFont(Theme.BODY_BOLD);
                setHorizontalAlignment(SwingConstants.CENTER);
                return c;
            }
        };
        table.getColumnModel().getColumn(2).setCellRenderer(scoreRenderer);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, Theme.BORDER_LIGHT));
        scrollPane.getViewport().setBackground(Theme.WHITE);
        add(scrollPane, BorderLayout.CENTER);

        // 3. FOOTER
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 14));
        footer.setBackground(Theme.WHITE);
        ModernButton btnClose = new ModernButton("Đóng", ModernButton.Style.PRIMARY);
        btnClose.setPreferredSize(new Dimension(100, 36));
        btnClose.addActionListener(e -> dispose());
        footer.add(btnClose);
        add(footer, BorderLayout.SOUTH);
    }
}
