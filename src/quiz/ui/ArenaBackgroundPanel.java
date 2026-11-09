package quiz.ui;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Frame;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.Objects;
import java.util.Random;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** Full-bleed scene art with quiet motion that pauses when its screen is hidden. */
public class ArenaBackgroundPanel extends JPanel {
    public enum Scene {
        AUTH,
        LOBBY
    }

    private record Particle(double x, double y, double speed, double phase, double size) {}

    private static final int FRAME_DELAY_MS = 42;
    private static final Color NAVY = new Color(8, 16, 37);
    private static final BufferedImage CYAN_GLOW = glow(new Color(53, 199, 255));
    private static final BufferedImage VIOLET_GLOW = glow(new Color(132, 93, 255));
    private final Scene scene;
    private final Timer animationTimer;
    private final Particle[] particles;
    private final WindowAdapter windowListener = new WindowAdapter() {
        @Override
        public void windowStateChanged(WindowEvent event) {
            updateAnimationState();
        }

        @Override
        public void windowIconified(WindowEvent event) {
            updateAnimationState();
        }

        @Override
        public void windowDeiconified(WindowEvent event) {
            updateAnimationState();
        }

        @Override
        public void windowClosed(WindowEvent event) {
            stopAnimation();
        }
    };

    private volatile boolean motionEnabled = !Boolean.getBoolean("quiz.ui.reduceMotion");
    private volatile boolean animationRunning;
    private volatile BufferedImage artwork;
    private BufferedImage backgroundCache;
    private int cachedWidth;
    private int cachedHeight;
    private Window observedWindow;
    private double elapsedSeconds;
    private long previousTickNanos;

    public ArenaBackgroundPanel(LayoutManager layout, Scene scene) {
        super(layout);
        this.scene = Objects.requireNonNull(scene, "scene");
        setOpaque(true);
        setBackground(NAVY);
        particles = createParticles(scene);

        animationTimer = new Timer(FRAME_DELAY_MS, event -> tick());
        animationTimer.setCoalesce(true);
        animationTimer.setInitialDelay(FRAME_DELAY_MS);
        addHierarchyListener(event -> {
            long flags = event.getChangeFlags();
            if ((flags & (HierarchyEvent.SHOWING_CHANGED
                    | HierarchyEvent.DISPLAYABILITY_CHANGED
                    | HierarchyEvent.PARENT_CHANGED)) != 0) {
                bindWindow();
                updateAnimationState();
            }
        });
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                updateAnimationState();
            }

            @Override
            public void componentShown(ComponentEvent event) {
                updateAnimationState();
            }

            @Override
            public void componentHidden(ComponentEvent event) {
                updateAnimationState();
            }
        });

        ArenaArtwork.load(scene).thenAccept(image -> {
            if (image != null) {
                SwingUtilities.invokeLater(() -> {
                    artwork = image;
                    backgroundCache = null;
                    repaint();
                });
            }
        });
    }

    public void setMotionEnabled(boolean enabled) {
        motionEnabled = enabled;
        updateAnimationState();
        repaint();
    }

    public boolean isMotionEnabled() {
        return motionEnabled;
    }

    public boolean isAnimationRunning() {
        return animationRunning;
    }

    public boolean isArtworkLoaded() {
        return artwork != null;
    }

    @Override
    public void addNotify() {
        super.addNotify();
        bindWindow();
        updateAnimationState();
    }

    @Override
    public void removeNotify() {
        stopAnimation();
        unbindWindow();
        super.removeNotify();
    }

    private void bindWindow() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::bindWindow);
            return;
        }
        Window window = isDisplayable() ? SwingUtilities.getWindowAncestor(this) : null;
        if (window != observedWindow) {
            unbindWindow();
            observedWindow = window;
            if (window != null) {
                window.addWindowListener(windowListener);
                window.addWindowStateListener(windowListener);
            }
        }
    }

    private void unbindWindow() {
        if (observedWindow != null) {
            observedWindow.removeWindowListener(windowListener);
            observedWindow.removeWindowStateListener(windowListener);
            observedWindow = null;
        }
    }

    private void updateAnimationState() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::updateAnimationState);
            return;
        }
        boolean iconified = observedWindow instanceof Frame frame
                && (frame.getExtendedState() & Frame.ICONIFIED) != 0;
        boolean active = motionEnabled && isDisplayable() && isShowing()
                && getWidth() > 0 && getHeight() > 0 && !iconified;
        if (active) {
            if (!animationTimer.isRunning()) {
                previousTickNanos = System.nanoTime();
                animationTimer.start();
            }
            animationRunning = true;
        } else {
            stopAnimation();
        }
    }

    private void stopAnimation() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::stopAnimation);
            return;
        }
        animationTimer.stop();
        animationRunning = false;
        previousTickNanos = 0L;
    }

    private void tick() {
        if (!motionEnabled || !isDisplayable() || !isShowing()) {
            updateAnimationState();
            return;
        }
        long now = System.nanoTime();
        if (previousTickNanos != 0L) {
            elapsedSeconds += Math.min(0.12, (now - previousTickNanos) / 1_000_000_000.0);
        }
        previousTickNanos = now;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        int width = getWidth();
        int height = getHeight();
        if (width < 1 || height < 1) {
            return;
        }
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            Theme.setupAntiAliasing(g);
            if (backgroundCache == null || cachedWidth != width || cachedHeight != height) {
                backgroundCache = createBackground(width, height);
                cachedWidth = width;
                cachedHeight = height;
            }
            g.drawImage(backgroundCache, 0, 0, null);
            paintAtmosphere(g, width, height);
        } finally {
            g.dispose();
        }
    }

    private BufferedImage createBackground(int width, int height) {
        BufferedImage buffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = buffer.createGraphics();
        try {
            g.setPaint(new GradientPaint(0, 0, new Color(9, 18, 42),
                    width, height, new Color(24, 23, 66)));
            g.fillRect(0, 0, width, height);

            BufferedImage source = artwork;
            if (source != null) {
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                if (scene == Scene.LOBBY && width > height * 2.3) {
                    // A shallow lobby banner needs the complete trophy, rather than a tall crop.
                    int drawWidth = (int) Math.ceil((double) height * source.getWidth()
                            / source.getHeight());
                    int imageX = width - drawWidth;
                    g.drawImage(source, imageX, 0, drawWidth, height, null);
                    int fadeWidth = Math.max(36, Math.min(100, drawWidth / 3));
                    g.setPaint(new GradientPaint(imageX, 0, new Color(15, 19, 49),
                            imageX + fadeWidth, 0, new Color(15, 19, 49, 0)));
                    g.fillRect(imageX, 0, fadeWidth, height);
                } else {
                    double scale = Math.max((double) width / source.getWidth(),
                            (double) height / source.getHeight());
                    int drawWidth = Math.max(width, (int) Math.ceil(source.getWidth() * scale));
                    int drawHeight = Math.max(height, (int) Math.ceil(source.getHeight() * scale));
                    g.drawImage(source, (width - drawWidth) / 2, (height - drawHeight) / 2,
                            drawWidth, drawHeight, null);
                }
                g.setColor(new Color(6, 13, 34, scene == Scene.AUTH ? 28 : 38));
                g.fillRect(0, 0, width, height);
            } else {
                g.drawImage(VIOLET_GLOW, (int) (-width * 0.12), (int) (height * 0.18),
                        (int) (width * 0.85), (int) (height * 1.05), null);
                g.drawImage(CYAN_GLOW, (int) (width * 0.47), (int) (-height * 0.3),
                        (int) (width * 0.8), (int) (height * 1.1), null);
                g.setColor(new Color(132, 166, 221, 17));
                for (int x = 24; x < width; x += 40) {
                    for (int y = 24; y < height; y += 40) {
                        g.fillOval(x, y, 1, 1);
                    }
                }
            }

            // Protect the region reserved for actual UI copy, regardless of the scene artwork.
            g.setPaint(scene == Scene.LOBBY
                    ? new GradientPaint(0, 0, new Color(5, 13, 34, 180),
                            width * 0.7f, 0, new Color(5, 13, 34, 0))
                    : new GradientPaint(0, 0, new Color(5, 13, 34, 18),
                            width, 0, new Color(5, 13, 34, 80)));
            g.fillRect(0, 0, width, height);
        } finally {
            g.dispose();
        }
        return buffer;
    }

    private void paintAtmosphere(Graphics2D g, int width, int height) {
        double time = elapsedSeconds;
        double focusX = scene == Scene.AUTH ? 0.26 : 0.78;
        double focusY = scene == Scene.AUTH ? 0.64 : 0.54;
        int glowSize = Math.max(90, (int) (Math.min(width, height * 1.8) * 0.64));
        float breath = (float) (0.11 + 0.025 * Math.sin(time * 0.65));
        g.setComposite(AlphaComposite.SrcOver.derive(breath));
        g.drawImage(CYAN_GLOW,
                (int) (width * focusX - glowSize / 2.0 + Math.sin(time * 0.18) * 8),
                (int) (height * focusY - glowSize / 2.0), glowSize, glowSize, null);
        g.setComposite(AlphaComposite.SrcOver);

        double orbitWidth = Math.min(width * 0.32, height * 0.86);
        double orbitHeight = orbitWidth * 0.28;
        double angle = Math.sin(time * 0.12) * 0.09 - 0.12;
        Graphics2D orbit = (Graphics2D) g.create();
        orbit.translate(width * focusX, height * focusY);
        orbit.rotate(angle);
        orbit.setStroke(new BasicStroke(1.0f));
        orbit.setColor(new Color(143, 211, 255, 25));
        orbit.draw(new Ellipse2D.Double(-orbitWidth / 2, -orbitHeight / 2,
                orbitWidth, orbitHeight));
        orbit.dispose();

        for (Particle particle : particles) {
            double x = particle.x() + Math.sin(time * 0.11 + particle.phase()) * 0.018;
            double y = (particle.y() - time * particle.speed() * 0.004) % 1.0;
            if (y < 0) {
                y += 1.0;
            }
            int alpha = (int) (45 + 25 * Math.sin(time * 0.4 + particle.phase()));
            g.setColor(new Color(166, 222, 255, alpha));
            double size = particle.size();
            g.fill(new Ellipse2D.Double(x * width, y * height, size, size));
        }
    }

    private static Particle[] createParticles(Scene scene) {
        Random random = new Random(scene == Scene.AUTH ? 0xA17E2L : 0xB0BB7L);
        Particle[] result = new Particle[scene == Scene.AUTH ? 24 : 16];
        for (int i = 0; i < result.length; i++) {
            result[i] = new Particle(random.nextDouble(), random.nextDouble(),
                    0.3 + random.nextDouble() * 0.7, random.nextDouble() * Math.PI * 2,
                    1.0 + random.nextDouble() * 1.8);
        }
        return result;
    }

    private static BufferedImage glow(Color color) {
        BufferedImage sprite = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = sprite.createGraphics();
        try {
            g.setPaint(new RadialGradientPaint(64, 64, 64,
                    new float[] {0f, 0.38f, 1f},
                    new Color[] {new Color(color.getRed(), color.getGreen(), color.getBlue(), 180),
                        new Color(color.getRed(), color.getGreen(), color.getBlue(), 70),
                        new Color(color.getRed(), color.getGreen(), color.getBlue(), 0)}));
            g.fillRect(0, 0, 128, 128);
        } finally {
            g.dispose();
        }
        return sprite;
    }
}
