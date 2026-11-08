package tests;

import quiz.ui.MatchQuestion;
import quiz.ui.MatchScreen;
import quiz.ui.TacticalToastBanner;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class TestRevealTacticalBanner {

    public static void main(String[] args) {
        System.out.println("=== KIEM TRA THONG BAO KET QUA CHIEN THUAT TREN TOAST BANNER ===");

        try {
            TacticalToastBanner banner = new TacticalToastBanner();

            // 1. Kiem tra Steal Success
            banner.showCombatOutcome("ATTACK", "NONE", "STEAL_SUCCESS", "BEING_STOLEN", 50, -50, "Bob");
            assertCondition(banner.isVisible(), "Banner phai hien thi");
            assertCondition(banner.getMessageText().contains("CƯỚP ĐIỂM THÀNH CÔNG"), "Msg phai chua CƯỚP ĐIỂM THÀNH CÔNG: " + banner.getMessageText());

            // 2. Kiem tra Block Success
            banner.showCombatOutcome("DEFEND", "ATTACK", "BLOCK_SUCCESS", "BLOCKED", 0, 0, "Bob");
            assertCondition(banner.getMessageText().contains("HÓA GIẢI THÀNH CÔNG"), "Msg phai chua HÓA GIẢI THÀNH CÔNG: " + banner.getMessageText());

            // 3. Kiem tra Counter Success
            banner.showCombatOutcome("DEFEND", "ATTACK", "COUNTER_SUCCESS", "COUNTERED", 50, -50, "Bob");
            assertCondition(banner.getMessageText().contains("PHẢN ĐÒN XUẤT SẮC"), "Msg phai chua PHẢN ĐÒN XUẤT SẮC: " + banner.getMessageText());

            // 4. Kiem tra Bi Phan Don
            banner.showCombatOutcome("ATTACK", "DEFEND", "COUNTERED", "COUNTER_SUCCESS", -50, 50, "Bob");
            assertCondition(banner.getMessageText().contains("BỊ PHẢN ĐÒN"), "Msg phai chua BỊ PHẢN ĐÒN: " + banner.getMessageText());

            // 5. Kiem tra Star Success & Fail
            banner.showCombatOutcome("STAR", "NONE", "STAR_SUCCESS", "NONE", 280, 0, "Bob");
            assertCondition(banner.getMessageText().contains("NGÔI SAO HY VỌNG"), "Msg phai chua NGÔI SAO HY VỌNG: " + banner.getMessageText());

            banner.showCombatOutcome("STAR", "NONE", "STAR_FAIL", "NONE", -50, 0, "Bob");
            assertCondition(banner.getMessageText().contains("NGÔI SAO THẤT BẠI"), "Msg phai chua NGÔI SAO THẤT BẠI: " + banner.getMessageText());

            // 6. Kiem tra Khi ca 2 khong dung ky nang -> Banner an
            banner.showCombatOutcome("NONE", "NONE", "NONE", "NONE", 0, 0, "Bob");
            assertCondition(!banner.isVisible(), "Khong co ky nang thi banner phai an");

            // 7. Kiem tra goi qua MatchScreen.showQuestionRevealPhase
            List<MatchQuestion> mockQuestions = new ArrayList<>();
            mockQuestions.add(new MatchQuestion("Cau 1", new String[]{"A", "B", "C", "D"}, "Exp"));
            MatchScreen screen = new MatchScreen("M1", "Alice", "Bob", "Toan", "De", "SET1", 60, mockQuestions, ans -> {}, () -> {});
            TacticalToastBanner screenBanner = screen.getTacticalToastBanner();

            screen.showQuestionRevealPhase(0, 0, "Exp", 150, 100, 1, 0, 0, 1,
                    "ATTACK", "NONE", "STEAL_SUCCESS", "BEING_STOLEN", 50, -50);

            assertCondition(screenBanner.isVisible(), "Banner tren MatchScreen phai duoc hien thi");
            assertCondition(screenBanner.getMessageText().contains("CƯỚP ĐIỂM THÀNH CÔNG"), "Banner tren MatchScreen phai bao cuop thanh cong");

            // Sang cau hoi moi: banner phai tu dong an
            screen.startQuestionActivePhase(1, 10);
            assertCondition(!screenBanner.isVisible(), "Sang cau moi banner phai duoc an");

            System.out.println(">>> TAT CA TEST THONG BAO KET QUA CHIEN THUAT DEU PASS 100%! <<<");
            System.exit(0);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }

    private static void assertCondition(boolean cond, String msg) {
        if (!cond) {
            System.err.println("TEST THAT BAI: " + msg);
            throw new AssertionError(msg);
        }
    }
}

