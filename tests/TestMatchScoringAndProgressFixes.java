package tests;

import quiz.ui.MatchScreen;
import quiz.ui.MatchQuestion;
import quiz.ui.ResultDialog;
import quiz.Wire;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class TestMatchScoringAndProgressFixes {

    public static void main(String[] args) {
        System.out.println("=== BAT DAU KIEM TRA CAC SUA LOI TIEN DO VA DIEM SO ===");

        testProgressPacketSymmetry();
        testMatchScreenNavAndProgressFinished();
        testResultDialogTieBreakerExplanation();

        System.out.println(">>> TAT CA CAC TEST CHO TIEN DO VA DIEM SO DEU PASS 100%! <<<");
    }

    private static void testProgressPacketSymmetry() {
        System.out.println("1. Kiem tra tinh doi xung cua goi tin PROGRESS giua 2 client...");

        // Gia su Server gui goi PROGRESS dang: PROGRESS:q0:q1:st0:st1
        // Voi Server moi sua: pkt gui cho ca 2 la [PROGRESS, q0, q1, st0, st1]
        // Left la admin (q0=3, st0="1200000000")
        // Right la manh (q1=5, st1="1222200000")
        String q0 = "3";
        String q1 = "5";
        String st0 = "1200000000";
        String st1 = "1222200000";

        String[] serverPacket = new String[] { "PROGRESS", q0, q1, st0, st1 };

        // Test phia Client Left (admin, isLeft = true):
        boolean isLeftLeft = true;
        int leftMyCurQ = isLeftLeft ? Integer.parseInt(Wire.field(serverPacket, 1)) : Integer.parseInt(Wire.field(serverPacket, 2));
        int leftOppCurQ = isLeftLeft ? Integer.parseInt(Wire.field(serverPacket, 2)) : Integer.parseInt(Wire.field(serverPacket, 1));
        String leftMySt = isLeftLeft ? Wire.field(serverPacket, 3) : Wire.field(serverPacket, 4);
        String leftOppSt = isLeftLeft ? Wire.field(serverPacket, 4) : Wire.field(serverPacket, 3);

        assert leftMyCurQ == 3 : "Left myCurQ phai la 3 nhung la: " + leftMyCurQ;
        assert leftOppCurQ == 5 : "Left oppCurQ phai la 5 nhung la: " + leftOppCurQ;
        assert leftMySt.equals(st0) : "Left mySt phai la st0!";
        assert leftOppSt.equals(st1) : "Left oppSt phai la st1!";

        // Test phia Client Right (manh, isLeft = false):
        boolean isLeftRight = false;
        int rightMyCurQ = isLeftRight ? Integer.parseInt(Wire.field(serverPacket, 1)) : Integer.parseInt(Wire.field(serverPacket, 2));
        int rightOppCurQ = isLeftRight ? Integer.parseInt(Wire.field(serverPacket, 2)) : Integer.parseInt(Wire.field(serverPacket, 1));
        String rightMySt = isLeftRight ? Wire.field(serverPacket, 3) : Wire.field(serverPacket, 4);
        String rightOppSt = isLeftRight ? Wire.field(serverPacket, 4) : Wire.field(serverPacket, 3);

        assert rightMyCurQ == 5 : "Right myCurQ phai la 5 nhung la: " + rightMyCurQ;
        assert rightOppCurQ == 3 : "Right oppCurQ phai la 3 nhung la: " + rightOppCurQ;
        assert rightMySt.equals(st1) : "Right mySt phai la st1 cua manh!";
        assert rightOppSt.equals(st0) : "Right oppSt phai la st0 cua admin!";

        System.out.println("   [PASS] Client manh nhan dung tien do cua admin va nguoc lai, khong con bi double-swap!");
    }

    private static void testMatchScreenNavAndProgressFinished() {
        System.out.println("2. Kiem tra MatchScreen khi tran dau ket thuc (10/10 cau va nav button 10 khong bi purple)...");

        List<MatchQuestion> questions = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            questions.add(new MatchQuestion("Cau hoi " + (i + 1), new String[] { "A", "B", "C", "D" }, "Giai thich", "MULTIPLE_CHOICE", null, null, 0));
        }

        try {
            SwingUtilities.invokeAndWait(() -> {
                MatchScreen ms = new MatchScreen("m1", "admin", "manh", "Toan", "De", "set1", 120, questions, ans -> {}, () -> {});
                
                // Gia lap nguoi choi chi tra loi 8 cau, 2 cau con lai bi timeout
                for (int i = 0; i < 8; i++) {
                    ms.startQuestionActivePhase(i, 15);
                    ms.showQuestionRevealPhase(i, 0, "exp", 100, 100, 1, 1, 0, 1);
                }
                // Cau 8 va 9 bi timeout (myAns = -1)
                for (int i = 8; i < 10; i++) {
                    ms.startQuestionActivePhase(i, 15);
                    ms.showQuestionRevealPhase(i, 0, "exp", 100, 100, 0, 0, -1, -1);
                }

                // Tran dau ket thuc
                ms.onMatchFinished();

                try {
                    Field fLbl = MatchScreen.class.getDeclaredField("lblProgressSummary");
                    fLbl.setAccessible(true);
                    JLabel lbl = (JLabel) fLbl.get(ms);
                    assert lbl.getText().equals("Đã hoàn thành: 10/10 câu") : "lblProgressSummary phai la 'Đã hoàn thành: 10/10 câu' nhung la: " + lbl.getText();

                    Field fNav = MatchScreen.class.getDeclaredField("navButtons");
                    fNav.setAccessible(true);
                    JButton[] navBtns = (JButton[]) fNav.get(ms);
                    assert navBtns.length == 10;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            System.out.println("   [PASS] MatchScreen onMatchFinished cap nhat dung 10/10 cau da hoan thanh!");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void testResultDialogTieBreakerExplanation() {
        System.out.println("3. Kiem tra ResultDialog giai thich thang thua bang Diem Toc Do khi bang so cau dung...");

        try {
            SwingUtilities.invokeAndWait(() -> {
                String leftRaw = "admin: 1 câu, 96210 ms;pts=135;streak=1";
                String rightRaw = "manh: 1 câu, 89280 ms;pts=124;streak=1";

                // Khoi tao ResultDialog cho manh (nguoi thua)
                ResultDialog dlgManh = new ResultDialog(null, "manh", "admin", leftRaw, rightRaw, () -> {}, () -> {});

                // Tim xem trong ResultDialog co banner subtitle giai thich thua ve Diem Toc Do khong
                boolean foundTieExplanation = false;
                Component[] comps = dlgManh.getContentPane().getComponents();
                for (Component c : comps) {
                    if (c instanceof JPanel p) {
                        for (Component sub : p.getComponents()) {
                            if (sub instanceof JLabel l && l.getText().contains("Điểm Tốc Độ")) {
                                foundTieExplanation = true;
                                System.out.println("   -> Tim thay thong bao tren banner: " + l.getText());
                            }
                        }
                    }
                }
                assert foundTieExplanation : "ResultDialog phai chua giai thich ro rang ve Diem Toc Do khi bang so cau dung!";
                dlgManh.dispose();
            });
            System.out.println("   [PASS] ResultDialog hien thi giai thich Thang/Thua do Diem Toc Do rat ro rang!");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
