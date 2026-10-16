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
        ANSWER_D,       // Xanh lá #26890C (Chữ trắng)
        EMERALD,        // Xanh ngọc lục bảo tươi #10B981
        ORANGE,         // Cam tươi #F97316
        SKY_BLUE,       // Xanh da trời tươi #0EA5E9
        SECONDARY_SKY,  // Viền xanh da trời, nền sáng
        TRUE_BLUE,      // Đúng: Xanh dương #1368CE
        FALSE_YELLOW    // Sai: Vàng tươi #D97706 / #EAB308
    }

    private Style style = Style.PRIMARY;
    private int cornerRadius = 10;
    private boolean isHovered = false;
    private boolean isPressed = false;
    private boolean isSelectedState = false;
    private String prefixBadge = null; // Ví dụ: "A", "B", "C", "D" hoặc "✓", "✗"
    private float animAlpha = 1.0f;
    private int animOffsetY = 0;
    private float animScale = 1.0f;
    private Color revealBgColor = null;
    private Color revealFgColor = null;

    public void setAnimState(float alpha, int offsetY) {
        this.animAlpha = Math.max(0.0f, Math.min(1.0f, alpha));
        this.animOffsetY = offsetY;
        repaint();
    }

    public float getAnimAlpha() { return animAlpha; }
    public int getAnimOffsetY() { return animOffsetY; }

    public void setAnimScale(float scale) {
        this.animScale = scale;
        repaint();
    }

    public float getAnimScale() { return animScale; }

    public void setRevealColors(Color bg, Color fg) {
        this.revealBgColor = bg;
        this.revealFgColor = fg;
        repaint();
    }

    public void resetReveal() {
        this.revealBgColor = null;
        this.revealFgColor = null;
        this.animScale = 1.0f;
        repaint();
    }

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
        if (animAlpha <= 0.001f) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.setupAntiAliasing(g2);
        if (animAlpha < 1.0f) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, animAlpha));
        }
        if (animOffsetY != 0) {
            g2.translate(0, animOffsetY);
        }
        if (animScale != 1.0f) {
            try {
                g2.setClip(null);
            } catch (Exception ignored) {}
            int cx = getWidth() / 2;
            int cy = getHeight() / 2;
            g2.translate(cx, cy);
            g2.scale(animScale, animScale);
            g2.translate(-cx, -cy);
        }

        int w = getWidth();
        int h = getHeight();

        Color bg = resolveBgColor();
        Color fg = resolveFgColor();
        Color border = resolveBorderColor();

        // 1. Vẽ nền bo góc
        g2.setColor(bg);
        g2.fill(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, cornerRadius, cornerRadius));

        // 2. Vẽ viền nếu có (hoặc khi reveal / selected)
        if (revealBgColor != null) {
            Color revealBorder = (revealBgColor.equals(new Color(0x16, 0xA3, 0x4A)) || revealBgColor.equals(Theme.GREEN))
                    ? new Color(0x86, 0xEF, 0xAC)
                    : new Color(0xFE, 0xCA, 0xCA);
            g2.setColor(revealBorder);
            g2.setStroke(new BasicStroke(2.5f));
            g2.draw(new RoundRectangle2D.Float(1.5f, 1.5f, w - 3, h - 3, cornerRadius, cornerRadius));
        } else if (isSelectedState) {
            g2.setColor(Theme.PURPLE);
            g2.setStroke(new BasicStroke(3f));
            g2.draw(new RoundRectangle2D.Float(1.5f, 1.5f, w - 3, h - 3, cornerRadius, cornerRadius));
        } else if (border != null) {
            g2.setColor(border);
            g2.setStroke(new BasicStroke(1.5f));
            g2.draw(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, cornerRadius, cornerRadius));
        }

        String text = getText();

        // Kiểm tra phong cách ĐÚNG / SAI đặc thù để căn giữa cả biểu tượng và chữ
        boolean isTfStyle = (style == Style.TRUE_BLUE || style == Style.FALSE_YELLOW);
        if (isTfStyle && prefixBadge != null && !prefixBadge.isEmpty()) {
            int badgeSize = Math.max(34, Math.min(46, h - 24));
            int badgeSpacing = 16;
            Font textFont = getFont();
            FontMetrics fmText = g2.getFontMetrics(textFont);
            int textW = (text != null) ? fmText.stringWidth(text) : 0;
            int totalW = badgeSize + badgeSpacing + textW;
            int startX = Math.max(14, (w - totalW) / 2);

            int badgeY = (h - badgeSize) / 2;
            g2.setColor(new Color(0, 0, 0, 45));
            g2.fillRoundRect(startX, badgeY, badgeSize, badgeSize, 10, 10);

            g2.setColor(fg);
            g2.setFont(new Font("Segoe UI", Font.BOLD, Math.max(20, badgeSize - 12)));
            FontMetrics fmBadge = g2.getFontMetrics();
            int bx = startX + (badgeSize - fmBadge.stringWidth(prefixBadge)) / 2;
            int by = badgeY + ((badgeSize - fmBadge.getHeight()) / 2) + fmBadge.getAscent();
            g2.drawString(prefixBadge, bx, by);

            if (text != null && !text.isEmpty()) {
                g2.setFont(textFont);
                int tx = startX + badgeSize + badgeSpacing;
                int ty = (h - fmText.getHeight()) / 2 + fmText.getAscent();
                g2.drawString(text, tx, ty);
            }
            g2.dispose();
            return;
        }

        // 3. Vẽ badge tiền tố nếu có (A, B, C, D)
        int textOffsetLeft = 0;
        if (prefixBadge != null && !prefixBadge.isEmpty()) {
            int badgeSize = Math.max(24, Math.min(38, h - 16));
            int badgeX = 14;
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

        // 4. Vẽ text nhãn (Hỗ trợ tự động xuống dòng và co giãn cỡ chữ để hiển thị đầy đủ)
        if (text != null && !text.isEmpty()) {
            int startX = (prefixBadge != null) ? (14 + textOffsetLeft) : 14;
            int availableWidth = (prefixBadge != null) ? (w - startX - 14) : (w - 28);
            drawWrappedOrCenteredText(g2, text, startX, availableWidth, w, h, fg, prefixBadge != null);
        }

        g2.dispose();
    }

    private void drawWrappedOrCenteredText(Graphics2D g2, String text, int startX, int availableWidth, int buttonWidth, int buttonHeight, Color fg, boolean hasPrefix) {
        g2.setColor(fg);
        Font baseFont = getFont();
        g2.setFont(baseFont);
        FontMetrics fm = g2.getFontMetrics();

        // Nếu văn bản ngắn vừa vặn 1 dòng
        if (fm.stringWidth(text) <= availableWidth) {
            int tx = hasPrefix ? startX : ((buttonWidth - fm.stringWidth(text)) / 2);
            int ty = (buttonHeight - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(text, tx, ty);
            return;
        }

        // Tự động ngắt dòng theo từ
        java.util.List<String> lines = breakIntoLines(text, fm, availableWidth);

        // Nếu có nhiều hơn 2 dòng hoặc chiều cao vượt quá nút, hạ nhẹ cỡ chữ
        if (lines.size() > 2 || (lines.size() * fm.getHeight() > buttonHeight - 14)) {
            Font smallerFont = baseFont.deriveFont(Math.max(11f, baseFont.getSize2D() - 2.5f));
            g2.setFont(smallerFont);
            fm = g2.getFontMetrics();
            lines = breakIntoLines(text, fm, availableWidth);
        }

        int lineHeight = fm.getHeight();
        int totalHeight = lines.size() * lineHeight;
        int startY = (buttonHeight - totalHeight) / 2 + fm.getAscent();

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            int tx = hasPrefix ? startX : ((buttonWidth - fm.stringWidth(line)) / 2);
            g2.drawString(line, tx, startY + i * lineHeight);
        }
    }

    private java.util.List<String> breakIntoLines(String text, FontMetrics fm, int maxWidth) {
        java.util.List<String> lines = new java.util.ArrayList<>();
        String[] words = text.split("\\s+");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            String candidate = currentLine.length() == 0 ? word : currentLine + " " + word;
            if (fm.stringWidth(candidate) <= maxWidth) {
                currentLine = new StringBuilder(candidate);
            } else {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                } else {
                    lines.add(word);
                    currentLine = new StringBuilder();
                }
            }
        }
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }
        return lines;
    }

    private Color resolveBgColor() {
        if (revealBgColor != null) {
            return revealBgColor;
        }
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

            case EMERALD:
                if (isPressed) return Theme.EMERALD_DARK.darker();
                if (isHovered) return Theme.EMERALD_DARK;
                return Theme.EMERALD;

            case ORANGE:
                if (isPressed) return Theme.ORANGE_DARK.darker();
                if (isHovered) return Theme.ORANGE_DARK;
                return Theme.ORANGE;

            case SKY_BLUE:
                if (isPressed) return Theme.SKY_BLUE.darker().darker();
                if (isHovered) return Theme.SKY_BLUE.darker();
                return Theme.SKY_BLUE;

            case SECONDARY_SKY:
                if (isPressed) return new Color(224, 242, 254);
                if (isHovered) return Theme.SKY_LIGHT;
                return Theme.WHITE;

            case TRUE_BLUE:
                if (isPressed) return new Color(0x0F, 0x55, 0xA8);
                if (isHovered) return new Color(0x1E, 0x7E, 0xF5);
                return new Color(0x13, 0x68, 0xCE);

            case FALSE_YELLOW:
                if (isPressed) return new Color(0xB4, 0x53, 0x09);
                if (isHovered) return new Color(0xF5, 0x9E, 0x0B);
                return new Color(0xD9, 0x77, 0x06);

            default:
                return Theme.BLUE;
        }
    }

    private Color resolveFgColor() {
        if (revealFgColor != null) {
            return revealFgColor;
        }
        if (!isEnabled()) {
            return Theme.WHITE;
        }

        switch (style) {
            case SECONDARY:
                return Theme.BLUE;
            case SECONDARY_SKY:
                return Theme.SKY_BLUE.darker();
            case OUTLINE_GRAY:
                return Theme.DARK_TEXT;
            case ANSWER_C:
                // Theo hướng dẫn: Nền vàng dùng chữ tối #252525 để đảm bảo tương phản
                return Theme.DARK_TEXT;
            case TRUE_BLUE:
            case FALSE_YELLOW:
                return Theme.WHITE;
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
            case SECONDARY_SKY:
                return isHovered ? Theme.SKY_BLUE.darker() : Theme.SKY_BLUE;
            case OUTLINE_GRAY:
                return isHovered ? Theme.MUTED_GRAY : Theme.BORDER_LIGHT;
            default:
                return null;
        }
    }
}
