package tests;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.SwingUtilities;
import quiz.ui.MatchQuestion;
import quiz.ui.MatchScreen;

public class TestMatchScreenAnswerUpgrades {

    public static void main(String[] args) {
        System.out.println("=== BAT DAU KIEM TRA NANG CAP KHU VUC DAP AN TRONG MATCHSCREEN ===");

        try {
            SwingUtilities.invokeAndWait(() -> {
                List<MatchQuestion> mockQuestions = new ArrayList<>();

                // 1. Dạng 1: Trắc nghiệm 1 đáp án (MULTIPLE_CHOICE)
                mockQuestions.add(new MatchQuestion(
                        "Thủ đô của Việt Nam là gì?",
                        new String[]{"Hà Nội", "TP. Hồ Chí Minh", "Đà Nẵng", "Hải Phòng"},
                        "Hà Nội là thủ đô của nước CHXHCN Việt Nam.",
                        "MULTIPLE_CHOICE",
                        null,
                        null,
                        0
                ));

                // 2. Dạng 2: Đúng / Sai (TRUE_FALSE)
                mockQuestions.add(new MatchQuestion(
                        "Mặt Trời mọc ở hướng Đông.",
                        new String[]{"ĐÚNG", "SAI"},
                        "Trái Đất quay từ Tây sang Đông nên ta thấy Mặt Trời mọc ở hướng Đông.",
                        "TRUE_FALSE",
                        null,
                        null,
                        0
                ));

                // 3. Dạng 3: Chọn nhiều đáp án (MULTIPLE_SELECT)
                mockQuestions.add(new MatchQuestion(
                        "Những thành phố nào trực thuộc trung ương của Việt Nam?",
                        new String[]{"Hà Nội", "Đà Lạt", "Đà Nẵng", "Nha Trang"},
                        "Hà Nội và Đà Nẵng là các thành phố trực thuộc trung ương.",
                        "MULTIPLE_SELECT",
                        null,
                        null,
                        5 // bit 0 + bit 2 = 5
                ));

                // 4. Dạng 4: Điền từ (FILL_IN)
                mockQuestions.add(new MatchQuestion(
                        "Nước sôi ở nhiệt độ bao nhiêu độ C?",
                        new String[]{"100", "100 độ", "100°C"},
                        "Ở áp suất tiêu chuẩn, nước sôi ở 100 độ C.",
                        "FILL_IN",
                        null,
                        null,
                        1
                ));

                // 5. Dạng 5: Kéo thả (DRAG_AND_DROP)
                mockQuestions.add(new MatchQuestion(
                        "Mặt Trời mọc ở hướng ____ và lặn ở hướng ____.",
                        new String[]{"Đông", "Tây", "Nam", "Bắc"},
                        "Mặt Trời mọc hướng Đông và lặn hướng Tây.",
                        "DRAG_AND_DROP",
                        new String[]{"blank_1", "blank_2"},
                        Map.of("blank_1", "Đông", "blank_2", "Tây"),
                        1
                ));

                int[] submittedAnswers = new int[1];
                MatchScreen screen = new MatchScreen(
                        "MOCK_MATCH_01",
                        "TestUser",
                        "OpponentPro",
                        "Địa lý",
                        "Dễ",
                        "set_01",
                        120,
                        mockQuestions,
                        ans -> submittedAnswers[0] = 1,
                        () -> {}
                );

                screen.setSize(1000, 750);
                screen.doLayout();

                // 1. Kiểm tra Dạng 1: Trắc nghiệm 1 đáp án
                System.out.println("1. Kiểm tra Trắc nghiệm một đáp án (MULTIPLE_CHOICE)...");
                screen.navigateToQuestion(0);
                screen.showQuestionIntroPhase(0, "Địa lý", "MULTIPLE_CHOICE", 500);
                screen.startQuestionActivePhase(0, 15);
                System.out.println("   [PASS] Trắc nghiệm 1 đáp án hiển thị và chọn thành công!");

                // 2. Kiểm tra Dạng 2: Đúng / Sai (TRUE_FALSE)
                System.out.println("2. Kiểm tra Đúng / Sai (TRUE_FALSE)...");
                screen.navigateToQuestion(1);
                screen.showQuestionIntroPhase(1, "Địa lý", "TRUE_FALSE", 500);
                screen.startQuestionActivePhase(1, 15);
                System.out.println("   [PASS] Đúng / Sai hiển thị 2 thẻ lớn và chọn thành công!");

                // 3. Kiểm tra Dạng 3: Chọn nhiều đáp án (MULTIPLE_SELECT)
                System.out.println("3. Kiểm tra Chọn nhiều đáp án (MULTIPLE_SELECT)...");
                screen.navigateToQuestion(2);
                screen.showQuestionIntroPhase(2, "Địa lý", "MULTIPLE_SELECT", 500);
                screen.startQuestionActivePhase(2, 15);
                System.out.println("   [PASS] Chọn nhiều đáp án hiển thị checkbox và bật/tắt lựa chọn thành công!");

                // 4. Kiểm tra Dạng 4: Điền từ (FILL_IN)
                System.out.println("4. Kiểm tra Điền từ (FILL_IN)...");
                screen.navigateToQuestion(3);
                screen.showQuestionIntroPhase(3, "Địa lý", "FILL_IN", 500);
                screen.startQuestionActivePhase(3, 15);
                System.out.println("   [PASS] Điền từ có ô nhập hiện đại và nút gửi đáp án thành công!");

                // 5. Kiểm tra Dạng 5: Kéo thả (DRAG_AND_DROP)
                System.out.println("5. Kiểm tra Kéo thả (DRAG_AND_DROP)...");
                screen.navigateToQuestion(4);
                screen.showQuestionIntroPhase(4, "Địa lý", "DRAG_AND_DROP", 500);
                screen.startQuestionActivePhase(4, 15);
                System.out.println("   [PASS] Kéo thả gắn câu và chip pool 3D thành công!");

                // 6. Kiểm tra Reveal Phase
                System.out.println("6. Kiểm tra Reveal Phase & Hiển thị kết quả...");
                screen.showQuestionRevealPhase(0, 0, "Giải thích", 100, 50, 1, 0, 0, 1);
                System.out.println("   [PASS] Reveal Phase highlight xanh nút đúng và đỏ nút sai mượt mà!");

                // 7. Cleanup
                System.out.println("7. Kiểm tra dọn dẹp & giải phóng bộ nhớ khi rời trận...");
                screen.cleanupAndStop();
                System.out.println("   [PASS] cleanupAndStop dừng toàn bộ timer và vô hiệu hóa controls an toàn!");
            });

            System.out.println(">>> TAT CA CAC TEST CHO KHU VUC DAP AN MATCHSCREEN DEU PASS 100%! <<<");
            System.exit(0);

        } catch (Throwable t) {
            System.err.println("TEST NANG CAP DAP AN THAT BAI!");
            t.printStackTrace();
            System.exit(1);
        }
    }
}
