package quiz.ui;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * ModernCard - Panel hình khối thẻ với bo góc mềm mại, viền phẳng tinh tế và hỗ trợ cả sáng/tối.
 */
public class ModernCard extends JPanel {

    private Color cardBackground = Theme.WHITE;
    private Color borderColor = Theme.BORDER_LIGHT;
    private int cornerRadius = 14;
    private boolean drawBorder = true;

    private boolean hasShadow = true;
    private Color shadowColor = new Color(0, 0, 0, 22);

    public ModernCard() {
        this(new BorderLayout());
    }

    public ModernCard(LayoutManager layout) {
        super(layout);
        setOpaque(false);
        setBorder(new EmptyBorder(16, 16, 16, 16));
    }

    public ModernCard(Color bg, Color border, int radius) {
        this();
        this.cardBackground = bg;
        this.borderColor = border;
        this.cornerRadius = radius;
    }

    public void setHasShadow(boolean shadow) {
        this.hasShadow = shadow;
        repaint();
    }

    public void setCardBackground(Color bg) {
        this.cardBackground = bg;
        repaint();
    }

    public void setBorderColor(Color border) {
        this.borderColor = border;
        repaint();
    }

    public void setCornerRadius(int radius) {
        this.cornerRadius = radius;
        repaint();
    }

    public void setDrawBorder(boolean drawBorder) {
        this.drawBorder = drawBorder;
        repaint();
    }

    public void setCardPadding(int top, int left, int bottom, int right) {
        setBorder(new EmptyBorder(top, left, bottom, right));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.setupAntiAliasing(g2);

        int w = getWidth();
        int h = getHeight();

        // 0. Vẽ bóng đổ nhẹ mềm mại (soft diffused ambient shadow)
        if (hasShadow) {
            for (int s = 1; s <= 4; s++) {
                int alpha = Math.max(2, 12 - s * 2);
                g2.setColor(new Color(15, 23, 42, alpha));
                g2.fill(new RoundRectangle2D.Float(1, 1 + s, w - 2, h - 2, cornerRadius + 2, cornerRadius + 2));
            }
        }

        // 1. Vẽ nền bo góc
        g2.setColor(cardBackground);
        g2.fill(new RoundRectangle2D.Float(0, 0, w - 1, h - 1, cornerRadius, cornerRadius));

        // 2. Vẽ viền tinh tế
        if (drawBorder && borderColor != null) {
            g2.setColor(borderColor);
            g2.setStroke(new BasicStroke(1.2f));
            g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 2, h - 2, cornerRadius, cornerRadius));
        }

        g2.dispose();
        super.paintComponent(g);
    }
}
