package quiz.ui;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import javax.swing.*;

/**
 * ModernButton - Nút bấm giao diện phẳng, bo góc hiện đại, hỗ trợ hiệu ứng hover/pressed
 * và các biến thể phong cách theo Color Guide.
 */
public class ModernButton extends JButton {

    public enum Style {
        PRIMARY,        // Màu xanh chính #1368CE
        SECONDARY,      // Viền xanh, nền trắng
        ACCENT_PURPLE,  // Màu tím accent #864CBF
        SUCCESS,        // Màu xanh lá #26890C
        DANGER,         // Màu đỏ #E21B3C
        OUTLINE_GRAY,   // Viền xám, nền trắng
        ANSWER_A,       // Đỏ #E21B3C (Chữ trắng)
        ANSWER_B,       // Xanh dương #1368CE (Chữ trắng)
        ANSWER_C,       // Vàng đậm #D89E00 (Chữ tối #252525 đảm bảo tương phản cao)
        ANSWER_D        // Xanh lá #26890C (Chữ trắng)
    }

    private Style style = Style.PRIMARY;
    private int cornerRadius = 10;
    private boolean isHovered = false;
    private boolean isPressed = false;
    private boolean isSelectedState = false;
    private String prefixBadge = null; // Ví dụ: "A", "B", "C", "D"

    public ModernButton(String text) {
        this(text, Style.PRIMARY);
    }

    public ModernButton(String text, Style style) {
        super(text);
        this.style = style;
        init();
    }

    public ModernButton(String text, Style style, String prefixBadge) {
        super(text);
        this.style = style;
        this.prefixBadge = prefixBadge;
        init();
    }

    private void init() {
        setOpaque(false);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setFont(Theme.BODY_BOLD);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setMargin(new Insets(10, 18, 10, 18));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (isEnabled()) {
                    isHovered = true;
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (isEnabled()) {
                    isPressed = true;
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                isPressed = false;
                repaint();
            }
        });
    }

    public void setStyle(Style style) {
        this.style = style;
        repaint();
    }

    public Style getStyle() {
        return style;
    }

    public void setCornerRadius(int radius) {
        this.cornerRadius = radius;
        repaint();
    }

    public void setSelectedState(boolean selected) {
        this.isSelectedState = selected;
        repaint();
    }

    public boolean isSelectedState() {
        return isSelectedState;
    }

    public void setPrefixBadge(String badge) {
        this.prefixBadge = badge;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.setupAntiAliasing(g2);

        int w = getWidth();
        int h = getHeight();

        Color bg = resolveBgColor();
        Color fg = resolveFgColor();
        Color border = resolveBorderColor();

        // 1. Vẽ nền bo góc
        g2.setColor(bg);
        g2.fill(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, cornerRadius, cornerRadius));

        // 2. Vẽ viền nếu có (hoặc khi selected)
        if (isSelectedState) {
            g2.setColor(Theme.PURPLE);
            g2.setStroke(new BasicStroke(3f));
            g2.draw(new RoundRectangle2D.Float(1.5f, 1.5f, w - 3, h - 3, cornerRadius, cornerRadius));
        } else if (border != null) {
            g2.setColor(border);
            g2.setStroke(new BasicStroke(1.5f));
            g2.draw(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, cornerRadius, cornerRadius));
        }

        // 3. Vẽ badge tiền tố nếu có (dành riêng cho A, B, C, D)
        int textOffsetLeft = 0;
        if (prefixBadge != null && !prefixBadge.isEmpty()) {
            int badgeSize = Math.max(24, h - 16);
            int badgeX = 12;
            int badgeY = (h - badgeSize) / 2;

            g2.setColor(new Color(0, 0, 0, 40));
            g2.fillRoundRect(badgeX, badgeY, badgeSize, badgeSize, 6, 6);

            g2.setColor(fg);
            g2.setFont(Theme.BODY_BOLD);
            FontMetrics fmBadge = g2.getFontMetrics();
            int bx = badgeX + (badgeSize - fmBadge.stringWidth(prefixBadge)) / 2;
            int by = badgeY + ((badgeSize - fmBadge.getHeight()) / 2) + fmBadge.getAscent();
            g2.drawString(prefixBadge, bx, by);

            textOffsetLeft = badgeSize + 16;
        }

        // 4. Vẽ text nhãn
        g2.setColor(fg);
        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        String text = getText();

        if (text != null && !text.isEmpty()) {
            int tx;
            if (prefixBadge != null) {
                tx = 12 + textOffsetLeft;
            } else {
                tx = (w - fm.stringWidth(text)) / 2;
            }
            int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(text, tx, ty);
        }

        g2.dispose();
    }

    private Color resolveBgColor() {
        if (!isEnabled()) {
            return Theme.DISABLED;
        }

        switch (style) {
            case PRIMARY:
                if (isPressed) return Theme.BLUE_HOVER.darker();
                if (isHovered) return Theme.BLUE_HOVER;
                return Theme.BLUE;

            case SECONDARY:
            case OUTLINE_GRAY:
                if (isPressed) return new Color(230, 235, 245);
                if (isHovered) return new Color(243, 246, 252);
                return Theme.WHITE;

            case ACCENT_PURPLE:
                if (isPressed) return Theme.PURPLE_HOVER.darker();
                if (isHovered) return Theme.PURPLE_HOVER;
                return Theme.PURPLE;

            case SUCCESS:
            case ANSWER_D:
                if (isPressed) return Theme.GREEN.darker();
                if (isHovered) return new Color(0x1F, 0x75, 0x0A);
                return Theme.GREEN;

            case DANGER:
            case ANSWER_A:
                if (isPressed) return Theme.RED.darker();
                if (isHovered) return new Color(0xC7, 0x15, 0x32);
                return Theme.RED;

            case ANSWER_B:
                if (isPressed) return Theme.BLUE_HOVER.darker();
                if (isHovered) return Theme.BLUE_HOVER;
                return Theme.BLUE;

            case ANSWER_C:
                if (isPressed) return new Color(0xBD, 0x8A, 0x00);
                if (isHovered) return new Color(0xEB, 0xAC, 0x00);
                return Theme.YELLOW;

            default:
                return Theme.BLUE;
        }
    }

    private Color resolveFgColor() {
        if (!isEnabled()) {
            return Theme.WHITE;
        }

        switch (style) {
            case SECONDARY:
                return Theme.BLUE;
            case OUTLINE_GRAY:
                return Theme.DARK_TEXT;
            case ANSWER_C:
                // Theo hướng dẫn: Nền vàng dùng chữ tối #252525 để đảm bảo tương phản
                return Theme.DARK_TEXT;
            default:
                return Theme.WHITE;
        }
    }

    private Color resolveBorderColor() {
        if (!isEnabled()) {
            return null;
        }

        switch (style) {
            case SECONDARY:
                return isHovered ? Theme.BLUE_HOVER : Theme.BLUE;
            case OUTLINE_GRAY:
                return isHovered ? Theme.MUTED_GRAY : Theme.BORDER_LIGHT;
            default:
                return null;
        }
    }
}
