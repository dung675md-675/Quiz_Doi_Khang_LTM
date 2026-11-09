package quiz.ui;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MatchHistoryCache — Bộ nhớ đệm lưu trữ danh sách câu hỏi và câu trả lời của các trận đấu vừa diễn ra.
 * Cho phép người chơi nhấn vào thẻ lịch sử trong HistoryPanel / HistoryDialog để xem lại chi tiết
 * qua ReviewAnswersDialog mà không cần tạo dữ liệu giả.
 */
public final class MatchHistoryCache {

    public record CachedMatch(
            String matchKey,
            List<MatchQuestion> questions,
            int[] userAnswers,
            int[] correctAnswers,
            String[] fillInInputs,
            ResultDialog.PlayerStat myStat,
            ResultDialog.PlayerStat oppStat,
            String[] myOutcomeHistory,
            String[] oppOutcomeHistory,
            int[] myDeltaHistory,
            int[] oppDeltaHistory
    ) {
        public CachedMatch(
                String matchKey,
                List<MatchQuestion> questions,
                int[] userAnswers,
                int[] correctAnswers,
                String[] fillInInputs,
                ResultDialog.PlayerStat myStat,
                ResultDialog.PlayerStat oppStat) {
            this(matchKey, questions, userAnswers, correctAnswers, fillInInputs, myStat, oppStat, null, null, null, null);
        }
    }

    private static final Map<String, CachedMatch> CACHE = new ConcurrentHashMap<>();

    public static void cacheMatch(
            String key,
            List<MatchQuestion> questions,
            int[] userAnswers,
            int[] correctAnswers,
            String[] fillInInputs,
            ResultDialog.PlayerStat myStat,
            ResultDialog.PlayerStat oppStat) {
        cacheMatch(key, questions, userAnswers, correctAnswers, fillInInputs, myStat, oppStat, null, null, null, null);
    }

    public static void cacheMatch(
            String key,
            List<MatchQuestion> questions,
            int[] userAnswers,
            int[] correctAnswers,
            String[] fillInInputs,
            ResultDialog.PlayerStat myStat,
            ResultDialog.PlayerStat oppStat,
            String[] myOutcomeHistory,
            String[] oppOutcomeHistory,
            int[] myDeltaHistory,
            int[] oppDeltaHistory) {

        if (key != null && !key.isBlank() && questions != null && !questions.isEmpty()) {
            CachedMatch match = new CachedMatch(key.trim(), questions, userAnswers, correctAnswers, fillInInputs, myStat, oppStat, myOutcomeHistory, oppOutcomeHistory, myDeltaHistory, oppDeltaHistory);
            CACHE.put(normalizeKey(key), match);

            // Cũng lưu theo key đảo ngược nếu là định dạng "P1 score1–score2 P2"
            String norm = normalizeKey(key);
            CACHE.put(norm, match);
        }
    }

    public static CachedMatch getMatch(String key) {
        if (key == null || key.isBlank()) return null;
        CachedMatch direct = CACHE.get(normalizeKey(key));
        if (direct != null) return direct;

        // Thử tìm kiếm mờ theo scoreInfo
        String clean = normalizeKey(key);
        for (Map.Entry<String, CachedMatch> entry : CACHE.entrySet()) {
            if (entry.getKey().contains(clean) || clean.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    public static boolean hasMatch(String key) {
        return getMatch(key) != null;
    }

    public static String normalizeKey(String key) {
        if (key == null) return "";
        return key.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private MatchHistoryCache() {}
}

