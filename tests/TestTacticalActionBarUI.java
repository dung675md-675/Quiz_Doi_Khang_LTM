package tests;

import quiz.ui.MatchQuestion;
import quiz.ui.MatchScreen;
import quiz.ui.TacticalActionBar;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class TestTacticalActionBarUI {

    public static void main(String[] args) {
        System.out.println("=== BAT DAU KIEM TRA TACTICAL ACTION BAR UI (PHAN 3) ===");

        // Test 1: Khởi tạo TacticalActionBar độc lập
        testStandaloneBar();

        // Test 2: Bật tắt theo điểm số & hủy chọn (CANCEL) & đổi nút
        testInteractivityAndCancel();

        // Test 3: Khóa khi nộp bài và reset ở câu hỏi mới
        testLockAndNewQuestion();

        // Test 4: Hết lượt sử dụng (Exhausted turns)
        testExhaustedTurns();

        // Test 5: Tích hợp trực tiếp vào MatchScreen
        testMatchScreenIntegration();

        System.out.println(">>> TAT CA CAC TEST CHO TACTICAL ACTION BAR DEU PASS 100%! <<<");
        System.exit(0);
    }

    private static void testStandaloneBar() {
        System.out.println("1. Kiem tra khoi tao TacticalActionBar va rang buoc diem ban dau (0 diem)...");
        TacticalActionBar bar = new TacticalActionBar();

        JButton atk = bar.getAttackButton();
        JButton def = bar.getDefendButton();
        JButton star = bar.getStarButton();

        assertCondition(atk.getText().equals("Tấn Công (1)"), "Text Tan Cong phai la (1), nhan duoc: " + atk.getText());
        assertCondition(def.getText().equals("Phòng Thủ (1)"), "Text Phong Thu phai la (1), nhan duoc: " + def.getText());
        assertCondition(star.getText().equals("Ngôi Sao (1)"), "Text Ngoi Sao phai la (1), nhan duoc: " + star.getText());

        // Khi 0 diem (< 50)
        assertCondition(!atk.isEnabled(), "Tan cong phai bi mo khi diem < 50");
        assertCondition(def.isEnabled(), "Phong thu phai luon sang khi con luot");
        assertCondition(!star.isEnabled(), "Ngoi sao phai bi mo khi diem < 50");

        System.out.println("   [PASS] Khoi tao va rang buoc diem < 50 chuan xac!");
    }

    private static void testInteractivityAndCancel() {
        System.out.println("2. Kiem tra kich hoat, doi ky nang va CANCEL khi bam lai nut...");
        TacticalActionBar bar = new TacticalActionBar();
        List<String> actions = new ArrayList<>();
        bar.setOnActionListener(actions::add);

        // Nguoi choi co 100 diem -> tat ca deu duoc bat
        bar.setPoints(100);
        JButton atk = bar.getAttackButton();
        JButton def = bar.getDefendButton();
        JButton star = bar.getStarButton();

        assertCondition(atk.isEnabled(), "Tan cong phai bat khi >= 50 diem");
        assertCondition(def.isEnabled(), "Phong thu phai bat");
        assertCondition(star.isEnabled(), "Ngoi sao phai bat khi >= 50 diem");

        // Click DEFEND
        def.doClick();
        assertCondition(actions.size() == 1 && actions.get(0).equals("DEFEND"), "Phai phat action DEFEND");
        assertCondition("DEFEND".equals(bar.getActiveSkill()), "Active skill phai la DEFEND");

        // Click lai DEFEND -> HỦY (CANCEL)
        def.doClick();
        assertCondition(actions.size() == 2 && actions.get(1).equals("CANCEL"), "Bam lai phai phat action CANCEL");
        assertCondition("NONE".equals(bar.getActiveSkill()), "Active skill phai ve NONE");

        // Click ATTACK -> sau do click STAR (chuyen ky nang)
        atk.doClick();
        assertCondition(actions.size() == 3 && actions.get(2).equals("ATTACK"), "Phai phat action ATTACK");
        assertCondition("ATTACK".equals(bar.getActiveSkill()), "Active skill phai la ATTACK");

        star.doClick();
        assertCondition(actions.size() == 4 && actions.get(3).equals("STAR"), "Phai chuyen sang STAR");
        assertCondition("STAR".equals(bar.getActiveSkill()), "Active skill phai la STAR");

        System.out.println("   [PASS] Kich hoat, doi nut va CANCEL hoat dong hoan hao!");
    }

    private static void testLockAndNewQuestion() {
        System.out.println("3. Kiem tra khoa toan bo thanh khi nop bai / het gio va reset o cau moi...");
        TacticalActionBar bar = new TacticalActionBar();
        List<String> actions = new ArrayList<>();
        bar.setOnActionListener(actions::add);
        bar.setPoints(100);

        // Kich hoat DEFEND
        bar.getDefendButton().doClick();
        assertCondition("DEFEND".equals(bar.getActiveSkill()), "Active skill phai la DEFEND");

        // Nop dap an / het gio -> khoa thanh
        bar.setQuestionLocked(true);
        assertCondition(!bar.getAttackButton().isEnabled(), "Tan cong phai bi khoa");
        assertCondition(!bar.getDefendButton().isEnabled(), "Phong thu phai bi khoa");
        assertCondition(!bar.getStarButton().isEnabled(), "Ngoi sao phai bi khoa");

        // Thu click khi bi khoa -> khong co them action nao
        int countBefore = actions.size();
        bar.getDefendButton().doClick();
        assertCondition(actions.size() == countBefore, "Khong duoc phep click khi thanh dang bi khoa!");

        // Sang cau hoi moi: onNewQuestion
        bar.onNewQuestion(120);
        assertCondition(!bar.isQuestionLocked(), "Cau moi phai mo khoa");
        assertCondition("NONE".equals(bar.getActiveSkill()), "Cau moi active skill phai reset ve NONE");
        assertCondition(bar.getAttackButton().isEnabled(), "Tan cong mo lai khi diem >= 50");
        assertCondition(bar.getDefendButton().isEnabled(), "Phong thu mo lai");
        assertCondition(bar.getStarButton().isEnabled(), "Ngoi sao mo lai khi diem >= 50");

        System.out.println("   [PASS] Khoa khi het gio / nop bai va reset cau moi chuan xac!");
    }

    private static void testExhaustedTurns() {
        System.out.println("4. Kiem tra khoa vinh vien khi het luot su dung...");
        TacticalActionBar bar = new TacticalActionBar();
        bar.setPoints(200);

        // Cap nhat so luot: Atk = 0, Def = 1, Star = 0
        bar.updateState(200, 0, 1, 0, "NONE");

        assertCondition(bar.getAttackRemaining() == 0, "Atk phai bang 0");
        assertCondition(bar.getDefendRemaining() == 1, "Def phai bang 1");
        assertCondition(bar.getStarRemaining() == 0, "Star phai bang 0");

        assertCondition(bar.getAttackButton().getText().equals("Tấn Công (0)"), "Label phai la Tấn Công (0)");
        assertCondition(bar.getStarButton().getText().equals("Ngôi Sao (0)"), "Label phai la Ngôi Sao (0)");

        assertCondition(!bar.getAttackButton().isEnabled(), "Tan cong phai bi disable vinh vien vi het luot");
        assertCondition(!bar.getStarButton().isEnabled(), "Ngoi sao phai bi disable vinh vien vi het luot");
        assertCondition(bar.getDefendButton().isEnabled(), "Phong thu van phai enabled vi con 1 luot");

        System.out.println("   [PASS] Khoa vinh vien khi het luot chinh xac!");
    }

    private static void testMatchScreenIntegration() {
        System.out.println("5. Kiem tra nhung TacticalActionBar vao MatchScreen...");

        // Tao MatchScreen voi du lieu gia lap
        List<MatchQuestion> questions = new ArrayList<>();
        questions.add(new MatchQuestion("Test Question 1", new String[]{"A", "B", "C", "D"}, "Explanation"));
        MatchScreen matchScreen = new MatchScreen("M123", "PlayerA", "PlayerB", "Toán", "Dễ", "SET1", 100, questions, ans -> {}, () -> {});

        TacticalActionBar bar = matchScreen.getTacticalActionBar();
        assertCondition(bar != null, "MatchScreen phai so huu TacticalActionBar khong null");

        // Kiem tra bar da duoc add vao component hierarchy cua MatchScreen
        Component parent = bar.getParent();
        assertCondition(parent != null, "TacticalActionBar phai duoc add vao giao dien MatchScreen");

        // Kiem tra truyen action tu bar len MatchScreen
        AtomicReference<String> caughtAction = new AtomicReference<>();
        matchScreen.setOnTacticalActionListener(caughtAction::set);

        bar.setPoints(100);
        bar.getDefendButton().doClick();
        assertCondition("DEFEND".equals(caughtAction.get()), "MatchScreen phai nhan duoc DEFEND qua setOnTacticalActionListener");

        // Kiem tra server confirm dong bo trang thai vao bar
        matchScreen.handleTacticalConfirm("DEFEND", 2, 1, 1);
        assertCondition("DEFEND".equals(bar.getActiveSkill()), "Bar phai active DEFEND");
        assertCondition(bar.getDefendRemaining() == 1, "Defend phai con 1 luot");

        // Kiem tra khi MatchScreen ket thuc cau hoi (finish / reveal)
        matchScreen.showQuestionRevealPhase(0, 0, "Exp", 100, 80, 1, 0, 0, 1, "DEFEND", "ATTACK", "DEFEND_BLOCKED", "ATTACK_BLOCKED", 0, 0);
        assertCondition(bar.isQuestionLocked(), "Bar phai bi khoa khi o pha Reveal");

        // Kiem tra khi sang cau hoi moi
        matchScreen.startQuestionActivePhase(1, 10);
        assertCondition(!bar.isQuestionLocked(), "Bar phai mo khoa o cau hoi moi");
        assertCondition("NONE".equals(bar.getActiveSkill()), "Bar active skill phai reset ve NONE");

        System.out.println("   [PASS] Tich hop vao MatchScreen hoat dong tron tru 100%!");
    }

    private static void assertCondition(boolean condition, String message) {
        if (!condition) {
            System.err.println("FAILED ASSERTION: " + message);
            throw new AssertionError(message);
        }
    }
}
