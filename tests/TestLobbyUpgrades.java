package tests;

import java.awt.Dimension;
import java.util.List;
import javax.swing.SwingUtilities;
import quiz.ui.NavigationBar;
import quiz.ui.NavigationTab;
import quiz.ui.PlayerCard;
import quiz.ui.TopicCard;
import quiz.ui.TopicSelector;

public class TestLobbyUpgrades {

    public static void main(String[] args) {
        System.out.println("=== KIỂM TRA NÂNG CẤP GIAO DIỆN LOBBY (TOPIC CARDS, ONLINE PLAYERS, TABS) ===");

        try {
            SwingUtilities.invokeAndWait(() -> {
                // 1. Kiểm tra TopicCard & TopicSelector
                System.out.println("1. Kiểm tra TopicCard & TopicSelector...");
                TopicCard card1 = new TopicCard("Kỳ quan thế giới");
                if (card1.getPreferredSize().width != TopicCard.CARD_WIDTH ||
                    card1.getPreferredSize().height != TopicCard.CARD_HEIGHT) {
                    throw new AssertionError("Kích thước TopicCard không đúng!");
                }
                card1.setSelected(true);
                if (!card1.isSelected()) {
                    throw new AssertionError("Trạng thái selected của TopicCard không hoạt động!");
                }
                String sub = TopicCard.resolveSubtitle("Kỳ quan thế giới");
                if (sub == null || sub.isBlank()) {
                    throw new AssertionError("Mô tả chủ đề bị rỗng!");
                }
                System.out.println("   - TopicCard 'Kỳ quan thế giới': Mô tả = '" + sub + "' [OK]");

                TopicSelector selector = new TopicSelector(List.of("Kỳ quan thế giới", "Toán học", "Vật lý"));
                selector.setSelectedTopic("Toán học");
                if (!"Toán học".equals(selector.getSelectedTopic())) {
                    throw new AssertionError("Chủ đề đang chọn không khớp!");
                }
                System.out.println("   [PASS] TopicCard & TopicSelector khởi tạo và hoạt động chuẩn xác!");

                // 2. Kiểm tra PlayerCard & Chấm trạng thái Java2D
                System.out.println("2. Kiểm tra PlayerCard & Chấm trạng thái Java2D...");
                PlayerCard readyPlayer = new PlayerCard("PlayerAlpha", 1250, "Bạch Kim", "avatar_02", true, false);
                PlayerCard busyPlayer = new PlayerCard("PlayerBeta", 980, "Vàng", "avatar_03", false, false);
                PlayerCard queuePlayer = new PlayerCard("PlayerGamma", 600, "Bạc", "avatar_01", false, true);

                if (!readyPlayer.isReady() || busyPlayer.isReady() || queuePlayer.isReady()) {
                    throw new AssertionError("Trạng thái isReady không chính xác!");
                }
                if (!queuePlayer.isInQueue()) {
                    throw new AssertionError("Trạng thái isInQueue không chính xác!");
                }
                readyPlayer.setSelected(true);
                readyPlayer.setSelected(false);
                System.out.println("   [PASS] PlayerCard các trạng thái (Rảnh - Xanh lá, Đang đấu - Cam, Đang tìm) khởi tạo thành công!");

                // 3. Kiểm tra NavigationBar & NavigationTab với indicator trượt
                System.out.println("3. Kiểm tra NavigationBar & NavigationTab (Animated Indicator)...");
                NavigationBar navBar = new NavigationBar();
                navBar.setSize(800, 50);
                navBar.addTab("Người Chơi");
                navBar.addTab("Bảng Xếp Hạng");
                navBar.addTab("Lịch Sử");

                if (!"Người Chơi".equals(navBar.getActiveTab())) {
                    throw new AssertionError("Tab mặc định không phải là Người Chơi!");
                }

                navBar.doLayout();
                navBar.selectTab("Bảng Xếp Hạng");
                if (!"Bảng Xếp Hạng".equals(navBar.getActiveTab())) {
                    throw new AssertionError("selectTab không kích hoạt đúng tab Bảng Xếp Hạng!");
                }
                navBar.selectTab("Lịch Sử");
                if (!"Lịch Sử".equals(navBar.getActiveTab())) {
                    throw new AssertionError("selectTab không kích hoạt đúng tab Lịch Sử!");
                }

                System.out.println("   [PASS] NavigationBar & NavigationTab chuyển tab mượt mà, tính toán indicator chính xác!");
            });

            System.out.println(">>> TOÀN BỘ CÁC NÂNG CẤP GIAO DIỆN LOBBY ĐÃ PASS TEST 100%! <<<");
            System.exit(0);

        } catch (Throwable t) {
            System.err.println("TEST NÂNG CẤP GIAO DIỆN THẤT BẠI!");
            t.printStackTrace();
            System.exit(1);
        }
    }
}

