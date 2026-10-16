package quiz.ui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Arc2D;
import javax.swing.*;

public class LoadingSpinner extends JComponent {
    private int angle = 0;
    private final Timer timer;
    private final int strokeWidth = 4;

    public LoadingSpinner() {
        setPreferredSize(new Dimension(48, 48));
        setOpaque(false);
        timer = new Timer(50, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                angle = (angle + 15) % 360;
                repaint();
            }
        });
    }

    @Override
    public void addNotify() {
        super.addNotify();
        timer.start();
    }

    @Override
    public void removeNotify() {
        timer.stop();
        super.removeNotify();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.setupAntiAliasing(g2);
        
        int width = getWidth();
        int height = getHeight();
        int size = Math.min(width, height) - strokeWidth;
        int x = (width - size) / 2;
        int y = (height - size) / 2;

        g2.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        
        // Vẽ vòng trong suốt
        g2.setColor(new Color(Theme.BLUE.getRed(), Theme.BLUE.getGreen(), Theme.BLUE.getBlue(), 50));
        g2.drawArc(x, y, size, size, 0, 360);
        
        // Vẽ phần cung quay
        g2.setColor(Theme.BLUE);
        g2.draw(new Arc2D.Double(x, y, size, size, -angle, 90, Arc2D.OPEN));
        
        g2.dispose();
    }
}
