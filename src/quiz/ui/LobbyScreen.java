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
    private JPanel sidebarPanel;

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
        super(new BorderLayout());
        setBackground(ArenaPalette.BG);

        // The navigation and account tools remain anchored while the arena scrolls.
        JPanel sidebar = new JPanel(new BorderLayout(0, 10));
        sidebarPanel = sidebar;
        sidebar.setBackground(ArenaPalette.SIDEBAR);
        sidebar.setPreferredSize(new Dimension(232, 0));
        sidebar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, ArenaPalette.BORDER),
                new EmptyBorder(24, 16, 18, 16)));

        JPanel brand = new JPanel(new BorderLayout(11, 0));
        brand.setOpaque(false);
        brand.add(new JLabel(new ArenaIcon(ArenaIcon.Type.BRAND, 34, ArenaPalette.ACCENT)), BorderLayout.WEST);
        JPanel brandCopy = new JPanel(new GridLayout(2, 1, 0, 2));
        brandCopy.setOpaque(false);
        brandCopy.add(arenaLabel("QUIZ ARENA", 17, Font.BOLD, ArenaPalette.TEXT));
        brandCopy.add(arenaLabel("PLAY. LEARN. CONQUER.", 9, Font.BOLD, ArenaPalette.MUTED));
        brand.add(brandCopy, BorderLayout.CENTER);

        navBar = new NavigationBar();
        navBar.setSidebarMode(true);
        navBar.addTab("Người Chơi");
        navBar.addTab("Bảng Xếp Hạng");
        navBar.addTab("Lịch Sử");
        navBar.addTab("Quản Lý Người Chơi");
        navBar.addTab("Ngân Hàng Câu Hỏi");
        navBar.setTabVisible("Quản Lý Người Chơi", false);
        navBar.setTabVisible("Ngân Hàng Câu Hỏi", false);
        navBar.setBorder(BorderFactory.createEmptyBorder());
        JPanel navigation = new JPanel(new BorderLayout(0, 10));
        navigation.setOpaque(false);
        JLabel navigationCaption = arenaLabel("KHÁM PHÁ", 10, Font.BOLD, ArenaPalette.MUTED);
        navigationCaption.setBorder(new EmptyBorder(0, 12, 0, 0));
        navigation.add(navigationCaption, BorderLayout.NORTH);
        JScrollPane navigationScroll = new JScrollPane(navBar);
        navigationScroll.setBorder(null);
        navigationScroll.setOpaque(false);
        navigationScroll.getViewport().setOpaque(false);
        navigationScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        navigationScroll.getVerticalScrollBar().setUnitIncrement(28);
        ArenaPalette.styleScrollBar(navigationScroll.getVerticalScrollBar());
        navigation.add(navigationScroll, BorderLayout.CENTER);
        JPanel sidebarTop = new JPanel(new BorderLayout(0, 20));
        sidebarTop.setOpaque(false);
        sidebarTop.add(brand, BorderLayout.NORTH);
        sidebarTop.add(navigation, BorderLayout.CENTER);
        sidebar.add(sidebarTop, BorderLayout.CENTER);

        ModernButton sound = accountButton(ArenaIcon.Type.SOUND, "Cài đặt âm thanh");
        sound.setToolTipText("Cài đặt âm thanh & nhạc nền");
        sound.addActionListener(e -> {
            SoundManager.playClick();
            new SoundSettingsDialog(SwingUtilities.getWindowAncestor(this)).setVisible(true);
            sound.setToolTipText(getSoundButtonLabel() + " — cài đặt âm thanh & nhạc nền");
        });
        ModernButton profile = accountButton(ArenaIcon.Type.PROFILE, "Hồ sơ cá nhân");
        profile.setToolTipText("Sửa thông tin cá nhân, mật khẩu hoặc xóa tài khoản");
        profile.addActionListener(e -> {
            SoundManager.playClick();
            openCurrentUserProfileDialog();
        });
        ModernButton logout = accountButton(ArenaIcon.Type.LOGOUT, "Đăng xuất");
        logout.addActionListener(e -> {
            SoundManager.playClick();
            if (logoutListener != null) logoutListener.run();
        });

        avatarCircle = new JPanel() {
            @Override protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                Graphics2D g = (Graphics2D) graphics.create();
                Theme.setupAntiAliasing(g);
                int size = Math.min(getWidth(), getHeight()) - 4;
                int x = (getWidth() - size) / 2;
                int y = (getHeight() - size) / 2;
                AvatarPicker.paintAvatar(g, currentAvatarId, x, y, size);
                g.setColor(ArenaPalette.ACCENT);
                g.setStroke(new BasicStroke(1.4f));
                g.drawOval(x, y, size, size);
                g.dispose();
            }
        };
        avatarCircle.setPreferredSize(new Dimension(42, 42));
        avatarCircle.setOpaque(false);
        avatarCircle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        avatarCircle.setToolTipText("Nhấn để đổi avatar");
        avatarCircle.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent event) {
                Window window = SwingUtilities.getWindowAncestor(LobbyScreen.this);
                Frame owner = window instanceof Frame ? (Frame) window : null;
                AvatarPicker picker = new AvatarPicker(owner, currentAvatarId, newId -> {
                    if (updateAvatarListener != null) updateAvatarListener.accept(newId);
                });
                picker.setVisible(true);
            }
        });
        lblAvatarEmoji = new JLabel("");
        lblAvatarEmoji.setPreferredSize(new Dimension(0, 0));
        lblUsername = arenaLabel("Người chơi", 14, Font.BOLD, ArenaPalette.TEXT);
        lblUsername.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        lblRankBadge = arenaLabel("Sắt", 12, Font.PLAIN, ArenaPalette.ACCENT);
        lblRankBadge.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        JPanel identity = new JPanel(new GridLayout(2, 1, 0, 1));
        identity.setOpaque(false);
        identity.add(lblUsername);
        identity.add(lblRankBadge);
        identity.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        identity.setToolTipText("Nhấn để sửa thông tin cá nhân");
        identity.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent event) {
                openCurrentUserProfileDialog();
            }
        });
        JPanel profileHeading = new JPanel(new BorderLayout(10, 0));
        profileHeading.setOpaque(false);
        profileHeading.add(avatarCircle, BorderLayout.WEST);
        profileHeading.add(identity, BorderLayout.CENTER);

        badgeScore = new ModernBadge("0 điểm", ModernBadge.Type.RANK);
        badgeRole = new ModernBadge("ADMIN", ModernBadge.Type.WARNING);
        badgeScore.setArenaStyle(true);
        badgeRole.setArenaStyle(true);
        badgeRole.setVisible(false);
        JPanel accountStats = new JPanel();
        accountStats.setLayout(new BoxLayout(accountStats, BoxLayout.Y_AXIS));
        accountStats.setOpaque(false);
        accountStats.add(badgeScore);
        accountStats.add(Box.createVerticalStrut(3));
        accountStats.add(badgeRole);
        JPanel accountActions = new JPanel(new GridLayout(1, 3, 8, 0));
        accountActions.setOpaque(false);
        accountActions.add(sound);
        accountActions.add(profile);
        accountActions.add(logout);
        JPanel accountDetails = new JPanel(new BorderLayout(0, 12));
        accountDetails.setOpaque(false);
        accountDetails.add(accountStats, BorderLayout.NORTH);
        accountDetails.add(accountActions, BorderLayout.SOUTH);
        ModernCard account = new ModernCard(new BorderLayout(0, 12));
        account.setCardBackground(ArenaPalette.SURFACE);
        account.setBorderColor(ArenaPalette.BORDER);
        account.setCornerRadius(22);
        account.setCardPadding(14, 12, 12, 12);
        account.add(profileHeading, BorderLayout.NORTH);
        account.add(accountDetails, BorderLayout.CENTER);
        sidebar.add(account, BorderLayout.SOUTH);
        add(sidebar, BorderLayout.WEST);

        JPanel centerContent = new JPanel(new BorderLayout());
        centerContent.setOpaque(false);
        JPanel pageHeader = new JPanel(new BorderLayout(14, 0));
        pageHeader.setOpaque(false);
        pageHeader.setBorder(new EmptyBorder(24, 24, 18, 24));
        JLabel pageTitle = arenaLabel("Đấu trường tri thức", 25, Font.BOLD, ArenaPalette.TEXT);
        pageTitle.setIcon(new ArenaIcon(ArenaIcon.Type.BOLT, 19, ArenaPalette.ACCENT));
        pageTitle.setIconTextGap(10);
        pageHeader.add(pageTitle, BorderLayout.CENTER);
        badgePing = new ModernBadge("Ping: -- ms", ModernBadge.Type.READY);
        badgePing.setArenaStyle(true);
        JPanel connection = new JPanel(new GridBagLayout());
        connection.setOpaque(false);
        connection.add(badgePing);
        pageHeader.add(connection, BorderLayout.EAST);
        centerContent.add(pageHeader, BorderLayout.NORTH);

        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.setOpaque(false);
        JPanel playersWrapper = new PlayersContentPanel();
        playersWrapper.setOpaque(false);
        playersWrapper.setBorder(new EmptyBorder(0, 24, 24, 24));
        JPanel topControls = new JPanel(new BorderLayout(0, 22));
        topControls.setOpaque(false);

        quickMatchStatusPanel = new JPanel();
        quickMatchStatusPanel.setLayout(new BoxLayout(quickMatchStatusPanel, BoxLayout.Y_AXIS));
        quickMatchStatusPanel.setOpaque(false);
        lblQuickMatchStatus = arenaLabel("Đang tìm đối thủ...", 12, Font.BOLD, ArenaPalette.TEXT);
        lblQuickMatchTimer = arenaLabel("0 giây", 11, Font.PLAIN, ArenaPalette.CYAN);
        quickMatchStatusPanel.add(lblQuickMatchStatus);
        quickMatchStatusPanel.add(lblQuickMatchTimer);
        quickMatchStatusPanel.setVisible(false);
        JPanel matchmakingActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        matchmakingActions.setOpaque(false);
        btnQuickMatch = new ModernButton("TÌM ĐỐI THỦ", ModernButton.Style.PRIMARY);
        btnQuickMatch.setArenaStyle(true);
        btnQuickMatch.setIcon(new ArenaIcon(ArenaIcon.Type.BOLT, 18, null));
        btnQuickMatch.setPreferredSize(new Dimension(188, 48));
        btnQuickMatch.addActionListener(e -> {
            SoundManager.playClick();
            if (quickMatchListener != null) quickMatchListener.run();
        });
        btnCancelMatchmaking = new ModernButton("Hủy tìm trận", ModernButton.Style.SECONDARY);
        btnCancelMatchmaking.setArenaStyle(true);
        btnCancelMatchmaking.setPreferredSize(new Dimension(158, 48));
        btnCancelMatchmaking.setVisible(false);
        btnCancelMatchmaking.addActionListener(e -> {
            SoundManager.playClick();
            if (cancelMatchmakingListener != null) cancelMatchmakingListener.run();
        });
        matchmakingActions.add(btnQuickMatch);
        matchmakingActions.add(btnCancelMatchmaking);
        ArenaHeroPanel arenaHero = new ArenaHeroPanel(matchmakingActions, quickMatchStatusPanel);
        topControls.add(arenaHero, BorderLayout.NORTH);

        ModernCard topicSectionCard = new ModernCard(new BorderLayout(0, 16));
        topicSectionCard.setCardPadding(20, 20, 18, 20);
        topicSectionCard.setCornerRadius(24);
        topicSectionCard.setCardBackground(ArenaPalette.SURFACE);
        topicSectionCard.setBorderColor(ArenaPalette.BORDER);
        topicSectionCard.setHasShadow(false);
        topicSelector = new TopicSelector(quiz.Bank.STANDARD_TOPICS);
        topicSectionCard.add(topicSelector, BorderLayout.CENTER);

        JPanel bottomActionRow = new JPanel(new PracticeRowLayout());
        bottomActionRow.setOpaque(false);
        bottomActionRow.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, ArenaPalette.BORDER),
                new EmptyBorder(16, 0, 0, 0)));
        levelSelector = new LevelSelector();
        bottomActionRow.add(levelSelector, BorderLayout.WEST);
        ModernButton practice = new ModernButton("Luyện tập đơn", ModernButton.Style.SECONDARY);
        practice.setArenaStyle(true);
        practice.setIcon(new ArenaIcon(ArenaIcon.Type.PLAY, 15, null));
        practice.setPreferredSize(new Dimension(158, 40));
        practice.addActionListener(e -> {
            SoundManager.playClick();
            if (soloPracticeListener != null) soloPracticeListener.run();
        });
        bottomActionRow.add(practice, BorderLayout.EAST);
        topicSectionCard.add(bottomActionRow, BorderLayout.SOUTH);
        topControls.add(topicSectionCard, BorderLayout.CENTER);
        playersWrapper.add(topControls, BorderLayout.NORTH);

        JPanel onlineSection = new JPanel(new BorderLayout(0, 14));
        onlineSection.setOpaque(false);
        JPanel onlineHeading = new JPanel(new BorderLayout(10, 0));
        onlineHeading.setOpaque(false);
        JLabel onlineTitle = arenaLabel("Đối thủ trực tuyến", 19, Font.BOLD, ArenaPalette.TEXT);
        onlineTitle.setIcon(new ArenaIcon(ArenaIcon.Type.PLAYERS, 22, ArenaPalette.CYAN));
        onlineTitle.setIconTextGap(10);
        onlineHeading.add(onlineTitle, BorderLayout.WEST);
        lblOnlineCount = arenaLabel("0 người chơi khác", 12, Font.PLAIN, ArenaPalette.MUTED);
        onlineHeading.add(lblOnlineCount, BorderLayout.EAST);
        onlineSection.add(onlineHeading, BorderLayout.NORTH);
        playersListPanel = new JPanel();
        playersListPanel.setLayout(new BoxLayout(playersListPanel, BoxLayout.Y_AXIS));
        playersListPanel.setOpaque(false);
        emptyStateCard = new ModernCard(new BorderLayout(20, 0));
        emptyStateCard.setCardPadding(24, 24, 24, 24);
        emptyStateCard.setCardBackground(ArenaPalette.SURFACE);
        emptyStateCard.setBorderColor(ArenaPalette.BORDER);
        emptyStateCard.setCornerRadius(22);
        emptyStateCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 132));
        emptyStateCard.setPreferredSize(new Dimension(600, 132));
        emptyStateCard.add(new JLabel(new ArenaIcon(ArenaIcon.Type.PLAYERS, 42, ArenaPalette.CYAN)), BorderLayout.WEST);
        JPanel emptyCopy = new JPanel(new GridLayout(2, 1, 0, 7));
        emptyCopy.setOpaque(false);
        emptyCopy.add(arenaLabel("Đấu trường đang chờ người chơi", 15, Font.BOLD, ArenaPalette.TEXT));
        JLabel hint = arenaLabel("Thử luyện tập đơn trong lúc chờ đối thủ trực tuyến.", 12, Font.PLAIN, ArenaPalette.MUTED);
        hint.setToolTipText(hint.getText());
        emptyCopy.add(hint);
        emptyStateCard.add(emptyCopy, BorderLayout.CENTER);
        playersListPanel.add(emptyStateCard);
        onlineSection.add(playersListPanel, BorderLayout.CENTER);
        playersWrapper.add(onlineSection, BorderLayout.CENTER);

        leaderboardPanel = new LeaderboardPanel();
        historyPanel = new HistoryPanel();
        playerManagementPanel = new PlayerManagementPanel();
        questionManagementPanel = new QuestionManagementPanel();
        JScrollPane playersScroll = new JScrollPane(playersWrapper);
        playersScroll.setBorder(null);
        playersScroll.setOpaque(false);
        playersScroll.getViewport().setOpaque(false);
        playersScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        playersScroll.getVerticalScrollBar().setUnitIncrement(28);
        playersScroll.getVerticalScrollBar().setPreferredSize(new Dimension(9, 0));
        playersScroll.getVerticalScrollBar().setUI(new QuietScrollBarUI());
        cardPanel.add(playersScroll, "Người Chơi");
        cardPanel.add(leaderboardPanel, "Bảng Xếp Hạng");
        cardPanel.add(historyPanel, "Lịch Sử");
        cardPanel.add(playerManagementPanel, "Quản Lý Người Chơi");
        cardPanel.add(questionManagementPanel, "Ngân Hàng Câu Hỏi");
        centerContent.add(cardPanel, BorderLayout.CENTER);
        add(new MaximumWidthPanel(centerContent, 1340), BorderLayout.CENTER);
        navBar.setOnTabChanged(tab -> {
            if (("Quản Lý Người Chơi".equals(tab) || "Ngân Hàng Câu Hỏi".equals(tab)) && !isAdmin()) return;
            cardLayout.show(cardPanel, tab);
            pageTitle.setText(switch (tab) {
                case "Người Chơi" -> "Đấu trường tri thức";
                case "Bảng Xếp Hạng" -> "Vinh danh cao thủ";
                case "Lịch Sử" -> "Thành tích của bạn";
                case "Quản Lý Người Chơi" -> "Quản lý tài khoản";
                default -> "Kho tri thức";
            });
            if (tabChangedListener != null) tabChangedListener.accept(tab);
        });
    }

    private static JLabel arenaLabel(String text, int size, int style, Color color) {
        JLabel label = new JLabel(text);
        label.putClientProperty("html.disable", Boolean.TRUE);
        label.setFont(ArenaPalette.font(size, style));
        label.setForeground(color);
        return label;
    }

    private static ModernButton accountButton(ArenaIcon.Type icon, String name) {
        ModernButton button = new ModernButton("", ModernButton.Style.SECONDARY);
        button.setArenaStyle(true);
        button.setPreferredSize(new Dimension(40, 38));
        button.setIcon(new ArenaIcon(icon, 18, null));
        button.setToolTipText(name);
        button.getAccessibleContext().setAccessibleName(name);
        return button;
    }

    /** Caps line lengths on a maximized desktop while remaining fluid on a laptop. */
    private static final class MaximumWidthPanel extends JPanel {
        private final JComponent content;
        private final int maximum;
        MaximumWidthPanel(JComponent content, int maximum) {
            super(null);
            this.content = content;
            this.maximum = maximum;
            setOpaque(false);
            add(content);
        }
        @Override public void doLayout() {
            int width = Math.min(maximum, getWidth());
            content.setBounds((getWidth() - width) / 2, 0, width, getHeight());
        }
        @Override public Dimension getPreferredSize() {
            Dimension preferred = content.getPreferredSize();
            return new Dimension(Math.min(maximum, preferred.width), preferred.height);
        }
    }

    @Override public void doLayout() {
        int width = getWidth() < 1000 ? 200 : 232;
        if (sidebarPanel != null && sidebarPanel.getPreferredSize().width != width)
            sidebarPanel.setPreferredSize(new Dimension(width, 0));
        super.doLayout();
    }

    /** Lets the existing difficulty and practice actions remain reachable at 760px. */
    private static final class PracticeRowLayout implements LayoutManager {
        @Override public void addLayoutComponent(String name, Component child) {}
        @Override public void removeLayoutComponent(Component child) {}
        @Override public Dimension preferredLayoutSize(Container parent) {
            int width = parent.getWidth();
            boolean narrow = width > 0 && width < 530;
            Insets insets = parent.getInsets();
            return new Dimension(480, (narrow ? 84 : 40) + insets.top + insets.bottom);
        }
        @Override public Dimension minimumLayoutSize(Container parent) { return preferredLayoutSize(parent); }
        @Override public void layoutContainer(Container parent) {
            if (parent.getComponentCount() < 2) return;
            Insets insets = parent.getInsets();
            int width = parent.getWidth() - insets.left - insets.right;
            Component levels = parent.getComponent(0);
            Component practice = parent.getComponent(1);
            if (width < 530) {
                levels.setBounds(insets.left, insets.top, width, 38);
                practice.setBounds(Math.max(insets.left, parent.getWidth() - insets.right - 158),
                        insets.top + 46, Math.min(158, width), 38);
            } else {
                levels.setBounds(insets.left, insets.top, levels.getPreferredSize().width, 40);
                practice.setBounds(parent.getWidth() - insets.right - 158, insets.top, 158, 40);
            }
        }
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
        lblUsername.setToolTipText(displayLabel);
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

    /** A quiet page scrollbar keeps the illustrated content visually prominent. */
    private static final class QuietScrollBarUI extends javax.swing.plaf.basic.BasicScrollBarUI {
        @Override protected JButton createDecreaseButton(int orientation) { return emptyButton(); }
        @Override protected JButton createIncreaseButton(int orientation) { return emptyButton(); }
        private JButton emptyButton() {
            JButton button = new JButton();
            button.setPreferredSize(new Dimension(0, 0));
            button.setMinimumSize(new Dimension(0, 0));
            button.setMaximumSize(new Dimension(0, 0));
            return button;
        }
        @Override protected void paintTrack(Graphics g, JComponent component, Rectangle bounds) {
            g.setColor(ArenaPalette.BG);
            g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
        }
        @Override protected void paintThumb(Graphics graphics, JComponent component, Rectangle bounds) {
            if (bounds.isEmpty() || !scrollbar.isEnabled()) return;
            Graphics2D g = (Graphics2D) graphics.create();
            Theme.setupAntiAliasing(g);
            g.setColor(isThumbRollover() ? new Color(102, 125, 161) : ArenaPalette.BORDER);
            g.fillRoundRect(bounds.x + 2, bounds.y + 2, Math.max(4, bounds.width - 4),
                    Math.max(8, bounds.height - 4), 8, 8);
            g.dispose();
        }
    }

    /** One page scrollbar keeps the hero, topics and player list reachable on small windows. */
    private static final class PlayersContentPanel extends JPanel implements Scrollable {
        PlayersContentPanel() {
            super(new BorderLayout(0, 14));
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return new Dimension(900, 610);
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 24;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return Math.max(24, visibleRect.height - 48);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    private String getSoundButtonLabel() {
        boolean allMuted = SoundManager.isMuted() && MusicManager.isMusicMuted();
        return allMuted ? "Âm Thanh: Tắt" : "Âm Thanh";
    }
}
