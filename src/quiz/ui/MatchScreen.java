package quiz.ui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * MatchScreen - Màn hình thi đấu đối kháng trực tiếp 1v1 của Quiz Arena:
 * - Giao diện Dark Game Theme (Deep Navy #1F2A44) theo chuẩn Kahoot-inspired.
 * - HUD trên cùng hiển thị thông tin trận đấu, huy hiệu chủ đề/mức độ, Versus
 * 1v1 Card và Live Timer đếm ngược.
 * - Thanh điều hướng 10 câu hỏi trực quan (đổi màu khi đã chọn đáp án, đánh dấu
 * câu hiện tại).
 * - Thẻ câu hỏi nổi bật cùng 4 nút đáp án A/B/C/D chuẩn màu Kahoot (Đỏ, Xanh
 * dương, Vàng, Xanh lá).
 * - Hỗ trợ nộp bài sớm, xác nhận thoát trận, tự động nộp khi hết giờ và trạng
 * thái chờ kết quả.
 */
public class MatchScreen extends JPanel {

    private final String matchId;
    private final String myName;
    private final String opponentName;
    private final String topic;
    private final String level;
    private final String setId;
    private final int totalSeconds;
    private final List<MatchQuestion> questions;

    private final Consumer<int[]> onSubmitListener;
    private final Runnable onLeaveListener;

    private int currentQuestionIndex = 0;
    private final int[] selectedAnswers = new int[10];
    private boolean isSubmitted = false;

    // Timer components
    private final long deadlineNano;
    private final Timer matchTimer;
    private final JLabel lblTimer;
    private final JProgressBar timeProgressBar;

    // Question display components
    private final JLabel lblQuestionIndex;
    private final JTextArea txtQuestion;
    private final ModernButton[] answerButtons = new ModernButton[4];
    private final JButton[] navButtons = new JButton[10];
    private final JLabel lblProgressSummary;

    // Action buttons
    private final ModernButton btnPrev;
    private final ModernButton btnNext;
    private final ModernButton btnSubmit;
    private final ModernButton btnLeave;

    // Waiting banner overlay
    private final JPanel waitingBanner;
    private final JLabel lblWaitingStatus;

    // Emotes & Audio components
    private Consumer<String> onEmoteListener;
    private final JLabel lblOpponentEmote = new JLabel("");
    private Timer emoteClearTimer;

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

        this.matchId = matchId;
        this.myName = myName;
        this.opponentName = opponentName;
        this.topic = topic;
        this.level = level;
        this.setId = setId;
        this.totalSeconds = totalSeconds > 0 ? totalSeconds : 60;
        this.questions = questions;
        this.onSubmitListener = onSubmitListener;
        this.onLeaveListener = onLeaveListener;

        for (int i = 0; i < 10; i++) {
            selectedAnswers[i] = -1;
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

        // 1.2 Center: 1v1 Versus HUD
        JPanel centerHud = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0));
        centerHud.setOpaque(false);

        JPanel p1 = createPlayerBadge(myName, true);
        JLabel vsBadge = createVsBadge();
        JPanel p2 = createPlayerBadge(opponentName, false);

        centerHud.add(p1);
        centerHud.add(vsBadge);
        centerHud.add(p2);
        lblOpponentEmote.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 22));
        lblOpponentEmote.setForeground(Theme.YELLOW);
        lblOpponentEmote.setVisible(false);
        centerHud.add(lblOpponentEmote);
        topHud.add(centerHud, BorderLayout.CENTER);

        // 1.3 Right: Countdown Timer HUD & Sound Toggle
        JPanel rightSection = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightSection.setOpaque(false);

        JPanel rightTimer = new JPanel();
        rightTimer.setLayout(new BoxLayout(rightTimer, BoxLayout.Y_AXIS));
        rightTimer.setOpaque(false);

        lblTimer = new JLabel(formatTime(totalSeconds), SwingConstants.RIGHT);
        lblTimer.setFont(Theme.TIMER);
        lblTimer.setForeground(Theme.WHITE);
        lblTimer.setAlignmentX(Component.RIGHT_ALIGNMENT);

        timeProgressBar = new JProgressBar(0, totalSeconds);
        timeProgressBar.setValue(totalSeconds);
        timeProgressBar.setPreferredSize(new Dimension(140, 6));
        timeProgressBar.setMaximumSize(new Dimension(140, 6));
        timeProgressBar.setForeground(Theme.GREEN);
        timeProgressBar.setBackground(new Color(0, 0, 0, 80));
        timeProgressBar.setBorderPainted(false);
        timeProgressBar.setAlignmentX(Component.RIGHT_ALIGNMENT);

        rightTimer.add(lblTimer);
        rightTimer.add(Box.createVerticalStrut(3));
        rightTimer.add(timeProgressBar);

        JButton btnSound = new JButton(SoundManager.isMuted() ? "🔇" : "🔊");
        btnSound.setToolTipText("Bật/Tắt âm thanh");
        btnSound.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
        btnSound.setPreferredSize(new Dimension(42, 38));
        btnSound.setFocusPainted(false);
        btnSound.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnSound.setBackground(new Color(0x2A, 0x37, 0x59));
        btnSound.setForeground(Theme.WHITE);
        btnSound.setBorder(BorderFactory.createLineBorder(Theme.BORDER_DARK, 1));
        btnSound.addActionListener(e -> {
            SoundManager.toggleMute();
            btnSound.setText(SoundManager.isMuted() ? "🔇" : "🔊");
        });

        rightSection.add(rightTimer);
        rightSection.add(btnSound);
        topHud.add(rightSection, BorderLayout.EAST);

        add(topHud, BorderLayout.NORTH);

        // ==========================================
        // 2. QUESTION NAVIGATOR STRIP (Top Sub-bar)
        // ==========================================
        JPanel navStrip = new JPanel(new BorderLayout(12, 0));
        navStrip.setBackground(new Color(0x18, 0x22, 0x38)); // Slightly darker navy
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
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.addActionListener(e -> navigateToQuestion(qIndex));
            navButtons[i] = btn;
            navGrid.add(btn);
        }
        navStrip.add(navGrid, BorderLayout.CENTER);

        // Empty right filler for balance
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
        lblWaitingStatus = new JLabel("🎉 BẠN ĐÃ NỘP BÀI! ĐANG CHỜ ĐỐI THỦ HOÀN THÀNH...", SwingConstants.CENTER);
        lblWaitingStatus.setFont(Theme.SUBTITLE);
        lblWaitingStatus.setForeground(Theme.WHITE);
        waitingBanner.add(lblWaitingStatus);
        waitingBanner.setVisible(false);

        // Main Question Card
        ModernCard questionCard = new ModernCard(new BorderLayout(0, 14));
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

        JLabel hint = new JLabel("Chọn 1 trong 4 đáp án bên dưới");
        hint.setFont(Theme.SMALL);
        hint.setForeground(Theme.MUTED_GRAY);
        qHeader.add(hint, BorderLayout.EAST);

        // Question text
        txtQuestion = new JTextArea("Đang tải câu hỏi...");
        txtQuestion.setFont(Theme.TITLE);
        txtQuestion.setForeground(Theme.WHITE);
        txtQuestion.setBackground(Theme.NAVY_CARD);
        txtQuestion.setLineWrap(true);
        txtQuestion.setWrapStyleWord(true);
        txtQuestion.setEditable(false);
        txtQuestion.setFocusable(false);
        txtQuestion.setBorder(new EmptyBorder(10, 0, 10, 0));

        questionCard.add(qHeader, BorderLayout.NORTH);
        questionCard.add(txtQuestion, BorderLayout.CENTER);

        // 4 Kahoot-style Answer Buttons (2x2 grid)
        JPanel answersGrid = new JPanel(new GridLayout(2, 2, 14, 14));
        answersGrid.setOpaque(false);
        answersGrid.setPreferredSize(new Dimension(800, 160));

        answerButtons[0] = new ModernButton("A", ModernButton.Style.ANSWER_A, "▲ A");
        answerButtons[1] = new ModernButton("B", ModernButton.Style.ANSWER_B, "◆ B");
        answerButtons[2] = new ModernButton("C", ModernButton.Style.ANSWER_C, "● C");
        answerButtons[3] = new ModernButton("D", ModernButton.Style.ANSWER_D, "■ D");

        for (int i = 0; i < 4; i++) {
            final int choiceIndex = i;
            answerButtons[i].setFont(Theme.BODY_BOLD);
            answerButtons[i].setCornerRadius(12);
            answerButtons[i].addActionListener(e -> selectAnswer(choiceIndex));
            answersGrid.add(answerButtons[i]);
        }

        JPanel questionArea = new JPanel(new BorderLayout(0, 14));
        questionArea.setOpaque(false);
        questionArea.add(questionCard, BorderLayout.CENTER);
        questionArea.add(answersGrid, BorderLayout.SOUTH);

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

        btnLeave = new ModernButton("🚪 Thoát", ModernButton.Style.DANGER);
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

        // 4.2 Center: Prev & Next Navigation
        JPanel centerNav = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        centerNav.setOpaque(false);

        btnPrev = new ModernButton("◀ Câu trước", ModernButton.Style.SECONDARY);
        btnPrev.addActionListener(e -> {
            SoundManager.playClick();
            navigateToQuestion(currentQuestionIndex - 1);
        });

        btnNext = new ModernButton("Câu tiếp ▶", ModernButton.Style.PRIMARY);
        btnNext.addActionListener(e -> {
            SoundManager.playClick();
            navigateToQuestion(currentQuestionIndex + 1);
        });

        centerNav.add(btnPrev);
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
        // 5. TIMER INITIALIZATION
        // ==========================================
        deadlineNano = System.nanoTime() + (long) this.totalSeconds * 1_000_000_000L;
        matchTimer = new Timer(250, this::onTick);
        matchTimer.start();

        // Render initial question
        renderQuestion();
    }

    private JPanel createPlayerBadge(String name, boolean isMe) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        p.setOpaque(false);

        String initial = (name != null && !name.isEmpty()) ? name.substring(0, 1).toUpperCase() : "?";

        JPanel avatar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setupAntiAliasing(g2);
                g2.setColor(isMe ? Theme.BLUE : Theme.PURPLE);
                g2.fillOval(0, 0, getWidth(), getHeight());

                g2.setColor(Theme.WHITE);
                g2.setFont(Theme.BODY_BOLD);
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(initial)) / 2;
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(initial, x, y);
                g2.dispose();
            }
        };
        avatar.setPreferredSize(new Dimension(32, 32));
        avatar.setOpaque(false);

        JLabel lblName = new JLabel(name + (isMe ? " (Bạn)" : ""));
        lblName.setFont(Theme.BODY_BOLD);
        lblName.setForeground(Theme.WHITE);

        p.add(avatar);
        p.add(lblName);
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

    private void onTick(ActionEvent e) {
        long remainNano = deadlineNano - System.nanoTime();
        long remainSec = Math.max(0, (remainNano + 999_999_999L) / 1_000_000_000L);

        lblTimer.setText(formatTime((int) remainSec));
        timeProgressBar.setValue((int) remainSec);

        // Color coding timer
        if (remainSec <= 10) {
            lblTimer.setForeground(Theme.RED);
            timeProgressBar.setForeground(Theme.RED);
            if (remainSec > 0) {
                SoundManager.playTick();
            }
        } else if (remainSec <= 25) {
            lblTimer.setForeground(Theme.YELLOW);
            timeProgressBar.setForeground(Theme.YELLOW);
        } else {
            lblTimer.setForeground(Theme.WHITE);
            timeProgressBar.setForeground(Theme.GREEN);
        }

        if (remainSec <= 0) {
            matchTimer.stop();
            if (!isSubmitted) {
                doSubmit();
            }
        }
    }

    private String formatTime(int sec) {
        int m = sec / 60;
        int s = sec % 60;
        return String.format("%02d:%02d", m, s);
    }

    public void navigateToQuestion(int index) {
        if (index < 0 || index >= 10 || index == currentQuestionIndex) {
            return;
        }
        currentQuestionIndex = index;
        renderQuestion();
    }

    private void renderQuestion() {
        if (questions == null || currentQuestionIndex >= questions.size()) {
            return;
        }

        MatchQuestion q = questions.get(currentQuestionIndex);
        lblQuestionIndex.setText("CÂU HỎI " + (currentQuestionIndex + 1) + " / 10");
        txtQuestion.setText(q.text());

        String[] choices = q.choices();
        String[] prefixes = { "▲ A", "◆ B", "● C", "■ D" };
        int selected = selectedAnswers[currentQuestionIndex];

        for (int i = 0; i < 4; i++) {
            answerButtons[i].setText(choices[i]);
            answerButtons[i].setPrefixBadge(prefixes[i]);
            answerButtons[i].setSelectedState(selected == i);
            answerButtons[i].setEnabled(!isSubmitted);
        }

        btnPrev.setEnabled(currentQuestionIndex > 0 && !isSubmitted);
        btnNext.setEnabled(currentQuestionIndex < 9 && !isSubmitted);

        // Update Nav buttons and progress count
        int answeredCount = 0;
        for (int i = 0; i < 10; i++) {
            if (selectedAnswers[i] != -1)
                answeredCount++;
            navButtons[i].repaint();
        }
        lblProgressSummary.setText("Đã làm: " + answeredCount + "/10 câu");
    }

    private void selectAnswer(int choiceIndex) {
        if (isSubmitted)
            return;
        SoundManager.playClick();

        if (selectedAnswers[currentQuestionIndex] == choiceIndex) {
            // Click lại cùng đáp án để bỏ chọn
            selectedAnswers[currentQuestionIndex] = -1;
        } else {
            selectedAnswers[currentQuestionIndex] = choiceIndex;
        }

        renderQuestion();
    }

    private void confirmSubmit() {
        if (isSubmitted)
            return;

        int answeredCount = 0;
        for (int ans : selectedAnswers) {
            if (ans != -1)
                answeredCount++;
        }

        String message;
        if (answeredCount < 10) {
            message = "Bạn mới trả lời " + answeredCount + "/10 câu hỏi.\n"
                    + "Còn " + (10 - answeredCount) + " câu chưa hoàn thành.\n"
                    + "Bạn có chắc chắn muốn nộp bài sớm không?";
        } else {
            message = "Bạn đã hoàn thành toàn bộ 10/10 câu hỏi!\n"
                    + "Xác nhận nộp bài thi đấu ngay?";
        }

        int choice = JOptionPane.showConfirmDialog(
                this,
                message,
                "Xác Nhận Nộp Bài",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (choice == JOptionPane.YES_OPTION) {
            doSubmit();
        }
    }

    private void doSubmit() {
        if (isSubmitted)
            return;
        isSubmitted = true;

        // Khóa các controls
        for (ModernButton b : answerButtons) {
            b.setEnabled(false);
        }
        btnSubmit.setEnabled(false);
        btnPrev.setEnabled(false);
        btnNext.setEnabled(false);

        // Hiển thị waiting banner
        waitingBanner.setVisible(true);
        revalidate();
        repaint();

        // Gửi kết quả qua listener
        if (onSubmitListener != null) {
            onSubmitListener.accept(selectedAnswers.clone());
        }
    }

    public void onSubmittedAcknowledged() {
        isSubmitted = true;
        waitingBanner.setVisible(true);
        lblWaitingStatus.setText("✅ ĐÃ GỬI BÀI THÀNH CÔNG! ĐANG CHỜ ĐỐI THỦ HOÀN THÀNH...");
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
        if (matchTimer != null && matchTimer.isRunning()) {
            matchTimer.stop();
        }
    }

    public String getMatchId() {
        return matchId;
    }

    public void setOnEmoteListener(Consumer<String> listener) {
        this.onEmoteListener = listener;
    }

    private void sendEmote(String emote) {
        SoundManager.playEmote();
        lblOpponentEmote.setText("Bạn: " + emote);
        lblOpponentEmote.setVisible(true);
        resetEmoteClearTimer();
        if (onEmoteListener != null) {
            onEmoteListener.accept(emote);
        }
    }

    public void showOpponentEmote(String from, String emote) {
        SoundManager.playEmote();
        lblOpponentEmote.setText(from + ": " + emote);
        lblOpponentEmote.setVisible(true);
        resetEmoteClearTimer();
    }

    private void resetEmoteClearTimer() {
        if (emoteClearTimer != null && emoteClearTimer.isRunning()) {
            emoteClearTimer.stop();
        }
        emoteClearTimer = new Timer(3000, e -> lblOpponentEmote.setVisible(false));
        emoteClearTimer.setRepeats(false);
        emoteClearTimer.start();
    }

    public List<MatchQuestion> getQuestions() {
        return questions;
    }

    public int[] getSelectedAnswers() {
        return selectedAnswers.clone();
    }
}
