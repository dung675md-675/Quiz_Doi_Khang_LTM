package quiz.ui;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * LobbyScreen - Màn hình Sảnh chờ (Home Screen) của Quiz Arena:
 * Hiển thị hồ sơ cá nhân (Tôi là ai, Điểm bao nhiêu), bộ chọn chủ đề/độ khó,
 * danh sách người chơi online dạng Player Card với trạng thái rõ ràng, và các
 * nút mở BXH / Lịch sử đấu.
 */
public class LobbyScreen extends JPanel {

    public interface ChallengeListener {
        void onChallenge(String targetUser, String topic, String level);
    }

    private String currentUsername = "";
    private int currentScore = 0;

    // Header Components
    private final JLabel lblUsername;
    private final ModernBadge badgeScore;
    private final JLabel lblAvatarLetter;
    private final JPanel avatarCircle;

    // Filter & Match Config Components
    private final JComboBox<String> cmbTopics;
    private final JComboBox<String> cmbLevels;

    // Player List Container
    private final JPanel playersListPanel;
    private final JLabel lblOnlineCount;
    private final ModernCard emptyStateCard;

    private ChallengeListener challengeListener;
    private Runnable leaderboardListener;
    private Runnable historyListener;
    private Runnable soloPracticeListener;

    public LobbyScreen(String[] levels) {
        setLayout(new BorderLayout());
        setBackground(Theme.OFF_WHITE);

        // ==========================================
        // 1. TOP HEADER (Brand + Profile + Actions)
        // ==========================================
        JPanel topHeader = new JPanel(new BorderLayout(16, 0));
        topHeader.setBackground(Theme.WHITE);
        topHeader.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER_LIGHT),
                new EmptyBorder(12, 24, 12, 24)));

        // Left: Game Logo & Connection badge
        JPanel brandPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        brandPanel.setOpaque(false);

        JLabel logo = new JLabel("QUIZ ARENA");
        logo.setFont(Theme.TITLE_XL);
        logo.setForeground(Theme.BLUE);
        brandPanel.add(logo);

        ModernBadge onlineBadge = new ModernBadge("Sảnh Online", ModernBadge.Type.READY);
        brandPanel.add(onlineBadge);

        topHeader.add(brandPanel, BorderLayout.WEST);

        // Right: Profile Widget + Action Buttons
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        rightPanel.setOpaque(false);

        ModernButton btnLeaderboard = new ModernButton("🏆 Bảng Xếp Hạng", ModernButton.Style.SECONDARY);
        btnLeaderboard.setPreferredSize(new Dimension(150, 36));
        btnLeaderboard.addActionListener(e -> {
            SoundManager.playClick();
            if (leaderboardListener != null)
                leaderboardListener.run();
        });

        ModernButton btnHistory = new ModernButton("Lịch Sử Đấu", ModernButton.Style.SECONDARY);
        btnHistory.setPreferredSize(new Dimension(135, 36));
        btnHistory.addActionListener(e -> {
            SoundManager.playClick();
            if (historyListener != null)
                historyListener.run();
        });

        JButton btnSound = new JButton(SoundManager.isMuted() ? "🔇" : "🔊");
        btnSound.setToolTipText("Bật/Tắt âm thanh");
        btnSound.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
        btnSound.setPreferredSize(new Dimension(38, 36));
        btnSound.setFocusPainted(false);
        btnSound.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnSound.setBackground(Theme.WHITE);
        btnSound.setBorder(BorderFactory.createLineBorder(Theme.BORDER_LIGHT, 1));
        btnSound.addActionListener(e -> {
            SoundManager.toggleMute();
            btnSound.setText(SoundManager.isMuted() ? "🔇" : "🔊");
        });

        rightPanel.add(btnSound);
        rightPanel.add(btnLeaderboard);
        rightPanel.add(btnHistory);

        // Avatar tròn nhỏ cho bản thân
        avatarCircle = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setupAntiAliasing(g2);
                g2.setColor(Theme.BLUE);
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        avatarCircle.setPreferredSize(new Dimension(36, 36));
        avatarCircle.setOpaque(false);

        lblAvatarLetter = new JLabel("?");
        lblAvatarLetter.setFont(Theme.BODY_BOLD);
        lblAvatarLetter.setForeground(Theme.WHITE);
        avatarCircle.add(lblAvatarLetter);
        rightPanel.add(avatarCircle);

        lblUsername = new JLabel("Đang tải...");
        lblUsername.setFont(Theme.SUBTITLE);
        lblUsername.setForeground(Theme.DARK_TEXT);
        rightPanel.add(lblUsername);

        badgeScore = new ModernBadge("0 điểm", ModernBadge.Type.RANK);
        rightPanel.add(badgeScore);

        topHeader.add(rightPanel, BorderLayout.EAST);
        add(topHeader, BorderLayout.NORTH);

        // ==========================================
        // 2. CENTER CONTENT (Settings Bar + Player Cards)
        // ==========================================
        JPanel centerContent = new JPanel();
        centerContent.setLayout(new BoxLayout(centerContent, BoxLayout.Y_AXIS));
        centerContent.setOpaque(false);
        centerContent.setBorder(new EmptyBorder(18, 24, 18, 24));

        // Match Config Card
        ModernCard configCard = new ModernCard(new BorderLayout(16, 0));
        configCard.setCardPadding(14, 20, 14, 20);
        configCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 65));

        JPanel configLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        configLeft.setOpaque(false);

        JLabel lblConfigTitle = new JLabel("Cấu hình trận đấu:");
        lblConfigTitle.setFont(Theme.BODY_BOLD);
        lblConfigTitle.setForeground(Theme.DARK_TEXT);
        configLeft.add(lblConfigTitle);

        configLeft.add(new JLabel("Chủ đề:"));
        cmbTopics = new JComboBox<>();
        cmbTopics.setFont(Theme.BODY);
        cmbTopics.setPreferredSize(new Dimension(200, 32));
        configLeft.add(cmbTopics);

        configLeft.add(new JLabel("Mức độ:"));
        cmbLevels = new JComboBox<>(levels != null ? levels : new String[] { "Dễ", "Trung bình", "Khó" });
        cmbLevels.setFont(Theme.BODY);
        cmbLevels.setPreferredSize(new Dimension(130, 32));
        configLeft.add(cmbLevels);

        configCard.add(configLeft, BorderLayout.WEST);

        JPanel configRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        configRight.setOpaque(false);

        JLabel lblNotice = new JLabel("10 câu • Đếm giờ");
        lblNotice.setFont(Theme.SMALL);
        lblNotice.setForeground(Theme.MUTED_GRAY);
        configRight.add(lblNotice);

        ModernButton btnPractice = new ModernButton("Luyện Tập Đơn", ModernButton.Style.PRIMARY);
        btnPractice.setPreferredSize(new Dimension(150, 36));
        btnPractice.setToolTipText("Luyện tập 10 câu hỏi ngay không cần chờ đối thủ");
        btnPractice.addActionListener(e -> {
            SoundManager.playClick();
            if (soloPracticeListener != null) {
                soloPracticeListener.run();
            }
        });
        configRight.add(btnPractice);

        configCard.add(configRight, BorderLayout.EAST);

        centerContent.add(configCard);
        centerContent.add(Box.createVerticalStrut(16));

        // Section Title: Online Players
        JPanel sectionHeader = new JPanel(new BorderLayout());
        sectionHeader.setOpaque(false);
        sectionHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        JLabel lblSection = new JLabel("DANH SÁCH NGƯỜI CHƠI TRỰC TUYẾN");
        lblSection.setFont(Theme.SUBTITLE);
        lblSection.setForeground(Theme.DARK_TEXT);
        sectionHeader.add(lblSection, BorderLayout.WEST);

        lblOnlineCount = new JLabel("0 người chơi");
        lblOnlineCount.setFont(Theme.SMALL_BOLD);
        lblOnlineCount.setForeground(Theme.MUTED_GRAY);
        sectionHeader.add(lblOnlineCount, BorderLayout.EAST);

        centerContent.add(sectionHeader);
        centerContent.add(Box.createVerticalStrut(10));

        // List Container for Player Cards
        playersListPanel = new JPanel();
        playersListPanel.setLayout(new BoxLayout(playersListPanel, BoxLayout.Y_AXIS));
        playersListPanel.setOpaque(false);

        // Empty state card
        emptyStateCard = new ModernCard(new BorderLayout(0, 10));
        emptyStateCard.setCardPadding(36, 20, 36, 20);
        emptyStateCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));

        JLabel iconEmpty = new JLabel("👥", SwingConstants.CENTER);
        iconEmpty.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 36));
        emptyStateCard.add(iconEmpty, BorderLayout.NORTH);

        JLabel lblEmptyTitle = new JLabel("Chưa có người chơi nào khác đang online", SwingConstants.CENTER);
        lblEmptyTitle.setFont(Theme.BODY_BOLD);
        lblEmptyTitle.setForeground(Theme.DARK_TEXT);

        JLabel lblEmptySub = new JLabel("Hãy mở thêm cửa sổ Client khác hoặc rủ bạn bè kết nối để bắt đầu so tài!",
                SwingConstants.CENTER);
        lblEmptySub.setFont(Theme.SMALL);
        lblEmptySub.setForeground(Theme.MUTED_GRAY);

        JPanel emptyTextPanel = new JPanel(new GridLayout(2, 1, 0, 4));
        emptyTextPanel.setOpaque(false);
        emptyTextPanel.add(lblEmptyTitle);
        emptyTextPanel.add(lblEmptySub);
        emptyStateCard.add(emptyTextPanel, BorderLayout.CENTER);

        playersListPanel.add(emptyStateCard);

        JScrollPane scrollPane = new JScrollPane(playersListPanel);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(Theme.OFF_WHITE);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        centerContent.add(scrollPane);
        add(centerContent, BorderLayout.CENTER);
    }

    public void setCurrentUser(String username) {
        this.currentUsername = username;
        this.lblUsername.setText(username);
        this.lblAvatarLetter
                .setText((username != null && !username.isEmpty()) ? username.substring(0, 1).toUpperCase() : "?");
        avatarCircle.repaint();
    }

    public void setCurrentScore(int score) {
        this.currentScore = score;
        this.badgeScore.setText(score + " điểm");
    }

    public void setTopics(List<String> topics) {
        cmbTopics.removeAllItems();
        for (String t : topics) {
            cmbTopics.addItem(t);
        }
    }

    public String getSelectedTopic() {
        return cmbTopics.getSelectedItem() != null ? cmbTopics.getSelectedItem().toString() : "";
    }

    public String getSelectedLevel() {
        return cmbLevels.getSelectedItem() != null ? cmbLevels.getSelectedItem().toString() : "";
    }

    public void setOnChallenge(ChallengeListener listener) {
        this.challengeListener = listener;
    }

    public void setOnLeaderboard(Runnable listener) {
        this.leaderboardListener = listener;
    }

    public void setOnHistory(Runnable listener) {
        this.historyListener = listener;
    }

    public void setOnSoloPractice(Runnable listener) {
        this.soloPracticeListener = listener;
    }

    /**
     * Cập nhật danh sách người chơi từ chuỗi packet LOBBY của Server:
     * Format: user;score;status|...
     */
    public void updatePlayersFromLobbyData(String lobbyData) {
        playersListPanel.removeAll();

        if (lobbyData == null || lobbyData.isBlank()) {
            lblOnlineCount.setText("0 người chơi khác");
            playersListPanel.add(emptyStateCard);
            playersListPanel.revalidate();
            playersListPanel.repaint();
            return;
        }

        String[] rows = lobbyData.split("\\|");
        List<PlayerCard> otherPlayers = new ArrayList<>();

        for (String row : rows) {
            if (row.isBlank())
                continue;
            String[] fields = row.split(";");
            if (fields.length < 3)
                continue;

            String name = fields[0];
            int score = 0;
            try {
                score = Integer.parseInt(fields[1]);
            } catch (NumberFormatException ignored) {
            }
            String status = fields[2];
            boolean isReady = status.equalsIgnoreCase("Đang rỗi");

            // Nếu là chính mình: cập nhật điểm vào Profile Header!
            if (name.equals(currentUsername)) {
                setCurrentScore(score);
            } else {
                // Là người chơi khác: tạo PlayerCard
                PlayerCard card = new PlayerCard(name, score, isReady);
                card.setOnChallenge(targetUser -> {
                    if (challengeListener != null) {
                        challengeListener.onChallenge(targetUser, getSelectedTopic(), getSelectedLevel());
                    }
                });
                otherPlayers.add(card);
            }
        }

        if (otherPlayers.isEmpty()) {
            lblOnlineCount.setText("0 người chơi khác");
            playersListPanel.add(emptyStateCard);
        } else {
            lblOnlineCount.setText(otherPlayers.size() + " người chơi khác trực tuyến");
            for (PlayerCard card : otherPlayers) {
                playersListPanel.add(card);
                playersListPanel.add(Box.createVerticalStrut(10));
            }
        }

        playersListPanel.revalidate();
        playersListPanel.repaint();
    }
}
