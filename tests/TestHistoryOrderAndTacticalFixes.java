package tests;

import quiz.ui.HistoryPanel;
import quiz.ui.HistoryCard;
import quiz.ui.ResultDialog;
import quiz.ui.TacticalActionBar;
import quiz.ui.TacticalToastBanner;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class TestHistoryOrderAndTacticalFixes {

    public static void main(String[] args) {
        System.out.println("=== KIEM TRA FIXES: FONT TOAST, TACTICAL DIMMED, HISTORY NEWEST FIRST ===");

        testToastBannerNoEmojis();
        testTacticalButtonDimmedOnZero();
        testHistoryNewestFirst();
        testRematchOfferNotification();

        System.out.println(">>> TAT CA CAC TEST DA PASS HOAN HAO 100%! <<<");
        System.exit(0);
    }

    private static void testToastBannerNoEmojis() {
        System.out.println("1. Kiem tra TacticalToastBanner khong con emoji unicode gay loi font Windows...");
        TacticalToastBanner banner = new TacticalToastBanner();

        banner.showSelfAction("ATTACK");
        assertFalse("ATTACK text must not contain sword emoji", banner.getMessageText().contains("⚔"));
        assertTrue("ATTACK text must contain [CHIẾN THUẬT]", banner.getMessageText().contains("[CHIẾN THUẬT]"));

        banner.showSelfAction("DEFEND");
        assertFalse("DEFEND text must not contain shield emoji", banner.getMessageText().contains("🛡"));
        assertTrue("DEFEND text must contain [CHIẾN THUẬT]", banner.getMessageText().contains("[CHIẾN THUẬT]"));

        banner.showSelfAction("STAR");
        assertFalse("STAR text must not contain star emoji", banner.getMessageText().contains("⭐"));
        assertTrue("STAR text must contain [CHIẾN THUẬT]", banner.getMessageText().contains("[CHIẾN THUẬT]"));

        System.out.println("   [PASS] TacticalToastBanner 100% sach emoji, khong con o vuong tofu!");
    }

    private static void testTacticalButtonDimmedOnZero() {
        System.out.println("2. Kiem tra nut chien thuat khi an lap tuc ve 0 va mo o (disabled)...");
        TacticalActionBar bar = new TacticalActionBar();
        bar.setPoints(100);

        JButton btnDef = bar.getDefendButton();
        assertTrue("Ban dau nut phong thu phai enabled", btnDef.isEnabled());
        assertTrue("Ban dau phai co nhan (1)", btnDef.getText().contains("(1)"));

        // Click nut phong thu
        btnDef.doClick();

        // Kiem tra ngay lap tuc
        assertEquals("So luot phong thu con lai phai la 0", 0, bar.getDefendRemaining());
        assertTrue("Nhan nut phong thu phai la (0)", btnDef.getText().contains("(0)"));
        assertFalse("Nut phong thu phai bi vo hieu hoa (disabled, mo o)", btnDef.isEnabled());

        // Chuyen sang cau moi (ke ca khi truyen nham defLeft = 1 tu packet cu)
        bar.updateState(100, 1, 1, 1, "DEFEND");
        assertEquals("Ke ca khi nhan packet cu def=1, luot phong thu van khoa o 0", 0, bar.getDefendRemaining());
        assertTrue("Nhan nut phong thu van la (0)", btnDef.getText().contains("(0)"));
        assertFalse("Nut phong thu van phai tat (disabled)", btnDef.isEnabled());

        bar.onNewQuestion(100, 1, 1, 1);
        assertEquals("Sang cau moi luot phong thu van la 0", 0, bar.getDefendRemaining());
        assertTrue("Sang cau moi van la (0)", btnDef.getText().contains("(0)"));
        assertFalse("Sang cau moi nut phong thu van bi vo hieu hoa mo o", btnDef.isEnabled());

        System.out.println("   [PASS] Nut chien thuat lap tuc ve 0 va mo o dung nhu yeu cau!");
    }

    private static void testHistoryNewestFirst() {
        System.out.println("3. Kiem tra lich su tran dau: tran moi nhat phai len dau tien...");
        String rawData =
            "Toán / Dễ / set1: player1 8–5 player2 → player1 | 35000 | 1700000000000\n" +  // Cu
            "Lý / Khó / set2: player1 9–7 player3 → player1 | 42000 | 1700000500000\n" +   // Trung binh
            "Hóa / Vừa / set3: player1 6–8 player4 → player4 | 38000 | 1700000900000\n";   // Moi nhat (timestamp lon nhat)

        HistoryPanel panel = new HistoryPanel();
        panel.updateData(rawData, "player1");

        List<HistoryCard> cards = panel.getAllCards();
        assertEquals("Phai co dung 3 the tran dau", 3, cards.size());

        // The dau tien (index 0) phai la tran moi nhat (timestamp = 1700000900000)
        assertEquals("The dau tien phai la tran moi nhat", 1700000900000L, cards.get(0).getTimestamp());
        assertEquals("The thu hai phai la tran tiep theo", 1700000500000L, cards.get(1).getTimestamp());
        assertEquals("The thu ba phai la tran cu nhat", 1700000000000L, cards.get(2).getTimestamp());

        System.out.println("   [PASS] Lich su tran dau sap xep giam dan theo thoi gian, tran moi nhat luon o tren dau!");
    }

    private static void testRematchOfferNotification() {
        System.out.println("4. Kiem tra thong bao Lời Mời Tái Đấu (Đồng Ý / Thoát Về Sảnh)...");
        final boolean[] accepted = {false};
        final boolean[] declined = {false};
        JFrame dummyFrame = new JFrame();
        ResultDialog dlg = new ResultDialog(
            dummyFrame,
            "player2",
            "player1",
            "player1(8/10, 25000ms; pts=950; streak=4)",
            "player2(6/10, 28000ms; pts=700; streak=2)",
            () -> accepted[0] = true,
            () -> declined[0] = true
        );

        dlg.onOpponentRequestedRematch("player1");
        assertTrue("Phai tao hop thoai thong bao tai dau", dlg.getRematchOfferDialog() != null);

        dlg.acceptRematch();
        assertTrue("An Dong Y phai kich hoat onRematchYes", accepted[0]);
        assertFalse("An Dong Y khong duoc kich hoat onRematchNo", declined[0]);

        dlg.dispose();
        dummyFrame.dispose();
        System.out.println("   [PASS] Thong bao Lời Mời Tái Đấu hoat dong chinh xac!");
    }

    private static void assertTrue(String msg, boolean cond) {
        if (!cond) throw new AssertionError("FAILED: " + msg);
    }

    private static void assertFalse(String msg, boolean cond) {
        if (cond) throw new AssertionError("FAILED: " + msg);
    }

    private static void assertEquals(String msg, Object expected, Object actual) {
        if (expected == null && actual == null) return;
        if (expected != null && expected.equals(actual)) return;
        throw new AssertionError("FAILED: " + msg + " (expected: " + expected + ", actual: " + actual + ")");
    }
}
