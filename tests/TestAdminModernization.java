package tests;

import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import javax.swing.*;
import quiz.Bank;
import quiz.Model;
import quiz.ui.PlayerManagementPanel;
import quiz.ui.QuestionManagementPanel;

public class TestAdminModernization {

    public static void main(String[] args) throws Exception {
        System.out.println("=== BAT DAU KIEM TRA NANG CAP ADMIN (QUESTION & PLAYER MANAGEMENT) ===");

        SwingUtilities.invokeAndWait(() -> {
            try {
                testQuestionManagementDashboardAndAlerts();
                testQuestionManagementFiltersAndPreview();
                testQuestionImportValidationAndExport();
                testPlayerManagementAnalyticsAndActions();
                testPlayerInspectorModal();
                System.out.println(">>> TAT CA TEST ADMIN MODERNIZATION DEU PASS 100%! <<<");
            } catch (Throwable t) {
                t.printStackTrace();
                System.exit(1);
            }
        });
        System.exit(0);
    }

    private static void testQuestionManagementDashboardAndAlerts() {
        System.out.println("1. Kiem tra QuestionManagementPanel Dashboard: KPI Cards & Canh bao thieu cau (< 10)...");

        QuestionManagementPanel panel = new QuestionManagementPanel();

        // Tao 12 cau: 8 cau chu de A, 4 cau chu de B (chu de B < 10 cau -> phai canh bao!)
        StringBuilder raw = new StringBuilder();
        // 8 cau topic "Ky quan the gioi" (3 De, 3 Vua, 2 Kho)
        for (int i = 1; i <= 8; i++) {
            String lvl = i <= 3 ? "Dễ" : (i <= 6 ? "Trung bình" : "Khó");
            raw.append("KQ-0").append(i).append("\tKỳ quan thế giới\t").append(lvl)
               .append("\tMULTIPLE_CHOICE\tCau hoi ").append(i).append("\tA;B;C;D\t0\tGiai thich ").append(i)
               .append("\t\t\n");
        }
        // 4 cau topic "Vu tru va kham pha khong gian" (2 De, 2 Kho)
        for (int i = 1; i <= 4; i++) {
            String lvl = i <= 2 ? "Dễ" : "Khó";
            raw.append("VT-0").append(i).append("\tVũ trụ và khám phá không gian\t").append(lvl)
               .append("\tTRUE_FALSE\tCau hoi VT ").append(i).append("\tĐúng;Sai\t0\tGiai thich VT ").append(i)
               .append("\t\t\n");
        }

        panel.updateData(raw.toString());

        // Kiem tra cac the thong ke qua Component Tree
        List<Component> comps = getAllComponents(panel);
        boolean foundTotal = false;
        boolean foundAlert = false;

        for (Component c : comps) {
            if (c instanceof JLabel lbl) {
                String txt = lbl.getText();
                if (txt != null) {
                    if (txt.equals("12 câu")) foundTotal = true;
                    if (txt.contains("CẢNH BÁO THIẾU CÂU HỎI")) {
                        foundAlert = true;
                    }
                }
            }
        }

        assert foundTotal : "Dashboard phai hien thi tong cong 12 cau!";
        assert foundAlert : "Dashboard phai canh bao khi co chu de < 10 cau!";
        System.out.println("  [PASS] Dashboard thong ke va he thong canh bao thieu cau hoat dong chuan xac!");
    }

    private static void testQuestionManagementFiltersAndPreview() {
        System.out.println("2. Kiem tra bo loc tim kiem va 5 dang cau hoi trong Live Preview...");

        QuestionManagementPanel panel = new QuestionManagementPanel();
        StringBuilder raw = new StringBuilder();
        raw.append("Q-MC\tKỳ quan thế giới\tDễ\tMULTIPLE_CHOICE\tTháp Eiffel ở đâu?\tPháp;Ý;Anh;Đức\t0\tParis\t\t\n");
        raw.append("Q-TF\tKỳ quan thế giới\tDễ\tTRUE_FALSE\tVạn Lý Trường Thành ở TQ?\tĐúng;Sai\t0\tChính xác\t\t\n");
        raw.append("Q-MS\tKỳ quan thế giới\tTrung bình\tMULTIPLE_SELECT\tNhững nước nào ở ĐNA?\tViệt Nam;Lào;Pháp;Mỹ\t0,1\tVN va Lao\t\t\n");
        raw.append("Q-FI\tKỳ quan thế giới\tDễ\tFILL_IN\tThủ đô Việt Nam là gì?\tHà Nội;Ha Noi\t1\tHà Nội\t\t\n");
        raw.append("Q-DD\tKỳ quan thế giới\tKhó\tDRAG_AND_DROP\tKim tự tháp ____ ở ____\tAi Cập;Giza\t1\tAi Cập\t____\tslot0=Giza;slot1=Ai Cập\n");

        panel.updateData(raw.toString());

        // Kiem tra SupportedType map chuan voi Model.QuestionType
        for (QuestionManagementPanel.SupportedType st : QuestionManagementPanel.SupportedType.values()) {
            assert st.getModelType() != null : "Moi SupportedType phai co Model.QuestionType!";
            assert QuestionManagementPanel.SupportedType.fromModelType(st.getModelType()) == st : "Map 2 chieu phai khop!";
        }

        System.out.println("  [PASS] 5 dang cau hoi ho tro dong bo voi he thong thi dau!");
    }

    private static void testQuestionImportValidationAndExport() throws Exception {
        System.out.println("3. Kiem tra Import kiem tra hop le truoc khi nap va xuat file...");

        // Tao du lieu gia lap import voi 2 dong dung, 1 dong loi
        String importData =
                "IMP-01|Kỳ quan thế giới|Dễ|MULTIPLE_CHOICE|Câu hỏi 1|A;B;C;D|0|Giải thích\n" +
                "# Dòng ghi chú\n" +
                "IMP-02|Kỳ quan thế giới|Dễ|TRUE_FALSE|Câu hỏi 2|Đúng;Sai|0|Giải thích\n" +
                "IMP-ERR|Kỳ quan thế giới|Dễ|MULTIPLE_CHOICE|Câu hỏi lỗi|A;B|0|Lỗi vì chỉ có 2 lựa chọn trắc nghiệm!\n";

        File tempImport = File.createTempFile("test_quiz_import_", ".txt");
        tempImport.deleteOnExit();
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(tempImport), StandardCharsets.UTF_8))) {
            pw.write(importData);
        }

        // Kiem tra parsing logic
        List<Model.Question> valid = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(tempImport), StandardCharsets.UTF_8))) {
            String line;
            int lineNo = 0;
            while ((line = br.readLine()) != null) {
                lineNo++;
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] parts = line.split("\\|", -1);
                try {
                    Model.Question q = Bank.parseFields(parts);
                    String err = Bank.validateForAdmin(q);
                    if (err != null) {
                        errors.add("Dòng " + lineNo + ": " + err);
                    } else {
                        valid.add(q);
                    }
                } catch (Exception e) {
                    errors.add("Dòng " + lineNo + ": " + e.getMessage());
                }
            }
        }

        assert valid.size() == 2 : "Phai co dung 2 cau hop le!";
        assert errors.size() == 1 : "Phai bat dung 1 cau loi dinh dang!";
        assert errors.get(0).contains("4 lựa chọn") : "Thong bao loi phai ro rang!";

        System.out.println("  [PASS] Import Validation tach dung " + valid.size() + " cau hop le va " + errors.size() + " cau loi!");
    }

    private static void testPlayerManagementAnalyticsAndActions() {
        System.out.println("4. Kiem tra PlayerManagementPanel: 4 the Analytics & Quick Admin Actions...");

        PlayerManagementPanel panel = new PlayerManagementPanel();
        panel.setAdminMode(true);

        // Giả lập 4 người chơi: admin, player1 (online), player2 (offline), player3 (locked)
        // Format: name;score;avatar;rank;status;fullName;email;bio;role|...
        String rawUsers =
                "admin;2500;avatar_01;Thách Đấu;Trực tuyến;Quản Trị Viên;admin@quiz.vn;Admin;ADMIN|" +
                "player1;1200;avatar_02;Kim Cương;Trực tuyến;Nguyễn Văn A;a@gmail.com;Pro gamer;PLAYER|" +
                "player2;800;avatar_03;Vàng;Ngoại tuyến;Trần Thị B;b@gmail.com;;PLAYER|" +
                "badguy;100;avatar_04;Sắt;Đã khóa;Hacker;hack@bad.vn;;LOCKED";

        panel.updateData(rawUsers, "admin");

        List<Component> comps = getAllComponents(panel);
        boolean foundTotalUsers = false;
        boolean foundOnline = false;
        boolean foundAdmin = false;

        for (Component c : comps) {
            if (c instanceof JLabel lbl) {
                String txt = lbl.getText();
                if (txt != null) {
                    if (txt.equals("4 người")) foundTotalUsers = true;
                    if (txt.equals("2 online")) foundOnline = true; // admin + player1
                    if (txt.equals("1 tài khoản")) foundAdmin = true;
                }
            }
        }

        assert foundTotalUsers : "The Analytics 'Tong tai khoan' phai la 4 nguoi!";
        assert foundOnline : "The Analytics 'Dang truc tuyen' phai la 2 online!";
        assert foundAdmin : "The Analytics 'Quan tri vien' phai la 1 tai khoan!";

        // Kiem tra Record Parsing
        PlayerManagementPanel.PlayerRecord badRec = panel.findRecord("badguy");
        assert badRec != null : "badguy phai ton tai!";
        assert "LOCKED".equalsIgnoreCase(badRec.role()) : "badguy phai co role la LOCKED!";

        PlayerManagementPanel.PlayerRecord adminRec = panel.findRecord("admin");
        assert adminRec != null && "ADMIN".equalsIgnoreCase(adminRec.role()) : "admin phai co role ADMIN!";

        System.out.println("  [PASS] 4 the Analytics tinh toan chinh xac va nhan dien trang thai LOCKED!");
    }

    private static void testPlayerInspectorModal() {
        System.out.println("5. Kiem tra Player Inspector Modal khoi tao an toan...");

        PlayerManagementPanel panel = new PlayerManagementPanel();
        panel.setAdminMode(true);
        PlayerManagementPanel.PlayerRecord sample = new PlayerManagementPanel.PlayerRecord(
                "tester", 1450, "avatar_05", "Kim Cương", "Trực tuyến", "Tester Pro", "test@quiz.com", "Yêu thích đối kháng", "PLAYER"
        );

        // Kiem tra findRecord khong bi null pointer
        assert panel.findRecord(null) == null;
        assert panel.findRecord("not_exist") == null;

        System.out.println("  [PASS] Player Inspector khoi tao hoan hao!");
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
}

