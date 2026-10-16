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

        // 1. Nền chuyển sắc nhẹ nhàng phong cách game hiện đại
        GradientPaint bgGradient = new GradientPaint(
            0, 0, new Color(245, 247, 252),
            w, h, new Color(236, 240, 249)
        );
        g2.setPaint(bgGradient);
        g2.fillRect(0, 0, w, h);

        // 2. Vẽ lưới chấm hạt mịn tinh tế (dot grid pattern)
        g2.setColor(new Color(180, 195, 220, 45));
        for (int x = 20; x < w; x += 36) {
            for (int y = 20; y < h; y += 36) {
                g2.fillOval(x, y, 2, 2);
            }
        }

        // 3. Các khối hình biểu tượng Kahoot Quiz với độ trong suốt mềm mại
        // Khối 1: Hình vuông đỏ (Kahoot Red: e21b3c) góc dưới phải
        g2.setColor(new Color(226, 27, 60, 40));
        g2.fillRoundRect(w - 180, h - 160, 130, 130, 26, 26);

        // Khối 2: Hình tròn vàng (Kahoot Yellow: d89e00) góc dưới trái
        g2.setColor(new Color(216, 158, 0, 42));
        g2.fillOval(35, h - 190, 130, 130);

        // Khối 3: Hình thoi xanh dương (Kahoot Blue: 1368ce) góc trên trái
        g2.setColor(new Color(19, 104, 206, 42));
        Polygon diamond = new Polygon();
        diamond.addPoint(120, 40);
        diamond.addPoint(180, 100);
        diamond.addPoint(120, 160);
        diamond.addPoint(60, 100);
        g2.fillPolygon(diamond);

        // Khối 4: Hình tam giác xanh lá (Kahoot Green: 26890c) góc trên phải
        g2.setColor(new Color(38, 137, 12, 42));
        Polygon triangle = new Polygon();
        triangle.addPoint(w - 90, 35);
        triangle.addPoint(w - 25, 125);
        triangle.addPoint(w - 155, 125);
        g2.fillPolygon(triangle);
        
        // Khối phụ: Vòng sáng tím và cyan
        g2.setColor(new Color(134, 76, 191, 35));
        g2.fillOval(w / 2 - 250, h / 2 - 200, 100, 100);
        
        g2.setColor(new Color(14, 165, 233, 35));
        g2.fillRoundRect(w / 2 + 180, h / 2 + 60, 80, 80, 20, 20);

        g2.dispose();
    }
}
