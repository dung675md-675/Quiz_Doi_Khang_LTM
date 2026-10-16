package quiz.ui;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * AvatarPicker — Dialog chọn avatar từ 12 avatar ảnh PNG.
 * Avatar được hiển thị dưới dạng hình tròn (crop tròn tự động).
 */
public class AvatarPicker extends JDialog {

    public static final String[] AVATAR_IDS = {
        "avatar_01", "avatar_02", "avatar_03", "avatar_04",
        "avatar_05", "avatar_06", "avatar_07", "avatar_08",
        "avatar_09", "avatar_10", "avatar_11", "avatar_12"
    };

    /** Cache ảnh avatar đã được crop tròn */
    private static final Map<String, BufferedImage> AVATAR_IMAGES = new HashMap<>();

    static {
        for (String id : AVATAR_IDS) {
            try {
                // Thử nhiều đường dẫn có thể
                File imgFile = new File("avatars/avatars/" + id + ".png");
                if (!imgFile.exists()) {
                    imgFile = new File("avatars/" + id + ".png");
                }
                if (imgFile.exists()) {
                    BufferedImage raw = ImageIO.read(imgFile);
                    if (raw != null) {
                        AVATAR_IMAGES.put(id, cropCircle(raw));
                    }
                }
            } catch (Exception e) {
                // ignore loading errors
            }
        }
    }

    /**
     * Crop ảnh thành hình tròn, tự động cắt viền dư thừa (center-crop hình vuông rồi clip tròn).
     */
    private static BufferedImage cropCircle(BufferedImage src) {
        int w = src.getWidth();
        int h = src.getHeight();
        // Center crop thành hình vuông
        int size = Math.min(w, h);
        int x = (w - size) / 2;
        int y = (h - size) / 2;
        BufferedImage square = src.getSubimage(x, y, size, size);

        // Tạo ảnh tròn
        BufferedImage circle = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = circle.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        // Clip tròn
        Ellipse2D.Double clipShape = new Ellipse2D.Double(0, 0, size, size);
        g2.setClip(clipShape);
        g2.drawImage(square, 0, 0, null);
        g2.dispose();
        return circle;
    }

    /**
     * Lấy ảnh avatar đã crop tròn. Trả về null nếu chưa load được.
     */
    public static BufferedImage getAvatarImage(String avatarId) {
        BufferedImage img = AVATAR_IMAGES.get(avatarId);
        if (img == null && avatarId != null) {
            // Lazy load nếu chưa có
            try {
                File imgFile = new File("avatars/avatars/" + avatarId + ".png");
                if (!imgFile.exists()) {
                    imgFile = new File("avatars/" + avatarId + ".png");
                }
                if (imgFile.exists()) {
                    BufferedImage raw = ImageIO.read(imgFile);
                    if (raw != null) {
                        img = cropCircle(raw);
                        AVATAR_IMAGES.put(avatarId, img);
                    }
                }
            } catch (Exception e) { /* ignore */ }
        }
        return img;
    }

    /**
     * Vẽ avatar tròn vào Graphics2D tại vị trí (x, y) với kích thước diameter.
     * Nếu không load được ảnh, vẽ fallback là vòng tròn màu + chữ cái đầu.
     */
    public static void paintAvatar(Graphics2D g2, String avatarId, int x, int y, int diameter) {
        BufferedImage img = getAvatarImage(avatarId);
        if (img != null) {
            // Vẽ ảnh avatar tròn
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.drawImage(img, x, y, diameter, diameter, null);
        } else {
            // Fallback: vòng tròn màu
            g2.setColor(getColor(avatarId));
            g2.fillOval(x, y, diameter, diameter);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Segoe UI", Font.BOLD, diameter / 2));
            FontMetrics fm = g2.getFontMetrics();
            String initial = "?";
            int tx = x + (diameter - fm.stringWidth(initial)) / 2;
            int ty = y + (diameter - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(initial, tx, ty);
        }
    }

    // Deprecated emoji – kept for backward compat
    @SuppressWarnings("unused")
    static final String[] AVATAR_EMOJIS = {};

    static final Color[] AVATAR_COLORS = {
        new Color(0x13, 0x68, 0xCE),  // Blue
        new Color(0x86, 0x4C, 0xBF),  // Purple
        new Color(0x08, 0x91, 0xB2),  // Teal
        new Color(0xE2, 0x1B, 0x3C),  // Red
        new Color(0x26, 0x89, 0x0C),  // Green
        new Color(0xD8, 0x9E, 0x00),  // Gold
        new Color(0x4F, 0x46, 0xE5),  // Indigo
        new Color(0x06, 0xB6, 0xD4),  // Cyan
        new Color(0xE0, 0x56, 0x7E),  // Rose
        new Color(0x7C, 0x3A, 0xED),  // Violet
        new Color(0xEA, 0x58, 0x0C),  // Orange
        new Color(0xCA, 0x8A, 0x04),  // Amber
    };

    private String selectedId;
    private final Consumer<String> onConfirm;

    public AvatarPicker(Frame owner, String currentAvatarId, Consumer<String> onConfirm) {
        super(owner, "Chọn Avatar", true);
        this.selectedId = currentAvatarId != null ? currentAvatarId : "avatar_01";
        this.onConfirm = onConfirm;
        buildUI();
        setSize(520, 420);
        setLocationRelativeTo(owner);
        setResizable(false);
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBorder(new EmptyBorder(20, 24, 20, 24));
        root.setBackground(Theme.WHITE);

        // Title
        JLabel title = new JLabel("Chọn avatar của bạn", SwingConstants.CENTER);
        title.setFont(Theme.TITLE);
        title.setForeground(Theme.DARK_TEXT);
        root.add(title, BorderLayout.NORTH);

        // Grid 4×3 cho 12 avatar
        JPanel grid = new JPanel(new GridLayout(3, 4, 12, 12));
        grid.setOpaque(false);
        ButtonGroup bg = new ButtonGroup();

        for (int i = 0; i < AVATAR_IDS.length; i++) {
            final String id = AVATAR_IDS[i];
            final Color color = i < AVATAR_COLORS.length ? AVATAR_COLORS[i] : AVATAR_COLORS[0];
            AvatarButton btn = new AvatarButton(id, color, id.equals(selectedId));
            btn.addActionListener(e -> {
                SoundManager.playClick();
                selectedId = id;
                // Deselect others
                for (Component c : grid.getComponents()) {
                    if (c instanceof AvatarButton ab && !ab.equals(btn)) ab.setSelectedState(false);
                }
                btn.setSelectedState(true);
            });
            bg.add(btn);
            grid.add(btn);
        }
        root.add(grid, BorderLayout.CENTER);

        // Confirm button
        ModernButton btnConfirm = new ModernButton("XÁC NHẬN", ModernButton.Style.PRIMARY);
        btnConfirm.setPreferredSize(new Dimension(160, 40));
        btnConfirm.addActionListener(e -> {
            SoundManager.playClick();
            dispose();
            if (onConfirm != null) onConfirm.accept(selectedId);
        });
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnPanel.setOpaque(false);
        btnPanel.add(btnConfirm);
        root.add(btnPanel, BorderLayout.SOUTH);

        setContentPane(root);
    }

    /**
     * Trả về emoji cho avatarId – DEPRECATED, dùng getAvatarImage() thay thế.
     */
    public static String getEmoji(String avatarId) {
        return ""; // No longer using emojis
    }

    public static Color getColor(String avatarId) {
        for (int i = 0; i < AVATAR_IDS.length; i++) {
            if (AVATAR_IDS[i].equals(avatarId)) return AVATAR_COLORS[i];
        }
        return AVATAR_COLORS[0];
    }

    /** Nút avatar hiển thị ảnh tròn, có thể toggle selected */
    private static class AvatarButton extends JToggleButton {
        private final String avatarId;
        private final Color bgColor;
        private boolean selected2 = false;

        AvatarButton(String avatarId, Color bgColor, boolean initialSelected) {
            this.avatarId = avatarId;
            this.bgColor = bgColor;
            this.selected2 = initialSelected;
            setPreferredSize(new Dimension(80, 80));
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            if (initialSelected) setSelected(true);
        }

        public void setSelectedState(boolean sel) {
            this.selected2 = sel;
            setSelected(sel);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.setupAntiAliasing(g2);
            int size = Math.min(getWidth(), getHeight()) - 8;
            int ox = (getWidth() - size) / 2;
            int oy = (getHeight() - size) / 2;

            // Selection ring
            if (selected2 || isSelected()) {
                g2.setColor(Theme.PURPLE);
                g2.setStroke(new BasicStroke(3f));
                g2.drawOval(ox - 3, oy - 3, size + 6, size + 6);
            }

            // Vẽ avatar tròn từ ảnh
            AvatarPicker.paintAvatar(g2, avatarId, ox, oy, size);

            g2.dispose();
        }
    }
}
