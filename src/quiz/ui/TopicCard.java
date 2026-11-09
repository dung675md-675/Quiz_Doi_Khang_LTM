package quiz.ui;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.*;

/** Photo-backed topic tile with keyboard selection and a restrained hover zoom. */
public class TopicCard extends JPanel {
    public static final int CARD_WIDTH = 210;
    public static final int CARD_HEIGHT = 140;
    private static final int CORNER_RADIUS = 20;
    private final String topicName;
    private boolean isSelected;
    private float hoverProgress;
    private Timer animTimer;
    private Consumer<String> onSelectListener;

    public TopicCard(String topicName) {
        this(topicName, false, null);
    }

    public TopicCard(String topicName, boolean selected, Consumer<String> onSelectListener) {
        this.topicName = topicName;
        this.isSelected = selected;
        this.onSelectListener = onSelectListener;
        Dimension size = new Dimension(CARD_WIDTH, CARD_HEIGHT);
        setPreferredSize(size);
        setMinimumSize(size);
        setMaximumSize(size);
        setOpaque(false);
        setFocusable(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setToolTipText(topicName);
        getAccessibleContext().setAccessibleName("Chủ đề: " + topicName);
        getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "selectTopic");
        getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "selectTopic");
        getActionMap().put("selectTopic", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent event) { chooseTopic(); }
        });
        addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent event) {
                scrollRectToVisible(new Rectangle(0, 0, getWidth(), getHeight()));
                repaint();
            }
            @Override public void focusLost(FocusEvent event) { repaint(); }
        });
        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent event) { animateHover(true); }
            @Override public void mouseExited(MouseEvent event) { animateHover(false); }
            @Override public void mouseClicked(MouseEvent event) {
                if (SwingUtilities.isLeftMouseButton(event)) {
                    requestFocusInWindow();
                    chooseTopic();
                }
            }
        });
    }

    private void chooseTopic() {
        SoundManager.playClick();
        if (onSelectListener != null) onSelectListener.accept(topicName);
    }

    public String getTopicName() { return topicName; }
    public boolean isSelected() { return isSelected; }
    public void setSelected(boolean selected) {
        if (isSelected != selected) {
            isSelected = selected;
            getAccessibleContext().setAccessibleDescription(selected ? "Chủ đề đang chọn" : "Nhấn Enter để chọn chủ đề");
            repaint();
        }
    }
    public void setOnSelectListener(Consumer<String> listener) { onSelectListener = listener; }

    private void animateHover(boolean forward) {
        if (animTimer != null) animTimer.stop();
        if (Boolean.getBoolean("quiz.ui.reduceMotion")) {
            hoverProgress = forward ? 1 : 0;
            repaint();
            return;
        }
        animTimer = new Timer(20, event -> {
            hoverProgress = forward ? Math.min(1, hoverProgress + 0.18f)
                    : Math.max(0, hoverProgress - 0.18f);
            if (hoverProgress == 0 || hoverProgress == 1) ((Timer) event.getSource()).stop();
            repaint();
        });
        animTimer.start();
    }

    @Override public void removeNotify() {
        if (animTimer != null) animTimer.stop();
        hoverProgress = 0;
        super.removeNotify();
    }

    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        Theme.setupAntiAliasing(g);
        int w = getWidth();
        int h = getHeight();
        Shape shape = new RoundRectangle2D.Float(1, 1, w - 2, h - 2, CORNER_RADIUS, CORNER_RADIUS);
        g.clip(shape);
        g.setColor(ArenaPalette.SURFACE_RAISED);
        g.fillRect(0, 0, w, h);
        boolean allTopics = "Tổng hợp".equalsIgnoreCase(topicName);
        boolean records = "Những điều lớn nhất, nhỏ nhất và đầu tiên".equalsIgnoreCase(topicName);
        BufferedImage artwork = allTopics || records ? null : TopicImageManager.getInstance().getTopicImage(topicName, w, h);
        if (artwork != null) {
            double scale = 1 + (Boolean.getBoolean("quiz.ui.reduceMotion") ? 0 : 0.045 * hoverProgress);
            int dw = (int) Math.ceil(w * scale);
            int dh = (int) Math.ceil(h * scale);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(artwork, (w - dw) / 2, (h - dh) / 2, dw, dh, null);
        }
        if (allTopics) {
            g.setPaint(new GradientPaint(0, 0, new Color(30, 65, 69), w, h, new Color(21, 27, 55)));
            g.fillRect(0, 0, w, h);
            g.setColor(new Color(196, 247, 107, 22));
            g.setStroke(new BasicStroke(1f));
            for (int radius : new int[] {35, 58, 82}) {
                g.drawOval(w / 2 - radius, 54 - radius, radius * 2, radius * 2);
            }
            new ArenaIcon(ArenaIcon.Type.BRAND, 46, ArenaPalette.ACCENT).paintIcon(this, g, w / 2 - 23, 25);
        }
        if (records) {
            g.setPaint(new GradientPaint(0, 0, new Color(25, 52, 68), w, h, new Color(25, 26, 55)));
            g.fillRect(0, 0, w, h);
            int center = w / 2;
            g.setColor(new Color(101, 222, 245, 25));
            g.setStroke(new BasicStroke(1f));
            g.drawOval(center - 60, 4, 120, 120);
            g.drawOval(center - 79, -15, 158, 158);
            g.setColor(new Color(78, 175, 185));
            g.fillRoundRect(center - 53, 52, 31, 43, 7, 7);
            g.setColor(ArenaPalette.ACCENT);
            g.fillRoundRect(center - 16, 32, 33, 63, 7, 7);
            g.setColor(new Color(85, 122, 176));
            g.fillRoundRect(center + 23, 65, 31, 30, 7, 7);
            g.setFont(ArenaPalette.font(20, Font.BOLD));
            g.setColor(ArenaPalette.ACCENT_INK);
            g.drawString("1", center - 6, 61);
            g.setColor(new Color(101, 222, 245, 160));
            g.fillOval(center + 58, 22, 5, 5);
            g.setColor(new Color(196, 247, 107, 150));
            g.fillOval(center - 70, 36, 4, 4);
        }
        g.setPaint(new GradientPaint(0, 32, new Color(9, 16, 37, 0),
                0, h, new Color(9, 16, 37, 242)));
        g.fillRect(0, 0, w, h);
        g.setPaint(new GradientPaint(0, 0, new Color(9, 16, 37, 65),
                0, 42, new Color(9, 16, 37, 0)));
        g.fillRect(0, 0, w, 42);
        g.setFont(ArenaPalette.font(14, Font.BOLD));
        FontMetrics fm = g.getFontMetrics();
        List<String> lines = wrapTitle(fm, w - 28);
        int firstBaseline = h - 16 - (lines.size() - 1) * 18;
        g.setColor(Color.WHITE);
        for (int i = 0; i < lines.size(); i++) g.drawString(lines.get(i), 15, firstBaseline + i * 18);
        if (isSelected) {
            g.setColor(ArenaPalette.ACCENT);
            g.fillOval(w - 35, 11, 24, 24);
            new ArenaIcon(ArenaIcon.Type.CHECK, 15, ArenaPalette.ACCENT_INK).paintIcon(this, g, w - 30, 16);
        }
        g.dispose();
        g = (Graphics2D) graphics.create();
        Theme.setupAntiAliasing(g);
        if (isSelected || hasFocus()) {
            g.setColor(isSelected ? ArenaPalette.ACCENT : ArenaPalette.CYAN);
            g.setStroke(new BasicStroke(2.4f));
        } else {
            g.setColor(new Color(116, 147, 187, (int) (70 + 110 * hoverProgress)));
            g.setStroke(new BasicStroke(1f));
        }
        g.draw(new RoundRectangle2D.Float(1.5f, 1.5f, w - 3, h - 3, CORNER_RADIUS, CORNER_RADIUS));
        g.dispose();
    }

    private List<String> wrapTitle(FontMetrics metrics, int width) {
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : topicName.split("\\s+")) {
            String next = current.length() == 0 ? word : current + " " + word;
            if (current.length() > 0 && metrics.stringWidth(next) > width && lines.size() < 2) {
                lines.add(current.toString());
                current = new StringBuilder(word);
            } else current = new StringBuilder(next);
        }
        String last = current.toString();
        while (!last.isEmpty() && metrics.stringWidth(last) > width) last = last.substring(0, last.length() - 1);
        if (!last.equals(current.toString()) && last.length() > 2) last = last.substring(0, last.length() - 2) + "…";
        lines.add(last);
        return lines;
    }

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
