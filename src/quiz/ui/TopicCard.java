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
 * TopicCard — Thẻ chủ đề hiện đại với bo góc mềm mại, đổ bóng nhẹ, hiệu ứng nổi (lift-up) khi hover,
 * ảnh thumbnail cover-crop không méo, bố cục tên & mô tả cân đối, viền sáng và checkmark khi được chọn.
 */
public class TopicCard extends JPanel {

    public static final int CARD_WIDTH = 212;
    public static final int CARD_HEIGHT = 176;
    private static final int CORNER_RADIUS = 16;
    private static final int IMAGE_HEIGHT = 104;

    // Lề trong component để hiển thị bóng đổ và hiệu ứng nổi (lift) mà không bị che khuất
    private static final int PAD_HORIZ = 5;
    private static final int PAD_TOP = 6;
    private static final int PAD_BOTTOM = 8;
    private static final int MAX_LIFT_PX = 4;

    private final String topicName;
    private final String subtitle;
    private boolean isSelected = false;

    // Animation hover mượt mà bằng Swing Timer (chạy an toàn trên Swing EDT)
    private float hoverProgress = 0.0f; // 0.0f -> 1.0f
    private Timer animTimer;

    private Consumer<String> onSelectListener;

    public TopicCard(String topicName) {
        this(topicName, false, null);
    }

    public TopicCard(String topicName, boolean selected, Consumer<String> onSelectListener) {
        this.topicName = topicName;
        this.subtitle = resolveSubtitle(topicName);
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
     * Chạy animation mượt mà bằng Swing Timer trên EDT (60 FPS).
     */
    private void startAnimation(boolean forward) {
        if (animTimer != null && animTimer.isRunning()) {
            animTimer.stop();
        }

        final float step = 0.16f;
        animTimer = new Timer(16, e -> {
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

        int compW = getWidth();
        int compH = getHeight();

        // 1. Tính toán toạ độ thẻ và hiệu ứng nâng lên (lift effect) khi hover
        float currentLift = MAX_LIFT_PX * hoverProgress;
        int cardX = PAD_HORIZ;
        int cardY = PAD_TOP - Math.round(currentLift);
        int cardW = compW - PAD_HORIZ * 2;
        int cardH = compH - PAD_TOP - PAD_BOTTOM;

        // 2. VẼ ĐỔ BÓNG MỀM MẠI (Soft drop shadow)
        paintCardShadow(g2, cardX, cardY, cardW, cardH, currentLift);

        // 3. KHUNG THẺ CHÍNH
        Shape cardShape = new RoundRectangle2D.Float(cardX, cardY, cardW, cardH, CORNER_RADIUS, CORNER_RADIUS);

        // Nền thẻ tổng thể (dark navy hiện đại)
        g2.setColor(new Color(0x13, 0x1D, 0x2E));
        g2.fill(cardShape);

        // 4. VẼ THUMBNAIL ẢNH (Cover-Crop không méo, không kéo giãn)
        Shape oldClip = g2.getClip();
        g2.clip(cardShape);

        BufferedImage img = TopicImageManager.getInstance().getTopicImage(topicName, cardW, IMAGE_HEIGHT);
        if (img != null) {
            // Zoom nhẹ 1.04x khi hover tạo chiều sâu
            float zoom = 1.0f + 0.04f * hoverProgress;
            int drawW = Math.round(cardW * zoom);
            int drawH = Math.round(IMAGE_HEIGHT * zoom);
            int drawX = cardX + (cardW - drawW) / 2;
            int drawY = cardY + (IMAGE_HEIGHT - drawH) / 2;

            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(img, drawX, drawY, drawW, drawH, null);
        }

        // Gradient chuyển tiếp mềm mại từ ảnh xuống vùng thông tin thẻ
        GradientPaint fadeGradient = new GradientPaint(
            cardX, cardY + IMAGE_HEIGHT - 38, new Color(0x13, 0x1D, 0x2E, 0),
            cardX, cardY + IMAGE_HEIGHT, new Color(0x13, 0x1D, 0x2E, 240)
        );
        g2.setPaint(fadeGradient);
        g2.fillRect(cardX, cardY + IMAGE_HEIGHT - 38, cardW, 38);

        // Lớp phủ tối nhẹ & pill chữ khi hover
        if (hoverProgress > 0.02f) {
            int alphaDark = (int) (90 * hoverProgress);
            g2.setColor(new Color(10, 18, 30, alphaDark));
            g2.fillRect(cardX, cardY, cardW, IMAGE_HEIGHT);

            // Pill tương tác ở tâm ảnh khi hover
            paintHoverPill(g2, cardX, cardY, cardW);
        }

        // Pill số câu hỏi nhỏ ở góc trên bên trái
        paintQuestionCountBadge(g2, cardX + 8, cardY + 8);

        g2.setClip(oldClip);

        // 5. VÙNG THÔNG TIN: TÊN CHỦ ĐỀ & MÔ TẢ PHÍA DƯỚI
        int titleAreaY = cardY + IMAGE_HEIGHT;
        int titleAreaH = cardH - IMAGE_HEIGHT;

        g2.clip(cardShape);
        Color titleBg = isSelected ? new Color(0x15, 0x25, 0x3D) : new Color(0x10, 0x18, 0x27);
        g2.setColor(titleBg);
        g2.fillRect(cardX, titleAreaY, cardW, titleAreaH);

        // Đường kẻ ngăn cách tinh tế
        g2.setColor(isSelected ? new Color(0x00, 0xE5, 0xFF, 70) : new Color(0x24, 0x33, 0x4B));
        g2.drawLine(cardX, titleAreaY, cardX + cardW, titleAreaY);

        // Bố trí Tên chủ đề & Mô tả cân đối
        paintTopicText(g2, cardX, titleAreaY, cardW, titleAreaH);

        g2.setClip(oldClip);

        // 6. VIỀN CARD VÀ HIỆU ỨNG SELECTED / HOVER
        if (isSelected) {
            // Hào quang tỏa sáng cyan quanh viền (Glow effect)
            g2.setColor(new Color(0x00, 0xE5, 0xFF, 60));
            g2.setStroke(new BasicStroke(3.8f));
            g2.draw(new RoundRectangle2D.Float(cardX + 1.5f, cardY + 1.5f, cardW - 3f, cardH - 3f, CORNER_RADIUS, CORNER_RADIUS));

            // Viền chính màu xanh cyan sắc nét
            g2.setColor(new Color(0x00, 0xE5, 0xFF));
            g2.setStroke(new BasicStroke(2.2f));
            g2.draw(new RoundRectangle2D.Float(cardX + 0.8f, cardY + 0.8f, cardW - 1.6f, cardH - 1.6f, CORNER_RADIUS, CORNER_RADIUS));

            // Biểu tượng dấu tích (check badge) góc trên bên phải
            drawCheckBadge(g2, cardX + cardW - 28, cardY + 8);
        } else if (hoverProgress > 0.02f) {
            // Viền sáng lên màu cyan khi hover
            int bAlpha = (int) (180 * hoverProgress);
            g2.setColor(new Color(0x06, 0xB6, 0xD4, bAlpha));
            g2.setStroke(new BasicStroke(1.8f));
            g2.draw(new RoundRectangle2D.Float(cardX + 0.8f, cardY + 0.8f, cardW - 1.6f, cardH - 1.6f, CORNER_RADIUS, CORNER_RADIUS));
        } else {
            // Viền mặc định phẳng tinh tế
            g2.setColor(new Color(0x2B, 0x3B, 0x54));
            g2.setStroke(new BasicStroke(1.2f));
            g2.draw(new RoundRectangle2D.Float(cardX + 0.6f, cardY + 0.6f, cardW - 1.2f, cardH - 1.2f, CORNER_RADIUS, CORNER_RADIUS));
        }

        g2.dispose();
    }

    /**
     * Vẽ bóng đổ nhẹ mềm mại (soft drop shadow) thích ứng theo trạng thái hover.
     */
    private void paintCardShadow(Graphics2D g2, int x, int y, int w, int h, float lift) {
        int baseAlpha = 8 + (int) (12 * hoverProgress);
        int shadowLayers = 5;
        for (int i = 1; i <= shadowLayers; i++) {
            int alpha = Math.max(2, baseAlpha - i * 2);
            g2.setColor(new Color(10, 15, 26, alpha));
            float offsetY = y + i * (1.2f + 0.4f * hoverProgress);
            g2.fill(new RoundRectangle2D.Float(
                x + i * 0.4f,
                offsetY,
                w - i * 0.8f,
                h - 1,
                CORNER_RADIUS + i,
                CORNER_RADIUS + i
            ));
        }
    }

    /**
     * Pill hiển thị "10 CÂU" ở góc trên bên trái ảnh.
     */
    private void paintQuestionCountBadge(Graphics2D g2, int x, int y) {
        String badgeText = "10 CÂU";
        g2.setFont(Theme.SMALL_BOLD);
        FontMetrics fm = g2.getFontMetrics();
        int bw = fm.stringWidth(badgeText) + 12;
        int bh = 18;

        // Nền mờ kính
        g2.setColor(new Color(15, 23, 42, 190));
        g2.fill(new RoundRectangle2D.Float(x, y, bw, bh, 8, 8));

        // Viền mờ
        g2.setColor(new Color(255, 255, 255, 45));
        g2.setStroke(new BasicStroke(1.0f));
        g2.draw(new RoundRectangle2D.Float(x, y, bw, bh, 8, 8));

        // Chữ
        g2.setColor(new Color(0x38, 0xBD, 0xF8));
        g2.drawString(badgeText, x + 6, y + (bh + fm.getAscent() - fm.getDescent()) / 2);
    }

    /**
     * Pill chữ nổi bật khi di chuột vào ảnh.
     */
    private void paintHoverPill(Graphics2D g2, int cardX, int cardY, int cardW) {
        int textAlpha = (int) (255 * hoverProgress);
        g2.setFont(Theme.SMALL_BOLD);
        FontMetrics fm = g2.getFontMetrics();
        String hoverText = isSelected ? "ĐANG CHỌN" : "CHỌN CHỦ ĐỀ";
        int tw = fm.stringWidth(hoverText);
        int tx = cardX + (cardW - tw) / 2;
        int ty = cardY + IMAGE_HEIGHT / 2 + fm.getAscent() / 2 - 2;

        int pillW = tw + 18;
        int pillH = fm.getHeight() + 6;
        int pillX = tx - 9;
        int pillY = ty - fm.getAscent() - 3;

        // Nền pill
        g2.setColor(new Color(15, 23, 42, (int) (210 * hoverProgress)));
        g2.fill(new RoundRectangle2D.Float(pillX, pillY, pillW, pillH, 12, 12));

        // Viền pill cyan
        g2.setColor(new Color(0x00, 0xE5, 0xFF, textAlpha));
        g2.setStroke(new BasicStroke(1.2f));
        g2.draw(new RoundRectangle2D.Float(pillX, pillY, pillW, pillH, 12, 12));

        g2.setColor(new Color(255, 255, 255, textAlpha));
        g2.drawString(hoverText, tx, ty);
    }

    /**
     * Bố trí Tên chủ đề và Mô tả cân đối ở phần dưới thẻ.
     */
    private void paintTopicText(Graphics2D g2, int cardX, int titleY, int cardW, int titleH) {
        int padSide = 10;
        int maxTextWidth = cardW - padSide * 2;

        // 1. Tên chủ đề
        g2.setFont(Theme.BODY_BOLD);
        FontMetrics fmTitle = g2.getFontMetrics();

        Color titleColor = isSelected ? new Color(0x38, 0xBD, 0xF8) : new Color(0xF8, 0xFA, 0xFC);
        g2.setColor(titleColor);

        // Kiểm tra xem tên có vừa 1 dòng không
        String title = topicName;
        boolean singleLine = fmTitle.stringWidth(title) <= maxTextWidth;

        if (singleLine) {
            // Tên 1 dòng + Mô tả bên dưới
            int titleBaseY = titleY + 22;
            int tx = cardX + padSide;
            g2.drawString(title, tx, titleBaseY);

            // Mô tả bên dưới
            g2.setFont(Theme.SMALL);
            g2.setColor(new Color(0x94, 0xA3, 0xB8));
            FontMetrics fmSub = g2.getFontMetrics();
            int subBaseY = titleBaseY + fmSub.getHeight() + 2;

            String subText = subtitle;
            if (fmSub.stringWidth(subText) > maxTextWidth) {
                while (subText.length() > 3 && fmSub.stringWidth(subText + "…") > maxTextWidth) {
                    subText = subText.substring(0, subText.length() - 1);
                }
                subText = subText + "…";
            }
            g2.drawString(subText, tx, subBaseY);

        } else {
            // Tên dài chia thành 2 dòng cân đối
            List<String> lines = splitIntoTwoLines(title, fmTitle, maxTextWidth);
            int lineSpacing = 2;
            int startY = titleY + 18;

            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                int tx = cardX + padSide;
                int ty = startY + i * (fmTitle.getHeight() + lineSpacing);
                g2.drawString(line, tx, ty);
            }

            // Nếu còn không gian, hiển thị mô tả ngắn gọn
            if (lines.size() <= 2) {
                g2.setFont(Theme.SMALL);
                g2.setColor(new Color(0x94, 0xA3, 0xB8));
                FontMetrics fmSub = g2.getFontMetrics();
                int subBaseY = startY + lines.size() * (fmTitle.getHeight() + lineSpacing) + fmSub.getAscent() - 2;
                if (subBaseY < titleY + titleH - 3) {
                    String subText = subtitle;
                    if (fmSub.stringWidth(subText) > maxTextWidth) {
                        while (subText.length() > 3 && fmSub.stringWidth(subText + "…") > maxTextWidth) {
                            subText = subText.substring(0, subText.length() - 1);
                        }
                        subText = subText + "…";
                    }
                    g2.drawString(subText, cardX + padSide, subBaseY);
                }
            }
        }
    }

    /**
     * Chia 2 dòng cân đối theo từ để hiển thị đẹp nhất.
     */
    private List<String> splitIntoTwoLines(String text, FontMetrics fm, int maxWidth) {
        List<String> result = new ArrayList<>();
        String[] words = text.split(" ");
        if (words.length <= 1) {
            result.add(text);
            return result;
        }

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
    private void drawCheckBadge(Graphics2D g2, int bx, int by) {
        int badgeSize = 22;

        // Vòng tròn nền badge
        g2.setColor(new Color(0x00, 0xE5, 0xFF));
        g2.fill(new Ellipse2D.Float(bx, by, badgeSize, badgeSize));

        // Viền vòng tròn
        g2.setColor(new Color(0x10, 0x1E, 0x36));
        g2.setStroke(new BasicStroke(1.5f));
        g2.draw(new Ellipse2D.Float(bx, by, badgeSize, badgeSize));

        // Dấu tích sẫm màu rõ nét
        g2.setColor(new Color(0x0F, 0x17, 0x2A));
        g2.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int[] xPoints = {bx + 6, bx + 9, bx + 16};
        int[] yPoints = {by + 11, by + 15, by + 7};
        g2.drawPolyline(xPoints, yPoints, 3);
    }

    /**
     * Trả về mô tả ngắn/chuyên đề phù hợp với từng chủ đề.
     */
    public static String resolveSubtitle(String topic) {
        if (topic == null || topic.isBlank()) {
            return "Trắc nghiệm kiến thức";
        }
        switch (topic.trim()) {
            case "Tổng hợp":
                return "Kho kiến thức đa dạng";
            case "Kỳ quan thế giới":
                return "Kiến trúc & Cảnh quan kỳ vĩ";
            case "Địa danh và khám phá thế giới":
                return "Hành trình vòng quanh thế giới";
            case "Những con người làm nên lịch sử":
                return "Danh nhân & Nhân vật vĩ đại";
            case "Sự thật thú vị về thế giới":
                return "Bí ẩn & Khám phá kỳ thú";
            case "Dòng thời gian và những cột mốc":
                return "Lịch sử & Niên đại tiêu biểu";
            case "Vũ trụ và khám phá không gian":
                return "Thiên văn & Dải ngân hà";
            case "Văn hóa và di sản nhân loại":
                return "Phong tục & Di sản nhân loại";
            case "Những điều lớn nhất, nhỏ nhất và đầu tiên":
                return "Kỷ lục ấn tượng của nhân loại";
            case "Toán học":
                return "Tư duy logic & Đại số";
            case "Vật lý":
                return "Quy luật tự nhiên & Cơ học";
            case "Hóa học":
                return "Nguyên tố & Phản ứng";
            case "Sinh học":
                return "Thế giới sinh vật & Tự nhiên";
            default:
                return "Bộ câu hỏi chọn lọc";
        }
    }
}
