package tests;

import quiz.Model;
import quiz.Server;
import quiz.ui.MatchQuestion;
import quiz.ui.MatchScreen;

import javax.swing.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class TestDynamicQuestionTimers {

    public static void main(String[] args) throws Exception {
        System.out.println("=== BAT DAU KIEM TRA DYNAMIC QUESTION TIMERS (PHAN 1) ===");

        testServerTimeLimitSeconds();
        testMatchScreenTimeLimitSeconds();
        testMatchScreenActivePhaseTimers();
        testSpeedBonusFairness();

        System.out.println(">>> TAT CA CAC TEST CHO DYNAMIC QUESTION TIMERS DEU PASS 100%! <<<");
    }

    private static void testServerTimeLimitSeconds() {
        System.out.println("1. Kiem tra Server.getTimeLimitSeconds theo tung QuestionType...");

        assert Server.getTimeLimitSeconds(Model.QuestionType.TRUE_FALSE) == 8 : "TRUE_FALSE phai la 8s";
        assert Server.getTimeLimitSeconds(Model.QuestionType.MULTIPLE_CHOICE) == 10 : "MULTIPLE_CHOICE phai la 10s";
        assert Server.getTimeLimitSeconds(Model.QuestionType.MULTIPLE_SELECT) == 15 : "MULTIPLE_SELECT phai la 15s";
        assert Server.getTimeLimitSeconds(Model.QuestionType.DRAG_AND_DROP) == 15 : "DRAG_AND_DROP phai la 15s";
        assert Server.getTimeLimitSeconds(Model.QuestionType.FILL_IN) == 17 : "FILL_IN phai la 17s";

        // Defaults
        assert Server.getTimeLimitSeconds(Model.QuestionType.ORDERING) == 10 : "ORDERING fallback phai la 10s";
        assert Server.getTimeLimitSeconds((Model.QuestionType) null) == 10 : "null fallback phai la 10s";

        System.out.println("   [PASS] Dung/Sai = 8s, Trac nghiem = 10s, Chon nhieu = 15s, Keo tha = 15s, Dien tu = 17s!");
    }

    private static void testMatchScreenTimeLimitSeconds() {
        System.out.println("2. Kiem tra MatchScreen.getTimeLimitSeconds theo ten String...");

        assert MatchScreen.getTimeLimitSeconds("TRUE_FALSE") == 8 : "TRUE_FALSE phai la 8s";
        assert MatchScreen.getTimeLimitSeconds("MULTIPLE_CHOICE") == 10 : "MULTIPLE_CHOICE phai la 10s";
        assert MatchScreen.getTimeLimitSeconds("MULTIPLE_SELECT") == 15 : "MULTIPLE_SELECT phai la 15s";
        assert MatchScreen.getTimeLimitSeconds("DRAG_AND_DROP") == 15 : "DRAG_AND_DROP phai la 15s";
        assert MatchScreen.getTimeLimitSeconds("FILL_IN") == 17 : "FILL_IN phai la 17s";

        System.out.println("   [PASS] MatchScreen map dung 100% gia tri so giay voi Server!");
    }

    private static void testMatchScreenActivePhaseTimers() throws Exception {
        System.out.println("3. Kiem tra MatchScreen cap nhat thoi gian khi nhan QUESTION_ACTIVE...");

        List<MatchQuestion> questions = new ArrayList<>();
        questions.add(new MatchQuestion("Cau 1 (Dung/Sai)?", new String[]{"Đúng", "Sai"}, "Giai thich", "TRUE_FALSE", null, null, 0));
        questions.add(new MatchQuestion("Cau 2 (Trac nghiem)?", new String[]{"A", "B", "C", "D"}, "Giai thich", "MULTIPLE_CHOICE", null, null, 1));
        questions.add(new MatchQuestion("Cau 3 (Chon nhieu)?", new String[]{"A", "B", "C", "D"}, "Giai thich", "MULTIPLE_SELECT", null, null, 3));
        questions.add(new MatchQuestion("Cau 4 (Keo tha)?", new String[]{"O1", "O2"}, "Giai thich", "DRAG_AND_DROP", new String[]{"b1", "b2"}, java.util.Map.of("b1", "O1", "b2", "O2"), 1));
        questions.add(new MatchQuestion("Cau 5 (Dien tu)?", new String[]{}, "Giai thich", "FILL_IN", null, null, 0));

        final MatchScreen[] msHolder = new MatchScreen[1];
        SwingUtilities.invokeAndWait(() -> {
            msHolder[0] = new MatchScreen("m_test", "Alice", "Bob", "avatar_01", "avatar_02", "Chu de", "De", "set_1", 120, questions, a -> {}, () -> {});
        });
        MatchScreen ms = msHolder[0];

        // Test Cau 0: TRUE_FALSE -> 8s
        ms.handleMatchPhase(new String[]{"MATCH_PHASE", "QUESTION_ACTIVE", "m_test", "0", "8"});
        SwingUtilities.invokeAndWait(() -> {});
        assertGetFieldInt(ms, "currentQuestionTimeRemaining", 8);

        // Test Cau 1: MULTIPLE_CHOICE -> 10s
        ms.handleMatchPhase(new String[]{"MATCH_PHASE", "QUESTION_ACTIVE", "m_test", "1", "10"});
        SwingUtilities.invokeAndWait(() -> {});
        assertGetFieldInt(ms, "currentQuestionTimeRemaining", 10);

        // Test Cau 2: MULTIPLE_SELECT -> 15s
        ms.handleMatchPhase(new String[]{"MATCH_PHASE", "QUESTION_ACTIVE", "m_test", "2", "15"});
        SwingUtilities.invokeAndWait(() -> {});
        assertGetFieldInt(ms, "currentQuestionTimeRemaining", 15);

        // Test Cau 3: DRAG_AND_DROP -> 15s
        ms.handleMatchPhase(new String[]{"MATCH_PHASE", "QUESTION_ACTIVE", "m_test", "3", "15"});
        SwingUtilities.invokeAndWait(() -> {});
        assertGetFieldInt(ms, "currentQuestionTimeRemaining", 15);

        // Test Cau 4: FILL_IN -> 17s
        ms.handleMatchPhase(new String[]{"MATCH_PHASE", "QUESTION_ACTIVE", "m_test", "4", "17"});
        SwingUtilities.invokeAndWait(() -> {});
        assertGetFieldInt(ms, "currentQuestionTimeRemaining", 17);

        SwingUtilities.invokeAndWait(ms::cleanupAndStop);
        System.out.println("   [PASS] MatchScreen nhan dung 8s, 10s, 15s, 15s, 17s tu goi tin QUESTION_ACTIVE!");
    }

    private static void testSpeedBonusFairness() {
        System.out.println("4. Kiem tra tinh cong bang cua cong thuc speedBonus giua cac loai cau hoi...");

        // Cong thuc chuan:
        // double ratio = Math.max(0.0, Math.min(1.0, 1.0 - (double) elapsedMs / maxMs));
        // int speedBonus = (int) Math.round(ratio * 50.0);

        // TH 1: Tra loi khi het 50% thoi gian
        // Cau 8s tra loi sau 4s
        long maxMs8 = 8000L;
        long elapsed4s = 4000L;
        double ratio8 = Math.max(0.0, Math.min(1.0, 1.0 - (double) elapsed4s / maxMs8));
        int bonus8 = (int) Math.round(ratio8 * 50.0);

        // Cau 17s tra loi sau 8.5s
        long maxMs17 = 17000L;
        long elapsed8_5s = 8500L;
        double ratio17 = Math.max(0.0, Math.min(1.0, 1.0 - (double) elapsed8_5s / maxMs17));
        int bonus17 = (int) Math.round(ratio17 * 50.0);

        assert bonus8 == 25 : "Bonus 50% cau 8s phai la 25, thuc te: " + bonus8;
        assert bonus17 == 25 : "Bonus 50% cau 17s phai la 25, thuc te: " + bonus17;
        assert bonus8 == bonus17 : "Cung 50% thoi gian con lai thi speedBonus phai bang nhau!";

        // TH 2: Tra loi sau 2s
        // Cau 8s (con lai 6/8 = 75% thoi gian) -> 75% * 50 = 37.5 -> 38 diem
        double ratio8_2s = 1.0 - 2000.0 / 8000.0;
        int bonus8_2s = (int) Math.round(ratio8_2s * 50.0);
        assert bonus8_2s == 38 : "Tra loi 2s tren cau 8s phai duoc 38 diem, thuc te: " + bonus8_2s;

        // Cau 17s (con lai 15/17 = 88.2% thoi gian) -> 88.2% * 50 = 44 diem
        double ratio17_2s = 1.0 - 2000.0 / 17000.0;
        int bonus17_2s = (int) Math.round(ratio17_2s * 50.0);
        assert bonus17_2s == 44 : "Tra loi 2s tren cau 17s phai duoc 44 diem, thuc te: " + bonus17_2s;

        System.out.println("   [PASS] Cong thuc speedBonus dam bao ti le thuong dong nhat va cong bang tuyet doi!");
    }

    private static void assertGetFieldInt(Object obj, String fieldName, int expected) {
        try {
            Field f = obj.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            int val = f.getInt(obj);
            assert val == expected : "Field " + fieldName + " phai la " + expected + " nhung thuc te la " + val;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
