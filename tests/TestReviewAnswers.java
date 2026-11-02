package tests;

import quiz.ui.*;
import java.awt.Component;
import java.util.*;
import javax.swing.*;

public class TestReviewAnswers {

    public static void main(String[] args) throws Exception {
        System.out.println("=== BAT DAU KIEM TRA REVIEW ANSWERS DIALOG & USER ANSWERS SYNC ===");

        SwingUtilities.invokeAndWait(() -> {
            try {
                testReviewAnswersCorrectCounting();
                testReviewAnswersCardBadges();
                testMatchScreenToResultDialogSync();
                System.out.println(">>> TAT CA TEST REVIEW ANSWERS DEU PASS 100%! <<<");
            } catch (Throwable t) {
                t.printStackTrace();
                System.exit(1);
            }
        });
        System.exit(0);
    }

    private static void testReviewAnswersCorrectCounting() {
        System.out.println("1. Kiem tra tinh dung ty le cau tra loi dung 7/10...");
        List<MatchQuestion> questions = createSampleQuestions();

        // 7 cau dung, 2 cau sai, 1 cau chua chon
        int[] userAns = new int[]{0, 0, 1, 1, 1, 0, 1, 0, -1, 1};
        int[] correctAns = new int[]{0, 0, 1, 1, 1, 0, 1, 1, 0, 0};
        String[] fillInInputs = new String[10];
        fillInInputs[3] = "1945"; // Cau 4 la fill-in

        ReviewAnswersDialog dlg = new ReviewAnswersDialog(null, questions, userAns, correctAns, fillInInputs);

        // Tim badge ty le tren header
        boolean foundScoreBadge = false;
        for (Component comp : getAllComponents(dlg)) {
            if (comp instanceof ModernBadge badge) {
                if (badge.getText() != null && badge.getText().contains("Đúng 7/10 câu (70%)")) {
                    foundScoreBadge = true;
                    break;
                }
            }
        }
        dlg.dispose();

        if (!foundScoreBadge) {
            throw new AssertionError("ReviewAnswersDialog phai co badge 'Đúng 7/10 câu (70%)'!");
        }
        System.out.println("  [PASS] Ty le dung 7/10 cau (70%) hien thi chuan xac!");
    }

    private static void testReviewAnswersCardBadges() {
        System.out.println("2. Kiem tra nhan (Badge) cua tung the cau hoi (Dung, Sai, Chua chon)...");
        List<MatchQuestion> questions = createSampleQuestions();

        int[] userAns = new int[]{0, 1, -1, 1, 0, 0, 0, 0, 0, 0};
        int[] correctAns = new int[]{0, 0, 0, 1, 1, 0, 0, 0, 0, 0};

        ReviewAnswersDialog dlg = new ReviewAnswersDialog(null, questions, userAns, correctAns, null);

        int countDung = 0;
        int countSai = 0;
        int countChuaChon = 0;

        for (Component comp : getAllComponents(dlg)) {
            if (comp instanceof ModernBadge badge) {
                String text = badge.getText();
                if ("Đúng (+1)".equals(text)) countDung++;
                else if ("Sai".equals(text)) countSai++;
                else if ("- Chưa chọn".equals(text)) countChuaChon++;
            }
        }
        dlg.dispose();

        if (countChuaChon != 1) {
            throw new AssertionError("Chi duoc co dung 1 cau chua chon (- Chưa chọn), thuc te: " + countChuaChon);
        }
        if (countDung < 1) {
            throw new AssertionError("Phai co nhan Đúng (+1) cho cau dung, thuc te: " + countDung);
        }
        if (countSai < 1) {
            throw new AssertionError("Phai co nhan Sai cho cau tra loi sai, thuc te: " + countSai);
        }
        System.out.println("  [PASS] Nhan cua tung the cau hoi phan loai chinh xac Đúng, Sai va Chưa chọn!");
    }

    private static void testMatchScreenToResultDialogSync() throws Exception {
        System.out.println("3. Kiem tra lay du lieu dap an tu MatchScreen qua ResultDialog...");
        List<MatchQuestion> questions = createSampleQuestions();

        MatchScreen ms = new MatchScreen("m_rev", "admin", "Bot", "avatar_01", "avatar_02", "Tong Hop", "De", "set_1", 120, questions, ans -> {}, () -> {});

        // Gia lap nguoi choi tra loi 2 cau dau tien
        ms.showQuestionRevealPhase(0, 0, "", 100, 0, 1, 0, 0, 1);
        ms.showQuestionRevealPhase(1, 0, "", 200, 0, 2, 0, 0, 1);

        int[] myAnswers = ms.getSelectedAnswers();
        if (myAnswers[0] != 0 || myAnswers[1] != 0) {
            throw new AssertionError("myAnswers phai luu dap an da tra loi!");
        }

        ResultDialog resultDialog = new ResultDialog(null, "admin", "admin", "admin: 2 câu", "Bot: 0 câu", () -> {}, () -> {},
                questions, myAnswers, new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, "avatar_01", "avatar_02", ms.getUserFillInAnswers());

        java.lang.reflect.Field fUserAns = ResultDialog.class.getDeclaredField("userAnswers");
        fUserAns.setAccessible(true);
        int[] resultUserAns = (int[]) fUserAns.get(resultDialog);
        if (resultUserAns[0] != 0 || resultUserAns[1] != 0) {
            throw new AssertionError("ResultDialog userAnswers phai giu nguyen dap an da tra loi!");
        }

        ReviewAnswersDialog reviewDlg = new ReviewAnswersDialog(resultDialog, questions, resultUserAns,
                new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, ms.getUserFillInAnswers());
        boolean hasCorrect = false;
        for (Component comp : getAllComponents(reviewDlg)) {
            if (comp instanceof ModernBadge badge) {
                if (badge.getText() != null && badge.getText().contains("Đúng 2/10 câu")) {
                    hasCorrect = true;
                    break;
                }
            }
        }
        reviewDlg.dispose();
        resultDialog.dispose();

        if (!hasCorrect) {
            throw new AssertionError("ReviewAnswersDialog mo tu ResultDialog phai hien thi 'Đúng 2/10 câu'!");
        }
        System.out.println("  [PASS] Du lieu dap an truyen tu MatchScreen -> ResultDialog -> ReviewAnswersDialog dong bo hoan hao!");
    }

    private static List<Component> getAllComponents(java.awt.Container c) {
        List<Component> list = new ArrayList<>();
        for (Component comp : c.getComponents()) {
            list.add(comp);
            if (comp instanceof java.awt.Container child) {
                list.addAll(getAllComponents(child));
            }
        }
        return list;
    }

    private static List<MatchQuestion> createSampleQuestions() {
        List<MatchQuestion> list = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            String type = (i == 3) ? "FILL_IN" : ((i == 4) ? "DRAG_AND_DROP" : "MULTIPLE_CHOICE");
            list.add(new MatchQuestion("Cau hoi so " + (i + 1), new String[]{"A", "B", "C", "D"}, "Giai thich", type, null, null, (i == 3 || i == 4) ? 1 : 0));
        }
        return list;
    }
}
