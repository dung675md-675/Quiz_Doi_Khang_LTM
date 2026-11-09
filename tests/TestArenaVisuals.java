package quiz;

import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.Preferences;
import java.util.prefs.PreferencesFactory;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import quiz.ui.AuthScreen;
import quiz.ui.SoundManager;

/** Authentication callbacks and minimum-window layout checks, without opening a window. */
public final class TestArenaVisuals {
    private static int checks;

    public static void main(String[] args) throws Exception {
        System.setProperty("java.util.prefs.PreferencesFactory", MemoryFactory.class.getName());
        SoundManager.setMuted(true);
        AtomicReference<AuthScreen> holder = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> holder.set(new AuthScreen()));
        AuthScreen screen = holder.get();

        long deadline = System.nanoTime() + 10_000_000_000L;
        while (!screen.isArtworkLoaded() && System.nanoTime() < deadline) {
            Thread.sleep(20);
        }
        check(screen.isArtworkLoaded(), "Original login artwork is available");

        AtomicReference<Throwable> failure = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            try {
                testLayout(screen, 1080, 760, AuthScreen.Mode.LOGIN, "auth-login-preview.png");
                testLayout(screen, 884, 601, AuthScreen.Mode.LOGIN, "auth-login-min-preview.png");
                testLayout(screen, 884, 601, AuthScreen.Mode.REGISTER, "auth-register-min-preview.png");
                testLayout(screen, 744, 541, AuthScreen.Mode.LOGIN, "auth-login-team-min-preview.png");
                testLayout(screen, 744, 541, AuthScreen.Mode.REGISTER, "auth-register-team-min-preview.png");
                testLayout(screen, 1920, 1000, AuthScreen.Mode.LOGIN, "auth-login-large-preview.png");
                testCallbacks(screen);
                JToggleButton toggle = motionToggle(screen);
                check(toggle != null, "Motion setting is reachable");
                boolean enabled = screen.isMotionEnabled();
                toggle.doClick();
                check(screen.isMotionEnabled() != enabled, "Motion toggle changes the backdrop preference");
                check(!screen.isAnimationRunning(), "Offscreen authentication starts no timer");
                screen.setMotionEnabled(false);
            } catch (Throwable error) {
                failure.set(error);
            }
        });
        if (failure.get() != null) {
            throw new AssertionError("Visual regression failed", failure.get());
        }
        System.out.println("PASS TestArenaVisuals: " + checks + " layout and authentication checks");
    }

    private static void testLayout(AuthScreen screen, int width, int height,
            AuthScreen.Mode mode, String preview) throws Exception {
        screen.setMode(mode);
        screen.setSize(width, height);
        layout(screen);
        JComponent card = field(screen, "card", JComponent.class);
        Rectangle bounds = card.getBounds();
        check(bounds.x >= 0 && bounds.y >= 0
                        && bounds.x + bounds.width <= width && bounds.y + bounds.height <= height,
                mode + " card fits " + width + "x" + height);
        javax.swing.JLabel title = field(screen, "lblTitle", javax.swing.JLabel.class);
        check(title.getWidth() >= title.getFontMetrics(title.getFont()).stringWidth(title.getText()),
                "Authentication headline is not truncated");
        checkWelcomeHeadlines(screen);
        for (String name : new String[] {"txtUsername", "txtPassword", "btnSubmit"}) {
            checkInside(screen, card, field(screen, name, JComponent.class), name);
        }
        JComponent confirm = field(screen, "txtConfirmPassword", JComponent.class);
        check(confirm.isVisible() == (mode == AuthScreen.Mode.REGISTER), "Confirmation follows mode");
        if (confirm.isVisible()) {
            checkInside(screen, card, confirm, "confirmation");
        }
        for (String name : new String[] {"lblUserHeader", "lblPassHeader", "lblConfirmHeader",
                "txtUsername", "txtPassword", "txtConfirmPassword", "btnSubmit"}) {
            JComponent child = field(screen, name, JComponent.class);
            if (!child.isVisible()) continue;
            for (Component node = child; node != card; node = node.getParent()) {
                Container parent = node.getParent();
                Rectangle rectangle = node.getBounds();
                check(rectangle.x >= 0 && rectangle.y >= 0 && rectangle.width > 0 && rectangle.height > 0
                                && rectangle.x + rectangle.width <= parent.getWidth()
                                && rectangle.y + rectangle.height <= parent.getHeight(),
                        name + " is not clipped by any form container");
            }
        }
        if (argsWantPreview()) {
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();
            screen.printAll(graphics);
            graphics.dispose();
            File directory = new File("target");
            directory.mkdirs();
            ImageIO.write(image, "png", new File(directory, preview));
        }
    }

    private static boolean argsWantPreview() {
        return Boolean.getBoolean("quiz.test.previews");
    }

    private static void checkWelcomeHeadlines(Container parent) {
        for (Component child : parent.getComponents()) {
            if (child instanceof javax.swing.JLabel label && (label.getText().equals("Đấu trí hôm nay.")
                    || label.getText().equals("Tỏa sáng mỗi ngày."))) {
                check(label.getWidth() >= label.getFontMetrics(label.getFont()).stringWidth(label.getText()),
                        "Welcome headline fits after resizing");
            }
            if (child instanceof Container nested) checkWelcomeHeadlines(nested);
        }
    }

    private static void testCallbacks(AuthScreen screen) throws Exception {
        JTextField username = field(screen, "txtUsername", JTextField.class);
        JPasswordField password = field(screen, "txtPassword", JPasswordField.class);
        JPasswordField confirm = field(screen, "txtConfirmPassword", JPasswordField.class);
        JButton submit = field(screen, "btnSubmit", JButton.class);
        AtomicReference<String> login = new AtomicReference<>();
        AtomicReference<String> register = new AtomicReference<>();
        screen.setOnLogin((name, pass) -> login.set(name + ":" + pass));
        screen.setOnRegister((name, pass) -> register.set(name + ":" + pass));

        screen.setMode(AuthScreen.Mode.LOGIN);
        username.setText("");
        password.setText("valid-pass");
        submit.doClick();
        check(login.get() == null, "Empty username does not log in");
        username.setText("visual_user");
        password.setText("valid-pass");
        submit.doClick();
        check("visual_user:valid-pass".equals(login.get()), "Login callback preserves credentials");
        check(!submit.isEnabled(), "Login loading state disables submit");
        screen.showError("Test response");
        check(submit.isEnabled(), "Server response releases loading state");

        screen.setMode(AuthScreen.Mode.REGISTER);
        confirm.setText("different-pass");
        submit.doClick();
        check(register.get() == null, "Mismatched password confirmation is rejected");
        confirm.setText("valid-pass");
        submit.doClick();
        check("visual_user:valid-pass".equals(register.get()), "Register callback preserves credentials");
        screen.resetForReuse();
        check(username.getText().isEmpty() && password.getPassword().length == 0,
                "Logout reset clears authentication fields");
        check(submit.isEnabled(), "Logout reset restores submit");
    }

    private static void checkInside(JComponent screen, JComponent card,
            JComponent child, String name) {
        Rectangle local = SwingUtilities.convertRectangle(child.getParent(), child.getBounds(), card);
        check(child.getWidth() >= 200 && child.getHeight() >= 30, name + " remains usable");
        check(local.x >= 0 && local.y >= 0
                        && local.x + local.width <= card.getWidth()
                        && local.y + local.height <= card.getHeight(),
                name + " stays inside its card");
    }

    private static void layout(Container parent) {
        parent.doLayout();
        for (Component child : parent.getComponents()) {
            if (child instanceof Container nested && child.isVisible()) {
                layout(nested);
            }
        }
    }

    private static JToggleButton motionToggle(Container parent) {
        for (Component child : parent.getComponents()) {
            if (child instanceof JToggleButton toggle && toggle.getText().startsWith("Hiệu ứng:")) {
                return toggle;
            }
            if (child instanceof Container nested) {
                JToggleButton found = motionToggle(nested);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static <T> T field(Object owner, String name, Class<T> type) throws Exception {
        Field field = owner.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return type.cast(field.get(owner));
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
        checks++;
    }

    public static final class MemoryFactory implements PreferencesFactory {
        private final Preferences root = new MemoryPreferences(null, "");
        public Preferences systemRoot() { return root; }
        public Preferences userRoot() { return root; }
    }

    private static final class MemoryPreferences extends AbstractPreferences {
        private final java.util.Map<String, String> values = new java.util.HashMap<>();
        MemoryPreferences(AbstractPreferences parent, String name) { super(parent, name); }
        protected void putSpi(String key, String value) { values.put(key, value); }
        protected String getSpi(String key) { return values.get(key); }
        protected void removeSpi(String key) { values.remove(key); }
        protected void removeNodeSpi() { values.clear(); }
        protected String[] keysSpi() { return values.keySet().toArray(String[]::new); }
        protected String[] childrenNamesSpi() { return new String[0]; }
        protected AbstractPreferences childSpi(String name) { return new MemoryPreferences(this, name); }
        protected void syncSpi() { }
        protected void flushSpi() { }
    }
}
