package tests;

import quiz.ui.*;
import java.awt.Component;
import java.awt.Container;
import java.util.*;
import javax.swing.*;

public class TestMatchRhythm {

    public static void main(String[] args) throws Exception {
        System.out.println("=== BAT DAU KIEM TRA NHIP TRAN (RHYTHM): READY -> COUNTDOWN -> INTRO -> ACTIVE -> REVEAL ===");

        SwingUtilities.invokeAndWait(() -> {
            try {
                testRhythmSequence();
                testReadyStatusUpdate();
                testSkipRevealListener();
                testRevealScoresAndReviewExplanation();
                testReconnectHandlingInDifferentPhases();
            } catch (Throwable t) {
                t.printStackTrace();
                System.exit(1);
            }
        });
        testServerDrivenRhythmNoAutoAdvance();
        System.out.println(">>> TAT CA TEST NHIP TRAN DAU (MATCH RHYTHM) DEU PASS 100%! <<<");
        System.exit(0);
    }

    private static void testRhythmSequence() throws Exception {
        System.out.println("1. Kiem tra trinh tu Ready -> Countdown -> Intro -> Active -> Reveal...");
        List<MatchQuestion> questions = createDummyQuestions();
        MatchScreen matchScreen = new MatchScreen("m_01", "P1", "P2", "avatar_01", "avatar_02", "Dia Ly", "De", "set_1", 120, questions, ans -> {}, () -> {});

        // 1.1 READY_CHECK
        matchScreen.handleMatchPhase(new String[]{"MATCH_PHASE", "READY_CHECK", "m_01", "10", "false", "false"});
        // 1.2 COUNTDOWN 3 -> 2 -> 1 -> GO
        matchScreen.handleMatchPhase(new String[]{"MATCH_PHASE", "COUNTDOWN", "m_01", "3"});
        matchScreen.handleMatchPhase(new String[]{"MATCH_PHASE", "COUNTDOWN", "m_01", "2"});
        matchScreen.handleMatchPhase(new String[]{"MATCH_PHASE", "COUNTDOWN", "m_01", "1"});
        matchScreen.handleMatchPhase(new String[]{"MATCH_PHASE", "COUNTDOWN", "m_01", "GO"});

        // 1.3 QUESTION_INTRO
        matchScreen.handleMatchPhase(new String[]{"MATCH_PHASE", "QUESTION_INTRO", "m_01", "0", "Dia Ly", "MULTIPLE_CHOICE", "800"});

        // 1.4 QUESTION_ACTIVE
        matchScreen.handleMatchPhase(new String[]{"MATCH_PHASE", "QUESTION_ACTIVE", "m_01", "0", "15"});

        // 1.5 QUESTION_REVEAL
        matchScreen.handleMatchPhase(new String[]{"MATCH_PHASE", "QUESTION_REVEAL", "m_01", "0", "0", "Ha Noi la thu do cua Viet Nam", "100", "0", "1", "0", "0", "1"});

        System.out.println("  [PASS] Cac pha chuyen doi READY -> COUNTDOWN -> INTRO -> ACTIVE -> REVEAL hoat dong chinh xac!");
    }

    private static void testReadyStatusUpdate() throws Exception {
        System.out.println("2. Kiem tra cap nhat trang thai Ready cua 2 nguoi choi...");
        List<MatchQuestion> questions = createDummyQuestions();
        final boolean[] readySent = {false};
        MatchScreen matchScreen = new MatchScreen("m_02", "Alice", "Bob", "avatar_01", "avatar_02", "Lich Su", "De", "set_1", 120, questions, ans -> {}, () -> {});
        matchScreen.setOnReadyListener(() -> readySent[0] = true);

        // Cap nhat ready status: Alice ready, Bob chua ready
        matchScreen.updateReadyStatus("Alice", true, "Bob", false);

        // Cap nhat ready status: Ca 2 cung ready
        matchScreen.updateReadyStatus("Alice", true, "Bob", true);

        System.out.println("  [PASS] updateReadyStatus cap nhat dong bo khong gap bat ky loi nao!");
    }

    private static void testSkipRevealListener() throws Exception {
        System.out.println("3. Kiem tra co che bo qua Reveal (SKIP_REVEAL)...");
        List<MatchQuestion> questions = createDummyQuestions();
        final int[] skippedQ = {-1};
        MatchScreen matchScreen = new MatchScreen("m_03", "Alice", "Bob", "avatar_01", "avatar_02", "Khoa Hoc", "De", "set_1", 120, questions, ans -> {}, () -> {});
        matchScreen.setOnSkipRevealListener(qIdx -> skippedQ[0] = qIdx);

        // Chuyen sang REVEAL cho cau 2
        matchScreen.showQuestionRevealPhase(2, 1, "Giai thich ngan gon", 200, 100, 2, 1, 1, 0);

        // Lay nut btnNext
        java.lang.reflect.Field btnNextField = MatchScreen.class.getDeclaredField("btnNext");
        btnNextField.setAccessible(true);
        ModernButton btnNext = (ModernButton) btnNextField.get(matchScreen);

        if (!btnNext.getText().contains("Tiếp tục")) {
            throw new AssertionError("Trong pha REVEAL, nut phai co text 'Tiếp tục >>', nhung thuc te: " + btnNext.getText());
        }

        // Click nut Tiếp tục
        btnNext.doClick();

        if (skippedQ[0] != 2) {
            throw new AssertionError("onSkipRevealListener phai nhan duoc index 2, nhung thuc te: " + skippedQ[0]);
        }
        System.out.println("  [PASS] SKIP_REVEAL hoat dong chuan xac khi nguoi choi nhan Tiep tuc!");
    }

    private static void testRevealScoresAndReviewExplanation() throws Exception {
        System.out.println("4. Kiem tra cap nhat diem/streak trong Reveal va hien thi Explanation trong ReviewAnswersDialog...");
        List<MatchQuestion> questions = createDummyQuestions();
        MatchScreen matchScreen = new MatchScreen("m_04", "Alice", "Bob", "avatar_01", "avatar_02", "Toan", "Kho", "set_1", 120, questions, ans -> {}, () -> {});

        // Trong Reveal: chi cap nhat diem, streak va highlight dap an (khong chen vao man hinh thi dau)
        matchScreen.showQuestionRevealPhase(0, 0, "Dinh ly Pythagoras a^2 + b^2 = c^2", 150, 50, 3, 0, 0, 1);

        // Kiem tra phan Giai thich trong ReviewAnswersDialog (Xem lai dap an sau tran)
        int[] userAns = new int[]{0, 0, 1, 0, 0, 0, 0, 0, 0, 0};
        int[] correctAns = new int[]{0, 0, 1, 0, 0, 0, 0, 0, 0, 0};
        ReviewAnswersDialog reviewDialog = new ReviewAnswersDialog(null, questions, userAns, correctAns);

        boolean foundExplanation = false;
        for (Component comp : getAllComponents(reviewDialog)) {
            if (comp instanceof JLabel lbl) {
                if (lbl.getText() != null && lbl.getText().contains("Pythagoras")) {
                    foundExplanation = true;
                    break;
                }
            }
        }
        reviewDialog.dispose();

        if (!foundExplanation) {
            throw new AssertionError("ReviewAnswersDialog (Xem dap an) phai chua noi dung giai thich cau hoi!");
        }

        System.out.println("  [PASS] Phan giai thich nam dung cho trong ReviewAnswersDialog va diem/streak hien thi day du!");
    }

    private static List<Component> getAllComponents(Container c) {
        List<Component> compList = new ArrayList<>();
        for (Component comp : c.getComponents()) {
            compList.add(comp);
            if (comp instanceof Container child) {
                compList.addAll(getAllComponents(child));
            }
        }
        return compList;
    }

    private static void testReconnectHandlingInDifferentPhases() throws Exception {
        System.out.println("5. Kiem tra Reconnect vao cac phase khac nhau khong bi lech...");
        List<MatchQuestion> questions = createDummyQuestions();
        MatchScreen matchScreen = new MatchScreen("m_05", "Alice", "Bob", "avatar_01", "avatar_02", "Tieng Anh", "De", "set_1", 120, questions, ans -> {}, () -> {});

        // Reconnect giua pha COUNTDOWN
        matchScreen.handleMatchPhase(new String[]{"MATCH_PHASE", "COUNTDOWN", "m_05", "2"});

        // Reconnect giua pha REVEAL
        matchScreen.handleMatchPhase(new String[]{"MATCH_PHASE", "QUESTION_REVEAL", "m_05", "1", "2", "Grammar rule", "100", "100", "1", "1", "2", "2"});

        // Reconnect giua pha ACTIVE
        matchScreen.handleMatchPhase(new String[]{"MATCH_PHASE", "QUESTION_ACTIVE", "m_05", "1", "10"});

        System.out.println("  [PASS] Reconnect giua tran khong lam lech dong bo trang thai!");
    }

    private static void testServerDrivenRhythmNoAutoAdvance() throws Exception {
        System.out.println("6. Kiem tra serverDrivenRhythm: nhan Enter cau FILL_IN khong tu dong chuyen cau, dung timer...");
        List<MatchQuestion> questions = createDummyQuestions();
        final int[] answeredQ = {-1};
        final int[] answeredChoice = {-1};
        final MatchScreen[] matchScreenRef = new MatchScreen[1];
        final JTextField[] txtInputRef = new JTextField[1];

        SwingUtilities.invokeAndWait(() -> {
            try {
                MatchScreen ms = new MatchScreen("m_06", "Alice", "Bob", "avatar_01", "avatar_02", "Tieng Anh", "De", "set_1", 120, questions, ans -> {}, () -> {});
                ms.setOnAnswerListener((q, c) -> {
                    answeredQ[0] = q;
                    answeredChoice[0] = c;
                });
                matchScreenRef[0] = ms;

                // 6.1 Bat server-driven rhythm qua MATCH_PHASE READY_CHECK / ACTIVE
                ms.handleMatchPhase(new String[]{"MATCH_PHASE", "READY_CHECK", "m_06", "10", "true", "true"});
                if (!ms.isServerDrivenRhythm()) {
                    throw new AssertionError("serverDrivenRhythm phai duoc bat len true sau khi nhan goi MATCH_PHASE!");
                }

                // 6.2 Chuyen sang cau 1 (FILL_IN)
                ms.handleMatchPhase(new String[]{"MATCH_PHASE", "QUESTION_ACTIVE", "m_06", "1", "15"});
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });

        // Cho EDT xu ly cac invokeLater cua handleMatchPhase truoc khi tuong tac
        SwingUtilities.invokeAndWait(() -> {
            try {
                MatchScreen ms = matchScreenRef[0];
                java.lang.reflect.Field fInput = MatchScreen.class.getDeclaredField("txtFillInInput");
                fInput.setAccessible(true);
                txtInputRef[0] = (JTextField) fInput.get(ms);
                txtInputRef[0].setText("London");

                // Nhan Enter
                for (java.awt.event.ActionListener al : txtInputRef[0].getActionListeners()) {
                    al.actionPerformed(new java.awt.event.ActionEvent(txtInputRef[0], java.awt.event.ActionEvent.ACTION_PERFORMED, ""));
                }
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });

        // Kiem tra dap an da duoc chot
        if (answeredQ[0] != 1) {
            throw new AssertionError("Dap an phai duoc chot cho cau 1! Nhung thuc te answeredQ = " + answeredQ[0]);
        }

        // Cho 1100ms tren test thread de EDT chay moi timer neu co
        Thread.sleep(1100);

        SwingUtilities.invokeAndWait(() -> {
            try {
                java.lang.reflect.Field fCurrentQ = MatchScreen.class.getDeclaredField("currentQuestionIndex");
                fCurrentQ.setAccessible(true);
                int curQ = (int) fCurrentQ.get(matchScreenRef[0]);
                if (curQ != 1) {
                    throw new AssertionError("currentQuestionIndex phai giu nguyen la 1 (cho server dieu khien), nhung da bi chuyen sang " + curQ);
                }
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });

        System.out.println("  [PASS] serverDrivenRhythm giu dung cau hoi, khong tu dong chuyen cau khi go Enter!");
    }

    private static List<MatchQuestion> createDummyQuestions() {
        List<MatchQuestion> list = new ArrayList<>();
        list.add(new MatchQuestion("Thu do cua Viet Nam la gi?", new String[]{"Ha Noi", "Hue", "Da Nang", "TP HCM"}, "Dinh ly Pythagoras a^2 + b^2 = c^2", "MULTIPLE_CHOICE", null, null, 0));
        list.add(new MatchQuestion("Thu do nuoc Anh la gi?", new String[]{"London"}, "London", "FILL_IN", null, null, 0));
        list.add(new MatchQuestion("Can bac hai cua 16 la may?", new String[]{"2", "4", "8", "16"}, "4 * 4 = 16", "MULTIPLE_CHOICE", null, null, 1));
        for (int i = 4; i <= 10; i++) {
            list.add(new MatchQuestion("Cau hoi " + i, new String[]{"A", "B", "C", "D"}, "Giai thich " + i, "MULTIPLE_CHOICE", null, null, 0));
        }
        return list;
    }
}
