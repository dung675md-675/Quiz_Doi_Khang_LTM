package quiz.ui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import quiz.Wire;

/**
 * MatchScreen - Màn hình thi đấu đối kháng trực tiếp 1v1 của Quiz Arena:
 * - Giao diện Dark Game Theme (Deep Navy #1F2A44) theo chuẩn Kahoot-inspired.
 * - HUD trên cùng: Hiển thị ĐIỂM SỐ THỜI GIAN THỰC của cả 2 người chơi ngay cạnh tên,
 *   kèm huy hiệu Versus 1v1 và đồng hồ đếm ngược cho từng câu hỏi.
 * - Đồng hồ câu hỏi: Thời gian chia đều cho từng câu hỏi, đếm ngược về 0 với cảnh báo âm thanh
 *   và tự động chuyển sang câu tiếp theo khi hết giờ.
 * - Hiệu ứng chuyển câu hỏi sống động:
 *   + Phase 1: Nội dung câu hỏi xuất hiện nổi bật ở giữa với hiệu ứng nở đàn hồi (BackEaseOut).
 *   + Phase 2: Câu hỏi chuyển vị trí lướt nhẹ lên vị trí mặc định trên cùng.
 *   + Phase 3: 4 câu trả lời lần lượt trượt từ dưới lên kèm hiệu ứng mờ dần (staggered rise).
 */
public class MatchScreen extends JPanel {

    private final String matchId;
    private final String myName;
    private final String opponentName;
    private final String myAvatarId;
    private final String opponentAvatarId;
    private final String topic;
    private final String level;
    private final String setId;
    private final int totalSeconds;
    private final int perQuestionSeconds;
    private final List<MatchQuestion> questions;

    private final Consumer<int[]> onSubmitListener;
    private final Runnable onLeaveListener;
    private BiConsumer<Integer, Integer> onAnswerListener;
    private Consumer<Integer> onQuestionChangeListener;
    private Consumer<String> onEmoteListener;

    private int currentQuestionIndex = 0;
    private final int[] selectedAnswers = new int[10];
    private final boolean[] questionFinalized = new boolean[10];
    private final int[] questionTimeLeft = new int[10];
    private boolean isSubmitted = false;
    private volatile boolean hasLeft = false;

    // Opponent 10-question real-time progress
    private int opponentCurrentQuestion = 0;
    private final int[] opponentStates = new int[10]; // 0 = chưa làm, 1 = đúng, 2 = sai
    private final JLabel lblOpponentProgress;
    private final JPanel opponentProgressStrip;

    // Real-time scores, speed bonus points, streak & ping
    private int myScore = 0;
    private int opponentScore = 0;
    private int myPoints = 0;
    private int opponentPoints = 0;
    private int myStreak = 0;
    private int opponentStreak = 0;
    private final ScoreBadge myScoreBadge;
    private final ScoreBadge oppScoreBadge;
    private final ModernBadge badgePing;
    private final JPanel tugOfWarBar;

    // Per-question countdown timer components
    private int currentQuestionTimeRemaining;
    private Timer questionCountdownTimer;
    private final JLabel lblTimer;
    private final JProgressBar timeProgressBar;
    private final SmoothTimerBanner questionTimerBanner;

    // Question display components
    private final JLabel lblQuestionIndex;
    private final JLabel lblQuestionTypeBadge;
    private final JLabel lblHint;
    private final JLabel lblMultiSelectNotice;
    private final JTextArea txtQuestion;
    private final ModernButton[] answerButtons = new ModernButton[4];
    private final JButton[] navButtons = new JButton[10];
    private final JLabel lblProgressSummary;
    private final AnimatedQuestionCard questionCard;
    private final JPanel answersGrid;
    private final JPanel tfPanel;
    private final ModernButton btnTrue;
    private final ModernButton btnFalse;
    private final JPanel fillInPanel;
    private final ModernFillInField txtFillInInput;
    private final JLabel lblFillInCorrectAnswer;
    private final String[] userFillInAnswers = new String[10];
    private final JPanel questionCardCenter;
    private final JPanel dndQuestionContainer;
    private final JPanel bottomOptionsCard;
    private final JPanel dndPoolContainer;
    private DragDropPanel currentDragDropPanel = null;
    private final DragDropPanel[] dndPanels = new DragDropPanel[10];

    // Entrance Animation state
    private Timer animTimer;
    private long animStartTime;
    private boolean isAnimating = false;

    // Reveal Animation state
    private boolean isRevealing = false;
    private Timer revealTimer = null;

    // Action buttons
    private final ModernButton btnNext;
    private final ModernButton btnSubmit;
    private final ModernButton btnLeave;

    // Waiting banner overlay
    private final JPanel waitingBanner;
    private final JLabel lblWaitingStatus;

    // Emotes & Audio containers
    private final JPanel myEmoteContainer;
    private final JPanel opponentEmoteContainer;
    private EmoteBubble currentMyBubble;
    private EmoteBubble currentOpponentBubble;
    private SubmitConfirmDialog activeSubmitDialog;

    // Synchronized Match Rhythm & Overlays (Ready Check, Countdown, Intro, Reveal)
    private Runnable onReadyListener;
    private Consumer<Integer> onSkipRevealListener;
    private RhythmOverlayPanel rhythmOverlayPanel;
    private boolean serverDrivenRhythm = false;

    public MatchScreen(
            String matchId,
            String myName,
            String opponentName,
            String topic,
            String level,
            String setId,
            int totalSeconds,
            List<MatchQuestion> questions,
            Consumer<int[]> onSubmitListener,
            Runnable onLeaveListener) {
        this(matchId, myName, opponentName, "avatar_01", "avatar_01", topic, level, setId, totalSeconds, questions, onSubmitListener, onLeaveListener);
    }

    public MatchScreen(
            String matchId,
            String myName,
            String opponentName,
            String myAvatarId,
            String opponentAvatarId,
            String topic,
            String level,
            String setId,
            int totalSeconds,
            List<MatchQuestion> questions,
            Consumer<int[]> onSubmitListener,
            Runnable onLeaveListener) {

        this.matchId = matchId;
        this.myName = myName;
        this.opponentName = opponentName;
        this.myAvatarId = myAvatarId != null ? myAvatarId : "avatar_01";
        this.opponentAvatarId = opponentAvatarId != null ? opponentAvatarId : "avatar_01";
        this.topic = topic;
        this.level = level;
        this.setId = setId;
        this.totalSeconds = totalSeconds > 0 ? totalSeconds : 60;
        this.questions = questions;
        this.onSubmitListener = onSubmitListener;
        this.onLeaveListener = onLeaveListener;

        int qCount = (questions != null && !questions.isEmpty()) ? questions.size() : 10;
        this.perQuestionSeconds = Math.max(5, this.totalSeconds / qCount);
        this.currentQuestionTimeRemaining = this.perQuestionSeconds;

        for (int i = 0; i < 10; i++) {
            selectedAnswers[i] = -1;
            userFillInAnswers[i] = "";
            dndPanels[i] = null;
            questionTimeLeft[i] = this.perQuestionSeconds;
        }

        setLayout(new BorderLayout());
        setOpaque(true);
        setBackground(new Color(0x0E, 0x15, 0x28));

        // ==========================================
        // 1. TOP HUD (Kahoot! / Quizizz Arena Header)
        // ==========================================
        JPanel topHud = new JPanel(new BorderLayout(12, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setupAntiAliasing(g2);
                int w = getWidth();
                int h = getHeight();
                GradientPaint gp = new GradientPaint(
                        0, 0, new Color(0x16, 0x1F, 0x38, 235),
                        w, 0, new Color(0x1E, 0x1B, 0x4B, 235));
                g2.setPaint(gp);
                g2.fillRect(0, 0, w, h);
                g2.setColor(new Color(255, 255, 255, 22));
                g2.drawLine(0, h - 1, w, h - 1);
                g2.dispose();
            }
        };
        topHud.setOpaque(false);
        topHud.setBorder(new EmptyBorder(10, 20, 10, 20));

        // 1.1 Left: Topic, Level & Ping Badges (Giữ nguyên ban đầu đầy đủ 3 huy hiệu)
        JPanel leftMeta = new JPanel();
        leftMeta.setLayout(new BoxLayout(leftMeta, BoxLayout.Y_AXIS));
        leftMeta.setOpaque(false);

        JLabel lblGameMode = new JLabel("ĐỐI KHÁNG 1V1");
        lblGameMode.setFont(Theme.SMALL_BOLD);
        lblGameMode.setForeground(Theme.MUTED_GRAY);

        JPanel badgesRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        badgesRow.setOpaque(false);

        String displayTopic = (topic != null) ? topic : "Tổng hợp";
        if (displayTopic.length() > 22) {
            displayTopic = displayTopic.substring(0, 20) + "...";
        }
        ModernBadge badgeTopic = new ModernBadge(displayTopic, ModernBadge.Type.INFO);
        if (topic != null && topic.length() > 22) {
            badgeTopic.setToolTipText(topic);
        }
        badgesRow.add(badgeTopic);
        badgesRow.add(new ModernBadge(level + " - Bộ " + setId, ModernBadge.Type.RANK));
        badgePing = new ModernBadge("Ping: -- ms", ModernBadge.Type.READY);
        badgesRow.add(badgePing);

        leftMeta.add(lblGameMode);
        leftMeta.add(Box.createVerticalStrut(4));
        leftMeta.add(badgesRow);
        topHud.add(leftMeta, BorderLayout.WEST);

        // 1.2 Center: 1v1 Versus HUD with REAL-TIME SCORES, STREAK & TUG-OF-WAR (Single horizontal row, NEVER wraps)
        JPanel centerHud = new JPanel(new GridBagLayout());
        centerHud.setOpaque(false);

        myScoreBadge = new ScoreBadge(true);
        oppScoreBadge = new ScoreBadge(false);

        JPanel p1 = createPlayerBadge(myName, true, myScoreBadge);
        JPanel p2 = createPlayerBadge(opponentName, false, oppScoreBadge);

        // Center VS + Momentum Tug-of-War column
        JPanel vsColumn = new JPanel();
        vsColumn.setLayout(new BoxLayout(vsColumn, BoxLayout.Y_AXIS));
        vsColumn.setOpaque(false);

        JLabel vsBadge = createVsBadge();
        vsBadge.setAlignmentX(Component.CENTER_ALIGNMENT);

        tugOfWarBar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setupAntiAliasing(g2);
                int w = getWidth();
                int h = getHeight();
                int totalPts = myPoints + opponentPoints;
                float myRatio = 0.5f;
                if (totalPts > 0) {
                    myRatio = Math.max(0.12f, Math.min(0.88f, (float) myPoints / totalPts));
                } else if (myScore + opponentScore > 0) {
                    myRatio = Math.max(0.12f, Math.min(0.88f, (float) myScore / (myScore + opponentScore)));
                }
                int splitX = Math.round(w * myRatio);

                g2.setColor(new Color(0xEC, 0x48, 0x99));
                g2.fill(new RoundRectangle2D.Float(0, 0, w, h, h, h));

                Shape oldClip = g2.getClip();
                g2.clipRect(0, 0, splitX, h);
                g2.setColor(new Color(0x38, 0xBD, 0xF8));
                g2.fill(new RoundRectangle2D.Float(0, 0, w, h, h, h));
                g2.setClip(oldClip);

                g2.setColor(Color.WHITE);
                g2.fillOval(splitX - 3, 0, 6, h);
                g2.dispose();
            }
        };
        tugOfWarBar.setOpaque(false);
        tugOfWarBar.setPreferredSize(new Dimension(56, 6));
        tugOfWarBar.setMaximumSize(new Dimension(56, 6));
        tugOfWarBar.setAlignmentX(Component.CENTER_ALIGNMENT);
        tugOfWarBar.setToolTipText("Tương quan điểm số trực tiếp giữa Bạn và Đối thủ");

        vsColumn.add(vsBadge);
        vsColumn.add(Box.createVerticalStrut(3));
        vsColumn.add(tugOfWarBar);

        // Emote bubble containers
        myEmoteContainer = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        myEmoteContainer.setOpaque(false);
        myEmoteContainer.setPreferredSize(new Dimension(36, 36));

        opponentEmoteContainer = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        opponentEmoteContainer.setOpaque(false);
        opponentEmoteContainer.setPreferredSize(new Dimension(36, 36));

        JPanel p1Row = new JPanel(new FlowLayout(FlowLayout.CENTER, 3, 0));
        p1Row.setOpaque(false);
        p1Row.add(myEmoteContainer);
        p1Row.add(p1);

        JPanel p2Row = new JPanel(new FlowLayout(FlowLayout.CENTER, 3, 0));
        p2Row.setOpaque(false);
        p2Row.add(p2);
        p2Row.add(opponentEmoteContainer);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(0, 3, 0, 3);

        gbc.gridx = 0;
        centerHud.add(p1Row, gbc);

        gbc.gridx = 1;
        centerHud.add(vsColumn, gbc);

        gbc.gridx = 2;
        centerHud.add(p2Row, gbc);

        topHud.add(centerHud, BorderLayout.CENTER);

        // 1.3 Right: Countdown Timer for Question & Sound Toggle
        JPanel rightSection = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightSection.setOpaque(false);

        JPanel rightTimer = new JPanel();
        rightTimer.setLayout(new BoxLayout(rightTimer, BoxLayout.Y_AXIS));
        rightTimer.setOpaque(false);

        JLabel lblTimerTitle = new JLabel("CÂU HỎI", SwingConstants.RIGHT);
        lblTimerTitle.setFont(Theme.SMALL_BOLD);
        lblTimerTitle.setForeground(Theme.MUTED_GRAY);
        lblTimerTitle.setAlignmentX(Component.RIGHT_ALIGNMENT);

        lblTimer = new JLabel(formatTime(perQuestionSeconds), SwingConstants.RIGHT);
        lblTimer.setFont(Theme.TIMER);
        lblTimer.setForeground(Theme.WHITE);
        lblTimer.setAlignmentX(Component.RIGHT_ALIGNMENT);

        timeProgressBar = new JProgressBar(0, perQuestionSeconds) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setupAntiAliasing(g2);
                int w = getWidth();
                int h = getHeight();
                g2.setColor(new Color(0x0F, 0x17, 0x2A, 180));
                g2.fill(new RoundRectangle2D.Float(0, 0, w, h, h, h));
                int max = Math.max(1, getMaximum());
                float ratio = Math.max(0f, Math.min(1f, (float) getValue() / max));
                int fillW = Math.round(w * ratio);
                if (fillW > 0) {
                    g2.setColor(getForeground());
                    g2.fill(new RoundRectangle2D.Float(0, 0, fillW, h, h, h));
                }
                g2.dispose();
            }
        };
        timeProgressBar.setOpaque(false);
        timeProgressBar.setValue(perQuestionSeconds);
        timeProgressBar.setPreferredSize(new Dimension(110, 7));
        timeProgressBar.setMaximumSize(new Dimension(110, 7));
        timeProgressBar.setForeground(Theme.GREEN);
        timeProgressBar.setBackground(new Color(0, 0, 0, 80));
        timeProgressBar.setBorderPainted(false);
        timeProgressBar.setAlignmentX(Component.RIGHT_ALIGNMENT);

        rightTimer.add(lblTimerTitle);
        rightTimer.add(lblTimer);
        rightTimer.add(Box.createVerticalStrut(3));
        rightTimer.add(timeProgressBar);

        JButton btnSoundSettings = new JButton(getMatchSoundText()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setupAntiAliasing(g2);
                int w = getWidth();
                int h = getHeight();
                g2.setColor(getModel().isRollover() ? new Color(0x33, 0x41, 0x55) : new Color(0x1E, 0x29, 0x3B));
                g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 12, 12));
                g2.setColor(new Color(255, 255, 255, 38));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, 12, 12));
                g2.setColor(Theme.WHITE);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int tx = (w - fm.stringWidth(getText())) / 2;
                int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(getText(), tx, ty);
                g2.dispose();
            }
        };
        btnSoundSettings.setToolTipText("Cài đặt âm thanh & nhạc nền (Bật/Tắt & Âm lượng)");
        btnSoundSettings.setFont(Theme.SMALL_BOLD);
        btnSoundSettings.setPreferredSize(new Dimension(84, 36));
        btnSoundSettings.setFocusPainted(false);
        btnSoundSettings.setBorderPainted(false);
        btnSoundSettings.setContentAreaFilled(false);
        btnSoundSettings.setOpaque(false);
        btnSoundSettings.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnSoundSettings.addActionListener(e -> {
            SoundManager.playClick();
            new SoundSettingsDialog(SwingUtilities.getWindowAncestor(this)).setVisible(true);
            btnSoundSettings.setText(getMatchSoundText());
            btnSoundSettings.repaint();
        });

        rightSection.add(rightTimer);
        rightSection.add(btnSoundSettings);
        topHud.add(rightSection, BorderLayout.EAST);

        add(topHud, BorderLayout.NORTH);

        // ==========================================
        // 2. QUESTION NAVIGATOR STRIP (Top Sub-bar)
        // ==========================================
        JPanel navStrip = new JPanel(new BorderLayout(12, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(0x11, 0x18, 0x2B, 215));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(255, 255, 255, 18));
                g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
                g2.dispose();
            }
        };
        navStrip.setOpaque(false);
        navStrip.setBorder(new EmptyBorder(7, 20, 7, 20));

        lblProgressSummary = new JLabel("Đã làm: 0/10 câu", SwingConstants.LEFT);
        lblProgressSummary.setFont(Theme.BODY_BOLD);
        lblProgressSummary.setForeground(new Color(0xE2, 0xE8, 0xF0));
        navStrip.add(lblProgressSummary, BorderLayout.WEST);

        JPanel navGrid = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        navGrid.setOpaque(false);

        for (int i = 0; i < 10; i++) {
            final int qIndex = i;
            JButton btn = new JButton(String.valueOf(i + 1)) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    Theme.setupAntiAliasing(g2);
                    int w = getWidth();
                    int h = getHeight();

                    boolean isCurrent = (qIndex == currentQuestionIndex);
                    boolean isAnswered = (selectedAnswers[qIndex] != -1);
                    boolean isFinal = questionFinalized[qIndex];
                    boolean isHover = getModel().isRollover();

                    Color bg;
                    Color fg;
                    Color border = null;

                    if (isCurrent) {
                        bg = new Color(0x7C, 0x3A, 0xED); // Vibrant Violet
                        fg = Theme.WHITE;
                        border = new Color(0xDD, 0xD6, 0xFE);
                    } else if (isFinal) {
                        bg = new Color(0x15, 0x80, 0x3D);
                        fg = Theme.WHITE;
                        border = new Color(0x4A, 0xDE, 0x80, 140);
                    } else if (isAnswered) {
                        bg = new Color(0x1D, 0x4E, 0xD8);
                        fg = Theme.WHITE;
                        border = new Color(0x60, 0xA5, 0xFA, 140);
                    } else {
                        bg = isHover ? new Color(0x33, 0x41, 0x55) : new Color(0x1E, 0x29, 0x3B);
                        fg = new Color(0x94, 0xA3, 0xB8);
                        border = new Color(0x33, 0x41, 0x55);
                    }

                    g2.setColor(bg);
                    g2.fill(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, 10, 10));

                    if (border != null) {
                        g2.setColor(border);
                        g2.setStroke(new BasicStroke(isCurrent ? 2f : 1.2f));
                        g2.draw(new RoundRectangle2D.Float(1.5f, 1.5f, w - 3, h - 3, 10, 10));
                    }

                    g2.setColor(fg);
                    g2.setFont(isCurrent ? Theme.BODY_BOLD : Theme.BODY);
                    FontMetrics fm = g2.getFontMetrics();
                    int tx = (w - fm.stringWidth(getText())) / 2;
                    int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
                    g2.drawString(getText(), tx, ty);
                    g2.dispose();
                }
            };
            btn.setPreferredSize(new Dimension(36, 30));
            btn.setFocusPainted(false);
            btn.setBorderPainted(false);
            btn.setContentAreaFilled(false);
            btn.setOpaque(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.setToolTipText("Chuyển tới Câu " + (i + 1));
            btn.addActionListener(e -> {
                if (!isSubmitted && !isAnimating && !isRevealing && !serverDrivenRhythm && qIndex != currentQuestionIndex) {
                    SoundManager.playClick();
                    syncCurrentAnswerInput();
                    navigateToQuestion(qIndex);
                }
            });
            navButtons[i] = btn;
            navGrid.add(btn);
        }
        navStrip.add(navGrid, BorderLayout.CENTER);

        JPanel oppProgPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 3));
        oppProgPanel.setOpaque(false);

        lblOpponentProgress = new JLabel("Đối thủ: 0/10", SwingConstants.RIGHT);
        lblOpponentProgress.setFont(Theme.SMALL_BOLD);
        lblOpponentProgress.setForeground(new Color(0xF5, 0xD0, 0xFE));
        oppProgPanel.add(lblOpponentProgress);

        opponentProgressStrip = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setupAntiAliasing(g2);
                int w = getWidth();
                int h = getHeight();
                int gap = 3;
                int segW = Math.max(8, (w - gap * 9) / 10);
                int segH = h - 4;
                int y = 2;
                for (int i = 0; i < 10; i++) {
                    int x = i * (segW + gap);
                    int st = opponentStates[i];
                    boolean isOppCur = (i == opponentCurrentQuestion);
                    Color segColor;
                    if (st == 1) {
                        segColor = new Color(0x22, 0xC5, 0x5E); // Đúng (Green)
                    } else if (st == 2) {
                        segColor = new Color(0xEF, 0x44, 0x44); // Sai (Red)
                    } else if (isOppCur) {
                        segColor = new Color(0xC0, 0x84, 0xFC); // Đang làm câu này (Purple)
                    } else {
                        segColor = new Color(0x2A, 0x37, 0x59); // Chưa làm
                    }
                    g2.setColor(segColor);
                    g2.fill(new RoundRectangle2D.Float(x, y, segW, segH, 5, 5));
                    if (isOppCur) {
                        g2.setColor(Theme.WHITE);
                        g2.setStroke(new BasicStroke(1.5f));
                        g2.draw(new RoundRectangle2D.Float(x + 0.5f, y + 0.5f, segW - 1, segH - 1, 5, 5));
                    }
                }
                g2.dispose();
            }
        };
        opponentProgressStrip.setOpaque(false);
        opponentProgressStrip.setPreferredSize(new Dimension(145, 22));
        opponentProgressStrip.setToolTipText("Tiến độ 10 câu hỏi thời gian thực của đối thủ");
        oppProgPanel.add(opponentProgressStrip);

        navStrip.add(oppProgPanel, BorderLayout.EAST);

        // ==========================================
        // 3. CENTER CONTENT (Question + 4 Kahoot Answers)
        // ==========================================
        JPanel centerWrapper = new JPanel(new BorderLayout(0, 14));
        centerWrapper.setOpaque(false);
        centerWrapper.setBorder(new EmptyBorder(14, 24, 14, 24));

        // 3.1 Waiting Banner Overlay (hidden by default)
        waitingBanner = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 10)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setupAntiAliasing(g2);
                g2.setColor(getBackground());
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 14, 14));
                g2.setColor(new Color(255, 255, 255, 90));
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(new RoundRectangle2D.Float(1, 1, getWidth() - 2, getHeight() - 2, 14, 14));
                g2.dispose();
            }
        };
        waitingBanner.setOpaque(false);
        waitingBanner.setBackground(new Color(0x13, 0x68, 0xCE, 220));
        lblWaitingStatus = new JLabel("BẠN ĐÃ NỘP BÀI! ĐANG CHỜ ĐỐI THỦ HOÀN THÀNH...", SwingConstants.CENTER);
        lblWaitingStatus.setFont(Theme.SUBTITLE);
        lblWaitingStatus.setForeground(Theme.WHITE);
        waitingBanner.add(lblWaitingStatus);
        waitingBanner.setVisible(false);

        // Animated Question Card (Kahoot! / Quizizz Arena Stage)
        questionCard = new AnimatedQuestionCard(new BorderLayout(0, 12));
        questionCard.setCardBackground(new Color(0x17, 0x20, 0x38));
        questionCard.setBorderColor(new Color(0x33, 0x41, 0x55));
        questionCard.setCardPadding(16, 24, 18, 24);

        // Question header with Question Pill, Type Pill, Hint, and Full-width Smooth Timer Banner
        JPanel qTopStack = new JPanel(new BorderLayout(0, 10));
        qTopStack.setOpaque(false);

        questionTimerBanner = new SmoothTimerBanner(perQuestionSeconds);
        qTopStack.add(questionTimerBanner, BorderLayout.NORTH);

        JPanel qHeader = new JPanel(new BorderLayout(12, 0));
        qHeader.setOpaque(false);

        JPanel qHeaderLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        qHeaderLeft.setOpaque(false);

        lblQuestionIndex = new JLabel("CÂU HỎI 1 / 10") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setupAntiAliasing(g2);
                g2.setColor(new Color(0x25, 0x63, 0xEB, 65));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                g2.setColor(new Color(0x60, 0xA5, 0xFA, 130));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 12, 12));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        lblQuestionIndex.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblQuestionIndex.setForeground(new Color(0x93, 0xC5, 0xFD));
        lblQuestionIndex.setBorder(new EmptyBorder(4, 12, 4, 12));

        lblQuestionTypeBadge = new JLabel("TRẮC NGHIỆM") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setupAntiAliasing(g2);
                g2.setColor(new Color(0x7C, 0x3A, 0xED, 65));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                g2.setColor(new Color(0xC0, 0x84, 0xFC, 120));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 12, 12));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        lblQuestionTypeBadge.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblQuestionTypeBadge.setForeground(new Color(0xE9, 0xD5, 0xFF));
        lblQuestionTypeBadge.setBorder(new EmptyBorder(4, 10, 4, 10));

        qHeaderLeft.add(lblQuestionIndex);
        qHeaderLeft.add(lblQuestionTypeBadge);
        qHeader.add(qHeaderLeft, BorderLayout.WEST);

        lblHint = new JLabel("Chọn 1 trong 4 đáp án bên dưới");
        lblHint.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        lblHint.setForeground(new Color(0x94, 0xA3, 0xB8));
        qHeader.add(lblHint, BorderLayout.EAST);

        qTopStack.add(qHeader, BorderLayout.CENTER);

        // Multi-select notice label (appears directly above question text)
        lblMultiSelectNotice = new JLabel("[Nhiều đáp án] Chọn tất cả các đáp án đúng");
        lblMultiSelectNotice.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblMultiSelectNotice.setForeground(new Color(0x38, 0xBD, 0xF8));
        lblMultiSelectNotice.setBorder(new EmptyBorder(0, 4, 4, 0));
        lblMultiSelectNotice.setVisible(false);

        // Question text
        txtQuestion = new JTextArea("Đang tải câu hỏi...");
        txtQuestion.setFont(Theme.TITLE);
        txtQuestion.setForeground(Theme.WHITE);
        txtQuestion.setOpaque(false);
        txtQuestion.setBackground(new Color(0, 0, 0, 0));
        txtQuestion.setLineWrap(true);
        txtQuestion.setWrapStyleWord(true);
        txtQuestion.setEditable(false);
        txtQuestion.setFocusable(false);
        txtQuestion.setBorder(new EmptyBorder(6, 4, 8, 4));

        JPanel normalQuestionPanel = new JPanel(new BorderLayout(0, 4));
        normalQuestionPanel.setOpaque(false);
        normalQuestionPanel.add(lblMultiSelectNotice, BorderLayout.NORTH);
        normalQuestionPanel.add(txtQuestion, BorderLayout.CENTER);

        // Question center card switcher (Standard Text vs Drag & Drop Sentence)
        questionCardCenter = new JPanel(new CardLayout());
        questionCardCenter.setOpaque(false);
        questionCardCenter.add(normalQuestionPanel, "NORMAL");

        dndQuestionContainer = new JPanel(new BorderLayout());
        dndQuestionContainer.setOpaque(false);
        questionCardCenter.add(dndQuestionContainer, "DND");

        questionCard.add(qTopStack, BorderLayout.NORTH);
        questionCard.add(questionCardCenter, BorderLayout.CENTER);

        // 4 Kahoot-style Answer Buttons (2x2 grid with geometric icons ▲ ◆ ● ■)
        answersGrid = new JPanel(new GridLayout(2, 2, 16, 16));
        answersGrid.setOpaque(false);
        answersGrid.setPreferredSize(new Dimension(0, 176));

        answerButtons[0] = new ModernButton("A", ModernButton.Style.ANSWER_A, "A");
        answerButtons[1] = new ModernButton("B", ModernButton.Style.ANSWER_B, "B");
        answerButtons[2] = new ModernButton("C", ModernButton.Style.ANSWER_C, "C");
        answerButtons[3] = new ModernButton("D", ModernButton.Style.ANSWER_D, "D");

        for (int i = 0; i < 4; i++) {
            final int choiceIndex = i;
            answerButtons[i].setFont(Theme.BODY_BOLD);
            answerButtons[i].setCornerRadius(16);
            answerButtons[i].addActionListener(e -> selectAnswer(choiceIndex));
            answersGrid.add(answerButtons[i]);
        }

        // True / False 2-Button Card (Split 50/50: Blue & Yellow)
        tfPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        tfPanel.setOpaque(false);
        tfPanel.setPreferredSize(new Dimension(0, 176));

        btnTrue = new ModernButton("ĐÚNG", ModernButton.Style.TRUE_BLUE, "✓");
        btnTrue.setFont(new Font("Segoe UI", Font.BOLD, 22));
        btnTrue.setCornerRadius(16);
        btnTrue.addActionListener(e -> selectAnswer(0));

        btnFalse = new ModernButton("SAI", ModernButton.Style.FALSE_YELLOW, "✗");
        btnFalse.setFont(new Font("Segoe UI", Font.BOLD, 22));
        btnFalse.setCornerRadius(16);
        btnFalse.addActionListener(e -> selectAnswer(1));

        tfPanel.add(btnTrue);
        tfPanel.add(btnFalse);

        // Fill-in text input panel (Center rounded square / card)
        fillInPanel = new JPanel(new GridBagLayout());
        fillInPanel.setOpaque(false);
        fillInPanel.setPreferredSize(new Dimension(0, 176));

        JPanel fillInBox = new JPanel();
        fillInBox.setLayout(new BoxLayout(fillInBox, BoxLayout.Y_AXIS));
        fillInBox.setOpaque(false);

        txtFillInInput = new ModernFillInField("Nhập đáp án (Nhấn Enter để chốt)");
        txtFillInInput.setPreferredSize(new Dimension(540, 58));
        txtFillInInput.setMaximumSize(new Dimension(540, 58));
        txtFillInInput.setAlignmentX(Component.CENTER_ALIGNMENT);
        txtFillInInput.addActionListener(e -> {
            if (!isRevealing && !isSubmitted) {
                if (isAnimating && animTimer != null) {
                    animTimer.stop();
                    isAnimating = false;
                    questionCard.setAnimState(1.0f, 0, 1.0f);
                    txtFillInInput.setAnimScale(1.0f);
                }
                SoundManager.playClick();
                submitFillInFromInput();
            }
        });
        txtFillInInput.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void update() {
                if (currentQuestionIndex >= 0 && currentQuestionIndex < 10) {
                    userFillInAnswers[currentQuestionIndex] = txtFillInInput.getText();
                    if (!txtFillInInput.getText().trim().isEmpty()) {
                        selectedAnswers[currentQuestionIndex] = 1;
                    } else {
                        selectedAnswers[currentQuestionIndex] = -1;
                    }
                    updateNavState();
                }
            }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { update(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { update(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { update(); }
        });

        lblFillInCorrectAnswer = new JLabel("", SwingConstants.CENTER);
        lblFillInCorrectAnswer.setFont(Theme.BODY_BOLD);
        lblFillInCorrectAnswer.setForeground(new Color(0x86, 0xEF, 0xAC));
        lblFillInCorrectAnswer.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblFillInCorrectAnswer.setBorder(new EmptyBorder(8, 0, 0, 0));
        lblFillInCorrectAnswer.setVisible(false);

        fillInBox.add(Box.createVerticalGlue());
        fillInBox.add(txtFillInInput);
        fillInBox.add(lblFillInCorrectAnswer);
        fillInBox.add(Box.createVerticalGlue());

        fillInPanel.add(fillInBox);

        // Bottom area card switcher (Standard 4 Buttons vs True/False vs Fill-In vs Drag & Drop Chip Pool)
        bottomOptionsCard = new JPanel(new CardLayout());
        bottomOptionsCard.setOpaque(false);
        bottomOptionsCard.setPreferredSize(new Dimension(0, 176));
        bottomOptionsCard.add(answersGrid, "BUTTONS");
        bottomOptionsCard.add(tfPanel, "TRUE_FALSE");
        bottomOptionsCard.add(fillInPanel, "FILL_IN");

        dndPoolContainer = new JPanel(new BorderLayout());
        dndPoolContainer.setOpaque(false);
        bottomOptionsCard.add(dndPoolContainer, "DND_POOL");

        JPanel questionArea = new JPanel(new BorderLayout(0, 14));
        questionArea.setOpaque(false);
        questionArea.add(questionCard, BorderLayout.CENTER);
        questionArea.add(bottomOptionsCard, BorderLayout.SOUTH);

        centerWrapper.add(waitingBanner, BorderLayout.NORTH);
        centerWrapper.add(questionArea, BorderLayout.CENTER);

        JPanel mainCenter = new JPanel(new BorderLayout());
        mainCenter.setOpaque(false);
        mainCenter.add(navStrip, BorderLayout.NORTH);
        mainCenter.add(centerWrapper, BorderLayout.CENTER);

        JPanel centerContainer = new JPanel() {
            @Override
            public boolean isOptimizedDrawingEnabled() {
                return false;
            }
        };
        centerContainer.setLayout(new OverlayLayout(centerContainer));
        centerContainer.setOpaque(false);

        rhythmOverlayPanel = new RhythmOverlayPanel(this);
        rhythmOverlayPanel.setAlignmentX(0.5f);
        rhythmOverlayPanel.setAlignmentY(0.5f);

        mainCenter.setAlignmentX(0.5f);
        mainCenter.setAlignmentY(0.5f);

        centerContainer.add(rhythmOverlayPanel);
        centerContainer.add(mainCenter);
        add(centerContainer, BorderLayout.CENTER);

        // ==========================================
        // 4. BOTTOM ACTION BAR (Footer)
        // ==========================================
        JPanel bottomBar = new JPanel(new BorderLayout(16, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(0x13, 0x1B, 0x30, 235));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(255, 255, 255, 20));
                g2.drawLine(0, 0, getWidth(), 0);
                g2.dispose();
            }
        };
        bottomBar.setOpaque(false);
        bottomBar.setBorder(new EmptyBorder(10, 24, 10, 24));

        // 4.1 Left: Leave Match Button & Emote Reactions
        JPanel leftBottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftBottom.setOpaque(false);

        btnLeave = new ModernButton("Thoát", ModernButton.Style.DANGER);
        btnLeave.setPreferredSize(new Dimension(92, 38));
        btnLeave.addActionListener(e -> {
            SoundManager.playClick();
            confirmLeave();
        });
        leftBottom.add(btnLeave);

        JPanel emoteBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        emoteBar.setOpaque(false);
        String[] emotes = { "🔥", "⚡", "😎", "👏", "😱", "💪" };
        for (String em : emotes) {
            JButton btnEm = new JButton(em) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    Theme.setupAntiAliasing(g2);
                    int w = getWidth();
                    int h = getHeight();
                    g2.setColor(getModel().isRollover() ? new Color(0x3B, 0x4C, 0x7A) : new Color(0x22, 0x2F, 0x4D));
                    g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 10, 10));
                    g2.setColor(new Color(255, 255, 255, 35));
                    g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, 10, 10));
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            btnEm.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 15));
            btnEm.setPreferredSize(new Dimension(35, 35));
            btnEm.setMargin(new Insets(0, 0, 0, 0));
            btnEm.setFocusPainted(false);
            btnEm.setBorderPainted(false);
            btnEm.setContentAreaFilled(false);
            btnEm.setOpaque(false);
            btnEm.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnEm.setForeground(Theme.WHITE);
            btnEm.addActionListener(e -> sendEmote(em));
            emoteBar.add(btnEm);
        }
        leftBottom.add(emoteBar);
        bottomBar.add(leftBottom, BorderLayout.WEST);

        // 4.2 Center: Next Navigation
        JPanel centerNav = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        centerNav.setOpaque(false);

        btnNext = new ModernButton("Chốt đáp án", ModernButton.Style.PRIMARY);
        btnNext.setPreferredSize(new Dimension(160, 40));
        btnNext.addActionListener(e -> {
            SoundManager.playClick();
            if (isRevealing) {
                btnNext.setText("Đã sẵn sàng...");
                btnNext.setEnabled(false);
                if (onSkipRevealListener != null) {
                    onSkipRevealListener.accept(currentQuestionIndex);
                }
            } else {
                finishCurrentQuestion();
                btnNext.setText("Đã chốt (Chờ...)");
                btnNext.setEnabled(false);
            }
        });

        centerNav.add(btnNext);
        bottomBar.add(centerNav, BorderLayout.CENTER);

        // 4.3 Right: Submit Button
        btnSubmit = new ModernButton("NỘP BÀI", ModernButton.Style.SUCCESS);
        btnSubmit.setPreferredSize(new Dimension(140, 40));
        btnSubmit.addActionListener(e -> {
            SoundManager.playClick();
            confirmSubmit();
        });
        bottomBar.add(btnSubmit, BorderLayout.EAST);

        add(bottomBar, BorderLayout.SOUTH);

        // ==========================================
        // 5. RESPONSIVE RESIZE / MAXIMIZE SCALING
        // ==========================================
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                int h = getHeight();
                int w = getWidth();
                if (h <= 0 || w <= 0) return;
                int optH = Math.max(152, Math.min(255, (int) (h * 0.28)));
                bottomOptionsCard.setPreferredSize(new Dimension(0, optH));
                answersGrid.setPreferredSize(new Dimension(0, optH));
                tfPanel.setPreferredSize(new Dimension(0, optH));
                fillInPanel.setPreferredSize(new Dimension(0, optH));

                int padX = Math.max(16, Math.min(36, w / 42));
                int padY = Math.max(10, Math.min(20, h / 48));
                centerWrapper.setBorder(new EmptyBorder(padY, padX, padY, padX));

                int qFontSize = Math.max(18, Math.min(27, 14 + h / 88));
                txtQuestion.setFont(Theme.TITLE.deriveFont((float) qFontSize));
                int btnFontSize = Math.max(15, Math.min(20, 12 + h / 120));
                Font btnFont = Theme.BODY_BOLD.deriveFont((float) btnFontSize);
                for (ModernButton b : answerButtons) {
                    if (b != null) b.setFont(btnFont);
                }
                revalidate();
            }
        });

        // ==========================================
        // 6. INITIALIZE IN READY_CHECK PHASE (Controlled by Server)
        // ==========================================
        SwingUtilities.invokeLater(() -> {
            rhythmOverlayPanel.showReadyCheck(10, false, false);
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.setupAntiAliasing(g2);
        int w = getWidth();
        int h = getHeight();

        // Kahoot! / Quizizz Arena Diagonal Gradient
        GradientPaint bgGrad = new GradientPaint(
                0, 0, new Color(0x0E, 0x15, 0x28),
                w, h, new Color(0x1E, 0x17, 0x3D));
        g2.setPaint(bgGrad);
        g2.fillRect(0, 0, w, h);

        // Ambient radial spotlight behind question stage
        RadialGradientPaint stageGlow = new RadialGradientPaint(
                new Point(w / 2, h / 2),
                Math.max(240, Math.min(w, h) * 0.65f),
                new float[] { 0.0f, 1.0f },
                new Color[] { new Color(0x4F, 0x46, 0xE5, 38), new Color(0, 0, 0, 0) });
        g2.setPaint(stageGlow);
        g2.fillRect(0, 0, w, h);

        // Subtle Kahoot geometric watermark shapes in background corners
        g2.setColor(new Color(255, 255, 255, 7));
        int sz = Math.max(90, Math.min(w, h) / 6);
        // Top-left Triangle
        int[] tx = { 45, 45 + sz, 45 - sz / 2 };
        int[] ty = { 70, 70 + sz, 70 + sz };
        g2.fillPolygon(tx, ty, 3);
        // Bottom-right Circle
        g2.fillOval(w - sz - 50, h - sz - 60, sz, sz);
        // Top-right Diamond
        int dx = w - 90;
        int dy = 110;
        int r = sz / 2;
        g2.fillPolygon(new int[] { dx, dx + r, dx, dx - r }, new int[] { dy - r, dy, dy + r, dy }, 4);
        // Bottom-left Rounded Square
        g2.fillRoundRect(40, h - sz - 70, sz - 15, sz - 15, 20, 20);

        g2.dispose();
    }

    // ==========================================
    // SCORE & PROGRESS MANAGEMENT & REAL-TIME DISPLAY
    // ==========================================
    public void updateProgress(int myCurrentQ, int oppCurrentQ, String myStateStr, String oppStateStr) {
        this.opponentCurrentQuestion = Math.max(0, Math.min(9, oppCurrentQ));
        int oppDoneCount = 0;
        if (oppStateStr != null) {
            for (int i = 0; i < 10 && i < oppStateStr.length(); i++) {
                char c = oppStateStr.charAt(i);
                if (c == '1') {
                    opponentStates[i] = 1;
                    oppDoneCount++;
                } else if (c == '2') {
                    opponentStates[i] = 2;
                    oppDoneCount++;
                } else {
                    opponentStates[i] = 0;
                }
            }
        }
        if (lblOpponentProgress != null) {
            lblOpponentProgress.setText("Đối thủ: " + oppDoneCount + "/10");
        }
        if (opponentProgressStrip != null) {
            opponentProgressStrip.repaint();
        }
    }

    public void updatePing(long ms) {
        if (badgePing != null) {
            badgePing.setText("Ping: " + ms + " ms");
        }
    }

    /** Khôi phục trạng thái sau khi reconnect (đáp án từ Server, điểm/tiến độ đến qua gói SCORE/PROGRESS). */
    public void restoreState(int myCurQ, int[] savedAnswers, boolean alreadySubmitted) {
        if (savedAnswers != null) {
            for (int i = 0; i < 10 && i < savedAnswers.length && i < selectedAnswers.length; i++) {
                selectedAnswers[i] = savedAnswers[i];
                if (savedAnswers[i] != -1) questionFinalized[i] = true;
            }
        }
        if (alreadySubmitted) {
            stopCountdownTimer();
            onSubmittedAcknowledged();
            return;
        }
        int targetQ = Math.max(0, Math.min(9, myCurQ));
        for (int i = 0; i < 10 && i < questionFinalized.length; i++) {
            if (!questionFinalized[i]) { targetQ = i; break; }
        }
        navigateToQuestion(targetQ);
    }

    public void restoreState(int myCurQ, int[] savedAnswers, int myScore, int oppScore,
                             int myPts, int oppPts, int myStrk, int oppStrk,
                             int oppCurQ, String mySt, String oppSt) {
        if (savedAnswers != null) {
            for (int i = 0; i < 10 && i < savedAnswers.length; i++) {
                selectedAnswers[i] = savedAnswers[i];
                if (savedAnswers[i] != -1) {
                    questionFinalized[i] = true;
                }
            }
        }
        updateScores(myScore, oppScore, myPts, oppPts, myStrk, oppStrk);
        updateProgress(myCurQ, oppCurQ, mySt, oppSt);
        int targetQ = Math.max(0, Math.min(9, myCurQ));
        for (int i = 0; i < 10; i++) {
            if (!questionFinalized[i]) {
                targetQ = i;
                break;
            }
        }
        navigateToQuestion(targetQ);
    }

    public void updateScores(int myScore, int opponentScore) {
        updateScores(myScore, opponentScore, myScore * 100, opponentScore * 100, this.myStreak, this.opponentStreak);
    }

    public void updateScores(int myScore, int opponentScore, int myPts, int oppPts, int myStrk, int oppStrk) {
        if (myScore > this.myScore || myPts > this.myPoints) {
            SoundManager.playScorePoint();
        }
        this.myScore = myScore;
        this.opponentScore = opponentScore;
        this.myPoints = myPts;
        this.opponentPoints = oppPts;
        this.myStreak = myStrk;
        this.opponentStreak = oppStrk;
        if (myScoreBadge != null) {
            myScoreBadge.setStats(myScore, myPts, myStrk);
        }
        if (oppScoreBadge != null) {
            oppScoreBadge.setStats(opponentScore, oppPts, oppStrk);
        }
        if (tugOfWarBar != null) {
            tugOfWarBar.repaint();
        }
    }

    public void setMyScore(int score) {
        if (score > this.myScore) {
            SoundManager.playScorePoint();
        }
        this.myScore = score;
        if (myScoreBadge != null) {
            myScoreBadge.setScore(score);
        }
        if (tugOfWarBar != null) {
            tugOfWarBar.repaint();
        }
    }

    public void setOpponentScore(int score) {
        this.opponentScore = score;
        if (oppScoreBadge != null) {
            oppScoreBadge.setScore(score);
        }
        if (tugOfWarBar != null) {
            tugOfWarBar.repaint();
        }
    }

    public int getMyScore() {
        return myScore;
    }

    public int getOpponentScore() {
        return opponentScore;
    }

    public void setOnAnswerListener(BiConsumer<Integer, Integer> listener) {
        this.onAnswerListener = listener;
    }

    public void setOnQuestionChangeListener(Consumer<Integer> listener) {
        this.onQuestionChangeListener = listener;
    }

    public void setOnReadyListener(Runnable listener) {
        this.onReadyListener = listener;
    }

    public void setOnSkipRevealListener(Consumer<Integer> listener) {
        this.onSkipRevealListener = listener;
    }

    public boolean isServerDrivenRhythm() {
        return serverDrivenRhythm;
    }

    public void setServerDrivenRhythm(boolean serverDrivenRhythm) {
        this.serverDrivenRhythm = serverDrivenRhythm;
    }

    private JPanel createPlayerBadge(String name, boolean isMe, ScoreBadge badge) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 2)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setupAntiAliasing(g2);
                int w = getWidth();
                int h = getHeight();
                Color bg = isMe ? new Color(0x1E, 0x29, 0x3B, 175) : new Color(0x23, 0x1D, 0x3C, 175);
                Color border = isMe ? new Color(0x38, 0xBD, 0xF8, 95) : new Color(0xE8, 0x79, 0xF9, 95);
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 16, 16));
                g2.setColor(border);
                g2.setStroke(new BasicStroke(1.2f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, 16, 16));
                g2.dispose();
            }
        };
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(1, 6, 1, 6));

        final String avatarId = isMe ? this.myAvatarId : this.opponentAvatarId;

        JPanel avatar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setupAntiAliasing(g2);
                int size = Math.min(getWidth(), getHeight()) - 4;
                int ox = (getWidth() - size) / 2;
                int oy = (getHeight() - size) / 2;
                AvatarPicker.paintAvatar(g2, avatarId, ox, oy, size);
                g2.setColor(isMe ? new Color(0x38, 0xBD, 0xF8) : new Color(0xE8, 0x79, 0xF9));
                g2.setStroke(new BasicStroke(2.0f));
                g2.drawOval(ox, oy, size, size);
                g2.dispose();
            }
        };
        avatar.setPreferredSize(new Dimension(34, 34));
        avatar.setOpaque(false);

        String rawName = (name != null) ? name : "Người chơi";
        String displayName = rawName;
        if (displayName.length() > 15) {
            displayName = displayName.substring(0, 14) + "...";
        }
        String labelText = displayName + (isMe ? " (Bạn)" : "");
        JLabel lblName = new JLabel(labelText);
        lblName.setToolTipText(rawName + (isMe ? " (Bạn)" : ""));
        lblName.setFont(Theme.BODY_BOLD);
        lblName.setForeground(Theme.WHITE);

        if (isMe) {
            p.add(avatar);
            p.add(lblName);
            p.add(badge);
        } else {
            p.add(badge);
            p.add(lblName);
            p.add(avatar);
        }
        return p;
    }

    private JLabel createVsBadge() {
        JLabel vs = new JLabel("VS", SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setupAntiAliasing(g2);
                int w = getWidth();
                int h = getHeight();
                GradientPaint gp = new GradientPaint(
                        0, 0, new Color(0xF4, 0x3F, 0x5E),
                        w, h, new Color(0xF9, 0x73, 0x16));
                g2.setPaint(gp);
                g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 10, 10));
                g2.setColor(new Color(255, 255, 255, 110));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, 10, 10));
                g2.setColor(Theme.WHITE);
                g2.setFont(Theme.SMALL_BOLD);
                FontMetrics fm = g2.getFontMetrics();
                int x = (w - fm.stringWidth("VS")) / 2;
                int y = (h - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString("VS", x, y);
                g2.dispose();
            }
        };
        vs.setPreferredSize(new Dimension(34, 22));
        vs.setOpaque(false);
        return vs;
    }

    // ==========================================
    // PER-QUESTION COUNTDOWN TIMER
    // ==========================================
    private void startPerQuestionTimer() {
        stopCountdownTimer();
        if (currentQuestionIndex >= 0 && currentQuestionIndex < questionTimeLeft.length) {
            currentQuestionTimeRemaining = questionTimeLeft[currentQuestionIndex];
        }
        updateTimerDisplay();
        if (currentQuestionTimeRemaining <= 0) {
            revealAnswerAndAdvance();
            return;
        }

        questionCountdownTimer = new Timer(1000, (ActionEvent e) -> {
            if (hasLeft || isSubmitted) {
                stopCountdownTimer();
                return;
            }
            currentQuestionTimeRemaining = Math.max(0, currentQuestionTimeRemaining - 1);
            if (currentQuestionIndex >= 0 && currentQuestionIndex < questionTimeLeft.length) {
                questionTimeLeft[currentQuestionIndex] = currentQuestionTimeRemaining;
            }
            updateTimerDisplay();

            if (currentQuestionTimeRemaining <= 3 && currentQuestionTimeRemaining > 0) {
                SoundManager.playCountdown(currentQuestionIndex);
            }

            if (currentQuestionTimeRemaining <= 0) {
                stopCountdownTimer();
                revealAnswerAndAdvance();
            }
        });
        questionCountdownTimer.start();
    }

    private void stopCountdownTimer() {
        if (questionCountdownTimer != null && questionCountdownTimer.isRunning()) {
            questionCountdownTimer.stop();
        }
    }

    private void updateTimerDisplay() {
        lblTimer.setText(formatTime(currentQuestionTimeRemaining));
        timeProgressBar.setMaximum(perQuestionSeconds);
        timeProgressBar.setValue(currentQuestionTimeRemaining);
        if (questionTimerBanner != null) {
            questionTimerBanner.updateTime(currentQuestionTimeRemaining, perQuestionSeconds, false);
        }

        int warnThreshold = Math.max(4, perQuestionSeconds / 2);
        int dangerThreshold = Math.max(3, perQuestionSeconds / 4);
        if (currentQuestionTimeRemaining <= dangerThreshold) {
            Color danger = new Color(0xF4, 0x3F, 0x5E);
            lblTimer.setForeground(danger);
            timeProgressBar.setForeground(danger);
        } else if (currentQuestionTimeRemaining <= warnThreshold) {
            Color amber = new Color(0xFB, 0xBF, 0x24);
            lblTimer.setForeground(amber);
            timeProgressBar.setForeground(amber);
        } else {
            lblTimer.setForeground(Theme.WHITE);
            timeProgressBar.setForeground(new Color(0x22, 0xC5, 0x5E));
        }
        timeProgressBar.repaint();
    }

    private String formatTime(int sec) {
        int m = sec / 60;
        int s = sec % 60;
        return String.format("%02d:%02d", m, s);
    }

    // ==========================================
    // QUESTION NAVIGATION & ENTRANCE ANIMATION
    // ==========================================
    public void navigateToQuestion(int index) {
        if (hasLeft || questions == null || index < 0 || index >= questions.size()) {
            return;
        }
        if (rhythmOverlayPanel != null && rhythmOverlayPanel.isVisible()) {
            rhythmOverlayPanel.hideOverlay();
        }
        currentQuestionIndex = index;
        startQuestionEntrance();
    }

    private void startQuestionEntrance() {
        if (hasLeft || questions == null || currentQuestionIndex >= questions.size()) {
            return;
        }

        stopCountdownTimer();
        SoundManager.stopCountdown();
        if (animTimer != null && animTimer.isRunning()) {
            animTimer.stop();
        }
        if (revealTimer != null && revealTimer.isRunning()) {
            revealTimer.stop();
        }
        isRevealing = false;
        isAnimating = true;

        MatchQuestion q = questions.get(currentQuestionIndex);
        lblQuestionIndex.setText("CÂU HỎI " + (currentQuestionIndex + 1) + " / " + questions.size());
        if (onQuestionChangeListener != null) {
            onQuestionChangeListener.accept(currentQuestionIndex);
        }

        if (q.isDragAndDrop()) {
            lblQuestionTypeBadge.setText("KÉO THẢ");
            lblMultiSelectNotice.setVisible(false);
            lblHint.setText("Kéo thả các mảnh ghép phía dưới vào ô trống tương ứng");
            if (dndPanels[currentQuestionIndex] == null) {
                DragDropPanel panel = new DragDropPanel(q);
                final int qIdx = currentQuestionIndex;
                panel.setOnStateChangeListener(() -> {
                    if (panel.isAllCorrect()) {
                        selectedAnswers[qIdx] = 1;
                    } else if (panel.hasAnyAnswer()) {
                        selectedAnswers[qIdx] = 0;
                    } else {
                        selectedAnswers[qIdx] = -1;
                    }
                    updateNavState();
                });
                dndPanels[currentQuestionIndex] = panel;
            }
            currentDragDropPanel = dndPanels[currentQuestionIndex];

            dndQuestionContainer.removeAll();
            dndQuestionContainer.add(currentDragDropPanel.getSentencePanel(), BorderLayout.CENTER);
            dndQuestionContainer.revalidate();
            dndQuestionContainer.repaint();

            dndPoolContainer.removeAll();
            dndPoolContainer.add(currentDragDropPanel.getChipPoolPanel(), BorderLayout.CENTER);
            dndPoolContainer.revalidate();
            dndPoolContainer.repaint();

            ((CardLayout) questionCardCenter.getLayout()).show(questionCardCenter, "DND");
            ((CardLayout) bottomOptionsCard.getLayout()).show(bottomOptionsCard, "DND_POOL");

            currentDragDropPanel.setChipsAnimState(0.0f, 18);
            currentDragDropPanel.setInteractive(false);
        } else if (q.isFillIn()) {
            lblQuestionTypeBadge.setText("ĐIỀN TỪ");
            lblMultiSelectNotice.setVisible(false);
            lblHint.setText("Nhập câu trả lời từ bàn phím và nhấn Enter hoặc Câu tiếp");
            currentDragDropPanel = null;
            txtQuestion.setText(q.text());

            txtFillInInput.resetReveal();
            txtFillInInput.setAnimScale(1.0f);
            txtFillInInput.setEnabled(false);
            txtFillInInput.setText(userFillInAnswers[currentQuestionIndex] != null ? userFillInAnswers[currentQuestionIndex] : "");
            lblFillInCorrectAnswer.setVisible(false);
            lblFillInCorrectAnswer.setText("");

            ((CardLayout) questionCardCenter.getLayout()).show(questionCardCenter, "NORMAL");
            ((CardLayout) bottomOptionsCard.getLayout()).show(bottomOptionsCard, "FILL_IN");
        } else if (q.isTrueFalse()) {
            lblQuestionTypeBadge.setText("ĐÚNG / SAI");
            lblMultiSelectNotice.setVisible(false);
            lblHint.setText("Chọn 1 trong 2 đáp án: ĐÚNG hoặc SAI");
            currentDragDropPanel = null;
            txtQuestion.setText(q.text());

            tfPanel.removeAll();
            tfPanel.add(btnTrue);
            tfPanel.add(btnFalse);
            tfPanel.revalidate();

            int selected = selectedAnswers[currentQuestionIndex];
            btnTrue.setSelectedState(selected == 0);
            btnFalse.setSelectedState(selected == 1);
            btnTrue.setEnabled(false);
            btnFalse.setEnabled(false);
            btnTrue.resetReveal();
            btnFalse.resetReveal();
            btnTrue.setAnimScale(1.0f);
            btnFalse.setAnimScale(1.0f);
            btnTrue.setAnimState(0.0f, 18);
            btnFalse.setAnimState(0.0f, 18);

            ((CardLayout) questionCardCenter.getLayout()).show(questionCardCenter, "NORMAL");
            ((CardLayout) bottomOptionsCard.getLayout()).show(bottomOptionsCard, "TRUE_FALSE");
        } else if (q.isMultipleSelect()) {
            lblQuestionTypeBadge.setText("NHIỀU ĐÁP ÁN");
            lblMultiSelectNotice.setVisible(true);
            lblHint.setText("Chọn tất cả các đáp án đúng rồi nhấn Câu tiếp");
            currentDragDropPanel = null;
            txtQuestion.setText(q.text());

            answersGrid.removeAll();
            for (int i = 0; i < 4; i++) {
                answersGrid.add(answerButtons[i]);
            }
            answersGrid.revalidate();

            String[] choices = q.choices();
            String[] prefixes = { "A", "B", "C", "D" };
            int mask = selectedAnswers[currentQuestionIndex];

            for (int i = 0; i < 4; i++) {
                answerButtons[i].setText(i < choices.length ? choices[i] : "—");
                answerButtons[i].setPrefixBadge(prefixes[i]);
                answerButtons[i].setSelectedState(mask != -1 && ((mask & (1 << i)) != 0));
                answerButtons[i].setEnabled(false);
                answerButtons[i].resetReveal();
                answerButtons[i].setAnimScale(1.0f);
                answerButtons[i].setAnimState(0.0f, 18);
            }

            ((CardLayout) questionCardCenter.getLayout()).show(questionCardCenter, "NORMAL");
            ((CardLayout) bottomOptionsCard.getLayout()).show(bottomOptionsCard, "BUTTONS");
        } else {
            lblQuestionTypeBadge.setText("TRẮC NGHIỆM");
            lblMultiSelectNotice.setVisible(false);
            lblHint.setText("Chọn 1 trong 4 đáp án bên dưới");
            currentDragDropPanel = null;
            txtQuestion.setText(q.text());

            answersGrid.removeAll();
            for (int i = 0; i < 4; i++) {
                answersGrid.add(answerButtons[i]);
            }
            answersGrid.revalidate();

            String[] choices = q.choices();
            String[] prefixes = { "A", "B", "C", "D" };
            int selected = selectedAnswers[currentQuestionIndex];

            for (int i = 0; i < 4; i++) {
                answerButtons[i].setText(i < choices.length ? choices[i] : "—");
                answerButtons[i].setPrefixBadge(prefixes[i]);
                answerButtons[i].setSelectedState(selected == i);
                answerButtons[i].setEnabled(false);
                answerButtons[i].resetReveal();
                answerButtons[i].setAnimScale(1.0f);
                answerButtons[i].setAnimState(0.0f, 18);
            }

            ((CardLayout) questionCardCenter.getLayout()).show(questionCardCenter, "NORMAL");
            ((CardLayout) bottomOptionsCard.getLayout()).show(bottomOptionsCard, "BUTTONS");
        }

        btnNext.setEnabled(false);
        updateNavState();

        // Dịch chuyển nhẹ nhàng trong phạm vi card
        final int centerOffsetY = 10;

        // Giữ nguyên thời gian còn lại của câu hỏi này (không reset khi chuyển qua lại giữa các câu)
        if (currentQuestionIndex >= 0 && currentQuestionIndex < questionTimeLeft.length) {
            currentQuestionTimeRemaining = questionTimeLeft[currentQuestionIndex];
        } else {
            currentQuestionTimeRemaining = perQuestionSeconds;
        }
        if (questionFinalized[currentQuestionIndex]) {
            lblTimer.setText("ĐÃ CHỐT");
            lblTimer.setForeground(Theme.GREEN);
            timeProgressBar.setValue(0);
            if (questionTimerBanner != null) {
                questionTimerBanner.updateTime(0, perQuestionSeconds, true);
            }
        } else {
            updateTimerDisplay();
        }

        SoundManager.playPop();

        animStartTime = System.currentTimeMillis();
        animTimer = new Timer(16, e -> onAnimTick(centerOffsetY));
        animTimer.start();
    }

    private void onAnimTick(int centerOffsetY) {
        if (hasLeft) {
            if (animTimer != null && animTimer.isRunning()) {
                animTimer.stop();
            }
            return;
        }
        long elapsed = System.currentTimeMillis() - animStartTime;
        boolean isDnd = (currentDragDropPanel != null);
        MatchQuestion q = (questions != null && currentQuestionIndex < questions.size()) ? questions.get(currentQuestionIndex) : null;
        boolean isTf = (q != null && q.isTrueFalse());
        boolean isFill = (q != null && q.isFillIn());

        // Chuyển cảnh nhẹ nhàng, mượt mà trong 360ms
        if (elapsed < 360) {
            // 1. Question Card: 0ms -> 200ms (Ease-out cubic)
            float qp = Math.min(1.0f, elapsed / 200.0f);
            float qEase = 1.0f - (float) Math.pow(1.0f - qp, 3);
            float qScale = 0.96f + 0.04f * qEase;
            int qOffsetY = Math.round(centerOffsetY * (1.0f - qEase));
            float qAlpha = 0.35f + 0.65f * qEase;
            questionCard.setAnimState(qScale, qOffsetY, qAlpha);

            // 2. Answer Options: 60ms -> 360ms (Staggered gentle rise)
            if (isDnd) {
                float bp = Math.max(0.0f, Math.min(1.0f, (elapsed - 60.0f) / 240.0f));
                float bEased = 1.0f - (1.0f - bp) * (1.0f - bp);
                currentDragDropPanel.setChipsAnimState(bp, Math.round(18 * (1.0f - bEased)));
            } else if (isTf) {
                ModernButton[] tfBtns = { btnTrue, btnFalse };
                for (int i = 0; i < 2; i++) {
                    int bStart = 60 + i * 45;
                    if (elapsed <= bStart) {
                        tfBtns[i].setAnimState(0.0f, 18);
                    } else {
                        float bp = Math.min(1.0f, (elapsed - bStart) / 180.0f);
                        float bEased = 1.0f - (1.0f - bp) * (1.0f - bp);
                        tfBtns[i].setAnimState(bp, Math.round(18 * (1.0f - bEased)));
                    }
                }
            } else if (isFill) {
                float bp = Math.max(0.0f, Math.min(1.0f, (elapsed - 60.0f) / 220.0f));
                float bEased = 1.0f - (1.0f - bp) * (1.0f - bp);
                txtFillInInput.setAnimScale(0.85f + 0.15f * bEased);
            } else {
                for (int i = 0; i < 4; i++) {
                    int bStart = 55 + i * 35;
                    if (elapsed <= bStart) {
                        answerButtons[i].setAnimState(0.0f, 18);
                    } else {
                        float bp = Math.min(1.0f, (elapsed - bStart) / 180.0f);
                        float bEased = 1.0f - (1.0f - bp) * (1.0f - bp);
                        answerButtons[i].setAnimState(bp, Math.round(18 * (1.0f - bEased)));
                    }
                }
            }
        }
        // KẾT THÚC ANIMATION: BẬT TƯƠNG TÁC VÀ BẮT ĐẦU ĐẾM NGƯỢC
        else {
            animTimer.stop();
            isAnimating = false;
            questionCard.setAnimState(1.0f, 0, 1.0f);
            boolean isLocked = isSubmitted || questionFinalized[currentQuestionIndex];
            if (isDnd) {
                currentDragDropPanel.setChipsAnimState(1.0f, 0);
                currentDragDropPanel.setInteractive(!isLocked);
            } else if (isTf) {
                btnTrue.setAnimState(1.0f, 0);
                btnFalse.setAnimState(1.0f, 0);
                btnTrue.setEnabled(!isLocked);
                btnFalse.setEnabled(!isLocked);
            } else if (isFill) {
                txtFillInInput.setAnimScale(1.0f);
                txtFillInInput.setEnabled(!isLocked);
                if (!isLocked) txtFillInInput.requestFocusInWindow();
            } else {
                for (int i = 0; i < 4; i++) {
                    answerButtons[i].setAnimState(1.0f, 0);
                    answerButtons[i].setEnabled(!isLocked);
                }
            }
            btnNext.setEnabled(!isSubmitted);
            updateNavState();

            if (questionFinalized[currentQuestionIndex]) {
                lblHint.setText("Câu hỏi này đã chốt đáp án");
                lblTimer.setText("ĐÃ CHỐT");
                lblTimer.setForeground(Theme.GREEN);
                timeProgressBar.setValue(0);
                if (questionTimerBanner != null) {
                    questionTimerBanner.updateTime(0, perQuestionSeconds, true);
                }
            } else if (!isSubmitted) {
                // Khởi động bộ đếm ngược thời gian câu hỏi
                startPerQuestionTimer();
            }

            if (onQuestionChangeListener != null) {
                onQuestionChangeListener.accept(currentQuestionIndex);
            }
        }
    }

    private void updateNavState() {
        int answeredCount = 0;
        for (int i = 0; i < 10; i++) {
            if (selectedAnswers[i] != -1) {
                answeredCount++;
            }
            navButtons[i].repaint();
        }
        lblProgressSummary.setText("Đã làm: " + answeredCount + "/10 câu");
        int totalQ = (questions != null && !questions.isEmpty()) ? questions.size() : 10;
        if (btnNext != null) {
            btnNext.setText(currentQuestionIndex >= totalQ - 1 ? "Hoàn thành" : "Chốt & Câu tiếp");
        }
    }

    private void selectAnswer(int choiceIndex) {
        if (isSubmitted || isAnimating || isRevealing || questionFinalized[currentQuestionIndex]) {
            return;
        }
        SoundManager.playAnswerSelect();

        MatchQuestion q = (questions != null && currentQuestionIndex < questions.size())
                ? questions.get(currentQuestionIndex)
                : null;

        if (q != null && q.isMultipleSelect()) {
            int cur = selectedAnswers[currentQuestionIndex];
            int curMask = (cur <= 0) ? 0 : cur;
            curMask ^= (1 << choiceIndex);
            selectedAnswers[currentQuestionIndex] = (curMask == 0) ? -1 : curMask;

            if (choiceIndex >= 0 && choiceIndex < 4) {
                answerButtons[choiceIndex].triggerBounce();
            }
            for (int i = 0; i < 4; i++) {
                answerButtons[i].setSelectedState((curMask & (1 << i)) != 0);
            }
        } else if (q != null && q.isTrueFalse()) {
            if (selectedAnswers[currentQuestionIndex] == choiceIndex) {
                selectedAnswers[currentQuestionIndex] = -1;
            } else {
                selectedAnswers[currentQuestionIndex] = choiceIndex;
            }
            if (choiceIndex == 0) btnTrue.triggerBounce();
            else if (choiceIndex == 1) btnFalse.triggerBounce();
            btnTrue.setSelectedState(selectedAnswers[currentQuestionIndex] == 0);
            btnFalse.setSelectedState(selectedAnswers[currentQuestionIndex] == 1);
        } else {
            if (selectedAnswers[currentQuestionIndex] == choiceIndex) {
                selectedAnswers[currentQuestionIndex] = -1;
            } else {
                selectedAnswers[currentQuestionIndex] = choiceIndex;
            }
            if (choiceIndex >= 0 && choiceIndex < 4) {
                answerButtons[choiceIndex].triggerBounce();
            }
            for (int i = 0; i < 4; i++) {
                answerButtons[i].setSelectedState(selectedAnswers[currentQuestionIndex] == i);
            }
        }
        if (q != null && !q.isMultipleSelect() && selectedAnswers[currentQuestionIndex] != -1) {
            finishCurrentQuestion();
            for (int i = 0; i < 4; i++) answerButtons[i].setEnabled(false);
            btnTrue.setEnabled(false);
            btnFalse.setEnabled(false);
            btnNext.setText("Đã chốt (Chờ...)");
            btnNext.setEnabled(false);
            lblHint.setText("Đã chốt đáp án! Đang chờ đối thủ / mở kết quả...");
        }
        updateNavState();
        // Không cập nhật hay gửi điểm ở đây để người chơi không thể thử từng đáp án!
    }

    private void syncCurrentAnswerInput() {
        if (currentQuestionIndex >= 0 && currentQuestionIndex < 10 && !questionFinalized[currentQuestionIndex]) {
            MatchQuestion q = (questions != null && currentQuestionIndex < questions.size())
                    ? questions.get(currentQuestionIndex)
                    : null;
            if (q != null) {
                if (q.isDragAndDrop() && currentDragDropPanel != null) {
                    if (currentDragDropPanel.isAllCorrect()) {
                        selectedAnswers[currentQuestionIndex] = 1;
                    } else if (currentDragDropPanel.hasAnyAnswer()) {
                        selectedAnswers[currentQuestionIndex] = 0;
                    } else {
                        selectedAnswers[currentQuestionIndex] = -1;
                    }
                } else if (q.isFillIn()) {
                    String typed = txtFillInInput.getText().trim();
                    userFillInAnswers[currentQuestionIndex] = typed;
                    if (typed.isEmpty()) {
                        selectedAnswers[currentQuestionIndex] = -1;
                    } else if (isFillInCorrect(typed, q.choices())) {
                        selectedAnswers[currentQuestionIndex] = 1;
                    } else {
                        selectedAnswers[currentQuestionIndex] = 0;
                    }
                }
            }
        }
    }

    private void submitFillInFromInput() {
        if (serverDrivenRhythm) {
            finishCurrentQuestion();
            txtFillInInput.setEnabled(false);
            if (btnNext != null) {
                btnNext.setText("Đã chốt (Chờ...)");
                btnNext.setEnabled(false);
            }
            lblHint.setText("Đã chốt đáp án! Đang chờ đối thủ / mở kết quả...");
        } else {
            revealAnswerAndAdvance();
        }
    }

    private void finishCurrentQuestion() {
        if (currentQuestionIndex >= 0 && currentQuestionIndex < 10) {
            syncCurrentAnswerInput();
            if (!questionFinalized[currentQuestionIndex]) {
                questionFinalized[currentQuestionIndex] = true;
                int choice = selectedAnswers[currentQuestionIndex];
                if (onAnswerListener != null) {
                    onAnswerListener.accept(currentQuestionIndex, choice);
                }
            }
            if (serverDrivenRhythm) {
                stopCountdownTimer();
                SoundManager.stopCountdown();
            }
        }
    }

    /**
     * revealAnswerAndAdvance — Hiển thị đáp án đúng/sai với hiệu ứng phóng to trong 0.35s - 0.5s:
     * - Nút đúng hiện màu xanh lá (#16A34A) và phóng to lên.
     * - Nếu người chơi chọn sai, nút đã chọn hiện màu đỏ (#DC2626) và nút đúng hiện màu xanh lá.
     * - Chữ và biểu tượng của đáp án được hiển thị đầy đủ và phóng to đồng bộ theo nút.
     * - Với dạng FILL_IN: ô nhập chuyển màu xanh/đỏ, hiện đáp án đúng nếu sai.
     * - Với dạng MULTIPLE_SELECT: các đáp án đúng đều hiện xanh lá, đáp án chọn sai hiện đỏ.
     * - Sau khi giữ một lúc sẽ tự động chuyển sang câu tiếp theo hoặc nộp bài nếu là câu cuối.
     */
    private void revealAnswerAndAdvance() {
        if (serverDrivenRhythm) {
            if (isRevealing || isSubmitted) return;
            finishCurrentQuestion();
            if (btnNext != null) {
                btnNext.setText("Đã chốt (Chờ...)");
                btnNext.setEnabled(false);
            }
            return;
        }

        if (isRevealing || isSubmitted || isAnimating) {
            return;
        }
        stopCountdownTimer();
        // Nếu người chơi chọn đáp án sớm trước khi hết giờ, dừng countdown ngay
        if (currentQuestionTimeRemaining > 0) {
            SoundManager.stopCountdown();
        }

        // Nếu câu hỏi này đã từng chốt trước đó, chuyển thẳng sang câu kế tiếp
        if (questionFinalized[currentQuestionIndex]) {
            if (currentQuestionIndex + 1 < questions.size()) {
                navigateToQuestion(currentQuestionIndex + 1);
            } else {
                doSubmit();
            }
            return;
        }

        isRevealing = true;

        finishCurrentQuestion();

        MatchQuestion q = (questions != null && currentQuestionIndex < questions.size())
                ? questions.get(currentQuestionIndex)
                : null;
        if (q == null) {
            isRevealing = false;
            return;
        }

        // Tạm khóa thao tác của người dùng trong khi chạy hiệu ứng reveal
        btnNext.setEnabled(false);
        for (ModernButton b : answerButtons) {
            b.setEnabled(false);
        }
        btnTrue.setEnabled(false);
        btnFalse.setEnabled(false);
        txtFillInInput.setEnabled(false);
        if (currentDragDropPanel != null) {
            currentDragDropPanel.setInteractive(false);
        }

        int userChoice = selectedAnswers[currentQuestionIndex];
        int correctChoice = q.correctAnswer();

        // 1. Dạng câu hỏi Kéo thả
        if (q.isDragAndDrop()) {
            boolean isCorrect = (userChoice == 1);
            if (isCorrect) {
                SoundManager.playCorrect();
            } else {
                SoundManager.playWrong();
            }
            revealTimer = new Timer(750, e -> {
                ((Timer) e.getSource()).stop();
                isRevealing = false;
                if (hasLeft) return;
                if (currentQuestionIndex + 1 < questions.size()) {
                    navigateToQuestion(currentQuestionIndex + 1);
                } else {
                    doSubmit();
                }
            });
            revealTimer.setRepeats(false);
            revealTimer.start();
            return;
        }

        // 2. Dạng câu hỏi Điền vào chỗ trống (FILL_IN)
        if (q.isFillIn()) {
            boolean isCorrect = (userChoice == 1);
            Color fillInBg = isCorrect ? new Color(0x16, 0xA3, 0x4A) : new Color(0xDC, 0x26, 0x26);
            Color fillInBorder = isCorrect ? new Color(0x86, 0xEF, 0xAC) : new Color(0xFE, 0xCA, 0xCA);
            txtFillInInput.setRevealColors(fillInBg, fillInBorder);

            if (isCorrect) {
                SoundManager.playCorrect();
            } else {
                SoundManager.playWrong();
                String displayTarget = (q.choices() != null && q.choices().length > 0) ? q.choices()[0] : "";
                lblFillInCorrectAnswer.setText("Đáp án chính xác: " + displayTarget);
                lblFillInCorrectAnswer.setVisible(true);
            }

            final long animStart = System.currentTimeMillis();
            final int growDuration = 400;
            final int totalDuration = 850;

            revealTimer = new Timer(16, e -> {
                if (hasLeft) {
                    revealTimer.stop();
                    isRevealing = false;
                    return;
                }
                long elapsed = System.currentTimeMillis() - animStart;
                if (elapsed < growDuration) {
                    float p = (float) elapsed / growDuration;
                    float scale = 1.0f + 0.12f * (float) Math.sin(p * Math.PI / 2.0);
                    txtFillInInput.setAnimScale(scale);
                    fillInPanel.repaint();
                } else if (elapsed < totalDuration) {
                    txtFillInInput.setAnimScale(1.12f);
                    fillInPanel.repaint();
                } else {
                    revealTimer.stop();
                    isRevealing = false;
                    txtFillInInput.resetReveal();
                    lblFillInCorrectAnswer.setVisible(false);
                    fillInPanel.repaint();

                    if (hasLeft) return;
                    if (currentQuestionIndex + 1 < questions.size()) {
                        navigateToQuestion(currentQuestionIndex + 1);
                    } else {
                        doSubmit();
                    }
                }
            });
            revealTimer.start();
            return;
        }

        // 3. Dạng câu hỏi Chọn nhiều đáp án (MULTIPLE_SELECT)
        if (q.isMultipleSelect()) {
            int userMask = userChoice;
            int correctMask = correctChoice;
            boolean isAllCorrect = (userMask != -1 && userMask == correctMask);

            Color correctBg = new Color(0x16, 0xA3, 0x4A); // Green
            Color wrongBg = new Color(0xDC, 0x26, 0x26);   // Red
            Color whiteText = Color.WHITE;

            List<ModernButton> scalingButtons = new ArrayList<>();

            for (int c = 0; c < 4; c++) {
                boolean isThisCorrect = (correctMask & (1 << c)) != 0;
                boolean isThisSelected = (userMask != -1) && ((userMask & (1 << c)) != 0);

                if (isThisCorrect) {
                    answerButtons[c].setRevealColors(correctBg, whiteText);
                    scalingButtons.add(answerButtons[c]);
                } else if (isThisSelected) {
                    answerButtons[c].setRevealColors(wrongBg, whiteText);
                    scalingButtons.add(answerButtons[c]);
                }
            }

            if (isAllCorrect) {
                SoundManager.playCorrect();
            } else {
                SoundManager.playWrong();
            }

            final long animStart = System.currentTimeMillis();
            final int growDuration = 400;
            final int totalDuration = 850;

            revealTimer = new Timer(16, e -> {
                if (hasLeft) {
                    revealTimer.stop();
                    isRevealing = false;
                    return;
                }
                long elapsed = System.currentTimeMillis() - animStart;
                if (elapsed < growDuration) {
                    float p = (float) elapsed / growDuration;
                    float scale = 1.0f + 0.08f * (float) Math.sin(p * Math.PI / 2.0);
                    for (ModernButton b : scalingButtons) b.setAnimScale(scale);
                    answersGrid.repaint();
                } else if (elapsed < totalDuration) {
                    for (ModernButton b : scalingButtons) b.setAnimScale(1.08f);
                    answersGrid.repaint();
                } else {
                    revealTimer.stop();
                    isRevealing = false;
                    for (ModernButton b : answerButtons) b.resetReveal();
                    answersGrid.repaint();

                    if (hasLeft) return;
                    if (currentQuestionIndex + 1 < questions.size()) {
                        navigateToQuestion(currentQuestionIndex + 1);
                    } else {
                        doSubmit();
                    }
                }
            });
            revealTimer.start();
            return;
        }

        // 4. Dạng câu hỏi Trắc nghiệm 1 đáp án hoặc Đúng/Sai
        final ModernButton correctBtn;
        final ModernButton userBtn;
        final Container parentContainer;

        if (q.isTrueFalse()) {
            correctBtn = (correctChoice == 0) ? btnTrue : ((correctChoice == 1) ? btnFalse : null);
            userBtn = (userChoice == 0) ? btnTrue : ((userChoice == 1) ? btnFalse : null);
            parentContainer = tfPanel;
        } else {
            correctBtn = (correctChoice >= 0 && correctChoice < 4) ? answerButtons[correctChoice] : null;
            userBtn = (userChoice >= 0 && userChoice < 4) ? answerButtons[userChoice] : null;
            parentContainer = answersGrid;
        }

        boolean isCorrect = (userChoice != -1 && userChoice == correctChoice);

        Color correctBg = new Color(0x16, 0xA3, 0x4A); // Xanh lá #16A34A
        Color wrongBg = new Color(0xDC, 0x26, 0x26);   // Đỏ #DC2626
        Color whiteText = Color.WHITE;

        if (isCorrect) {
            // Đúng: Nút đúng hiện màu xanh lá rực rỡ
            if (correctBtn != null) {
                correctBtn.setRevealColors(correctBg, whiteText);
            }
            SoundManager.playCorrect();
        } else {
            // Sai: Nút người chơi chọn hiện đỏ, nút đúng hiện xanh lá
            if (userBtn != null) {
                userBtn.setRevealColors(wrongBg, whiteText);
            }
            if (correctBtn != null) {
                correctBtn.setRevealColors(correctBg, whiteText);
            }
            SoundManager.playWrong();
        }

        // Animation phóng to vừa vặn không tràn biên:
        final long animStart = System.currentTimeMillis();
        final int growDuration = 400;
        final int totalDuration = 850;

        revealTimer = new Timer(16, e -> {
            if (hasLeft) {
                revealTimer.stop();
                isRevealing = false;
                return;
            }
            long elapsed = System.currentTimeMillis() - animStart;
            if (elapsed < growDuration) {
                float p = (float) elapsed / growDuration;
                float scale = 1.0f + 0.08f * (float) Math.sin(p * Math.PI / 2.0);
                if (correctBtn != null) {
                    correctBtn.setAnimScale(scale);
                }
                if (userBtn != null && userBtn != correctBtn) {
                    userBtn.setAnimScale(scale);
                }
                if (parentContainer != null) {
                    parentContainer.repaint();
                }
            } else if (elapsed < totalDuration) {
                if (correctBtn != null) {
                    correctBtn.setAnimScale(1.08f);
                }
                if (userBtn != null && userBtn != correctBtn) {
                    userBtn.setAnimScale(1.08f);
                }
                if (parentContainer != null) {
                    parentContainer.repaint();
                }
            } else {
                revealTimer.stop();
                isRevealing = false;

                if (correctBtn != null) {
                    correctBtn.resetReveal();
                }
                if (userBtn != null) {
                    userBtn.resetReveal();
                }
                for (ModernButton b : answerButtons) {
                    b.resetReveal();
                }
                btnTrue.resetReveal();
                btnFalse.resetReveal();
                if (parentContainer != null) {
                    parentContainer.repaint();
                }

                if (hasLeft) return;
                if (currentQuestionIndex + 1 < questions.size()) {
                    navigateToQuestion(currentQuestionIndex + 1);
                } else {
                    doSubmit();
                }
            }
        });
        revealTimer.start();
    }

    private void confirmSubmit() {
        if (isSubmitted || isRevealing) {
            return;
        }

        // Đồng bộ lựa chọn ở câu hiện tại (nhưng chưa khóa cứng câu hỏi nếu người chơi chọn làm tiếp)
        syncCurrentAnswerInput();
        updateNavState();

        boolean timerWasRunning = (questionCountdownTimer != null && questionCountdownTimer.isRunning());
        stopCountdownTimer();
        SoundManager.stopCountdown();

        Window owner = SwingUtilities.getWindowAncestor(this);
        activeSubmitDialog = new SubmitConfirmDialog(
                owner,
                questions,
                selectedAnswers,
                currentQuestionIndex);
        activeSubmitDialog.setVisible(true);

        SubmitConfirmDialog.Choice choice = activeSubmitDialog.getUserChoice();
        int targetIdx = activeSubmitDialog.getTargetQuestionIndex();
        activeSubmitDialog = null;

        if (choice == SubmitConfirmDialog.Choice.SUBMIT) {
            doSubmit();
        } else if (choice == SubmitConfirmDialog.Choice.NAVIGATE) {
            int totalQ = (questions != null && !questions.isEmpty()) ? questions.size() : 10;
            if (targetIdx >= 0 && targetIdx < totalQ && targetIdx != currentQuestionIndex) {
                navigateToQuestion(targetIdx);
            } else if (timerWasRunning && !isSubmitted && !questionFinalized[currentQuestionIndex] && currentQuestionTimeRemaining > 0) {
                questionCountdownTimer.start();
            }
        } else if (timerWasRunning && !isSubmitted && !questionFinalized[currentQuestionIndex] && currentQuestionTimeRemaining > 0) {
            questionCountdownTimer.start();
        }
    }

    private void doSubmit() {
        if (hasLeft || isSubmitted) {
            return;
        }
        if (activeSubmitDialog != null && activeSubmitDialog.isVisible()) {
            activeSubmitDialog.dispose();
            activeSubmitDialog = null;
        }
        syncCurrentAnswerInput();
        for (int k = 0; k < 10; k++) {
            if (questions != null && k < questions.size()) {
                MatchQuestion q = questions.get(k);
                if (q.isDragAndDrop() && dndPanels[k] != null && !questionFinalized[k]) {
                    if (dndPanels[k].isAllCorrect()) {
                        selectedAnswers[k] = 1;
                    } else if (dndPanels[k].hasAnyAnswer()) {
                        selectedAnswers[k] = 0;
                    }
                }
            }
            if (!questionFinalized[k] && selectedAnswers[k] != -1) {
                questionFinalized[k] = true;
                if (!hasLeft && onAnswerListener != null) {
                    onAnswerListener.accept(k, selectedAnswers[k]);
                }
            }
        }
        isSubmitted = true;
        stopTimer();

        for (ModernButton b : answerButtons) {
            b.setEnabled(false);
        }
        btnTrue.setEnabled(false);
        btnFalse.setEnabled(false);
        txtFillInInput.setEnabled(false);
        if (currentDragDropPanel != null) {
            currentDragDropPanel.setInteractive(false);
        }
        btnSubmit.setEnabled(false);
        btnNext.setEnabled(false);

        waitingBanner.setVisible(true);
        revalidate();
        repaint();

        if (!hasLeft && onSubmitListener != null) {
            onSubmitListener.accept(selectedAnswers.clone());
        }
    }

    public void onSubmittedAcknowledged() {
        if (hasLeft) return;
        isSubmitted = true;
        if (activeSubmitDialog != null && activeSubmitDialog.isVisible()) {
            activeSubmitDialog.dispose();
            activeSubmitDialog = null;
        }
        waitingBanner.setVisible(true);
        lblWaitingStatus.setText("ĐÃ GỬI BÀI THÀNH CÔNG! ĐANG CHỜ ĐỐI THỦ HOÀN THÀNH...");
        revalidate();
        repaint();
    }

    public void onMatchFinished() {
        if (hasLeft) return;
        syncCurrentAnswerInput();
        isSubmitted = true;
        stopTimer();
        waitingBanner.setVisible(true);
        waitingBanner.setBackground(new Color(0x16, 0xA3, 0x4A, 220));
        lblWaitingStatus.setText("TRẬN ĐẤU ĐÃ KẾT THÚC!");
        revalidate();
        repaint();
    }

    private void confirmLeave() {
        boolean timerWasRunning = (questionCountdownTimer != null && questionCountdownTimer.isRunning());
        stopCountdownTimer();
        SoundManager.stopCountdown();

        int choice = JOptionPane.showConfirmDialog(
                this,
                "Bạn có chắc chắn muốn rời trận đấu?\n"
                        + "Thoát giữa trận sẽ bị xử THUA và bị trừ điểm xếp hạng!",
                "Cảnh Báo Thoát Trận",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (choice == JOptionPane.YES_OPTION) {
            cleanupAndStop();
            if (onLeaveListener != null) {
                onLeaveListener.run();
            }
        } else if (timerWasRunning && !hasLeft && !isSubmitted && !questionFinalized[currentQuestionIndex] && currentQuestionTimeRemaining > 0) {
            questionCountdownTimer.start();
            if (currentQuestionTimeRemaining <= 5) {
                SoundManager.playCountdown(currentQuestionIndex);
            }
        }
    }

    /**
     * Dừng toàn bộ timer, countdown, animation, vô hiệu hóa callback và giải phóng tài nguyên khi rời trận
     */
    public void cleanupAndStop() {
        hasLeft = true;
        isSubmitted = true;
        stopTimer();

        if (rhythmOverlayPanel != null) {
            rhythmOverlayPanel.cleanup();
        }

        if (activeSubmitDialog != null && activeSubmitDialog.isVisible()) {
            activeSubmitDialog.dispose();
            activeSubmitDialog = null;
        }
        if (currentMyBubble != null) {
            currentMyBubble.dismiss();
            currentMyBubble = null;
        }
        if (currentOpponentBubble != null) {
            currentOpponentBubble.dismiss();
            currentOpponentBubble = null;
        }

        for (ModernButton b : answerButtons) {
            if (b != null) b.setEnabled(false);
        }
        if (btnTrue != null) btnTrue.setEnabled(false);
        if (btnFalse != null) btnFalse.setEnabled(false);
        if (txtFillInInput != null) txtFillInInput.setEnabled(false);
        if (currentDragDropPanel != null) currentDragDropPanel.setInteractive(false);
        if (btnSubmit != null) btnSubmit.setEnabled(false);
        if (btnNext != null) btnNext.setEnabled(false);
        if (btnLeave != null) btnLeave.setEnabled(false);

        onAnswerListener = null;
        onQuestionChangeListener = null;
        onEmoteListener = null;
    }

    public void stopTimer() {
        if (activeSubmitDialog != null && activeSubmitDialog.isVisible()) {
            activeSubmitDialog.dispose();
            activeSubmitDialog = null;
        }
        stopCountdownTimer();
        SoundManager.stopCountdown();
        if (animTimer != null && animTimer.isRunning()) {
            animTimer.stop();
        }
        if (revealTimer != null && revealTimer.isRunning()) {
            revealTimer.stop();
        }
        if (myScoreBadge != null) {
            myScoreBadge.stopTimer();
        }
        if (oppScoreBadge != null) {
            oppScoreBadge.stopTimer();
        }
        isRevealing = false;
    }

    public String getMatchId() {
        return matchId;
    }

    public String getMyAvatarId() {
        return myAvatarId;
    }

    public String getOpponentAvatarId() {
        return opponentAvatarId;
    }

    public void setOnEmoteListener(Consumer<String> listener) {
        this.onEmoteListener = listener;
    }

    private void sendEmote(String emote) {
        SoundManager.playEmote();
        showBubble(myEmoteContainer, emote, true);
        if (onEmoteListener != null) {
            onEmoteListener.accept(emote);
        }
    }

    public void showOpponentEmote(String from, String emote) {
        SoundManager.playEmote();
        showBubble(opponentEmoteContainer, emote, false);
    }

    private void showBubble(JPanel container, String emoji, boolean isMe) {
        if (container == null) return;
        if (isMe && currentMyBubble != null) { currentMyBubble.dismiss(); }
        if (!isMe && currentOpponentBubble != null) { currentOpponentBubble.dismiss(); }

        EmoteBubble bubble = new EmoteBubble(emoji);
        container.removeAll();
        container.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
        container.add(bubble);
        container.revalidate();
        container.repaint();

        if (isMe) currentMyBubble = bubble;
        else currentOpponentBubble = bubble;

        bubble.show(() -> {
            container.removeAll();
            container.revalidate();
            container.repaint();
            if (isMe) currentMyBubble = null;
            else currentOpponentBubble = null;
        });
    }

    public List<MatchQuestion> getQuestions() {
        return questions;
    }

    public int[] getSelectedAnswers() {
        return selectedAnswers.clone();
    }

    public String[] getUserFillInAnswers() {
        return userFillInAnswers.clone();
    }

    // ==========================================
    // CUSTOM HELPER COMPONENTS
    // ==========================================

    /**
     * SmoothTimerBanner — Thanh thời gian trực quan phong cách Kahoot!/Quizizz
     * chuyển màu mượt mà từ Xanh lục -> Vàng hổ phách -> Đỏ hồng mà không nhấp nháy gây mỏi mắt.
     */
    private static class SmoothTimerBanner extends JPanel {
        private int remaining;
        private int maxSeconds;
        private boolean finalized = false;

        SmoothTimerBanner(int maxSeconds) {
            this.maxSeconds = Math.max(1, maxSeconds);
            this.remaining = this.maxSeconds;
            setOpaque(false);
            setPreferredSize(new Dimension(0, 8));
        }

        void updateTime(int remaining, int maxSeconds, boolean finalized) {
            this.remaining = Math.max(0, remaining);
            this.maxSeconds = Math.max(1, maxSeconds);
            this.finalized = finalized;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.setupAntiAliasing(g2);
            int w = getWidth();
            int h = getHeight();

            // Track background
            g2.setColor(new Color(0x0F, 0x17, 0x2A, 190));
            g2.fill(new RoundRectangle2D.Float(0, 0, w, h, h, h));

            if (finalized) {
                g2.setColor(new Color(0x22, 0xC5, 0x5E, 140));
                g2.fill(new RoundRectangle2D.Float(0, 0, w, h, h, h));
                g2.dispose();
                return;
            }

            float ratio = Math.max(0.0f, Math.min(1.0f, (float) remaining / maxSeconds));
            int fillW = Math.round(w * ratio);
            if (fillW > 0) {
                Color c1;
                Color c2;
                if (ratio <= 0.25f || remaining <= 3) {
                    c1 = new Color(0xE1, 0x1D, 0x48);
                    c2 = new Color(0xFB, 0x71, 0x85);
                } else if (ratio <= 0.52f) {
                    c1 = new Color(0xD9, 0x77, 0x06);
                    c2 = new Color(0xFB, 0xBF, 0x24);
                } else {
                    c1 = new Color(0x10, 0xB9, 0x81);
                    c2 = new Color(0x34, 0xD3, 0x99);
                }
                g2.setPaint(new GradientPaint(0, 0, c1, fillW, 0, c2));
                g2.fill(new RoundRectangle2D.Float(0, 0, fillW, h, h, h));
            }
            g2.dispose();
        }
    }

    /**
     * ScoreBadge — Huy hiệu điểm số & chuỗi trả lời đúng (Streak) thời gian thực
     * với hiệu ứng phóng to (pulse) và huy hiệu lửa 🔥 khi đạt chuỗi Streak >= 2.
     */
    private static class ScoreBadge extends JPanel {
        private int score = 0;
        private int points = 0;
        private int streak = 0;
        private final boolean isMe;
        private float pulseScale = 1.0f;
        private Timer pulseTimer;

        ScoreBadge(boolean isMe) {
            this.isMe = isMe;
            setOpaque(false);
            setPreferredSize(new Dimension(132, 28));
        }

        public void setScore(int newScore) {
            setStats(newScore, newScore * 100, this.streak);
        }

        public void setStats(int newScore, int newPoints, int newStreak) {
            boolean increased = (newScore > this.score) || (newPoints > this.points);
            this.score = newScore;
            this.points = newPoints;
            this.streak = newStreak;
            if (increased) {
                triggerPulse();
            } else {
                repaint();
            }
        }

        public int getScore() {
            return score;
        }

        public void stopTimer() {
            if (pulseTimer != null && pulseTimer.isRunning()) {
                pulseTimer.stop();
            }
            pulseScale = 1.0f;
        }

        private void triggerPulse() {
            if (pulseTimer != null && pulseTimer.isRunning()) {
                pulseTimer.stop();
            }
            final long startTime = System.currentTimeMillis();
            pulseTimer = new Timer(16, e -> {
                long elapsed = System.currentTimeMillis() - startTime;
                if (elapsed >= 300) {
                    pulseScale = 1.0f;
                    pulseTimer.stop();
                    repaint();
                } else {
                    float p = elapsed / 300.0f;
                    pulseScale = 1.0f + 0.28f * (float) Math.sin(p * Math.PI);
                    repaint();
                }
            });
            pulseTimer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.setupAntiAliasing(g2);

            int w = getWidth();
            int h = getHeight();

            if (pulseScale != 1.0f) {
                int cx = w / 2;
                int cy = h / 2;
                g2.translate(cx, cy);
                g2.scale(pulseScale, pulseScale);
                g2.translate(-cx, -cy);
            }

            Color bg = isMe ? new Color(0x13, 0x68, 0xCE, 95) : new Color(0x86, 0x4C, 0xBF, 95);
            Color border = isMe ? new Color(0x38, 0xBD, 0xF8) : new Color(0xC0, 0x84, 0xFC);
            if (pulseScale > 1.05f || streak >= 3) {
                bg = new Color(0xD9, 0x77, 0x06, 175);
                border = new Color(0xFB, 0xBF, 0x24);
            } else if (streak == 2) {
                border = new Color(0xFB, 0x92, 0x3C);
            }

            g2.setColor(bg);
            g2.fill(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, 14, 14));

            g2.setColor(border);
            g2.setStroke(new BasicStroke(1.5f));
            g2.draw(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, 14, 14));

            String scoreText = points + " đ (" + score + " đúng)";
            g2.setFont(Theme.SMALL_BOLD);
            FontMetrics fm = g2.getFontMetrics();

            boolean hasStreak = (streak >= 2);
            String streakNum = hasStreak ? "x" + streak : "";
            int flameW = hasStreak ? 12 : 0;
            int gap = hasStreak ? 6 : 0;
            int streakNumW = hasStreak ? fm.stringWidth(streakNum) : 0;
            int totalContentW = fm.stringWidth(scoreText) + (hasStreak ? (gap + flameW + 3 + streakNumW) : 0);

            int tx = Math.max(4, (w - totalContentW) / 2);
            int ty = (h - fm.getHeight()) / 2 + fm.getAscent();

            g2.setColor(Theme.WHITE);
            g2.drawString(scoreText, tx, ty);

            if (hasStreak) {
                int fx = tx + fm.stringWidth(scoreText) + gap;
                int fy = (h - 14) / 2;
                drawVectorFlame(g2, fx, fy, 11, 14);

                g2.setFont(Theme.SMALL_BOLD);
                g2.setColor(new Color(0xFD, 0xE0, 0x47));
                g2.drawString(streakNum, fx + flameW + 2, ty);
            }

            g2.dispose();
        }

        private void drawVectorFlame(Graphics2D g2, int x, int y, int w, int h) {
            Graphics2D g = (Graphics2D) g2.create();
            Theme.setupAntiAliasing(g);

            // Outer flame (Orange to Red gradient)
            java.awt.geom.Path2D.Float outer = new java.awt.geom.Path2D.Float();
            outer.moveTo(x + w * 0.5f, y);
            outer.curveTo(x + w * 0.85f, y + h * 0.35f, x + w, y + h * 0.7f, x + w * 0.75f, y + h);
            outer.curveTo(x + w * 0.55f, y + h * 1.05f, x + w * 0.25f, y + h * 1.02f, x + w * 0.15f, y + h * 0.8f);
            outer.curveTo(x - w * 0.05f, y + h * 0.5f, x + w * 0.2f, y + h * 0.3f, x + w * 0.5f, y);
            outer.closePath();

            g.setPaint(new GradientPaint(x, y, new Color(0xFB, 0x92, 0x3C), x, y + h, new Color(0xEF, 0x44, 0x44)));
            g.fill(outer);

            // Inner core flame (Warm Golden Yellow)
            java.awt.geom.Path2D.Float inner = new java.awt.geom.Path2D.Float();
            float ix = x + w * 0.25f;
            float iy = y + h * 0.45f;
            float iw = w * 0.5f;
            float ih = h * 0.45f;
            inner.moveTo(ix + iw * 0.5f, iy);
            inner.curveTo(ix + iw * 0.95f, iy + ih * 0.35f, ix + iw * 0.8f, iy + ih, ix + iw * 0.5f, iy + ih);
            inner.curveTo(ix + iw * 0.2f, iy + ih, ix, iy + ih * 0.35f, ix + iw * 0.5f, iy);
            inner.closePath();

            g.setColor(new Color(0xFE, 0xF0, 0x8A));
            g.fill(inner);
            g.dispose();
        }
    }

    /**
     * AnimatedQuestionCard — Card câu hỏi hỗ trợ biến đổi scale, vị trí Y và alpha
     * phục vụ hiệu ứng chuyển câu hỏi nhẹ nhàng.
     */
    private static class AnimatedQuestionCard extends ModernCard {
        private float animScale = 1.0f;
        private int animOffsetY = 0;
        private float animAlpha = 1.0f;

        AnimatedQuestionCard(LayoutManager layout) {
            super(layout);
        }

        public void setAnimState(float scale, int offsetY, float alpha) {
            this.animScale = scale;
            this.animOffsetY = offsetY;
            this.animAlpha = Math.max(0.0f, Math.min(1.0f, alpha));
            repaint();
        }

        @Override
        public void paint(Graphics g) {
            if (animAlpha <= 0.001f) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.setupAntiAliasing(g2);
            if (animAlpha < 1.0f) {
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, animAlpha));
            }
            if (animOffsetY != 0 || animScale != 1.0f) {
                int cx = getWidth() / 2;
                int cy = getHeight() / 2;
                g2.translate(cx, cy + animOffsetY);
                g2.scale(animScale, animScale);
                g2.translate(-cx, -cy);
            }
            super.paint(g2);
            g2.dispose();
        }
    }

    /**
     * ModernFillInField — Ô nhập văn bản dạng bo góc ở giữa màn hình:
     * - Khi chưa nhấp chuột vào: Hiển thị dòng placeholder mờ "Nhập đáp án".
     * - Khi nhấp chuột vào (focus): Placeholder biến mất, xuất hiện con trỏ soạn thảo nhấp nháy.
     * - Hỗ trợ hoạt ảnh co giãn (animScale) khi xuất hiện câu hỏi và khi hiển thị đúng/sai (reveal).
     * - Hỗ trợ đổi màu viền/nền khi hiển thị kết quả (xanh lá nếu đúng, đỏ nếu sai).
     */
    public static class ModernFillInField extends JTextField {
        private final String placeholder;
        private float animScale = 1.0f;
        private Color revealBg = null;
        private Color revealBorder = null;

        public ModernFillInField(String placeholder) {
            super();
            this.placeholder = placeholder != null ? placeholder : "Nhập đáp án";
            setFont(Theme.TITLE);
            setForeground(Theme.WHITE);
            setCaretColor(new Color(0x38, 0xBD, 0xF8)); // Bright Cyan cursor
            setHorizontalAlignment(JTextField.CENTER);
            setOpaque(false);
            setBorder(new EmptyBorder(12, 24, 12, 24));

            addFocusListener(new java.awt.event.FocusListener() {
                @Override
                public void focusGained(java.awt.event.FocusEvent e) {
                    repaint();
                }

                @Override
                public void focusLost(java.awt.event.FocusEvent e) {
                    repaint();
                }
            });
        }

        public void setAnimScale(float scale) {
            this.animScale = Math.max(0.0f, scale);
            repaint();
        }

        public void setRevealColors(Color bg, Color border) {
            this.revealBg = bg;
            this.revealBorder = border;
            repaint();
        }

        public void resetReveal() {
            this.revealBg = null;
            this.revealBorder = null;
            this.animScale = 1.0f;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.setupAntiAliasing(g2);

            int w = getWidth();
            int h = getHeight();

            if (animScale != 1.0f && animScale > 0.001f) {
                int cx = w / 2;
                int cy = h / 2;
                g2.translate(cx, cy);
                g2.scale(animScale, animScale);
                g2.translate(-cx, -cy);
            }

            // 1. Vẽ nền ô vuông bo góc
            Color bg;
            Color border;
            if (revealBg != null) {
                bg = revealBg;
                border = (revealBorder != null) ? revealBorder : Theme.WHITE;
            } else if (isFocusOwner()) {
                bg = new Color(0x1F, 0x2E, 0x4D); // Navy highlight
                border = new Color(0x38, 0xBD, 0xF8); // Bright Cyan
            } else {
                bg = new Color(0x18, 0x22, 0x38, 220); // Deep card navy
                border = Theme.BORDER_DARK;
            }

            g2.setColor(bg);
            g2.fill(new RoundRectangle2D.Float(2, 2, w - 4, h - 4, 18, 18));

            // 2. Vẽ nội dung văn bản và con trỏ chuột
            super.paintComponent(g2);

            // 3. Hiển thị chữ "Nhập đáp án" mờ khi chưa focus và chưa có chữ
            if (getText().isEmpty() && !isFocusOwner()) {
                g2.setColor(new Color(0x94, 0xA3, 0xB8, 150));
                g2.setFont(Theme.TITLE.deriveFont(Font.ITALIC));
                FontMetrics fm = g2.getFontMetrics();
                int tx = (w - fm.stringWidth(placeholder)) / 2;
                int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(placeholder, tx, ty);
            }

            // 4. Vẽ viền bo góc
            g2.setColor(border);
            g2.setStroke(new BasicStroke(isFocusOwner() || revealBg != null ? 2.5f : 1.5f));
            g2.draw(new RoundRectangle2D.Float(2, 2, w - 4, h - 4, 18, 18));

            g2.dispose();
        }

        @Override
        protected void paintBorder(Graphics g) {
            // Đã được xử lý vẽ viền bo góc mượt mà trong paintComponent
        }
    }

    /**
     * isFillInCorrect — Chấm điểm câu hỏi điền từ bằng thuật toán Fuzzy Matching (>= 80% tương đồng):
     * - Bỏ qua viết hoa/thường, khoảng trắng thừa ở đầu/cuối và giữa từ.
     * - Chấp nhận đáp án có dấu hoặc không dấu tiếng Việt.
     * - Chấp nhận sai sót nhỏ về chính tả/ký tự (Levenshtein distance similarity >= 0.80).
     */
    public static boolean isFillInCorrect(String userInput, String[] acceptableChoices) {
        if (userInput == null || userInput.trim().isEmpty()) {
            return false;
        }
        if (acceptableChoices == null || acceptableChoices.length == 0) {
            return false;
        }

        String normUser = normalizeText(userInput);
        String noAccentUser = removeAccents(normUser);

        for (String target : acceptableChoices) {
            if (target == null || target.trim().isEmpty()) {
                continue;
            }
            String normTarget = normalizeText(target);
            String noAccentTarget = removeAccents(normTarget);

            // 1. So khớp chính xác (có dấu hoặc không dấu)
            if (normUser.equals(normTarget) || noAccentUser.equals(noAccentTarget)) {
                return true;
            }

            // 2. So khớp mờ (Fuzzy matching Levenshtein >= 80%)
            double sim1 = calculateSimilarity(normUser, normTarget);
            double sim2 = calculateSimilarity(noAccentUser, noAccentTarget);
            if (Math.max(sim1, sim2) >= 0.80) {
                return true;
            }
        }
        return false;
    }

    public static double calculateSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) return 0.0;
        if (s1.equals(s2)) return 1.0;
        int len1 = s1.length();
        int len2 = s2.length();
        if (len1 == 0 || len2 == 0) return 0.0;

        int[][] dp = new int[len1 + 1][len2 + 1];
        for (int i = 0; i <= len1; i++) dp[i][0] = i;
        for (int j = 0; j <= len2; j++) dp[0][j] = j;

        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost
                );
            }
        }
        int dist = dp[len1][len2];
        int maxLen = Math.max(len1, len2);
        return 1.0 - ((double) dist / (double) maxLen);
    }

    public static String normalizeText(String s) {
        if (s == null) return "";
        return s.trim().toLowerCase().replaceAll("\\s+", " ");
    }

    public static String removeAccents(String s) {
        if (s == null) return "";
        String normalized = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD);
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String result = pattern.matcher(normalized).replaceAll("");
        return result.replace('đ', 'd').replace('Đ', 'd');
    }

    // ==========================================
    // SYNCHRONIZED MATCH RHYTHM CONTROLLERS
    // ==========================================
    public void handleMatchPhase(String[] a) {
        if (hasLeft || a == null || a.length < 2) return;
        this.serverDrivenRhythm = true;
        String phase = Wire.field(a, 1);
        SwingUtilities.invokeLater(() -> {
            switch (phase) {
                case "READY_CHECK" -> {
                    int timeoutSec = 10;
                    try { timeoutSec = Integer.parseInt(Wire.field(a, 3)); } catch (Exception ignored) {}
                    boolean myReady = Boolean.parseBoolean(Wire.field(a, 4));
                    boolean oppReady = Boolean.parseBoolean(Wire.field(a, 5));
                    showReadyCheckPhase(timeoutSec, myReady, oppReady);
                }
                case "COUNTDOWN" -> {
                    String stepStr = Wire.field(a, 3);
                    showCountdownPhase(stepStr);
                }
                case "QUESTION_INTRO" -> {
                    int qIdx = 0;
                    try { qIdx = Integer.parseInt(Wire.field(a, 3)); } catch (Exception ignored) {}
                    String topicStr = Wire.field(a, 4);
                    String qTypeStr = Wire.field(a, 5);
                    int durMs = 800;
                    try { durMs = Integer.parseInt(Wire.field(a, 6)); } catch (Exception ignored) {}
                    showQuestionIntroPhase(qIdx, topicStr, qTypeStr, durMs);
                }
                case "QUESTION_ACTIVE" -> {
                    int qIdx = 0;
                    try { qIdx = Integer.parseInt(Wire.field(a, 3)); } catch (Exception ignored) {}
                    int sec = perQuestionSeconds;
                    try { sec = Integer.parseInt(Wire.field(a, 4)); } catch (Exception ignored) {}
                    startQuestionActivePhase(qIdx, sec);
                }
                case "QUESTION_REVEAL" -> {
                    int qIdx = 0;
                    try { qIdx = Integer.parseInt(Wire.field(a, 3)); } catch (Exception ignored) {}
                    int correctAns = -1;
                    try { correctAns = Integer.parseInt(Wire.field(a, 4)); } catch (Exception ignored) {}
                    String exp = Wire.field(a, 5);
                    int myPts = 0, oppPts = 0, myStrk = 0, oppStrk = 0, myAns = -1, oppAns = -1;
                    try { myPts = Integer.parseInt(Wire.field(a, 6)); } catch (Exception ignored) {}
                    try { oppPts = Integer.parseInt(Wire.field(a, 7)); } catch (Exception ignored) {}
                    try { myStrk = Integer.parseInt(Wire.field(a, 8)); } catch (Exception ignored) {}
                    try { oppStrk = Integer.parseInt(Wire.field(a, 9)); } catch (Exception ignored) {}
                    try { myAns = Integer.parseInt(Wire.field(a, 10)); } catch (Exception ignored) {}
                    try { oppAns = Integer.parseInt(Wire.field(a, 11)); } catch (Exception ignored) {}
                    showQuestionRevealPhase(qIdx, correctAns, exp, myPts, oppPts, myStrk, oppStrk, myAns, oppAns);
                }
            }
        });
    }

    public void updateReadyStatus(String p1, boolean r1, String p2, boolean r2) {
        if (hasLeft) return;
        boolean myR = (myName != null && myName.equals(p1)) ? r1 : ((myName != null && myName.equals(p2)) ? r2 : false);
        boolean oppR = (opponentName != null && opponentName.equals(p1)) ? r1 : ((opponentName != null && opponentName.equals(p2)) ? r2 : false);
        if (rhythmOverlayPanel != null) {
            rhythmOverlayPanel.updateReadyStatus(myR, oppR);
        }
    }

    public void showReadyCheckPhase(int timeoutSec, boolean myReady, boolean oppReady) {
        if (hasLeft) return;
        this.serverDrivenRhythm = true;
        if (rhythmOverlayPanel != null) {
            rhythmOverlayPanel.showReadyCheck(timeoutSec, myReady, oppReady);
        }
    }

    public void showCountdownPhase(String stepStr) {
        if (hasLeft) return;
        this.serverDrivenRhythm = true;
        if (rhythmOverlayPanel != null) {
            rhythmOverlayPanel.showCountdown(stepStr);
        }
    }

    public void showQuestionIntroPhase(int qIdx, String topicStr, String qTypeStr, int durMs) {
        if (hasLeft) return;
        this.serverDrivenRhythm = true;
        stopCountdownTimer();
        SoundManager.stopCountdown();
        if (animTimer != null && animTimer.isRunning()) {
            animTimer.stop();
        }
        if (revealTimer != null && revealTimer.isRunning()) {
            revealTimer.stop();
        }
        isRevealing = false;
        if (lblFillInCorrectAnswer != null) {
            lblFillInCorrectAnswer.setVisible(false);
        }
        for (ModernButton b : answerButtons) {
            if (b != null) b.resetReveal();
        }
        if (btnTrue != null) btnTrue.resetReveal();
        if (btnFalse != null) btnFalse.resetReveal();
        if (txtFillInInput != null) txtFillInInput.resetReveal();

        if (qIdx >= 0 && questions != null && qIdx < questions.size()) {
            currentQuestionIndex = qIdx;
        }

        if (rhythmOverlayPanel != null) {
            rhythmOverlayPanel.showQuestionIntro(qIdx, topicStr, qTypeStr);
        }
    }

    public void startQuestionActivePhase(int qIdx, int durationSec) {
        if (hasLeft) return;
        this.serverDrivenRhythm = true;
        stopCountdownTimer();
        SoundManager.stopCountdown();
        if (animTimer != null && animTimer.isRunning()) {
            animTimer.stop();
        }
        if (revealTimer != null && revealTimer.isRunning()) {
            revealTimer.stop();
        }
        if (rhythmOverlayPanel != null) {
            rhythmOverlayPanel.hideOverlay();
        }
        isRevealing = false;
        if (lblFillInCorrectAnswer != null) {
            lblFillInCorrectAnswer.setVisible(false);
        }
        if (qIdx >= 0 && qIdx < questionTimeLeft.length) {
            questionTimeLeft[qIdx] = Math.max(5, durationSec);
        }
        this.currentQuestionTimeRemaining = Math.max(5, durationSec);

        if (qIdx >= 0 && questions != null && qIdx < questions.size()) {
            currentQuestionIndex = qIdx;
            startQuestionEntrance();
        }
    }

    public void showQuestionRevealPhase(int qIdx, int correctAns, String exp,
                                        int myPts, int oppPts, int myStrk, int oppStrk,
                                        int myAns, int oppAns) {
        if (hasLeft) return;
        this.serverDrivenRhythm = true;
        stopCountdownTimer();
        SoundManager.stopCountdown();
        if (animTimer != null && animTimer.isRunning()) {
            animTimer.stop();
        }
        if (revealTimer != null && revealTimer.isRunning()) {
            revealTimer.stop();
        }
        if (rhythmOverlayPanel != null) {
            rhythmOverlayPanel.hideOverlay();
        }
        currentQuestionIndex = qIdx;
        isRevealing = true;
        stopCountdownTimer();
        SoundManager.stopCountdown();

        if (qIdx >= 0 && qIdx < selectedAnswers.length) {
            if (myAns != -1) {
                selectedAnswers[qIdx] = myAns;
            }
            questionFinalized[qIdx] = true;
        }

        // 1. Highlight visual colors on buttons
        MatchQuestion q = (questions != null && qIdx < questions.size()) ? questions.get(qIdx) : null;
        if (q != null) {
            Color correctBg = new Color(0x16, 0xA3, 0x4A); // Green
            Color wrongBg = new Color(0xDC, 0x26, 0x26);   // Red
            Color whiteText = Color.WHITE;

            int actualMyAns = (myAns != -1) ? myAns : selectedAnswers[qIdx];

            if (q.isTrueFalse()) {
                if (correctAns == 0) {
                    btnTrue.setRevealColors(correctBg, whiteText);
                    if (actualMyAns == 1) btnFalse.setRevealColors(wrongBg, whiteText);
                } else if (correctAns == 1) {
                    btnFalse.setRevealColors(correctBg, whiteText);
                    if (actualMyAns == 0) btnTrue.setRevealColors(wrongBg, whiteText);
                }
            } else if (q.isMultipleSelect()) {
                for (int c = 0; c < 4; c++) {
                    boolean isThisCorrect = (correctAns & (1 << c)) != 0;
                    boolean isThisSelected = (actualMyAns != -1) && ((actualMyAns & (1 << c)) != 0);
                    if (isThisCorrect) {
                        answerButtons[c].setRevealColors(correctBg, whiteText);
                    } else if (isThisSelected) {
                        answerButtons[c].setRevealColors(wrongBg, whiteText);
                    }
                }
            } else if (q.isFillIn()) {
                boolean isCorrect = (actualMyAns == 1 || (actualMyAns == correctAns && correctAns != -1));
                txtFillInInput.setRevealColors(isCorrect ? correctBg : wrongBg, isCorrect ? new Color(0x86, 0xEF, 0xAC) : new Color(0xFE, 0xCA, 0xCA));
                if (!isCorrect && q.choices() != null && q.choices().length > 0) {
                    lblFillInCorrectAnswer.setText("Đáp án chính xác: " + q.choices()[0]);
                    lblFillInCorrectAnswer.setVisible(true);
                }
            } else if (q.isDragAndDrop()) {
                // Drag & drop handled
            } else {
                if (correctAns >= 0 && correctAns < 4) {
                    answerButtons[correctAns].setRevealColors(correctBg, whiteText);
                }
                if (actualMyAns >= 0 && actualMyAns < 4 && actualMyAns != correctAns) {
                    answerButtons[actualMyAns].setRevealColors(wrongBg, whiteText);
                }
            }

            // Audio
            boolean isUserCorrect = (actualMyAns != -1 && actualMyAns == correctAns);
            if (q.isFillIn() || q.isDragAndDrop()) {
                isUserCorrect = (actualMyAns == 1);
            }
            if (isUserCorrect) {
                SoundManager.playCorrect();
            } else {
                SoundManager.playWrong();
            }
        }

        // 2. Update real-time score & streak stats
        updateScores(this.myScore, this.opponentScore, myPts, oppPts, myStrk, oppStrk);

        // 3. Configure Next / Skip Reveal button
        if (btnNext != null) {
            btnNext.setText("Tiếp tục >>");
            btnNext.setEnabled(!isSubmitted);
            btnNext.setToolTipText("Bấm để sẵn sàng chuyển sang câu tiếp theo ngay lập tức");
        }
    }

    /**
     * RhythmOverlayPanel — Lớp phủ đồng bộ nhịp trận Client-Server:
     * 1. READY_CHECK: hiển thị card thông tin trận, trạng thái sẵn sàng của 2 bên, đếm ngược 10s, nút SẴN SÀNG
     * 2. COUNTDOWN: 3 - 2 - 1 - GO! với hiệu ứng phóng to thu nhỏ & âm thanh tick/matchFound
     * 3. QUESTION_INTRO: Splash card câu hỏi (0.8s) với số câu, chủ đề, dạng câu hỏi
     */
    private static class RhythmOverlayPanel extends JPanel {
        enum Phase { HIDDEN, READY_CHECK, COUNTDOWN, QUESTION_INTRO }

        private Phase phase = Phase.HIDDEN;
        private final MatchScreen matchScreen;

        // Ready Check
        private int readySeconds = 10;
        private boolean myReady = false;
        private boolean oppReady = false;
        private ModernButton btnReady;
        private JLabel lblTimeout;
        private Timer localReadyTimer;

        // Countdown
        private String countdownStep = "3";
        private float countdownScale = 1.0f;
        private Timer scaleAnimTimer;

        // Question Intro
        private int introIndex = 0;
        private String introTopic = "";
        private String introType = "";

        RhythmOverlayPanel(MatchScreen matchScreen) {
            this.matchScreen = matchScreen;
            setOpaque(false);
            setLayout(null);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    e.consume();
                }
            });
        }

        void showReadyCheck(int sec, boolean myR, boolean oppR) {
            this.phase = Phase.READY_CHECK;
            this.readySeconds = Math.max(1, sec);
            this.myReady = myR;
            this.oppReady = oppR;
            stopLocalReadyTimer();
            removeAll();

            btnReady = new ModernButton(myReady ? "ĐÃ SẴN SÀNG" : "BẤM ĐỂ SẴN SÀNG", ModernButton.Style.SUCCESS);
            btnReady.setFont(Theme.TITLE.deriveFont(16f));
            btnReady.setCornerRadius(16);
            btnReady.setEnabled(!myReady);
            btnReady.addActionListener(e -> {
                SoundManager.playClick();
                myReady = true;
                btnReady.setText("Đã sẵn sàng (Chờ đối thủ...)");
                btnReady.setEnabled(false);
                if (matchScreen.onReadyListener != null) {
                    matchScreen.onReadyListener.run();
                }
                updateReadyStatus(true, oppReady);
            });
            add(btnReady);

            lblTimeout = new JLabel("Tự động kết thúc sau: " + readySeconds + "s", SwingConstants.CENTER);
            lblTimeout.setFont(Theme.SMALL_BOLD);
            lblTimeout.setForeground(new Color(0x94, 0xA3, 0xB8));
            add(lblTimeout);

            setVisible(true);
            revalidate();
            repaint();

            localReadyTimer = new Timer(1000, e -> {
                readySeconds = Math.max(0, readySeconds - 1);
                if (lblTimeout != null) {
                    lblTimeout.setText("Tự động thoát sau: " + readySeconds + "s");
                }
                if (readySeconds <= 0) {
                    stopLocalReadyTimer();
                }
            });
            localReadyTimer.start();
        }

        void updateReadyStatus(boolean myR, boolean oppR) {
            this.myReady = myR;
            this.oppReady = oppR;
            if (btnReady != null && myReady) {
                btnReady.setText("ĐÃ SẴN SÀNG");
                btnReady.setEnabled(false);
            }
            repaint();
        }

        void showCountdown(String step) {
            stopLocalReadyTimer();
            this.phase = Phase.COUNTDOWN;
            this.countdownStep = (step != null) ? step : "3";
            removeAll();
            setVisible(true);

            if ("GO".equalsIgnoreCase(countdownStep) || "0".equals(countdownStep)) {
                this.countdownStep = "GO!";
                SoundManager.playMatchFound();
            } else {
                SoundManager.playTick();
            }

            if (scaleAnimTimer != null && scaleAnimTimer.isRunning()) {
                scaleAnimTimer.stop();
            }
            final long start = System.currentTimeMillis();
            countdownScale = 1.35f;
            scaleAnimTimer = new Timer(16, e -> {
                long el = System.currentTimeMillis() - start;
                if (el < 220) {
                    float p = (float) el / 220.0f;
                    countdownScale = 1.35f - 0.35f * p;
                    repaint();
                } else {
                    scaleAnimTimer.stop();
                    countdownScale = 1.0f;
                    repaint();
                }
            });
            scaleAnimTimer.start();
            repaint();
        }

        void showQuestionIntro(int qIdx, String topic, String qType) {
            stopLocalReadyTimer();
            if (scaleAnimTimer != null && scaleAnimTimer.isRunning()) scaleAnimTimer.stop();
            this.phase = Phase.QUESTION_INTRO;
            this.introIndex = qIdx;
            this.introTopic = (topic != null) ? topic : matchScreen.topic;
            this.introType = (qType != null) ? qType : "MULTIPLE_CHOICE";
            removeAll();
            setVisible(true);
            SoundManager.playPop();
            repaint();
        }

        void hideOverlay() {
            stopLocalReadyTimer();
            if (scaleAnimTimer != null && scaleAnimTimer.isRunning()) scaleAnimTimer.stop();
            this.phase = Phase.HIDDEN;
            removeAll();
            setVisible(false);
            repaint();
        }

        void cleanup() {
            stopLocalReadyTimer();
            if (scaleAnimTimer != null && scaleAnimTimer.isRunning()) scaleAnimTimer.stop();
        }

        private void stopLocalReadyTimer() {
            if (localReadyTimer != null && localReadyTimer.isRunning()) {
                localReadyTimer.stop();
            }
        }

        @Override
        public void doLayout() {
            super.doLayout();
            if (phase == Phase.READY_CHECK && btnReady != null && lblTimeout != null) {
                int w = getWidth();
                int h = getHeight();
                int cardW = Math.min(520, w - 40);
                int cardH = 310;
                int cx = (w - cardW) / 2;
                int cy = (h - cardH) / 2;
                int btnW = Math.min(280, cardW - 60);
                int btnH = 46;
                btnReady.setBounds(cx + (cardW - btnW) / 2, cy + 205, btnW, btnH);
                lblTimeout.setBounds(cx + 20, cy + 262, cardW - 40, 24);
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (phase == Phase.HIDDEN) return;
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.setupAntiAliasing(g2);
            int w = getWidth();
            int h = getHeight();

            // Dim backdrop
            g2.setColor(new Color(0x0A, 0x0F, 0x1E, 220));
            g2.fillRect(0, 0, w, h);

            if (phase == Phase.READY_CHECK) {
                paintReadyCheck(g2, w, h);
            } else if (phase == Phase.COUNTDOWN) {
                paintCountdown(g2, w, h);
            } else if (phase == Phase.QUESTION_INTRO) {
                paintQuestionIntro(g2, w, h);
            }
            g2.dispose();
        }

        private void paintReadyCheck(Graphics2D g2, int w, int h) {
            int cardW = Math.min(520, w - 40);
            int cardH = 310;
            int cx = (w - cardW) / 2;
            int cy = (h - cardH) / 2;

            // Card background & glowing border
            GradientPaint gp = new GradientPaint(cx, cy, new Color(0x13, 0x1E, 0x36), cx + cardW, cy + cardH, new Color(0x1E, 0x19, 0x38));
            g2.setPaint(gp);
            g2.fill(new RoundRectangle2D.Float(cx, cy, cardW, cardH, 22, 22));

            g2.setColor(new Color(0x38, 0xBD, 0xF8, 140));
            g2.setStroke(new BasicStroke(2.0f));
            g2.draw(new RoundRectangle2D.Float(cx + 1, cy + 1, cardW - 2, cardH - 2, 22, 22));

            // Title
            g2.setFont(Theme.TITLE.deriveFont(20f));
            g2.setColor(Theme.WHITE);
            String title = " CHUẨN BỊ TRANH TÀI";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(title, cx + (cardW - fm.stringWidth(title)) / 2, cy + 38);

            // Topic metadata pill
            g2.setFont(Theme.SMALL_BOLD);
            fm = g2.getFontMetrics();
            String meta = matchScreen.topic + " - " + matchScreen.level + " - 10 CÂU HỎI";
            int metaW = fm.stringWidth(meta) + 24;
            int metaH = 24;
            int metaX = cx + (cardW - metaW) / 2;
            int metaY = cy + 50;
            g2.setColor(new Color(0x3B, 0x82, 0xF6, 60));
            g2.fill(new RoundRectangle2D.Float(metaX, metaY, metaW, metaH, 12, 12));
            g2.setColor(new Color(0x60, 0xA5, 0xFA, 120));
            g2.draw(new RoundRectangle2D.Float(metaX + 0.5f, metaY + 0.5f, metaW - 1, metaH - 1, 12, 12));
            g2.setColor(new Color(0xBA, 0xE6, 0xFD));
            g2.drawString(meta, metaX + 12, metaY + (metaH - fm.getHeight()) / 2 + fm.getAscent());

            // Two player columns
            int colW = (cardW - 60) / 2;
            int colH = 92;
            int p1X = cx + 24;
            int p2X = cx + cardW - 24 - colW;
            int pY = cy + 92;

            paintPlayerStatus(g2, p1X, pY, colW, colH, matchScreen.myName, matchScreen.myAvatarId, myReady, true);
            paintPlayerStatus(g2, p2X, pY, colW, colH, matchScreen.opponentName, matchScreen.opponentAvatarId, oppReady, false);
        }

        private void paintPlayerStatus(Graphics2D g2, int x, int y, int w, int h, String name, String avatarId, boolean isReady, boolean isMe) {
            Color bg = isReady ? new Color(0x06, 0x4E, 0x3B, 180) : new Color(0x1E, 0x29, 0x3B, 180);
            Color border = isReady ? new Color(0x10, 0xB9, 0x81) : (isMe ? new Color(0x38, 0xBD, 0xF8, 120) : new Color(0xC0, 0x84, 0xFC, 120));

            g2.setColor(bg);
            g2.fill(new RoundRectangle2D.Float(x, y, w, h, 16, 16));
            g2.setColor(border);
            g2.setStroke(new BasicStroke(1.5f));
            g2.draw(new RoundRectangle2D.Float(x + 0.5f, y + 0.5f, w - 1, h - 1, 16, 16));

            // Avatar
            int avSz = 40;
            int avX = x + 12;
            int avY = y + (h - avSz) / 2;
            AvatarPicker.paintAvatar(g2, avatarId, avX, avY, avSz);
            g2.setColor(border);
            g2.setStroke(new BasicStroke(1.8f));
            g2.drawOval(avX, avY, avSz, avSz);

            // Name
            g2.setFont(Theme.BODY_BOLD);
            g2.setColor(Theme.WHITE);
            String rawName = isMe ? "Bạn (" + name + ")" : name;
            if (rawName.length() > 14) rawName = rawName.substring(0, 13) + "...";
            g2.drawString(rawName, avX + avSz + 10, avY + 17);

            // Ready Badge
            g2.setFont(Theme.SMALL_BOLD);
            if (isReady) {
                g2.setColor(new Color(0x34, 0xD3, 0x99));
                g2.drawString("ĐÃ SẴN SÀNG", avX + avSz + 10, avY + 35);
            } else {
                g2.setColor(isMe ? new Color(0x94, 0xA3, 0xB8) : new Color(0xFB, 0xBF, 0x24));
                g2.drawString(isMe ? "Chưa sẵn sàng" : "Đang chuẩn bị...", avX + avSz + 10, avY + 35);
            }
        }

        private void paintCountdown(Graphics2D g2, int w, int h) {
            int cx = w / 2;
            int cy = h / 2 - 15;
            int dia = 160;

            boolean isGo = "GO!".equalsIgnoreCase(countdownStep);
            Color ringColor = isGo ? new Color(0x10, 0xB9, 0x81) : new Color(0xF5, 0x9E, 0x0B);

            // Circle background
            g2.setColor(new Color(0x0F, 0x17, 0x2A, 235));
            g2.fillOval(cx - dia / 2, cy - dia / 2, dia, dia);

            // Glowing ring
            g2.setColor(ringColor);
            g2.setStroke(new BasicStroke(4.0f));
            g2.drawOval(cx - dia / 2 + 2, cy - dia / 2 + 2, dia - 4, dia - 4);

            // Scale transform for text
            Graphics2D gt = (Graphics2D) g2.create();
            gt.translate(cx, cy);
            gt.scale(countdownScale, countdownScale);
            gt.translate(-cx, -cy);

            gt.setFont(isGo ? new Font("Segoe UI", Font.BOLD, 58) : new Font("Segoe UI", Font.BOLD, 72));
            gt.setColor(ringColor);
            FontMetrics fm = gt.getFontMetrics();
            int tx = cx - fm.stringWidth(countdownStep) / 2;
            int ty = cy + (fm.getAscent() - fm.getDescent()) / 2;
            gt.drawString(countdownStep, tx, ty);
            gt.dispose();

            // Subtitle
            g2.setFont(Theme.BODY_BOLD);
            g2.setColor(new Color(0xE2, 0xE8, 0xF0));
            String sub = isGo ? "CHIẾN ĐẤU!" : "CHUẨN BỊ TRANH TÀI...";
            FontMetrics sfm = g2.getFontMetrics();
            g2.drawString(sub, cx - sfm.stringWidth(sub) / 2, cy + dia / 2 + 36);
        }

        private void paintQuestionIntro(Graphics2D g2, int w, int h) {
            int cardW = Math.min(480, w - 40);
            int cardH = 210;
            int cx = (w - cardW) / 2;
            int cy = (h - cardH) / 2;

            // Card background & purple border
            GradientPaint gp = new GradientPaint(cx, cy, new Color(0x16, 0x1B, 0x33), cx + cardW, cy + cardH, new Color(0x27, 0x1B, 0x3E));
            g2.setPaint(gp);
            g2.fill(new RoundRectangle2D.Float(cx, cy, cardW, cardH, 20, 20));

            g2.setColor(new Color(0xC0, 0x84, 0xFC, 180));
            g2.setStroke(new BasicStroke(2.0f));
            g2.draw(new RoundRectangle2D.Float(cx + 1, cy + 1, cardW - 2, cardH - 2, 20, 20));

            // Topic header
            g2.setFont(Theme.SMALL_BOLD);
            g2.setColor(new Color(0xC0, 0x84, 0xFC));
            FontMetrics fm = g2.getFontMetrics();
            String topPill = "CHỦ ĐỀ: " + introTopic.toUpperCase();
            g2.drawString(topPill, cx + (cardW - fm.stringWidth(topPill)) / 2, cy + 38);

            // Big Question Index
            g2.setFont(new Font("Segoe UI", Font.BOLD, 36));
            g2.setColor(Theme.WHITE);
            fm = g2.getFontMetrics();
            String qText = "CÂU HỎI " + (introIndex + 1) + " / " + (matchScreen.questions != null ? matchScreen.questions.size() : 10);
            g2.drawString(qText, cx + (cardW - fm.stringWidth(qText)) / 2, cy + 92);

            // Question Type Badge
            String badgeStr = switch (introType) {
                case "TRUE_FALSE" -> "ĐÚNG / SAI";
                case "MULTIPLE_SELECT" -> "CHỌN NHIỀU ĐÁP ÁN";
                case "FILL_IN" -> "ĐIỀN TỪ VÀO CHỖ TRỐNG";
                case "DRAG_AND_DROP" -> "KÉO THẢ TỪ VỰNG";
                default -> "TRẮC NGHIỆM 4 ĐÁP ÁN";
            };
            g2.setFont(Theme.BODY_BOLD);
            fm = g2.getFontMetrics();
            int bW = fm.stringWidth(badgeStr) + 24;
            int bH = 30;
            int bX = cx + (cardW - bW) / 2;
            int bY = cy + 120;
            g2.setColor(new Color(0x25, 0x63, 0xEB, 90));
            g2.fill(new RoundRectangle2D.Float(bX, bY, bW, bH, 14, 14));
            g2.setColor(new Color(0x60, 0xA5, 0xFA));
            g2.draw(new RoundRectangle2D.Float(bX + 0.5f, bY + 0.5f, bW - 1, bH - 1, 14, 14));
            g2.setColor(new Color(0x93, 0xC5, 0xFD));
            g2.drawString(badgeStr, bX + 12, bY + (bH - fm.getHeight()) / 2 + fm.getAscent());

            // Subtitle
            g2.setFont(Theme.SMALL_BOLD);
            g2.setColor(new Color(0x94, 0xA3, 0xB8));
            fm = g2.getFontMetrics();
            String sub = "Chuẩn bị làm bài...";
            g2.drawString(sub, cx + (cardW - fm.stringWidth(sub)) / 2, cy + 180);
        }
    }

    private String getMatchSoundText() {
        return (SoundManager.isMuted() && MusicManager.isMusicMuted()) ? "Tắt Âm" : "Âm Thanh";
    }
}
