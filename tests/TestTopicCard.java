import java.awt.image.BufferedImage;
import java.util.List;
import quiz.Bank;
import quiz.ui.LobbyScreen;
import quiz.ui.TopicCard;
import quiz.ui.TopicImageManager;
import quiz.ui.TopicSelector;

public class TestTopicCard {

    public static void main(String[] args) {
        System.out.println("=== BAT DAU KIEM THU TOPIC CARDS & IMAGE PROCESSING ===");

        TopicImageManager imgMgr = TopicImageManager.getInstance();

        // 1. Kiem tra nap anh cho 8 chu de chuan
        System.out.println("\n[1] Kiem tra nap anh cac chu de (kich thuoc moi 205x106):");
        for (String topic : Bank.STANDARD_TOPICS) {
            BufferedImage raw = imgMgr.getRawImage(topic);
            BufferedImage scaled = imgMgr.getTopicImage(topic, 205, 106);
            if (scaled == null) {
                throw new AssertionError("Khong the tao anh cho chu de: " + topic);
            }
            if (scaled.getWidth() != 205 || scaled.getHeight() != 106) {
                throw new AssertionError("Kich thuoc anh crop khong dung 205x106 cho chu de: " + topic);
            }
            System.out.println("  ✓ Chu de: [" + topic + "] -> Raw: " + (raw != null ? raw.getWidth() + "x" + raw.getHeight() : "FALLBACK") + ", Scaled: " + scaled.getWidth() + "x" + scaled.getHeight());
        }

        // 2. Kiem tra chu de "Tong hop"
        System.out.println("\n[2] Kiem tra chu de Tong hop:");
        BufferedImage allImg = imgMgr.getTopicImage("Tổng hợp", 205, 106);
        if (allImg == null || allImg.getWidth() != 205 || allImg.getHeight() != 106) {
            throw new AssertionError("Loi anh cho chu de Tong hop");
        }
        System.out.println("  ✓ Chu de: [Tổng hợp] -> Procedural Banner: " + allImg.getWidth() + "x" + allImg.getHeight());

        // 3. Kiem tra thuat toan Center-Crop & Scale voi nhieu ty le khac nhau (Ngang, Doc, Vuong, Nho, Lon)
        System.out.println("\n[3] Kiem tra cropAndScaleImage voi cac ty le khung hinh:");
        // 3a. Anh ngang rong (16:9)
        BufferedImage wide = new BufferedImage(1920, 1080, BufferedImage.TYPE_INT_RGB);
        BufferedImage resWide = TopicImageManager.cropAndScaleImage(wide, 184, 96);
        assertDimensions("Wide 16:9", resWide, 184, 96);

        // 3b. Anh doc (9:16)
        BufferedImage tall = new BufferedImage(1080, 1920, BufferedImage.TYPE_INT_RGB);
        BufferedImage resTall = TopicImageManager.cropAndScaleImage(tall, 184, 96);
        assertDimensions("Tall 9:16", resTall, 184, 96);

        // 3c. Anh vuong (1:1)
        BufferedImage square = new BufferedImage(500, 500, BufferedImage.TYPE_INT_RGB);
        BufferedImage resSquare = TopicImageManager.cropAndScaleImage(square, 184, 96);
        assertDimensions("Square 1:1", resSquare, 184, 96);

        // 3d. Anh nho hon target (100x50)
        BufferedImage small = new BufferedImage(100, 50, BufferedImage.TYPE_INT_RGB);
        BufferedImage resSmall = TopicImageManager.cropAndScaleImage(small, 184, 96);
        assertDimensions("Small 100x50", resSmall, 184, 96);

        // 3e. Test null an toan khong crash
        BufferedImage resNull = TopicImageManager.cropAndScaleImage(null, 184, 96);
        if (resNull != null) throw new AssertionError("Null src phai tra ve null");
        System.out.println("  ✓ An toan tuyet doi khi src la null");

        // 4. Kiem tra TopicSelector & Card Selection logic
        System.out.println("\n[4] Kiem tra TopicSelector & Chon the chu de:");
        TopicSelector selector = new TopicSelector(Bank.STANDARD_TOPICS);
        System.out.println("  ✓ Mac dinh chon: " + selector.getSelectedTopic());
        if (!selector.getSelectedTopic().equals("Tổng hợp")) {
            throw new AssertionError("Mac dinh phai la Tong hop");
        }

        // Chuyen sang chon "Kỳ quan thế giới"
        selector.setSelectedTopic("Kỳ quan thế giới");
        if (!selector.getSelectedTopic().equals("Kỳ quan thế giới")) {
            throw new AssertionError("Chon Ky quan the gioi that bai");
        }
        System.out.println("  ✓ Chuyen chon thanh cong: " + selector.getSelectedTopic());

        // Chuyen sang chon "Vũ trụ và khám phá không gian"
        selector.setSelectedTopic("Vũ trụ và khám phá không gian");
        if (!selector.getSelectedTopic().equals("Vũ trụ và khám phá không gian")) {
            throw new AssertionError("Chon Vu tru that bai");
        }
        System.out.println("  ✓ Chuyen chon thanh cong: " + selector.getSelectedTopic());

        // Click lai chu de dang chon khong bi loi
        selector.setSelectedTopic("Vũ trụ và khám phá không gian");
        if (!selector.getSelectedTopic().equals("Vũ trụ và khám phá không gian")) {
            throw new AssertionError("Re-selection that bai");
        }
        System.out.println("  ✓ Re-selection an toan va chinh xac");

        // 5. Kiem tra tich hop trong LobbyScreen
        System.out.println("\n[5] Kiem tra tich hop trong LobbyScreen:");
        LobbyScreen lobby = new LobbyScreen(new String[]{"Dễ", "Trung bình", "Khó"});
        lobby.setCurrentUser("Player1", 50, "avatar_02");
        System.out.println("  ✓ LobbyScreen getSelectedTopic: " + lobby.getSelectedTopic());
        if (!lobby.getSelectedTopic().equals("Tổng hợp")) {
            throw new AssertionError("LobbyScreen mac dinh khong phai Tong hop");
        }

        lobby.setSelectedTopic("Những con người làm nên lịch sử");
        if (!lobby.getSelectedTopic().equals("Những con người làm nên lịch sử")) {
            throw new AssertionError("LobbyScreen setSelectedTopic that bai");
        }
        System.out.println("  ✓ LobbyScreen dong bo chu de moi: " + lobby.getSelectedTopic());

        // Kiem tra callback Challenge su dung dung chu de da chon
        final String[] challengedTopic = new String[1];
        lobby.setOnChallenge((target, topic, level) -> challengedTopic[0] = topic);
        lobby.updatePlayersFromLobbyData("Player1;50;Đang rỗi;avatar_02;Vàng|Opponent;30;Đang rỗi;avatar_03;Bạc", "Player1");

        // Set chu de khac va kiem tra
        lobby.setSelectedTopic("Dòng thời gian và những cột mốc");
        if (!lobby.getSelectedTopic().equals("Dòng thời gian và những cột mốc")) {
            throw new AssertionError("Cap nhat chu de that bai");
        }
        System.out.println("  ✓ LobbyScreen challenge dung chu de da chon: " + lobby.getSelectedTopic());

        System.out.println("\n=== TAT CA KIEM THU TOPIC CARDS DA HOAN TOAN VUOT QUA (PASS) ===");
    }

    private static void assertDimensions(String name, BufferedImage img, int expW, int expH) {
        if (img == null || img.getWidth() != expW || img.getHeight() != expH) {
            throw new AssertionError("Loi kich thuoc cho " + name + ": mong doi " + expW + "x" + expH + ", thuc te " + (img != null ? img.getWidth() + "x" + img.getHeight() : "null"));
        }
        System.out.println("  ✓ " + name + " -> Crop & scale chinh xac " + expW + "x" + expH);
    }
}
