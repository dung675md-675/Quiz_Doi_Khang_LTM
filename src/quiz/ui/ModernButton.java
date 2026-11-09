package quiz.ui;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import javax.swing.*;

/**
 * ModernButton - Nút bấm giao diện game quiz hiện đại, bo góc mềm mại, hiệu ứng 3D Kahoot/Quizizz,
 * hỗ trợ biểu tượng hình học Java2D (A/B/C/D & Đúng/Sai), trạng thái chọn nổi bật, chế độ chọn nhiều
 * và co giãn văn bản thông minh không bị tràn viền.
 */
public class ModernButton extends JButton {

    public enum Style {
        PRIMARY,        // Màu xanh chính #1368CE
        SECONDARY,      // Viền xanh, nền trắng
        ACCENT_PURPLE,  // Màu tím accent #864CBF
        SUCCESS,        // Màu xanh lá #26890C
        DANGER,         // Màu đỏ #E21B3C
        OUTLINE_GRAY,   // Viền xám, nền trắng
        ANSWER_A,       // Đỏ Kahoot #E21B3C
        ANSWER_B,       // Xanh dương Kahoot #1368CE
        ANSWER_C,       // Vàng hổ phách #D89E00
        ANSWER_D,       // Xanh lá Kahoot #26890C
        EMERALD,        // Xanh ngọc lục bảo tươi #10B981
        ORANGE,         // Cam tươi #F97316
        SKY_BLUE,       // Xanh da trời tươi #0EA5E9
        SECONDARY_SKY,  // Viền xanh da trời, nền sáng
        TRUE_BLUE,      // Đúng: Xanh dương #1368CE
        FALSE_YELLOW    // Sai: Vàng tươi #D97706 / #EAB308
    }

    private Style style = Style.PRIMARY;
    private boolean arenaStyle;
    private int cornerRadius = 14;
    private boolean isHovered = false;
    private boolean isPressed = false;
    private boolean isSelectedState = false;
    private boolean isDimmed = false;
    private boolean isMultiSelectMode = false;

    private String prefixBadge = null; // "A", "B", "C", "D" hoặc "✓", "✗"
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

    public void setDimmed(boolean dimmed) {
        if (this.isDimmed != dimmed) {
            this.isDimmed = dimmed;
            repaint();
        }
    }

    public boolean isDimmed() { return isDimmed; }

    public void setMultiSelectMode(boolean multi) {
        if (this.isMultiSelectMode != multi) {
            this.isMultiSelectMode = multi;
            repaint();
        }
    }

    public boolean isMultiSelectMode() { return isMultiSelectMode; }

    public void triggerBounce() {
        if (arenaStyle && (Boolean.getBoolean("quiz.ui.reduceMotion") || !isShowing())) {
            bounceScale = 1.0f;
            repaint();
            return;
        }
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
        this.isDimmed = false;
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
        if (arenaStyle) setForeground(resolveFgColor());
        repaint();
    }

    public Style getStyle() {
        return style;
    }

    /** Opt-in dashboard appearance; answer rendering keeps the current team implementation. */
    public void setArenaStyle(boolean enabled) {
        arenaStyle = enabled;
        setForeground(resolveFgColor());
        setIconTextGap(8);
        repaint();
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

        // Xử lý độ trong suốt động & giảm độ nổi bật (dimmed) khi chọn đáp án khác
        float effectiveAlpha = animAlpha;
        if (isDimmed && !isSelectedState && revealBgColor == null) {
            effectiveAlpha *= 0.48f; // Giảm sáng nhẹ nhàng, vẫn đọc được rõ ràng
        }
        if (effectiveAlpha < 0.999f) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, effectiveAlpha));
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
        int depth = (isAns || isTfStyle) ? 5 : (isSolid && !arenaStyle ? 3 : 0);

        // Hiệu ứng hover nhô lên (-2px) và hiệu ứng bấm lún (pressOffset)
        int hoverLift = (isHovered && !isPressed && isEnabled() && (isAns || isTfStyle)) ? -2 : 0;
        int pressOffset = (isPressed && isEnabled()) ? Math.max(2, depth - 1) : hoverLift;

        // 1. VẼ NỀN THẺ BO GÓC 3D VỚI CẠNH ĐÁY VÁT PHONG CÁCH KAHOOT
        if (isSolid && depth > 0 && isEnabled()) {
            Color shadowColor = resolveShadowColor(bg);

            // Bóng đổ 3D bên dưới cạnh thẻ
            g2.setColor(new Color(0, 0, 0, isHovered ? 45 : 30));
            g2.fill(new RoundRectangle2D.Float(1, 3 + depth, w - 2, h - 3 - depth, cornerRadius + 2, cornerRadius + 2));

            // Chân đế 3D
            g2.setColor(shadowColor);
            g2.fill(new RoundRectangle2D.Float(1, 2 + depth, w - 2, h - 3 - depth, cornerRadius, cornerRadius));

            // Mặt thẻ chính với gradient ánh sáng tự nhiên
            int faceY = Math.max(1, 2 + pressOffset);
            int faceH = h - 3 - (isPressed ? 1 : depth);

            Color topHighlight = resolveTopHighlightColor(bg);
            GradientPaint faceGrad = new GradientPaint(0, faceY, topHighlight, 0, faceY + faceH, bg);
            g2.setPaint(faceGrad);
            g2.fill(new RoundRectangle2D.Float(1, faceY, w - 2, faceH, cornerRadius, cornerRadius));

            // Lớp phủ ánh sáng mịn (sheen) ở nửa trên thẻ
            if (isHovered || isAns || isTfStyle) {
                int sheenAlpha = isHovered ? 38 : 18;
                g2.setColor(new Color(255, 255, 255, sheenAlpha));
                g2.fill(new RoundRectangle2D.Float(2, faceY + 1, w - 4, Math.max(8, faceH / 2), cornerRadius, cornerRadius));
            }
        } else {
            int faceY = Math.max(1, 1 + pressOffset);
            g2.setColor(bg);
            g2.fill(new RoundRectangle2D.Float(1, faceY, w - 2, h - 2 - Math.max(0, pressOffset), cornerRadius, cornerRadius));
        }

        int faceY = Math.max(1, (isSolid && depth > 0 && isEnabled()) ? (2 + pressOffset) : (1 + pressOffset));
        int faceH = (isSolid && depth > 0 && isEnabled()) ? (h - 3 - (isPressed ? 1 : depth)) : (h - 2 - Math.max(0, pressOffset));

        // 2. VẼ VIỀN NỔI BẬT KHI REVEAL HOẶC KHI ĐƯỢC CHỌN (isSelectedState)
        if (revealBgColor != null) {
            boolean isCorrectReveal = (revealBgColor.equals(new Color(0x16, 0xA3, 0x4A)) || revealBgColor.equals(Theme.GREEN));
            Color revealBorder = isCorrectReveal ? new Color(0x86, 0xEF, 0xAC) : new Color(0xFE, 0xCA, 0xCA);

            // Hào quang tỏa sáng quanh viền
            g2.setColor(new Color(revealBorder.getRed(), revealBorder.getGreen(), revealBorder.getBlue(), 70));
            g2.setStroke(new BasicStroke(4.8f));
            g2.draw(new RoundRectangle2D.Float(2f, faceY + 0.5f, w - 4, faceH - 1, cornerRadius, cornerRadius));

            g2.setColor(revealBorder);
            g2.setStroke(new BasicStroke(3.0f));
            g2.draw(new RoundRectangle2D.Float(2f, faceY + 0.5f, w - 4, faceH - 1, cornerRadius, cornerRadius));
        } else if (isSelectedState) {
            // Viền phát sáng trắng tinh khiết nổi bật khi được chọn
            g2.setColor(new Color(255, 255, 255, 100));
            g2.setStroke(new BasicStroke(5.0f));
            g2.draw(new RoundRectangle2D.Float(2f, faceY + 0.5f, w - 4, faceH - 1, cornerRadius, cornerRadius));

            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(3.0f));
            g2.draw(new RoundRectangle2D.Float(2f, faceY + 0.5f, w - 4, faceH - 1, cornerRadius, cornerRadius));
        } else if (isHovered && isEnabled() && (isAns || isTfStyle)) {
            // Viền sáng nhẹ khi hover
            g2.setColor(new Color(255, 255, 255, 75));
            g2.setStroke(new BasicStroke(1.8f));
            g2.draw(new RoundRectangle2D.Float(1.5f, faceY + 0.5f, w - 3, faceH - 1, cornerRadius, cornerRadius));
        } else if (border != null) {
            g2.setColor(border);
            g2.setStroke(new BasicStroke(1.5f));
            g2.draw(new RoundRectangle2D.Float(1, faceY, w - 2, faceH, cornerRadius, cornerRadius));
        }

        if (arenaStyle && hasFocus() && isEnabled()) {
            g2.setColor(isSolid ? fg : ArenaPalette.CYAN);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(4, faceY + 3, w - 9, Math.max(1, faceH - 7),
                    Math.max(4, cornerRadius - 4), Math.max(4, cornerRadius - 4));
        }

        // Dịch chuyển nội dung chữ/icon theo độ lún khi bấm
        if (pressOffset != 0) {
            g2.translate(0, pressOffset);
        }

        int contentH = h - depth;
        String text = getText();

        // Toolbar icons are separate from the updated geometric answer badges below.
        if (!isAns && !isTfStyle && (prefixBadge == null || prefixBadge.isEmpty()) && getIcon() != null) {
            Icon icon = isEnabled() ? getIcon() : getDisabledIcon();
            if (icon == null) icon = getIcon();
            FontMetrics metrics = g2.getFontMetrics(getFont());
            int textWidth = text == null || text.isEmpty() ? 0 : metrics.stringWidth(text);
            int gap = textWidth == 0 ? 0 : getIconTextGap();
            int iconX = Math.max(6, (w - icon.getIconWidth() - gap - textWidth) / 2);
            g2.clipRect(6, 0, Math.max(0, w - 12), h);
            g2.setColor(fg);
            icon.paintIcon(this, g2, iconX, (contentH - icon.getIconHeight()) / 2);
            if (textWidth > 0) {
                int textX = iconX + icon.getIconWidth() + gap;
                drawWrappedOrCenteredText(g2, text, textX, Math.max(1, w - textX - 8),
                        w, contentH, fg, true);
            }
            g2.dispose();
            return;
        }

        // 3. VẼ PHONG CÁCH ĐÚNG / SAI (True = Hình thoi + Dấu tích, False = Hình tam giác + Dấu nhân)
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
                drawSelectedIndicator(g2, w, contentH, false);
            }
            g2.dispose();
            return;
        }

        // 4. VẼ BIỂU TƯỢNG HÌNH HỌC KAHOOT! + NHÃN A, B, C, D CHO CÁC THẺ ĐÁP ÁN
        int textOffsetLeft = 0;
        int rightReserved = isMultiSelectMode ? 40 : (isSelectedState ? 38 : 16);
        if (prefixBadge != null && !prefixBadge.isEmpty()) {
            int badgeSize = isAns ? Math.max(36, Math.min(52, contentH - 16)) : Math.max(24, Math.min(38, contentH - 14));
            int badgeX = 14;
            int badgeY = (contentH - badgeSize) / 2;

            // Nền khối hình học bên trái
            g2.setColor(new Color(0, 0, 0, 48));
            g2.fillRoundRect(badgeX, badgeY, badgeSize, badgeSize, 10, 10);
            if (isAns) {
                g2.setColor(new Color(255, 255, 255, 55));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(badgeX, badgeY, badgeSize, badgeSize, 10, 10);
                drawKahootGeometricBadge(g2, style, prefixBadge, badgeX, badgeY, badgeSize);
            } else {
                g2.setFont(Theme.BODY_BOLD);
                drawBadgeSymbol(g2, prefixBadge, badgeX, badgeY, badgeSize, fg);
            }

            textOffsetLeft = badgeSize + 16;
        }

        // 5. VẼ TEXT NHÃN ĐÁP ÁN (Tự động xuống dòng & co giãn cỡ chữ thông minh)
        if (text != null && !text.isEmpty()) {
            int startX = (prefixBadge != null) ? (14 + textOffsetLeft) : 14;
            int availableWidth = Math.max(40, w - startX - rightReserved);
            drawWrappedOrCenteredText(g2, text, startX, availableWidth, w, contentH, fg, prefixBadge != null);
        }

        // 6. VẼ DẤU TÍCH / CHECKBOX TRẠNG THÁI Ở GÓC PHẢI
        if (isAns || isTfStyle) {
            if (isMultiSelectMode) {
                drawMultiSelectCheckbox(g2, w, contentH, isSelectedState);
            } else if (isSelectedState) {
                drawSelectedIndicator(g2, w, contentH, false);
            }
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
        int r = (int) (bSize * 0.35f);

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
            int fontSize = Math.max(12, (int) (bSize * 0.29f));
            g2.setFont(new Font("Segoe UI", Font.BOLD, fontSize));
            FontMetrics fm = g2.getFontMetrics();
            int tx = cx - fm.stringWidth(badge) / 2;
            int ty = cy - fm.getHeight() / 2 + fm.getAscent() + (s == Style.ANSWER_A ? 3 : 0);
            g2.drawString(badge, tx, ty);
        }
    }

    /**
     * Vẽ huy hiệu dấu tích tròn nổi bật khi đáp án đơn hoặc Đúng/Sai được chọn.
     */
    private void drawSelectedIndicator(Graphics2D g2, int w, int contentH, boolean isMulti) {
        int r = 24;
        int ix = w - r - 12;
        int iy = (contentH - r) / 2;

        // Vòng tròn trắng nổi
        g2.setColor(Color.WHITE);
        g2.fillOval(ix, iy, r, r);

        // Viền bóng
        g2.setColor(new Color(0, 0, 0, 30));
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawOval(ix, iy, r, r);

        // Dấu tích màu xanh lá tươi
        g2.setColor(new Color(0x16, 0xA3, 0x4A));
        g2.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int cx = ix + r / 2;
        int cy = iy + r / 2;
        g2.drawLine(cx - 5, cy, cx - 1, cy + 4);
        g2.drawLine(cx - 1, cy + 4, cx + 5, cy - 4);
    }

    /**
     * Vẽ hộp checkbox vuông bo góc rõ ràng cho dạng Chọn nhiều đáp án (MULTIPLE_SELECT).
     */
    private void drawMultiSelectCheckbox(Graphics2D g2, int w, int contentH, boolean isChecked) {
        int boxSize = 22;
        int ix = w - boxSize - 12;
        int iy = (contentH - boxSize) / 2;

        if (isChecked) {
            // Nền hộp đã chọn (xanh ngọc lục bảo rực rỡ)
            g2.setColor(new Color(0x10, 0xB9, 0x81));
            g2.fillRoundRect(ix, iy, boxSize, boxSize, 6, 6);

            // Viền trắng
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(ix, iy, boxSize, boxSize, 6, 6);

            // Dấu tích trắng
            g2.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int cx = ix + boxSize / 2;
            int cy = iy + boxSize / 2;
            g2.drawLine(cx - 5, cy, cx - 1, cy + 4);
            g2.drawLine(cx - 1, cy + 4, cx + 5, cy - 4);
        } else {
            // Hộp chưa chọn
            g2.setColor(new Color(0, 0, 0, 60));
            g2.fillRoundRect(ix, iy, boxSize, boxSize, 6, 6);

            g2.setColor(new Color(255, 255, 255, 130));
            g2.setStroke(new BasicStroke(1.6f));
            g2.drawRoundRect(ix, iy, boxSize, boxSize, 6, 6);
        }
    }

    private Color resolveShadowColor(Color faceColor) {
        int r = Math.max(0, (int) (faceColor.getRed() * 0.65f));
        int gr = Math.max(0, (int) (faceColor.getGreen() * 0.65f));
        int b = Math.max(0, (int) (faceColor.getBlue() * 0.65f));
        return new Color(r, gr, b);
    }

    private Color resolveTopHighlightColor(Color faceColor) {
        int r = Math.min(255, (int) (faceColor.getRed() * 1.15f) + 12);
        int gr = Math.min(255, (int) (faceColor.getGreen() * 1.15f) + 12);
        int b = Math.min(255, (int) (faceColor.getBlue() * 1.15f) + 12);
        return new Color(r, gr, b);
    }

    private void drawBadgeSymbol(Graphics2D g2, String badge, int bx, int by, int bSize, Color fg) {
        g2.setColor(fg);
        if ("✓".equals(badge) || "\u2713".equals(badge)) {
            g2.setStroke(new BasicStroke(3.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int cx = bx + bSize / 2;
            int cy = by + bSize / 2;
            int s = (int) (bSize * 0.55f);
            g2.drawLine(cx - s / 2, cy + 1, cx - s / 6, cy + s / 3 + 1);
            g2.drawLine(cx - s / 6, cy + s / 3 + 1, cx + s / 2, cy - s / 3 + 1);
        } else if ("✗".equals(badge) || "\u2717".equals(badge) || "✕".equals(badge) || "\u2715".equals(badge)) {
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

    /**
     * Tự động điều chỉnh kích thước phông chữ và ngắt dòng thông minh để đáp án dài không bị cắt tràn.
     */
    private void drawWrappedOrCenteredText(Graphics2D g2, String text, int startX, int availableWidth, int buttonWidth, int buttonHeight, Color fg, boolean hasPrefix) {
        g2.setColor(fg);
        Font baseFont = getFont();
        g2.setFont(baseFont);
        FontMetrics fm = g2.getFontMetrics();

        // 1. Nếu văn bản ngắn vừa vặn 1 dòng
        if (fm.stringWidth(text) <= availableWidth) {
            int tx = hasPrefix ? startX : ((buttonWidth - fm.stringWidth(text)) / 2);
            int ty = (buttonHeight - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(text, tx, ty);
            return;
        }

        // 2. Thử phân dòng với font gốc
        java.util.List<String> lines = breakIntoLines(text, fm, availableWidth);

        // 3. Nếu vượt quá 2 dòng hoặc chiều cao quá chật, hạ cỡ chữ thông minh (multi-pass)
        float currentSize = baseFont.getSize2D();
        while ((lines.size() > 2 || (lines.size() * fm.getHeight() > buttonHeight - 16)) && currentSize > 10.5f) {
            currentSize -= 1.2f;
            Font smallerFont = baseFont.deriveFont(currentSize);
            g2.setFont(smallerFont);
            fm = g2.getFontMetrics();
            lines = breakIntoLines(text, fm, availableWidth);
        }

        // 4. Nếu vẫn còn quá nhiều dòng (trên 3 dòng), giữ tối đa 3 dòng và thêm dấu ba chấm
        if (lines.size() > 3) {
            java.util.List<String> clamped = new java.util.ArrayList<>();
            clamped.add(lines.get(0));
            clamped.add(lines.get(1));
            String lastLine = lines.get(2);
            while (lastLine.length() > 3 && fm.stringWidth(lastLine + "…") > availableWidth) {
                lastLine = lastLine.substring(0, lastLine.length() - 1);
            }
            clamped.add(lastLine + "…");
            lines = clamped;
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
        if (arenaStyle && !isAnswerCardStyle() && style != Style.TRUE_BLUE && style != Style.FALSE_YELLOW) {
            if (!isEnabled()) return new Color(30, 43, 65);
            Color base = switch (style) {
                case PRIMARY -> ArenaPalette.ACCENT;
                case SECONDARY, OUTLINE_GRAY, SECONDARY_SKY -> ArenaPalette.SURFACE_RAISED;
                case SUCCESS, EMERALD -> new Color(47, 200, 154);
                case DANGER -> new Color(184, 43, 73);
                case ACCENT_PURPLE -> new Color(95, 70, 196);
                case SKY_BLUE -> ArenaPalette.CYAN;
                case ORANGE -> new Color(241, 172, 78);
                default -> ArenaPalette.SURFACE_RAISED;
            };
            if (isPressed || getModel().isPressed() && getModel().isArmed()) return base.darker();
            if (isHovered) return new Color(Math.min(255, base.getRed() + 10),
                    Math.min(255, base.getGreen() + 10), Math.min(255, base.getBlue() + 10));
            return base;
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
        if (arenaStyle && !isAnswerCardStyle() && style != Style.TRUE_BLUE && style != Style.FALSE_YELLOW) {
            if (!isEnabled()) return ArenaPalette.MUTED;
            return switch (style) {
                case PRIMARY -> ArenaPalette.ACCENT_INK;
                case SUCCESS, EMERALD, SKY_BLUE, ORANGE -> new Color(6, 31, 31);
                default -> ArenaPalette.TEXT;
            };
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
        if (arenaStyle && (style == Style.SECONDARY || style == Style.OUTLINE_GRAY
                || style == Style.SECONDARY_SKY)) return isHovered ? ArenaPalette.CYAN : ArenaPalette.BORDER;

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

    @Override
    public void removeNotify() {
        if (arenaStyle && bounceTimer != null) {
            bounceTimer.stop();
            bounceScale = 1.0f;
        }
        super.removeNotify();
    }
}
