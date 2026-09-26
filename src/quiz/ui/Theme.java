package quiz.ui;

import java.awt.*;
import javax.swing.*;

/**
 * Quiz Arena - Design System Tokens & Helpers
 * Áp dụng bảng màu Kahoot-inspired palette và quy tắc UX theo hướng dẫn thiết kế.
 */
public final class Theme {

    // ==========================================
    // 1. BẢNG MÀU CỐT LÕI (Core Palette)
    // ==========================================
    public static final Color RED = new Color(0xE2, 0x1B, 0x3C);           // #E21B3C - Đáp án A / Lỗi / Cảnh báo
    public static final Color BLUE = new Color(0x13, 0x68, 0xCE);          // #1368CE - Đáp án B / Hành động chính
    public static final Color BLUE_HOVER = new Color(0x0F, 0x5C, 0xB8);    // #0F5CB8 - Hover nút chính
    public static final Color YELLOW = new Color(0xD8, 0x9E, 0x00);        // #D89E00 - Đáp án C / Sắp hết giờ / Cảnh báo
    public static final Color GREEN = new Color(0x26, 0x89, 0x0C);         // #26890C - Đáp án D / Đúng / Thành công
    public static final Color PURPLE = new Color(0x86, 0x4C, 0xBF);        // #864CBF - Accent phụ / Rank / Selected
    public static final Color PURPLE_HOVER = new Color(0x75, 0x3E, 0xAA);  // Hover cho nút tím

    public static final Color DEEP_NAVY = new Color(0x1F, 0x2A, 0x44);     // #1F2A44 - Nền chính Battle Screen
    public static final Color NAVY_CARD = new Color(0x2A, 0x37, 0x59);     // Nền card trên dark UI
    public static final Color OFF_WHITE = new Color(0xF7, 0xF7, 0xF7);     // #F7F7F7 - Nền sáng / Vùng nội dung
    public static final Color WHITE = new Color(0xFF, 0xFF, 0xFF);         // #FFFFFF - Nền card sáng / Chữ trên nền đậm
    public static final Color DARK_TEXT = new Color(0x25, 0x25, 0x25);     // #252525 - Chữ trên nền sáng / trên nền vàng
    public static final Color MUTED_GRAY = new Color(0x6B, 0x72, 0x80);    // #6B7280 - Text phụ / Metadata
    public static final Color DISABLED = new Color(0x9C, 0xA3, 0xAF);      // #9CA3AF - Không thể thao tác
    public static final Color BORDER_LIGHT = new Color(0xE5, 0xE7, 0xEB);  // Viền card sáng
    public static final Color BORDER_DARK = new Color(0x3B, 0x4B, 0x72);   // Viền card dark

    // ==========================================
    // 2. TYPOGRAPHY
    // ==========================================
    private static final String FONT_FAMILY = resolveFontFamily();

    public static final Font TITLE_XL = new Font(FONT_FAMILY, Font.BOLD, 24);
    public static final Font TITLE = new Font(FONT_FAMILY, Font.BOLD, 18);
    public static final Font SUBTITLE = new Font(FONT_FAMILY, Font.BOLD, 15);
    public static final Font BODY = new Font(FONT_FAMILY, Font.PLAIN, 13);
    public static final Font BODY_BOLD = new Font(FONT_FAMILY, Font.BOLD, 13);
    public static final Font SMALL = new Font(FONT_FAMILY, Font.PLAIN, 11);
    public static final Font SMALL_BOLD = new Font(FONT_FAMILY, Font.BOLD, 11);
    public static final Font TIMER = new Font(FONT_FAMILY, Font.BOLD, 28);
    public static final Font BADGE = new Font(FONT_FAMILY, Font.BOLD, 12);

    private static String resolveFontFamily() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] fontNames = ge.getAvailableFontFamilyNames();
        for (String preferred : new String[]{"Segoe UI", "Inter", "Roboto", "Helvetica Neue", "Arial"}) {
            for (String f : fontNames) {
                if (f.equalsIgnoreCase(preferred)) return f;
            }
        }
        return Font.SANS_SERIF;
    }

    // ==========================================
    // 3. GRAPHICS 2D RENDERING HELPERS
    // ==========================================
    public static void setupAntiAliasing(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    private Theme() {}
}
