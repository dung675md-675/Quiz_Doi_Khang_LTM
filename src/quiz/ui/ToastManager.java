package quiz.ui;

import java.awt.*;
import javax.swing.*;

/**
 * ToastManager - Quản lý hiển thị các thông báo Toast trên LayeredPane của JFrame.
 * Đảm bảo thread-safe và tự động biến mất sau thời gian định sẵn.
 */
public final class ToastManager {

    private static final int DISPLAY_DURATION_MS = 3200;

    public static void show(JFrame frame, String message, ToastNotification.Type type) {
        if (frame == null || message == null || message.isBlank()) return;

        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> show(frame, message, type));
            return;
        }

        JLayeredPane layeredPane = frame.getLayeredPane();
        if (layeredPane == null) return;

        ToastNotification toast = new ToastNotification(message, type);
        Dimension size = toast.getPreferredSize();

        int x = (frame.getWidth() - size.width) / 2;
        int y = 20; // Xuất hiện cách đỉnh cửa sổ 20px
        toast.setBounds(x, y, size.width, size.height);

        layeredPane.add(toast, JLayeredPane.POPUP_LAYER);
        layeredPane.repaint();

        // Tự hủy sau DISPLAY_DURATION_MS
        Timer timer = new Timer(DISPLAY_DURATION_MS, e -> {
            layeredPane.remove(toast);
            layeredPane.repaint();
        });
        timer.setRepeats(false);
        timer.start();
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
