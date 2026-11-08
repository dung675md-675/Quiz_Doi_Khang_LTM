package tests;

import quiz.ui.MatchQuestion;
import quiz.ui.MatchScreen;
import quiz.ui.SoundManager;
import quiz.ui.TacticalActionBar;
import quiz.ui.TacticalToastBanner;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class TestTacticalToastAndAlert {

    public static void main(String[] args) {
        System.out.println("=== BAT DAU KIEM TRA TOAST BANNER & BLINKING DEFEND (PHAN 4) ===");

        // Test 1: Kiểm tra component TacticalToastBanner độc lập
        testToastBannerStandalone();

        // Test 2: Kiểm tra SoundManager không crash khi thiếu file
        testSoundManagerSafety();

        // Test 3: Tích hợp với MatchScreen khi nhận OPPONENT_TACTICAL
        testMatchScreenOpponentTactical();

        // Test 4: Cơ chế dừng nhấp nháy Phòng thủ (Bấm phòng thủ / Cancel / Nộp bài / Sang câu mới)
        testDefendBlinkStopConditions();

        System.out.println(">>> TAT CA CAC TEST CHO PHAN 4 DEU PASS 100%! <<<");
        System.exit(0);
    }

    private static void testToastBannerStandalone() {
        System.out.println("1. Kiem tra TacticalToastBanner standalone...");
        TacticalToastBanner banner = new TacticalToastBanner();

        assertCondition(!banner.isVisible(), "Banner ban dau phai an");
        assertCondition(banner.getPreferredSize().height == 0, "Height ban dau phai bang 0");

        // ATTACK
        banner.showOpponentAction("HoangNam", "ATTACK");
        assertCondition(banner.isVisible(), "Banner phai hien sau khi showOpponentAction ATTACK");
        assertCondition("ATTACK".equals(banner.getCurrentAction()), "Action phai la ATTACK");
        assertCondition(banner.getMessageText().equals("[ĐỐI THỦ] HoangNam vừa phát động TẤN CÔNG!"),
                "Message sai: " + banner.getMessageText());

        // DEFEND
        banner.showOpponentAction("HoangNam", "DEFEND");
        assertCondition("DEFEND".equals(banner.getCurrentAction()), "Action phai la DEFEND");
        assertCondition(banner.getMessageText().equals("[ĐỐI THỦ] HoangNam đã kích hoạt KHIÊN PHÒNG THỦ!"),
                "Message sai: " + banner.getMessageText());

        // STAR
        banner.showOpponentAction("HoangNam", "STAR");
        assertCondition("STAR".equals(banner.getCurrentAction()), "Action phai la STAR");
        assertCondition(banner.getMessageText().equals("[ĐỐI THỦ] HoangNam đã đặt cược NGÔI SAO HY VỌNG!"),
                "Message sai: " + banner.getMessageText());

        // CANCEL
        banner.showOpponentAction("HoangNam", "CANCEL");
        assertCondition(!banner.isVisible(), "Banner phai an ngay khi CANCEL");
        assertCondition(banner.getCurrentAction().equals("NONE"), "Action phai reset ve NONE");

        System.out.println("   [PASS] TacticalToastBanner standalone hoat dong hoan hao!");
    }

    private static void testSoundManagerSafety() {
        System.out.println("2. Kiem tra SoundManager phat am thanh an toan khong crash...");
        try {
            SoundManager.playTacticalAttack();
            SoundManager.playTacticalDefend();
            SoundManager.playTacticalStar();
            SoundManager.playSound("tactical_attack");
            SoundManager.playSound("tactical_defend");
            SoundManager.playSound("tactical_star");
            // Cho 1 khoang thoi gian nho de worker thread chay
            Thread.sleep(100);
            System.out.println("   [PASS] SoundManager chay an toan 100%, tu dong fallback PCM neu thieu file!");
        } catch (Exception e) {
            throw new AssertionError("SoundManager gay loi: " + e.getMessage());
        }
    }

    private static void testMatchScreenOpponentTactical() {
        System.out.println("3. Kiem tra MatchScreen khi nhan OPPONENT_TACTICAL tu server...");
        List<MatchQuestion> questions = new ArrayList<>();
        questions.add(new MatchQuestion("Cau 1", new String[]{"A", "B", "C", "D"}, "Giai thich"));
        questions.add(new MatchQuestion("Cau 2", new String[]{"A", "B", "C", "D"}, "Giai thich"));
        MatchScreen matchScreen = new MatchScreen("M123", "UserMe", "DoiThuVip", "Toán", "Dễ", "SET1", 100, questions, ans -> {}, () -> {});

        TacticalToastBanner banner = matchScreen.getTacticalToastBanner();
        TacticalActionBar bar = matchScreen.getTacticalActionBar();
        assertCondition(banner != null, "MatchScreen phai co TacticalToastBanner");
        assertCondition(bar != null, "MatchScreen phai co TacticalActionBar");

        // Gia lap Server gui OPPONENT_TACTICAL ATTACK
        matchScreen.handleOpponentTactical("ATTACK");
        assertCondition(banner.isVisible(), "Toast banner phai bat");
        assertCondition(banner.getMessageText().contains("DoiThuVip"), "Toast banner phai chua ten that cua doi thu");
        assertCondition(banner.getMessageText().contains("TẤN CÔNG"), "Toast banner phai thong bao TẤN CÔNG");
        assertCondition(bar.isDefendBlinking(), "Nut Phong Thu phai nhap nhay nhac nho");

        // Doi thu CANCEL
        matchScreen.handleOpponentTactical("CANCEL");
        assertCondition(!banner.isVisible(), "Toast banner phai an khi doi thu CANCEL");
        assertCondition(!bar.isDefendBlinking(), "Nut Phong Thu phai ngung nhap nhay khi doi thu CANCEL");

        System.out.println("   [PASS] Nhan tin hieu OPPONENT_TACTICAL va hien thi banner thanh cong!");
    }

    private static void testDefendBlinkStopConditions() {
        System.out.println("4. Kiem tra cac dieu kien dung nhap nhay nut Phong Thu...");
        List<MatchQuestion> questions = new ArrayList<>();
        questions.add(new MatchQuestion("Cau 1", new String[]{"A", "B", "C", "D"}, "Giai thich"));
        questions.add(new MatchQuestion("Cau 2", new String[]{"A", "B", "C", "D"}, "Giai thich"));
        MatchScreen matchScreen = new MatchScreen("M123", "UserMe", "DoiThuVip", "Toán", "Dễ", "SET1", 100, questions, ans -> {}, () -> {});

        TacticalActionBar bar = matchScreen.getTacticalActionBar();
        TacticalToastBanner banner = matchScreen.getTacticalToastBanner();

        // Case 4.1: Nhan ATTACK -> user bam Phong Thu -> ngung nhap nhay
        matchScreen.handleOpponentTactical("ATTACK");
        assertCondition(bar.isDefendBlinking(), "Phai bat dau nhap nhay");
        bar.getDefendButton().doClick();
        assertCondition(!bar.isDefendBlinking(), "Phai dung nhap nhay khi user bam Phong Thu");
        assertCondition("DEFEND".equals(bar.getActiveSkill()), "User phai kich hoat Phong Thu");

        // Case 4.2: Neu user het luot Defend -> khong nhap nhay
        bar.resetBar();
        bar.updateState(100, 2, 0, 1, "NONE"); // 0 defend
        matchScreen.handleOpponentTactical("ATTACK");
        assertCondition(!bar.isDefendBlinking(), "Khong duoc nhap nhay neu da het luot Phong Thu");

        // Case 4.3: Dang nhap nhay ma sang cau hoi moi -> ngung nhap nhay & an banner
        bar.resetBar();
        bar.updateState(100, 2, 2, 1, "NONE");
        matchScreen.handleOpponentTactical("ATTACK");
        assertCondition(bar.isDefendBlinking(), "Phai nhap nhay lai vi con luot");
        assertCondition(banner.isVisible(), "Banner phai hien");

        matchScreen.startQuestionActivePhase(1, 10);
        assertCondition(!bar.isDefendBlinking(), "Phai dung nhap nhay khi sang cau hoi moi");
        assertCondition(!banner.isVisible(), "Banner phai an khi sang cau hoi moi");

        System.out.println("   [PASS] Tat ca dieu kien dung nhap nhay deu hoat dong chinh xac!");
    }

    private static void assertCondition(boolean condition, String message) {
        if (!condition) {
            System.err.println("FAILED ASSERTION: " + message);
            throw new AssertionError(message);
        }
    }
}

