package quiz.ui;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * DragDropPanel — Panel chuyên biệt cho câu hỏi dạng DRAG_AND_DROP.
 *
 * Tính năng chính:
 * 1. Câu hỏi dạng text với các chỗ trống dạng ____, độ dài vừa đủ để chứa đáp án dài nhất.
 * 2. Các đáp án đặt ở khu vực bên dưới câu hỏi (vị trí tương đương 4 lựa chọn A-B-C-D).
 * 3. Mỗi đáp án là chip hình chữ nhật bo tròn góc nổi khối 3D (hiệu ứng bóng đổ, viền bevel, highlight).
 * 4. Kéo chuột để gắn đáp án vào ô trống:
 *    - Khi gắn vào ô: CHỈ hiện text, mất khung hình chữ nhật phía sau (không nền khung).
 *    - Khi kéo chip ra khỏi ô trống: chip quay về khu vực đáp án ban đầu.
 *    - Khi kéo chip mới lên ô đã có chip: chip mới thay thế, chip cũ quay về khu vực ban đầu.
 * 5. Hỗ trợ thao tác click nhanh:
 *    - Click vào chip dưới pool -> tự động bay vào ô trống đầu tiên chưa điền.
 *    - Click vào ô trống đã điền -> gỡ text ra, trả chip về pool.
 * 6. Kiểm tra vị trí chính xác của từng chỗ trống, hỗ trợ đáp án thừa (distractor).
 */
public class DragDropPanel extends JPanel {

    private final MatchQuestion question;
    private final JPanel sentencePanel;
    private final JPanel chipPoolPanel;

    // Mapping: blankId -> BlankSlot
    private final Map<String, BlankSlot> slotMap = new LinkedHashMap<>();
    private final List<AnswerChip> allChips = new ArrayList<>();

    // Listener thông báo khi trạng thái trả lời thay đổi
    private Runnable onStateChangeListener = null;

    public DragDropPanel(MatchQuestion q) {
        this.question = q;
        setLayout(new BorderLayout(0, 16));
        setOpaque(false);

        // ========================================================
        // 1. TÍNH TOÁN ĐỘ RỘNG Ô TRỐNG (Độ dài vừa đủ chứa đáp án dài nhất)
        // ========================================================
        int maxCharLen = 4;
        String longestStr = "____";
        if (q.fillMap() != null) {
            for (String val : q.fillMap().values()) {
                if (val != null && val.length() > maxCharLen) {
                    maxCharLen = val.length();
                    longestStr = val;
                }
            }
        }
        if (q.choices() != null) {
            for (String c : q.choices()) {
                if (c != null && !c.trim().isEmpty() && !c.equals("—") && c.trim().length() > maxCharLen) {
                    maxCharLen = c.trim().length();
                    longestStr = c.trim();
                }
            }
        }

        FontMetrics fm = getFontMetrics(Theme.BODY_BOLD);
        int slotWidth = Math.max(88, fm.stringWidth(longestStr) + 28);
        int slotHeight = Math.max(38, fm.getHeight() + 12);

        // ========================================================
        // 2. KHỞI TẠO CÁC Ô TRỐNG (BlankSlots)
        // ========================================================
        if (q.blanks() != null) {
            for (String blankId : q.blanks()) {
                BlankSlot slot = new BlankSlot(blankId, slotWidth, slotHeight);
                slotMap.put(blankId, slot);
            }
        }

        // ========================================================
        // 3. TẠO SENTENCE PANEL (Hiển thị câu hỏi kèm ô trống ____ inline)
        // ========================================================
        sentencePanel = new JPanel();
        sentencePanel.setLayout(new FlowLayout(FlowLayout.CENTER, 8, 10));
        sentencePanel.setOpaque(false);
        sentencePanel.setBorder(new EmptyBorder(6, 12, 6, 12));

        String text = q.text();
        List<BlankSlot> slotList = new ArrayList<>(slotMap.values());

        // Kiểm tra xem câu hỏi có chứa ký tự chỗ trống "____" không
        if (text != null && text.matches(".*_{2,}.*")) {
            // Tách câu thành các phân đoạn theo chỗ trống ____
            String[] parts = text.split("_{2,}", -1);
            int slotIdx = 0;
            for (int i = 0; i < parts.length; i++) {
                String part = parts[i].trim();
                if (!part.isEmpty()) {
                    JLabel lblPart = new JLabel(part);
                    lblPart.setFont(Theme.TITLE);
                    lblPart.setForeground(Theme.WHITE);
                    sentencePanel.add(lblPart);
                }
                if (i < parts.length - 1 && slotIdx < slotList.size()) {
                    sentencePanel.add(slotList.get(slotIdx++));
                }
            }
        } else {
            // Nếu câu hỏi không chứa "____", hiển thị text câu hỏi ở trên và các ô trống ở dưới
            JPanel compound = new JPanel(new BorderLayout(0, 10));
            compound.setOpaque(false);

            JTextArea txtArea = new JTextArea(text != null ? text : "");
            txtArea.setFont(Theme.TITLE);
            txtArea.setForeground(Theme.WHITE);
            txtArea.setBackground(Theme.NAVY_CARD);
            txtArea.setEditable(false);
            txtArea.setFocusable(false);
            txtArea.setLineWrap(true);
            txtArea.setWrapStyleWord(true);
            txtArea.setBorder(new EmptyBorder(4, 8, 4, 8));
            compound.add(txtArea, BorderLayout.NORTH);

            JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 6));
            row.setOpaque(false);
            int idx = 1;
            for (BlankSlot sl : slotList) {
                JLabel lblNum = new JLabel("(" + idx++ + ")");
                lblNum.setFont(Theme.SUBTITLE);
                lblNum.setForeground(Theme.BLUE);
                row.add(lblNum);
                row.add(sl);
            }
            compound.add(row, BorderLayout.CENTER);
            sentencePanel.setLayout(new BorderLayout());
            sentencePanel.add(compound, BorderLayout.CENTER);
        }

        // ========================================================
        // 4. TẠO KHU VỰC ĐÁP ÁN (Bottom Chip Pool)
        // ========================================================
        chipPoolPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 12));
        chipPoolPanel.setOpaque(false);
        chipPoolPanel.setPreferredSize(new Dimension(800, 160));
        chipPoolPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(255, 255, 255, 25)),
            new EmptyBorder(12, 16, 12, 16)
        ));

        // Tập hợp tất cả các chip (đáp án đúng + đáp án thừa)
        Set<String> allAnswers = new LinkedHashSet<>();
        if (q.fillMap() != null) allAnswers.addAll(q.fillMap().values());
        if (q.choices() != null) {
            for (String c : q.choices()) {
                if (c != null && !c.trim().isEmpty() && !c.equals("—")) {
                    allAnswers.add(c.trim());
                }
            }
        }

        // Shuffle thứ tự các chip đáp án để tăng tính thử thách
        List<String> shuffled = new ArrayList<>(allAnswers);
        Collections.shuffle(shuffled);

        for (String ans : shuffled) {
            AnswerChip chip = new AnswerChip(ans);
            allChips.add(chip);
            chipPoolPanel.add(chip);
        }

        // Đăng ký drop target cho chip pool để chip quay về khi kéo thả ra ngoài
        new DropTarget(chipPoolPanel, DnDConstants.ACTION_MOVE, new PoolDropHandler());

        // Lắp ráp mặc định nếu panel được sử dụng độc lập
        add(sentencePanel, BorderLayout.CENTER);
        add(chipPoolPanel, BorderLayout.SOUTH);
    }

    public JPanel getSentencePanel() {
        return sentencePanel;
    }

    public JPanel getChipPoolPanel() {
        return chipPoolPanel;
    }

    public void setOnStateChangeListener(Runnable listener) {
        this.onStateChangeListener = listener;
    }

    private void notifyStateChanged() {
        if (onStateChangeListener != null) {
            onStateChangeListener.run();
        }
    }

    /** Trả về true nếu tất cả các ô trống đã được điền đúng */
    public boolean isAllCorrect() {
        if (question.fillMap() == null || question.fillMap().isEmpty()) return false;
        Map<String, String> userMap = getUserAnswerMap();
        for (Map.Entry<String, String> entry : question.fillMap().entrySet()) {
            String userAns = userMap.get(entry.getKey());
            if (userAns == null || !userAns.trim().equalsIgnoreCase(entry.getValue().trim())) {
                return false;
            }
        }
        return true;
    }

    /** Trả về true nếu người chơi đã điền ít nhất 1 ô trống */
    public boolean hasAnyAnswer() {
        for (BlankSlot slot : slotMap.values()) {
            if (slot.getFilledText() != null && !slot.getFilledText().isBlank()) {
                return true;
            }
        }
        return false;
    }

    /** Map kết quả hiện tại: blankId -> text người dùng đã điền */
    public Map<String, String> getUserAnswerMap() {
        Map<String, String> map = new LinkedHashMap<>();
        for (Map.Entry<String, BlankSlot> entry : slotMap.entrySet()) {
            String text = entry.getValue().getFilledText();
            map.put(entry.getKey(), text != null ? text : "");
        }
        return map;
    }

    /** Cập nhật hiệu ứng hoạt họa xuất hiện (staggered entrance) cho các chip đáp án */
    public void setChipsAnimState(float alpha, int offsetY) {
        for (AnswerChip chip : allChips) {
            chip.setAnimState(alpha, offsetY);
        }
    }

    /** Bật/tắt khả năng tương tác cho toàn bộ chip */
    public void setInteractive(boolean enabled) {
        for (AnswerChip chip : allChips) {
            chip.setEnabled(enabled);
        }
        for (BlankSlot slot : slotMap.values()) {
            slot.setEnabled(enabled);
        }
    }

    // =======================================================
    // BlankSlot — Ô trống nhận đáp án kéo thả
    // =======================================================
    public class BlankSlot extends JPanel {
        private final String blankId;
        private String filledText = null;
        private final int slotWidth;
        private final int slotHeight;
        private boolean hovering = false;

        BlankSlot(String id, int width, int height) {
            this.blankId = id;
            this.slotWidth = width;
            this.slotHeight = height;
            setOpaque(false);
            setPreferredSize(new Dimension(slotWidth, slotHeight));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            // Đăng ký nhận drop kéo thả
            new DropTarget(this, DnDConstants.ACTION_MOVE, new SlotDropHandler(this));

            // Đăng ký kéo từ slot (để dời hoặc gỡ chip ra)
            DragSource.getDefaultDragSource().createDefaultDragGestureRecognizer(
                this, DnDConstants.ACTION_MOVE, new SlotDragGestureListener(this)
            );

            // Click trực tiếp vào ô đã điền -> gỡ đáp án trả về pool
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hovering = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hovering = false;
                    repaint();
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    if (filledText != null) {
                        String old = clear();
                        returnChipToPool(old);
                        SoundManager.playPop();
                        notifyStateChanged();
                    }
                }
            });
        }

        public String getFilledText() {
            return filledText;
        }

        public void fill(String text) {
            this.filledText = text;
            repaint();
        }

        public String clear() {
            String old = filledText;
            filledText = null;
            repaint();
            return old;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.setupAntiAliasing(g2);
            int w = getWidth(), h = getHeight();

            if (filledText != null) {
                // YÊU CẦU ĐẶC BIỆT: "khi đáp án gắn vào chỗ trống, dù đúng hoặc sai
                // sẽ chỉ hiện text và mất đi khung hình chữ nhật ở sau"
                // -> KHÔNG VẼ KHUNG HÌNH CHỮ NHẬT NỀN PHÍA SAU, CHỈ HIỆN TEXT

                g2.setFont(Theme.BODY_BOLD);
                FontMetrics fm = g2.getFontMetrics();
                int tx = (w - fm.stringWidth(filledText)) / 2;
                int ty = (h - fm.getHeight()) / 2 + fm.getAscent();

                // Text vàng/cyan nổi bật trên nền tối
                g2.setColor(hovering ? Theme.YELLOW : new Color(0x67, 0xE8, 0xF9));
                g2.drawString(filledText, tx, ty);

                // Đường gạch chân tinh tế định vị vị trí chỗ trống
                g2.setColor(hovering ? Theme.YELLOW : new Color(0x38, 0xBD, 0xF8, 180));
                g2.setStroke(new BasicStroke(2.0f));
                g2.drawLine(4, h - 3, w - 4, h - 3);
            } else {
                // Trạng thái trống: khung viền nét đứt kèm placeholder ____
                g2.setColor(new Color(0x1E, 0x29, 0x3B, hovering ? 200 : 140));
                g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 8, 8));

                g2.setColor(hovering ? Theme.BLUE : new Color(255, 255, 255, 70));
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10, new float[]{6, 4}, 0));
                g2.draw(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, 8, 8));

                g2.setFont(Theme.BODY);
                g2.setColor(new Color(255, 255, 255, hovering ? 180 : 90));
                FontMetrics fm = g2.getFontMetrics();
                String placeholder = "____";
                int tx = (w - fm.stringWidth(placeholder)) / 2;
                int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(placeholder, tx, ty);
            }
            g2.dispose();
        }
    }

    // =======================================================
    // AnswerChip — Chip đáp án hình chữ nhật bo tròn, nổi khối 3D
    // =======================================================
    public class AnswerChip extends JPanel {
        private final String text;
        private boolean hovering = false;
        private float animAlpha = 1.0f;
        private int animOffsetY = 0;

        AnswerChip(String text) {
            this.text = text;
            setOpaque(false);
            FontMetrics fm = getFontMetrics(Theme.BODY_BOLD);
            int w = Math.max(fm.stringWidth(text) + 36, 84);
            int h = Math.max(fm.getHeight() + 16, 40);
            setPreferredSize(new Dimension(w, h));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            // Đăng ký nhận diện cử chỉ kéo chuột
            DragSource.getDefaultDragSource().createDefaultDragGestureRecognizer(
                this, DnDConstants.ACTION_MOVE, new ChipDragGestureListener(this)
            );

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hovering = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hovering = false;
                    repaint();
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    if (!isEnabled() || !isVisible()) return;
                    // Click nhanh: Tự động đưa chip vào ô trống đầu tiên còn trống
                    for (BlankSlot slot : slotMap.values()) {
                        if (slot.getFilledText() == null) {
                            slot.fill(text);
                            setVisible(false);
                            chipPoolPanel.revalidate();
                            chipPoolPanel.repaint();
                            SoundManager.playAnswerSelect();
                            notifyStateChanged();
                            return;
                        }
                    }
                }
            });
        }

        public String getChipText() {
            return text;
        }

        public void setAnimState(float alpha, int offsetY) {
            this.animAlpha = Math.max(0.0f, Math.min(1.0f, alpha));
            this.animOffsetY = offsetY;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (animAlpha <= 0.001f) return;
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.setupAntiAliasing(g2);
            if (animAlpha < 1.0f) {
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, animAlpha));
            }
            if (animOffsetY != 0) {
                g2.translate(0, animOffsetY);
            }

            int w = getWidth();
            int h = getHeight();

            // YÊU CẦU: "đóng trong khung hình chữ nhật được bo tròn góc, làm nổi khối cho tôi"
            // 1. Bóng đổ 3D mềm mại phía dưới
            g2.setColor(new Color(0, 0, 0, 70));
            g2.fill(new RoundRectangle2D.Float(2, 4, w - 4, h - 4, 14, 14));

            // 2. Chân đế vát bevel 3D tạo chiều sâu
            Color baseRim = hovering ? new Color(0x1E, 0x40, 0xAF) : new Color(0x1E, 0x3A, 0x8A);
            g2.setColor(baseRim);
            g2.fill(new RoundRectangle2D.Float(0, 2, w - 2, h - 3, 14, 14));

            // 3. Mặt nút gradient nổi khối màu xanh sang trọng
            Color topGrad = hovering ? new Color(0x60, 0xA5, 0xFA) : new Color(0x3B, 0x82, 0xF6);
            Color botGrad = hovering ? new Color(0x3B, 0x82, 0xF6) : new Color(0x25, 0x63, 0xEB);
            GradientPaint gp = new GradientPaint(0, 0, topGrad, 0, h - 5, botGrad);
            g2.setPaint(gp);
            g2.fill(new RoundRectangle2D.Float(0, 0, w - 2, h - 5, 14, 14));

            // 4. Ánh sáng bóng gương nửa trên (Specular glass reflection)
            g2.setColor(new Color(255, 255, 255, hovering ? 65 : 45));
            g2.fill(new RoundRectangle2D.Float(1, 1, w - 4, (h - 6) / 2, 12, 12));

            // 5. Viền bo tương phản nổi bật
            g2.setColor(new Color(0x93, 0xC5, 0xFD, hovering ? 230 : 160));
            g2.setStroke(new BasicStroke(1.5f));
            g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 3, h - 5.5f, 14, 14));

            // 6. Text chữ đậm sắc nét có đổ bóng 1px
            g2.setFont(Theme.BODY_BOLD);
            FontMetrics fm = g2.getFontMetrics();
            int tx = (w - 2 - fm.stringWidth(text)) / 2;
            int ty = ((h - 5) - fm.getHeight()) / 2 + fm.getAscent();

            g2.setColor(new Color(0, 0, 0, 80));
            g2.drawString(text, tx + 1, ty + 1);

            g2.setColor(Theme.WHITE);
            g2.drawString(text, tx, ty);

            g2.dispose();
        }
    }

    // =======================================================
    // Drag & Drop Handlers (Xử lý kéo thả và hoán đổi)
    // =======================================================

    private static class ChipTransferable implements Transferable {
        private final String text;
        private final boolean fromSlot;
        private final String slotId;

        ChipTransferable(String text, boolean fromSlot, String slotId) {
            this.text = text;
            this.fromSlot = fromSlot;
            this.slotId = slotId;
        }

        @Override public DataFlavor[] getTransferDataFlavors() { return new DataFlavor[]{DataFlavor.stringFlavor}; }
        @Override public boolean isDataFlavorSupported(DataFlavor f) { return DataFlavor.stringFlavor.equals(f); }
        @Override public Object getTransferData(DataFlavor f) { return text; }
        boolean isFromSlot() { return fromSlot; }
        String getSlotId() { return slotId; }
    }

    /** Bắt đầu kéo từ chip pool */
    private class ChipDragGestureListener implements DragGestureListener {
        private final AnswerChip chip;
        ChipDragGestureListener(AnswerChip chip) { this.chip = chip; }

        @Override
        public void dragGestureRecognized(DragGestureEvent dge) {
            ChipTransferable t = new ChipTransferable(chip.getChipText(), false, null);
            dge.startDrag(DragSource.DefaultMoveDrop, t, new DragSourceAdapter() {
                @Override
                public void dragDropEnd(DragSourceDropEvent e) {
                    if (e.getDropSuccess()) {
                        chip.setVisible(false);
                        chipPoolPanel.revalidate();
                        chipPoolPanel.repaint();
                    }
                }
            });
        }
    }

    /** Bắt đầu kéo từ ô trống đã điền */
    private class SlotDragGestureListener implements DragGestureListener {
        private final BlankSlot slot;
        SlotDragGestureListener(BlankSlot slot) { this.slot = slot; }

        @Override
        public void dragGestureRecognized(DragGestureEvent dge) {
            if (slot.getFilledText() == null) return;
            String text = slot.getFilledText();
            ChipTransferable t = new ChipTransferable(text, true, slot.blankId);
            slot.clear();
            dge.startDrag(DragSource.DefaultMoveDrop, t, new DragSourceAdapter() {
                @Override
                public void dragDropEnd(DragSourceDropEvent e) {
                    if (!e.getDropSuccess()) {
                        // Kéo ra ngoài mà không thả vào ô khác -> trả chip về pool
                        returnChipToPool(text);
                        notifyStateChanged();
                    }
                }
            });
        }
    }

    /** Xử lý thả vào ô trống BlankSlot */
    private class SlotDropHandler extends DropTargetAdapter {
        private final BlankSlot slot;
        SlotDropHandler(BlankSlot slot) { this.slot = slot; }

        @Override
        public void drop(DropTargetDropEvent dtde) {
            try {
                dtde.acceptDrop(DnDConstants.ACTION_MOVE);
                Transferable t = dtde.getTransferable();
                String text = (String) t.getTransferData(DataFlavor.stringFlavor);

                // YÊU CẦU: "khi kéo 1 chip lên ô mà 1 chip khác đã được đặt, thì chip mới sẽ thay thế chip cũ"
                // Nếu ô hiện tại đã có chip, đẩy chip cũ quay về pool
                String oldText = slot.clear();
                if (oldText != null && !oldText.equals(text)) {
                    returnChipToPool(oldText);
                }

                slot.fill(text);
                SoundManager.playAnswerSelect();
                dtde.dropComplete(true);
                notifyStateChanged();
            } catch (Exception ex) {
                dtde.dropComplete(false);
            }
        }
    }

    /** Xử lý thả vào chip pool (trả chip về) */
    private class PoolDropHandler extends DropTargetAdapter {
        @Override
        public void drop(DropTargetDropEvent dtde) {
            try {
                dtde.acceptDrop(DnDConstants.ACTION_MOVE);
                Transferable t = dtde.getTransferable();
                String text = (String) t.getTransferData(DataFlavor.stringFlavor);
                returnChipToPool(text);
                SoundManager.playPop();
                dtde.dropComplete(true);
                notifyStateChanged();
            } catch (Exception ex) {
                dtde.dropComplete(false);
            }
        }
    }

    /** Làm chip có nội dung text hiển thị lại trong pool */
    public void returnChipToPool(String text) {
        if (text == null) return;
        for (AnswerChip chip : allChips) {
            if (chip.getChipText().equals(text) && !chip.isVisible()) {
                chip.setVisible(true);
                chipPoolPanel.revalidate();
                chipPoolPanel.repaint();
                return;
            }
        }
    }
}
