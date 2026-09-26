package quiz.ui;

/**
 * MatchQuestion - Cấu trúc dữ liệu đại diện cho một câu hỏi thi đấu trong trận.
 */
public record MatchQuestion(String text, String[] choices) {
    public MatchQuestion {
        if (choices == null || choices.length != 4) {
            throw new IllegalArgumentException("Mỗi câu hỏi phải có đúng 4 đáp án lựa chọn.");
        }
    }
}
