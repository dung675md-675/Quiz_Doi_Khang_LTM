package quiz;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Security - Cung cấp cơ chế băm mật khẩu an toàn chuẩn mật mã học (SHA-256 với Salt ngẫu nhiên):
 * - Hạn chế tối đa nguy cơ lộ mật khẩu lưu trong cơ sở dữ liệu / file serialization.
 * - Hỗ trợ tương thích ngược (Backward Compatibility) với các tài khoản tạo bằng văn bản thuần trước đó,
 *   tự động nâng cấp (re-hash) lên định dạng bảo mật khi người dùng đăng nhập thành công.
 */
public final class Security {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final HexFormat HEX = HexFormat.of();

    /**
     * Băm mật khẩu người dùng với salt ngẫu nhiên 16 bytes.
     * @return Chuỗi định dạng "salt:sha256Hex"
     */
    public static String hashPassword(String password) {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        byte[] hash = computeHash(password, salt);
        return HEX.formatHex(salt) + ":" + HEX.formatHex(hash);
    }

    /**
     * Kiểm tra tính đúng đắn của mật khẩu người dùng nhập vào.
     * @param inputPassword Mật khẩu người dùng nhập
     * @param storedHash Mật khẩu lưu trữ trong hệ thống (dạng băm salt:hash hoặc plain text cũ)
     * @return true nếu mật khẩu khớp
     */
    public static boolean verifyPassword(String inputPassword, String storedHash) {
        if (inputPassword == null || storedHash == null) {
            return false;
        }

        // Trường hợp mật khẩu cũ chưa băm
        if (!storedHash.contains(":")) {
            return storedHash.equals(inputPassword);
        }

        try {
            String[] parts = storedHash.split(":", 2);
            byte[] salt = HEX.parseHex(parts[0]);
            byte[] expectedHash = HEX.parseHex(parts[1]);
            byte[] actualHash = computeHash(inputPassword, salt);

            // So sánh trong thời gian không đổi (Constant-time comparison) chống tấn công Timing Attack
            return MessageDigest.isEqual(expectedHash, actualHash);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Kiểm tra xem mật khẩu có thuộc định dạng cũ (chưa băm) để nâng cấp hay không.
     */
    public static boolean isLegacy(String storedHash) {
        return storedHash != null && !storedHash.contains(":");
    }

    private static byte[] computeHash(String password, byte[] salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            return md.digest(password.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    private Security() {}
}
