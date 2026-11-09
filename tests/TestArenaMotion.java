package quiz;

import java.awt.BorderLayout;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.event.HierarchyEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import quiz.ui.ArenaBackgroundPanel;

/** Headless lifecycle and in-memory asset-loading checks; no windows or disk writes. */
public final class TestArenaMotion {
    private static int checks;

    public static void main(String[] args) throws Exception {
        testVisibilityLifecycle();
        testReducedMotion();
        testAsyncArtworkAndCache();
        System.out.println("PASS TestArenaMotion: " + checks + " checks");
    }

    private static void testVisibilityLifecycle() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            FakeBackdrop panel = new FakeBackdrop();
            check(!panel.isAnimationRunning(), "Constructing a backdrop starts no timer");
            panel.setMotionEnabled(true);
            panel.setSize(640, 360);
            panel.addNotify();
            check(!panel.isAnimationRunning(), "Displayable but hidden panel remains paused");

            panel.showForTest(true);
            check(panel.isAnimationRunning(), "Visible panel starts its animation");
            panel.showForTest(false);
            check(!panel.isAnimationRunning(), "Hidden card pauses its animation");
            panel.showForTest(true);
            check(panel.isAnimationRunning(), "Showing a card resumes its animation");

            panel.setMotionEnabled(false);
            check(!panel.isMotionEnabled() && !panel.isAnimationRunning(),
                    "Motion toggle disables the running animation");
            panel.setMotionEnabled(true);
            check(panel.isAnimationRunning(), "Motion toggle resumes a visible scene");

            panel.removeNotify();
            check(!panel.isAnimationRunning(), "Removed panel releases its animation timer");
            panel.addNotify();
            check(panel.isAnimationRunning(), "Reattached visible panel resumes");
            panel.showForTest(false);
            panel.setMotionEnabled(false);
            panel.setMotionEnabled(true);
            check(!panel.isAnimationRunning(), "Enabling motion does not animate a hidden card");
            panel.removeNotify();
        });
    }

    private static void testReducedMotion() throws Exception {
        String previous = System.getProperty("quiz.ui.reduceMotion");
        try {
            System.setProperty("quiz.ui.reduceMotion", "true");
            SwingUtilities.invokeAndWait(() -> {
                FakeBackdrop panel = new FakeBackdrop();
                panel.setSize(640, 360);
                panel.addNotify();
                panel.showForTest(true);
                check(!panel.isMotionEnabled() && !panel.isAnimationRunning(),
                        "Reduced-motion launch starts with a static scene");
                panel.setMotionEnabled(true);
                check(panel.isAnimationRunning(), "User can explicitly enable scene motion");
                panel.removeNotify();
            });
        } finally {
            if (previous == null) {
                System.clearProperty("quiz.ui.reduceMotion");
            } else {
                System.setProperty("quiz.ui.reduceMotion", previous);
            }
        }
    }

    private static void testAsyncArtworkAndCache() throws Exception {
        String previous = System.getProperty("quiz.assetsDir");
        try {
            System.clearProperty("quiz.assetsDir");
            checkAsyncArtworkAndCache();
        } finally {
            if (previous == null) {
                System.clearProperty("quiz.assetsDir");
            } else {
                System.setProperty("quiz.assetsDir", previous);
            }
        }
    }

    private static void checkAsyncArtworkAndCache() throws Exception {
        byte[] fixture = fixtureImage();
        URL classes = ArenaBackgroundPanel.class.getProtectionDomain().getCodeSource().getLocation();
        try (ArtworkClassLoader loader = new ArtworkClassLoader(classes, fixture)) {
            Class<?> panelType = loader.loadClass("quiz.ui.ArenaBackgroundPanel");
            Class<?> sceneType = loader.loadClass("quiz.ui.ArenaBackgroundPanel$Scene");
            Object authScene = sceneType.getEnumConstants()[0];
            Object lobbyScene = sceneType.getEnumConstants()[1];
            AtomicReference<JPanel> first = new AtomicReference<>();
            AtomicReference<JPanel> second = new AtomicReference<>();
            AtomicReference<JPanel> lobby = new AtomicReference<>();
            AtomicReference<Throwable> failure = new AtomicReference<>();
            AtomicReference<BufferedImage> fallback = new AtomicReference<>();

            SwingUtilities.invokeAndWait(() -> {
                try {
                    var constructor = panelType.getConstructor(LayoutManager.class, sceneType);
                    first.set((JPanel) constructor.newInstance(new BorderLayout(), authScene));
                    second.set((JPanel) constructor.newInstance(new BorderLayout(), authScene));
                    lobby.set((JPanel) constructor.newInstance(new BorderLayout(), lobbyScene));
                    first.get().setSize(640, 360);
                    second.get().setSize(640, 360);
                    lobby.get().setSize(900, 180);
                    fallback.set(render(first.get()));
                    check(!((Boolean) panelType.getMethod("isArtworkLoaded").invoke(first.get())),
                            "Scene can paint its fallback while artwork loads asynchronously");
                    check(!((Boolean) panelType.getMethod("isAnimationRunning").invoke(first.get())),
                            "Offscreen rendering starts no timer");
                } catch (Throwable problem) {
                    failure.set(problem);
                }
            });
            loader.releaseArtwork.countDown();
            if (failure.get() != null) {
                throw new AssertionError("Scene creation failed", failure.get());
            }

            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (System.nanoTime() < deadline) {
                boolean ready = (Boolean) panelType.getMethod("isArtworkLoaded").invoke(first.get())
                        && (Boolean) panelType.getMethod("isArtworkLoaded").invoke(second.get())
                        && (Boolean) panelType.getMethod("isArtworkLoaded").invoke(lobby.get());
                if (ready) {
                    break;
                }
                Thread.sleep(10);
            }
            check((Boolean) panelType.getMethod("isArtworkLoaded").invoke(first.get()),
                    "First scene receives decoded image");
            check((Boolean) panelType.getMethod("isArtworkLoaded").invoke(second.get()),
                    "Another scene receives the shared decoded image");
            check((Boolean) panelType.getMethod("isArtworkLoaded").invoke(lobby.get()),
                    "Lobby scene receives its own asset");
            check(loader.authLoads.get() == 1 && loader.lobbyLoads.get() == 1,
                    "Artwork is decoded only once per scene across component instances");
            check(loader.edtLoads.get() == 0, "Artwork I/O runs outside the Swing event thread");

            SwingUtilities.invokeAndWait(() -> {
                BufferedImage loaded = render(first.get());
                check(pixel(loaded, 320, 180) != pixel(fallback.get(), 320, 180),
                        "Loaded scene visibly uses the real image rather than its fallback");
                int imageColor = pixel(loaded, 320, 180);
                check(((imageColor >> 16) & 255) > 150 && ((imageColor >> 8) & 255) > 70,
                        "Image pixels survive the readable scene overlay");
                check(pixel(render(first.get()), 320, 180) == imageColor,
                        "Static repeated frames paint consistently");
                BufferedImage hero = render(lobby.get());
                check(((pixel(hero, 870, 90) >> 16) & 255) > 150,
                        "Shallow lobby banner displays the height-fitted right-side artwork");
                check(((pixel(hero, 30, 90) >> 16) & 255) < 80,
                        "Lobby banner preserves a quiet dark region for its left-side copy");
            });
        }
    }

    private static byte[] fixtureImage() throws IOException {
        BufferedImage image = new BufferedImage(64, 36, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(new java.awt.Color(250, 180, 20));
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        graphics.dispose();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(image, "png", bytes);
        return bytes.toByteArray();
    }

    private static BufferedImage render(JPanel panel) {
        BufferedImage image = new BufferedImage(panel.getWidth(), panel.getHeight(),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        panel.paint(graphics);
        graphics.dispose();
        return image;
    }

    private static int pixel(BufferedImage image, int x, int y) {
        return image.getRGB(x, y) & 0xFFFFFF;
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
        checks++;
    }

    private static final class FakeBackdrop extends ArenaBackgroundPanel {
        private boolean shown;

        FakeBackdrop() {
            super(new BorderLayout(), Scene.AUTH);
        }

        @Override
        public boolean isShowing() {
            return shown && isDisplayable();
        }

        void showForTest(boolean visible) {
            shown = visible;
            dispatchEvent(new HierarchyEvent(this, HierarchyEvent.HIERARCHY_CHANGED,
                    this, null, HierarchyEvent.SHOWING_CHANGED));
        }
    }

    /** Isolates the image cache and supplies actual PNG bytes entirely from memory. */
    private static final class ArtworkClassLoader extends URLClassLoader {
        final CountDownLatch releaseArtwork = new CountDownLatch(1);
        final AtomicInteger authLoads = new AtomicInteger();
        final AtomicInteger lobbyLoads = new AtomicInteger();
        final AtomicInteger edtLoads = new AtomicInteger();
        private final byte[] png;

        ArtworkClassLoader(URL classes, byte[] png) {
            super(new URL[] {classes}, TestArenaMotion.class.getClassLoader());
            this.png = png;
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (name.startsWith("quiz.ui.ArenaBackgroundPanel") || name.equals("quiz.ui.ArenaArtwork")) {
                synchronized (getClassLoadingLock(name)) {
                    Class<?> loaded = findLoadedClass(name);
                    if (loaded == null) {
                        loaded = findClass(name);
                    }
                    if (resolve) {
                        resolveClass(loaded);
                    }
                    return loaded;
                }
            }
            return super.loadClass(name, resolve);
        }

        @Override
        public InputStream getResourceAsStream(String name) {
            if (name.startsWith("assets/backgrounds/")) {
                if (SwingUtilities.isEventDispatchThread()) {
                    edtLoads.incrementAndGet();
                }
                if (name.endsWith("auth-arena-v1.png")) {
                    authLoads.incrementAndGet();
                } else if (name.endsWith("lobby-arena-v1.png")) {
                    lobbyLoads.incrementAndGet();
                }
                try {
                    if (!releaseArtwork.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Test artwork gate timed out");
                    }
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(interrupted);
                }
                return new ByteArrayInputStream(png);
            }
            return super.getResourceAsStream(name);
        }
    }
}
