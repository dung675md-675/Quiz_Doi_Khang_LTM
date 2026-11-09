package quiz.ui;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * LeaderboardDialog — Hộp thoại hiển thị Bảng xếp hạng cao thủ với giao diện hiện đại:
 * - Vinh danh bục Top 1-2-3 phong cách Esports rực rỡ.
 * - Danh sách cao thủ chi tiết bên dưới.
 * - Thanh thứ hạng cá nhân ở đáy hộp thoại.
 */
public class LeaderboardDialog extends JDialog {

    private final LeaderboardPanel leaderboardPanel;

    public LeaderboardDialog(Frame owner, String rawData) {
        this(owner, "", rawData);
    }

    public LeaderboardDialog(Frame owner, String currentUsername, String rawData) {
        super(owner, "Bảng Xếp Hạng — Quiz Arena", true);
        setSize(860, 680);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());
        getContentPane().setBackground(ArenaPalette.BG);

        // Modern Header
        JPanel header = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setupAntiAliasing(g2);
                int w = getWidth();
                int h = getHeight();
                GradientPaint gp = new GradientPaint(0, 0, ArenaPalette.CYAN, w, h, Theme.INDIGO);
                g2.setPaint(gp);
                g2.fillRect(0, 0, w, h);
                g2.dispose();
            }
        };
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBorder(new EmptyBorder(16, 24, 14, 24));

        JLabel title = new JLabel("BẢNG XẾP HẠNG CAO THỦ 1V1");
        title.setFont(Theme.TITLE);
        title.setForeground(Theme.WHITE);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel desc = new JLabel("Vinh danh những đấu thủ xuất sắc nhất toàn server • Xếp theo Điểm số tích lũy & Tỉ lệ thắng");
        desc.setFont(Theme.SMALL);
        desc.setForeground(new Color(255, 255, 255, 220));
        desc.setAlignmentX(Component.LEFT_ALIGNMENT);

        header.add(title);
        header.add(Box.createVerticalStrut(4));
        header.add(desc);
        add(header, BorderLayout.NORTH);

        // Center Content: Embed LeaderboardPanel
        leaderboardPanel = new LeaderboardPanel();
        leaderboardPanel.updateData(rawData, currentUsername);
        add(leaderboardPanel, BorderLayout.CENTER);

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
