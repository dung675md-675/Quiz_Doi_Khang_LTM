package quiz.ui;

import java.awt.*;
import javax.swing.*;
import javax.swing.plaf.basic.BasicTabbedPaneUI;

/** Quiet capsule tabs that retain Swing's keyboard and accessibility support. */
final class ArenaTabbedPane extends JTabbedPane {
    ArenaTabbedPane() {
        setTabLayoutPolicy(SCROLL_TAB_LAYOUT);
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder());
        setFont(Theme.BODY_BOLD);
    }

    @Override public void updateUI() {
        setUI(new BasicTabbedPaneUI() {
            @Override protected void installDefaults() {
                super.installDefaults();
                tabInsets = new Insets(10, 17, 10, 17);
                selectedTabPadInsets = new Insets(0, 0, 0, 0);
                tabAreaInsets = new Insets(4, 0, 12, 0);
                contentBorderInsets = new Insets(0, 0, 0, 0);
                tabRunOverlay = 0;
            }

            @Override protected void paintTabBackground(Graphics graphics, int placement,
                    int index, int x, int y, int width, int height, boolean selected) {
                Graphics2D g = (Graphics2D) graphics.create();
                Theme.setupAntiAliasing(g);
                g.setColor(selected ? new Color(40, 57, 48) : getRolloverTab() == index
                        ? ArenaPalette.SURFACE_RAISED : ArenaPalette.BG);
                g.fillRoundRect(x + 2, y + 1, width - 4, height - 2, 14, 14);
                g.dispose();
            }

            @Override protected void paintText(Graphics graphics, int placement, Font font,
                    FontMetrics metrics, int index, String title, Rectangle text, boolean selected) {
                graphics.setFont(font);
                graphics.setColor(selected ? ArenaPalette.ACCENT : ArenaPalette.MUTED);
                graphics.drawString(title, text.x, text.y + metrics.getAscent());
            }

            @Override protected void paintTabBorder(Graphics g, int placement, int index,
                    int x, int y, int width, int height, boolean selected) {}

            @Override protected void paintContentBorder(Graphics g, int placement, int selected) {}

            @Override protected void paintFocusIndicator(Graphics graphics, int placement,
                    Rectangle[] rectangles, int index, Rectangle icon, Rectangle text, boolean selected) {
                if (!tabPane.hasFocus() || !selected) return;
                Graphics2D g = (Graphics2D) graphics.create();
                Theme.setupAntiAliasing(g);
                Rectangle bounds = rectangles[index];
                g.setColor(ArenaPalette.ACCENT);
                g.drawRoundRect(bounds.x + 3, bounds.y + 2, bounds.width - 7, bounds.height - 5, 12, 12);
                g.dispose();
            }
        });
    }
}
