package quiz.ui;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * WaitingChallengeDialog - Hộp thoại hiển thị trạng thái đang chờ đối thủ phản hồi lời mời thách đấu,
 * có đồng hồ đếm ngược 30 giây và nút Đóng/Hủy.
 */
public class WaitingChallengeDialog extends JDialog {

    private final String targetUser;
    private int remainingSeconds = 30;
    private final Timer timer;
    private final JLabel lblTimer;
    private final JProgressBar progressBar;

    public WaitingChallengeDialog(Frame owner, String targetUser, String topic, String level) {
        super(owner, "Đang Thách Đấu — Quiz Arena", false);
        this.targetUser = targetUser;

        setSize(420, 360);
        setLocationRelativeTo(owner);
        setResizable(false);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Theme.WHITE);

        // 1. HEADER
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(Theme.WHITE);
        header.setBorder(new EmptyBorder(22, 24, 10, 24));

        JLabel title = new JLabel("⏳ ĐÃ GỬI LỜI MỜI THÁCH ĐẤU", SwingConstants.CENTER);
        title.setFont(Theme.TITLE);
        title.setForeground(Theme.BLUE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Đang chờ " + targetUser + " phản hồi...", SwingConstants.CENTER);
        subtitle.setFont(Theme.BODY);
        subtitle.setForeground(Theme.MUTED_GRAY);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        header.add(title);
        header.add(Box.createVerticalStrut(4));
        header.add(subtitle);
        add(header, BorderLayout.NORTH);

        // 2. BODY
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(10, 28, 10, 28));

        ModernCard infoCard = new ModernCard(new GridLayout(2, 2, 8, 8));
        infoCard.setCardBackground(Theme.OFF_WHITE);
        infoCard.setCardPadding(14, 16, 14, 16);
        infoCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

        JLabel lblTopicTitle = new JLabel("Chủ đề:");
        lblTopicTitle.setForeground(Theme.MUTED_GRAY);
        JLabel lblTopicVal = new JLabel(topic);
        lblTopicVal.setFont(Theme.BODY_BOLD);
        lblTopicVal.setForeground(Theme.BLUE);

        JLabel lblLevelTitle = new JLabel("Mức độ:");
        lblLevelTitle.setForeground(Theme.MUTED_GRAY);
        JLabel lblLevelVal = new JLabel(level);
        lblLevelVal.setFont(Theme.BODY_BOLD);
        lblLevelVal.setForeground(Theme.PURPLE);

        infoCard.add(lblTopicTitle);
        infoCard.add(lblTopicVal);
        infoCard.add(lblLevelTitle);
        infoCard.add(lblLevelVal);
        body.add(infoCard);
        body.add(Box.createVerticalStrut(18));

        lblTimer = new JLabel("Lời mời sẽ hết hạn sau: " + remainingSeconds + "s", SwingConstants.CENTER);
        lblTimer.setFont(Theme.BODY_BOLD);
        lblTimer.setForeground(Theme.DARK_TEXT);
        lblTimer.setAlignmentX(Component.CENTER_ALIGNMENT);
        body.add(lblTimer);
        body.add(Box.createVerticalStrut(8));

        progressBar = new JProgressBar(0, 30);
        progressBar.setValue(30);
        progressBar.setForeground(Theme.BLUE);
        progressBar.setPreferredSize(new Dimension(320, 6));
        progressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 6));
        progressBar.setBorder(null);
        body.add(progressBar);

        add(body, BorderLayout.CENTER);

        // 3. FOOTER
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 16));
        footer.setBackground(Theme.WHITE);

        ModernButton btnCancel = new ModernButton("Đóng / Hủy Chờ", ModernButton.Style.SECONDARY);
        btnCancel.setPreferredSize(new Dimension(160, 40));
        btnCancel.addActionListener(e -> closeDialog());
        footer.add(btnCancel);
        add(footer, BorderLayout.SOUTH);

        // Timer đếm ngược 30 giây
        timer = new Timer(1000, e -> {
            remainingSeconds--;
            progressBar.setValue(remainingSeconds);
            lblTimer.setText("Lời mời sẽ hết hạn sau: " + remainingSeconds + "s");

            if (remainingSeconds <= 10) {
                lblTimer.setForeground(Theme.YELLOW.darker());
                progressBar.setForeground(Theme.YELLOW);
            }

            if (remainingSeconds <= 0) {
                closeDialog();
            }
        });
        timer.start();
    }

    public void closeDialog() {
        if (timer != null && timer.isRunning()) {
            timer.stop();
        }
        dispose();
    }

    public String getTargetUser() {
        return targetUser;
    }
}
