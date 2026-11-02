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

        setSize(480, 520);
        setLocationRelativeTo(owner);
        setResizable(false);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Theme.BG_LIGHT);

        // 1. MODERN HEADER WITH VIBRANT GRADIENT
        JPanel header = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setupAntiAliasing(g2);
                int w = getWidth();
                int h = getHeight();
                GradientPaint gp = new GradientPaint(0, 0, Theme.INDIGO, w, h, Theme.PURPLE);
                g2.setPaint(gp);
                g2.fillRect(0, 0, w, h);

                // Subtle circular ambient deco
                g2.setColor(new Color(255, 255, 255, 20));
                g2.fillOval(-20, -20, 100, 100);
                g2.fillOval(w - 70, h - 50, 120, 120);
                g2.dispose();
            }
        };
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBorder(new EmptyBorder(20, 24, 18, 24));
        header.setPreferredSize(new Dimension(480, 95));

        JLabel title = new JLabel("LỜI MỜI THÁCH ĐẤU 1V1", SwingConstants.CENTER);
        title.setFont(Theme.TITLE);
        title.setForeground(Theme.WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Đối thủ đang đợi bạn bước vào võ đài so tài kiến thức!", SwingConstants.CENTER);
        subtitle.setFont(Theme.SMALL);
        subtitle.setForeground(new Color(255, 255, 255, 220));
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        header.add(title);
        header.add(Box.createVerticalStrut(4));
        header.add(subtitle);
        add(header, BorderLayout.NORTH);

        // 2. BODY (Opponent info + Match info + Countdown)
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(16, 24, 14, 24));

        // Card đối thủ
        ModernCard userCard = new ModernCard(new BorderLayout(14, 0));
        userCard.setCardBackground(Theme.WHITE);
        userCard.setBorderColor(Theme.BORDER_LIGHT);
        userCard.setCardPadding(12, 16, 12, 16);
        userCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 66));

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
        avatar.setPreferredSize(new Dimension(42, 42));
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
        ModernCard matchCard = new ModernCard(new GridLayout(2, 2, 12, 12));
        matchCard.setCardBackground(Theme.WHITE);
        matchCard.setBorderColor(Theme.BORDER_LIGHT);
        matchCard.setCardPadding(14, 18, 14, 18);
        matchCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));

        addDetailCell(matchCard, "Chủ đề", topic, Theme.BLUE);
        addDetailCell(matchCard, "Mức độ", level, Theme.PURPLE);
        addDetailCell(matchCard, "Số lượng", "10 câu hỏi", Theme.DARK_TEXT);
        addDetailCell(matchCard, "Thời gian", durationSec + " giây", Theme.DARK_TEXT);

        body.add(matchCard);
        body.add(Box.createVerticalStrut(16));

        // Đếm ngược 30 giây
        lblCountdown = new JLabel("Tự động từ chối sau: " + remainingSeconds + "s", SwingConstants.CENTER);
        lblCountdown.setFont(Theme.BODY_BOLD);
        lblCountdown.setForeground(Theme.DARK_TEXT);
        lblCountdown.setAlignmentX(Component.CENTER_ALIGNMENT);
        body.add(lblCountdown);
        body.add(Box.createVerticalStrut(8));

        progressBar = new JProgressBar(0, 30);
        progressBar.setValue(30);
        progressBar.setForeground(Theme.PURPLE);
        progressBar.setBackground(new Color(0xE2, 0xE8, 0xF0));
        progressBar.setPreferredSize(new Dimension(380, 8));
        progressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 8));
        progressBar.setBorder(null);
        body.add(progressBar);

        add(body, BorderLayout.CENTER);

        // 3. FOOTER (Accept / Decline buttons)
        JPanel footer = new JPanel(new GridLayout(1, 2, 14, 0));
        footer.setBackground(Theme.WHITE);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER_LIGHT),
                new EmptyBorder(14, 24, 16, 24)));

        ModernButton btnDecline = new ModernButton("Từ Chối", ModernButton.Style.DANGER);
        btnDecline.setFont(Theme.BODY_BOLD);
        btnDecline.setPreferredSize(new Dimension(170, 44));
        btnDecline.addActionListener(e -> decide(false));

        ModernButton btnAccept = new ModernButton("Chấp Nhận", ModernButton.Style.EMERALD);
        btnAccept.setFont(Theme.BODY_BOLD);
        btnAccept.setPreferredSize(new Dimension(170, 44));
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
                if (remainingSeconds == 10) {
                    Toolkit.getDefaultToolkit().beep();
                }
            }

            if (remainingSeconds <= 0) {
                decide(false); // Hết giờ -> tự động từ chối
            }
        });
        countdownTimer.start();
    }

    private void addDetailCell(JPanel p, String label, String value, Color valColor) {
        JPanel cell = new JPanel();
        cell.setLayout(new BoxLayout(cell, BoxLayout.Y_AXIS));
        cell.setOpaque(false);

        JLabel l = new JLabel(label);
        l.setFont(Theme.SMALL_BOLD);
        l.setForeground(Theme.MUTED_GRAY);
        cell.add(l);
        cell.add(Box.createVerticalStrut(2));

        JLabel v = new JLabel(value != null ? value : "-");
        v.setFont(Theme.BODY_BOLD);
        v.setForeground(valColor);
        cell.add(v);

        p.add(cell);
    }

    private void decide(boolean accept) {
        SoundManager.playClick();
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
