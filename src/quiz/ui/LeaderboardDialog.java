package quiz.ui;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * LeaderboardDialog - Hộp thoại hiển thị Bảng xếp hạng cao thủ với giao diện
 * hiện đại,
 * làm nổi bật Top 1-2-3 và các tiêu chí xếp hạng (Điểm, TB đối thủ, Tốc độ).
 */
public class LeaderboardDialog extends JDialog {

    public LeaderboardDialog(Frame owner, String rawData) {
        super(owner, "Bảng Xếp Hạng — Quiz Arena", true);
        setSize(680, 520);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Theme.BG_LIGHT);

        // 1. MODERN HEADER
        JPanel header = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setupAntiAliasing(g2);
                int w = getWidth();
                int h = getHeight();
                GradientPaint gp = new GradientPaint(0, 0, Theme.BLUE, w, h, Theme.INDIGO);
                g2.setPaint(gp);
                g2.fillRect(0, 0, w, h);
                g2.dispose();
            }
        };
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBorder(new EmptyBorder(18, 24, 16, 24));

        JLabel title = new JLabel("BẢNG XẾP HẠNG CAO THỦ 1V1");
        title.setFont(Theme.TITLE);
        title.setForeground(Theme.WHITE);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel desc = new JLabel("Thứ tự ưu tiên: Điểm xếp hạng cao nhất → Điểm TB đối thủ → Tỉ lệ thời gian thắng");
        desc.setFont(Theme.SMALL);
        desc.setForeground(new Color(255, 255, 255, 220));
        desc.setAlignmentX(Component.LEFT_ALIGNMENT);

        header.add(title);
        header.add(Box.createVerticalStrut(4));
        header.add(desc);
        add(header, BorderLayout.NORTH);

        // 2. TABLE
        String[] columnNames = { "Hạng", "Người chơi", "Điểm số", "TB Đối thủ", "Tốc độ thắng" };
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
                if (r.isBlank())
                    continue;
                String[] f = r.split(";");
                if (f.length >= 4) {
                    String rankText = switch (rank) {
                        case 1 -> "TOP 1";
                        case 2 -> "TOP 2";
                        case 3 -> "TOP 3";
                        default -> "#" + rank;
                    };
                    model.addRow(new Object[] { rankText, f[0], f[1] + " pts", f[2], f[3] });
                    rank++;
                }
            }
        }

        JTable table = new JTable(model) {
            @Override
            public Component prepareRenderer(javax.swing.table.TableCellRenderer renderer, int row, int col) {
                Component c = super.prepareRenderer(renderer, row, col);
                if (!isRowSelected(row)) {
                    c.setBackground(row % 2 == 0 ? Theme.WHITE : new Color(0xF8, 0xFA, 0xFC));
                }
                return c;
            }
        };
        table.setRowHeight(42);
        table.setFont(Theme.BODY);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(new Color(0xEE, 0xF2, 0xFF));
        table.setSelectionForeground(Theme.DARK_TEXT);
        table.getTableHeader().setFont(Theme.BODY_BOLD);
        table.getTableHeader().setBackground(new Color(0xF1, 0xF5, 0xF9));
        table.getTableHeader().setForeground(Theme.DARK_TEXT);
        table.getTableHeader().setPreferredSize(new Dimension(0, 40));

        // Rank column renderer with medals
        DefaultTableCellRenderer rankRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                setHorizontalAlignment(SwingConstants.CENTER);
                setFont(Theme.BODY_BOLD);
                String str = String.valueOf(val);
                if ("TOP 1".equals(str)) {
                    setForeground(Theme.GOLD);
                } else if ("TOP 2".equals(str)) {
                    setForeground(Theme.SILVER.darker());
                } else if ("TOP 3".equals(str)) {
                    setForeground(Theme.BRONZE);
                } else {
                    setForeground(Theme.MUTED_GRAY);
                    setFont(Theme.BODY);
                }
                return c;
            }
        };
        table.getColumnModel().getColumn(0).setCellRenderer(rankRenderer);
        table.getColumnModel().getColumn(0).setPreferredWidth(90);

        // Score renderer
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

        // Center remaining columns
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, Theme.BORDER_LIGHT));
        scrollPane.getViewport().setBackground(Theme.WHITE);
        add(scrollPane, BorderLayout.CENTER);

        // 3. FOOTER
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 14));
        footer.setBackground(Theme.WHITE);
        ModernButton btnClose = new ModernButton("Đóng", ModernButton.Style.PRIMARY);
        btnClose.setPreferredSize(new Dimension(110, 38));
        btnClose.addActionListener(e -> {
            SoundManager.playClick();
            dispose();
        });
        footer.add(btnClose);
        add(footer, BorderLayout.SOUTH);
    }
}
