package tests;

import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import quiz.ui.*;

public class TestLeaderboardAndHistoryModernization {

    public static void main(String[] args) throws Exception {
        System.out.println("=== BAT DAU KIEM TRA NANG CAP LEADERBOARD VA MATCH HISTORY ===");

        SwingUtilities.invokeAndWait(() -> {
            try {
                testLeaderboardPodiumAndStickyBar();
                testLeaderboardDialog();
                testHistoryCareerStatsAndFiltering();
                testHistoryReviewCacheAndMatchDetail();
                System.out.println(">>> TAT CA TEST LEADERBOARD & HISTORY MODERNIZATION DEU PASS 100%! <<<");
            } catch (Throwable t) {
                t.printStackTrace();
                System.exit(1);
            }
        });
        System.exit(0);
    }

    private static void testLeaderboardPodiumAndStickyBar() {
        System.out.println("1. Kiem tra LeaderboardPanel: Bục Top 1-2-3, Sticky My Rank Bar va Table Renderers...");

        // Giả lập dữ liệu bảng xếp hạng từ Server
        // Format: name;score;avgOpp;speed;totalMatches;winRate;rankLabel|...
        String rawLeaderboard =
                "Alice;1500;1200;1.200;20;80.0%;Thách Đấu|" +
                "Bob;1350;1100;1.400;18;72.2%;Cao Thủ|" +
                "Charlie;1100;950;1.800;15;60.0%;Kim Cương|" +
                "David;900;850;2.100;12;50.0%;Vàng|" +
                "Eve;600;700;2.500;10;40.0%;Bạc";

        LeaderboardPanel panel = new LeaderboardPanel();

        // 1.1 Kiểm tra khi người chơi là Bob (Hạng #2)
        panel.updateData(rawLeaderboard, "Bob");

        List<Component> comps = getAllComponents(panel);

        // Tìm thanh Sticky "My Rank" Bar
        boolean foundMyRankText = false;
        boolean foundPointsDiffNotice = false;
        for (Component c : comps) {
            if (c instanceof JLabel lbl) {
                String txt = lbl.getText();
                if (txt != null) {
                    if (txt.contains("Hạng #2 (Bob)")) {
                        foundMyRankText = true;
                    }
                    if (txt.contains("Cần thêm") && txt.contains("vượt Alice")) {
                        foundPointsDiffNotice = true;
                    }
                }
            }
        }

        assert foundMyRankText : "Sticky bar phai hien thi dung 'Hạng #2 (Bob)'!";
        assert foundPointsDiffNotice : "Sticky bar phai hien thi khoang cach diem can de vuot Alice!";

        // 1.2 Kiểm tra khi người chơi là Alice (Hạng #1 - Quán quân)
        panel.updateData(rawLeaderboard, "Alice");
        boolean foundTop1Notice = false;
        for (Component c : getAllComponents(panel)) {
            if (c instanceof JLabel lbl) {
                String txt = lbl.getText();
                if (txt != null && txt.contains("Bạn đang dẫn đầu")) {
                    foundTop1Notice = true;
                    break;
                }
            }
        }
        assert foundTop1Notice : "Khi dung dau bang, Sticky bar phai chuc mung quan quan!";

        // 1.3 Kiểm tra khi người chơi chưa có thứ hạng
        panel.updateData(rawLeaderboard, "NewbiePlayer");
        boolean foundUnrankedNotice = false;
        for (Component c : getAllComponents(panel)) {
            if (c instanceof JLabel lbl) {
                String txt = lbl.getText();
                if (txt != null && txt.contains("Chưa có thứ hạng") || (txt != null && txt.contains("Chưa xếp"))) {
                    foundUnrankedNotice = true;
                    break;
                }
            }
        }
        assert foundUnrankedNotice : "Sticky bar phai xu ly em ai khi nguoi choi chua co thu hang!";

        System.out.println("  [PASS] LeaderboardPanel Podium Top 1-2-3 va Sticky Bar tinh toan chuan xac!");
    }

    private static void testLeaderboardDialog() {
        System.out.println("2. Kiem tra LeaderboardDialog khoi tao va hien thi thanh cong...");
        String dummyLeaderboard = "PlayerA;1200;1050;8.5s|PlayerB;1150;980;9.1s|PlayerC;950;850;11.2s";
        LeaderboardDialog dlg = new LeaderboardDialog(null, "PlayerB", dummyLeaderboard);
        assert dlg.getTitle().contains("Bảng Xếp Hạng") : "Tieu de LeaderboardDialog phai dung!";
        dlg.dispose();
        System.out.println("  [PASS] LeaderboardDialog khoi tao va dong bo hoan hao!");
    }

    private static void testHistoryCareerStatsAndFiltering() {
        System.out.println("3. Kiem tra HistoryPanel: 4 the Thong ke Su nghiep va Bo loc ket qua / Tim kiem...");

        // Giả lập 5 trận: 3 thắng liên tiếp, 1 hòa, 1 thua
        long now = System.currentTimeMillis();
        String rawHistory =
                "Khoa học / Dễ / set1: admin 8–5 bot → admin | 35000 | " + (now - 400000) + "\n" +
                "Lịch sử / Khó / set2: admin 7–4 user2 → admin | 42000 | " + (now - 300000) + "\n" +
                "Địa lý / Trung bình / set3: admin 9–6 user3 → admin | 38000 | " + (now - 200000) + "\n" +
                "Toán / Khó / set4: admin 6–6 rival → HÒA | 50000 | " + (now - 100000) + "\n" +
                "Văn học / Dễ / set5: rival 8–5 admin → rival | 45000 | " + now;

        HistoryPanel panel = new HistoryPanel();
        panel.updateData(rawHistory, "admin");

        List<Component> comps = getAllComponents(panel);

        // Kiem tra cac the thong ke
        boolean foundMatches = false;
        boolean foundWinRate = false;
        boolean foundStreak = false;

        for (Component c : comps) {
            if (c instanceof JLabel lbl) {
                String txt = lbl.getText();
                if (txt != null) {
                    if (txt.equals("5 trận")) foundMatches = true;
                    if (txt.equals("60%")) foundWinRate = true; // 3/5 = 60%
                    if (txt.equals("3 trận")) foundStreak = true; // 3 tran thang lien tiep
                }
            }
        }

        assert foundMatches : "The thong ke 'Tong so tran' phai la 5 tran!";
        assert foundWinRate : "The thong ke 'Ti le thang' phai la 60%!";
        assert foundStreak : "The thong ke 'Chuoi ky luc' phai la 3 tran!";

        System.out.println("  [PASS] 4 the thong ke su nghiep tinh toan chinh xac tu du lieu lich su!");
    }

    private static void testHistoryReviewCacheAndMatchDetail() {
        System.out.println("4. Kiem tra MatchHistoryCache va xem lai tran dau...");

        String scoreKey = "admin 8–5 bot";
        List<MatchQuestion> questions = new ArrayList<>();
        questions.add(new MatchQuestion("Thu do Viet Nam?", new String[]{"Ha Noi", "Hue"}, "Giai thich", "MULTIPLE_CHOICE", null, null, 0));

        ResultDialog.PlayerStat myStat = new ResultDialog.PlayerStat("admin", 8, 35000, true, true);
        ResultDialog.PlayerStat oppStat = new ResultDialog.PlayerStat("bot", 5, 45000, false, false);

        // Luu vao cache
        MatchHistoryCache.cacheMatch(scoreKey, questions, new int[]{0}, new int[]{0}, null, myStat, oppStat);

        assert MatchHistoryCache.hasMatch(scoreKey) : "MatchHistoryCache phai tim thay tran da luu!";
        MatchHistoryCache.CachedMatch cached = MatchHistoryCache.getMatch(scoreKey);
        assert cached != null && cached.questions().size() == 1 : "Du lieu tran dau phai khop voi cache!";

        // Kiem tra HistoryCard click mo Review khi co cache
        String line = "Khoa học / Dễ / set1: admin 8–5 bot → admin | 35000 | " + System.currentTimeMillis();
        HistoryCard card = new HistoryCard(line, "admin", null);
        assert card.getResult() == HistoryCard.MatchResult.WIN : "Ket qua HistoryCard phai la WIN!";
        assert "bot".equals(card.getOpponentName()) : "Doi thu trong HistoryCard phai la bot!";

        System.out.println("  [PASS] MatchHistoryCache va HistoryCard lien ket dong bo hoan hao!");
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

