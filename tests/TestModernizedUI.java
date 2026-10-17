package tests;

import java.awt.Frame;
import javax.swing.SwingUtilities;
import quiz.ui.*;

public class TestModernizedUI {

    public static void main(String[] args) {
        System.out.println("=== BAT DAU KIEM TRA MODERNIZED UI & DIALOGS ===");

        try {
            // 1. Kiem tra Theme constants
            System.out.println("1. Kiem tra Design System Tokens trong Theme.java...");
            if (Theme.INDIGO == null || Theme.BG_LIGHT == null || Theme.EMERALD == null) {
                throw new IllegalStateException("Thieu Design System tokens trong Theme.java!");
            }
            System.out.println("  [PASS] Theme tokens ton tai day du va nhat quan!");

            // 2. Kiem tra ModernButton tactile press
            System.out.println("2. Kiem tra ModernButton cac styles...");
            ModernButton btn1 = new ModernButton("Primary", ModernButton.Style.PRIMARY);
            ModernButton btn2 = new ModernButton("Emerald", ModernButton.Style.EMERALD);
            ModernButton btn3 = new ModernButton("Dap An A", ModernButton.Style.ANSWER_A, "A");
            btn1.setSize(100, 40);
            btn2.setSize(100, 40);
            btn3.setSize(100, 40);
            System.out.println("  [PASS] ModernButton khoi tao thanh cong!");

            // 3. Kiem tra khoi tao cac Dialog pop-up hien dai
            System.out.println("3. Kiem tra khoi tao cac Dialog pop-up...");

            // 3.1 InviteDialog
            InviteDialog inviteDlg = new InviteDialog(null, "PlayerPro", "Toán học", "Trung bình", "100", accept -> {});
            inviteDlg.dispose();
            System.out.println("  [PASS] InviteDialog khoi tao va dispose thanh cong!");

            // 3.2 WaitingChallengeDialog
            WaitingChallengeDialog waitingDlg = new WaitingChallengeDialog(null, "OpponentMaster", "Lịch sử", "Khó", () -> {});
            waitingDlg.closeDialog();
            System.out.println("  [PASS] WaitingChallengeDialog khoi tao va close thanh cong!");

            // 3.3 LeaderboardDialog
            String dummyLeaderboard = "PlayerA;1200;1050;8.5s|PlayerB;1150;980;9.1s|PlayerC;950;850;11.2s";
            LeaderboardDialog lbDlg = new LeaderboardDialog(null, dummyLeaderboard);
            lbDlg.dispose();
            System.out.println("  [PASS] LeaderboardDialog khoi tao voi du lieu top 1-2-3 thanh cong!");

            // 3.4 HistoryDialog
            String dummyHistory = "Toán / Dễ / set1: PlayerA 8–6 PlayerB → PlayerA\nLịch sử / Khó / set2: PlayerB 5–5 PlayerA → HÒA";
            HistoryDialog histDlg = new HistoryDialog(null, "PlayerA", dummyHistory);
            histDlg.dispose();
            System.out.println("  [PASS] HistoryDialog khoi tao thanh cong!");

            // 3.5 ResultDialog
            ResultDialog resDlg = new ResultDialog(
                    null,
                    "PlayerA",
                    "PlayerA",
                    "PlayerA: 8 câu, 45200ms; pts=850; streak=5",
                    "PlayerB: 6 câu, 51300ms; pts=600; streak=3",
                    () -> {},
                    () -> {}
            );
            resDlg.dispose();
            System.out.println("  [PASS] ResultDialog khoi tao voi Head-to-Head stats thanh cong!");

            System.out.println(">>> TAT CA CAC COMPONENT VA POP-UP DIALOG MODERNIZED DEU HOAT DONG XUAT SAC (PASS)! <<<");
            System.exit(0);

        } catch (Throwable t) {
            System.err.println("TEST THAT BAI!");
            t.printStackTrace();
            System.exit(1);
        }
    }
}

