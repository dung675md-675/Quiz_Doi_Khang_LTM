package quiz.ui;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * MatchDetailDialog — Hộp thoại hiển thị thông tin tổng quan của trận đấu lịch sử
 * khi không có bộ nhớ cache câu hỏi chi tiết từ phiên thi đấu trước.
 */
public class MatchDetailDialog extends JDialog {

    public MatchDetailDialog(
            Window owner,
            String topic,
            String level,
            String p1Name,
            int p1Score,
            String p2Name,
            int p2Score,
            String winner,
            long durationSec,
            long timestamp,
            boolean isWin,
            boolean isDraw) {

        super(owner, "Chi Tiết Trận Đấu — Quiz Arena", ModalityType.APPLICATION_MODAL);
        setSize(520, 420);
        setLocationRelativeTo(owner);
        setResizable(false);
        setLayout(new BorderLayout());
        getContentPane().setBackground(ArenaPalette.SURFACE);

        // Header
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(isWin ? Theme.EMERALD : (isDraw ? Theme.INDIGO : Theme.RED));
        header.setBorder(new EmptyBorder(18, 22, 16, 22));

        String titleText = isWin ? "CHIẾN THẮNG (+2 ĐIỂM)" : (isDraw ? "TRẬN ĐẤU HÒA (0 ĐIỂM)" : "THẤT BẠI (-1 ĐIỂM)");
        JLabel lblTitle = new JLabel(titleText, SwingConstants.CENTER);
        lblTitle.setFont(Theme.TITLE);
        lblTitle.setForeground(Theme.WHITE);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        String dateStr = (timestamp > 0) ? sdf.format(new Date(timestamp)) : "Gần đây";
        JLabel lblDate = new JLabel("Thời gian: " + dateStr + " • Thời lượng: " + durationSec + "s", SwingConstants.CENTER);
        lblDate.setFont(Theme.SMALL);
        lblDate.setForeground(new Color(255, 255, 255, 220));
        lblDate.setAlignmentX(Component.CENTER_ALIGNMENT);

        header.add(lblTitle);
        header.add(Box.createVerticalStrut(4));
        header.add(lblDate);
        add(header, BorderLayout.NORTH);

        // Center Content
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);
        center.setBorder(new EmptyBorder(16, 24, 12, 24));

        ModernCard vsCard = new ModernCard(new BorderLayout(16, 0));
        vsCard.setCardPadding(16, 18, 16, 18);
        vsCard.setCardBackground(ArenaPalette.BG);
        vsCard.setBorderColor(ArenaPalette.BORDER);

        // Player 1
        JPanel p1 = new JPanel();
        p1.setLayout(new BoxLayout(p1, BoxLayout.Y_AXIS));
        p1.setOpaque(false);
        JLabel lblP1Name = new JLabel(p1Name, SwingConstants.CENTER);
        lblP1Name.setFont(Theme.SUBTITLE);
        lblP1Name.setForeground(ArenaPalette.TEXT);
        lblP1Name.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel lblP1Score = new JLabel(p1Score + "/10 câu", SwingConstants.CENTER);
        lblP1Score.setFont(Theme.TITLE_XL);
        lblP1Score.setForeground(p1Score >= p2Score ? Theme.EMERALD_DARK : ArenaPalette.MUTED);
        lblP1Score.setAlignmentX(Component.CENTER_ALIGNMENT);
        p1.add(lblP1Name);
        p1.add(Box.createVerticalStrut(4));
        p1.add(lblP1Score);

        // VS Center
        JLabel lblVs = new JLabel("VS", SwingConstants.CENTER);
        lblVs.setFont(Theme.TITLE);
        lblVs.setForeground(ArenaPalette.MUTED);

        // Player 2
        JPanel p2 = new JPanel();
        p2.setLayout(new BoxLayout(p2, BoxLayout.Y_AXIS));
        p2.setOpaque(false);
        JLabel lblP2Name = new JLabel(p2Name, SwingConstants.CENTER);
        lblP2Name.setFont(Theme.SUBTITLE);
        lblP2Name.setForeground(ArenaPalette.TEXT);
        lblP2Name.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel lblP2Score = new JLabel(p2Score + "/10 câu", SwingConstants.CENTER);
        lblP2Score.setFont(Theme.TITLE_XL);
        lblP2Score.setForeground(p2Score >= p1Score ? Theme.EMERALD_DARK : ArenaPalette.MUTED);
        lblP2Score.setAlignmentX(Component.CENTER_ALIGNMENT);
        p2.add(lblP2Name);
        p2.add(Box.createVerticalStrut(4));
        p2.add(lblP2Score);

        vsCard.add(p1, BorderLayout.WEST);
        vsCard.add(lblVs, BorderLayout.CENTER);
        vsCard.add(p2, BorderLayout.EAST);
        center.add(vsCard);
        center.add(Box.createVerticalStrut(12));

        // Topic info card
        ModernCard infoCard = new ModernCard(new GridLayout(2, 1, 0, 4));
        infoCard.setCardPadding(10, 16, 10, 16);
        infoCard.setCardBackground(ArenaPalette.SURFACE);
        infoCard.setBorderColor(ArenaPalette.BORDER);

        JLabel lblTopicInfo = new JLabel("Chủ đề: " + topic + " • Độ khó: " + level);
        lblTopicInfo.setFont(Theme.BODY_BOLD);
        lblTopicInfo.setForeground(ArenaPalette.TEXT);

        JLabel lblNotice = new JLabel("Trận đấu từ phiên trước (Chi tiết câu hỏi chỉ lưu khi xem ngay sau trận).");
        lblNotice.setFont(Theme.SMALL);
        lblNotice.setForeground(ArenaPalette.MUTED);

        infoCard.add(lblTopicInfo);
        infoCard.add(lblNotice);
        center.add(infoCard);

        add(center, BorderLayout.CENTER);

        // Bottom
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 18, 12));
        bottom.setBackground(ArenaPalette.SURFACE);
        bottom.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, ArenaPalette.BORDER));

        ModernButton btnClose = new ModernButton("Đóng", ModernButton.Style.PRIMARY);
        btnClose.setArenaStyle(true);
        btnClose.setPreferredSize(new Dimension(110, 36));
        btnClose.addActionListener(e -> dispose());
        bottom.add(btnClose);
        add(bottom, BorderLayout.SOUTH);
        ArenaPalette.styleTree(getContentPane());
    }
}

