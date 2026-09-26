package quiz.ui;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import javax.swing.*;

/**
 * ModernBadge - Huy hiệu trạng thái dạng viên nang (pill), kết hợp chấm tròn tín hiệu
 * và màu sắc rõ ràng (Sẵn sàng, Đang đấu, Rank/Điểm, Chủ đề).
 */
public class ModernBadge extends JComponent {

    public enum Type {
        READY,      // Xanh lá: "Sẵn sàng" / "Đang rỗi"
        BUSY,       // Đỏ: "Đang đấu" / "Đang bận"
        RANK,       // Tím: "Điểm xếp hạng"
        INFO,       // Xanh dương: "Chủ đề / Thông tin"
        WARNING     // Vàng: "Cảnh báo"
    }

    private String text;
    private Type type = Type.READY;

    public ModernBadge(String text, Type type) {
        this.text = text;
        this.type = type;
        setFont(Theme.SMALL_BOLD);
        setOpaque(false);
    }

    public void setText(String text) {
        this.text = text;
        revalidate();
        repaint();
    }

    public void setType(Type type) {
        this.type = type;
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(getFont());
        int textWidth = fm.stringWidth(text);
        int dotSize = (type == Type.READY || type == Type.BUSY) ? 14 : 0;
        int width = textWidth + dotSize + 22;
        int height = Math.max(22, fm.getHeight() + 8);
        return new Dimension(width, height);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.setupAntiAliasing(g2);

        int w = getWidth();
        int h = getHeight();
        int r = h; // Viên nang pill

        Color bg = resolveBg();
        Color fg = resolveFg();
        Color dotColor = resolveDot();

        // 1. Vẽ nền viên nang
        g2.setColor(bg);
        g2.fill(new RoundRectangle2D.Float(0, 0, w, h, r, r));

        // 2. Vẽ chấm tròn chỉ báo (nếu là READY hoặc BUSY)
        int xOffset = 10;
        if (dotColor != null) {
            int dotD = 7;
            int dotY = (h - dotD) / 2;
            g2.setColor(dotColor);
            g2.fillOval(xOffset, dotY, dotD, dotD);
            xOffset += dotD + 6;
        }

        // 3. Vẽ chữ
        g2.setColor(fg);
        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        int y = (h - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(text, xOffset, y);

        g2.dispose();
    }

    private Color resolveBg() {
        switch (type) {
            case READY:
                return new Color(0xDC, 0xFD, 0xD7); // Xanh lá pastel nhạt
            case BUSY:
                return new Color(0xFF, 0xE4, 0xE8); // Đỏ pastel nhạt
            case RANK:
                return new Color(0xF3, 0xE8, 0xFF); // Tím pastel nhạt
            case WARNING:
                return new Color(0xFE, 0xF9, 0xC3); // Vàng pastel nhạt
            case INFO:
            default:
                return new Color(0xE0, 0xEE, 0xFF); // Xanh dương pastel nhạt
        }
    }

    private Color resolveFg() {
        switch (type) {
            case READY:
                return Theme.GREEN;
            case BUSY:
                return Theme.RED;
            case RANK:
                return Theme.PURPLE;
            case WARNING:
                return Theme.YELLOW.darker();
            case INFO:
            default:
                return Theme.BLUE;
        }
    }

    private Color resolveDot() {
        switch (type) {
            case READY:
                return Theme.GREEN;
            case BUSY:
                return Theme.RED;
            default:
                return null;
        }
    }
}
