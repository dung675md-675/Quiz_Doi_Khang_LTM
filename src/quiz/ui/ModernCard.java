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
