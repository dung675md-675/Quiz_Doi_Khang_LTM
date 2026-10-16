package quiz.ui;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import javax.swing.*;

/**
 * ToastManager - Quản lý hiển thị các thông báo Toast trên LayeredPane của JFrame.
 * Đảm bảo thread-safe và tự động biến mất sau thời gian định sẵn.
 * Hỗ trợ Toast Stacking: Tối đa 3 toast cùng lúc.
 */
public final class ToastManager {

    private static final Map<JFrame, List<ToastContext>> activeToasts = new WeakHashMap<>();
    private static final int MAX_TOASTS = 3;
    private static final int TOAST_GAP = 10;
    private static final int START_Y = 20;

    private static class ToastContext {
        ToastNotification toast;
        Timer timer;
    }

    public static void show(JFrame frame, String message, ToastNotification.Type type) {
        if (frame == null || message == null || message.isBlank()) return;

        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> show(frame, message, type));
            return;
        }

        JLayeredPane layeredPane = frame.getLayeredPane();
        if (layeredPane == null) return;

        List<ToastContext> frameToasts = activeToasts.computeIfAbsent(frame, k -> new ArrayList<>());

        // Đóng toast cũ nhất nếu vượt quá MAX_TOASTS
        if (frameToasts.size() >= MAX_TOASTS) {
            ToastContext oldest = frameToasts.remove(0);
            oldest.timer.stop();
            layeredPane.remove(oldest.toast);
        }

        ToastNotification toast = new ToastNotification(message, type);
        Dimension size = toast.getPreferredSize();

        int x = (frame.getWidth() - size.width) / 2;
        int y = START_Y;

        // Tính toán vị trí Y dựa trên các toast hiện có
        for (ToastContext ctx : frameToasts) {
            y += ctx.toast.getHeight() + TOAST_GAP;
        }

        toast.setBounds(x, y, size.width, size.height);
        layeredPane.add(toast, JLayeredPane.POPUP_LAYER);
        
        // Di chuyển các toast khác lên? Không, toast mới ở dưới.
        // Thực tế toast mới sẽ xuất hiện ở vị trí Y thấp hơn.

        ToastContext ctx = new ToastContext();
        ctx.toast = toast;

        // Tự hủy sau thời gian tùy loại
        int duration = resolveDuration(type);
        ctx.timer = new Timer(duration, e -> {
            frameToasts.remove(ctx);
            layeredPane.remove(toast);
            recalculatePositions(frame, frameToasts);
            layeredPane.repaint();
        });
        ctx.timer.setRepeats(false);
        ctx.timer.start();

        frameToasts.add(ctx);
        layeredPane.repaint();
    }

    private static void recalculatePositions(JFrame frame, List<ToastContext> frameToasts) {
        int currentY = START_Y;
        for (ToastContext ctx : frameToasts) {
            Rectangle bounds = ctx.toast.getBounds();
            // Animate or jump to new position
            ctx.toast.setBounds(bounds.x, currentY, bounds.width, bounds.height);
            currentY += bounds.height + TOAST_GAP;
        }
    }

    private static int resolveDuration(ToastNotification.Type type) {
        switch (type) {
            case SUCCESS: return 2500;
            case ERROR: return 5000;
            case WARNING: return 4000;
            case INFO:
            default: return 3000;
        }
    }

    public static void success(JFrame frame, String message) {
        show(frame, message, ToastNotification.Type.SUCCESS);
    }

    public static void error(JFrame frame, String message) {
        show(frame, message, ToastNotification.Type.ERROR);
    }

    public static void warning(JFrame frame, String message) {
        show(frame, message, ToastNotification.Type.WARNING);
    }

    public static void info(JFrame frame, String message) {
        show(frame, message, ToastNotification.Type.INFO);
    }

    private ToastManager() {}
}
