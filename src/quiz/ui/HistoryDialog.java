package quiz.ui;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * HistoryDialog — Hộp thoại hiển thị Lịch sử thi đấu của người chơi dưới dạng
 * các thẻ trực quan hiện đại, phân biệt rõ Thắng / Thua / Hòa và tỉ số chi tiết.
 * Tích hợp đầy đủ Thống kê thành tích cá nhân (4 thẻ), Bộ lọc và Tìm kiếm.
 */
public class HistoryDialog extends JDialog {

    private final HistoryPanel historyPanel;

    public HistoryDialog(Frame owner, String currentUserName, String rawData) {
        super(owner, "Lịch Sử Thi Đấu — Quiz Arena", true);
        setSize(800, 640);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());
        getContentPane().setBackground(ArenaPalette.BG);

        // Header Banner
        JPanel header = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setupAntiAliasing(g2);
                int w = getWidth();
                int h = getHeight();
                GradientPaint gp = new GradientPaint(0, 0, Theme.INDIGO, w, h, ArenaPalette.CYAN);
                g2.setPaint(gp);
                g2.fillRect(0, 0, w, h);
                g2.dispose();
            }
        };
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBorder(new EmptyBorder(16, 24, 14, 24));

        JLabel title = new JLabel("LỊCH SỬ THI ĐẤU 1V1");
        title.setFont(Theme.TITLE);
        title.setForeground(Theme.WHITE);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel desc = new JLabel("Danh sách các trận đối kháng 1v1 bạn đã tham gia gần đây kèm thống kê sự nghiệp");
        desc.setFont(Theme.SMALL);
        desc.setForeground(new Color(255, 255, 255, 220));
        desc.setAlignmentX(Component.LEFT_ALIGNMENT);

        header.add(title);
        header.add(Box.createVerticalStrut(4));
        header.add(desc);
        add(header, BorderLayout.NORTH);

        // Center Content: Embed HistoryPanel
        historyPanel = new HistoryPanel();
        historyPanel.updateData(rawData, currentUserName);
        add(historyPanel, BorderLayout.CENTER);

        // Footer Bar
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 12));
        footer.setBackground(ArenaPalette.SURFACE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, ArenaPalette.BORDER));
        ModernButton btnClose = new ModernButton("Đóng", ModernButton.Style.PRIMARY);
        btnClose.setArenaStyle(true);
        btnClose.setPreferredSize(new Dimension(110, 38));
        btnClose.addActionListener(e -> {
            SoundManager.playClick();
            dispose();
        });
        footer.add(btnClose);
        add(footer, BorderLayout.SOUTH);
        ArenaPalette.styleTree(getContentPane());
    }
}
