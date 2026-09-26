package quiz.ui;

import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.RoundRectangle2D;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * ModernTextField - Trường nhập liệu hiện đại với placeholder, viền bo góc,
 * hiệu ứng highlight focus và trạng thái báo lỗi màu đỏ.
 */
public class ModernTextField extends JTextField {

    private String placeholder = "";
    private boolean isFocused = false;
    private boolean hasError = false;
    private int cornerRadius = 8;

    public ModernTextField() {
        this("", 15);
    }

    public ModernTextField(String placeholder) {
        this(placeholder, 15);
    }

    public ModernTextField(String placeholder, int columns) {
        super(columns);
        this.placeholder = placeholder;
        init();
    }

    private void init() {
        setOpaque(false);
        setFont(Theme.BODY);
        setForeground(Theme.DARK_TEXT);
        setCaretColor(Theme.BLUE);
        setBorder(new EmptyBorder(10, 14, 10, 14));

        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                isFocused = true;
                repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                isFocused = false;
                repaint();
            }
        });
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
        repaint();
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public void setError(boolean error) {
        this.hasError = error;
        repaint();
    }

    public boolean hasError() {
        return hasError;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.setupAntiAliasing(g2);

        int w = getWidth();
        int h = getHeight();

        // 1. Vẽ nền trắng bo góc
        g2.setColor(isEnabled() ? Theme.WHITE : Theme.OFF_WHITE);
        g2.fill(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, cornerRadius, cornerRadius));

        // 2. Vẽ viền (thay đổi theo focus hoặc error)
        if (hasError) {
            g2.setColor(Theme.RED);
            g2.setStroke(new BasicStroke(1.8f));
        } else if (isFocused) {
            g2.setColor(Theme.BLUE);
            g2.setStroke(new BasicStroke(1.8f));
        } else {
            g2.setColor(Theme.BORDER_LIGHT);
            g2.setStroke(new BasicStroke(1.2f));
        }
        g2.draw(new RoundRectangle2D.Float(1.5f, 1.5f, w - 3, h - 3, cornerRadius, cornerRadius));

        g2.dispose();
        super.paintComponent(g);

        // 3. Vẽ placeholder khi chưa nhập chữ
        if (getText().isEmpty() && !isFocused && placeholder != null && !placeholder.isEmpty()) {
            Graphics2D gPlaceholder = (Graphics2D) g.create();
            Theme.setupAntiAliasing(gPlaceholder);
            gPlaceholder.setColor(Theme.MUTED_GRAY);
            gPlaceholder.setFont(getFont());
            Insets insets = getInsets();
            FontMetrics fm = gPlaceholder.getFontMetrics();
            int y = (h - fm.getHeight()) / 2 + fm.getAscent();
            gPlaceholder.drawString(placeholder, insets.left, y);
            gPlaceholder.dispose();
        }
    }
}
