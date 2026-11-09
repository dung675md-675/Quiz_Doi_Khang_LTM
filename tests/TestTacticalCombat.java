package tests;

import quiz.Server;
import quiz.Server.TacticalResolution;

public class TestTacticalCombat {

    public static void main(String[] args) {
        System.out.println("=== BAT DAU KIEM TRA HE THONG KY NANG CHIEN THUAT (PHAN 2) ===");

        testAttackSuccessNoDefend();
        testAttackBlockedByDefend();
        testCounterAttackOnFailedAttack();
        testAttackFailBothWrong();
        testVictimPointsLessThan50();
        testStarSuccessAndFail();
        testStarPointFloorAtZero();
        testDefendMissedNoAttack();
        testBothAttackSimultaneously();

        System.out.println(">>> TAT CA CAC TEST CHO TACTICAL COMBAT DEU PASS 100%! <<<");
    }

    /** 1. A dùng ATTACK và đúng, B không dùng DEFEND: A cướp 50 điểm của B */
    private static void testAttackSuccessNoDefend() {
        System.out.println("1. Kiem tra A attack dung vs B khong defend...");
        // A có 100 điểm, B có 100 điểm.
        // A trả lời đúng (base = 120), B trả lời đúng (base = 110).
        TacticalResolution res = Server.resolveTacticalCombat(
            100, 100,
            120, 110,
            true, true,
            "ATTACK", "NONE"
        );

        // A điểm ban đầu 100 + 120 = 220 + 50 (cướp) = 270
        // B điểm ban đầu 100 + 110 = 210 - 50 (bị cướp) = 160
        assert res.newPoints0() == 270 : "A phai dat 270 diem nhung la: " + res.newPoints0();
        assert res.newPoints1() == 160 : "B phai con 160 diem nhung la: " + res.newPoints1();
        assert "STEAL_SUCCESS".equals(res.outcome0()) : "A outcome phai la STEAL_SUCCESS!";
        assert "BEING_STOLEN".equals(res.outcome1()) : "B outcome phai la BEING_STOLEN!";
        assert res.tacticalDelta0() == 50 : "A delta phai la +50!";
        assert res.tacticalDelta1() == -50 : "B delta phai la -50!";

        System.out.println("   [PASS] A cuop thanh cong 50 diem cua B!");
    }

    /** 2. A dùng ATTACK và đúng, B dùng DEFEND: Hóa giải, A không cướp được điểm */
    private static void testAttackBlockedByDefend() {
        System.out.println("2. Kiem tra A attack dung vs B defend (Hoa giai)...");
        TacticalResolution res = Server.resolveTacticalCombat(
            80, 80,
            100, 100,
            true, true,
            "ATTACK", "DEFEND"
        );

        // A = 80 + 100 = 180, B = 80 + 100 = 180 (không ai bị cướp điểm)
        assert res.newPoints0() == 180 : "A phai giu nguyen 180 diem!";
        assert res.newPoints1() == 180 : "B phai giu nguyen 180 diem!";
        assert "BLOCKED".equals(res.outcome0()) : "A phai bi BLOCKED!";
        assert "BLOCK_SUCCESS".equals(res.outcome1()) : "B phai dat BLOCK_SUCCESS!";
        assert res.tacticalDelta0() == 0 : "A delta phai la 0!";
        assert res.tacticalDelta1() == 0 : "B delta phai la 0!";

        System.out.println("   [PASS] B hoa giai thanh cong don tan cong cua A, khong bi mat diem!");
    }

    /** 3. A dùng ATTACK và sai, B dùng DEFEND và đúng: Phản đòn, B cướp ngược 50 điểm của A */
    private static void testCounterAttackOnFailedAttack() {
        System.out.println("3. Kiem tra A attack sai vs B defend dung (Phan don)...");
        TacticalResolution res = Server.resolveTacticalCombat(
            100, 80,
            0, 120, // A sai -> 0 điểm, B đúng -> 120 điểm
            false, true,
            "ATTACK", "DEFEND"
        );

        // Điểm A trước combat: 100 + 0 = 100
        // Điểm B trước combat: 80 + 120 = 200
        // B phản đòn cướp 50 điểm của A:
        // A = 100 - 50 = 50
        // B = 200 + 50 = 250
        assert res.newPoints0() == 50 : "A phai con 50 diem sau khi bi phan don nhung la: " + res.newPoints0();
        assert res.newPoints1() == 250 : "B phai dat 250 diem nhung la: " + res.newPoints1();
        assert "COUNTERED".equals(res.outcome0()) : "A phai bi COUNTERED!";
        assert "COUNTER_SUCCESS".equals(res.outcome1()) : "B phai dat COUNTER_SUCCESS!";
        assert res.tacticalDelta0() == -50 : "A delta phai la -50!";
        assert res.tacticalDelta1() == 50 : "B delta phai la +50!";

        System.out.println("   [PASS] B phan don thanh cong, cuop nguoc 50 diem cua A!");
    }

    /** 4. A attack sai vs B defend sai: Không ai cướp được gì */
    private static void testAttackFailBothWrong() {
        System.out.println("4. Kiem tra A attack sai vs B defend sai...");
        TacticalResolution res = Server.resolveTacticalCombat(
            70, 70,
            0, 0,
            false, false,
            "ATTACK", "DEFEND"
        );

        assert res.newPoints0() == 70 : "A phai con nguyen 70 diem!";
        assert res.newPoints1() == 70 : "B phai con nguyen 70 diem!";
        assert "ATTACK_FAIL".equals(res.outcome0()) : "A outcome phai la ATTACK_FAIL!";
        assert "DEFEND_FAIL".equals(res.outcome1()) : "B outcome phai la DEFEND_FAIL!";

        System.out.println("   [PASS] Ca 2 cung sai thi khong kich hoat phan don!");
    }

    /** 5. Điểm của người bị cướp < 50: Chỉ cướp tối đa số điểm thực có, sàn điểm = 0 */
    private static void testVictimPointsLessThan50() {
        System.out.println("5. Kiem tra diem nguoi bi cuop < 50 (vi du: B co 30 diem)...");
        // A có 50 điểm, B có 30 điểm.
        // A đúng (100 base), B sai (0 base).
        // Điểm B trước combat = 30. A chỉ cướp được tối đa 30 điểm!
        TacticalResolution res = Server.resolveTacticalCombat(
            50, 30,
            100, 0,
            true, false,
            "ATTACK", "NONE"
        );

        // A = 50 + 100 + 30 = 180
        // B = 30 - 30 = 0 (khong bi am)
        assert res.newPoints0() == 180 : "A phai la 180 nhung la: " + res.newPoints0();
        assert res.newPoints1() == 0 : "B phai ve 0 nhung la: " + res.newPoints1();
        assert res.tacticalDelta0() == 30 : "A chi duoc +30 vi B chi co 30!";
        assert res.tacticalDelta1() == -30 : "B bi tru dung 30!";

        System.out.println("   [PASS] Chi cuop dung 30 diem thuc co cua B, san diem o muc 0!");
    }

    /** 6. Ngôi sao hy vọng: Đúng x2 điểm câu đó; Sai trừ 50 điểm */
    private static void testStarSuccessAndFail() {
        System.out.println("6. Kiem tra Ngoi sao hy vong: dung x2, sai tru 50...");

        // 6.1 Đúng: baseEarned = 125 -> nhận 125 * 2 = 250 điểm
        TacticalResolution resCorrect = Server.resolveTacticalCombat(
            100, 100,
            125, 100,
            true, true,
            "STAR", "NONE"
        );
        assert resCorrect.newPoints0() == 350 : "A phai dat 100 + 250 = 350 nhung la: " + resCorrect.newPoints0();
        assert "STAR_SUCCESS".equals(resCorrect.outcome0());
        assert resCorrect.tacticalDelta0() == 125 : "Delta phai la +125 (phan x2 them vao)!";

        // 6.2 Sai: bị phạt -50 điểm
        TacticalResolution resWrong = Server.resolveTacticalCombat(
            100, 100,
            0, 100,
            false, true,
            "STAR", "NONE"
        );
        assert resWrong.newPoints0() == 50 : "A phai con 100 - 50 = 50 nhung la: " + resWrong.newPoints0();
        assert "STAR_FAIL".equals(resWrong.outcome0());
        assert resWrong.tacticalDelta0() == -50 : "Delta phai la -50!";

        System.out.println("   [PASS] Star dung duoc x2 diem cau, sai bi tru dung 50 diem!");
    }

    /** 7. Sàn điểm 0 khi dùng Ngôi sao sai nhưng điểm < 50 */
    private static void testStarPointFloorAtZero() {
        System.out.println("7. Kiem tra san diem khong bi am khi Star that bai voi diem < 50...");
        TacticalResolution res = Server.resolveTacticalCombat(
            30, 100,
            0, 100,
            false, true,
            "STAR", "NONE"
        );
        assert res.newPoints0() == 0 : "A co 30 diem bi tru 50 phai ve 0 chu khong the -20!";
        assert res.tacticalDelta0() == -30 : "Delta bi tru dung 30 diem!";

        System.out.println("   [PASS] San diem khong bao gio bi am!");
    }

    /** 8. B dùng DEFEND mà A không tấn công: DEFEND không tác dụng và là DEFEND_MISSED */
    private static void testDefendMissedNoAttack() {
        System.out.println("8. Kiem tra B dung Defend nhung A khong attack (DEFEND_MISSED)...");
        TacticalResolution res = Server.resolveTacticalCombat(
            100, 100,
            100, 100,
            true, true,
            "NONE", "DEFEND"
        );

        assert "DEFEND_MISSED".equals(res.outcome1()) : "B outcome phai la DEFEND_MISSED nhung la: " + res.outcome1();
        assert res.tacticalDelta1() == 0 : "Delta phai la 0!";
        assert res.newPoints0() == 200;
        assert res.newPoints1() == 200;

        System.out.println("   [PASS] Defend missed duoc ghi nhan chuan xac!");
    }

    /** 9. Cả 2 cùng tấn công (A attack & B attack) */
    private static void testBothAttackSimultaneously() {
        System.out.println("9. Kiem tra ca A va B cung tan cong (Doi cong)...");
        // A đúng (100 base), B đúng (100 base)
        // Cả 2 cùng cướp 50 của nhau
        TacticalResolution res = Server.resolveTacticalCombat(
            100, 100,
            100, 100,
            true, true,
            "ATTACK", "ATTACK"
        );

        // A = 100 + 100 + 50 - 50 = 200
        // B = 100 + 100 + 50 - 50 = 200
        assert res.newPoints0() == 200;
        assert res.newPoints1() == 200;
        assert "STEAL_SUCCESS".equals(res.outcome0());
        assert "STEAL_SUCCESS".equals(res.outcome1());

        System.out.println("   [PASS] Doi cong xu ly cong bang cho ca 2!");
    }
}

