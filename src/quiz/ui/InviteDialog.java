package quiz.ui;

import java.awt.*;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * InviteDialog - Hộp thoại nhận lời mời thách đấu hiện đại:
 * Hiển thị tên/avatar đối thủ, chủ đề, mức độ, số câu, đồng hồ đếm ngược 30 giây
 * và 2 nút Chấp nhận (Xanh lá) / Từ chối (Đỏ).
 */
public class InviteDialog extends JDialog {

    private final String fromUser;
    private final String topic;
    private final String level;
    private final int durationSec;

    private int remainingSeconds = 30;
    private final Timer countdownTimer;
    private final JLabel lblCountdown;
    private final JProgressBar progressBar;
    private final Consumer<Boolean> onDecisionCallback;

    public InviteDialog(Frame owner, String fromUser, String topic, String level, String durationStr, Consumer<Boolean> onDecision) {
        super(owner, "Lời Mời Thách Đấu — Quiz Arena", true);
        this.fromUser = fromUser;
        this.topic = topic;
        this.level = level;
        this.onDecisionCallback = onDecision;

        int dur = 100;
        try {
            dur = Integer.parseInt(durationStr);
        } catch (NumberFormatException ignored) {}
        this.durationSec = dur;

        setSize(460, 480);
        setLocationRelativeTo(owner);
        setResizable(false);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Theme.WHITE);

        // 1. HEADER
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(Theme.WHITE);
        header.setBorder(new EmptyBorder(22, 24, 10, 24));

        JLabel title = new JLabel("⚔️ LỜI MỜI THÁCH ĐẤU", SwingConstants.CENTER);
        title.setFont(Theme.TITLE);
        title.setForeground(Theme.PURPLE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Bạn nhận được lời mời so tài trực tiếp 1v1", SwingConstants.CENTER);
        subtitle.setFont(Theme.SMALL);
        subtitle.setForeground(Theme.MUTED_GRAY);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        header.add(title);
        header.add(Box.createVerticalStrut(4));
        header.add(subtitle);
        add(header, BorderLayout.NORTH);

        // 2. BODY (Opponent info + Match info + Countdown)
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(10, 28, 10, 28));

        // Card đối thủ
        ModernCard userCard = new ModernCard(new BorderLayout(14, 0));
        userCard.setCardBackground(Theme.OFF_WHITE);
        userCard.setCardPadding(12, 16, 12, 16);
        userCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        JPanel avatar = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setupAntiAliasing(g2);
                g2.setColor(Theme.PURPLE);
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        avatar.setPreferredSize(new Dimension(38, 38));
        avatar.setOpaque(false);
        String letter = (fromUser != null && !fromUser.isEmpty()) ? fromUser.substring(0, 1).toUpperCase() : "?";
        JLabel lblLetter = new JLabel(letter);
        lblLetter.setFont(Theme.TITLE);
        lblLetter.setForeground(Theme.WHITE);
        avatar.add(lblLetter);
        userCard.add(avatar, BorderLayout.WEST);

        JPanel namePanel = new JPanel(new GridLayout(2, 1, 0, 2));
        namePanel.setOpaque(false);
        JLabel lblFrom = new JLabel(fromUser);
        lblFrom.setFont(Theme.SUBTITLE);
        lblFrom.setForeground(Theme.DARK_TEXT);
        JLabel lblRole = new JLabel("Người thách đấu");
        lblRole.setFont(Theme.SMALL);
        lblRole.setForeground(Theme.MUTED_GRAY);
        namePanel.add(lblFrom);
        namePanel.add(lblRole);
        userCard.add(namePanel, BorderLayout.CENTER);

        body.add(userCard);
        body.add(Box.createVerticalStrut(14));

        // Card chi tiết trận đấu
        ModernCard matchCard = new ModernCard(new GridLayout(4, 2, 8, 8));
        matchCard.setCardPadding(14, 18, 14, 18);
        matchCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));

        addDetailRow(matchCard, "Chủ đề:", topic, Theme.BLUE);
        addDetailRow(matchCard, "Mức độ:", level, Theme.PURPLE);
        addDetailRow(matchCard, "Số câu hỏi:", "10 câu", Theme.DARK_TEXT);
        addDetailRow(matchCard, "Thời gian làm bài:", durationSec + " giây", Theme.DARK_TEXT);

        body.add(matchCard);
        body.add(Box.createVerticalStrut(14));

        // Đếm ngược 30 giây
        lblCountdown = new JLabel("Tự động từ chối sau: " + remainingSeconds + "s", SwingConstants.CENTER);
        lblCountdown.setFont(Theme.BODY_BOLD);
        lblCountdown.setForeground(Theme.DARK_TEXT);
        lblCountdown.setAlignmentX(Component.CENTER_ALIGNMENT);
        body.add(lblCountdown);
        body.add(Box.createVerticalStrut(6));

        progressBar = new JProgressBar(0, 30);
        progressBar.setValue(30);
        progressBar.setForeground(Theme.PURPLE);
        progressBar.setPreferredSize(new Dimension(360, 6));
        progressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 6));
        progressBar.setBorder(null);
        body.add(progressBar);

        add(body, BorderLayout.CENTER);

        // 3. FOOTER (Accept / Decline buttons)
        JPanel footer = new JPanel(new GridLayout(1, 2, 14, 0));
        footer.setBackground(Theme.WHITE);
        footer.setBorder(new EmptyBorder(10, 28, 22, 28));

        ModernButton btnDecline = new ModernButton("Từ chối", ModernButton.Style.DANGER);
        btnDecline.setFont(Theme.BODY_BOLD);
        btnDecline.setPreferredSize(new Dimension(160, 44));
        btnDecline.addActionListener(e -> decide(false));

        ModernButton btnAccept = new ModernButton("Chấp nhận", ModernButton.Style.SUCCESS);
        btnAccept.setFont(Theme.BODY_BOLD);
        btnAccept.setPreferredSize(new Dimension(160, 44));
        btnAccept.addActionListener(e -> decide(true));

        footer.add(btnDecline);
        footer.add(btnAccept);
        add(footer, BorderLayout.SOUTH);

        // Khởi động đồng hồ đếm ngược
        countdownTimer = new Timer(1000, e -> {
            remainingSeconds--;
            progressBar.setValue(remainingSeconds);
            lblCountdown.setText("Tự động từ chối sau: " + remainingSeconds + "s");

            if (remainingSeconds <= 10) {
                lblCountdown.setForeground(Theme.RED);
                progressBar.setForeground(Theme.RED);
            }

            if (remainingSeconds <= 0) {
                decide(false); // Hết giờ -> tự động từ chối
            }
        });
        countdownTimer.start();
    }

    private void addDetailRow(JPanel p, String label, String value, Color valColor) {
        JLabel l = new JLabel(label);
        l.setFont(Theme.BODY);
        l.setForeground(Theme.MUTED_GRAY);
        p.add(l);

        JLabel v = new JLabel(value);
        v.setFont(Theme.BODY_BOLD);
        v.setForeground(valColor);
        p.add(v);
    }

    private void decide(boolean accept) {
        if (countdownTimer.isRunning()) {
            countdownTimer.stop();
        }
        dispose();
        if (onDecisionCallback != null) {
            onDecisionCallback.accept(accept);
        }
    }

    @Override
    public void dispose() {
        if (countdownTimer != null && countdownTimer.isRunning()) {
            countdownTimer.stop();
        }
        super.dispose();
    }
}
