package quiz.ui;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import javax.swing.*;

/**
 * NavigationTab — Tab điều hướng hiện đại:
 * - Tab đang chọn nổi bật với chữ màu xanh đậm và nền pill nhấn nhẹ.
 * - Tab chưa chọn có màu xám thanh lịch, sáng lên khi hover.
 * - Vạch indicator trượt mượt mà được quản lý và vẽ đồng bộ bởi NavigationBar.
 */
public class NavigationTab extends JButton {

    private boolean active = false;
    private boolean hovered = false;

    public NavigationTab(String text) {
        super(text);
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFont(Theme.BODY_BOLD);
        setForeground(Theme.MUTED_GRAY);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hovered = true;
                if (!active) {
                    setForeground(Theme.DARK_TEXT);
                }
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hovered = false;
                if (!active) {
                    setForeground(Theme.MUTED_GRAY);
                }
                repaint();
            }
        });
    }

    public void setActive(boolean active) {
        this.active = active;
        if (active) {
            setForeground(new Color(0x1D, 0x4E, 0xD8)); // Blue-700 nổi bật
        } else {
            setForeground(hovered ? Theme.DARK_TEXT : Theme.MUTED_GRAY);
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

        // 1. Nền pill bo góc khi Active hoặc Hover
        if (active) {
            // Nền xanh nhạt pastel tinh tế tôn vinh tab đang chọn
            g2.setColor(new Color(0x25, 0x63, 0xEB, 16));
            g2.fill(new RoundRectangle2D.Float(4, 8, w - 8, h - 18, 10, 10));
        } else if (hovered) {
            // Nền xám slate nhạt khi hover
            g2.setColor(new Color(0xF1, 0xF5, 0xF9));
            g2.fill(new RoundRectangle2D.Float(4, 8, w - 8, h - 18, 10, 10));
        }

        g2.dispose();
        super.paintComponent(g);
    }
}
