package quiz.ui;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.*;

public class NavigationTab extends JButton {
    
    private boolean active = false;
    private boolean hovered = false;
    
    public NavigationTab(String text) {
        super(text);
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setFont(Theme.BODY_BOLD);
        setForeground(Theme.MUTED_GRAY);
        
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hovered = true;
                repaint();
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                hovered = false;
                repaint();
            }
        });
    }
    
    public void setActive(boolean active) {
        this.active = active;
        if (active) {
            setForeground(Theme.BLUE);
        } else {
            setForeground(Theme.MUTED_GRAY);
        }
        repaint();
    }
    
    public boolean isActive() {
        return active;
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.setupAntiAliasing(g2);
        
        int w = getWidth();
        int h = getHeight();

        // 1. Hover pill background khi chưa active
        if (hovered && !active) {
            g2.setColor(new Color(0xF1, 0xF5, 0xF9));
            g2.fillRoundRect(4, 6, w - 8, h - 16, 8, 8);
        }

        // 2. Active bottom indicator bar
        if (active) {
            g2.setColor(Theme.BLUE);
            g2.fillRoundRect(8, h - 4, w - 16, 4, 4, 4);
        }

        g2.dispose();
        super.paintComponent(g);
    }
}
