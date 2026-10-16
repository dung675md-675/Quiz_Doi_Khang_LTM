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
        super.paintComponent(g);
        if (active || hovered) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.setupAntiAliasing(g2);
            
            if (active) {
                g2.setColor(Theme.BLUE);
                g2.fillRoundRect(0, getHeight() - 3, getWidth(), 3, 3, 3);
            } else if (hovered) {
                g2.setColor(new Color(Theme.MUTED_GRAY.getRed(), Theme.MUTED_GRAY.getGreen(), Theme.MUTED_GRAY.getBlue(), 100));
                g2.fillRoundRect(0, getHeight() - 3, getWidth(), 3, 3, 3);
            }
            
            g2.dispose();
        }
    }
}
