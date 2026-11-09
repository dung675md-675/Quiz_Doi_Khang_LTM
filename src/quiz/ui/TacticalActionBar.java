package quiz.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.awt.geom.RoundRectangle2D;
import java.util.function.Consumer;

/**
 * TacticalActionBar - Thanh kỹ năng chiến thuật tương tác 3 nút trong trận đấu:
 * 1. Tấn Công (2 lượt): Neon Đỏ Cam (#F43F5E / #FB7185). Yêu cầu điểm >= 50.
 * 2. Phòng Thủ (2 lượt): Neon Xanh Ngọc (#06B6D4 / #22D3EE). Luôn bật nếu còn lượt.
 * 3. Ngôi Sao (1 lượt): Neon Vàng Gold (#F59E0B / #FBBF24). Yêu cầu điểm >= 50.
 *
 * Tính năng chính:
 * - Hiệu ứng phát sáng viền Neon động khi được kích hoạt.
 * - Bấm lại nút đang sáng để HỦY (CANCEL).
 * - Bấm nút khác khi đang sáng sẽ tự động đổi sang kỹ năng mới.
 * - Khóa tự động khi đã nộp đáp án / hết giờ / hết số lượt dùng.
 * - Vẽ Vector Graphics cho icon (Kiếm, Khiên, Sao) chống 100% lỗi font trên Windows.
 */
public class TacticalActionBar extends JPanel {

    public enum TacticalSkill {
        ATTACK("ATTACK", "Tấn Công", new Color(0xF4, 0x3F, 0x5E), new Color(0xFB, 0x71, 0x85), 1),
        DEFEND("DEFEND", "Phòng Thủ", new Color(0x06, 0xB6, 0xD4), new Color(0x22, 0xD3, 0xEE), 1),
        STAR("STAR", "Ngôi Sao", new Color(0xF5, 0x9E, 0x0B), new Color(0xFB, 0xBF, 0x24), 1);

        public final String actionCode;
        public final String title;
        public final Color coreColor;
        public final Color glowColor;
        public final int maxUses;

        TacticalSkill(String actionCode, String title, Color coreColor, Color glowColor, int maxUses) {
            this.actionCode = actionCode;
            this.title = title;
            this.coreColor = coreColor;
            this.glowColor = glowColor;
            this.maxUses = maxUses;
        }
    }

    private final TacticalButton btnAttack;
    private final TacticalButton btnDefend;
    private final TacticalButton btnStar;

    private Consumer<String> onActionListener;
    private String activeSkill = "NONE";
    private int currentPoints = 0;
    private boolean questionLocked = false;

    private Timer defendBlinkTimer;
    private boolean defendBlinking = false;
    private boolean blinkCycleState = false;

    public TacticalActionBar() {
        setLayout(new FlowLayout(FlowLayout.CENTER, 14, 2));
        setOpaque(false);
        setPreferredSize(new Dimension(0, 42));
        setMaximumSize(new Dimension(650, 42));

        btnAttack = new TacticalButton(TacticalSkill.ATTACK);
        btnDefend = new TacticalButton(TacticalSkill.DEFEND);
        btnStar = new TacticalButton(TacticalSkill.STAR);

        btnAttack.addActionListener(e -> handleButtonClick(TacticalSkill.ATTACK));
        btnDefend.addActionListener(e -> handleButtonClick(TacticalSkill.DEFEND));
        btnStar.addActionListener(e -> handleButtonClick(TacticalSkill.STAR));

        add(btnAttack);
        add(btnDefend);
        add(btnStar);

        updateButtonStates();
    }

    // ==========================================
    // PUBLIC API FOR MATCHSCREEN
    // ==========================================

    /**
     * Gắn callback khi người chơi bấm nút kỹ năng.
     * Trả về actionCode: "ATTACK", "DEFEND", "STAR" hoặc "CANCEL".
     */
    public void setOnActionListener(Consumer<String> listener) {
        this.onActionListener = listener;
    }

    /**
     * Cập nhật toàn bộ trạng thái kỹ năng và điểm số nhận từ Server.
     */
    public void updateState(int points, int attackUses, int defendUses, int starUses, String serverActiveSkill) {
        this.currentPoints = points;
        String act = (serverActiveSkill != null && !serverActiveSkill.isBlank()) ? serverActiveSkill.toUpperCase() : "NONE";
        if (attackUses <= 0 || "ATTACK".equals(act)) btnAttack.usedInMatch = true;
        if (defendUses <= 0 || "DEFEND".equals(act)) btnDefend.usedInMatch = true;
        if (starUses <= 0 || "STAR".equals(act)) btnStar.usedInMatch = true;
        btnAttack.remainingUses = btnAttack.usedInMatch ? 0 : Math.max(0, attackUses);
        btnDefend.remainingUses = btnDefend.usedInMatch ? 0 : Math.max(0, defendUses);
        btnStar.remainingUses = btnStar.usedInMatch ? 0 : Math.max(0, starUses);
        this.activeSkill = act;
        updateButtonStates();
        repaint();
    }

    /**
     * Đặt kỹ năng đang được kích hoạt ("ATTACK", "DEFEND", "STAR", "NONE").
     */
    public void setActiveSkill(String skill) {
        this.activeSkill = (skill != null && !skill.isBlank()) ? skill.toUpperCase() : "NONE";
        updateButtonStates();
        repaint();
    }

    /**
     * Khóa hoặc mở thanh kỹ năng (khóa khi đã nộp bài, hết giờ hoặc đang xem đáp án).
     */
    public void setQuestionLocked(boolean locked) {
        if (locked) {
            stopDefendBlink();
        }
        this.questionLocked = locked;
        updateButtonStates();
        repaint();
    }

    /**
     * Khởi động câu hỏi mới: xóa viền sáng, mở khóa tạm thời và cập nhật theo điểm số mới.
     */
    public void onNewQuestion(int points) {
        stopDefendBlink();
        this.currentPoints = points;
        this.activeSkill = "NONE";
        this.questionLocked = false;
        updateButtonStates();
        repaint();
    }

    /**
     * Khởi động câu hỏi mới với số lượt còn lại chính xác được đồng bộ từ trận đấu.
     */
    public void onNewQuestion(int points, int atkLeft, int defLeft, int starLeft) {
        stopDefendBlink();
        this.currentPoints = points;
        this.activeSkill = "NONE";
        this.questionLocked = false;
        if (atkLeft <= 0) btnAttack.usedInMatch = true;
        if (defLeft <= 0) btnDefend.usedInMatch = true;
        if (starLeft <= 0) btnStar.usedInMatch = true;
        btnAttack.remainingUses = btnAttack.usedInMatch ? 0 : Math.max(0, atkLeft);
        btnDefend.remainingUses = btnDefend.usedInMatch ? 0 : Math.max(0, defLeft);
        btnStar.remainingUses = btnStar.usedInMatch ? 0 : Math.max(0, starLeft);
        updateButtonStates();
        repaint();
    }

    /**
     * Bật / tắt nhấp nháy nhắc nhở nút Phòng Thủ khi đối thủ phát động TẤN CÔNG.
     * Chỉ nhấp nháy khi nút còn lượt, chưa nộp bài và chưa bấm Phòng Thủ.
     */
    public void setDefendPromptBlinking(boolean enable) {
        if (!enable || btnDefend.remainingUses <= 0 || questionLocked || "DEFEND".equals(activeSkill)) {
            stopDefendBlink();
            return;
        }

        if (defendBlinking) return;
        defendBlinking = true;
        blinkCycleState = true;
        btnDefend.setBlinkingAlert(true);

        if (defendBlinkTimer == null) {
            defendBlinkTimer = new Timer(380, e -> {
                if (!defendBlinking || btnDefend.remainingUses <= 0 || questionLocked || "DEFEND".equals(activeSkill)) {
                    stopDefendBlink();
                    return;
                }
                blinkCycleState = !blinkCycleState;
                btnDefend.setBlinkingAlert(blinkCycleState);
            });
        }
        defendBlinkTimer.start();
    }

    public void stopDefendBlink() {
        defendBlinking = false;
        blinkCycleState = false;
        if (defendBlinkTimer != null && defendBlinkTimer.isRunning()) {
            defendBlinkTimer.stop();
        }
        btnDefend.setBlinkingAlert(false);
    }

    public boolean isDefendBlinking() {
        return defendBlinking;
    }

    /**
     * Cập nhật số lượt còn lại của 3 kỹ năng.
     */
    public void setTurns(int attackUses, int defendUses, int starUses) {
        if (attackUses <= 0) btnAttack.usedInMatch = true;
        if (defendUses <= 0) btnDefend.usedInMatch = true;
        if (starUses <= 0) btnStar.usedInMatch = true;
        btnAttack.remainingUses = btnAttack.usedInMatch ? 0 : Math.max(0, attackUses);
        btnDefend.remainingUses = btnDefend.usedInMatch ? 0 : Math.max(0, defendUses);
        btnStar.remainingUses = btnStar.usedInMatch ? 0 : Math.max(0, starUses);
        updateButtonStates();
        repaint();
    }

    /**
     * Cập nhật điểm số hiện tại của người chơi (dùng để bật/tắt Tấn công và Ngôi sao).
     */
    public void setPoints(int points) {
        this.currentPoints = points;
        updateButtonStates();
        repaint();
    }

    /**
     * Reset toàn bộ thanh về trạng thái đầu trận.
     */
    public void resetBar() {
        stopDefendBlink();
        this.currentPoints = 0;
        this.activeSkill = "NONE";
        this.questionLocked = false;
        btnAttack.usedInMatch = false;
        btnDefend.usedInMatch = false;
        btnStar.usedInMatch = false;
        btnAttack.remainingUses = TacticalSkill.ATTACK.maxUses;
        btnDefend.remainingUses = TacticalSkill.DEFEND.maxUses;
        btnStar.remainingUses = TacticalSkill.STAR.maxUses;
        updateButtonStates();
        repaint();
    }

    public String getActiveSkill() {
        return activeSkill;
    }

    public boolean isQuestionLocked() {
        return questionLocked;
    }

    public int getAttackRemaining() {
        return btnAttack.remainingUses;
    }

    public int getDefendRemaining() {
        return btnDefend.remainingUses;
    }

    public int getStarRemaining() {
        return btnStar.remainingUses;
    }

    public JButton getAttackButton() {
        return btnAttack;
    }

    public JButton getDefendButton() {
        return btnDefend;
    }

    public JButton getStarButton() {
        return btnStar;
    }

    // ==========================================
    // INTERNAL LOGIC & STATE HANDLING
    // ==========================================

    private void handleButtonClick(TacticalSkill skill) {
        if (questionLocked) return;

        // Nếu đã có kỹ năng đang kích hoạt trong câu này thì khóa, không cho đổi hay spam
        if (!"NONE".equals(activeSkill)) return;

        // Kiểm tra điều kiện điểm cho Tấn công và Ngôi sao
        if ((skill == TacticalSkill.ATTACK || skill == TacticalSkill.STAR) && currentPoints < 50) {
            return;
        }

        // Kiểm tra còn lượt dùng không
        TacticalButton btn = getButton(skill);
        if (btn == null || btn.usedInMatch || btn.remainingUses <= 0) {
            return;
        }

        if (skill == TacticalSkill.DEFEND) {
            stopDefendBlink();
        }

        // Tiêu hao ngay lượt dùng thành 0 và tắt nút, mờ ô ngay lập tức
        btn.usedInMatch = true;
        btn.remainingUses = 0;
        btn.setActive(false);
        btn.setEnabled(false);
        btn.updateLabel();
        activeSkill = skill.actionCode;
        updateButtonStates();
        repaint();

        if (onActionListener != null) {
            onActionListener.accept(skill.actionCode);
        }
    }

    private TacticalButton getButton(TacticalSkill skill) {
        return switch (skill) {
            case ATTACK -> btnAttack;
            case DEFEND -> btnDefend;
            case STAR -> btnStar;
        };
    }

    private void updateButtonStates() {
        updateSingleButton(btnAttack, TacticalSkill.ATTACK);
        updateSingleButton(btnDefend, TacticalSkill.DEFEND);
        updateSingleButton(btnStar, TacticalSkill.STAR);
    }

    private void updateSingleButton(TacticalButton btn, TacticalSkill skill) {
        if (btn.usedInMatch) {
            btn.remainingUses = 0;
        }
        boolean isCurrentActive = skill.actionCode.equals(activeSkill);
        boolean isExhausted = btn.usedInMatch || (btn.remainingUses <= 0);
        boolean isPointRestricted = (skill != TacticalSkill.DEFEND) && (currentPoints < 50);

        // Nút chỉ bấm được khi: chưa khóa câu hỏi, CHƯA HẾT LƯỢT DÙNG, đủ điểm, và chưa có chiêu nào đang kích hoạt
        boolean anotherSkillActive = !"NONE".equals(activeSkill) && !isCurrentActive;
        boolean canClick = !questionLocked && !isExhausted && !isPointRestricted && !anotherSkillActive;

        btn.setActive(isCurrentActive && !isExhausted);
        btn.setEnabled(canClick);
        btn.updateLabel();

        // Cập nhật tooltip trực quan
        if (isExhausted) {
            if (isCurrentActive) {
                btn.setToolTipText(skill.title + " đang được kích hoạt cho câu này (Đã hết lượt dùng)");
            } else {
                btn.setToolTipText("Đã hết số lượt dùng kỹ năng này (" + skill.title + ")");
            }
        } else if (anotherSkillActive) {
            btn.setToolTipText("Đã kích hoạt kỹ năng khác cho câu này");
        } else if (questionLocked) {
            btn.setToolTipText("Đã nộp bài hoặc hết giờ câu hỏi");
        } else if (isPointRestricted) {
            btn.setToolTipText("Cần tối thiểu 50 điểm để kích hoạt " + skill.title + " (Điểm hiện tại: " + currentPoints + ")");
        } else {
            String desc = switch (skill) {
                case ATTACK -> "Tấn công: Nếu trả lời đúng, cướp 50 điểm của đối thủ";
                case DEFEND -> "Phòng thủ: Hóa giải đòn tấn công, hoặc phản đòn nếu đối thủ sai";
                case STAR -> "Ngôi sao hy vọng: Trả lời đúng x2 điểm, sai bị trừ 50 điểm";
            };
            btn.setToolTipText(desc + " (Còn " + btn.remainingUses + " lượt)");
        }
    }

    // ==========================================
    // CUSTOM TACTICAL BUTTON COMPONENT
    // ==========================================

    private static class TacticalButton extends JButton {
        final TacticalSkill skill;
        int remainingUses;
        boolean usedInMatch = false;
        boolean isActive = false;
        boolean isHovered = false;
        boolean isBlinkingAlert = false;

        void setBlinkingAlert(boolean b) {
            if (this.isBlinkingAlert != b) {
                this.isBlinkingAlert = b;
                repaint();
            }
        }

        TacticalButton(TacticalSkill skill) {
            this.skill = skill;
            this.remainingUses = skill.maxUses;

            setFont(new Font("Segoe UI", Font.BOLD, 13));
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(168, 38));
            setMaximumSize(new Dimension(185, 38));
            setBorder(new EmptyBorder(0, 0, 0, 0));

            updateLabel();

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (isEnabled()) {
                        isHovered = true;
                        repaint();
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    isHovered = false;
                    repaint();
                }
            });
        }

        void setActive(boolean active) {
            this.isActive = active;
        }

        void updateLabel() {
            int disp = usedInMatch ? 0 : Math.max(0, remainingUses);
            setText(skill.title + " (" + disp + ")");
        }

        @Override
        public void setEnabled(boolean b) {
            super.setEnabled(b);
            setCursor(b ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int cornerRadius = 14;

            boolean enabled = isEnabled();

            if (!enabled || remainingUses <= 0 || usedInMatch) {
                // Trạng thái vô hiệu hóa / hết lượt (0 lượt) -> MỜ Ô HOÀN TOÀN NHƯ Ô BÊN CẠNH
                g2.setColor(new Color(0x0F, 0x17, 0x2A, 180));
                g2.fill(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, cornerRadius, cornerRadius));
                g2.setColor(new Color(255, 255, 255, 25));
                g2.setStroke(new BasicStroke(1.0f));
                g2.draw(new RoundRectangle2D.Float(1.5f, 1.5f, w - 3, h - 3, cornerRadius, cornerRadius));

                // Vẽ icon và text xám mờ
                drawButtonContent(g2, w, h, new Color(0x64, 0x74, 0x8B), new Color(0x47, 0x55, 0x69));
                g2.dispose();
                return;
            }

            if (isActive) {
                // TRẠNG THÁI ACTIVE TRONG CÂU HIỆN TẠI (NẾU CÒN LƯỢT DÙNG)
                // 1. Lớp hào quang Neon bên ngoài (Outer Glow)
                Color glow = skill.glowColor;
                Color core = skill.coreColor;

                g2.setColor(new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), 32));
                g2.setStroke(new BasicStroke(5.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(new RoundRectangle2D.Float(3.5f, 3.5f, w - 7, h - 7, cornerRadius, cornerRadius));

                // 2. Lớp hào quang Neon tầm trung (Mid Glow)
                g2.setColor(new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), 85));
                g2.setStroke(new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(new RoundRectangle2D.Float(2.5f, 2.5f, w - 5, h - 5, cornerRadius, cornerRadius));

                // 3. Nền đổ bóng Gradient Neon tinh tế
                g2.setPaint(new GradientPaint(
                    0, 0, new Color(core.getRed(), core.getGreen(), core.getBlue(), 55),
                    0, h, new Color(0x0F, 0x17, 0x2A, 220)
                ));
                g2.fill(new RoundRectangle2D.Float(1.5f, 1.5f, w - 3, h - 3, cornerRadius, cornerRadius));

                // 4. Viền Core Neon sắc nét
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(new RoundRectangle2D.Float(1.5f, 1.5f, w - 3, h - 3, cornerRadius, cornerRadius));

                // 5. Nội dung nút
                drawButtonContent(g2, w, h, Color.WHITE, core);
                g2.dispose();
                return;
            }

            if (isBlinkingAlert) {
                // TRẠNG THÁI NHẮP NHÁY NHẮC NHỞ KHI ĐỐI THỦ TẤN CÔNG
                Color glow = skill.glowColor;
                Color core = skill.coreColor;

                // 1. Hào quang cảnh báo Cyan rực sáng
                g2.setColor(new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), 80));
                g2.setStroke(new BasicStroke(4.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(new RoundRectangle2D.Float(2.5f, 2.5f, w - 5, h - 5, cornerRadius, cornerRadius));

                // 2. Nền nổi bật sáng
                g2.setPaint(new GradientPaint(
                    0, 0, new Color(core.getRed(), core.getGreen(), core.getBlue(), 95),
                    0, h, new Color(0x16, 0x4E, 0x63, 240)
                ));
                g2.fill(new RoundRectangle2D.Float(1.5f, 1.5f, w - 3, h - 3, cornerRadius, cornerRadius));

                // 3. Viền cảnh báo trắng sáng
                g2.setColor(new Color(255, 255, 255, 240));
                g2.setStroke(new BasicStroke(1.8f));
                g2.draw(new RoundRectangle2D.Float(1.5f, 1.5f, w - 3, h - 3, cornerRadius, cornerRadius));

                drawButtonContent(g2, w, h, Color.WHITE, skill.glowColor);
            } else {
                // TRẠNG THÁI BÌNH THƯỜNG / HOVER
                if (isHovered) {
                    g2.setPaint(new GradientPaint(
                        0, 0, new Color(0x27, 0x35, 0x4E),
                        0, h, new Color(0x1A, 0x24, 0x3B)
                    ));
                } else {
                    g2.setColor(new Color(0x19, 0x23, 0x39, 210));
                }
                g2.fill(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, cornerRadius, cornerRadius));

                // Viền nút
                if (isHovered) {
                    Color borderCol = skill.coreColor;
                    g2.setColor(new Color(borderCol.getRed(), borderCol.getGreen(), borderCol.getBlue(), 160));
                    g2.setStroke(new BasicStroke(1.4f));
                } else {
                    g2.setColor(new Color(255, 255, 255, 45));
                    g2.setStroke(new BasicStroke(1.0f));
                }
                g2.draw(new RoundRectangle2D.Float(1.5f, 1.5f, w - 3, h - 3, cornerRadius, cornerRadius));

                // Nội dung
                Color textCol = isHovered ? Color.WHITE : new Color(0xE2, 0xE8, 0xF0);
                drawButtonContent(g2, w, h, textCol, skill.coreColor);
            }

            g2.dispose();
        }

        private void drawButtonContent(Graphics2D g2, int w, int h, Color textColor, Color iconColor) {
            FontMetrics fm = g2.getFontMetrics(getFont());
            String text = getText();
            int textW = fm.stringWidth(text);
            int iconSize = 16;
            int gap = 8;
            int totalContentW = iconSize + gap + textW;

            int startX = (w - totalContentW) / 2;
            int iconY = (h - iconSize) / 2;

            // 1. Vẽ Vector Icon sắc nét (Không sợ lỗi font Windows)
            drawVectorIcon(g2, startX, iconY, iconSize, iconColor);

            // 2. Vẽ Chữ
            int textX = startX + iconSize + gap;
            int textY = (h - fm.getHeight()) / 2 + fm.getAscent();

            g2.setFont(getFont());
            g2.setColor(textColor);
            g2.drawString(text, textX, textY);
        }

        private void drawVectorIcon(Graphics2D g2, int x, int y, int size, Color color) {
            Graphics2D g = (Graphics2D) g2.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(color);

            int cx = x + size / 2;
            int cy = y + size / 2;

            switch (skill) {
                case ATTACK -> {
                    // Vector 2 thanh kiếm đan chéo (Crossed Swords)
                    g.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.drawLine(cx - 6, cy - 6, cx + 6, cy + 6);
                    g.drawLine(cx + 6, cy - 6, cx - 6, cy + 6);
                    // Chuôi kiếm
                    g.setStroke(new BasicStroke(1.5f));
                    g.drawLine(cx - 5, cy - 2, cx - 2, cy - 5);
                    g.drawLine(cx + 5, cy - 2, cx + 2, cy - 5);
                }
                case DEFEND -> {
                    // Vector chiếc khiên bảo vệ (Shield)
                    GeneralPath shield = new GeneralPath();
                    shield.moveTo(cx - 6, cy - 6);
                    shield.lineTo(cx + 6, cy - 6);
                    shield.lineTo(cx + 6, cy);
                    shield.curveTo(cx + 5, cy + 4, cx + 2, cy + 7, cx, cy + 8);
                    shield.curveTo(cx - 2, cy + 7, cx - 5, cy + 4, cx - 6, cy);
                    shield.closePath();

                    g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 80));
                    g.fill(shield);
                    g.setColor(color);
                    g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.draw(shield);

                    // Biểu tượng thập tự bên trong khiên
                    g.setStroke(new BasicStroke(1.3f));
                    g.drawLine(cx, cy - 3, cx, cy + 4);
                    g.drawLine(cx - 3, cy, cx + 3, cy);
                }
                case STAR -> {
                    // Vector Ngôi sao 5 cánh (5-pointed Star)
                    GeneralPath star = new GeneralPath();
                    int points = 5;
                    double outerR = size * 0.48;
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
                    g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.draw(star);
                }
            }
            g.dispose();
        }
    }
}
