package quiz.ui;

import java.awt.*;
import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.text.JTextComponent;

/** Scoped dashboard colors; authentication and game answer colors remain independent. */
public final class ArenaPalette {
    public static final Color BG = new Color(0x08, 0x0D, 0x1C);
    public static final Color SIDEBAR = new Color(0x0D, 0x14, 0x28);
    public static final Color SURFACE = new Color(0x12, 0x1D, 0x34);
    public static final Color SURFACE_RAISED = new Color(0x1A, 0x27, 0x42);
    public static final Color BORDER = new Color(0x26, 0x35, 0x50);
    public static final Color TEXT = new Color(0xF3, 0xF6, 0xFF);
    public static final Color MUTED = new Color(0x9B, 0xAA, 0xC6);
    public static final Color ACCENT = new Color(0xC4, 0xF7, 0x6B);
    public static final Color ACCENT_INK = new Color(0x13, 0x26, 0x0A);
    public static final Color CYAN = new Color(0x65, 0xDE, 0xF5);

    private ArenaPalette() {}

    public static Font font(float size, int style) {
        return Theme.BODY.deriveFont(style, size);
    }

    /** Applies the dashboard treatment to existing controls without changing their actions. */
    public static void styleTree(Component component) {
        if (component instanceof ModernButton button) button.setArenaStyle(true);
        else if (component instanceof ModernBadge badge) badge.setArenaStyle(true);
        else if (component instanceof ModernTextField field) field.setArenaStyle(true);
        else if (component instanceof ModernPasswordField field) field.setArenaStyle(true);
        else if (component instanceof ModernCard card) card.setArenaStyle(true);
        else if (component instanceof EmptyStatePanel empty) empty.setArenaStyle(true);
        else if (component instanceof JTextComponent text) {
            text.setBackground(SURFACE_RAISED);
            text.setForeground(TEXT);
            text.setCaretColor(CYAN);
            text.setSelectionColor(new Color(56, 76, 111));
            text.setSelectedTextColor(TEXT);
        } else if (component instanceof JLabel label) {
            Color old = label.getForeground();
            if (Theme.MUTED_GRAY.equals(old) || Theme.DISABLED.equals(old)) label.setForeground(MUTED);
            else if (Theme.BLUE.equals(old) || Theme.PURPLE.equals(old)
                    || Theme.INDIGO_DARK.equals(old) || Theme.ACTION.equals(old)) label.setForeground(CYAN);
            else if (old == null || Theme.DARK_TEXT.equals(old) || Theme.TEXT_BODY.equals(old)
                    || Color.BLACK.equals(old) || old instanceof javax.swing.plaf.UIResource) label.setForeground(TEXT);
        } else if (component instanceof JScrollPane scroll) {
            scroll.setBackground(BG);
            scroll.getViewport().setBackground(BG);
            scroll.setBorder(BorderFactory.createEmptyBorder());
            styleScrollBar(scroll.getVerticalScrollBar());
            styleScrollBar(scroll.getHorizontalScrollBar());
        } else if (component instanceof JTable table) {
            table.setBackground(SURFACE);
            table.setForeground(TEXT);
            table.setGridColor(BORDER);
            table.setSelectionBackground(new Color(42, 61, 88));
            table.setSelectionForeground(TEXT);
            table.getTableHeader().setBackground(SURFACE_RAISED);
            table.getTableHeader().setForeground(MUTED);
        } else if (component instanceof JComboBox<?> combo) {
            combo.setBackground(SURFACE_RAISED);
            combo.setForeground(TEXT);
        } else if (component instanceof AbstractButton button) {
            button.setBackground(SURFACE_RAISED);
            button.setForeground(TEXT);
        } else if (component instanceof JPanel panel && panel.isOpaque()) {
            Color old = panel.getBackground();
            if (old == null || old instanceof javax.swing.plaf.UIResource
                    || old.getRed() + old.getGreen() + old.getBlue() > 550) panel.setBackground(SURFACE);
        }
        if (component instanceof Container parent) {
            for (Component child : parent.getComponents()) styleTree(child);
        }
    }

    public static void styleScrollBar(JScrollBar bar) {
        if (Boolean.TRUE.equals(bar.getClientProperty("arena.scrollbar"))) return;
        bar.putClientProperty("arena.scrollbar", Boolean.TRUE);
        bar.setOpaque(false);
        bar.setPreferredSize(bar.getOrientation() == Adjustable.VERTICAL
                ? new Dimension(10, 0) : new Dimension(0, 10));
        bar.setUI(new BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() {
                trackColor = BG;
                thumbColor = BORDER;
            }
            @Override protected JButton createDecreaseButton(int direction) { return zeroButton(); }
            @Override protected JButton createIncreaseButton(int direction) { return zeroButton(); }
            private JButton zeroButton() {
                JButton button = new JButton();
                button.setPreferredSize(new Dimension(0, 0));
                return button;
            }
            @Override protected void paintTrack(Graphics graphics, JComponent c, Rectangle bounds) {
                graphics.setColor(BG);
                graphics.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            }
            @Override protected void paintThumb(Graphics graphics, JComponent c, Rectangle bounds) {
                if (!c.isEnabled() || bounds.isEmpty()) return;
                Graphics2D g = (Graphics2D) graphics.create();
                Theme.setupAntiAliasing(g);
                g.setColor(isThumbRollover() ? new Color(73, 94, 128) : BORDER);
                g.fillRoundRect(bounds.x + 2, bounds.y + 2,
                        Math.max(4, bounds.width - 4), Math.max(4, bounds.height - 4), 8, 8);
                g.dispose();
            }
        });
    }
}
