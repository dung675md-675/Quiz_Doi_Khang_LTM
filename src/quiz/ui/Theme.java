package quiz.ui;

import java.awt.*;
import javax.swing.*;

/**
 * Quiz Arena - Design System Tokens & Helpers
 * Áp dụng bảng màu Kahoot-inspired palette và quy tắc UX theo hướng dẫn thiết kế.
 */
public final class Theme {

    // ==========================================
    // 1. BẢNG MÀU CỐT LÕI (Core Modern Quiz Palette)
    // ==========================================
    public static final Color RED = new Color(0xE1, 0x1D, 0x48);           // #E11D48 - Đáp án A / Lỗi / Cảnh báo (Rose-600)
    public static final Color BLUE = new Color(0x25, 0x63, 0xEB);          // #2563EB - Đáp án B / Hành động chính (Blue-600)
    public static final Color BLUE_HOVER = new Color(0x1D, 0x4E, 0xD8);    // #1D4ED8 - Hover nút chính (Blue-700)
    public static final Color YELLOW = new Color(0xD9, 0x77, 0x06);        // #D97706 - Đáp án C / Cảnh báo / Hổ phách (Amber-600)
    public static final Color GREEN = new Color(0x05, 0x96, 0x69);         // #059669 - Đáp án D / Đúng / Thành công (Emerald-600)
    public static final Color PURPLE = new Color(0x7C, 0x3A, 0xED);        // #7C3AED - Accent / Rank / Selected (Violet-600)
    public static final Color PURPLE_HOVER = new Color(0x6D, 0x28, 0xD9);  // #6D28D9 - Hover cho nút tím

    // Nền & Phân lớp sáng/tối
    public static final Color DEEP_NAVY = new Color(0x0F, 0x17, 0x2A);     // #0F172A - Nền chính Battle Screen (Slate-900)
    public static final Color NAVY_CARD = new Color(0x1E, 0x29, 0x3B);     // #1E293B - Nền card trên dark UI (Slate-800)
    public static final Color NAVY_CARD_HOVER = new Color(0x33, 0x41, 0x55);// #334155 - Hover card dark UI (Slate-700)
    public static final Color OFF_WHITE = new Color(0xF8, 0xFA, 0xFC);     // #F8FAFC - Nền sáng / Vùng nội dung (Slate-50)
    public static final Color BG_LIGHT = new Color(0xF8, 0xFA, 0xFC);      // #F8FAFC - Alias nền sáng
    public static final Color WHITE = new Color(0xFF, 0xFF, 0xFF);         // #FFFFFF - Nền card sáng / Chữ trên nền đậm
    public static final Color SURFACE_LIGHT = new Color(0xFF, 0xFF, 0xFF);  // #FFFFFF - Bề mặt thẻ sáng
    public static final Color SURFACE_HOVER = new Color(0xF1, 0xF5, 0xF9);  // #F1F5F9 - Bề mặt hover sáng (Slate-100)
    public static final Color DARK_TEXT = new Color(0x0F, 0x17, 0x2A);     // #0F172A - Chữ chính trên nền sáng (Slate-900)
    public static final Color TEXT_BODY = new Color(0x33, 0x41, 0x55);     // #334155 - Chữ thân bài (Slate-700)
    public static final Color MUTED_GRAY = new Color(0x64, 0x74, 0x8B);    // #64748B - Text phụ / Metadata (Slate-500)
    public static final Color DISABLED = new Color(0x94, 0xA3, 0xB8);      // #94A3B8 - Không thể thao tác (Slate-400)
    public static final Color BORDER_LIGHT = new Color(0xE2, 0xE8, 0xF0);  // #E2E8F0 - Viền card sáng (Slate-200)
    public static final Color BORDER_SUBTLE = new Color(0xCB, 0xD5, 0xE1); // #CBD5E1 - Viền card hover/đậm hơn (Slate-300)
    public static final Color BORDER_DARK = new Color(0x33, 0x41, 0x55);   // #334155 - Viền card dark (Slate-700)

    // ==========================================
    // 1.1 BẢNG MÀU TƯƠI SÁNG (Vibrant Palette)
    // ==========================================
    public static final Color INDIGO = new Color(0x63, 0x66, 0xF1);        // #6366F1 - Indigo hiện đại (Indigo-500)
    public static final Color INDIGO_DARK = new Color(0x4F, 0x46, 0xE5);   // #4F46E5 - Indigo đậm (Indigo-600)
    public static final Color INDIGO_LIGHT = new Color(0xEE, 0xF2, 0xFF);  // #EEF2FF - Nền Indigo pastel
    public static final Color EMERALD = new Color(0x10, 0xB9, 0x81);       // #10B981 - Xanh ngọc / Xanh lá tươi
    public static final Color EMERALD_DARK = new Color(0x05, 0x96, 0x69);  // #059669 - Xanh ngọc đậm (Emerald-600)
    public static final Color EMERALD_LIGHT = new Color(0xEC, 0xFD, 0xF5); // #ECFDF5 - Nền xanh ngọc pastel
    public static final Color ORANGE = new Color(0xF9, 0x73, 0x16);        // #F97316 - Cam tươi rực rỡ
    public static final Color ORANGE_DARK = new Color(0xEA, 0x58, 0x0C);   // #EA580C - Cam đậm
    public static final Color AMBER = new Color(0xF5, 0x9E, 0x0B);         // #F59E0B - Vàng cam hổ phách
    public static final Color AMBER_LIGHT = new Color(0xFF, 0xFB, 0xEB);   // #FFFBEB - Nền cam vàng pastel
    public static final Color CYAN = new Color(0x06, 0xB6, 0xD4);          // #06B6D4 - Xanh lơ cyan tươi
    public static final Color SKY_BLUE = new Color(0x0E, 0xA5, 0xE9);      // #0EA5E9 - Xanh da trời tươi
    public static final Color SKY_LIGHT = new Color(0xEF, 0xF6, 0xFF);     // #EFF6FF - Nền xanh da trời pastel

    // Huân chương Rank (Medals)
    public static final Color GOLD = new Color(0xF5, 0x9E, 0x0B);          // #F59E0B - Vàng Top 1
    public static final Color SILVER = new Color(0x94, 0xA3, 0xB8);        // #94A3B8 - Bạc Top 2
    public static final Color BRONZE = new Color(0xD9, 0x77, 0x06);        // #D97706 - Đồng Top 3

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
