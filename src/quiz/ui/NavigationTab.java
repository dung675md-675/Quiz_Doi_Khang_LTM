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
    private boolean sidebarMode;

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
                    setForeground(sidebarMode ? ArenaPalette.TEXT : Theme.DARK_TEXT);
                }
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hovered = false;
                if (!active) {
                    setForeground(sidebarMode ? ArenaPalette.MUTED : Theme.MUTED_GRAY);
                }
                repaint();
            }
        });
    }

    public void setActive(boolean active) {
        this.active = active;
        if (sidebarMode) {
            setForeground(active ? ArenaPalette.ACCENT : ArenaPalette.MUTED);
        } else if (active) {
            setForeground(new Color(0x1D, 0x4E, 0xD8)); // Blue-700 nổi bật
        } else {
            setForeground(hovered ? Theme.DARK_TEXT : Theme.MUTED_GRAY);
        }
        repaint();
    }

    public boolean isActive() {
        return active;
    }

    public void setSidebarMode(boolean enabled) {
        sidebarMode = enabled;
        setHorizontalAlignment(enabled ? SwingConstants.LEFT : SwingConstants.CENTER);
        setBorder(enabled ? BorderFactory.createEmptyBorder(10, 9, 10, 9) : null);
        setIcon(enabled ? new ArenaIcon(iconFor(getText()), 17, null) : null);
        setIconTextGap(7);
        setToolTipText(getText());
        setActive(active);
    }

    private static ArenaIcon.Type iconFor(String text) {
        return switch (text) {
            case "Người Chơi" -> ArenaIcon.Type.PLAYERS;
            case "Bạn Bè" -> ArenaIcon.Type.FRIENDS;
            case "Bảng Xếp Hạng" -> ArenaIcon.Type.TROPHY;
            case "Lịch Sử" -> ArenaIcon.Type.HISTORY;
            case "Ngân Hàng Câu Hỏi" -> ArenaIcon.Type.BOOK;
            default -> ArenaIcon.Type.SETTINGS;
        };
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.setupAntiAliasing(g2);

        int w = getWidth();
        int h = getHeight();

        if (sidebarMode) {
            if (active || hovered || hasFocus()) {
                g2.setColor(active ? new Color(34, 51, 47) : ArenaPalette.SURFACE_RAISED);
                g2.fillRoundRect(0, 0, w, h, 14, 14);
            }
            if (active) {
                g2.setColor(ArenaPalette.ACCENT);
                g2.fillRoundRect(0, 12, 3, h - 24, 3, 3);
            }
            if (hasFocus()) {
                g2.setColor(ArenaPalette.CYAN);
                g2.drawRoundRect(1, 1, w - 3, h - 3, 12, 12);
            }
            g2.dispose();
            super.paintComponent(g);
            return;
        }

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
