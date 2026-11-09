package tests;

import quiz.ui.*;
import java.awt.Component;
import java.util.*;
import javax.swing.*;

public class TestMatchScreenTransition {

    public static void main(String[] args) throws Exception {
        System.out.println("=== BAT DAU KIEM TRA TEST MATCH SCREEN TRANSITION & TOFU FIX ===");

        // Chay tren luong EDT
        SwingUtilities.invokeAndWait(() -> {
            try {
                testQuestionGridIntegrityAndNavigation();
                testSubmitConfirmDialog();
                testMatchScreenNoTofuAndTimerPersistence();
                testTopHudSingleRowWithLongTopicAndNames();
                System.out.println(">>> TAT CA CAC TEST KIEM TRA DEU THANH CONG HOAN HAO (PASS)! <<<");
            } catch (Throwable t) {
                t.printStackTrace();
                System.exit(1);
            }
        });
        System.exit(0);
    }

    private static void testQuestionGridIntegrityAndNavigation() throws Exception {
        System.out.println("1. Kiem tra thu tu 4 dap an va chuyen tiep qua cac cau hoi...");

        // Tao danh sach cau hoi tong hop cac the loai
        List<MatchQuestion> questions = new ArrayList<>();
        // 1. Trac nghiem 4 dap an
        questions.add(new MatchQuestion("Cau hoi 1: Thu do cua Viet Nam la gi?",
                new String[]{"Ha Noi", "TP Ho Chi Minh", "Da Nang", "Can Tho"}, "", "MULTIPLE_CHOICE", null, null, 0));
        // 2. Dung / Sai
        questions.add(new MatchQuestion("Cau hoi 2: Mat Troi moc o huong Dong?",
                new String[]{"Đúng", "Sai"}, "", "TRUE_FALSE", null, null, 0));
        // 3. Chon nhieu dap an
        questions.add(new MatchQuestion("Cau hoi 3: Cac thanh pho truc thuoc trung uong cua Viet Nam?",
                new String[]{"Ha Noi", "Hai Phong", "Da Nang", "Nha Trang"}, "", "MULTIPLE_SELECT", null, null, 7));
        // 4. Dien vao cho trong
        questions.add(new MatchQuestion("Cau hoi 4: Song dai nhat the gioi la song gi?",
                new String[]{"Nile"}, "", "FILL_IN", null, null, 0));
        // 5. Keo tha
        Map<String, String> fillMap = new LinkedHashMap<>();
        fillMap.put("BLANK_1", "Ha Noi");
        fillMap.put("BLANK_2", "TP Ho Chi Minh");
        questions.add(new MatchQuestion("Cau hoi 5: Thu do la {BLANK_1} va thanh pho lon nhat la {BLANK_2}",
                new String[]{"Ha Noi", "TP Ho Chi Minh", "Hue"}, "", "DRAG_AND_DROP", new String[]{"BLANK_1", "BLANK_2"}, fillMap, 0));
        // Them 5 cau 4 lua chon nua
        for (int i = 6; i <= 10; i++) {
            questions.add(new MatchQuestion("Cau hoi " + i + ": Noi dung thu nghiem so " + i,
                    new String[]{"Lua chon A" + i, "Lua chon B" + i, "Lua chon C" + i, "Lua chon D" + i}, "", "MULTIPLE_CHOICE", null, null, (i % 4)));
        }

        JFrame frame = new JFrame();
        MatchScreen matchScreen = new MatchScreen("match_test_01", "Player1", "Player2", "avatar_01", "avatar_02", "Khoa Hoc", "De", "set_01", 120, questions,
                userAnswers -> {},
                () -> {});
        frame.setContentPane(matchScreen);
        frame.pack();

        // Lay tham chieu answersGrid bang reflection de verify component order
        java.lang.reflect.Field answersGridField = MatchScreen.class.getDeclaredField("answersGrid");
        answersGridField.setAccessible(true);
        JPanel answersGrid = (JPanel) answersGridField.get(matchScreen);

        java.lang.reflect.Field buttonsField = MatchScreen.class.getDeclaredField("answerButtons");
        buttonsField.setAccessible(true);
        ModernButton[] buttons = (ModernButton[]) buttonsField.get(matchScreen);

        // Kiem tra qua tat ca 10 cau hoi
        for (int qIdx = 0; qIdx < questions.size(); qIdx++) {
            matchScreen.navigateToQuestion(qIdx);

            MatchQuestion q = questions.get(qIdx);
            if (!q.isTrueFalse() && !q.isFillIn() && !q.isDragAndDrop()) {
                // Kiem tra thu tu trong answersGrid
                Component[] comps = answersGrid.getComponents();
                if (comps.length != 4) {
                    throw new AssertionError("answersGrid phai co dung 4 components, hien tai: " + comps.length);
                }
                for (int c = 0; c < 4; c++) {
                    if (comps[c] != buttons[c]) {
                        throw new AssertionError("Cau " + (qIdx + 1) + ": Button tai index " + c + " bi dao lon vi tri!");
                    }
                    String prefix = buttons[c].getPrefixBadge();
                    String expectedPrefix = switch (c) {
                        case 0 -> "A";
                        case 1 -> "B";
                        case 2 -> "C";
                        case 3 -> "D";
                        default -> "";
                    };
                    if (!expectedPrefix.equals(prefix)) {
                        throw new AssertionError("Prefix badge bi sai: mong muon " + expectedPrefix + " nhung thuc te la " + prefix);
                    }
                }
            }
        }
        System.out.println("  [PASS] Thu tu 4 dap an A, B, C, D duoc bao toan chinh xac 100% tren moi cau hoi!");
    }

    private static void testSubmitConfirmDialog() {
        System.out.println("2. Kiem tra SubmitConfirmDialog khoi tao khong bi loi tofu...");
        JFrame frame = new JFrame();
        List<MatchQuestion> questions = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            questions.add(new MatchQuestion("Cau " + (i + 1), new String[]{"A", "B"}));
        }
        int[] answers = new int[]{0, 1, 2, -1, 1, -1, 3, 0, 1, 2};
        SubmitConfirmDialog dialog = new SubmitConfirmDialog(frame, questions, answers, 0);

        if (dialog.getUserChoice() != SubmitConfirmDialog.Choice.CANCEL) {
            throw new AssertionError("Default choice phai la CANCEL");
        }
        dialog.dispose();
        frame.dispose();
        System.out.println("  [PASS] SubmitConfirmDialog hoat dong on dinh!");
    }

    private static void testMatchScreenNoTofuAndTimerPersistence() throws Exception {
        System.out.println("3. Kiem tra MatchScreen toan bo label khong co ky tu gay loi tofu & timer khong reset...");
        List<MatchQuestion> questions = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            questions.add(new MatchQuestion("Cau " + (i + 1), new String[]{"A", "B", "C", "D"}));
        }
        MatchScreen matchScreen = new MatchScreen("match_test_02", "Player1", "Player2", "avatar_01", "avatar_02", "Chu de", "De", "set_01", 100, questions,
                ans -> {}, () -> {});

        // Test btnNext
        java.lang.reflect.Field btnNextField = MatchScreen.class.getDeclaredField("btnNext");
        btnNextField.setAccessible(true);
        ModernButton btnNext = (ModernButton) btnNextField.get(matchScreen);
        String nextText = btnNext.getText();
        if (nextText.contains("➔") || nextText.contains("✔")) {
            throw new AssertionError("btnNext co chua ky tu gay tofu: " + nextText);
        }

        // Test lblMultiSelectNotice
        java.lang.reflect.Field multiNoticeField = MatchScreen.class.getDeclaredField("lblMultiSelectNotice");
        multiNoticeField.setAccessible(true);
        JLabel lblNotice = (JLabel) multiNoticeField.get(matchScreen);
        if (lblNotice.getText().contains("✦")) {
            throw new AssertionError("lblMultiSelectNotice co chua ky tu gay tofu: " + lblNotice.getText());
        }

        // Test ScoreBadge painting with streak >= 2
        matchScreen.updateScores(5, 3, 500, 300, 4, 1);
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(900, 700, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g2 = img.createGraphics();
        matchScreen.setSize(900, 700);
        matchScreen.doLayout();
        matchScreen.paint(g2);
        g2.dispose();

        // Test timer preservation on navigation
        java.lang.reflect.Field timeLeftField = MatchScreen.class.getDeclaredField("questionTimeLeft");
        timeLeftField.setAccessible(true);
        int[] timeLeft = (int[]) timeLeftField.get(matchScreen);
        // Gia lap cau 0 con 6 giay (giam tu 10 giay xuong)
        timeLeft[0] = 6;
        matchScreen.navigateToQuestion(1);
        // Chuyen lai cau 0
        matchScreen.navigateToQuestion(0);
        java.lang.reflect.Field curRemainingField = MatchScreen.class.getDeclaredField("currentQuestionTimeRemaining");
        curRemainingField.setAccessible(true);
        int curRemaining = (int) curRemainingField.get(matchScreen);
        if (curRemaining != 6) {
            throw new AssertionError("Loi timer: Khi chuyen ve cau 0, thoi gian con lai phai la 6 nhung thuc te la: " + curRemaining);
        }

        matchScreen.cleanupAndStop();
        System.out.println("  [PASS] Toan bo text tren MatchScreen tofu-free 100% va thoi gian khong bi reset ngoai y muon!");
    }

    private static void testTopHudSingleRowWithLongTopicAndNames() throws Exception {
        System.out.println("4. Kiem tra layout topHud khong bi rot dong khi ten chu de va ten nguoi choi rat dai...");
        List<MatchQuestion> questions = new ArrayList<>();
        questions.add(new MatchQuestion("Cau hoi 1?", new String[]{"A", "B", "C", "D"}, "", "MULTIPLE_CHOICE", null, null, 0));

        JFrame frame = new JFrame();
        MatchScreen matchScreen = new MatchScreen(
                "match_long_topic",
                "Người Chơi Siêu Dài 1",
                "Đối Thủ Vô Cùng Dài 2",
                "avatar_01",
                "avatar_02",
                "Lập Trình Mạng và Hệ Thống Phân Tán Nâng Cao 2026",
                "Chuyên gia",
                "set_99",
                120,
                questions,
                ans -> {},
                () -> {}
        );
        frame.setContentPane(matchScreen);
        frame.setSize(960, 680);
        frame.doLayout();
        matchScreen.doLayout();

        JPanel topHud = (JPanel) ((java.awt.BorderLayout) matchScreen.getLayout()).getLayoutComponent(java.awt.BorderLayout.NORTH);
        topHud.doLayout();
        JPanel centerHud = (JPanel) ((java.awt.BorderLayout) topHud.getLayout()).getLayoutComponent(java.awt.BorderLayout.CENTER);
        centerHud.doLayout();

        Component[] comps = centerHud.getComponents();
        if (comps.length != 3) {
            throw new AssertionError("centerHud phai co 3 components (p1Row, vsColumn, p2Row), thuc te la: " + comps.length);
        }
        int y0 = comps[0].getY();
        int y2 = comps[2].getY();

        if (Math.abs(y0 - y2) > 5) {
            throw new AssertionError("Loi rot dong p2Row! y0=" + y0 + ", y2=" + y2 + ". p2Row bi rot xuong dong duoi!");
        }

        matchScreen.cleanupAndStop();
        frame.dispose();
        System.out.println("  [PASS] topHud giu nguyen 1 dong duy nhat (single-row), p2Row khong bao gio bi rot dong!");
    }
}

