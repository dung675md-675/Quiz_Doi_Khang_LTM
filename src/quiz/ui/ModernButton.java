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
    private float bounceScale = 1.0f;
    private Timer bounceTimer = null;
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
    public String getPrefixBadge() { return prefixBadge; }

    public void triggerBounce() {
        if (bounceTimer != null && bounceTimer.isRunning()) {
            bounceTimer.stop();
        }
        final long start = System.currentTimeMillis();
        final int duration = 220;
        bounceTimer = new Timer(16, e -> {
            long elapsed = System.currentTimeMillis() - start;
            if (elapsed >= duration) {
                bounceScale = 1.0f;
                ((Timer) e.getSource()).stop();
                repaint();
            } else {
                float p = (float) elapsed / duration;
                // Nhịp lún nhẹ rồi nảy đàn hồi (0.95 -> 1.035 -> 1.0)
                bounceScale = 1.0f - 0.05f * (float) Math.sin(p * Math.PI * 2.0) * (1.0f - p);
                repaint();
            }
        });
        bounceTimer.start();
    }

    public void setRevealColors(Color bg, Color fg) {
        this.revealBgColor = bg;
        this.revealFgColor = fg;
        repaint();
    }

    public void resetReveal() {
        this.revealBgColor = null;
        this.revealFgColor = null;
        this.animScale = 1.0f;
        this.bounceScale = 1.0f;
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
                isPressed = false;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (isEnabled() && SwingUtilities.isLeftMouseButton(e)) {
                    isPressed = true;
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (isPressed && isEnabled()) {
                    isPressed = false;
                    triggerBounce();
                } else {
                    isPressed = false;
                    repaint();
                }
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
        boolean changed = (this.isSelectedState != selected);
        this.isSelectedState = selected;
        if (changed && selected) {
            triggerBounce();
        } else {
            repaint();
        }
    }

    public boolean isSelectedState() {
        return isSelectedState;
    }

    public void setPrefixBadge(String badge) {
        this.prefixBadge = badge;
        repaint();
    }

    private boolean isAnswerCardStyle() {
        return style == Style.ANSWER_A || style == Style.ANSWER_B
                || style == Style.ANSWER_C || style == Style.ANSWER_D;
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
        float effectiveScale = animScale * bounceScale;
        if (Math.abs(effectiveScale - 1.0f) > 0.001f) {
            try {
                g2.setClip(null);
            } catch (Exception ignored) {}
            int cx = getWidth() / 2;
            int cy = getHeight() / 2;
            g2.translate(cx, cy);
            g2.scale(effectiveScale, effectiveScale);
            g2.translate(-cx, -cy);
        }

        int w = getWidth();
        int h = getHeight();

        Color bg = resolveBgColor();
        Color fg = resolveFgColor();
        Color border = resolveBorderColor();

        boolean isAns = isAnswerCardStyle();
        boolean isTfStyle = (style == Style.TRUE_BLUE || style == Style.FALSE_YELLOW);
        boolean isSolid = (style != Style.SECONDARY && style != Style.OUTLINE_GRAY && style != Style.SECONDARY_SKY);
        int depth = (isAns || isTfStyle) ? 5 : (isSolid ? 3 : 0);

        int hoverLift = (isHovered && !isPressed && isEnabled() && (isAns || isTfStyle)) ? -1 : 0;
        int pressOffset = (isPressed && isEnabled()) ? Math.max(2, depth - 1) : hoverLift;

        // 1. Vẽ nền bo góc với cạnh đáy 3D kiểu Kahoot!
        if (isSolid && depth > 0 && isEnabled()) {
            Color shadowColor = resolveShadowColor(bg);
            g2.setColor(shadowColor);
            g2.fill(new RoundRectangle2D.Float(1, 2 + depth, w - 2, h - 3 - depth, cornerRadius, cornerRadius));

            int faceY = Math.max(1, 2 + pressOffset);
            int faceH = h - 3 - (isPressed ? 1 : depth);
            g2.setColor(bg);
            g2.fill(new RoundRectangle2D.Float(1, faceY, w - 2, faceH, cornerRadius, cornerRadius));

            // Lớp phủ ánh sáng mịn phía trên khi hover hoặc cho thẻ đáp án
            if (isHovered || isAns || isTfStyle) {
                g2.setColor(new Color(255, 255, 255, isHovered ? 36 : 18));
                g2.fill(new RoundRectangle2D.Float(2, faceY + 1, w - 4, Math.max(8, faceH / 2), cornerRadius, cornerRadius));
            }
        } else {
            int faceY = Math.max(1, 1 + pressOffset);
            g2.setColor(bg);
            g2.fill(new RoundRectangle2D.Float(1, faceY, w - 2, h - 2 - Math.max(0, pressOffset), cornerRadius, cornerRadius));
        }

        int faceY = Math.max(1, (isSolid && depth > 0 && isEnabled()) ? (2 + pressOffset) : (1 + pressOffset));
        int faceH = (isSolid && depth > 0 && isEnabled()) ? (h - 3 - (isPressed ? 1 : depth)) : (h - 2 - Math.max(0, pressOffset));

        // 2. Vẽ viền nổi bật khi reveal hoặc khi đáp án đang được chọn (isSelectedState)
        if (revealBgColor != null) {
            Color revealBorder = (revealBgColor.equals(new Color(0x16, 0xA3, 0x4A)) || revealBgColor.equals(Theme.GREEN))
                    ? new Color(0x86, 0xEF, 0xAC)
                    : new Color(0xFE, 0xCA, 0xCA);
            g2.setColor(revealBorder);
            g2.setStroke(new BasicStroke(3f));
            g2.draw(new RoundRectangle2D.Float(2f, faceY + 0.5f, w - 4, faceH - 1, cornerRadius, cornerRadius));
        } else if (isSelectedState) {
            // Viền phát sáng trắng-vàng nổi bật khi đã chọn đáp án
            g2.setColor(new Color(255, 255, 255, 240));
            g2.setStroke(new BasicStroke(3.2f));
            g2.draw(new RoundRectangle2D.Float(2f, faceY + 0.5f, w - 4, faceH - 1, cornerRadius, cornerRadius));
        } else if (border != null) {
            g2.setColor(border);
            g2.setStroke(new BasicStroke(1.5f));
            g2.draw(new RoundRectangle2D.Float(1, faceY, w - 2, faceH, cornerRadius, cornerRadius));
        }

        // Dịch chuyển nội dung chữ/icon theo độ lún khi bấm
        if (pressOffset != 0) {
            g2.translate(0, pressOffset);
        }

        int contentH = h - depth;
        String text = getText();

        // 3. Phong cách ĐÚNG / SAI đặc thù (True = Hình thoi + Dấu tích, False = Hình tam giác + Dấu nhân)
        if (isTfStyle && prefixBadge != null && !prefixBadge.isEmpty()) {
            int badgeSize = Math.max(38, Math.min(54, contentH - 20));
            int badgeSpacing = 16;
            Font textFont = getFont();
            FontMetrics fmText = g2.getFontMetrics(textFont);
            int textW = (text != null) ? fmText.stringWidth(text) : 0;
            int totalW = badgeSize + badgeSpacing + textW;
            int startX = Math.max(16, (w - totalW) / 2);

            int badgeY = (contentH - badgeSize) / 2;
            g2.setColor(new Color(0, 0, 0, 48));
            g2.fillRoundRect(startX, badgeY, badgeSize, badgeSize, 12, 12);
            g2.setColor(new Color(255, 255, 255, 55));
            g2.setStroke(new BasicStroke(1.2f));
            g2.drawRoundRect(startX, badgeY, badgeSize, badgeSize, 12, 12);

            drawBadgeSymbol(g2, prefixBadge, startX, badgeY, badgeSize, fg);

            if (text != null && !text.isEmpty()) {
                g2.setFont(textFont);
                g2.setColor(fg);
                int tx = startX + badgeSize + badgeSpacing;
                int ty = (contentH - fmText.getHeight()) / 2 + fmText.getAscent();
                g2.drawString(text, tx, ty);
            }
            if (isSelectedState) {
                drawSelectedIndicator(g2, w, contentH);
            }
            g2.dispose();
            return;
        }

        // 4. Vẽ huy hiệu Hình Học Kahoot! + Nhãn A, B, C, D cho các thẻ đáp án
        int textOffsetLeft = 0;
        int rightReserved = isSelectedState ? 36 : 14;
        if (prefixBadge != null && !prefixBadge.isEmpty()) {
            int badgeSize = isAns ? Math.max(36, Math.min(52, contentH - 16)) : Math.max(24, Math.min(38, contentH - 14));
            int badgeX = 12;
            int badgeY = (contentH - badgeSize) / 2;

            // Nền khối hình học bên trái
            g2.setColor(new Color(0, 0, 0, 52));
            g2.fillRoundRect(badgeX, badgeY, badgeSize, badgeSize, 10, 10);
            if (isAns) {
                g2.setColor(new Color(255, 255, 255, 50));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(badgeX, badgeY, badgeSize, badgeSize, 10, 10);
                drawKahootGeometricBadge(g2, style, prefixBadge, badgeX, badgeY, badgeSize);
            } else {
                g2.setFont(Theme.BODY_BOLD);
                drawBadgeSymbol(g2, prefixBadge, badgeX, badgeY, badgeSize, fg);
            }

            textOffsetLeft = badgeSize + 14;
        }

        // 5. Vẽ text nhãn đáp án (Tự động xuống dòng & co giãn cỡ chữ)
        if (text != null && !text.isEmpty()) {
            int startX = (prefixBadge != null) ? (12 + textOffsetLeft) : 14;
            int availableWidth = Math.max(40, (prefixBadge != null) ? (w - startX - rightReserved) : (w - 28));
            drawWrappedOrCenteredText(g2, text, startX, availableWidth, w, contentH, fg, prefixBadge != null);
        }

        // 6. Vẽ dấu tích tròn bên phải khi đáp án đang được chọn
        if (isSelectedState && (isAns || isTfStyle)) {
            drawSelectedIndicator(g2, w, contentH);
        }

        g2.dispose();
    }

    /**
     * Vẽ biểu tượng hình học chuẩn Kahoot! (Tam giác, Hình thoi, Hình tròn, Hình vuông)
     * kết hợp ký tự A, B, C, D sắc nét bên trong.
     */
    private void drawKahootGeometricBadge(Graphics2D g2, Style s, String badge, int bx, int by, int bSize) {
        int cx = bx + bSize / 2;
        int cy = by + bSize / 2;
        int r = (int) (bSize * 0.34f);

        g2.setColor(Color.WHITE);
        switch (s) {
            case ANSWER_A -> {
                // Hình Tam Giác (Triangle)
                Polygon tri = new Polygon();
                tri.addPoint(cx, cy - r - 2);
                tri.addPoint(cx - r - 2, cy + r);
                tri.addPoint(cx + r + 2, cy + r);
                g2.fillPolygon(tri);
            }
            case ANSWER_B -> {
                // Hình Thoi (Diamond)
                Polygon dia = new Polygon();
                dia.addPoint(cx, cy - r - 2);
                dia.addPoint(cx + r + 2, cy);
                dia.addPoint(cx, cy + r + 2);
                dia.addPoint(cx - r - 2, cy);
                g2.fillPolygon(dia);
            }
            case ANSWER_C -> {
                // Hình Tròn (Circle)
                int d = (r + 1) * 2;
                g2.fillOval(cx - r - 1, cy - r - 1, d, d);
            }
            case ANSWER_D -> {
                // Hình Vuông bo góc nhẹ (Square)
                int sq = (int) (r * 1.75f);
                g2.fillRoundRect(cx - sq / 2, cy - sq / 2, sq, sq, 5, 5);
            }
            default -> {}
        }

        // Vẽ chữ A, B, C, D nổi bật ở tâm khối hình học
        if (badge != null && !badge.isEmpty()) {
            Color letterColor = switch (s) {
                case ANSWER_A -> new Color(0xBE, 0x12, 0x3C);
                case ANSWER_B -> new Color(0x1D, 0x4E, 0xD8);
                case ANSWER_C -> new Color(0xB4, 0x53, 0x09);
                case ANSWER_D -> new Color(0x15, 0x80, 0x3D);
                default -> Theme.DARK_TEXT;
            };
            g2.setColor(letterColor);
            int fontSize = Math.max(11, (int) (bSize * 0.28f));
            g2.setFont(new Font("Segoe UI", Font.BOLD, fontSize));
            FontMetrics fm = g2.getFontMetrics();
            int tx = cx - fm.stringWidth(badge) / 2;
            int ty = cy - fm.getHeight() / 2 + fm.getAscent() + (s == Style.ANSWER_A ? 3 : 0);
            g2.drawString(badge, tx, ty);
        }
    }

    private void drawSelectedIndicator(Graphics2D g2, int w, int contentH) {
        int r = 22;
        int ix = w - r - 10;
        int iy = (contentH - r) / 2;
        g2.setColor(Color.WHITE);
        g2.fillOval(ix, iy, r, r);
        g2.setColor(new Color(0x16, 0xA3, 0x4A));
        g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int cx = ix + r / 2;
        int cy = iy + r / 2;
        g2.drawLine(cx - 5, cy, cx - 1, cy + 4);
        g2.drawLine(cx - 1, cy + 4, cx + 5, cy - 4);
    }

    private Color resolveShadowColor(Color faceColor) {
        int r = Math.max(0, (int) (faceColor.getRed() * 0.68f));
        int gr = Math.max(0, (int) (faceColor.getGreen() * 0.68f));
        int b = Math.max(0, (int) (faceColor.getBlue() * 0.68f));
        return new Color(r, gr, b);
    }

    private void drawBadgeSymbol(Graphics2D g2, String badge, int bx, int by, int bSize, Color fg) {
        g2.setColor(fg);
        if ("✓".equals(badge) || "\u2713".equals(badge)) {
            // Draw crisp vector checkmark
            g2.setStroke(new BasicStroke(3.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int cx = bx + bSize / 2;
            int cy = by + bSize / 2;
            int s = (int) (bSize * 0.55f);
            g2.drawLine(cx - s / 2, cy + 1, cx - s / 6, cy + s / 3 + 1);
            g2.drawLine(cx - s / 6, cy + s / 3 + 1, cx + s / 2, cy - s / 3 + 1);
        } else if ("✗".equals(badge) || "\u2717".equals(badge) || "✕".equals(badge) || "\u2715".equals(badge)) {
            // Draw crisp vector cross
            g2.setStroke(new BasicStroke(3.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int cx = bx + bSize / 2;
            int cy = by + bSize / 2;
            int s = (int) (bSize * 0.45f);
            g2.drawLine(cx - s / 2, cy - s / 2, cx + s / 2, cy + s / 2);
            g2.drawLine(cx + s / 2, cy - s / 2, cx - s / 2, cy + s / 2);
        } else {
            FontMetrics fmBadge = g2.getFontMetrics();
            int tx = bx + (bSize - fmBadge.stringWidth(badge)) / 2;
            int ty = by + ((bSize - fmBadge.getHeight()) / 2) + fmBadge.getAscent();
            g2.drawString(badge, tx, ty);
        }
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
                if (isPressed) return new Color(0xE2, 0xE8, 0xF0);
                if (isHovered) return Theme.SURFACE_HOVER;
                return Theme.WHITE;

            case ACCENT_PURPLE:
                if (isPressed) return Theme.PURPLE_HOVER.darker();
                if (isHovered) return Theme.PURPLE_HOVER;
                return Theme.PURPLE;

            case SUCCESS:
            case ANSWER_D:
                if (isPressed) return Theme.GREEN.darker();
                if (isHovered) return Theme.EMERALD_DARK;
                return Theme.GREEN;

            case DANGER:
            case ANSWER_A:
                if (isPressed) return Theme.RED.darker();
                if (isHovered) return new Color(0xBE, 0x12, 0x3C);
                return Theme.RED;

            case ANSWER_B:
                if (isPressed) return Theme.BLUE_HOVER.darker();
                if (isHovered) return Theme.BLUE_HOVER;
                return Theme.BLUE;

            case ANSWER_C:
                if (isPressed) return new Color(0xB4, 0x53, 0x09);
                if (isHovered) return Theme.AMBER;
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
                if (isPressed) return Theme.BLUE.darker();
                if (isHovered) return Theme.BLUE_HOVER;
                return Theme.BLUE;

            case FALSE_YELLOW:
                if (isPressed) return new Color(0xB4, 0x53, 0x09);
                if (isHovered) return Theme.AMBER;
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
