package tests;

import quiz.Bank;
import quiz.Model;
import quiz.ui.QuestionManagementPanel;
import quiz.ui.QuestionManagementPanel.SupportedType;

import java.util.*;

public class TestAdminQuestionManagement {

    public static void main(String[] args) throws Exception {
        System.out.println("=== BAT DAU KIEM TRA ADMIN QUESTION MANAGEMENT IMPROVEMENTS ===");

        testSupportedTypesMapping();
        testAddAllFiveQuestionTypesIntoBank();
        testValidationRulesForAdmin();

        System.out.println(">>> TAT CA TEST ADMIN QUESTION MANAGEMENT DEU THANH CONG (PASS)! <<<");
    }

    private static void testSupportedTypesMapping() {
        System.out.println("1. Kiem tra danh sach 5 loai cau hoi duoc ho tro...");
        SupportedType[] types = SupportedType.values();
        if (types.length != 5) {
            throw new AssertionError("Phai co dung 5 loai cau hoi, hien tai: " + types.length);
        }

        // Kiem tra tung loai co Model.QuestionType tuong ung
        if (SupportedType.MULTIPLE_CHOICE.getModelType() != Model.QuestionType.MULTIPLE_CHOICE) throw new AssertionError();
        if (SupportedType.TRUE_FALSE.getModelType() != Model.QuestionType.TRUE_FALSE) throw new AssertionError();
        if (SupportedType.MULTIPLE_SELECT.getModelType() != Model.QuestionType.MULTIPLE_SELECT) throw new AssertionError();
        if (SupportedType.FILL_IN.getModelType() != Model.QuestionType.FILL_IN) throw new AssertionError();
        if (SupportedType.DRAG_AND_DROP.getModelType() != Model.QuestionType.DRAG_AND_DROP) throw new AssertionError();

        System.out.println("  [PASS] 5 loai cau hoi da duoc xac dinh va anh xa chinh xac 100%!");
    }

    private static void testAddAllFiveQuestionTypesIntoBank() throws Exception {
        System.out.println("2. Kiem tra tao va them 5 loai cau hoi vao ngan hang de thi dau...");

        java.nio.file.Path tempFile = java.nio.file.Files.createTempFile("bank_test_", ".txt");
        tempFile.toFile().deleteOnExit();
        Bank bank = new Bank(tempFile);

        // 1. MULTIPLE_CHOICE
        String[] f1 = new String[]{
                "Q-TEST-MC", "Khoa học", "Dễ", "MULTIPLE_CHOICE",
                "Kim loại nào dẫn điện tốt nhất ở điều kiện thường?",
                "Bạc;Đồng;Vàng;Nhôm", "0", "Bạc (Ag) là kim loại có độ dẫn điện cao nhất.", "", ""
        };
        Model.Question q1 = Bank.parseFields(f1);
        String err1 = Bank.validateForAdmin(q1);
        if (err1 != null) throw new AssertionError("Loi q1: " + err1);
        if (!bank.addQuestion(q1)) throw new AssertionError("Khong them duoc q1");

        // 2. TRUE_FALSE
        String[] f2 = new String[]{
                "Q-TEST-TF", "Khoa học", "Dễ", "TRUE_FALSE",
                "Nước sôi ở 100 độ C tại áp suất khí quyển tiêu chuẩn 1 atm?",
                "Đúng;Sai", "0", "Ở 1 atm, điểm sôi của nước tinh khiết là 100 độ C.", "", ""
        };
        Model.Question q2 = Bank.parseFields(f2);
        String err2 = Bank.validateForAdmin(q2);
        if (err2 != null) throw new AssertionError("Loi q2: " + err2);
        if (!bank.addQuestion(q2)) throw new AssertionError("Khong them duoc q2");

        // 3. MULTIPLE_SELECT
        String[] f3 = new String[]{
                "Q-TEST-MS", "Khoa học", "Trung bình", "MULTIPLE_SELECT",
                "Những khí nào sau đây là khí hiếm (khí trơ)?",
                "Heli;Neon;Oxy;Argon", "0,1,3", "Heli, Neon và Argon là các khí hiếm thuộc nhóm 18.", "", ""
        };
        Model.Question q3 = Bank.parseFields(f3);
        String err3 = Bank.validateForAdmin(q3);
        if (err3 != null) throw new AssertionError("Loi q3: " + err3);
        if (!bank.addQuestion(q3)) throw new AssertionError("Khong them duoc q3");

        // 4. FILL_IN
        String[] f4 = new String[]{
                "Q-TEST-FILL", "Khoa học", "Trung bình", "FILL_IN",
                "Hành tinh nào gần Mặt Trời nhất trong Hệ Mặt Trời?",
                "Sao Thủy;Thủy Tinh;Mercury", "1", "Sao Thủy (Mercury) là hành tinh gần Mặt Trời nhất.", "", ""
        };
        Model.Question q4 = Bank.parseFields(f4);
        String err4 = Bank.validateForAdmin(q4);
        if (err4 != null) throw new AssertionError("Loi q4: " + err4);
        if (!bank.addQuestion(q4)) throw new AssertionError("Khong them duoc q4");

        // 5. DRAG_AND_DROP
        String[] f5 = new String[]{
                "Q-TEST-DND", "Khoa học", "Dễ", "DRAG_AND_DROP",
                "Công thức hóa học của nước là ____, còn của khí cacbonic là ____.",
                "H2O;CO2;O2;NaCl", "1", "Nước là H2O và cacbonic là CO2.", "b1;b2", "b1=H2O;b2=CO2"
        };
        Model.Question q5 = Bank.parseFields(f5);
        String err5 = Bank.validateForAdmin(q5);
        if (err5 != null) throw new AssertionError("Loi q5: " + err5);
        if (!bank.addQuestion(q5)) throw new AssertionError("Khong them duoc q5");

        // Them them 5 cau nua de du 10 cau tao tran
        for (int i = 6; i <= 10; i++) {
            String[] fExtra = new String[]{
                    "Q-EXTRA-" + i, "Khoa học", "Dễ", "MULTIPLE_CHOICE",
                    "Câu hỏi phụ số " + i + "?",
                    "A" + i + ";B" + i + ";C" + i + ";D" + i, "0", "", "", ""
            };
            Model.Question qEx = Bank.parseFields(fExtra);
            bank.addQuestion(qEx);
        }

        // Kiem tra tao tran dau thuc te bang generateMatchSet
        Model.SetData set = bank.generateMatchSet("Khoa học", "Dễ", new HashSet<>(), Model.ExamConfig.DEFAULT);
        if (set == null || set.questions().size() != 10) {
            throw new AssertionError("generateMatchSet phai sinh du 10 cau hoi thuc te!");
        }

        System.out.println("  [PASS] Ca 5 loai cau hoi duoc them vao ngan hang va dua vao tao tran dau thanh cong!");
    }

    private static void testValidationRulesForAdmin() {
        System.out.println("3. Kiem tra cac quy tac validation chat che...");

        // Test thieu topic
        String[] fNoTopic = new String[]{"Q-ERR", "", "Dễ", "MULTIPLE_CHOICE", "Cau hoi?", "A;B;C;D", "0", "", "", ""};
        Model.Question qNoTopic = Bank.parseFields(fNoTopic);
        if (Bank.validateForAdmin(qNoTopic) == null) throw new AssertionError("Phai bao loi khi thieu topic!");

        // Test MC thieu dap an
        String[] fMcMissingChoice = new String[]{"Q-ERR", "Toán", "Dễ", "MULTIPLE_CHOICE", "1+1=?", "2;3;4;", "0", "", "", ""};
        Model.Question qMcMissing = Bank.parseFields(fMcMissingChoice);
        if (Bank.validateForAdmin(qMcMissing) == null) throw new AssertionError("Phai bao loi khi thieu lua chon MC!");

        // Test TF khong dung 2 lua chon
        String[] fTfBadChoices = new String[]{"Q-ERR", "Toán", "Dễ", "TRUE_FALSE", "Mệnh đề?", "Đúng;Sai;Có lẽ", "0", "", "", ""};
        Model.Question qTfBad = Bank.parseFields(fTfBadChoices);
        // Bank.parseFields normalizes choices for TRUE_FALSE to exactly 2 choices, so let's verify answer bounds
        Model.Question qTfBadAns = new Model.Question("Q-ERR", "Toán", "Dễ", Model.QuestionType.TRUE_FALSE, "Mệnh đề?", new String[]{"Đúng", "Sai"}, 5, "");
        if (Bank.validateForAdmin(qTfBadAns) == null) throw new AssertionError("Phai bao loi khi answer TF khong phai 0 hoac 1!");

        // Test MS answer <= 0
        Model.Question qMsBadAns = new Model.Question("Q-ERR", "Toán", "Dễ", Model.QuestionType.MULTIPLE_SELECT, "Chọn nhiều?", new String[]{"A", "B", "C", "D"}, 0, "");
        if (Bank.validateForAdmin(qMsBadAns) == null) throw new AssertionError("Phai bao loi khi answer MS khong co dap an duoc chon!");

        // Test DND thieu blanks
        Model.Question qDndNoBlanks = new Model.Question("Q-ERR", "Toán", "Dễ", Model.QuestionType.DRAG_AND_DROP, "Hà Nội là ____", new String[]{"A", "B"}, 1, "", new String[0], new HashMap<>());
        if (Bank.validateForAdmin(qDndNoBlanks) == null) throw new AssertionError("Phai bao loi khi DND thieu blanks!");

        System.out.println("  [PASS] Cac quy tac validation phat hien va chan loi du lieu chinh xac!");
    }
}
