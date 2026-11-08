package tests;

import quiz.ui.MatchHistoryCache;
import quiz.ui.MatchQuestion;
import quiz.ui.ResultDialog;
import quiz.ui.ReviewAnswersDialog;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class TestResultAndReviewTactical {

    public static void main(String[] args) {
        System.out.println("=== BAT DAU KIEM TRA RESULT & REVIEW TACTICAL (PHAN 5) ===");

        // Test 1: Kiểm tra ResultDialog hiển thị đúng 3 chỉ số Đối đầu (Head-to-Head)
        testResultDialogHeadToHead();

        // Test 2: Kiểm tra ReviewAnswersDialog hiển thị đầy đủ các huy hiệu (badges)
        testReviewAnswersBadges();

        // Test 3: Kiểm tra tính lưu trữ trong MatchHistoryCache
        testMatchHistoryCacheIntegration();

        System.out.println(">>> TAT CA CAC TEST CHO PHAN 5 DEU PASS 100%! <<<");
        System.exit(0);
    }

    private static void testResultDialogHeadToHead() {
        System.out.println("1. Kiem tra ResultDialog phan tich chi so Doi dau (atkWins, counters, stolen)...");
        String leftRaw = "PlayerA: 8 câu, 12400 ms;pts=850;streak=4;atkWins=2;counters=1;stolen=100";
        String rightRaw = "PlayerB: 6 câu, 18200 ms;pts=550;streak=2;atkWins=1;counters=0;stolen=50";

        List<MatchQuestion> questions = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            questions.add(new MatchQuestion("Cau " + (i + 1), new String[]{"A", "B", "C", "D"}, "Giai thich " + (i + 1)));
        }

        ResultDialog dlg = new ResultDialog(
                null, "PlayerA", "PlayerA",
                leftRaw, rightRaw,
                () -> {}, () -> {},
                questions, new int[10], new int[10],
                "avatar_01", "avatar_02", null
        );

        ResultDialog.PlayerStat p1 = dlg.getPlayer1();
        ResultDialog.PlayerStat p2 = dlg.getPlayer2();

        assertCondition(p1.attackWins() == 2, "PlayerA phai co 2 lan tan cong thanh cong, thuc te: " + p1.attackWins());
        assertCondition(p1.counterWins() == 1, "PlayerA phai co 1 lan phan don, thuc te: " + p1.counterWins());
        assertCondition(p1.pointsStolen() == 100, "PlayerA phai cuop 100 diem, thuc te: " + p1.pointsStolen());

        assertCondition(p2.attackWins() == 1, "PlayerB phai co 1 lan tan cong thanh cong, thuc te: " + p2.attackWins());
        assertCondition(p2.counterWins() == 0, "PlayerB phai co 0 lan phan don, thuc te: " + p2.counterWins());
        assertCondition(p2.pointsStolen() == 50, "PlayerB phai cuop 50 diem, thuc te: " + p2.pointsStolen());

        System.out.println("   [PASS] ResultDialog phan tich va hien thi Doi dau Head-to-Head chinh xac!");
    }

    private static void testReviewAnswersBadges() {
        System.out.println("2. Kiem tra ReviewAnswersDialog tao du cac loai badge chien thuat...");

        List<MatchQuestion> questions = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            questions.add(new MatchQuestion("Cau " + (i + 1), new String[]{"A", "B", "C", "D"}, "Giai thich " + (i + 1)));
        }

        String[] myOutcomes = new String[]{
                "STEAL_SUCCESS",    // Cau 1: [⚔️ +50đ Cướp điểm]
                "BLOCK_SUCCESS",    // Cau 2: [🛡️ Đã Hóa Giải]
                "COUNTER_SUCCESS",  // Cau 3: [🛡️ +50đ Phản đòn]
                "STAR_SUCCESS",     // Cau 4: [⭐ x2 Điểm: +280đ]
                "STAR_FAIL",        // Cau 5: [⭐ -50đ]
                "BLOCKED",          // Cau 6: [⚔️ Bị Hóa Giải]
                "COUNTERED",        // Cau 7: [⚔️ -50đ Bị Phản đòn]
                "BEING_STOLEN",     // Cau 8: [⚔️ -50đ Bị Cướp]
                "NONE",             // Cau 9: Khong co badge
                "NONE"              // Cau 10: Khong co badge
        };

        int[] myDeltas = new int[]{50, 0, 50, 280, -50, 0, 50, 50, 0, 0};

        ReviewAnswersDialog review = new ReviewAnswersDialog(
                null, questions, new int[10], new int[10], null,
                new ResultDialog.PlayerStat("PlayerA", 8, 12000, true, true),
                new ResultDialog.PlayerStat("PlayerB", 6, 15000, false, false),
                myOutcomes, null, myDeltas, null
        );

        ReviewAnswersDialog.TacticalBadge b0 = review.getTacticalBadgeAt(0);
        assertCondition(b0 != null && b0.getText().equals("[⚔️ +50đ Cướp điểm]"), "Badge 0 phai la [⚔️ +50đ Cướp điểm], nhan duoc: " + (b0 != null ? b0.getText() : "null"));

        ReviewAnswersDialog.TacticalBadge b1 = review.getTacticalBadgeAt(1);
        assertCondition(b1 != null && b1.getText().equals("[🛡️ Đã Hóa Giải]"), "Badge 1 phai la [🛡️ Đã Hóa Giải], nhan duoc: " + (b1 != null ? b1.getText() : "null"));

        ReviewAnswersDialog.TacticalBadge b2 = review.getTacticalBadgeAt(2);
        assertCondition(b2 != null && b2.getText().equals("[🛡️ +50đ Phản đòn]"), "Badge 2 phai la [🛡️ +50đ Phản đòn], nhan duoc: " + (b2 != null ? b2.getText() : "null"));

        ReviewAnswersDialog.TacticalBadge b3 = review.getTacticalBadgeAt(3);
        assertCondition(b3 != null && b3.getText().equals("[⭐ x2 Điểm: +280đ]"), "Badge 3 phai la [⭐ x2 Điểm: +280đ], nhan duoc: " + (b3 != null ? b3.getText() : "null"));

        ReviewAnswersDialog.TacticalBadge b4 = review.getTacticalBadgeAt(4);
        assertCondition(b4 != null && b4.getText().equals("[⭐ -50đ]"), "Badge 4 phai la [⭐ -50đ], nhan duoc: " + (b4 != null ? b4.getText() : "null"));

        ReviewAnswersDialog.TacticalBadge b5 = review.getTacticalBadgeAt(5);
        assertCondition(b5 != null && b5.getText().equals("[⚔️ Bị Hóa Giải]"), "Badge 5 phai la [⚔️ Bị Hóa Giải]");

        ReviewAnswersDialog.TacticalBadge b6 = review.getTacticalBadgeAt(6);
        assertCondition(b6 != null && b6.getText().equals("[⚔️ -50đ Bị Phản đòn]"), "Badge 6 phai la [⚔️ -50đ Bị Phản đòn]");

        ReviewAnswersDialog.TacticalBadge b7 = review.getTacticalBadgeAt(7);
        assertCondition(b7 != null && b7.getText().equals("[⚔️ -50đ Bị Cướp]"), "Badge 7 phai la [⚔️ -50đ Bị Cướp]");

        ReviewAnswersDialog.TacticalBadge b8 = review.getTacticalBadgeAt(8);
        assertCondition(b8 == null, "Cau 9 khong co su kien thi khong duoc co badge!");

        ReviewAnswersDialog.TacticalBadge b9 = review.getTacticalBadgeAt(9);
        assertCondition(b9 == null, "Cau 10 khong co su kien thi khong duoc co badge!");

        System.out.println("   [PASS] ReviewAnswersDialog tao day du tat ca cac loai badge dung 100%!");
    }

    private static void testMatchHistoryCacheIntegration() {
        System.out.println("3. Kiem tra luu tru lich su chien thuat trong MatchHistoryCache...");
        List<MatchQuestion> questions = new ArrayList<>();
        questions.add(new MatchQuestion("Cau 1", new String[]{"A", "B"}, "GT"));

        String[] myOut = new String[]{"STEAL_SUCCESS"};
        int[] myDeltas = new int[]{50};

        MatchHistoryCache.cacheMatch(
                "User1 1-0 User2", questions, new int[]{0}, new int[]{0}, null,
                new ResultDialog.PlayerStat("User1", 1, 1000, true, true),
                new ResultDialog.PlayerStat("User2", 0, 1000, false, false),
                myOut, null, myDeltas, null
        );

        MatchHistoryCache.CachedMatch cached = MatchHistoryCache.getMatch("User1 1-0 User2");
        assertCondition(cached != null, "Cached match khong duoc null");
        assertCondition(cached.myOutcomeHistory() != null && cached.myOutcomeHistory()[0].equals("STEAL_SUCCESS"), "Lich su chien thuat phai duoc luu trong cache");

        System.out.println("   [PASS] MatchHistoryCache hoat dong hoan hao!");
    }

    private static void assertCondition(boolean condition, String message) {
        if (!condition) {
            System.err.println("FAILED ASSERTION: " + message);
            throw new AssertionError(message);
        }
    }
}

