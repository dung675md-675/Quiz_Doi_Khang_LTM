package quiz.ui;

import java.awt.*;
import javax.swing.*;

/**
 * KahootBackgroundPanel - Panel nền có hiệu ứng trang trí các khối hình màu sắc 
 * giống giao diện Kahoot (đỏ, vàng, xanh dương, xanh lá) trên nền tím nhạt.
 */
public class KahootBackgroundPanel extends JPanel {

    public KahootBackgroundPanel(LayoutManager layout) {
        super(layout);
        setOpaque(true);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.setupAntiAliasing(g2);
        
        int w = getWidth();
        int h = getHeight();

        // Nền chính màu tím nhạt/xám sáng Kahoot
        g2.setColor(new Color(242, 243, 248)); // Màu nền pastel sáng
        g2.fillRect(0, 0, w, h);

        // Đặt độ trong suốt mờ để các hình khối không che chữ
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f));

        // Khối 1: Hình vuông đỏ (Kahoot Red: e21b3c)
        g2.setColor(new Color(226, 27, 60));
        g2.fillRoundRect(w - 180, h - 150, 120, 120, 20, 20);

        // Khối 2: Hình tròn vàng (Kahoot Yellow: d89e00)
        g2.setColor(new Color(216, 158, 0));
        g2.fillOval(40, h - 200, 140, 140);

        // Khối 3: Hình thoi xanh dương (Kahoot Blue: 1368ce)
        g2.setColor(new Color(19, 104, 206));
        Polygon diamond = new Polygon();
        diamond.addPoint(150, 60);
        diamond.addPoint(220, 130);
        diamond.addPoint(150, 200);
        diamond.addPoint(80, 130);
        g2.fillPolygon(diamond);

        // Khối 4: Hình tam giác xanh lá (Kahoot Green: 26890c)
        g2.setColor(new Color(38, 137, 12));
        Polygon triangle = new Polygon();
        triangle.addPoint(w - 80, 40);
        triangle.addPoint(w - 20, 130);
        triangle.addPoint(w - 140, 130);
        g2.fillPolygon(triangle);
        
        // Khối phụ: Chấm tròn trang trí
        g2.setColor(Theme.PURPLE);
        g2.fillOval(w / 2 - 20, 80, 40, 40);
        
        g2.setColor(Theme.BLUE);
        g2.fillRoundRect(w / 2 + 150, h - 80, 60, 60, 15, 15);

        g2.dispose();
    }
}
