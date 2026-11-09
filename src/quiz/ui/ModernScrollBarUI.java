package quiz.ui;

import java.awt.*;
import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;

/**
 * ModernScrollBarUI — Thanh cuộn phẳng thanh lịch, bo góc mềm mại không có mũi tên cồng kềnh.
 */
public class ModernScrollBarUI extends BasicScrollBarUI {

    private final boolean arenaStyle;

    public ModernScrollBarUI() { this(false); }
    public ModernScrollBarUI(boolean arenaStyle) { this.arenaStyle = arenaStyle; }

    @Override
    protected void configureScrollBarColors() {
        this.thumbColor = arenaStyle ? ArenaPalette.BORDER : new Color(0xCB, 0xD5, 0xE1);
        this.trackColor = arenaStyle ? ArenaPalette.BG : Theme.OFF_WHITE;
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        if (arenaStyle && scrollbar.getOrientation() == Adjustable.HORIZONTAL)
            return new Dimension(super.getPreferredSize(c).width, 8);
        return new Dimension(8, super.getPreferredSize(c).height);
    }

    @Override
    protected JButton createDecreaseButton(int orientation) {
        return createZeroButton();
    }

    @Override
    protected JButton createIncreaseButton(int orientation) {
        return createZeroButton();
    }

    private JButton createZeroButton() {
        JButton btn = new JButton();
        btn.setPreferredSize(new Dimension(0, 0));
        btn.setMinimumSize(new Dimension(0, 0));
        btn.setMaximumSize(new Dimension(0, 0));
        return btn;
    }

    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
        // Track trong suốt
    }

    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
        if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) return;
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.setupAntiAliasing(g2);
        boolean isRollover = isThumbRollover();
        Color cThumb = arenaStyle ? isRollover ? new Color(73, 94, 128) : ArenaPalette.BORDER
                : isRollover ? new Color(0x94, 0xA3, 0xB8) : new Color(0xCB, 0xD5, 0xE1);
        g2.setColor(cThumb);
        g2.fillRoundRect(thumbBounds.x + 1, thumbBounds.y + 2, thumbBounds.width - 2, thumbBounds.height - 4, 6, 6);
        g2.dispose();
    }
}

