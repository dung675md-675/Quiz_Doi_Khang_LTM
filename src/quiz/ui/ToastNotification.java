package quiz.ui;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import javax.swing.*;

/**
 * ToastNotification - Thành phần thông báo nổi tạm thời (Toast),
 * hiển thị trạng thái Thành công, Lỗi, Cảnh báo hoặc Thông tin mà không gây chặn luồng người dùng.
 */
public class ToastNotification extends JPanel {

    public enum Type {
        SUCCESS, ERROR, WARNING, INFO
    }

    private final String message;
    private final Type type;
    private final int cornerRadius = 10;

    public ToastNotification(String message, Type type) {
        this.message = message;
        this.type = type;
        setOpaque(false);
        setFont(Theme.BODY_BOLD);
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(getFont());
        int textWidth = fm.stringWidth(message);
        int width = Math.max(260, textWidth + 70);
        int height = 46;
        return new Dimension(width, height);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.setupAntiAliasing(g2);

        int w = getWidth();
        int h = getHeight();

        // 1. Vẽ bóng mờ nhẹ hoặc nền thẻ nổi màu đậm
        g2.setColor(new Color(0, 0, 0, 30));
        g2.fill(new RoundRectangle2D.Float(2, 2, w - 4, h - 3, cornerRadius, cornerRadius));

        // 2. Vẽ nền thẻ
        g2.setColor(Theme.WHITE);
        g2.fill(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, cornerRadius, cornerRadius));

        // 3. Vẽ dải màu bên trái hoặc icon theo trạng thái
        Color themeColor = resolveColor();
        g2.setColor(themeColor);
        g2.fill(new RoundRectangle2D.Float(1, 1, 6, h - 2, cornerRadius, cornerRadius));
        g2.fillRect(4, 1, 4, h - 2);

        // 4. Vẽ icon tròn nhỏ
        int iconSize = 24;
        int iconX = 16;
        int iconY = (h - iconSize) / 2;
        g2.setColor(themeColor);
        g2.fillOval(iconX, iconY, iconSize, iconSize);

        // Biểu tượng icon bên trong
        g2.setColor(Theme.WHITE);
        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        FontMetrics fmIcon = g2.getFontMetrics();
        String symbol = resolveSymbol();
        int sx = iconX + (iconSize - fmIcon.stringWidth(symbol)) / 2;
        int sy = iconY + ((iconSize - fmIcon.getHeight()) / 2) + fmIcon.getAscent();
        g2.drawString(symbol, sx, sy);

        // 5. Vẽ thông điệp text
        g2.setColor(Theme.DARK_TEXT);
        g2.setFont(Theme.BODY_BOLD);
        FontMetrics fmText = g2.getFontMetrics();
        int tx = iconX + iconSize + 12;
        int ty = (h - fmText.getHeight()) / 2 + fmText.getAscent();
        g2.drawString(message, tx, ty);

        // 6. Viền thẻ
        g2.setColor(Theme.BORDER_LIGHT);
        g2.setStroke(new BasicStroke(1.0f));
        g2.draw(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, cornerRadius, cornerRadius));

        g2.dispose();
    }

    private Color resolveColor() {
        switch (type) {
            case SUCCESS: return Theme.GREEN;
            case ERROR: return Theme.RED;
            case WARNING: return Theme.YELLOW;
            case INFO:
            default:
                return Theme.BLUE;
        }
    }

    private String resolveSymbol() {
        switch (type) {
            case SUCCESS: return "\u2713"; // Checkmark
            case ERROR: return "\u2715";   // Cross
            case WARNING: return "!";      // Exclamation
            case INFO:
            default:
                return "i";
        }
    }
}
