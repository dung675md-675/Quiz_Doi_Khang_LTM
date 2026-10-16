package quiz;

/**
 * Hệ thống Rank dựa trên điểm số.
 * Tính toán trực tiếp từ score, không cần lưu vào DB.
 */
public enum Rank {
    IRON("Sắt", "", 0, 9),
    BRONZE("Đồng", "", 10, 19),
    SILVER("Bạc", "", 20, 29),
    GOLD("Vàng", "", 30, 49),
    DIAMOND("Kim Cương", "", 50, 79),
    MASTER("Cao Thủ", "", 80, 119),
    CHALLENGER("Thách Đấu", "", 120, Integer.MAX_VALUE);

    private final String displayName;
    private final String icon;
    private final int minScore;
    private final int maxScore;

    Rank(String displayName, String icon, int minScore, int maxScore) {
        this.displayName = displayName;
        this.icon = icon;
        this.minScore = minScore;
        this.maxScore = maxScore;
    }

    public String getDisplayName() { return displayName; }
    public String getIcon() { return icon; }
    public int getMinScore() { return minScore; }
    public int getMaxScore() { return maxScore; }

    /** Trả về tên hiển thị: ví dụ "Vàng", "Kim Cương" */
    public String getLabel() { return displayName; }

    /**
     * Tính rank từ điểm số.
     * @param score điểm số hiện tại (>= 0)
     * @return Rank tương ứng
     */
    public static Rank fromScore(int score) {
        for (Rank r : values()) {
            if (score >= r.minScore && score <= r.maxScore) return r;
        }
        return CHALLENGER;
    }

    /**
     * Tiến độ đến rank tiếp theo (0.0 – 1.0).
     * Dùng cho progress bar nếu cần.
     */
    public static double progressToNext(int score) {
        Rank current = fromScore(score);
        if (current == CHALLENGER) return 1.0;
        int range = current.maxScore - current.minScore + 1;
        int progress = score - current.minScore;
        return (double) progress / range;
    }
}
