package quiz.ui;

import java.awt.*;
import java.awt.event.MouseAdapter;
import javax.swing.*;

public class DisconnectOverlay extends JComponent {

    public DisconnectOverlay(Runnable onExit) {
        setOpaque(false);
        
        // Bắt mọi sự kiện chuột để không cho tương tác với UI phía dưới
        addMouseListener(new MouseAdapter() {});
        addMouseMotionListener(new MouseAdapter() {});
        
        setLayout(new GridBagLayout());
        
        ModernCard card = new ModernCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setCardBackground(Theme.WHITE);
        card.setCardPadding(32, 40, 32, 40);
        
        JLabel lblIcon = new JLabel("!", SwingConstants.CENTER);
        lblIcon.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 48));
        lblIcon.setForeground(Theme.RED);
        lblIcon.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel lblTitle = new JLabel("MẤT KẾT NỐI", SwingConstants.CENTER);
        lblTitle.setFont(Theme.TITLE_XL);
        lblTitle.setForeground(Theme.RED);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel lblSub = new JLabel("Không thể kết nối đến máy chủ.", SwingConstants.CENTER);
        lblSub.setFont(Theme.BODY);
        lblSub.setForeground(Theme.DARK_TEXT);
        lblSub.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel lblSub2 = new JLabel("Vui lòng kiểm tra mạng và khởi động lại ứng dụng.", SwingConstants.CENTER);
        lblSub2.setFont(Theme.BODY);
        lblSub2.setForeground(Theme.DARK_TEXT);
        lblSub2.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        ModernButton btnExit = new ModernButton("Thoát", ModernButton.Style.PRIMARY);
        btnExit.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnExit.addActionListener(e -> {
            if (onExit != null) onExit.run();
            else System.exit(1);
        });
        
        card.add(lblIcon);
        card.add(Box.createVerticalStrut(16));
        card.add(lblTitle);
        card.add(Box.createVerticalStrut(16));
        card.add(lblSub);
        card.add(Box.createVerticalStrut(4));
        card.add(lblSub2);
        card.add(Box.createVerticalStrut(24));
        card.add(btnExit);
        
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
