package quiz.ui;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import quiz.Rank;

/**
 * LobbyScreen — Màn hình Sảnh chờ với Quick Match, Rank, Avatar, và Quản lý Người chơi.
 */
public class LobbyScreen extends JPanel {

    public interface ChallengeListener {
        void onChallenge(String targetUser, String topic, String level);
    }

    private String currentUsername = "";
    private int currentScore = 0;
    private String currentAvatarId = "avatar_01";
    private String currentFullName = "";
    private String currentEmail = "";
    private String currentBio = "";
    private String currentRole = "PLAYER";

    // Header Components
    private final JLabel lblUsername;
    private final JLabel lblRankBadge;
    private final ModernBadge badgeScore;
    private final ModernBadge badgeRole;
    private final ModernBadge badgePing;
    private final JLabel lblAvatarEmoji;
    private final JPanel avatarCircle;
    private final NavigationBar navBar;

    // Filter & Match Config Components
    private final TopicSelector topicSelector;
    private final LevelSelector levelSelector;

    // CardLayout và các tab
    private final CardLayout cardLayout;
    private final JPanel cardPanel;

    // Player List Container
    private final JPanel playersListPanel;
    private final JLabel lblOnlineCount;
    private final ModernCard emptyStateCard;

    // Quick Match UI
    private final JPanel quickMatchStatusPanel;
    private final JLabel lblQuickMatchStatus;
    private final JLabel lblQuickMatchTimer;
    private final ModernButton btnQuickMatch;
    private final ModernButton btnCancelMatchmaking;
    private Timer quickMatchTimer;
    private int quickMatchSeconds = 0;
    private boolean isMatchmaking = false;

    // Tab panels
    private final LeaderboardPanel leaderboardPanel;
    private final HistoryPanel historyPanel;
    private final PlayerManagementPanel playerManagementPanel;
    private final QuestionManagementPanel questionManagementPanel;

    // Listeners
    private ChallengeListener challengeListener;
    private Runnable logoutListener;
    private Consumer<String> tabChangedListener;
    private Runnable soloPracticeListener;
    private Runnable quickMatchListener;
    private Runnable cancelMatchmakingListener;
    private Consumer<String> updateAvatarListener;

    public LobbyScreen(String[] levels) {
        setLayout(new BorderLayout());
        setBackground(Theme.OFF_WHITE);

        // ===================================================
        // 1. TOP HEADER
        // ===================================================
        JPanel topHeader = new JPanel(new BorderLayout(16, 0));
        topHeader.setBackground(Theme.WHITE);
        topHeader.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER_LIGHT),
                new EmptyBorder(12, 24, 12, 24)));

        // Left: Game Logo + Online Badge + Ping Badge
        JPanel brandPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        brandPanel.setOpaque(false);
        JLabel logo = new JLabel("QUIZ ARENA");
        logo.setFont(Theme.TITLE_XL);
        logo.setForeground(Theme.BLUE);
        brandPanel.add(logo);
        ModernBadge onlineBadge = new ModernBadge("Sảnh Online", ModernBadge.Type.READY);
        brandPanel.add(onlineBadge);
        badgePing = new ModernBadge("Ping: -- ms", ModernBadge.Type.INFO);
        brandPanel.add(badgePing);
        topHeader.add(brandPanel, BorderLayout.WEST);

        // Right: Profile + Actions
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setOpaque(false);

        ModernButton btnSoundSettings = new ModernButton(getSoundButtonLabel(), ModernButton.Style.SECONDARY);
        btnSoundSettings.setPreferredSize(new Dimension(108, 36));
        btnSoundSettings.setToolTipText("Cài đặt âm thanh & nhạc nền (Bật/Tắt & Âm lượng)");
        btnSoundSettings.addActionListener(e -> {
            SoundManager.playClick();
            new SoundSettingsDialog(SwingUtilities.getWindowAncestor(this)).setVisible(true);
            btnSoundSettings.setText(getSoundButtonLabel());
        });

        ModernButton btnProfile = new ModernButton("Hồ Sơ", ModernButton.Style.SECONDARY);
        btnProfile.setPreferredSize(new Dimension(88, 36));
        btnProfile.setToolTipText("Sửa thông tin cá nhân, mật khẩu hoặc xóa tài khoản");
        btnProfile.addActionListener(e -> {
            SoundManager.playClick();
            openCurrentUserProfileDialog();
        });

        ModernButton btnLogout = new ModernButton("Đăng Xuất", ModernButton.Style.SECONDARY);
        btnLogout.setPreferredSize(new Dimension(110, 36));
        btnLogout.addActionListener(e -> {
            SoundManager.playClick();
            if (logoutListener != null)
                logoutListener.run();
        });

        // Avatar tròn clickable — hiển thị ảnh tròn
        avatarCircle = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setupAntiAliasing(g2);
                int size = Math.min(getWidth(), getHeight());
                int ox = (getWidth() - size) / 2;
                int oy = (getHeight() - size) / 2;
                AvatarPicker.paintAvatar(g2, currentAvatarId, ox, oy, size);
                g2.dispose();
            }
        };
        avatarCircle.setPreferredSize(new Dimension(40, 40));
        avatarCircle.setOpaque(false);
        avatarCircle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        avatarCircle.setToolTipText("Nhấn để đổi avatar");

        lblAvatarEmoji = new JLabel("");
        lblAvatarEmoji.setPreferredSize(new Dimension(0, 0));

        avatarCircle.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                Frame owner = (Frame) SwingUtilities.getWindowAncestor(LobbyScreen.this);
                AvatarPicker picker = new AvatarPicker(owner, currentAvatarId, newId -> {
                    if (updateAvatarListener != null)
                        updateAvatarListener.accept(newId);
                });
                picker.setVisible(true);
            }
        });

        lblUsername = new JLabel("Đang tải...");
        lblUsername.setFont(Theme.SUBTITLE);
        lblUsername.setForeground(Theme.DARK_TEXT);

        lblRankBadge = new JLabel("Sắt");
        lblRankBadge.setFont(Theme.SMALL_BOLD);
        lblRankBadge.setForeground(Theme.PURPLE);

        badgeScore = new ModernBadge("0 điểm", ModernBadge.Type.RANK);
        badgeRole = new ModernBadge("ADMIN", ModernBadge.Type.WARNING);
        badgeRole.setVisible(false);

        // Stack name + rank vertically
        JPanel profileInfo = new JPanel();
        profileInfo.setLayout(new BoxLayout(profileInfo, BoxLayout.Y_AXIS));
        profileInfo.setOpaque(false);
        profileInfo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        profileInfo.setToolTipText("Nhấn để sửa thông tin cá nhân");
        profileInfo.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                openCurrentUserProfileDialog();
            }
        });
        lblUsername.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblRankBadge.setAlignmentX(Component.LEFT_ALIGNMENT);
        profileInfo.add(lblUsername);
        profileInfo.add(lblRankBadge);

        rightPanel.add(btnSoundSettings);
        rightPanel.add(btnProfile);
        rightPanel.add(btnLogout);
        rightPanel.add(avatarCircle);
        rightPanel.add(profileInfo);
        rightPanel.add(badgeRole);
        rightPanel.add(badgeScore);
        topHeader.add(rightPanel, BorderLayout.EAST);
        add(topHeader, BorderLayout.NORTH);

        // ===================================================
        // 2. CENTER CONTENT
        // ===================================================
        KahootBackgroundPanel centerContent = new KahootBackgroundPanel(new BorderLayout());
        centerContent.setOpaque(false);

        navBar = new NavigationBar();
        navBar.addTab("Người Chơi");
        navBar.addTab("Bảng Xếp Hạng");
        navBar.addTab("Lịch Sử");
        navBar.addTab("Quản Lý Người Chơi");
        navBar.addTab("Ngân Hàng Câu Hỏi");
        navBar.setTabVisible("Quản Lý Người Chơi", false);
        navBar.setTabVisible("Ngân Hàng Câu Hỏi", false);
        navBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER_LIGHT),
                new EmptyBorder(10, 24, 0, 24)));
        centerContent.add(navBar, BorderLayout.NORTH);

        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.setOpaque(false);

        // --- Tab 1: Players Panel (Responsive BorderLayout on resize/maximize) ---
        JPanel playersWrapper = new JPanel(new BorderLayout(0, 14));
        playersWrapper.setOpaque(false);
        playersWrapper.setBorder(new EmptyBorder(16, 24, 16, 24));

        JPanel topControls = new JPanel(new BorderLayout(0, 12));
        topControls.setOpaque(false);

        // ---- QUICK MATCH CARD ----
        ModernCard quickMatchCard = new ModernCard(new BorderLayout(16, 0));
        quickMatchCard.setCardPadding(14, 22, 14, 22);
        quickMatchCard.setCardBackground(new Color(0xF0, 0xEB, 0xFF));
        quickMatchCard.setBorderColor(Theme.PURPLE);

        JPanel qmLeft = new JPanel();
        qmLeft.setLayout(new BoxLayout(qmLeft, BoxLayout.Y_AXIS));
        qmLeft.setOpaque(false);

        JLabel qmTitle = new JLabel("QUICK MATCH — GHÉP TRẬN TỰ ĐỘNG");
        qmTitle.setFont(Theme.SUBTITLE);
        qmTitle.setForeground(Theme.PURPLE);
        JLabel qmDesc = new JLabel("Tìm ngay đối thủ trực tuyến có thứ hạng và điểm số tương đương bạn");
        qmDesc.setFont(Theme.SMALL);
        qmDesc.setForeground(Theme.MUTED_GRAY);
        qmLeft.add(qmTitle);
        qmLeft.add(Box.createVerticalStrut(2));
        qmLeft.add(qmDesc);
        quickMatchCard.add(qmLeft, BorderLayout.WEST);

        // Quick match status + timer (hidden by default)
        quickMatchStatusPanel = new JPanel();
        quickMatchStatusPanel.setLayout(new BoxLayout(quickMatchStatusPanel, BoxLayout.Y_AXIS));
        quickMatchStatusPanel.setOpaque(false);
        lblQuickMatchStatus = new JLabel("Đang tìm đối thủ...");
        lblQuickMatchStatus.setFont(Theme.BODY_BOLD);
        lblQuickMatchStatus.setForeground(Theme.BLUE);
        lblQuickMatchTimer = new JLabel("0 giây");
        lblQuickMatchTimer.setFont(Theme.SMALL);
        lblQuickMatchTimer.setForeground(Theme.MUTED_GRAY);
        quickMatchStatusPanel.add(lblQuickMatchStatus);
        quickMatchStatusPanel.add(lblQuickMatchTimer);
        quickMatchStatusPanel.setVisible(false);
        quickMatchCard.add(quickMatchStatusPanel, BorderLayout.CENTER);

        JPanel qmRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 2));
        qmRight.setOpaque(false);

        btnQuickMatch = new ModernButton("GHÉP TRẬN", ModernButton.Style.PRIMARY);
        btnQuickMatch.setPreferredSize(new Dimension(148, 40));
        btnQuickMatch.addActionListener(e -> {
            SoundManager.playClick();
            if (quickMatchListener != null)
                quickMatchListener.run();
        });

        btnCancelMatchmaking = new ModernButton("HỦY TÌM", ModernButton.Style.SECONDARY);
        btnCancelMatchmaking.setPreferredSize(new Dimension(120, 40));
        btnCancelMatchmaking.setVisible(false);
        btnCancelMatchmaking.addActionListener(e -> {
            SoundManager.playClick();
            if (cancelMatchmakingListener != null)
                cancelMatchmakingListener.run();
        });

        qmRight.add(btnQuickMatch);
        qmRight.add(btnCancelMatchmaking);
        quickMatchCard.add(qmRight, BorderLayout.EAST);

        topControls.add(quickMatchCard, BorderLayout.NORTH);

        // ---- Topic & Level Selection Card ----
        ModernCard topicSectionCard = new ModernCard(new BorderLayout(0, 10));
        topicSectionCard.setCardPadding(14, 20, 14, 20);
        topicSectionCard.setCardBackground(Theme.WHITE);
        topicSectionCard.setBorderColor(Theme.BORDER_LIGHT);

        // Visual Topic Cards selector
        topicSelector = new TopicSelector(quiz.Bank.STANDARD_TOPICS);
        topicSectionCard.add(topicSelector, BorderLayout.CENTER);

        // Action row: Level selection & Solo Practice
        JPanel bottomActionRow = new JPanel(new BorderLayout(16, 0));
        bottomActionRow.setOpaque(false);
        bottomActionRow.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER_LIGHT),
                new EmptyBorder(10, 4, 0, 4)));

        JPanel levelBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        levelBox.setOpaque(false);
        JLabel lblLevel = new JLabel("Mức độ:");
        lblLevel.setFont(Theme.BODY_BOLD);
        lblLevel.setForeground(Theme.DARK_TEXT);
        levelBox.add(lblLevel);
        levelSelector = new LevelSelector();
        levelBox.add(levelSelector);
        bottomActionRow.add(levelBox, BorderLayout.WEST);

        JPanel rightAction = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        rightAction.setOpaque(false);
        JLabel lblNotice = new JLabel("10 câu • Đếm giờ • Speed Bonus");
        lblNotice.setFont(Theme.SMALL);
        lblNotice.setForeground(Theme.MUTED_GRAY);
        rightAction.add(lblNotice);
        ModernButton btnPractice = new ModernButton("Luyện Tập Đơn", ModernButton.Style.PRIMARY);
        btnPractice.setPreferredSize(new Dimension(150, 36));
        btnPractice.addActionListener(e -> {
            SoundManager.playClick();
            if (soloPracticeListener != null)
                soloPracticeListener.run();
        });
        rightAction.add(btnPractice);
        bottomActionRow.add(rightAction, BorderLayout.EAST);

        topicSectionCard.add(bottomActionRow, BorderLayout.SOUTH);
        topControls.add(topicSectionCard, BorderLayout.CENTER);

        playersWrapper.add(topControls, BorderLayout.NORTH);

        // ---- Online Players Section (Expands dynamically on resize/maximize) ----
        JPanel onlineSection = new JPanel(new BorderLayout(0, 8));
        onlineSection.setOpaque(false);

        JPanel sectionHeader = new JPanel(new BorderLayout());
        sectionHeader.setOpaque(false);
        JLabel lblSection = new JLabel("DANH SÁCH NGƯỜI CHƠI TRỰC TUYẾN");
        lblSection.setFont(Theme.SUBTITLE);
        lblSection.setForeground(Theme.DARK_TEXT);
        sectionHeader.add(lblSection, BorderLayout.WEST);
        lblOnlineCount = new JLabel("0 người chơi");
        lblOnlineCount.setFont(Theme.SMALL_BOLD);
        lblOnlineCount.setForeground(Theme.MUTED_GRAY);
        sectionHeader.add(lblOnlineCount, BorderLayout.EAST);
        onlineSection.add(sectionHeader, BorderLayout.NORTH);

        playersListPanel = new JPanel();
        playersListPanel.setLayout(new BoxLayout(playersListPanel, BoxLayout.Y_AXIS));
        playersListPanel.setOpaque(false);

        emptyStateCard = new ModernCard(new BorderLayout(0, 10));
        emptyStateCard.setCardPadding(36, 20, 36, 20);
        emptyStateCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 180));
        JLabel lblEmptyTitle = new JLabel("Chưa có người chơi nào khác đang online", SwingConstants.CENTER);
        lblEmptyTitle.setFont(Theme.BODY_BOLD);
        lblEmptyTitle.setForeground(Theme.DARK_TEXT);
        JLabel lblEmptySub = new JLabel("Hãy mở thêm cửa sổ Client hoặc bấm Ghép Trận / Luyện Tập Đơn!", SwingConstants.CENTER);
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
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        onlineSection.add(scrollPane, BorderLayout.CENTER);

        playersWrapper.add(onlineSection, BorderLayout.CENTER);

        leaderboardPanel = new LeaderboardPanel();
        historyPanel = new HistoryPanel();
        playerManagementPanel = new PlayerManagementPanel();
        questionManagementPanel = new QuestionManagementPanel();

        cardPanel.add(playersWrapper, "Người Chơi");
        cardPanel.add(leaderboardPanel, "Bảng Xếp Hạng");
        cardPanel.add(historyPanel, "Lịch Sử");
        cardPanel.add(playerManagementPanel, "Quản Lý Người Chơi");
        cardPanel.add(questionManagementPanel, "Ngân Hàng Câu Hỏi");

        centerContent.add(cardPanel, BorderLayout.CENTER);
        add(centerContent, BorderLayout.CENTER);

        navBar.setOnTabChanged(tabName -> {
            if (("Quản Lý Người Chơi".equals(tabName) || "Ngân Hàng Câu Hỏi".equals(tabName)) && !isAdmin()) {
                return;
            }
            cardLayout.show(cardPanel, tabName);
            if (tabChangedListener != null)
                tabChangedListener.accept(tabName);
        });
    }

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(currentRole) || "admin".equalsIgnoreCase(currentUsername);
    }

    public void updatePing(long ms) {
        if (badgePing != null) {
            badgePing.setText("Ping: " + ms + " ms");
        }
    }

    public void openCurrentUserProfileDialog() {
        PlayerManagementPanel.PlayerRecord rec = playerManagementPanel.findRecord(currentUsername);
        if (rec == null) {
            rec = new PlayerManagementPanel.PlayerRecord(
                    currentUsername,
                    currentScore,
                    currentAvatarId,
                    Rank.fromScore(currentScore).getDisplayName(),
                    "Trực tuyến",
                    currentFullName,
                    currentEmail,
                    currentBio,
                    currentRole);
        }
        playerManagementPanel.openEditDialog(rec);
    }

    public void setCurrentUser(String username) {
        setCurrentUser(username, 0, "avatar_01", "", "", "", "PLAYER");
    }

    public void setCurrentUser(String username, int score, String avatarId) {
        setCurrentUser(username, score, avatarId, this.currentFullName, this.currentEmail, this.currentBio, this.currentRole);
    }

    public void setCurrentUser(String username, int score, String avatarId, String role) {
        setCurrentUser(username, score, avatarId, this.currentFullName, this.currentEmail, this.currentBio, role);
    }

    public void setCurrentUser(String username, int score, String avatarId, String fullName, String email, String bio) {
        setCurrentUser(username, score, avatarId, fullName, email, bio, this.currentRole);
    }

    // --- Setter for current user (with score + avatarId + profile info + role) ---
    public void setCurrentUser(String username, int score, String avatarId, String fullName, String email, String bio, String role) {
        this.currentUsername = username != null ? username : "";
        this.currentScore = score;
        this.currentAvatarId = avatarId != null && !avatarId.isBlank() ? avatarId : "avatar_01";
        this.currentFullName = fullName != null ? fullName : "";
        this.currentEmail = email != null ? email : "";
        this.currentBio = bio != null ? bio : "";
        this.currentRole = (role != null && !role.isBlank()) ? role : ("admin".equalsIgnoreCase(this.currentUsername) ? "ADMIN" : "PLAYER");
        boolean admin = isAdmin();

        String displayLabel = (!this.currentFullName.isBlank())
                ? (this.currentUsername + " (" + this.currentFullName + ")")
                : this.currentUsername;
        lblUsername.setText(displayLabel);
        Rank rank = Rank.fromScore(score);
        lblRankBadge.setText(rank.getLabel());
        badgeScore.setText(score + " điểm");
        badgeRole.setVisible(admin);
        navBar.setTabVisible("Quản Lý Người Chơi", admin);
        navBar.setTabVisible("Ngân Hàng Câu Hỏi", admin);
        playerManagementPanel.setAdminMode(admin);
        if (!admin && ("Quản Lý Người Chơi".equals(navBar.getActiveTab()) || "Ngân Hàng Câu Hỏi".equals(navBar.getActiveTab()))) {
            cardLayout.show(cardPanel, "Người Chơi");
        }
        avatarCircle.repaint();
    }

    public void setMyAvatar(String avatarId) {
        this.currentAvatarId = avatarId != null ? avatarId : "avatar_01";
        avatarCircle.repaint();
    }

    public void setCurrentScore(int score) {
        Rank oldRank = Rank.fromScore(this.currentScore);
        this.currentScore = score;
        Rank rank = Rank.fromScore(score);
        lblRankBadge.setText(rank.getLabel());
        badgeScore.setText(score + " điểm");
        if (oldRank != null && rank.ordinal() > oldRank.ordinal()) {
            SoundManager.playRankUp();
        }
    }

    // --- Quick Match UI state transitions ---
    public void onMatchmakingStarted(String msg) {
        isMatchmaking = true;
        btnQuickMatch.setVisible(false);
        btnCancelMatchmaking.setVisible(true);
        quickMatchStatusPanel.setVisible(true);
        lblQuickMatchStatus.setText(msg);
        quickMatchSeconds = 0;
        if (quickMatchTimer != null && quickMatchTimer.isRunning())
            quickMatchTimer.stop();
        quickMatchTimer = new Timer(1000, e -> {
            quickMatchSeconds++;
            lblQuickMatchTimer.setText(quickMatchSeconds + " giây");
        });
        quickMatchTimer.start();
    }

    public void onMatchFound(String opponentName, int myScore, int opponentScore) {
        if (quickMatchTimer != null)
            quickMatchTimer.stop();
        lblQuickMatchStatus.setText("Đã tìm thấy đối thủ: " + opponentName + " (" + opponentScore + " điểm)");
        lblQuickMatchTimer.setText("Đang chuẩn bị trận đấu...");
        btnCancelMatchmaking.setVisible(false);
    }

    public void onMatchmakingCancelled(String msg) {
        isMatchmaking = false;
        if (quickMatchTimer != null)
            quickMatchTimer.stop();
        btnQuickMatch.setVisible(true);
        btnCancelMatchmaking.setVisible(false);
        quickMatchStatusPanel.setVisible(false);
        quickMatchSeconds = 0;
    }

    // --- Topic / Level ---
    public void setTopics(List<String> topics) {
        topicSelector.setTopics(topics);
    }

    public String getSelectedTopic() {
        return topicSelector.getSelectedTopic();
    }

    public void setSelectedTopic(String topic) {
        topicSelector.setSelectedTopic(topic);
    }

    public String getSelectedLevel() {
        return levelSelector.getSelectedLevel();
    }

    // --- Listeners ---
    public void setOnChallenge(ChallengeListener listener) {
        this.challengeListener = listener;
    }

    public void setOnLogout(Runnable listener) {
        this.logoutListener = listener;
    }

    public void setOnTabChanged(Consumer<String> listener) {
        this.tabChangedListener = listener;
    }

    public void setOnSoloPractice(Runnable listener) {
        this.soloPracticeListener = listener;
    }

    public void setOnQuickMatch(Runnable listener) {
        this.quickMatchListener = listener;
    }

    public void setOnCancelMatchmaking(Runnable listener) {
        this.cancelMatchmakingListener = listener;
    }

    public void setOnUpdateAvatar(Consumer<String> listener) {
        this.updateAvatarListener = listener;
    }

    public LeaderboardPanel getLeaderboardPanel() {
        return leaderboardPanel;
    }

    public HistoryPanel getHistoryPanel() {
        return historyPanel;
    }

    public PlayerManagementPanel getPlayerManagementPanel() {
        return playerManagementPanel;
    }

    public QuestionManagementPanel getQuestionManagementPanel() {
        return questionManagementPanel;
    }

    public void updatePlayersFromLobbyData(String lobbyData) {
        updatePlayersFromLobbyData(lobbyData, currentUsername);
    }

    /**
     * Cập nhật danh sách người chơi từ LOBBY packet.
     * Format mới: name;score;status;avatarId;rankLabel|...
     */
    public void updatePlayersFromLobbyData(String lobbyData, String myName) {
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
            String avatarId = fields.length > 3 ? fields[3] : "avatar_01";
            String rankLabel = fields.length > 4 ? fields[4] : Rank.fromScore(score).getDisplayName();
            boolean isReady = status.equalsIgnoreCase("Đang rỗi");

            if (name.equals(myName)) {
                setCurrentScore(score);
                setMyAvatar(avatarId);
            } else {
                boolean isInQueue = status.equalsIgnoreCase("Đang tìm");
                PlayerCard card = new PlayerCard(name, score, rankLabel, avatarId, isReady, isInQueue);
                card.setOnChallenge(targetUser -> {
                    if (challengeListener != null) {
                        challengeListener.onChallenge(targetUser, getSelectedTopic(), getSelectedLevel());
                    }
                });
                otherPlayers.add(card);
            }
        }

        // Sort: isReady first, then score descending
        otherPlayers.sort((a, b) -> {
            if (a.isReady() && !b.isReady())
                return -1;
            if (!a.isReady() && b.isReady())
                return 1;
            return Integer.compare(b.getScore(), a.getScore());
        });

        if (otherPlayers.isEmpty()) {
            lblOnlineCount.setText("0 người chơi khác");
            playersListPanel.add(emptyStateCard);
        } else {
            lblOnlineCount.setText(otherPlayers.size() + " người chơi khác trực tuyến");
            for (PlayerCard card : otherPlayers) {
                playersListPanel.add(card);
                playersListPanel.add(Box.createVerticalStrut(8));
            }
        }

        playersListPanel.revalidate();
        playersListPanel.repaint();
    }

    private String getSoundButtonLabel() {
        boolean allMuted = SoundManager.isMuted() && MusicManager.isMusicMuted();
        return allMuted ? "Âm Thanh: Tắt" : "Âm Thanh";
    }
}
