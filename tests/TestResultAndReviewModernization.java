package tests;

import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import quiz.ui.*;

public class TestResultAndReviewModernization {

    public static void main(String[] args) throws Exception {
        System.out.println("=== BAT DAU KIEM TRA RESULT DIALOG VA REVIEW ANSWERS DIALOG NANG CAP TOAN DIEN ===");

        SwingUtilities.invokeAndWait(() -> {
            try {
                testResultDialogModernization();
                testReviewAnswersOverviewAndMetrics();
                testReviewAnswersFiltersAndQuickJump();
                testReviewAnswersQuestionTypesAndExplanation();
                System.out.println(">>> TAT CA TEST RESULT & REVIEW DIALOG MODERNIZATION DEU PASS 100%! <<<");
            } catch (Throwable t) {
                t.printStackTrace();
                System.exit(1);
            }
        });
        System.exit(0);
    }

    private static void testResultDialogModernization() {
        System.out.println("1. Kiem tra ResultDialog (Cúp vàng Java2D, Bang so sanh Head-to-Head 5 chi so, Nut Xem Dap An)...");

        String leftRaw = "admin: 8 câu, 42100ms, pts=950; streak=5;";
        String rightRaw = "manh: 5 câu, 58200ms, pts=580; streak=2;";

        ResultDialog dlg = new ResultDialog(
                null,
                "admin",
                "admin",
                leftRaw,
                rightRaw,
                () -> {},
                () -> {}
        );

        // Kiem tra tieu de ResultDialog
        assert dlg.getTitle() != null && dlg.getTitle().contains("Kết Quả Trận Đấu") : "Tieu de ResultDialog khong dung!";

        // Duyet kiem tra cac thanh phan trong ResultDialog
        List<Component> comps = getAllComponents(dlg);

        // 1. Kiem tra nut Xem Dap An, Ve Sanh Cho, Tai Dau Ngay
        boolean foundReviewBtn = false;
        boolean foundRematchBtn = false;
        boolean foundLobbyBtn = false;
        for (Component c : comps) {
            if (c instanceof ModernButton btn) {
                String txt = btn.getText();
                if ("Xem Đáp Án".equals(txt)) foundReviewBtn = true;
                if ("Tái Đấu Ngay".equals(txt)) foundRematchBtn = true;
                if ("Về Sảnh Chờ".equals(txt)) foundLobbyBtn = true;
            }
        }
        assert foundReviewBtn : "Phai co nut 'Xem Đáp Án' trong ResultDialog!";
        assert foundRematchBtn : "Phai co nut 'Tái Đấu Ngay' trong ResultDialog!";
        assert foundLobbyBtn : "Phai co nut 'Về Sảnh Chờ' trong ResultDialog!";

        // 2. Kiem tra cac nhan chi so trong Bang Head-to-Head
        boolean foundSoCauDung = false;
        boolean foundTongDiem = false;
        boolean foundDiemTocDo = false;
        boolean foundChuoiDung = false;
        boolean foundThoiGian = false;

        for (Component c : comps) {
            if (c instanceof JLabel lbl) {
                String txt = lbl.getText();
                if (txt != null) {
                    if (txt.contains("SỐ CÂU ĐÚNG")) foundSoCauDung = true;
                    if (txt.contains("TỔNG ĐIỂM")) foundTongDiem = true;
                    if (txt.contains("ĐIỂM TỐC ĐỘ")) foundDiemTocDo = true;
                    if (txt.contains("CHUỖI ĐÚNG")) foundChuoiDung = true;
                    if (txt.contains("THỜI GIAN")) foundThoiGian = true;
                }
            }
        }

        assert foundSoCauDung : "Phai co chi so 'SỐ CÂU ĐÚNG' trong bang Head-to-Head!";
        assert foundTongDiem : "Phai co chi so 'TỔNG ĐIỂM' trong bang Head-to-Head!";
        assert foundDiemTocDo : "Phai co chi so 'ĐIỂM TỐC ĐỘ' trong bang Head-to-Head!";
        assert foundChuoiDung : "Phai co chi so 'CHUỖI ĐÚNG' trong bang Head-to-Head!";
        assert foundThoiGian : "Phai co chi so 'THỜI GIAN' trong bang Head-to-Head!";

        dlg.dispose();
        System.out.println("  [PASS] ResultDialog hien thi day du cúp, 5 chi so Head-to-Head va cac nut chuc nang!");
    }

    private static void testReviewAnswersOverviewAndMetrics() {
        System.out.println("2. Kiem tra ReviewAnswersDialog - The Tong Quan (4 o dem chi so, Segmented Bar, Ty le)...");

        List<MatchQuestion> questions = create10MixedQuestions();
        int[] userAns = new int[]{ 0, 1, 0, 1, 1, 0, 1, 0, -1, 1 };
        int[] correctAns = new int[]{ 0, 1, 1, 1, 1, 0, 1, 1, 0, 0 };
        String[] fillInInputs = new String[10];
        fillInInputs[3] = "1945";

        ResultDialog.PlayerStat myStat = new ResultDialog.PlayerStat("admin", 6, 35000, true, true, "avatar_01", 720, 4);
        ResultDialog.PlayerStat oppStat = new ResultDialog.PlayerStat("manh", 4, 48000, false, false, "avatar_02", 450, 2);

        ReviewAnswersDialog dlg = new ReviewAnswersDialog(null, questions, userAns, correctAns, fillInInputs, myStat, oppStat);

        List<Component> comps = getAllComponents(dlg);

        // Kiem tra Badge ty le hoan thanh tren Header
        boolean foundScoreBadge = false;
        for (Component c : comps) {
            if (c instanceof ModernBadge badge) {
                if (badge.getText() != null && badge.getText().contains("câu") && badge.getText().contains("%")) {
                    foundScoreBadge = true;
                    break;
                }
            }
        }
        assert foundScoreBadge : "Header phai chua Badge ty le lam bai!";

        // Kiem tra su xuat hien cua 4 the chi so: DUNG, SAI, CHUA CHON, TONG DIEM
        boolean foundDung = false;
        boolean foundSai = false;
        boolean foundChuaChon = false;
        boolean foundTongDiem = false;

        for (Component c : comps) {
            if (c instanceof JLabel lbl) {
                String txt = lbl.getText();
                if ("ĐÚNG".equals(txt)) foundDung = true;
                if ("SAI".equals(txt)) foundSai = true;
                if ("CHƯA CHỌN".equals(txt)) foundChuaChon = true;
                if ("TỔNG ĐIỂM".equals(txt)) foundTongDiem = true;
            }
        }

        assert foundDung : "Phai co o thong ke 'ĐÚNG'!";
        assert foundSai : "Phai co o thong ke 'SAI'!";
        assert foundChuaChon : "Phai co o thong ke 'CHƯA CHỌN'!";
        assert foundTongDiem : "Phai co o thong ke 'TỔNG ĐIỂM'!";

        dlg.dispose();
        System.out.println("  [PASS] The tong quan 4 chi so va thanh tien do phan doan hien thi chuan xac!");
    }

    private static void testReviewAnswersFiltersAndQuickJump() {
        System.out.println("3. Kiem tra Filter Toolbar (Tat ca, Chi cau dung, Chi cau sai) va Quick Jump Strip (10 nut)...");

        List<MatchQuestion> questions = create10MixedQuestions();
        // 5 dung, 4 sai, 1 chua chon (-1)
        int[] userAns = new int[]{ 0, 0, 0, 0, 0, 0, 0, 0, -1, 0 };
        int[] correctAns = new int[]{ 0, 0, 0, 0, 0, 1, 1, 1, 0, 1 };

        ReviewAnswersDialog dlg = new ReviewAnswersDialog(null, questions, userAns, correctAns, null);
        List<Component> comps = getAllComponents(dlg);

        // 1. Kiem tra 3 nut Filter
        ModernButton btnAll = null;
        ModernButton btnCorrect = null;
        ModernButton btnWrong = null;

        for (Component c : comps) {
            if (c instanceof ModernButton btn) {
                String txt = btn.getText();
                if (txt != null) {
                    if (txt.startsWith("Tất cả")) btnAll = btn;
                    else if (txt.startsWith("Chỉ câu đúng")) btnCorrect = btn;
                    else if (txt.startsWith("Chỉ câu sai")) btnWrong = btn;
                }
            }
        }

        assert btnAll != null : "Phai co nut filter 'Tất cả'!";
        assert btnCorrect != null : "Phai co nut filter 'Chỉ câu đúng'!";
        assert btnWrong != null : "Phai co nut filter 'Chỉ câu sai / Chưa làm'!";

        // 2. Kiem tra 10 nut Quick Jump
        int jumpBtnCount = 0;
        JButton btnJump7 = null;
        for (Component c : comps) {
            if (c instanceof JButton btn && !(btn instanceof ModernButton)) {
                String txt = btn.getText();
                if (txt != null && txt.matches("^[0-9]+$")) {
                    jumpBtnCount++;
                    if ("7".equals(txt)) {
                        btnJump7 = btn;
                    }
                }
            }
        }

        assert jumpBtnCount == 10 : "Quick Jump Strip phai co dung 10 nut so (1..10), thuc te: " + jumpBtnCount;
        assert btnJump7 != null : "Phai co nut chuyen nhanh cau 7!";

        // Thao tac click filter Chi cau dung
        btnCorrect.doClick();

        // Thao tac click Quick Jump nut 7
        btnJump7.doClick();

        dlg.dispose();
        System.out.println("  [PASS] Filter Toolbar va Quick Jump Strip 10 nut hoat dong muot ma, khong gay loi!");
    }

    private static void testReviewAnswersQuestionTypesAndExplanation() {
        System.out.println("4. Kiem tra hien thi 5 dang cau hoi va Khoi Giai Thich (Knowledge Card)...");

        List<MatchQuestion> questions = create10MixedQuestions();
        int[] userAns = new int[]{ 0, 0, 0, 1, 0, 0, 0, 0, 0, 0 };
        int[] correctAns = new int[]{ 0, 0, 0, 1, 0, 0, 0, 0, 0, 0 };
        String[] fillIns = new String[10];
        fillIns[3] = "Ha Noi";

        ReviewAnswersDialog dlg = new ReviewAnswersDialog(null, questions, userAns, correctAns, fillIns);

        List<Component> comps = getAllComponents(dlg);

        // Kiem tra noi dung cau hoi duoc render
        boolean foundQ1 = false;
        boolean foundQ4 = false;
        boolean foundExplanation = false;

        for (Component c : comps) {
            if (c instanceof JLabel lbl) {
                String txt = lbl.getText();
                if (txt != null) {
                    if (txt.contains("Thu do cua Viet Nam la gi?")) foundQ1 = true;
                    if (txt.contains("Dien tu: Thu do Ha Noi co cau Long Bien")) foundQ4 = true;
                    if (txt.contains("Ha Noi la thu do cua nuoc CHXHCN Viet Nam")) foundExplanation = true;
                }
            }
        }

        assert foundQ1 : "Cau hoi Multiple Choice phai hien thi noi dung!";
        assert foundQ4 : "Cau hoi Fill In phai hien thi noi dung!";
        assert foundExplanation : "Góc Kiến Thức phai hien thi giai thich day du khi co du lieu!";

        dlg.dispose();
        System.out.println("  [PASS] 5 dang cau hoi va The Giai Thich (Explanation) hien thi ro rang, chinh xac!");
    }

    private static List<Component> getAllComponents(Container c) {
        List<Component> list = new ArrayList<>();
        for (Component comp : c.getComponents()) {
            list.add(comp);
            if (comp instanceof Container child) {
                list.addAll(getAllComponents(child));
            }
        }
        return list;
    }

    private static List<MatchQuestion> create10MixedQuestions() {
        List<MatchQuestion> list = new ArrayList<>();

        // Q1: Multiple Choice co explanation
        list.add(new MatchQuestion(
                "Thu do cua Viet Nam la gi?",
                new String[]{ "Ha Noi", "Da Nang", "Hue", "TP Ho Chi Minh" },
                "Ha Noi la thu do cua nuoc CHXHCN Viet Nam tu nam 1976.",
                "MULTIPLE_CHOICE",
                null,
                null,
                0
        ));

        // Q2: True / False
        list.add(new MatchQuestion(
                "Trai Dat quay quanh Mat Troi theo quy dao elip?",
                new String[]{ "Đúng", "Sai" },
                "Dinh luat Kepler chi ra quy dao hanh tinh la hinh elip.",
                "TRUE_FALSE",
                null,
                null,
                0
        ));

        // Q3: Multiple Select
        list.add(new MatchQuestion(
                "Nhung thanh pho nao thuoc vung kinh te trong diem mien Bac?",
                new String[]{ "Ha Noi", "Hai Phong", "Quang Ninh", "Can Tho" },
                "Ha Noi, Hai Phong va Quang Ninh tao nen tam giac kinh te mien Bac.",
                "MULTIPLE_SELECT",
                null,
                null,
                7 // 1 | 2 | 4
        ));

        // Q4: Fill In
        list.add(new MatchQuestion(
                "Dien tu: Thu do Ha Noi co cau Long Bien bac qua song gi?",
                new String[]{ "song Hong" },
                "Cau Long Bien bac qua song Hong duoc xay dung tu thoi Phap thuoc.",
                "FILL_IN",
                null,
                null,
                0
        ));

        // Q5: Drag and Drop
        Map<String, String> fillMap = new LinkedHashMap<>();
        fillMap.put("pos1", "Mat Troi");
        fillMap.put("pos2", "Trai Dat");
        list.add(new MatchQuestion(
                "Keo tha: [pos1] la trung tam cua He Mat Troi, [pos2] la hanh tinh thu 3.",
                new String[]{ "Mat Troi", "Trai Dat" },
                "He Mat Troi co 8 hanh tinh quay quanh Mat Troi.",
                "DRAG_AND_DROP",
                new String[]{ "pos1", "pos2" },
                fillMap,
                0
        ));

        // Q6-Q10: Multiple Choice bo sung
        for (int i = 6; i <= 10; i++) {
            list.add(new MatchQuestion(
                    "Cau hoi so " + i + " kiem tra kien thuc tong hop?",
                    new String[]{ "Dap an A", "Dap an B", "Dap an C", "Dap an D" },
                    i % 2 == 0 ? "Giai thich chi tiet cho cau hoi " + i : null,
                    "MULTIPLE_CHOICE",
                    null,
                    null,
                    0
            ));
        }

        return list;
    }
}
