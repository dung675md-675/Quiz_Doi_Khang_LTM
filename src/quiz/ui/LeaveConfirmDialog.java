package quiz.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.GeneralPath;
import java.awt.geom.RoundRectangle2D;

/**
 * LeaveConfirmDialog - Hộp thoại xác nhận rời trận đấu phong cách Cyberpunk Dark:
 * - Nền tối hiện đại (#0F172A, #1E293B) đồng bộ toàn diện với MatchScreen và ResultDialog.
 * - Huy hiệu cảnh báo Vector phát sáng Neon Đỏ Cam (#F43F5E / #FB7185).
 * - Hộp cảnh báo thủy tinh mờ Glassmorphism hiển thị hậu quả thoát trận (bị xử thua, trừ điểm).
 * - Hai nút hành động rõ ràng: "TIẾP TỤC CHIẾN ĐẤU" (Xanh ngọc) và "XÁC NHẬN RỜI TRẬN" (Đỏ nguy hiểm).
 */
public class LeaveConfirmDialog extends JDialog {

    private boolean confirmed = false;

    public LeaveConfirmDialog(Window owner) {
        super(owner, "Cảnh Báo Thoát Trận — Quiz Arena", ModalityType.APPLICATION_MODAL);
        setSize(480, 340);
        setLocationRelativeTo(owner);
        setResizable(false);
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(0x0F, 0x17, 0x2A));

        // 1. TOP HEADER & WARNING ICON
        JPanel headerPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setupAntiAliasing(g2);
                int w = getWidth();
                int h = getHeight();

                // Nền gradient tối với ánh đỏ cảnh báo nhẹ
                GradientPaint gp = new GradientPaint(
                        0, 0, new Color(0x1E, 0x29, 0x3B),
                        0, h, new Color(0x0F, 0x17, 0x2A)
                );
                g2.setPaint(gp);
                g2.fillRect(0, 0, w, h);

                // Dải viền Neon đỏ cam phát sáng ở trên đỉnh
                g2.setColor(new Color(0xF4, 0x3F, 0x5E));
                g2.fillRect(0, 0, w, 3);

                g2.dispose();
            }
        };
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBorder(new EmptyBorder(20, 24, 10, 24));
        headerPanel.setOpaque(false);

        // 1.1 Vector Warning Icon (Hình tam giác cảnh báo phát sáng)
        JPanel iconBadge = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setupAntiAliasing(g2);
                int size = Math.min(getWidth(), getHeight());

                // Vầng hào quang đỏ cam
                g2.setColor(new Color(0xF4, 0x3F, 0x5E, 35));
                g2.fillOval(0, 0, size, size);

                g2.setColor(new Color(0xF4, 0x3F, 0x5E, 70));
                g2.fillOval(4, 4, size - 8, size - 8);

                // Nền tròn tối bên trong
                g2.setColor(new Color(0x1E, 0x1B, 0x2E));
                g2.fillOval(8, 8, size - 16, size - 16);

                // Viền Neon đỏ
                g2.setColor(new Color(0xFB, 0x71, 0x85));
                g2.setStroke(new BasicStroke(1.8f));
                g2.drawOval(8, 8, size - 16, size - 16);

                // Vẽ dấu chấm than '!' sắc nét
                g2.setColor(new Color(0xFF, 0x4D, 0x6D));
                g2.setFont(new Font("Segoe UI", Font.BOLD, 26));
                FontMetrics fm = g2.getFontMetrics();
                String ex = "!";
                int tx = (size - fm.stringWidth(ex)) / 2;
                int ty = (size - fm.getHeight()) / 2 + fm.getAscent() - 1;
                g2.drawString(ex, tx, ty);

                g2.dispose();
            }
        };
        iconBadge.setPreferredSize(new Dimension(54, 54));
        iconBadge.setMaximumSize(new Dimension(54, 54));
        iconBadge.setOpaque(false);
        iconBadge.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblTitle = new JLabel("CẢNH BÁO RỜI TRẬN ĐẤU", SwingConstants.CENTER);
        lblTitle.setFont(Theme.TITLE);
        lblTitle.setForeground(new Color(0xFB, 0x71, 0x85));
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        headerPanel.add(iconBadge);
        headerPanel.add(Box.createVerticalStrut(10));
        headerPanel.add(lblTitle);

        add(headerPanel, BorderLayout.NORTH);

        // 2. CENTER BODY & NOTICE CARD
        JPanel bodyPanel = new JPanel();
        bodyPanel.setLayout(new BoxLayout(bodyPanel, BoxLayout.Y_AXIS));
        bodyPanel.setBorder(new EmptyBorder(10, 24, 16, 24));
        bodyPanel.setBackground(new Color(0x0F, 0x17, 0x2A));

        JLabel lblQuestion = new JLabel("Bạn có chắc chắn muốn rời khỏi trận đấu ngay bây giờ?", SwingConstants.CENTER);
        lblQuestion.setFont(Theme.BODY_BOLD);
        lblQuestion.setForeground(new Color(0xF1, 0xF5, 0xF9));
        lblQuestion.setAlignmentX(Component.CENTER_ALIGNMENT);

        bodyPanel.add(lblQuestion);
        bodyPanel.add(Box.createVerticalStrut(12));

        // Hộp thông báo hậu quả
        JPanel noticeBox = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setupAntiAliasing(g2);
                int w = getWidth();
                int h = getHeight();

                // Nền kính tối ánh đỏ
                g2.setColor(new Color(0x3B, 0x14, 0x21, 190));
                g2.fill(new RoundRectangle2D.Float(0, 0, w - 1, h - 1, 12, 12));

                // Viền cảnh báo đỏ rực
                g2.setColor(new Color(0xF4, 0x3F, 0x5E, 140));
                g2.setStroke(new BasicStroke(1.2f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 2, h - 2, 12, 12));

                g2.dispose();
            }
        };
        noticeBox.setLayout(new BorderLayout(10, 0));
        noticeBox.setBorder(new EmptyBorder(12, 14, 12, 14));
        noticeBox.setOpaque(false);
        noticeBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 76));

        JLabel lblNoticeText = new JLabel(
                "<html><div style='line-height: 130%; color: #FDA4AF;'>"
                        + "<b>Lưu ý:</b> Thoát trận giữa chừng sẽ tính là <b>BỎ CUỘC</b>.<br>"
                        + "Bạn sẽ bị <b>xử THUA</b> ngay lập tức và <b>bị trừ điểm xếp hạng (-1đ)</b>!</div></html>"
        );
        lblNoticeText.setFont(Theme.BODY);
        noticeBox.add(lblNoticeText, BorderLayout.CENTER);

        bodyPanel.add(noticeBox);
        add(bodyPanel, BorderLayout.CENTER);

        // 3. BOTTOM ACTIONS FOOTER
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 14));
        footerPanel.setBackground(new Color(0x0B, 0x11, 0x20));
        footerPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0x1E, 0x29, 0x3B)));

        ModernButton btnStay = new ModernButton("Ở LẠI CHIẾN ĐẤU", ModernButton.Style.EMERALD);
        btnStay.setPreferredSize(new Dimension(170, 42));
        btnStay.setFont(Theme.BODY_BOLD);
        btnStay.addActionListener(e -> {
            SoundManager.playClick();
            confirmed = false;
            dispose();
        });

        ModernButton btnLeave = new ModernButton("RỜI TRẬN ĐẤU", ModernButton.Style.DANGER);
        btnLeave.setPreferredSize(new Dimension(150, 42));
        btnLeave.setFont(Theme.BODY_BOLD);
        btnLeave.addActionListener(e -> {
            SoundManager.playClick();
            confirmed = true;
            dispose();
        });

        footerPanel.add(btnLeave);
        footerPanel.add(btnStay);
        add(footerPanel, BorderLayout.SOUTH);

        // Phím tắt ESC = Ở lại, Enter = Ở lại (mặc định an toàn)
        KeyAdapter keyAdapter = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    confirmed = false;
                    dispose();
                }
            }
        };
        addKeyListener(keyAdapter);
        btnStay.addKeyListener(keyAdapter);
        btnLeave.addKeyListener(keyAdapter);
    }

    public boolean isConfirmed() {
        return confirmed;
    }
}

