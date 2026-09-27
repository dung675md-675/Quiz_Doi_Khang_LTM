import quiz.DB;
import quiz.Model;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

public class TestPostgreSQL {
    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("       KIỂM THỬ KẾT NỐI VÀ DAO POSTGRESQL        ");
        System.out.println("=================================================");

        Path dataFolder = Path.of("data");
        try (DB db = new DB(dataFolder)) {
            if (!db.isAvailable()) {
                System.out.println("⚠️  Chưa kết nối được PostgreSQL server (Dịch vụ PostgreSQL chưa bật hoặc sai cấu hình trong data/db.properties).");
                System.out.println("ℹ️  Server sẽ tự động hoạt động ở chế độ lưu trữ File (users.dat, questions.txt).");
                System.out.println("👉 Khi bạn bật PostgreSQL và tạo database 'quiz_db', hãy chạy lại class này để kiểm tra!");
                return;
            }

            System.out.println("✅ Kết nối PostgreSQL thành công!");

            try (Connection conn = db.getConnection();
                 Statement st = conn.createStatement()) {

                // Kiểm tra các bảng đã được tự động tạo chưa
                String[] tables = {"users", "question_sets", "questions", "match_results"};
                for (String table : tables) {
                    try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + table)) {
                        if (rs.next()) {
                            System.out.println("  • Bảng [" + table + "]: " + rs.getInt(1) + " bản ghi");
                        }
                    }
                }
            }

            // Kiểm tra tải ngân hàng câu hỏi
            List<Model.SetData> sets = db.loadQuestionSets();
            System.out.println("✅ Ngân hàng câu hỏi trong PostgreSQL: " + sets.size() + " bộ đề (" + (sets.size() * 10) + " câu).");

            System.out.println("\n🎉 KIỂM THỬ POSTGRESQL THÀNH CÔNG RỰC RỠ!");
        } catch (Exception e) {
            System.err.println("❌ Lỗi trong quá trình kiểm thử PostgreSQL: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
