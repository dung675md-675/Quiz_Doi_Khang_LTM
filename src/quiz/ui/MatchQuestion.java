package quiz.ui;

/**
 * MatchQuestion - Cấu trúc dữ liệu đại diện cho một câu hỏi thi đấu trong trận.
 * Hỗ trợ trắc nghiệm (MULTIPLE_CHOICE), đúng/sai (TRUE_FALSE) và kéo thả (DRAG_AND_DROP).
 */
public record MatchQuestion(
    String text,
    String[] choices,
    String explanation,
    String type,
    String[] blanks,
    java.util.Map<String,String> fillMap,
    int correctAnswer
) {
    public MatchQuestion {
        if (choices == null || (choices.length < 2 && !"FILL_IN".equalsIgnoreCase(type) && !"DRAG_AND_DROP".equalsIgnoreCase(type))) {
            throw new IllegalArgumentException("Mỗi câu hỏi phải có ít nhất 2 đáp án lựa chọn.");
        }
    }

    public MatchQuestion(String text, String[] choices) {
        this(text, choices, "", "MULTIPLE_CHOICE", null, null, -1);
    }

    public MatchQuestion(String text, String[] choices, String explanation) {
        this(text, choices, explanation, "MULTIPLE_CHOICE", null, null, -1);
    }

    public MatchQuestion(String text, String[] choices, String explanation, String type) {
        this(text, choices, explanation, type, null, null, -1);
    }

    public MatchQuestion(String text, String[] choices, String explanation, String type, String[] blanks, java.util.Map<String,String> fillMap) {
        this(text, choices, explanation, type, blanks, fillMap, -1);
    }

    public boolean isDragAndDrop() {
        return "DRAG_AND_DROP".equalsIgnoreCase(type) && blanks != null && fillMap != null;
    }

    public boolean isTrueFalse() {
        return "TRUE_FALSE".equalsIgnoreCase(type)
            || (choices != null && choices.length == 2 && ("Đúng".equalsIgnoreCase(choices[0]) || "True".equalsIgnoreCase(choices[0])));
    }

    public boolean isMultipleSelect() {
        return "MULTIPLE_SELECT".equalsIgnoreCase(type);
    }

    public boolean isFillIn() {
        return "FILL_IN".equalsIgnoreCase(type);
    }
}
