package quiz.ui;

import java.awt.*;
import java.awt.geom.*;
import javax.swing.*;

/**
 * EmoteBubble — Speech bubble nổi hiển thị emoji emote trong MatchScreen.
 * Tự biến mất sau ~2 giây với fade-out animation.
 * Không block UI thread (dùng javax.swing.Timer).
 */
public class EmoteBubble extends JComponent {

    private final String emoji;
    private float alpha = 1.0f;
    private final Timer fadeTimer;
    private final Timer showTimer;
    private Runnable onDone;

    private static final int BUBBLE_W = 80;
    private static final int BUBBLE_H = 60;
    private static final int TAIL_H = 12;
    private static final int TOTAL_H = BUBBLE_H + TAIL_H;
    private static final int CORNER = 16;

    public EmoteBubble(String emoji) {
        this.emoji = emoji;
        setPreferredSize(new Dimension(BUBBLE_W, TOTAL_H + 4));
        setOpaque(false);

        // Fade out sau 1.8 giây
        fadeTimer = new Timer(40, null);
        fadeTimer.addActionListener(e -> {
            alpha -= 0.06f;
            if (alpha <= 0f) {
                alpha = 0f;
                fadeTimer.stop();
                if (onDone != null) onDone.run();
            }
            repaint();
        });

        // Bắt đầu fade sau 1.8s hiển thị
        // Dùng Timer[1] pattern để tránh 'might not have been initialized'
        Timer[] holder = new Timer[1];
        holder[0] = new Timer(1800, e -> {
            holder[0].stop();
            fadeTimer.start();
        });
        holder[0].setRepeats(false);
        showTimer = holder[0];
    }

    /** Bắt đầu hiển thị. Gọi sau khi đã add vào parent. */
    public void show(Runnable onDone) {
        this.onDone = onDone;
        alpha = 1.0f;
        repaint();
        showTimer.start();
    }

    /** Dừng ngay lập tức */
    public void dismiss() {
        showTimer.stop();
        fadeTimer.stop();
        alpha = 0f;
        if (onDone != null) onDone.run();
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (alpha <= 0f) return;
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.setupAntiAliasing(g2);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

        int w = BUBBLE_W;
        // Vẽ bubble body
        RoundRectangle2D body = new RoundRectangle2D.Float(0, 0, w, BUBBLE_H, CORNER, CORNER);
        g2.setColor(new Color(0xFF, 0xFF, 0xFF, 230));
        g2.fill(body);
        g2.setColor(new Color(0x86, 0x4C, 0xBF)); // Theme.PURPLE border
        g2.setStroke(new BasicStroke(2f));
        g2.draw(body);

        // Vẽ đuôi hình tam giác nhọn xuống dưới giữa
        int mx = w / 2;
        Path2D tail = new Path2D.Float();
        tail.moveTo(mx - 8, BUBBLE_H - 1);
        tail.lineTo(mx + 8, BUBBLE_H - 1);
        tail.lineTo(mx, BUBBLE_H + TAIL_H);
        tail.closePath();
        g2.setColor(new Color(0xFF, 0xFF, 0xFF, 230));
        g2.fill(tail);
        // Viền tail
        g2.setColor(new Color(0x86, 0x4C, 0xBF));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawLine(mx - 8, BUBBLE_H - 1, mx, BUBBLE_H + TAIL_H);
        g2.drawLine(mx + 8, BUBBLE_H - 1, mx, BUBBLE_H + TAIL_H);

        // Vẽ emoji
        g2.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));
        FontMetrics fm = g2.getFontMetrics();
        int tx = (w - fm.stringWidth(emoji)) / 2;
        int ty = (BUBBLE_H - fm.getHeight()) / 2 + fm.getAscent();
        g2.setColor(Color.BLACK);
        g2.drawString(emoji, tx, ty);

        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(BUBBLE_W, TOTAL_H + 4);
    }
}
