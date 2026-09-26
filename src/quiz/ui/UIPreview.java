package quiz.ui;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * UIPreview - Màn hình showcase kiểm thử toàn bộ các UI Components nền tảng đã xây dựng trong Phase 1.
 */
public class UIPreview {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(UIPreview::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("Quiz Arena — UI Design System Preview (Phase 1)");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(950, 750);
        frame.setLocationRelativeTo(null);
        frame.getContentPane().setBackground(Theme.OFF_WHITE);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setOpaque(false);
        mainPanel.setBorder(new EmptyBorder(20, 24, 20, 24));

        // 1. Header Title
        JLabel title = new JLabel("QUIZ ARENA — DESIGN SYSTEM & COMPONENTS (PHASE 1)");
        title.setFont(Theme.TITLE_XL);
        title.setForeground(Theme.DARK_TEXT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainPanel.add(title);

        JLabel subtitle = new JLabel("Kahoot-Inspired Palette: Blue #1368CE | Purple #864CBF | Red #E21B3C | Yellow #D89E00 | Green #26890C");
        subtitle.setFont(Theme.BODY);
        subtitle.setForeground(Theme.MUTED_GRAY);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainPanel.add(subtitle);
        mainPanel.add(Box.createVerticalStrut(18));

        // 2. Section: Button Styles
        ModernCard btnCard = new ModernCard(new BorderLayout(10, 10));
        btnCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel btnCardTitle = new JLabel("1. Modern Buttons (Action, Accent, Feedback & 4 Battle Answers)");
        btnCardTitle.setFont(Theme.SUBTITLE);
        btnCard.add(btnCardTitle, BorderLayout.NORTH);

        JPanel btnGrid = new JPanel(new GridLayout(2, 5, 10, 10));
        btnGrid.setOpaque(false);

        ModernButton btnPrimary = new ModernButton("Primary (#1368CE)", ModernButton.Style.PRIMARY);
        ModernButton btnSecondary = new ModernButton("Secondary", ModernButton.Style.SECONDARY);
        ModernButton btnAccent = new ModernButton("Accent (#864CBF)", ModernButton.Style.ACCENT_PURPLE);
        ModernButton btnSuccess = new ModernButton("Success (#26890C)", ModernButton.Style.SUCCESS);
        ModernButton btnDanger = new ModernButton("Danger (#E21B3C)", ModernButton.Style.DANGER);

        ModernButton ansA = new ModernButton("Đáp án A - Red", ModernButton.Style.ANSWER_A, "A");
        ModernButton ansB = new ModernButton("Đáp án B - Blue", ModernButton.Style.ANSWER_B, "B");
        ModernButton ansC = new ModernButton("Đáp án C - Yellow", ModernButton.Style.ANSWER_C, "C");
        ModernButton ansD = new ModernButton("Đáp án D - Green", ModernButton.Style.ANSWER_D, "D");
        ModernButton btnDisabled = new ModernButton("Disabled State", ModernButton.Style.PRIMARY);
        btnDisabled.setEnabled(false);

        btnGrid.add(btnPrimary);
        btnGrid.add(btnSecondary);
        btnGrid.add(btnAccent);
        btnGrid.add(btnSuccess);
        btnGrid.add(btnDanger);

        btnGrid.add(ansA);
        btnGrid.add(ansB);
        btnGrid.add(ansC);
        btnGrid.add(ansD);
        btnGrid.add(btnDisabled);

        btnCard.add(btnGrid, BorderLayout.CENTER);
        mainPanel.add(btnCard);
        mainPanel.add(Box.createVerticalStrut(16));

        // 3. Section: Text Fields & Badges
        JPanel row2 = new JPanel(new GridLayout(1, 2, 16, 0));
        row2.setOpaque(false);
        row2.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Inputs Card
        ModernCard inputCard = new ModernCard(new BorderLayout(8, 8));
        JLabel inputCardTitle = new JLabel("2. Inputs & Validation");
        inputCardTitle.setFont(Theme.SUBTITLE);
        inputCard.add(inputCardTitle, BorderLayout.NORTH);

        JPanel inputFields = new JPanel();
        inputFields.setLayout(new BoxLayout(inputFields, BoxLayout.Y_AXIS));
        inputFields.setOpaque(false);

        ModernTextField txtUser = new ModernTextField("Nhập tên đăng nhập...", 15);
        ModernPasswordField txtPass = new ModernPasswordField("Nhập mật khẩu...", 15);
        ModernTextField txtError = new ModernTextField("Trường báo lỗi...", 15);
        txtError.setText("admin_invalid_!");
        txtError.setError(true);

        inputFields.add(new JLabel("Tài khoản:"));
        inputFields.add(txtUser);
        inputFields.add(Box.createVerticalStrut(8));
        inputFields.add(new JLabel("Mật khẩu:"));
        inputFields.add(txtPass);
        inputFields.add(Box.createVerticalStrut(8));
        inputFields.add(new JLabel("Báo lỗi (Border đỏ):"));
        inputFields.add(txtError);

        inputCard.add(inputFields, BorderLayout.CENTER);
        row2.add(inputCard);

        // Badges Card
        ModernCard badgeCard = new ModernCard(new BorderLayout(8, 8));
        JLabel badgeCardTitle = new JLabel("3. Status & Rank Badges");
        badgeCardTitle.setFont(Theme.SUBTITLE);
        badgeCard.add(badgeCardTitle, BorderLayout.NORTH);

        JPanel badgeFlow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        badgeFlow.setOpaque(false);
        badgeFlow.add(new ModernBadge("Đang rỗi", ModernBadge.Type.READY));
        badgeFlow.add(new ModernBadge("Đang đấu", ModernBadge.Type.BUSY));
        badgeFlow.add(new ModernBadge("Điểm: 1,450", ModernBadge.Type.RANK));
        badgeFlow.add(new ModernBadge("Mạng máy tính", ModernBadge.Type.INFO));
        badgeFlow.add(new ModernBadge("Hết giờ", ModernBadge.Type.WARNING));
        badgeCard.add(badgeFlow, BorderLayout.CENTER);
        row2.add(badgeCard);

        mainPanel.add(row2);
        mainPanel.add(Box.createVerticalStrut(16));

        // 4. Section: Toast Notifications Testing
        ModernCard toastCard = new ModernCard(new BorderLayout(8, 8));
        toastCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel toastTitle = new JLabel("4. Toast Notifications Test (Thay thế JTextArea log & JOptionPane)");
        toastTitle.setFont(Theme.SUBTITLE);
        toastCard.add(toastTitle, BorderLayout.NORTH);

        JPanel toastBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6));
        toastBtns.setOpaque(false);

        ModernButton tSuccess = new ModernButton("Bắn Toast Success", ModernButton.Style.SUCCESS);
        tSuccess.addActionListener(e -> ToastManager.success(frame, "Đăng nhập thành công! Chào mừng bạn."));

        ModernButton tError = new ModernButton("Bắn Toast Error", ModernButton.Style.DANGER);
        tError.addActionListener(e -> ToastManager.error(frame, "Tài khoản hoặc mật khẩu không chính xác!"));

        ModernButton tWarning = new ModernButton("Bắn Toast Warning", ModernButton.Style.ANSWER_C);
        tWarning.addActionListener(e -> ToastManager.warning(frame, "Chú ý: Còn 10 giây cuối cùng!"));

        ModernButton tInfo = new ModernButton("Bắn Toast Info", ModernButton.Style.ACCENT_PURPLE);
        tInfo.addActionListener(e -> ToastManager.info(frame, "Alice đã gửi cho bạn một lời mời thách đấu."));

        toastBtns.add(tSuccess);
        toastBtns.add(tError);
        toastBtns.add(tWarning);
        toastBtns.add(tInfo);
        toastCard.add(toastBtns, BorderLayout.CENTER);

        mainPanel.add(toastCard);
        mainPanel.add(Box.createVerticalStrut(16));

        // 5. Section: Phase 5 - In-Game Match & Result Dialog Showcase
        ModernCard phase5Card = new ModernCard(new BorderLayout(8, 8));
        phase5Card.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel p5Title = new JLabel("5. Phase 5: In-Game Battle Screen & Result / Rematch Dialogs");
        p5Title.setFont(Theme.SUBTITLE);
        phase5Card.add(p5Title, BorderLayout.NORTH);

        JPanel p5Btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6));
        p5Btns.setOpaque(false);

        ModernButton btnDemoMatch = new ModernButton("⚔️ Mở MatchScreen Demo", ModernButton.Style.PRIMARY);
        btnDemoMatch.addActionListener(e -> {
            JFrame matchFrame = new JFrame("Demo MatchScreen 1v1 — Quiz Arena");
            matchFrame.setSize(960, 720);
            matchFrame.setLocationRelativeTo(frame);
            java.util.List<MatchQuestion> demoQuestions = new java.util.ArrayList<>();
            for (int i = 1; i <= 10; i++) {
                demoQuestions.add(new MatchQuestion(
                    "Câu hỏi demo số " + i + ": Giao thức nào hoạt động ở tầng ứng dụng trong mô hình TCP/IP?",
                    new String[]{"TCP (Transmission Control Protocol)", "DNS & HTTP", "IP (Internet Protocol)", "Ethernet 802.3"}
                ));
            }
            MatchScreen screen = new MatchScreen(
                "demo-match-id", "Dũng (Bạn)", "Chiến (Đối thủ)", "Mạng máy tính", "Dễ", "1", 60,
                demoQuestions,
                answers -> ToastManager.success(matchFrame, "Đã gửi nộp bài! Đang chờ đối thủ..."),
                matchFrame::dispose
            );
            matchFrame.setContentPane(screen);
            matchFrame.setVisible(true);
        });

        ModernButton btnDemoWin = new ModernButton("🏆 Result Dialog (Thắng)", ModernButton.Style.SUCCESS);
        btnDemoWin.addActionListener(e -> {
            new ResultDialog(
                frame, "alice", "alice",
                "alice: 8 câu, 14200 ms", "bob: 6 câu, 22100 ms",
                () -> ToastManager.info(frame, "Đã gửi yêu cầu tái đấu!"),
                () -> ToastManager.warning(frame, "Đã từ chối tái đấu, về sảnh.")
            ).setVisible(true);
        });

        ModernButton btnDemoLose = new ModernButton("💔 Result Dialog (Thua)", ModernButton.Style.DANGER);
        btnDemoLose.addActionListener(e -> {
            new ResultDialog(
                frame, "alice", "bob",
                "alice: 5 câu, 18500 ms", "bob: 9 câu, 12300 ms",
                () -> ToastManager.info(frame, "Đã gửi yêu cầu tái đấu!"),
                () -> ToastManager.warning(frame, "Đã từ chối tái đấu, về sảnh.")
            ).setVisible(true);
        });

        ModernButton btnDemoDraw = new ModernButton("🤝 Result Dialog (Hòa)", ModernButton.Style.ACCENT_PURPLE);
        btnDemoDraw.addActionListener(e -> {
            new ResultDialog(
                frame, "alice", "HÒA",
                "alice: 7 câu, 16000 ms", "bob: 7 câu, 16200 ms",
                () -> ToastManager.info(frame, "Đã gửi yêu cầu tái đấu!"),
                () -> ToastManager.warning(frame, "Đã từ chối tái đấu, về sảnh.")
            ).setVisible(true);
        });

        p5Btns.add(btnDemoMatch);
        p5Btns.add(btnDemoWin);
        p5Btns.add(btnDemoLose);
        p5Btns.add(btnDemoDraw);
        phase5Card.add(p5Btns, BorderLayout.CENTER);

        mainPanel.add(phase5Card);
        mainPanel.add(Box.createVerticalStrut(16));

        // 6. Section: Phase 6 - Sound Effects & Audio Synthesis
        ModernCard soundCard = new ModernCard(new BorderLayout(8, 8));
        soundCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel soundTitle = new JLabel("6. Phase 6: Sound Effects & Audio Engine (Không phụ thuộc file ngoài)");
        soundTitle.setFont(Theme.SUBTITLE);
        soundCard.add(soundTitle, BorderLayout.NORTH);

        JPanel soundBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        soundBtns.setOpaque(false);

        ModernButton sndClick = new ModernButton("🔊 Click Haptic", ModernButton.Style.SECONDARY);
        sndClick.addActionListener(e -> SoundManager.playClick());

        ModernButton sndTick = new ModernButton("⏱️ Tick Đếm Ngược", ModernButton.Style.SECONDARY);
        sndTick.addActionListener(e -> SoundManager.playTick());

        ModernButton sndInvite = new ModernButton("🔔 Chuông Thách Đấu", ModernButton.Style.PRIMARY);
        sndInvite.addActionListener(e -> SoundManager.playInviteAlert());

        ModernButton sndWin = new ModernButton("🏆 Fanfare Thắng", ModernButton.Style.SUCCESS);
        sndWin.addActionListener(e -> SoundManager.playVictory());

        ModernButton sndLose = new ModernButton("💔 Nhạc Thất Bại", ModernButton.Style.DANGER);
        sndLose.addActionListener(e -> SoundManager.playDefeat());

        ModernButton sndEmote = new ModernButton("✨ Reaction Pop", ModernButton.Style.ACCENT_PURPLE);
        sndEmote.addActionListener(e -> SoundManager.playEmote());

        ModernButton sndToggle = new ModernButton(SoundManager.isMuted() ? "🔇 Đang Tắt Âm" : "🔊 Đang Bật Âm", ModernButton.Style.OUTLINE_GRAY);
        sndToggle.addActionListener(e -> {
            SoundManager.toggleMute();
            sndToggle.setText(SoundManager.isMuted() ? "🔇 Đang Tắt Âm" : "🔊 Đang Bật Âm");
        });

        soundBtns.add(sndClick);
        soundBtns.add(sndTick);
        soundBtns.add(sndInvite);
        soundBtns.add(sndWin);
        soundBtns.add(sndLose);
        soundBtns.add(sndEmote);
        soundBtns.add(sndToggle);
        soundCard.add(soundBtns, BorderLayout.CENTER);

        mainPanel.add(soundCard);

        JScrollPane scroll = new JScrollPane(mainPanel);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        frame.add(scroll);
        frame.setVisible(true);
    }
}
