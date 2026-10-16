package quiz.ui;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.*;

/**
 * TopicCard — Thẻ chủ đề trực quan với ảnh nền cover-crop, hiệu ứng hover phóng to mượt mà,
 * lớp phủ tương tác, viền phát sáng cyan và trạng thái chọn (check mark).
 */
public class TopicCard extends JPanel {

    private static final int CARD_WIDTH = 205;
    private static final int CARD_HEIGHT = 160;
    private static final int IMAGE_HEIGHT = 106;
    private static final int CORNER_RADIUS = 14;

    private final String topicName;
    private boolean isSelected = false;

    // Animation hover bằng Swing Timer (chạy an toàn trên EDT)
    private float hoverProgress = 0.0f; // 0.0f -> 1.0f
    private Timer animTimer;

    private Consumer<String> onSelectListener;

    public TopicCard(String topicName) {
        this(topicName, false, null);
    }

    public TopicCard(String topicName, boolean selected, Consumer<String> onSelectListener) {
        this.topicName = topicName;
        this.isSelected = selected;
        this.onSelectListener = onSelectListener;

        setPreferredSize(new Dimension(CARD_WIDTH, CARD_HEIGHT));
        setMinimumSize(new Dimension(CARD_WIDTH, CARD_HEIGHT));
        setMaximumSize(new Dimension(CARD_WIDTH, CARD_HEIGHT));
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        initMouseListeners();
    }

    public String getTopicName() {
        return topicName;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        if (this.isSelected != selected) {
            this.isSelected = selected;
            repaint();
        }
    }

    public void setOnSelectListener(Consumer<String> listener) {
        this.onSelectListener = listener;
    }

    private void initMouseListeners() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                startAnimation(true);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                startAnimation(false);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                SoundManager.playClick();
                if (onSelectListener != null) {
                    onSelectListener.accept(topicName);
                }
            }
        });
    }

    /**
     * Chạy animation mượt mà bằng Swing Timer, không làm đơ giao diện.
     */
    private void startAnimation(boolean forward) {
        if (animTimer != null && animTimer.isRunning()) {
            animTimer.stop();
        }

        final float step = 0.15f;
        animTimer = new Timer(15, e -> {
            if (forward) {
                hoverProgress = Math.min(1.0f, hoverProgress + step);
                if (hoverProgress >= 1.0f) {
                    ((Timer) e.getSource()).stop();
                }
            } else {
                hoverProgress = Math.max(0.0f, hoverProgress - step);
                if (hoverProgress <= 0.0f) {
                    ((Timer) e.getSource()).stop();
                }
            }
            repaint();
        });
        animTimer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.setupAntiAliasing(g2);

        int w = getWidth();
        int h = getHeight();

        // 1. Định nghĩa khung bo góc toàn thẻ
        Shape cardShape = new RoundRectangle2D.Float(0, 0, w, h, CORNER_RADIUS, CORNER_RADIUS);

        // Nền thẻ tổng thể
        g2.setColor(new Color(0x18, 0x22, 0x36));
        g2.fill(cardShape);

        // 2. VẼ ẢNH CHỦ ĐỀ TRONG VÙNG ẢNH (BỊ CLIP CHẶT CHẼ, KHÔNG TRÀN RA NGOÀI)
        Shape oldClip = g2.getClip();
        // Cắt theo khung thẻ VÀ chỉ lấy nửa trên
        g2.clip(cardShape);

        BufferedImage img = TopicImageManager.getInstance().getTopicImage(topicName, w, IMAGE_HEIGHT);
        if (img != null) {
            // Hiệu ứng zoom nhẹ 1.05 - 1.08x khi hover
            float scale = 1.0f + 0.08f * hoverProgress;
            int drawW = (int) Math.round(w * scale);
            int drawH = (int) Math.round(IMAGE_HEIGHT * scale);
            int drawX = (w - drawW) / 2;
            int drawY = (IMAGE_HEIGHT - drawH) / 2;

            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(img, drawX, drawY, drawW, drawH, null);
        }

        // Lớp phủ tối nhẹ mặc định để ảnh hòa nhập tốt
        g2.setColor(new Color(0, 0, 0, 30));
        g2.fillRect(0, 0, w, IMAGE_HEIGHT);

        // 3. HIỆU ỨNG HOVER TRÊN ẢNH
        if (hoverProgress > 0.01f) {
            // Lớp phủ xám/tối mờ tăng dần khi di chuột
            int alphaDark = (int) (105 * hoverProgress);
            g2.setColor(new Color(10, 18, 30, alphaDark));
            g2.fillRect(0, 0, w, IMAGE_HEIGHT);

            // Chữ hiển thị trên ảnh khi hover
            int textAlpha = (int) (255 * hoverProgress);
            g2.setColor(new Color(255, 255, 255, textAlpha));
            g2.setFont(Theme.SMALL_BOLD);
            FontMetrics fm = g2.getFontMetrics();
            String hoverText = isSelected ? "ĐANG CHỌN" : "NHẤP ĐỂ CHỌN";
            int tw = fm.stringWidth(hoverText);
            int tx = (w - tw) / 2;
            int ty = IMAGE_HEIGHT / 2 + fm.getAscent() / 2 - 2;

            // Nền pill nhỏ sau chữ hover cho nét và nổi bật
            g2.setColor(new Color(0, 0, 0, (int) (140 * hoverProgress)));
            g2.fill(new RoundRectangle2D.Float(tx - 10, ty - fm.getAscent() - 3, tw + 20, fm.getHeight() + 6, 12, 12));

            // Viền pill
            g2.setColor(new Color(0x06, 0xB6, 0xD4, textAlpha));
            g2.setStroke(new BasicStroke(1.0f));
            g2.draw(new RoundRectangle2D.Float(tx - 10, ty - fm.getAscent() - 3, tw + 20, fm.getHeight() + 6, 12, 12));

            g2.setColor(Color.WHITE);
            g2.drawString(hoverText, tx, ty);
        }

        // Khôi phục clip gốc
        g2.setClip(oldClip);

        // 4. VÙNG TÊN CHỦ ĐỀ PHÍA DƯỚI
        int titleY = IMAGE_HEIGHT;
        int titleH = h - IMAGE_HEIGHT;

        // Nền dải tên chủ đề (gradient nhẹ từ đen slate)
        Color titleBg = isSelected ? new Color(0x13, 0x24, 0x40) : new Color(0x14, 0x1C, 0x2E);
        g2.setColor(titleBg);
        // Clip lại theo khung thẻ để 2 góc dưới được bo tròn hoàn hảo
        g2.clip(cardShape);
        g2.fillRect(0, titleY, w, titleH);

        // Đường kẻ phân tách nhẹ giữa ảnh và tên
        g2.setColor(isSelected ? new Color(0x06, 0xB6, 0xD4, 80) : new Color(0x2A, 0x37, 0x50));
        g2.drawLine(0, titleY, w, titleY);

        // Vẽ tên chủ đề (tự động ngắt dòng nếu dài, căn giữa)
        drawTopicTitle(g2, w, titleY, titleH);

        g2.setClip(oldClip);

        // 5. VIỀN CARD VÀ HIỆU ỨNG SELECTED / HOVER
        if (isSelected) {
            // Hào quang tỏa sáng cyan quanh viền (Glow effect)
            g2.setColor(new Color(0x00, 0xE5, 0xFF, 60));
            g2.setStroke(new BasicStroke(4.0f));
            g2.draw(new RoundRectangle2D.Float(1.5f, 1.5f, w - 3, h - 3, CORNER_RADIUS, CORNER_RADIUS));

            // Viền chính màu xanh cyan sắc nét
            g2.setColor(new Color(0x00, 0xE5, 0xFF));
            g2.setStroke(new BasicStroke(2.4f));
            g2.draw(new RoundRectangle2D.Float(1.0f, 1.0f, w - 2, h - 2, CORNER_RADIUS, CORNER_RADIUS));

            // Biểu tượng dấu tích (check icon) góc trên bên phải
            drawCheckBadge(g2, w);
        } else if (hoverProgress > 0.01f) {
            // Viền sáng lên màu cyan khi hover
            int bAlpha = (int) (180 * hoverProgress);
            g2.setColor(new Color(0x06, 0xB6, 0xD4, bAlpha));
            g2.setStroke(new BasicStroke(1.8f));
            g2.draw(new RoundRectangle2D.Float(1.0f, 1.0f, w - 2, h - 2, CORNER_RADIUS, CORNER_RADIUS));
        } else {
            // Viền mặc định phẳng tinh tế
            g2.setColor(new Color(0x35, 0x45, 0x62));
            g2.setStroke(new BasicStroke(1.2f));
            g2.draw(new RoundRectangle2D.Float(0.6f, 0.6f, w - 1.2f, h - 1.2f, CORNER_RADIUS, CORNER_RADIUS));
        }

        g2.dispose();
    }

    /**
     * Vẽ tên chủ đề căn giữa, tự động tính toán 1 hoặc 2 dòng để không bị cắt.
     */
    private void drawTopicTitle(Graphics2D g2, int w, int titleY, int titleH) {
        g2.setFont(Theme.SMALL_BOLD);
        FontMetrics fm = g2.getFontMetrics();

        int maxTextWidth = w - 16;
        String title = topicName;

        if (isSelected) {
            g2.setColor(new Color(0x00, 0xE5, 0xFF));
        } else {
            g2.setColor(new Color(0xF3, 0xF4, 0xF6));
        }

        if (fm.stringWidth(title) <= maxTextWidth) {
            // 1 dòng duy nhất
            int tx = (w - fm.stringWidth(title)) / 2;
            int ty = titleY + (titleH + fm.getAscent() - fm.getDescent()) / 2;
            g2.drawString(title, tx, ty);
        } else {
            // 2 dòng cân đối
            List<String> lines = splitIntoTwoLines(title, fm, maxTextWidth);
            int lineSpacing = 3;
            int totalTextHeight = fm.getHeight() * lines.size() + (lines.size() - 1) * lineSpacing;
            int startY = titleY + (titleH - totalTextHeight) / 2 + fm.getAscent();

            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                int tx = (w - fm.stringWidth(line)) / 2;
                int ty = startY + i * (fm.getHeight() + lineSpacing);
                g2.drawString(line, tx, ty);
            }
        }
    }

    /**
     * Chia 2 dòng cân đối theo từ và độ rộng thực tế (pixel) để chữ hiển thị đẹp nhất.
     */
    private List<String> splitIntoTwoLines(String text, FontMetrics fm, int maxWidth) {
        List<String> result = new ArrayList<>();
        String[] words = text.split(" ");
        if (words.length <= 1) {
            result.add(text);
            return result;
        }

        // Tìm điểm cắt sao cho độ dài pixel 2 dòng cân bằng nhất
        int bestSplit = 1;
        int minDiff = Integer.MAX_VALUE;

        for (int i = 1; i < words.length; i++) {
            StringBuilder s1 = new StringBuilder();
            for (int j = 0; j < i; j++) {
                if (j > 0) s1.append(" ");
                s1.append(words[j]);
            }
            StringBuilder s2 = new StringBuilder();
            for (int j = i; j < words.length; j++) {
                if (j > i) s2.append(" ");
                s2.append(words[j]);
            }
            int w1 = fm.stringWidth(s1.toString());
            int w2 = fm.stringWidth(s2.toString());
            int diff = Math.abs(w1 - w2);

            // Ưu tiên cách chia mà cả 2 dòng đều nằm trong maxWidth
            if (w1 <= maxWidth && w2 <= maxWidth) {
                if (diff < minDiff) {
                    minDiff = diff;
                    bestSplit = i;
                }
            } else if (minDiff == Integer.MAX_VALUE && i == words.length / 2) {
                bestSplit = i;
            }
        }

        StringBuilder line1 = new StringBuilder();
        for (int j = 0; j < bestSplit; j++) {
            if (j > 0) line1.append(" ");
            line1.append(words[j]);
        }
        StringBuilder line2 = new StringBuilder();
        for (int j = bestSplit; j < words.length; j++) {
            if (j > bestSplit) line2.append(" ");
            line2.append(words[j]);
        }

        result.add(line1.toString());
        result.add(line2.toString());
        return result;
    }

    /**
     * Vẽ badge dấu tích xanh cyan xác nhận trạng thái được chọn ở góc trên bên phải.
     */
    private void drawCheckBadge(Graphics2D g2, int w) {
        int badgeSize = 22;
        int bx = w - badgeSize - 8;
        int by = 8;

        // Vòng tròn nền badge
        g2.setColor(new Color(0x00, 0xE5, 0xFF));
        g2.fill(new Ellipse2D.Float(bx, by, badgeSize, badgeSize));

        // Viền vòng tròn
        g2.setColor(new Color(0x10, 0x1E, 0x36));
        g2.setStroke(new BasicStroke(1.5f));
        g2.draw(new Ellipse2D.Float(bx, by, badgeSize, badgeSize));

        // Dấu tích trắng
        g2.setColor(new Color(0x10, 0x1E, 0x36));
        g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int[] xPoints = {bx + 5, bx + 9, bx + 16};
        int[] yPoints = {by + 11, by + 15, by + 7};
        g2.drawPolyline(xPoints, yPoints, 3);
    }
}
