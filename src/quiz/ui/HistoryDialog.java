package quiz.ui;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * HistoryDialog - Hộp thoại hiển thị Lịch sử thi đấu của người chơi dưới dạng các thẻ trực quan,
 * phân biệt rõ Thắng / Thua / Hòa và tỉ số chi tiết.
 */
public class HistoryDialog extends JDialog {

    public HistoryDialog(Frame owner, String currentUserName, String rawData) {
        super(owner, "Lịch Sử Thi Đấu — Quiz Arena", true);
        setSize(680, 500);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Theme.OFF_WHITE);

        // 1. HEADER
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(Theme.WHITE);
        header.setBorder(new EmptyBorder(18, 24, 14, 24));

        JLabel title = new JLabel("📜 LỊCH SỬ THI ĐẤU");
        title.setFont(Theme.TITLE);
        title.setForeground(Theme.BLUE);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel desc = new JLabel("Danh sách các trận đối kháng 1v1 bạn đã tham gia gần đây");
        desc.setFont(Theme.SMALL);
        desc.setForeground(Theme.MUTED_GRAY);
        desc.setAlignmentX(Component.LEFT_ALIGNMENT);

        header.add(title);
        header.add(Box.createVerticalStrut(4));
        header.add(desc);
        add(header, BorderLayout.NORTH);

        // 2. LIST OF MATCH CARDS
        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setOpaque(false);
        listPanel.setBorder(new EmptyBorder(12, 20, 12, 20));

        if (rawData == null || rawData.isBlank()) {
            ModernCard emptyCard = new ModernCard(new BorderLayout());
            emptyCard.setCardPadding(30, 20, 30, 20);
            JLabel lblEmpty = new JLabel("Chưa có trận đấu nào được ghi lại.", SwingConstants.CENTER);
            lblEmpty.setFont(Theme.BODY_BOLD);
            lblEmpty.setForeground(Theme.MUTED_GRAY);
            emptyCard.add(lblEmpty, BorderLayout.CENTER);
            listPanel.add(emptyCard);
        } else {
            String[] lines = rawData.split("\n");
            for (String line : lines) {
                if (line.isBlank()) continue;
                listPanel.add(createMatchHistoryCard(currentUserName, line));
                listPanel.add(Box.createVerticalStrut(10));
            }
        }

        JScrollPane scrollPane = new JScrollPane(listPanel);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(Theme.OFF_WHITE);
        scrollPane.getVerticalScrollBar().setUnitIncrement(14);
        add(scrollPane, BorderLayout.CENTER);

        // 3. FOOTER
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 12));
        footer.setBackground(Theme.WHITE);
        ModernButton btnClose = new ModernButton("Đóng", ModernButton.Style.PRIMARY);
        btnClose.setPreferredSize(new Dimension(100, 36));
        btnClose.addActionListener(e -> dispose());
        footer.add(btnClose);
        add(footer, BorderLayout.SOUTH);
    }

    private JPanel createMatchHistoryCard(String currentUser, String line) {
        ModernCard card = new ModernCard(new BorderLayout(12, 0));
        card.setCardPadding(12, 16, 12, 16);
        card.setCornerRadius(10);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 65));

        // Format: topic / level / setId: left leftCorrect–rightCorrect right → winner
        try {
            String[] parts = line.split(":", 2);
            String meta = parts[0].trim();
            String detail = parts.length > 1 ? parts[1].trim() : "";

            String winner = "";
            if (detail.contains("→")) {
                String[] dp = detail.split("→");
                detail = dp[0].trim();
                winner = dp[1].trim();
            }

            // Left Meta
            JPanel left = new JPanel();
            left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
            left.setOpaque(false);

            JLabel lblMeta = new JLabel(meta);
            lblMeta.setFont(Theme.SMALL_BOLD);
            lblMeta.setForeground(Theme.BLUE);

            JLabel lblDetail = new JLabel(detail);
            lblDetail.setFont(Theme.BODY_BOLD);
            lblDetail.setForeground(Theme.DARK_TEXT);

            left.add(lblMeta);
            left.add(Box.createVerticalStrut(2));
            left.add(lblDetail);
            card.add(left, BorderLayout.WEST);

            // Right Outcome Badge
            String outcomeText;
            Color outcomeBg;
            Color outcomeFg;

            if (winner.equalsIgnoreCase("HÒA")) {
                outcomeText = "HÒA";
                outcomeBg = new Color(0xFE, 0xF9, 0xC3);
                outcomeFg = Theme.YELLOW.darker();
            } else if (winner.equalsIgnoreCase(currentUser)) {
                outcomeText = "THẮNG";
                outcomeBg = new Color(0xDC, 0xFD, 0xD7);
                outcomeFg = Theme.GREEN;
            } else {
                outcomeText = "THUA";
                outcomeBg = new Color(0xFF, 0xE4, 0xE8);
                outcomeFg = Theme.RED;
            }

            JLabel badge = new JLabel(outcomeText, SwingConstants.CENTER) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    Theme.setupAntiAliasing(g2);
                    g2.setColor(outcomeBg);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            badge.setFont(Theme.BODY_BOLD);
            badge.setForeground(outcomeFg);
            badge.setPreferredSize(new Dimension(80, 32));

            JPanel badgeWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 4));
            badgeWrapper.setOpaque(false);
            badgeWrapper.add(badge);
            card.add(badgeWrapper, BorderLayout.EAST);

        } catch (Exception e) {
            JLabel lblRaw = new JLabel(line);
            lblRaw.setFont(Theme.BODY);
            card.add(lblRaw, BorderLayout.CENTER);
        }

        return card;
    }
}
