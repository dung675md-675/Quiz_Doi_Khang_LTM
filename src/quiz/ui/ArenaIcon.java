package quiz.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import javax.swing.Icon;

/** Small, resolution-independent icons shared by the Arena screens. */
public final class ArenaIcon implements Icon {
    public enum Type {
        BRAND, PLAYERS, FRIENDS, TROPHY, HISTORY, SOUND, PROFILE, LOGOUT,
        ARROW, SEARCH, PLAY, BOLT, CHECK, SETTINGS, BOOK
    }

    private final Type type;
    private final int size;
    private final Color color;

    /** A null color follows the owning component's foreground. */
    public ArenaIcon(Type type, int size, Color color) {
        this.type = java.util.Objects.requireNonNull(type);
        this.size = Math.max(8, size);
        this.color = color;
    }

    @Override public int getIconWidth() { return size; }
    @Override public int getIconHeight() { return size; }

    @Override
    public void paintIcon(Component component, Graphics graphics, int x, int y) {
        Graphics2D g = (Graphics2D) graphics.create();
        Theme.setupAntiAliasing(g);
        g.translate(x, y);
        g.scale(size / 24.0, size / 24.0);
        g.setStroke(new BasicStroke(1.75f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        // ModernButton chooses its ink color for enabled/reveal/disabled states
        // immediately before painting an icon. Labels use their own foreground.
        g.setColor(color != null ? color : component instanceof ModernButton
                ? graphics.getColor() : component.getForeground());
        switch (type) {
            case BRAND -> {
                g.drawRoundRect(3, 3, 18, 18, 7, 7);
                path(g, true, 13, 5.5, 7.8, 13, 11.5, 13, 10.7, 18.5, 16.4, 10.8, 12.6, 10.8);
            }
            case BOLT -> path(g, true, 13.5, 2.5, 5.5, 13, 11, 13, 10, 21.5, 19, 10, 13, 10);
            case PLAYERS, FRIENDS -> {
                g.drawOval(5, 3, 7, 7);
                g.drawArc(2, 12, 13, 12, 0, 180);
                if (type == Type.PLAYERS) {
                    g.drawArc(14, 4, 6, 7, -75, 245);
                    g.drawArc(14, 12, 8, 10, 0, 95);
                } else {
                    path(g, false, 15, 15, 17.2, 17.2, 21.5, 12.5);
                }
            }
            case TROPHY -> {
                path(g, false, 7, 3, 17, 3, 17, 9, 15.5, 12, 12, 14, 8.5, 12, 7, 9, 7, 3);
                path(g, false, 7, 5, 3, 5, 3, 8, 5, 11, 8, 11);
                path(g, false, 17, 5, 21, 5, 21, 8, 19, 11, 16, 11);
                g.drawLine(12, 14, 12, 19);
                g.drawLine(8, 21, 16, 21);
                g.drawLine(9, 19, 15, 19);
            }
            case HISTORY -> {
                g.drawArc(4, 4, 16, 16, -60, 315);
                path(g, false, 3, 4, 3, 9, 8, 9);
                path(g, false, 12, 7, 12, 12, 15.5, 14);
            }
            case SOUND -> {
                path(g, false, 4, 9, 8, 9, 13, 5, 13, 19, 8, 15, 4, 15, 4, 9);
                g.drawArc(11, 7, 8, 10, -55, 110);
                g.drawArc(11, 3, 12, 18, -55, 110);
            }
            case PROFILE -> {
                g.drawOval(8, 3, 8, 8);
                g.drawArc(4, 13, 16, 14, 0, 180);
            }
            case LOGOUT -> {
                path(g, false, 9, 4, 4, 4, 4, 20, 9, 20);
                g.drawLine(10, 12, 21, 12);
                path(g, false, 16, 7, 21, 12, 16, 17);
            }
            case ARROW -> {
                g.drawLine(4, 12, 20, 12);
                path(g, false, 14, 6, 20, 12, 14, 18);
            }
            case SEARCH -> {
                g.drawOval(3, 3, 13, 13);
                g.drawLine(14, 14, 21, 21);
            }
            case PLAY -> path(g, true, 7, 4, 20, 12, 7, 20);
            case CHECK -> path(g, false, 4, 12, 9, 17, 20, 6);
            case BOOK -> {
                path(g, false, 12, 5, 9, 3.5, 3, 3.5, 3, 19, 9, 19, 12, 21, 15, 19, 21, 19, 21, 3.5, 15, 3.5, 12, 5, 12, 21);
            }
            case SETTINGS -> {
                g.drawOval(4, 4, 16, 16);
                g.drawOval(9, 9, 6, 6);
                for (int i = 0; i < 8; i++) {
                    double a = i * Math.PI / 4;
                    g.drawLine((int) (12 + 8 * Math.cos(a)), (int) (12 + 8 * Math.sin(a)),
                            (int) (12 + 10 * Math.cos(a)), (int) (12 + 10 * Math.sin(a)));
                }
            }
        }
        g.dispose();
    }

    private static void path(Graphics2D g, boolean fill, double... points) {
        Path2D path = new Path2D.Double();
        path.moveTo(points[0], points[1]);
        for (int i = 2; i < points.length; i += 2) path.lineTo(points[i], points[i + 1]);
        if (fill) {
            path.closePath();
            g.fill(path);
        } else {
            g.draw(path);
        }
    }
}
