package tests;

import quiz.ui.MatchQuestion;
import quiz.ui.MatchScreen;
import quiz.ui.TacticalActionBar;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class TestPointsDesyncFix {

    public static void main(String[] args) {
        System.out.println("=== KIEM TRA FIX LOI DIEM SO VA TACTICAL ACTION BAR ===");

        List<MatchQuestion> mockQuestions = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            mockQuestions.add(new MatchQuestion(
                    "Cau hoi " + (i + 1),
                    new String[]{"A", "B", "C", "D"},
                    "Giai thich"
            ));
        }

        SwingUtilities.invokeLater(() -> {
            try {
                MatchScreen screen = new MatchScreen(
                        "MATCH-999", "manh", "admin",
                        "Tong hop", "De", "SET1", 60,
                        mockQuestions,
                        answers -> {},
                        () -> {}
                );

                TacticalActionBar bar = screen.getTacticalActionBar();
                assertCondition(bar != null, "TacticalActionBar phai ton tai");

                // Ban dau: 0 diem, myScore = 0
                assertCondition(!bar.getAttackButton().isEnabled(), "0 diem: Attack phai bi disable");
                assertCondition(!bar.getStarButton().isEnabled(), "0 diem: Star phai bi disable");
                assertCondition(bar.getDefendButton().isEnabled(), "0 diem: Defend phai duoc enable");

                // Tai cau 8: nguoi choi co 4 cau dung (myScore = 4), nhung tong diem la 513 (myPts = 513)
                // Nhu trong anh nguoi dung chup: 513d (4 dung)
                screen.updateScores(4, 4, 513, 518, 2, 2);

                System.out.println("Diem hien tai trong screen: myScore=" + screen.getMyScore() + ", myPoints=" + screen.getMyPoints() + ", effective=" + screen.getEffectivePoints());
                assertCondition(bar.getAttackButton().isEnabled(), "513 diem: Nut Tan Cong (2) PHAI DUOC BAT!");
                assertCondition(bar.getStarButton().isEnabled(), "513 diem: Nut Ngoi Sao (1) PHAI DUOC BAT!");
                assertCondition(bar.getDefendButton().isEnabled(), "513 diem: Nut Phong Thu (2) PHAI DUOC BAT!");

                // Sang cau hoi moi (startQuestionActivePhase)
                screen.startQuestionActivePhase(7, 10); // Cau 8
                assertCondition(bar.getAttackButton().isEnabled(), "Sang cau moi voi 513 diem: Tan Cong van phai BAT!");
                assertCondition(bar.getStarButton().isEnabled(), "Sang cau moi voi 513 diem: Ngoi Sao van phai BAT!");
                assertCondition(bar.getDefendButton().isEnabled(), "Sang cau moi: Phong Thu van phai BAT!");

                // Khi server gui TACTICAL_CONFIRM
                screen.handleTacticalConfirm("ATTACK", 2, 2, 1);
                assertCondition("ATTACK".equals(bar.getActiveSkill()), "Skill active phai la ATTACK");
                assertCondition(bar.getAttackButton().isEnabled(), "Attack dang active thi phai bam duoc de CANCEL");

                // Khi server gui TACTICAL_STATUS
                screen.handleTacticalStatus(2, 2, 1, "ATTACK", "NONE");
                assertCondition(bar.getStarButton().isEnabled(), "Ngoi sao van bat vi co 513 diem");

                System.out.println(">>> TAT CA CAC TEST KIEM TRA DESYNC DIEM SO DA PASS 100%! <<<");
                System.exit(0);
            } catch (Throwable t) {
                t.printStackTrace();
                System.exit(1);
            }
        });
    }

    private static void assertCondition(boolean condition, String msg) {
        if (!condition) {
            System.err.println("TEST THAT BAI: " + msg);
            throw new AssertionError(msg);
        }
    }
}
