package quiz.ui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

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
    private boolean isSubmitted = false;

    // Real-time scores
    private int myScore = 0;
    private int opponentScore = 0;
    private final ScoreBadge myScoreBadge;
    private final ScoreBadge oppScoreBadge;

    // Per-question countdown timer components
    private int currentQuestionTimeRemaining;
    private Timer questionCountdownTimer;
    private final JLabel lblTimer;
    private final JProgressBar timeProgressBar;

    // Question display components
    private final JLabel lblQuestionIndex;
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
        }

        setLayout(new BorderLayout());
        setBackground(Theme.DEEP_NAVY);

        // ==========================================
        // 1. TOP HUD (Header)
        // ==========================================
        JPanel topHud = new JPanel(new BorderLayout(16, 0));
        topHud.setBackground(Theme.NAVY_CARD);
        topHud.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER_DARK),
                new EmptyBorder(10, 20, 10, 20)));

        // 1.1 Left: Topic & Level Badges
        JPanel leftMeta = new JPanel();
        leftMeta.setLayout(new BoxLayout(leftMeta, BoxLayout.Y_AXIS));
        leftMeta.setOpaque(false);

        JLabel lblGameMode = new JLabel("ĐỐI KHÁNG 1V1");
        lblGameMode.setFont(Theme.SMALL_BOLD);
        lblGameMode.setForeground(Theme.MUTED_GRAY);

        JPanel badgesRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        badgesRow.setOpaque(false);
        badgesRow.add(new ModernBadge(topic, ModernBadge.Type.INFO));
        badgesRow.add(new ModernBadge(level + " • Bộ " + setId, ModernBadge.Type.RANK));

        leftMeta.add(lblGameMode);
        leftMeta.add(Box.createVerticalStrut(4));
        leftMeta.add(badgesRow);
        topHud.add(leftMeta, BorderLayout.WEST);

        // 1.2 Center: 1v1 Versus HUD with REAL-TIME SCORES
        JPanel centerHud = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0));
        centerHud.setOpaque(false);

        myScoreBadge = new ScoreBadge(true);
        oppScoreBadge = new ScoreBadge(false);

        JPanel p1 = createPlayerBadge(myName, true, myScoreBadge);
        JLabel vsBadge = createVsBadge();
        JPanel p2 = createPlayerBadge(opponentName, false, oppScoreBadge);

        // Emote bubble containers
        myEmoteContainer = new JPanel(null);
        myEmoteContainer.setOpaque(false);
        myEmoteContainer.setPreferredSize(new Dimension(80, 52));

        opponentEmoteContainer = new JPanel(null);
        opponentEmoteContainer.setOpaque(false);
        opponentEmoteContainer.setPreferredSize(new Dimension(80, 52));

        JPanel p1Stack = new JPanel();
        p1Stack.setLayout(new BoxLayout(p1Stack, BoxLayout.Y_AXIS));
        p1Stack.setOpaque(false);
        p1Stack.add(myEmoteContainer);
        p1Stack.add(p1);

        JPanel p2Stack = new JPanel();
        p2Stack.setLayout(new BoxLayout(p2Stack, BoxLayout.Y_AXIS));
        p2Stack.setOpaque(false);
        p2Stack.add(opponentEmoteContainer);
        p2Stack.add(p2);

        centerHud.add(p1Stack);
        centerHud.add(vsBadge);
        centerHud.add(p2Stack);
        topHud.add(centerHud, BorderLayout.CENTER);

        // 1.3 Right: Countdown Timer for Question & Sound Toggle
        JPanel rightSection = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
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

        timeProgressBar = new JProgressBar(0, perQuestionSeconds);
        timeProgressBar.setValue(perQuestionSeconds);
        timeProgressBar.setPreferredSize(new Dimension(140, 6));
        timeProgressBar.setMaximumSize(new Dimension(140, 6));
        timeProgressBar.setForeground(Theme.GREEN);
        timeProgressBar.setBackground(new Color(0, 0, 0, 80));
        timeProgressBar.setBorderPainted(false);
        timeProgressBar.setAlignmentX(Component.RIGHT_ALIGNMENT);

        rightTimer.add(lblTimerTitle);
        rightTimer.add(lblTimer);
        rightTimer.add(Box.createVerticalStrut(3));
        rightTimer.add(timeProgressBar);

        JButton btnSound = new JButton(SoundManager.isMuted() ? "Tắt âm" : "Bật âm");
        btnSound.setToolTipText("Bật/Tắt âm thanh");
        btnSound.setFont(Theme.SMALL_BOLD);
        btnSound.setPreferredSize(new Dimension(72, 38));
        btnSound.setFocusPainted(false);
        btnSound.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnSound.setBackground(new Color(0x2A, 0x37, 0x59));
        btnSound.setForeground(Theme.WHITE);
        btnSound.setBorder(BorderFactory.createLineBorder(Theme.BORDER_DARK, 1));
        btnSound.addActionListener(e -> {
            SoundManager.toggleMute();
            btnSound.setText(SoundManager.isMuted() ? "Tắt âm" : "Bật âm");
        });

        JButton btnSoundSettings = new JButton("⚙");
        btnSoundSettings.setToolTipText("Cài đặt âm thanh & nhạc nền");
        btnSoundSettings.setFont(Theme.SMALL_BOLD);
        btnSoundSettings.setPreferredSize(new Dimension(42, 38));
        btnSoundSettings.setFocusPainted(false);
        btnSoundSettings.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnSoundSettings.setBackground(new Color(0x2A, 0x37, 0x59));
        btnSoundSettings.setForeground(Theme.WHITE);
        btnSoundSettings.setBorder(BorderFactory.createLineBorder(Theme.BORDER_DARK, 1));
        btnSoundSettings.addActionListener(e -> {
            SoundManager.playClick();
            new SoundSettingsDialog(SwingUtilities.getWindowAncestor(this)).setVisible(true);
            btnSound.setText(SoundManager.isMuted() ? "Tắt âm" : "Bật âm");
        });

        rightSection.add(rightTimer);
        rightSection.add(btnSound);
        rightSection.add(btnSoundSettings);
        topHud.add(rightSection, BorderLayout.EAST);

        add(topHud, BorderLayout.NORTH);

        // ==========================================
        // 2. QUESTION NAVIGATOR STRIP (Top Sub-bar)
        // ==========================================
        JPanel navStrip = new JPanel(new BorderLayout(12, 0));
        navStrip.setBackground(new Color(0x18, 0x22, 0x38));
        navStrip.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER_DARK),
                new EmptyBorder(8, 20, 8, 20)));

        lblProgressSummary = new JLabel("Đã làm: 0/10 câu", SwingConstants.LEFT);
        lblProgressSummary.setFont(Theme.BODY_BOLD);
        lblProgressSummary.setForeground(Theme.WHITE);
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

                    Color bg;
                    Color fg;
                    Color border = null;

                    if (isCurrent) {
                        bg = Theme.PURPLE;
                        fg = Theme.WHITE;
                        border = Theme.WHITE;
                    } else if (isAnswered) {
                        bg = Theme.BLUE;
                        fg = Theme.WHITE;
                    } else {
                        bg = new Color(0x2A, 0x37, 0x59);
                        fg = Theme.MUTED_GRAY;
                        border = Theme.BORDER_DARK;
                    }

                    g2.setColor(bg);
                    g2.fill(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, 8, 8));

                    if (border != null) {
                        g2.setColor(border);
                        g2.setStroke(new BasicStroke(isCurrent ? 2f : 1f));
                        g2.draw(new RoundRectangle2D.Float(1.5f, 1.5f, w - 3, h - 3, 8, 8));
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
            btn.setPreferredSize(new Dimension(36, 32));
            btn.setFocusPainted(false);
            btn.setBorderPainted(false);
            btn.setContentAreaFilled(false);
            btn.setOpaque(false);
            navButtons[i] = btn;
            navGrid.add(btn);
        }
        navStrip.add(navGrid, BorderLayout.CENTER);

        JLabel dummyRight = new JLabel("             ");
        navStrip.add(dummyRight, BorderLayout.EAST);

        // ==========================================
        // 3. CENTER CONTENT (Question + 4 Kahoot Answers)
        // ==========================================
        JPanel centerWrapper = new JPanel(new BorderLayout(0, 16));
        centerWrapper.setOpaque(false);
        centerWrapper.setBorder(new EmptyBorder(16, 24, 16, 24));

        // 3.1 Waiting Banner Overlay (hidden by default)
        waitingBanner = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 10));
        waitingBanner.setBackground(new Color(0x13, 0x68, 0xCE, 220));
        waitingBanner.setBorder(BorderFactory.createLineBorder(Theme.WHITE, 1));
        lblWaitingStatus = new JLabel("BẠN ĐÃ NỘP BÀI! ĐANG CHỜ ĐỐI THỦ HOÀN THÀNH...", SwingConstants.CENTER);
        lblWaitingStatus.setFont(Theme.SUBTITLE);
        lblWaitingStatus.setForeground(Theme.WHITE);
        waitingBanner.add(lblWaitingStatus);
        waitingBanner.setVisible(false);

        // Animated Question Card
        questionCard = new AnimatedQuestionCard(new BorderLayout(0, 14));
        questionCard.setCardBackground(Theme.NAVY_CARD);
        questionCard.setBorderColor(Theme.BORDER_DARK);
        questionCard.setCardPadding(20, 24, 20, 24);

        // Question header
        JPanel qHeader = new JPanel(new BorderLayout());
        qHeader.setOpaque(false);

        lblQuestionIndex = new JLabel("CÂU HỎI 1 / 10");
        lblQuestionIndex.setFont(Theme.SUBTITLE);
        lblQuestionIndex.setForeground(Theme.BLUE);
        qHeader.add(lblQuestionIndex, BorderLayout.WEST);

        lblHint = new JLabel("Chọn 1 trong 4 đáp án bên dưới");
        lblHint.setFont(Theme.SMALL);
        lblHint.setForeground(Theme.MUTED_GRAY);
        qHeader.add(lblHint, BorderLayout.EAST);

        // Multi-select notice label (appears directly above question text)
        lblMultiSelectNotice = new JLabel("✦ Câu hỏi có thể chọn nhiều đáp án");
        lblMultiSelectNotice.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblMultiSelectNotice.setForeground(new Color(0x38, 0xBD, 0xF8)); // Bright Sky Cyan
        lblMultiSelectNotice.setBorder(new EmptyBorder(0, 0, 4, 0));
        lblMultiSelectNotice.setVisible(false);

        // Question text
        txtQuestion = new JTextArea("Đang tải câu hỏi...");
        txtQuestion.setFont(Theme.TITLE);
        txtQuestion.setForeground(Theme.WHITE);
        txtQuestion.setBackground(Theme.NAVY_CARD);
        txtQuestion.setLineWrap(true);
        txtQuestion.setWrapStyleWord(true);
        txtQuestion.setEditable(false);
        txtQuestion.setFocusable(false);
        txtQuestion.setBorder(new EmptyBorder(4, 0, 10, 0));

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

        questionCard.add(qHeader, BorderLayout.NORTH);
        questionCard.add(questionCardCenter, BorderLayout.CENTER);

        // 4 Kahoot-style Answer Buttons (2x2 grid)
        answersGrid = new JPanel(new GridLayout(2, 2, 14, 14));
        answersGrid.setOpaque(false);
        answersGrid.setPreferredSize(new Dimension(800, 160));

        answerButtons[0] = new ModernButton("A", ModernButton.Style.ANSWER_A, "A");
        answerButtons[1] = new ModernButton("B", ModernButton.Style.ANSWER_B, "B");
        answerButtons[2] = new ModernButton("C", ModernButton.Style.ANSWER_C, "C");
        answerButtons[3] = new ModernButton("D", ModernButton.Style.ANSWER_D, "D");

        for (int i = 0; i < 4; i++) {
            final int choiceIndex = i;
            answerButtons[i].setFont(Theme.BODY_BOLD);
            answerButtons[i].setCornerRadius(12);
            answerButtons[i].addActionListener(e -> selectAnswer(choiceIndex));
            answersGrid.add(answerButtons[i]);
        }

        // True / False 2-Button Card (Split 50/50: Blue & Yellow)
        tfPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        tfPanel.setOpaque(false);
        tfPanel.setPreferredSize(new Dimension(800, 160));

        btnTrue = new ModernButton("ĐÚNG", ModernButton.Style.TRUE_BLUE, "✓");
        btnTrue.setFont(new Font("Segoe UI", Font.BOLD, 22));
        btnTrue.setCornerRadius(14);
        btnTrue.addActionListener(e -> selectAnswer(0));

        btnFalse = new ModernButton("SAI", ModernButton.Style.FALSE_YELLOW, "✗");
        btnFalse.setFont(new Font("Segoe UI", Font.BOLD, 22));
        btnFalse.setCornerRadius(14);
        btnFalse.addActionListener(e -> selectAnswer(1));

        tfPanel.add(btnTrue);
        tfPanel.add(btnFalse);

        // Fill-in text input panel (Center rounded square / card)
        fillInPanel = new JPanel(new GridBagLayout());
        fillInPanel.setOpaque(false);
        fillInPanel.setPreferredSize(new Dimension(800, 160));

        JPanel fillInBox = new JPanel();
        fillInBox.setLayout(new BoxLayout(fillInBox, BoxLayout.Y_AXIS));
        fillInBox.setOpaque(false);

        txtFillInInput = new ModernFillInField("Nhập đáp án");
        txtFillInInput.setPreferredSize(new Dimension(540, 58));
        txtFillInInput.setMaximumSize(new Dimension(540, 58));
        txtFillInInput.setAlignmentX(Component.CENTER_ALIGNMENT);
        txtFillInInput.addActionListener(e -> {
            if (!isRevealing && !isSubmitted && !isAnimating) {
                revealAnswerAndAdvance();
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
        bottomOptionsCard.setPreferredSize(new Dimension(800, 160));
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
        add(mainCenter, BorderLayout.CENTER);

        // ==========================================
        // 4. BOTTOM ACTION BAR (Footer)
        // ==========================================
        JPanel bottomBar = new JPanel(new BorderLayout(16, 0));
        bottomBar.setBackground(Theme.NAVY_CARD);
        bottomBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER_DARK),
                new EmptyBorder(12, 24, 12, 24)));

        // 4.1 Left: Leave Match Button & Emote Reactions
        JPanel leftBottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftBottom.setOpaque(false);

        btnLeave = new ModernButton("Thoát", ModernButton.Style.DANGER);
        btnLeave.setPreferredSize(new Dimension(90, 38));
        btnLeave.addActionListener(e -> {
            SoundManager.playClick();
            confirmLeave();
        });
        leftBottom.add(btnLeave);

        JPanel emoteBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        emoteBar.setOpaque(false);
        String[] emotes = { "🔥", "⚡", "😎", "👏", "😱", "💪" };
        for (String em : emotes) {
            JButton btnEm = new JButton(em);
            btnEm.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 15));
            btnEm.setPreferredSize(new Dimension(34, 34));
            btnEm.setMargin(new Insets(0, 0, 0, 0));
            btnEm.setFocusPainted(false);
            btnEm.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnEm.setBackground(new Color(0x2A, 0x37, 0x59));
            btnEm.setForeground(Theme.WHITE);
            btnEm.setBorder(BorderFactory.createLineBorder(Theme.BORDER_DARK, 1));
            btnEm.addActionListener(e -> sendEmote(em));
            emoteBar.add(btnEm);
        }
        leftBottom.add(emoteBar);
        bottomBar.add(leftBottom, BorderLayout.WEST);

        // 4.2 Center: Next Navigation
        JPanel centerNav = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        centerNav.setOpaque(false);

        btnNext = new ModernButton("Câu tiếp", ModernButton.Style.PRIMARY);
        btnNext.setPreferredSize(new Dimension(130, 40));
        btnNext.addActionListener(e -> {
            SoundManager.playClick();
            revealAnswerAndAdvance();
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
        // 5. START FIRST QUESTION WITH ENTRANCE ANIMATION
        // ==========================================
        SwingUtilities.invokeLater(() -> startQuestionEntrance());
    }

    // ==========================================
    // SCORE MANAGEMENT & REAL-TIME DISPLAY
    // ==========================================
    public void updateScores(int myScore, int opponentScore) {
        setMyScore(myScore);
        setOpponentScore(opponentScore);
    }

    public void setMyScore(int score) {
        if (score > this.myScore) {
            SoundManager.playScorePoint();
        }
        this.myScore = score;
        if (myScoreBadge != null) {
            myScoreBadge.setScore(score);
        }
    }

    public void setOpponentScore(int score) {
        this.opponentScore = score;
        if (oppScoreBadge != null) {
            oppScoreBadge.setScore(score);
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

    private JPanel createPlayerBadge(String name, boolean isMe, ScoreBadge badge) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        p.setOpaque(false);

        final String avatarId = isMe ? this.myAvatarId : this.opponentAvatarId;

        JPanel avatar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setupAntiAliasing(g2);
                int size = Math.min(getWidth(), getHeight());
                int ox = (getWidth() - size) / 2;
                int oy = (getHeight() - size) / 2;
                AvatarPicker.paintAvatar(g2, avatarId, ox, oy, size);
                g2.dispose();
            }
        };
        avatar.setPreferredSize(new Dimension(36, 36));
        avatar.setOpaque(false);

        JLabel lblName = new JLabel(name + (isMe ? " (Bạn)" : ""));
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
                g2.setColor(Theme.RED);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.setColor(Theme.WHITE);
                g2.setFont(Theme.SMALL_BOLD);
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth("VS")) / 2;
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString("VS", x, y);
                g2.dispose();
            }
        };
        vs.setPreferredSize(new Dimension(28, 22));
        vs.setOpaque(false);
        return vs;
    }

    // ==========================================
    // PER-QUESTION COUNTDOWN TIMER
    // ==========================================
    private void startPerQuestionTimer() {
        stopCountdownTimer();
        currentQuestionTimeRemaining = perQuestionSeconds;
        updateTimerDisplay();

        questionCountdownTimer = new Timer(1000, (ActionEvent e) -> {
            currentQuestionTimeRemaining--;
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

        if (currentQuestionTimeRemaining <= 3) {
            lblTimer.setForeground(Theme.RED);
            timeProgressBar.setForeground(Theme.RED);
        } else if (currentQuestionTimeRemaining <= Math.max(4, perQuestionSeconds / 2)) {
            lblTimer.setForeground(Theme.YELLOW);
            timeProgressBar.setForeground(Theme.YELLOW);
        } else {
            lblTimer.setForeground(Theme.WHITE);
            timeProgressBar.setForeground(Theme.GREEN);
        }
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
        if (questions == null || index < 0 || index >= questions.size()) {
            return;
        }
        currentQuestionIndex = index;
        startQuestionEntrance();
    }

    private void startQuestionEntrance() {
        if (questions == null || currentQuestionIndex >= questions.size()) {
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

        if (q.isDragAndDrop()) {
            lblMultiSelectNotice.setVisible(false);
            lblHint.setText("Kéo thả các đáp án phía dưới vào chỗ trống tương ứng");
            currentDragDropPanel = new DragDropPanel(q);
            currentDragDropPanel.setOnStateChangeListener(() -> {
                if (currentDragDropPanel.isAllCorrect()) {
                    selectedAnswers[currentQuestionIndex] = 1;
                } else if (currentDragDropPanel.hasAnyAnswer()) {
                    selectedAnswers[currentQuestionIndex] = 0;
                } else {
                    selectedAnswers[currentQuestionIndex] = -1;
                }
                updateNavState();
            });

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

            currentDragDropPanel.setChipsAnimState(0.0f, 40);
            currentDragDropPanel.setInteractive(false);
        } else if (q.isFillIn()) {
            lblMultiSelectNotice.setVisible(false);
            lblHint.setText("Nhập câu trả lời từ bàn phím");
            currentDragDropPanel = null;
            txtQuestion.setText(q.text());

            txtFillInInput.resetReveal();
            txtFillInInput.setEnabled(false);
            txtFillInInput.setText(userFillInAnswers[currentQuestionIndex] != null ? userFillInAnswers[currentQuestionIndex] : "");
            lblFillInCorrectAnswer.setVisible(false);
            lblFillInCorrectAnswer.setText("");

            ((CardLayout) questionCardCenter.getLayout()).show(questionCardCenter, "NORMAL");
            ((CardLayout) bottomOptionsCard.getLayout()).show(bottomOptionsCard, "FILL_IN");
        } else if (q.isTrueFalse()) {
            lblMultiSelectNotice.setVisible(false);
            lblHint.setText("Chọn 1 trong 2 đáp án: ĐÚNG hoặc SAI");
            currentDragDropPanel = null;
            txtQuestion.setText(q.text());

            int selected = selectedAnswers[currentQuestionIndex];
            btnTrue.setSelectedState(selected == 0);
            btnFalse.setSelectedState(selected == 1);
            btnTrue.setEnabled(false);
            btnFalse.setEnabled(false);
            btnTrue.resetReveal();
            btnFalse.resetReveal();
            btnTrue.setAnimState(0.0f, 40);
            btnFalse.setAnimState(0.0f, 40);

            ((CardLayout) questionCardCenter.getLayout()).show(questionCardCenter, "NORMAL");
            ((CardLayout) bottomOptionsCard.getLayout()).show(bottomOptionsCard, "TRUE_FALSE");
        } else if (q.isMultipleSelect()) {
            lblMultiSelectNotice.setVisible(true);
            lblHint.setText("Chọn tất cả các đáp án đúng");
            currentDragDropPanel = null;
            txtQuestion.setText(q.text());

            String[] choices = q.choices();
            String[] prefixes = { "A", "B", "C", "D" };
            int mask = selectedAnswers[currentQuestionIndex];

            for (int i = 0; i < 4; i++) {
                answerButtons[i].setText(i < choices.length ? choices[i] : "—");
                answerButtons[i].setPrefixBadge(prefixes[i]);
                answerButtons[i].setSelectedState(mask != -1 && ((mask & (1 << i)) != 0));
                answerButtons[i].setEnabled(false);
                answerButtons[i].resetReveal();
                answerButtons[i].setAnimState(0.0f, 40);
            }

            ((CardLayout) questionCardCenter.getLayout()).show(questionCardCenter, "NORMAL");
            ((CardLayout) bottomOptionsCard.getLayout()).show(bottomOptionsCard, "BUTTONS");
        } else {
            lblMultiSelectNotice.setVisible(false);
            lblHint.setText("Chọn 1 trong 4 đáp án bên dưới");
            currentDragDropPanel = null;
            txtQuestion.setText(q.text());

            String[] choices = q.choices();
            String[] prefixes = { "A", "B", "C", "D" };
            int selected = selectedAnswers[currentQuestionIndex];

            for (int i = 0; i < 4; i++) {
                answerButtons[i].setText(i < choices.length ? choices[i] : "—");
                answerButtons[i].setPrefixBadge(prefixes[i]);
                answerButtons[i].setSelectedState(selected == i);
                answerButtons[i].setEnabled(false);
                answerButtons[i].resetReveal();
                answerButtons[i].setAnimState(0.0f, 40);
            }

            ((CardLayout) questionCardCenter.getLayout()).show(questionCardCenter, "NORMAL");
            ((CardLayout) bottomOptionsCard.getLayout()).show(bottomOptionsCard, "BUTTONS");
        }

        btnNext.setEnabled(false);
        updateNavState();

        // Tính khoảng cách dịch chuyển để đưa questionCard về vị trí giữa
        int gridH = bottomOptionsCard.getHeight() > 0 ? bottomOptionsCard.getHeight() : 160;
        final int centerOffsetY = Math.max(70, (gridH + 14) / 2);

        // Reset hiển thị timer chuẩn bị cho câu hỏi mới
        currentQuestionTimeRemaining = perQuestionSeconds;
        updateTimerDisplay();

        // Phát âm thanh pop đàn hồi khi bắt đầu câu hỏi
        SoundManager.playPop();

        animStartTime = System.currentTimeMillis();
        animTimer = new Timer(16, e -> onAnimTick(centerOffsetY));
        animTimer.start();
    }

    private void onAnimTick(int centerOffsetY) {
        long elapsed = System.currentTimeMillis() - animStartTime;
        boolean isDnd = (currentDragDropPanel != null);
        MatchQuestion q = (questions != null && currentQuestionIndex < questions.size()) ? questions.get(currentQuestionIndex) : null;
        boolean isTf = (q != null && q.isTrueFalse());
        boolean isFill = (q != null && q.isFillIn());

        // Phase 1: 0ms - 400ms: NỔI NỔI ĐÀN HỒI Ở VỊ TRÍ GIỮA (BackEaseOut with overshoot)
        if (elapsed <= 400) {
            float p = Math.min(1.0f, elapsed / 400.0f);
            float c1 = 1.70158f;
            float c3 = c1 + 1.0f;
            float eased = (float) (1.0 + c3 * Math.pow(p - 1.0, 3) + c1 * Math.pow(p - 1.0, 2));
            float scale = 0.35f + 0.65f * eased;
            float alpha = Math.min(1.0f, p * 2.5f);
            questionCard.setAnimState(scale, centerOffsetY, alpha);
            if (isDnd) {
                currentDragDropPanel.setChipsAnimState(0.0f, 40);
            } else if (isTf) {
                btnTrue.setAnimState(0.0f, 40);
                btnFalse.setAnimState(0.0f, 40);
            } else if (isFill) {
                txtFillInInput.setAnimScale(0.0f);
            } else {
                for (ModernButton b : answerButtons) {
                    b.setAnimState(0.0f, 40);
                }
            }
        }
        // Phase 2: 400ms - 660ms: CHUYỂN LÊN VỊ TRÍ MẶC ĐỊNH TRÊN CÙNG
        else if (elapsed <= 660) {
            float p = Math.min(1.0f, (elapsed - 400.0f) / 260.0f);
            float eased = 1.0f - (1.0f - p) * (1.0f - p);
            int curOffsetY = (int) (centerOffsetY * (1.0f - eased));
            questionCard.setAnimState(1.0f, curOffsetY, 1.0f);
            if (isDnd) {
                currentDragDropPanel.setChipsAnimState(0.0f, 40);
            } else if (isTf) {
                btnTrue.setAnimState(0.0f, 40);
                btnFalse.setAnimState(0.0f, 40);
            } else if (isFill) {
                txtFillInInput.setAnimScale(0.0f);
            } else {
                for (ModernButton b : answerButtons) {
                    b.setAnimState(0.0f, 40);
                }
            }
        }
        // Phase 3: 660ms - 1050ms: CÁC ĐÁP ÁN LẦN LƯỢT HIỆN DẦN TỪ DƯỚI LÊN (Staggered)
        else if (elapsed < 1050) {
            questionCard.setAnimState(1.0f, 0, 1.0f);
            if (isDnd) {
                float bp = Math.min(1.0f, (elapsed - 660.0f) / 300.0f);
                float bEased = 1.0f - (1.0f - bp) * (1.0f - bp);
                int bOffset = (int) (40 * (1.0f - bEased));
                currentDragDropPanel.setChipsAnimState(bp, bOffset);
            } else if (isTf) {
                int bStart0 = 660;
                int bStart1 = 740;
                if (elapsed < bStart0) {
                    btnTrue.setAnimState(0.0f, 40);
                } else if (elapsed < bStart0 + 170) {
                    float bp = (elapsed - bStart0) / 170.0f;
                    float bEased = 1.0f - (1.0f - bp) * (1.0f - bp);
                    btnTrue.setAnimState(bp, (int) (40 * (1.0f - bEased)));
                } else {
                    btnTrue.setAnimState(1.0f, 0);
                }

                if (elapsed < bStart1) {
                    btnFalse.setAnimState(0.0f, 40);
                } else if (elapsed < bStart1 + 170) {
                    float bp = (elapsed - bStart1) / 170.0f;
                    float bEased = 1.0f - (1.0f - bp) * (1.0f - bp);
                    btnFalse.setAnimState(bp, (int) (40 * (1.0f - bEased)));
                } else {
                    btnFalse.setAnimState(1.0f, 0);
                }
            } else if (isFill) {
                float bp = Math.min(1.0f, (elapsed - 660.0f) / 300.0f);
                txtFillInInput.setAnimScale(bp);
            } else {
                for (int i = 0; i < 4; i++) {
                    int bStart = 660 + i * 65;
                    if (elapsed < bStart) {
                        answerButtons[i].setAnimState(0.0f, 40);
                    } else if (elapsed < bStart + 170) {
                        float bp = (elapsed - bStart) / 170.0f;
                        float bEased = 1.0f - (1.0f - bp) * (1.0f - bp);
                        int bOffset = (int) (40 * (1.0f - bEased));
                        answerButtons[i].setAnimState(bp, bOffset);
                    } else {
                        answerButtons[i].setAnimState(1.0f, 0);
                    }
                }
            }
        }
        // KẾT THÚC ANIMATION: BẬT TƯƠNG TÁC VÀ BẮT ĐẦU ĐẾM NGƯỢC
        else {
            animTimer.stop();
            isAnimating = false;
            questionCard.setAnimState(1.0f, 0, 1.0f);
            if (isDnd) {
                currentDragDropPanel.setChipsAnimState(1.0f, 0);
                currentDragDropPanel.setInteractive(!isSubmitted);
            } else if (isTf) {
                btnTrue.setAnimState(1.0f, 0);
                btnFalse.setAnimState(1.0f, 0);
                btnTrue.setEnabled(!isSubmitted);
                btnFalse.setEnabled(!isSubmitted);
            } else if (isFill) {
                txtFillInInput.setAnimScale(1.0f);
                txtFillInInput.setEnabled(!isSubmitted);
                txtFillInInput.requestFocusInWindow();
            } else {
                for (int i = 0; i < 4; i++) {
                    answerButtons[i].setAnimState(1.0f, 0);
                    answerButtons[i].setEnabled(!isSubmitted);
                }
            }
            btnNext.setEnabled(!isSubmitted);
            updateNavState();

            // Khởi động bộ đếm ngược thời gian câu hỏi
            startPerQuestionTimer();

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
            btnNext.setText(currentQuestionIndex >= totalQ - 1 ? "Hoàn thành" : "Câu tiếp");
        }
    }

    private void selectAnswer(int choiceIndex) {
        if (isSubmitted || isAnimating || isRevealing) {
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

            for (int i = 0; i < 4; i++) {
                answerButtons[i].setSelectedState((curMask & (1 << i)) != 0);
            }
        } else if (q != null && q.isTrueFalse()) {
            if (selectedAnswers[currentQuestionIndex] == choiceIndex) {
                selectedAnswers[currentQuestionIndex] = -1;
            } else {
                selectedAnswers[currentQuestionIndex] = choiceIndex;
            }
            btnTrue.setSelectedState(selectedAnswers[currentQuestionIndex] == 0);
            btnFalse.setSelectedState(selectedAnswers[currentQuestionIndex] == 1);
        } else {
            if (selectedAnswers[currentQuestionIndex] == choiceIndex) {
                selectedAnswers[currentQuestionIndex] = -1;
            } else {
                selectedAnswers[currentQuestionIndex] = choiceIndex;
            }
            for (int i = 0; i < 4; i++) {
                answerButtons[i].setSelectedState(selectedAnswers[currentQuestionIndex] == i);
            }
        }
        updateNavState();
        // Không cập nhật hay gửi điểm ở đây để người chơi không thể thử từng đáp án!
    }

    private void finishCurrentQuestion() {
        if (currentQuestionIndex >= 0 && currentQuestionIndex < 10) {
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
            if (!questionFinalized[currentQuestionIndex]) {
                questionFinalized[currentQuestionIndex] = true;
                int choice = selectedAnswers[currentQuestionIndex];
                if (onAnswerListener != null) {
                    onAnswerListener.accept(currentQuestionIndex, choice);
                }
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
        if (isRevealing || isSubmitted || isAnimating) {
            return;
        }
        stopCountdownTimer();
        // Nếu người chơi chọn đáp án sớm trước khi hết giờ, dừng countdown ngay
        if (currentQuestionTimeRemaining > 0) {
            SoundManager.stopCountdown();
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
                lblFillInCorrectAnswer.setText("✓ Đáp án chính xác: " + displayTarget);
                lblFillInCorrectAnswer.setVisible(true);
            }

            final long animStart = System.currentTimeMillis();
            final int growDuration = 400;
            final int totalDuration = 850;

            revealTimer = new Timer(16, e -> {
                long elapsed = System.currentTimeMillis() - animStart;
                if (elapsed < growDuration) {
                    float p = (float) elapsed / growDuration;
                    float scale = 1.0f + 0.15f * (float) Math.sin(p * Math.PI / 2.0);
                    txtFillInInput.setAnimScale(scale);
                    fillInPanel.repaint();
                } else if (elapsed < totalDuration) {
                    txtFillInInput.setAnimScale(1.15f);
                    fillInPanel.repaint();
                } else {
                    revealTimer.stop();
                    isRevealing = false;
                    txtFillInInput.resetReveal();
                    lblFillInCorrectAnswer.setVisible(false);
                    fillInPanel.repaint();

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
                    try { answersGrid.setComponentZOrder(answerButtons[c], 0); } catch (Exception ignored) {}
                } else if (isThisSelected) {
                    answerButtons[c].setRevealColors(wrongBg, whiteText);
                    scalingButtons.add(answerButtons[c]);
                    try { answersGrid.setComponentZOrder(answerButtons[c], 0); } catch (Exception ignored) {}
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
                long elapsed = System.currentTimeMillis() - animStart;
                if (elapsed < growDuration) {
                    float p = (float) elapsed / growDuration;
                    float scale = 1.0f + 0.18f * (float) Math.sin(p * Math.PI / 2.0);
                    for (ModernButton b : scalingButtons) b.setAnimScale(scale);
                    answersGrid.repaint();
                } else if (elapsed < totalDuration) {
                    for (ModernButton b : scalingButtons) b.setAnimScale(1.18f);
                    answersGrid.repaint();
                } else {
                    revealTimer.stop();
                    isRevealing = false;
                    for (ModernButton b : answerButtons) b.resetReveal();
                    answersGrid.repaint();

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

        // Đảm bảo nút phóng to nằm trên cùng Z-order của container
        if (correctBtn != null && parentContainer != null) {
            try {
                parentContainer.setComponentZOrder(correctBtn, 0);
            } catch (Exception ignored) {}
        }
        if (userBtn != null && parentContainer != null && userBtn != correctBtn) {
            try {
                parentContainer.setComponentZOrder(userBtn, 0);
            } catch (Exception ignored) {}
        }

        // Animation phóng to:
        // Giai đoạn nở lớn: 400ms (0.4s) scale từ 1.0f lên 1.18f
        // Giai đoạn giữ nguyên: 450ms ở 1.18f
        // Tổng thời gian: ~850ms, sau đó reset màu sắc và chuyển câu tiếp theo
        final long animStart = System.currentTimeMillis();
        final int growDuration = 400;
        final int totalDuration = 850;

        revealTimer = new Timer(16, e -> {
            long elapsed = System.currentTimeMillis() - animStart;
            if (elapsed < growDuration) {
                float p = (float) elapsed / growDuration;
                float scale = 1.0f + 0.18f * (float) Math.sin(p * Math.PI / 2.0);
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
                    correctBtn.setAnimScale(1.18f);
                }
                if (userBtn != null && userBtn != correctBtn) {
                    userBtn.setAnimScale(1.18f);
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
        if (isSubmitted) {
            return;
        }

        // Đảm bảo ghi nhận đáp án của câu hỏi hiện tại nếu người chơi vừa thao tác
        finishCurrentQuestion();

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
            if (targetIdx >= 0 && targetIdx < totalQ) {
                navigateToQuestion(targetIdx);
            }
        }
    }

    private void doSubmit() {
        if (isSubmitted) {
            return;
        }
        if (activeSubmitDialog != null && activeSubmitDialog.isVisible()) {
            activeSubmitDialog.dispose();
            activeSubmitDialog = null;
        }
        finishCurrentQuestion();
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

        if (onSubmitListener != null) {
            onSubmitListener.accept(selectedAnswers.clone());
        }
    }

    public void onSubmittedAcknowledged() {
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

    private void confirmLeave() {
        int choice = JOptionPane.showConfirmDialog(
                this,
                "Bạn có chắc chắn muốn rời trận đấu?\n"
                        + "Thoát giữa trận sẽ bị xử THUA và bị trừ điểm xếp hạng!",
                "Cảnh Báo Thoát Trận",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (choice == JOptionPane.YES_OPTION) {
            stopTimer();
            if (onLeaveListener != null) {
                onLeaveListener.run();
            }
        }
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
        container.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 2));
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

    // ==========================================
    // CUSTOM HELPER COMPONENTS
    // ==========================================

    /**
     * ScoreBadge — Huy hiệu điểm số thời gian thực hiển thị ngay cạnh tên người chơi
     * với hiệu ứng phóng to (pulse) và phát sáng ánh kim khi điểm số tăng.
     */
    private static class ScoreBadge extends JPanel {
        private int score = 0;
        private final boolean isMe;
        private float pulseScale = 1.0f;
        private Timer pulseTimer;

        ScoreBadge(boolean isMe) {
            this.isMe = isMe;
            setOpaque(false);
            setPreferredSize(new Dimension(84, 28));
        }

        public void setScore(int newScore) {
            if (newScore != this.score) {
                this.score = newScore;
                triggerPulse();
            }
        }

        public int getScore() {
            return score;
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
                    pulseScale = 1.0f + 0.35f * (float) Math.sin(p * Math.PI);
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

            Color bg = isMe ? new Color(0x13, 0x68, 0xCE, 70) : new Color(0x86, 0x4C, 0xBF, 70);
            Color border = isMe ? new Color(0x3B, 0x82, 0xF6) : new Color(0xA8, 0x55, 0xF7);
            if (pulseScale > 1.05f) {
                bg = new Color(0xF5, 0x9E, 0x0B, 140);
                border = new Color(0xFB, 0xBF, 0x24);
            }

            g2.setColor(bg);
            g2.fill(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, 12, 12));

            g2.setColor(border);
            g2.setStroke(new BasicStroke(1.5f));
            g2.draw(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, 12, 12));

            String text = score + " đ";
            g2.setColor(pulseScale > 1.05f ? Theme.WHITE : (isMe ? new Color(0xBF, 0xDB, 0xFE) : new Color(0xE9, 0xD5, 0xFF)));
            g2.setFont(Theme.SMALL_BOLD);
            FontMetrics fm = g2.getFontMetrics();
            int tx = (w - fm.stringWidth(text)) / 2;
            int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(text, tx, ty);

            g2.dispose();
        }
    }

    /**
     * AnimatedQuestionCard — Card câu hỏi hỗ trợ biến đổi scale, vị trí Y và alpha
     * phục vụ hiệu ứng nổi đàn hồi và lướt lên vị trí mặc định.
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
}
