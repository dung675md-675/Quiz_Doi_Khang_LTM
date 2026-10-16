package quiz.ui;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.text.SimpleDateFormat;
import java.util.Date;

public class HistoryCard extends ModernCard {

    public HistoryCard(String rawData, String currentUsername) {
        super(new BorderLayout(14, 0));
        setCardBackground(Theme.WHITE);
        setBorderColor(Theme.BORDER_LIGHT);
        setCornerRadius(10);
        setCardPadding(12, 16, 12, 16);
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        // Format: topic / level / setId: left X–Y right → winner | durationMs | timestamp
        String[] parts = rawData.split("\\|");
        String mainInfo = parts[0].trim();
        long duration = parts.length > 1 ? Long.parseLong(parts[1].trim()) : 0;
        long timestamp = parts.length > 2 ? Long.parseLong(parts[2].trim()) : 0;

        // Parse mainInfo
        String[] colonSplit = mainInfo.split(":");
        String configPart = colonSplit[0].trim();
        String resultPart = colonSplit.length > 1 ? colonSplit[1].trim() : "";

        String[] configParts = configPart.split("/");
        String topic = configParts[0].trim();
        String level = configParts.length > 1 ? configParts[1].trim() : "";

        String winner = "";
        String scoreInfo = resultPart;
        if (resultPart.contains("→")) {
            String[] resSplit = resultPart.split("→");
            scoreInfo = resSplit[0].trim();
            winner = resSplit.length > 1 ? resSplit[1].trim() : "";
        }

        boolean isWin = currentUsername.equals(winner);
        boolean isDraw = winner.equals("HÒA");

        // LEFT: Status Badge + Date
        JPanel leftPanel = new JPanel(new GridLayout(2, 1, 0, 4));
        leftPanel.setOpaque(false);
        leftPanel.setPreferredSize(new Dimension(100, 50));
        
        ModernBadge.Type type = isDraw ? ModernBadge.Type.WARNING : (isWin ? ModernBadge.Type.READY : ModernBadge.Type.BUSY);
        String statusText = isDraw ? "HÒA" : (isWin ? "THẮNG" : "THUA");
        ModernBadge badge = new ModernBadge(statusText, type);
        
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        JLabel lblDate = new JLabel(timestamp > 0 ? sdf.format(new Date(timestamp)) : "");
        lblDate.setFont(Theme.SMALL);
        lblDate.setForeground(Theme.MUTED_GRAY);
        
        leftPanel.add(badge);
        leftPanel.add(lblDate);
        add(leftPanel, BorderLayout.WEST);

        // CENTER: Topic & Level
        JPanel centerPanel = new JPanel(new GridLayout(2, 1, 0, 4));
        centerPanel.setOpaque(false);
        JLabel lblTopic = new JLabel(topic);
        lblTopic.setFont(Theme.BODY_BOLD);
        lblTopic.setForeground(Theme.DARK_TEXT);
        JLabel lblLevel = new JLabel(level + " • " + (duration/1000) + "s");
        lblLevel.setFont(Theme.SMALL);
        lblLevel.setForeground(Theme.MUTED_GRAY);
        centerPanel.add(lblTopic);
        centerPanel.add(lblLevel);
        add(centerPanel, BorderLayout.CENTER);

        // RIGHT: Score
        JLabel lblScore = new JLabel(scoreInfo, SwingConstants.RIGHT);
        lblScore.setFont(Theme.SUBTITLE);
        lblScore.setForeground(Theme.BLUE);
        add(lblScore, BorderLayout.EAST);
    }
}
