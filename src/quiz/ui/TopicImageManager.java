package quiz.ui;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

/**
 * TopicImageManager — Quản lý nạp ảnh, ánh xạ chủ đề, scale & center-crop (cover mode),
 * tự động cache và tạo ảnh dự phòng nếu thiếu file.
 */
public final class TopicImageManager {

    private static final TopicImageManager INSTANCE = new TopicImageManager();

    public static TopicImageManager getInstance() {
        return INSTANCE;
    }

    // Bảng ánh xạ chủ đề -> danh sách tên file ưu tiên trong thư mục Ảnh_Chủ_Đề
    private static final Map<String, List<String>> TOPIC_FILE_MAP = new LinkedHashMap<>();
    static {
        TOPIC_FILE_MAP.put("Kỳ quan thế giới", List.of(
            "Ky_Quan_The_Gioi.jpg", "Ky_Quan_The_Gioi.webp", "ky_quan_the_gioi.jpg"
        ));
        TOPIC_FILE_MAP.put("Địa danh và khám phá thế giới", List.of(
            "ĐỊA_DANH_VÀ_KHÁM_PHÁ_THẾ_GIỚI.jpg", "ĐỊA_DANH_VÀ_KHÁM_PHÁ_THẾ_GIỚI.webp", "dia_danh_va_kham_pha_the_gioi.jpg"
        ));
        TOPIC_FILE_MAP.put("Những con người làm nên lịch sử", List.of(
            "NHỮNG CON NGƯỜI LÀM NÊN LỊCH SỬ.jpg", "nhung_con_nguoi_lam_nen_lich_su.jpg"
        ));
        TOPIC_FILE_MAP.put("Sự thật thú vị về thế giới", List.of(
            "SỰ THẬT THÚ VỊ VỀ THẾ GIỚI.jpg", "SỰ THẬT THÚ VỊ VỀ THẾ GIỚI.webp", "su_that_thu_vi_ve_the_gioi.jpg"
        ));
        TOPIC_FILE_MAP.put("Dòng thời gian và những cột mốc", List.of(
            "DÒNG THỜI GIAN VÀ NHỮNG CỘT MỐC.jpg", "DÒNG THỜI GIAN VÀ NHỮNG CỘT MỐC.webp", "dong_thoi_gian_va_nhung_cot_moc.jpg"
        ));
        TOPIC_FILE_MAP.put("Vũ trụ và khám phá không gian", List.of(
            "VŨ TRỤ VÀ KHÁM PHÁ KHÔNG GIAN.jpg", "VŨ TRỤ VÀ KHÁM PHÁ KHÔNG GIAN.webp", "vu_tru_va_kham_pha_khong_gian.jpg"
        ));
        TOPIC_FILE_MAP.put("Văn hóa và di sản nhân loại", List.of(
            "VĂN HÓA VÀ DI SẢN NHÂN LOẠI.jpg", "van_hoa_va_di_san_nhan_loai.jpg"
        ));
        TOPIC_FILE_MAP.put("Những điều lớn nhất, nhỏ nhất và đầu tiên", List.of(
            "NHỮNG ĐIỀU LỚN NHẤT, NHỎ NHẤT VÀ ĐẦU TIÊN.jpg", "nhung_dieu_lon_nhat_nho_nhat_va_dau_tien.jpg"
        ));
        TOPIC_FILE_MAP.put("Toán học", List.of("toán_học.jpg", "toán_học.webp"));
        TOPIC_FILE_MAP.put("Vật lý", List.of("vật_lý.jpg"));
        TOPIC_FILE_MAP.put("Hóa học", List.of("hóa_học.jpg"));
        TOPIC_FILE_MAP.put("Sinh học", List.of("sinh_học.jpg", "sinh_học.webp"));
    }

    private File imageDir;
    private final Map<String, BufferedImage> rawImageCache = new ConcurrentHashMap<>();
    private final Map<String, BufferedImage> scaledCache = new ConcurrentHashMap<>();

    private TopicImageManager() {
        findImageDirectory();
    }

    /**
     * Tự động dò tìm thư mục Ảnh_Chủ_Đề trong project.
     */
    private void findImageDirectory() {
        String[] candidates = {
            "Ảnh_Chủ_Đề",
            "data/Ảnh_Chủ_Đề",
            "../Ảnh_Chủ_Đề",
            "../../Ảnh_Chủ_Đề"
        };
        for (String c : candidates) {
            File f = new File(c);
            if (f.exists() && f.isDirectory()) {
                imageDir = f;
                return;
            }
        }
        // Thử tìm theo wildcard nếu font chữ gõ không khớp
        File current = new File(".");
        File[] subdirs = current.listFiles(File::isDirectory);
        if (subdirs != null) {
            for (File d : subdirs) {
                String n = d.getName().toLowerCase();
                if (n.contains("anh") && n.contains("chu") || n.contains("chủ") || n.contains("ảnh")) {
                    imageDir = d;
                    return;
                }
            }
        }
    }

    /**
     * Lấy ảnh đã được center-crop và scale chuẩn hóa (cover mode) cho topic.
     * Trả về ảnh có kích thước đúng targetW x targetH.
     */
    public BufferedImage getTopicImage(String topic, int targetW, int targetH) {
        if (topic == null || topic.isBlank()) {
            topic = "Tổng hợp";
        }
        String cacheKey = topic + "_" + targetW + "x" + targetH;
        BufferedImage cached = scaledCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        BufferedImage raw = getRawImage(topic);
        BufferedImage processed;
        if (raw != null) {
            processed = cropAndScaleImage(raw, targetW, targetH);
        } else {
            processed = createFallbackImage(topic, targetW, targetH);
        }

        if (processed != null) {
            scaledCache.put(cacheKey, processed);
        }
        return processed;
    }

    /**
     * Nạp ảnh gốc của topic từ ổ đĩa (có cache để tránh đọc lại nhiều lần).
     */
    public BufferedImage getRawImage(String topic) {
        if (topic == null || topic.isBlank() || topic.equalsIgnoreCase("Tổng hợp") || topic.equalsIgnoreCase("Ngẫu nhiên")) {
            return null; // Sẽ dùng fallback graphic đẹp mắt riêng cho "Tổng hợp"
        }
        if (rawImageCache.containsKey(topic)) {
            return rawImageCache.get(topic);
        }

        BufferedImage img = loadFromDisk(topic);
        if (img != null) {
            rawImageCache.put(topic, img);
        }
        return img;
    }

    private BufferedImage loadFromDisk(String topic) {
        if (imageDir == null || !imageDir.exists()) {
            return null;
        }

        // 1. Thử theo bảng cấu hình TOPIC_FILE_MAP
        List<String> mappedFiles = TOPIC_FILE_MAP.get(topic);
        if (mappedFiles != null) {
            for (String fn : mappedFiles) {
                BufferedImage img = tryReadFile(new File(imageDir, fn));
                if (img != null) return img;
            }
        }

        // 2. Thử tìm file khớp chính xác tên topic (+ .jpg, .png, .webp)
        for (String ext : List.of(".jpg", ".png", ".jpeg", ".webp")) {
            File f = new File(imageDir, topic + ext);
            BufferedImage img = tryReadFile(f);
            if (img != null) return img;
        }

        // 3. Quét thư mục và so khớp mềm (loại bỏ dấu tiếng Việt, so sánh không phân biệt hoa thường)
        File[] allFiles = imageDir.listFiles();
        if (allFiles != null) {
            String normTopic = normalize(topic);
            for (File f : allFiles) {
                if (f.isFile()) {
                    String baseName = f.getName();
                    int dot = baseName.lastIndexOf('.');
                    if (dot > 0) baseName = baseName.substring(0, dot);
                    String normFile = normalize(baseName);
                    if (normFile.equals(normTopic) || normFile.contains(normTopic) || normTopic.contains(normFile)) {
                        BufferedImage img = tryReadFile(f);
                        if (img != null) return img;
                    }
                }
            }
        }

        return null;
    }

    private BufferedImage tryReadFile(File file) {
        if (!file.exists() || !file.isFile() || file.length() == 0) {
            return null;
        }
        try {
            BufferedImage img = ImageIO.read(file);
            if (img != null) {
                return img;
            }
            // Nếu ImageIO không đọc được (ví dụ .webp trên JDK thuần không có plugin),
            // kiểm tra xem có file .jpg hoặc .png cùng tên không
            String name = file.getName();
            int dot = name.lastIndexOf('.');
            if (dot > 0) {
                String base = name.substring(0, dot);
                File altJpg = new File(file.getParentFile(), base + ".jpg");
                if (altJpg.exists()) {
                    BufferedImage altImg = ImageIO.read(altJpg);
                    if (altImg != null) return altImg;
                }
                File altPng = new File(file.getParentFile(), base + ".png");
                if (altPng.exists()) {
                    BufferedImage altImg = ImageIO.read(altPng);
                    if (altImg != null) return altImg;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * BỔ SUNG BẮT BUỘC: Tự động điều chỉnh kích thước và cắt ảnh (Cover Center-Crop).
     * Phóng ảnh đủ để lấp đầy vùng hiển thị mà không kéo giãn méo tỷ lệ, sau đó cắt phần thừa từ trung tâm.
     */
    public static BufferedImage cropAndScaleImage(BufferedImage src, int targetW, int targetH) {
        if (src == null || targetW <= 0 || targetH <= 0) {
            return null;
        }

        int origW = src.getWidth();
        int origH = src.getHeight();
        if (origW <= 0 || origH <= 0) {
            return null;
        }

        double targetAspect = (double) targetW / targetH;
        double origAspect = (double) origW / origH;

        int cropX = 0;
        int cropY = 0;
        int cropW = origW;
        int cropH = origH;

        if (origAspect > targetAspect) {
            // Ảnh gốc rộng hơn tỷ lệ đích -> cắt bớt 2 bên trái/phải từ tâm
            cropW = (int) Math.round(origH * targetAspect);
            cropX = (origW - cropW) / 2;
        } else {
            // Ảnh gốc cao hơn tỷ lệ đích -> cắt bớt trên/dưới từ tâm
            cropH = (int) Math.round(origW / targetAspect);
            cropY = (origH - cropH) / 2;
        }

        // Đảm bảo không vượt quá kích thước ảnh gốc
        cropX = Math.max(0, Math.min(cropX, origW - 1));
        cropY = Math.max(0, Math.min(cropY, origH - 1));
        cropW = Math.max(1, Math.min(cropW, origW - cropX));
        cropH = Math.max(1, Math.min(cropH, origH - cropY));

        BufferedImage cropped = src.getSubimage(cropX, cropY, cropW, cropH);

        // Tạo ảnh mới với kích thước targetW x targetH và chất lượng render cao
        BufferedImage scaled = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = scaled.createGraphics();
        Theme.setupAntiAliasing(g2);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.drawImage(cropped, 0, 0, targetW, targetH, null);
        g2.dispose();

        return scaled;
    }

    /**
     * Tạo hình nền mỹ thuật phong cách game cho chủ đề "Tổng hợp" hoặc khi thiếu file ảnh.
     * Đảm bảo card không bao giờ bị lỗi hiển thị hay vỡ giao diện.
     */
    public static BufferedImage createFallbackImage(String topic, int targetW, int targetH) {
        BufferedImage img = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        Theme.setupAntiAliasing(g2);

        // Gradient nền chính theo phong cách game
        boolean isAllTopics = topic.equalsIgnoreCase("Tổng hợp") || topic.equalsIgnoreCase("Ngẫu nhiên");
        Color c1 = isAllTopics ? new Color(0x3B, 0x1A, 0x78) : new Color(0x11, 0x1E, 0x38);
        Color c2 = isAllTopics ? new Color(0x0E, 0x5C, 0xB8) : new Color(0x1F, 0x3A, 0x60);
        Color c3 = isAllTopics ? new Color(0x06, 0xB6, 0xD4) : new Color(0x3B, 0x4B, 0x72);

        GradientPaint gp = new GradientPaint(0, 0, c1, targetW, targetH, c2);
        g2.setPaint(gp);
        g2.fillRect(0, 0, targetW, targetH);

        // Họa tiết trang trí dạng sóng ánh sáng
        g2.setColor(new Color(c3.getRed(), c3.getGreen(), c3.getBlue(), 40));
        g2.fillOval(-targetW / 4, -targetH / 3, targetW, targetH * 2);
        g2.setColor(new Color(255, 255, 255, 20));
        g2.fillOval(targetW / 2, -targetH / 4, targetW, targetH);

        // Biểu tượng trung tâm
        if (isAllTopics) {
            int cx = targetW / 2;
            int cy = targetH / 2 - 6;

            // Vòng tròn hào quang
            g2.setColor(new Color(0x00, 0xE5, 0xFF, 120));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawOval(cx - 24, cy - 24, 48, 48);

            // Ngôi sao 5 cánh dạng vector
            int[] xPoints = new int[10];
            int[] yPoints = new int[10];
            for (int i = 0; i < 10; i++) {
                double angle = -Math.PI / 2 + i * Math.PI / 5;
                double rad = (i % 2 == 0) ? 14 : 6;
                xPoints[i] = (int) Math.round(cx + rad * Math.cos(angle));
                yPoints[i] = (int) Math.round(cy + rad * Math.sin(angle));
            }
            g2.setColor(new Color(0x00, 0xE5, 0xFF));
            g2.fillPolygon(xPoints, yPoints, 10);

            g2.setFont(Theme.SMALL_BOLD);
            String sub = "ALL TOPICS";
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(Color.WHITE);
            g2.drawString(sub, cx - fm.stringWidth(sub) / 2, cy + 30);
        } else {
            // Icon Quiz sách/la bàn cho chủ đề khác
            int cx = targetW / 2;
            int cy = targetH / 2 - 2;
            g2.setColor(new Color(0x00, 0xE5, 0xFF, 120));
            g2.drawOval(cx - 20, cy - 20, 40, 40);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 16));
            FontMetrics fm = g2.getFontMetrics();
            String sym = "QUIZ";
            g2.drawString(sym, cx - fm.stringWidth(sym) / 2, cy + fm.getAscent() / 2 - 1);
        }

        g2.dispose();
        return img;
    }

    /**
     * Chuẩn hóa chuỗi (loại bỏ dấu, ký tự đặc biệt, đưa về chữ thường).
     */
    private static String normalize(String input) {
        if (input == null) return "";
        String temp = Normalizer.normalize(input, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(temp).replaceAll("")
                .toLowerCase()
                .replaceAll("[^a-z0-9]", "");
    }
}
