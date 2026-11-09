package quiz.ui;

import java.awt.*;
import java.awt.event.MouseAdapter;
import javax.swing.*;

public class DisconnectOverlay extends JComponent {

    public DisconnectOverlay(Runnable onExit) {
        this(onExit, null);
    }

    public DisconnectOverlay(Runnable onExit, Runnable onReturnToLogin) {
        setOpaque(false);
        
        // Bắt mọi sự kiện chuột để không cho tương tác với UI phía dưới
        addMouseListener(new MouseAdapter() {});
        addMouseMotionListener(new MouseAdapter() {});
        
        setLayout(new GridBagLayout());
        
        ModernCard card = new ModernCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setCardBackground(Theme.WHITE);
        card.setCardPadding(32, 40, 32, 40);
        card.setCornerRadius(18);
        
        JComponent iconBadge = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setupAntiAliasing(g2);
                int size = Math.min(getWidth(), getHeight()) - 4;
                int x = (getWidth() - size) / 2;
                int y = (getHeight() - size) / 2;
                g2.setColor(new Color(0xFE, 0xF2, 0xF2));
                g2.fillOval(x, y, size, size);
                g2.setColor(new Color(0xEF, 0x44, 0x44));
                g2.setStroke(new BasicStroke(2.5f));
                g2.drawOval(x + 1, y + 1, size - 2, size - 2);

                g2.setFont(new Font("Segoe UI", Font.BOLD, 32));
                FontMetrics fm = g2.getFontMetrics();
                String mark = "!";
                int tx = x + (size - fm.stringWidth(mark)) / 2;
                int ty = y + (size - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(mark, tx, ty);
                g2.dispose();
            }
        };
        iconBadge.setPreferredSize(new Dimension(56, 56));
        iconBadge.setMaximumSize(new Dimension(56, 56));
        iconBadge.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel lblTitle = new JLabel("MẤT KẾT NỐI", SwingConstants.CENTER);
        lblTitle.setFont(Theme.TITLE_XL);
        lblTitle.setForeground(Theme.RED);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel lblSub = new JLabel("Không thể duy trì kết nối đến máy chủ.", SwingConstants.CENTER);
        lblSub.setFont(Theme.BODY);
        lblSub.setForeground(Theme.DARK_TEXT);
        lblSub.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel lblSub2 = new JLabel("Vui lòng kiểm tra mạng hoặc thử đăng nhập lại.", SwingConstants.CENTER);
        lblSub2.setFont(Theme.BODY);
        lblSub2.setForeground(Theme.DARK_TEXT);
        lblSub2.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        btnRow.setOpaque(false);
        btnRow.setAlignmentX(Component.CENTER_ALIGNMENT);

        if (onReturnToLogin != null) {
            ModernButton btnLogin = new ModernButton("Đăng Nhập Lại", ModernButton.Style.PRIMARY);
            btnLogin.setPreferredSize(new Dimension(140, 40));
            btnLogin.addActionListener(e -> onReturnToLogin.run());
            btnRow.add(btnLogin);
        }

        ModernButton btnExit = new ModernButton("Thoát", onReturnToLogin != null ? ModernButton.Style.SECONDARY : ModernButton.Style.PRIMARY);
        btnExit.setPreferredSize(new Dimension(110, 40));
        btnExit.addActionListener(e -> {
            if (onExit != null) onExit.run();
            else System.exit(1);
        });
        btnRow.add(btnExit);
        
        card.add(iconBadge);
        card.add(Box.createVerticalStrut(14));
        card.add(lblTitle);
        card.add(Box.createVerticalStrut(12));
        card.add(lblSub);
        card.add(Box.createVerticalStrut(4));
        card.add(lblSub2);
        card.add(Box.createVerticalStrut(22));
        card.add(btnRow);
        
        add(card);
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.dispose();
        super.paintComponent(g);
    }
}
