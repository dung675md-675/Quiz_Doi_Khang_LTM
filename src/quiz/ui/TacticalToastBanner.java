package quiz.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.GeneralPath;

/**
 * TacticalToastBanner - Dải banner mỏng (~26px) thông báo kỹ năng chiến thuật của đối thủ:
 * - Trượt nhẹ xuống ngay dưới thanh Header đối đầu khi đối thủ dùng kỹ năng.
 * - Tự ẩn sau ~2.5 giây hoặc khi sang câu hỏi mới.
 * - Ẩn tức thời khi đối thủ CANCEL.
 * - Vẽ Vector Graphics cho icon (Kiếm, Khiên, Sao) chống 100% lỗi font trên Windows.
 * - Animation mượt mà bằng javax.swing.Timer (non-blocking EDT).
 */
public class TacticalToastBanner extends JPanel {

    public static final int TARGET_HEIGHT = 26;

    private final Timer slideTimer;
    private final Timer autoHideTimer;

    private int currentHeight = 0;
    private float animProgress = 0.0f; // 0.0 -> 1.0
    private boolean isSlidingIn = false;
    private boolean isSlidingOut = false;

    private String opponentName = "";
    private String currentAction = "NONE";
    private String messageText = "";

    private Color bgStart = new Color(0x7F, 0x1D, 0x1D, 230);
    private Color bgEnd = new Color(0x45, 0x0A, 0x0A, 240);
    private Color borderCol = new Color(0xF4, 0x3F, 0x5E);
    private Color iconCol = new Color(0xFB, 0x71, 0x85);

    public TacticalToastBanner() {
        setOpaque(false);
        setPreferredSize(new Dimension(0, 0));
        setMinimumSize(new Dimension(0, 0));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, TARGET_HEIGHT));
        setVisible(false);

        // Auto Hide Timer (~2.5 giây)
        autoHideTimer = new Timer(2500, e -> startSlideOut());
        autoHideTimer.setRepeats(false);

        // Slide Animation Timer (60 FPS ~16ms)
        slideTimer = new Timer(16, e -> updateSlideAnimation());
    }

    /**
     * Hiển thị banner tương ứng với action của đối thủ:
     * - ATTACK: "⚔️ [ĐỐI THỦ] <tên> vừa phát động TẤN CÔNG!"
     * - DEFEND: "🛡️ [ĐỐI THỦ] <tên> đã kích hoạt KHIÊN PHÒNG THỦ!"
     * - STAR: "⭐ [ĐỐI THỦ] <tên> đã đặt cược NGÔI SAO HY VỌNG!"
     * - CANCEL: Ẩn ngay banner.
     */
    public void showOpponentAction(String oppName, String action) {
        if (action == null || action.isBlank() || "CANCEL".equalsIgnoreCase(action) || "NONE".equalsIgnoreCase(action)) {
            hideToast();
            return;
        }

        this.opponentName = (oppName != null && !oppName.isBlank()) ? oppName : "Đối thủ";
        this.currentAction = action.toUpperCase();

        switch (this.currentAction) {
            case "ATTACK" -> {
                this.messageText = "[ĐỐI THỦ] " + this.opponentName + " vừa phát động TẤN CÔNG!";
                this.bgStart = new Color(0x7F, 0x1D, 0x1D, 235);
                this.bgEnd = new Color(0x45, 0x0A, 0x0A, 245);
                this.borderCol = new Color(0xF4, 0x3F, 0x5E);
                this.iconCol = new Color(0xFB, 0x71, 0x85);
            }
            case "DEFEND" -> {
                this.messageText = "[ĐỐI THỦ] " + this.opponentName + " đã kích hoạt KHIÊN PHÒNG THỦ!";
                this.bgStart = new Color(0x16, 0x4E, 0x63, 235);
                this.bgEnd = new Color(0x08, 0x33, 0x44, 245);
                this.borderCol = new Color(0x06, 0xB6, 0xD4);
                this.iconCol = new Color(0x22, 0xD3, 0xEE);
            }
            case "STAR" -> {
                this.messageText = "[ĐỐI THỦ] " + this.opponentName + " đã đặt cược NGÔI SAO HY VỌNG!";
                this.bgStart = new Color(0x78, 0x35, 0x0F, 235);
                this.bgEnd = new Color(0x45, 0x1A, 0x03, 245);
                this.borderCol = new Color(0xF5, 0x9E, 0x0B);
                this.iconCol = new Color(0xFB, 0xBF, 0x24);
            }
            default -> {
                hideToast();
                return;
            }
        }

        autoHideTimer.stop();
        setVisible(true);
        isSlidingIn = true;
        isSlidingOut = false;
        slideTimer.start();
    }

    /**
     * Hiển thị kết quả đối đầu chiến thuật ở pha Mở đáp án (Reveal):
     * Phân tích kết quả của bản thân và đối thủ để đưa ra thông báo phù hợp nhất.
     */
    public void showCombatOutcome(String mySkill, String oppSkill, String myOutcome, String oppOutcome,
                                  int myDelta, int oppDelta, String oppName) {
        String opp = (oppName != null && !oppName.isBlank()) ? oppName : "Đối thủ";
        String action = "NONE";
        String msg = "";
        Color start = null, end = null, border = null, icon = null;

        String mo = (myOutcome != null) ? myOutcome.toUpperCase() : "NONE";
        String oo = (oppOutcome != null) ? oppOutcome.toUpperCase() : "NONE";

        // 1. Kết quả liên quan đến Ngôi sao của bản thân
        if ("STAR_SUCCESS".equals(mo)) {
            action = "STAR";
            int pts = (myDelta > 0) ? myDelta : 200;
            msg = "NGÔI SAO HY VỌNG: Nhân đôi điểm số thành công (+" + pts + "đ)!";
            start = new Color(0x78, 0x35, 0x0F, 240);
            end = new Color(0x45, 0x1A, 0x03, 245);
            border = new Color(0xF5, 0x9E, 0x0B);
            icon = new Color(0xFB, 0xBF, 0x24);
        } else if ("STAR_FAIL".equals(mo)) {
            action = "STAR";
            msg = "NGÔI SAO THẤT BẠI: Bạn trả lời sai, bị phạt trừ -50đ!";
            start = new Color(0x7F, 0x1D, 0x1D, 240);
            end = new Color(0x45, 0x0A, 0x0A, 245);
            border = new Color(0xEF, 0x44, 0x44);
            icon = new Color(0xF8, 0x71, 0x71);
        }
        // 2. Kết quả đối kháng Tấn công / Phòng thủ của bản thân
        else if ("STEAL_SUCCESS".equals(mo)) {
            action = "ATTACK";
            int pts = Math.abs(myDelta > 0 ? myDelta : 50);
            msg = "CƯỚP ĐIỂM THÀNH CÔNG: Bạn đã cướp +" + pts + "đ từ đối thủ!";
            start = new Color(0x7F, 0x1D, 0x1D, 240);
            end = new Color(0x45, 0x0A, 0x0A, 245);
            border = new Color(0xF4, 0x3F, 0x5E);
            icon = new Color(0xFB, 0x71, 0x85);
        } else if ("COUNTER_SUCCESS".equals(mo)) {
            action = "DEFEND";
            int pts = Math.abs(myDelta > 0 ? myDelta : 50);
            msg = "PHẢN ĐÒN XUẤT SẮC: Bạn đã cướp ngược +" + pts + "đ từ đối thủ!";
            start = new Color(0x58, 0x1C, 0x87, 240);
            end = new Color(0x3B, 0x07, 0x64, 245);
            border = new Color(0xA8, 0x55, 0xF7);
            icon = new Color(0xC0, 0x84, 0xFC);
        } else if ("BLOCK_SUCCESS".equals(mo)) {
            action = "DEFEND";
            msg = "HÓA GIẢI THÀNH CÔNG: Khiên của bạn đã chặn đứng đòn tấn công!";
            start = new Color(0x06, 0x4E, 0x3B, 240);
            end = new Color(0x02, 0x2C, 0x22, 245);
            border = new Color(0x10, 0xB9, 0x81);
            icon = new Color(0x34, 0xD3, 0x99);
        } else if ("COUNTERED".equals(mo)) {
            action = "ATTACK";
            int pts = Math.abs(myDelta < 0 ? myDelta : 50);
            msg = "BỊ PHẢN ĐÒN: Bạn tấn công sai, đối thủ cướp ngược -" + pts + "đ!";
            start = new Color(0x4C, 0x05, 0x19, 240);
            end = new Color(0x1F, 0x03, 0x0B, 245);
            border = new Color(0xF4, 0x3F, 0x5E);
            icon = new Color(0xFB, 0x71, 0x85);
        } else if ("BLOCKED".equals(mo)) {
            action = "ATTACK";
            msg = "BỊ HÓA GIẢI: Đòn tấn công của bạn bị Khiên đối thủ chặn đứng!";
            start = new Color(0x16, 0x4E, 0x63, 240);
            end = new Color(0x08, 0x33, 0x44, 245);
            border = new Color(0x06, 0xB6, 0xD4);
            icon = new Color(0x22, 0xD3, 0xEE);
        } else if ("BEING_STOLEN".equals(mo)) {
            action = "ATTACK";
            int pts = Math.abs(myDelta < 0 ? myDelta : 50);
            msg = "BỊ TẤN CÔNG: Đối thủ trả lời đúng và cướp -" + pts + "đ của bạn!";
            start = new Color(0x7F, 0x1D, 0x1D, 240);
            end = new Color(0x45, 0x0A, 0x0A, 245);
            border = new Color(0xF4, 0x3F, 0x5E);
            icon = new Color(0xFB, 0x71, 0x85);
        } else if ("ATTACK_FAIL".equals(mo)) {
            action = "ATTACK";
            msg = "TẤN CÔNG THẤT BẠI: Bạn trả lời sai câu này, không cướp được điểm!";
            start = new Color(0x45, 0x0A, 0x0A, 240);
            end = new Color(0x1C, 0x04, 0x04, 245);
            border = new Color(0xE1, 0x1D, 0x48);
            icon = new Color(0xFB, 0x71, 0x85);
        } else if ("DEFEND_MISSED".equals(mo)) {
            action = "DEFEND";
            msg = "PHÒNG THỦ: Đối thủ không tấn công ở câu này (không bị mất điểm).";
            start = new Color(0x16, 0x4E, 0x63, 240);
            end = new Color(0x08, 0x33, 0x44, 245);
            border = new Color(0x06, 0xB6, 0xD4);
            icon = new Color(0x22, 0xD3, 0xEE);
        }
        // 3. Nếu bản thân không dùng kỹ năng, nhưng đối thủ dùng
        else if ("STAR_SUCCESS".equals(oo)) {
            action = "STAR";
            int pts = (oppDelta > 0) ? oppDelta : 200;
            msg = "[ĐỐI THỦ] " + opp + " dùng Ngôi Sao thành công (+" + pts + "đ)!";
            start = new Color(0x78, 0x35, 0x0F, 240);
            end = new Color(0x45, 0x1A, 0x03, 245);
            border = new Color(0xF5, 0x9E, 0x0B);
            icon = new Color(0xFB, 0xBF, 0x24);
        } else if ("STAR_FAIL".equals(oo)) {
            action = "STAR";
            msg = "[ĐỐI THỦ] " + opp + " dùng Ngôi Sao thất bại, bị phạt trừ -50đ!";
            start = new Color(0x7F, 0x1D, 0x1D, 240);
            end = new Color(0x45, 0x0A, 0x0A, 245);
            border = new Color(0xEF, 0x44, 0x44);
            icon = new Color(0xF8, 0x71, 0x71);
        } else if ("DEFEND_MISSED".equals(oo)) {
            action = "DEFEND";
            msg = "[ĐỐI THỦ] " + opp + " kích hoạt Khiên nhưng bạn không tấn công.";
            start = new Color(0x16, 0x4E, 0x63, 240);
            end = new Color(0x08, 0x33, 0x44, 245);
            border = new Color(0x06, 0xB6, 0xD4);
            icon = new Color(0x22, 0xD3, 0xEE);
        }

        if ("NONE".equals(action) || msg.isEmpty()) {
            hideToast();
            return;
        }

        this.currentAction = action;
        this.messageText = msg;
        this.bgStart = start;
        this.bgEnd = end;
        this.borderCol = border;
        this.iconCol = icon;

        autoHideTimer.stop();
        setVisible(true);
        isSlidingIn = true;
        isSlidingOut = false;
        slideTimer.start();
    }

    /**
     * Ẩn banner ngay lập tức (dùng khi CANCEL, kết thúc câu hoặc sang câu mới)
     */
    public void hideToast() {
        if (autoHideTimer.isRunning()) autoHideTimer.stop();
        if (slideTimer.isRunning()) slideTimer.stop();
        isSlidingIn = false;
        isSlidingOut = false;
        animProgress = 0.0f;
        currentHeight = 0;
        this.currentAction = "NONE";
        this.messageText = "";
        setVisible(false);
        setPreferredSize(new Dimension(0, 0));
        revalidateHierarchy();
        repaint();
    }

    private void startSlideOut() {
        if (!isVisible()) return;
        isSlidingIn = false;
        isSlidingOut = true;
        slideTimer.start();
    }

    private void updateSlideAnimation() {
        if (isSlidingIn) {
            animProgress += 0.12f;
            if (animProgress >= 1.0f) {
                animProgress = 1.0f;
                currentHeight = TARGET_HEIGHT;
                slideTimer.stop();
                isSlidingIn = false;
                autoHideTimer.restart(); // Chờ ~2.5s rồi tự ẩn
            } else {
                // Cubic ease-out
                float t = animProgress - 1.0f;
                float ease = t * t * t + 1.0f;
                currentHeight = Math.max(1, (int) (TARGET_HEIGHT * ease));
            }
        } else if (isSlidingOut) {
            animProgress -= 0.12f;
            if (animProgress <= 0.0f) {
                animProgress = 0.0f;
                currentHeight = 0;
                slideTimer.stop();
                isSlidingOut = false;
                setVisible(false);
            } else {
                currentHeight = Math.max(0, (int) (TARGET_HEIGHT * animProgress));
            }
        }

        setPreferredSize(new Dimension(0, currentHeight));
        revalidateHierarchy();
        repaint();
    }

    private void revalidateHierarchy() {
        Container parent = getParent();
        if (parent != null) {
            parent.revalidate();
        } else {
            revalidate();
        }
    }

    public boolean isBannerVisible() {
        return isVisible() && currentHeight > 0;
    }

    public String getCurrentAction() {
        return currentAction;
    }

    public String getMessageText() {
        return messageText;
    }

    public int getCurrentHeight() {
        return currentHeight;
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (currentHeight <= 2 || !isVisible()) return;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = currentHeight;

        // Clip theo độ cao hiện tại của animation
        g2.setClip(0, 0, w, h);

        // 1. Nền Gradient tinh tế theo màu của kỹ năng
        g2.setPaint(new GradientPaint(0, 0, bgStart, w, 0, bgEnd));
        g2.fillRect(0, 0, w, h);

        // 2. Viền Neon mỏng ở trên và dưới
        g2.setColor(borderCol);
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawLine(0, h - 1, w, h - 1);
        g2.setColor(new Color(borderCol.getRed(), borderCol.getGreen(), borderCol.getBlue(), 80));
        g2.drawLine(0, 0, w, 0);

        // 3. Nội dung: Vector Icon + Text
        if (!messageText.isEmpty()) {
            Font font = new Font("Segoe UI", Font.BOLD, 12);
            g2.setFont(font);
            FontMetrics fm = g2.getFontMetrics(font);

            int iconSize = 14;
            int gap = 8;
            int textW = fm.stringWidth(messageText);
            int totalContentW = iconSize + gap + textW;

            int startX = Math.max(10, (w - totalContentW) / 2);
            int centerY = h / 2;
            int iconY = centerY - iconSize / 2;

            // Vẽ Vector Icon sắc nét (không sợ lỗi font ô vuông Windows)
            drawVectorIcon(g2, startX, iconY, iconSize, iconCol);

            // Vẽ Text
            int textX = startX + iconSize + gap;
            int textY = centerY + (fm.getAscent() - fm.getDescent()) / 2;

            g2.setColor(Color.WHITE);
            g2.drawString(messageText, textX, textY);
        }

        g2.dispose();
    }

    private void drawVectorIcon(Graphics2D g2, int x, int y, int size, Color color) {
        Graphics2D g = (Graphics2D) g2.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(color);

        int cx = x + size / 2;
        int cy = y + size / 2;

        switch (currentAction) {
            case "ATTACK" -> {
                // Hai thanh kiếm chéo (Crossed Swords)
                g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawLine(cx - 5, cy - 5, cx + 5, cy + 5);
                g.drawLine(cx + 5, cy - 5, cx - 5, cy + 5);
                g.setStroke(new BasicStroke(1.3f));
                g.drawLine(cx - 4, cy - 2, cx - 2, cy - 4);
                g.drawLine(cx + 4, cy - 2, cx + 2, cy - 4);
            }
            case "DEFEND" -> {
                // Chiếc khiên (Shield)
                GeneralPath shield = new GeneralPath();
                shield.moveTo(cx - 5, cy - 5);
                shield.lineTo(cx + 5, cy - 5);
                shield.lineTo(cx + 5, cy);
                shield.curveTo(cx + 4, cy + 4, cx + 2, cy + 6, cx, cy + 7);
                shield.curveTo(cx - 2, cy + 6, cx - 4, cy + 4, cx - 5, cy);
                shield.closePath();

                g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 90));
                g.fill(shield);
                g.setColor(color);
                g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(shield);

                g.setStroke(new BasicStroke(1.2f));
                g.drawLine(cx, cy - 3, cx, cy + 3);
                g.drawLine(cx - 3, cy, cx + 3, cy);
            }
            case "STAR" -> {
                // Ngôi sao 5 cánh (Star)
                GeneralPath star = new GeneralPath();
                int points = 5;
                double outerR = size * 0.46;
                double innerR = outerR * 0.45;

                for (int i = 0; i < points * 2; i++) {
                    double angle = i * Math.PI / points - Math.PI / 2;
                    double r = (i % 2 == 0) ? outerR : innerR;
                    double px = cx + Math.cos(angle) * r;
                    double py = cy + Math.sin(angle) * r;
                    if (i == 0) star.moveTo(px, py);
                    else star.lineTo(px, py);
                }
                star.closePath();

                g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 120));
                g.fill(star);
                g.setColor(color);
                g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(star);
            }
        }
        g.dispose();
    }
}
