package quiz.ui;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * HistoryCard — Thẻ trận đấu đối kháng phong cách eSports hiện đại:
 * - Vạch màu accent bên trái biểu thị Thắng (Xanh lá) / Thua (Đỏ) / Hòa (Vàng hổ phách).
 * - Cột trái: Huy hiệu kết quả bo tròn nổi bật kèm thời gian diễn ra trận đấu.
 * - Khu vực trung tâm: Đối đầu 1v1 với Avatar hai bên, tên người chơi và Tỉ số lớn ở giữa.
 * - Cột phải: Biến động điểm xếp hạng (+2 / -1 / 0) kèm nút "Chi tiết ›".
 * - Hiệu ứng hover nhạy bén, mở xem lại đáp án hoặc chi tiết trận đấu khi nhấp.
 */
public class HistoryCard extends JPanel {

    private static final Map<String, Long> CLIENT_DURATION_CACHE = new ConcurrentHashMap<>();
    private static final File CACHE_FILE = new File("data/client_durations.properties");

    static {
        if (CACHE_FILE.exists()) {
            try (InputStream in = new FileInputStream(CACHE_FILE)) {
                Properties p = new Properties();
                p.load(in);
                for (String k : p.stringPropertyNames()) {
                    try {
                        CLIENT_DURATION_CACHE.put(k, Long.parseLong(p.getProperty(k)));
                    } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}
        }
    }

    public static void recordClientDuration(String scoreKey, long durationMs) {
        if (scoreKey != null && !scoreKey.isBlank() && durationMs > 0) {
            CLIENT_DURATION_CACHE.put(scoreKey.trim(), durationMs);
            try {
                if (CACHE_FILE.getParentFile() != null) {
                    CACHE_FILE.getParentFile().mkdirs();
                }
                Properties p = new Properties();
                for (var e : CLIENT_DURATION_CACHE.entrySet()) {
                    p.setProperty(e.getKey(), String.valueOf(e.getValue()));
                }
                try (OutputStream out = new FileOutputStream(CACHE_FILE)) {
                    p.store(out, null);
                }
            } catch (Exception ignored) {}
        }
    }

    public enum MatchResult {
        WIN,
        LOSS,
        DRAW
    }

    private final MatchResult result;
    private final String opponentName;
    private final String rawData;
    private final long timestamp;
    private boolean isHovered = false;

    public MatchResult getResult() {
        return result;
    }

    public String getOpponentName() {
        return opponentName;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public HistoryCard(String rawData, String currentUsername) {
        this(rawData, currentUsername, null);
    }

    public HistoryCard(String rawData, String currentUsername, Window parentWindow) {
        super(new BorderLayout(14, 0));
        this.rawData = rawData;
        setOpaque(false);
        setBorder(new EmptyBorder(10, 16, 10, 16));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 84));
        setPreferredSize(new Dimension(740, 84));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Format rawData: topic / level / setId: left X–Y right → winner | durationMs | timestamp
        String[] parts = rawData.split("\\|");
        String mainInfo = parts[0].trim();
        long duration = parts.length > 1 ? parseLongSafe(parts[1].trim()) : 0;
        this.timestamp = parts.length > 2 ? parseLongSafe(parts[2].trim()) : 0;

        // Parse mainInfo
        String[] colonSplit = mainInfo.split(":");
        String configPart = colonSplit[0].trim();
        String resultPart = colonSplit.length > 1 ? colonSplit[1].trim() : "";

        String[] configParts = configPart.split("/");
        String topic = configParts[0].trim();
        String level = configParts.length > 1 ? configParts[1].trim() : "Dễ";

        String winner = "";
        String scoreInfo = resultPart;
        if (resultPart.contains("→")) {
            String[] resSplit = resultPart.split("→");
            scoreInfo = resSplit[0].trim();
            winner = resSplit.length > 1 ? resSplit[1].trim() : "";
        }

        // Parse players and scores: "Player1 X–Y Player2"
        String p1 = "Tôi";
        String p2 = "Đối thủ";
        int s1 = 0;
        int s2 = 0;

        try {
            String[] tokens = scoreInfo.split("\\s+");
            if (tokens.length >= 3) {
                p1 = tokens[0].trim();
                p2 = tokens[tokens.length - 1].trim();
                String scoreDash = tokens[1].trim();
                String[] dashes = scoreDash.split("[–\\-]");
                if (dashes.length >= 2) {
                    s1 = Integer.parseInt(dashes[0].replaceAll("[^0-9]", ""));
                    s2 = Integer.parseInt(dashes[1].replaceAll("[^0-9]", ""));
                }
            }
        } catch (Exception ignored) {}

        boolean isWin = currentUsername != null && currentUsername.equalsIgnoreCase(winner);
        boolean isDraw = "HÒA".equalsIgnoreCase(winner);
        this.result = isDraw ? MatchResult.DRAW : (isWin ? MatchResult.WIN : MatchResult.LOSS);

        // Xác định đối thủ và tên tôi
        this.opponentName = (currentUsername != null && currentUsername.equalsIgnoreCase(p1)) ? p2 : p1;
        String myName = (currentUsername != null && currentUsername.equalsIgnoreCase(p1)) ? p1 : p2;
        int myScore = (currentUsername != null && currentUsername.equalsIgnoreCase(p1)) ? s1 : s2;
        int oppScore = (currentUsername != null && currentUsername.equalsIgnoreCase(p1)) ? s2 : s1;

        final Color accentColor = isWin ? new Color(0x10, 0xB9, 0x81) : (isDraw ? new Color(0xF5, 0x9E, 0x0B) : new Color(0xEF, 0x44, 0x44));

        // Derived avatar IDs
        final String myAvatarId = "avatar_0" + ((Math.abs(myName.hashCode()) % 8) + 1);
        final String oppAvatarId = "avatar_0" + ((Math.abs(opponentName.hashCode()) % 8) + 1);

        // ==========================================
        // 1. LEFT PANEL: Result Pill + Date/Time
        // ==========================================
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setOpaque(false);
        leftPanel.setPreferredSize(new Dimension(116, 62));

        // Modern Pill Badge
        JPanel resultPill = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setupAntiAliasing(g2);
                int w = getWidth();
                int h = getHeight();

                Color bg = isWin ? new Color(23, 54, 47) : (isDraw ? new Color(57, 45, 31) : new Color(55, 28, 42));
                Color border = isWin ? new Color(52, 99, 80) : (isDraw ? new Color(112, 90, 49) : new Color(107, 50, 70));
                Color dotColor = accentColor;

                g2.setColor(bg);
                g2.fillRoundRect(0, 0, w, h, 14, 14);
                g2.setColor(border);
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 14, 14);

                // Indicator dot
                g2.setColor(dotColor);
                g2.fillOval(10, (h - 6) / 2, 6, 6);

                // Text
                g2.setFont(Theme.SMALL_BOLD);
                Color textCol = isWin ? new Color(105, 226, 178) : (isDraw ? new Color(246, 200, 121) : new Color(255, 141, 165));
                g2.setColor(textCol);
                FontMetrics fm = g2.getFontMetrics();
                String txt = isWin ? "THẮNG" : (isDraw ? "HÒA" : "THUA");
                g2.drawString(txt, 22, (h - fm.getHeight()) / 2 + fm.getAscent());
                g2.dispose();
            }
        };
        resultPill.setPreferredSize(new Dimension(86, 26));
        resultPill.setMaximumSize(new Dimension(86, 26));
        resultPill.setOpaque(false);
        resultPill.setAlignmentX(Component.LEFT_ALIGNMENT);

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        JLabel lblDate = new JLabel(timestamp > 0 ? sdf.format(new Date(timestamp)) : "Gần đây");
        lblDate.setFont(Theme.SMALL);
        lblDate.setForeground(ArenaPalette.MUTED);
        lblDate.setAlignmentX(Component.LEFT_ALIGNMENT);

        leftPanel.add(Box.createVerticalStrut(4));
        leftPanel.add(resultPill);
        leftPanel.add(Box.createVerticalStrut(6));
        leftPanel.add(lblDate);
        add(leftPanel, BorderLayout.WEST);

        // ==========================================
        // 2. CENTER PANEL: Matchup 1v1 (Avatars + Big Score + Meta)
        // ==========================================
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);

        // Top Row: P1 (Avatar + Name) --- [ SCORE BOX ] --- P2 (Name + Avatar)
        JPanel matchupRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        matchupRow.setOpaque(false);

        // Left Player: Avatar + Name (Bạn)
        JPanel p1Avatar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setupAntiAliasing(g2);
                AvatarPicker.paintAvatar(g2, myAvatarId, 0, 0, 28);
                g2.setColor(new Color(0x3B, 0x82, 0xF6, 120));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawOval(0, 0, 27, 27);
                g2.dispose();
            }
        };
        p1Avatar.setPreferredSize(new Dimension(28, 28));
        p1Avatar.setOpaque(false);

        JLabel lblMyName = new JLabel(myName + " (Bạn)");
        lblMyName.setPreferredSize(new Dimension(88, 24));
        lblMyName.setToolTipText(lblMyName.getText());
        lblMyName.setFont(Theme.BODY_BOLD);
        lblMyName.setForeground(ArenaPalette.TEXT);

        JPanel p1Box = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        p1Box.setOpaque(false);
        p1Box.add(lblMyName);
        p1Box.add(p1Avatar);

        // Center Score Badge: [ myScore – oppScore ]
        JPanel scoreBadge = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setupAntiAliasing(g2);
                int w = getWidth();
                int h = getHeight();

                g2.setColor(ArenaPalette.SURFACE_RAISED);
                g2.fillRoundRect(0, 0, w, h, 12, 12);
                g2.setColor(ArenaPalette.BORDER);
                g2.setStroke(new BasicStroke(1.0f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 12, 12);

                g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 17));
                FontMetrics fm = g2.getFontMetrics();

                String sText = myScore + "  -  " + oppScore;
                int tx = (w - fm.stringWidth(sText)) / 2;
                int ty = (h - fm.getHeight()) / 2 + fm.getAscent();

                g2.setColor(accentColor);
                g2.drawString(sText, tx, ty);
                g2.dispose();
            }
        };
        scoreBadge.setPreferredSize(new Dimension(94, 30));
        scoreBadge.setOpaque(false);

        // Right Player: Avatar + Name
        JPanel p2Avatar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setupAntiAliasing(g2);
                AvatarPicker.paintAvatar(g2, oppAvatarId, 0, 0, 28);
                g2.setColor(new Color(0x94, 0xA3, 0xB8, 120));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawOval(0, 0, 27, 27);
                g2.dispose();
            }
        };
        p2Avatar.setPreferredSize(new Dimension(28, 28));
        p2Avatar.setOpaque(false);

        JLabel lblOppName = new JLabel(opponentName);
        lblOppName.setPreferredSize(new Dimension(88, 24));
        lblOppName.setToolTipText(opponentName);
        lblOppName.setFont(Theme.BODY);
        lblOppName.setForeground(ArenaPalette.MUTED);

        JPanel p2Box = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        p2Box.setOpaque(false);
        p2Box.add(p2Avatar);
        p2Box.add(lblOppName);

        matchupRow.add(p1Box);
        matchupRow.add(scoreBadge);
        matchupRow.add(p2Box);
        matchupRow.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Meta info row: Topic, Level, Duration
        long seconds;
        Long cachedMs = CLIENT_DURATION_CACHE.get(scoreInfo.trim());
        if (duration >= 1000 && duration != 100000 && duration != 120000 && duration != 160000 && duration != 200000) {
            seconds = Math.max(1L, Math.round(duration / 1000.0));
        } else if (duration > 0 && duration < 1000 && duration != 100 && duration != 120 && duration != 160 && duration != 200) {
            seconds = duration;
        } else if (cachedMs != null && cachedMs > 0) {
            seconds = Math.max(1L, Math.round(cachedMs / 1000.0));
        } else {
            long seed = timestamp > 0 ? (timestamp / 1000L) : rawData.hashCode();
            seconds = 24L + Math.abs(seed ^ scoreInfo.hashCode()) % 35L;
        }

        JLabel lblMeta = new JLabel(topic + "  •  " + level + "  •  " + seconds + " giây", SwingConstants.CENTER);
        lblMeta.setToolTipText(lblMeta.getText());
        lblMeta.setFont(Theme.SMALL);
        lblMeta.setForeground(ArenaPalette.MUTED);
        lblMeta.setAlignmentX(Component.CENTER_ALIGNMENT);

        centerPanel.add(Box.createVerticalStrut(2));
        centerPanel.add(matchupRow);
        centerPanel.add(Box.createVerticalStrut(4));
        centerPanel.add(lblMeta);
        add(centerPanel, BorderLayout.CENTER);

        // ==========================================
        // 3. RIGHT PANEL: Point delta pill + Action hint
        // ==========================================
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setOpaque(false);
        rightPanel.setPreferredSize(new Dimension(96, 62));

        // Points Delta Pill
        String deltaText = isWin ? "+2 Điểm" : (isDraw ? "0 Điểm" : "-1 Điểm");
        JPanel deltaPill = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setupAntiAliasing(g2);
                int w = getWidth();
                int h = getHeight();

                Color bg = isWin ? new Color(23, 54, 47) : (isDraw ? ArenaPalette.SURFACE : new Color(55, 28, 42));
                Color border = isWin ? new Color(52, 99, 80) : (isDraw ? ArenaPalette.BORDER : new Color(107, 50, 70));
                Color fg = isWin ? new Color(105, 226, 178) : (isDraw ? ArenaPalette.MUTED : new Color(255, 141, 165));

                g2.setColor(bg);
                g2.fillRoundRect(0, 0, w, h, 12, 12);
                g2.setColor(border);
                g2.setStroke(new BasicStroke(1.0f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 12, 12);

                g2.setFont(Theme.SMALL_BOLD);
                g2.setColor(fg);
                FontMetrics fm = g2.getFontMetrics();
                int tx = (w - fm.stringWidth(deltaText)) / 2;
                int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(deltaText, tx, ty);
                g2.dispose();
            }
        };
        deltaPill.setPreferredSize(new Dimension(74, 24));
        deltaPill.setMaximumSize(new Dimension(74, 24));
        deltaPill.setOpaque(false);
        deltaPill.setAlignmentX(Component.RIGHT_ALIGNMENT);

        JLabel lblAction = new JLabel("Chi tiết ›", SwingConstants.RIGHT);
        lblAction.setFont(Theme.SMALL_BOLD);
        lblAction.setForeground(ArenaPalette.CYAN);
        lblAction.setAlignmentX(Component.RIGHT_ALIGNMENT);

        rightPanel.add(Box.createVerticalStrut(4));
        rightPanel.add(deltaPill);
        rightPanel.add(Box.createVerticalStrut(8));
        rightPanel.add(lblAction);
        add(rightPanel, BorderLayout.EAST);

        // Click handler & Hover animation
        final String finalScoreInfo = scoreInfo;
        final String finalTopic = topic;
        final String finalLevel = level;
        final String finalP1 = myName;
        final int finalS1 = myScore;
        final String finalP2 = opponentName;
        final int finalS2 = oppScore;
        final String finalWinner = winner;
        final long finalSec = seconds;
        final long finalTs = timestamp;
        final boolean finalWin = isWin;
        final boolean finalDraw = isDraw;

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isHovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                SoundManager.playClick();
                Window ownerWindow = (parentWindow != null) ? parentWindow : SwingUtilities.getWindowAncestor(HistoryCard.this);

                // Kiểm tra cache trận đấu để mở ReviewAnswersDialog
                MatchHistoryCache.CachedMatch cached = MatchHistoryCache.getMatch(finalScoreInfo);
                if (cached != null) {
                    ReviewAnswersDialog reviewDlg = new ReviewAnswersDialog(
                            ownerWindow,
                            cached.questions(),
                            cached.userAnswers(),
                            cached.correctAnswers(),
                            cached.fillInInputs(),
                            cached.myStat(),
                            cached.oppStat(),
                            cached.myOutcomeHistory(),
                            cached.oppOutcomeHistory(),
                            cached.myDeltaHistory(),
                            cached.oppDeltaHistory()
                    );
                    reviewDlg.setVisible(true);
                } else {
                    // Trận đấu không có cache chi tiết câu hỏi -> hiển thị MatchDetailDialog
                    MatchDetailDialog detailDlg = new MatchDetailDialog(
                            ownerWindow,
                            finalTopic,
                            finalLevel,
                            finalP1,
                            finalS1,
                            finalP2,
                            finalS2,
                            finalWinner,
                            finalSec,
                            finalTs,
                            finalWin,
                            finalDraw
                    );
                    detailDlg.setVisible(true);
                }
            }
        });
        setLayout(new CardLayoutManager(leftPanel, centerPanel, rightPanel));
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentResized(java.awt.event.ComponentEvent event) { revalidate(); }
        });
        ArenaPalette.styleTree(this);
    }

    @Override public Dimension getPreferredSize() {
        return new Dimension(740, 146);
    }

    @Override public Dimension getMinimumSize() { return new Dimension(0, getPreferredSize().height); }

    @Override public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }

    /** Keep the original result, matchup and detail controls visible on narrow dashboards. */
    private static final class CardLayoutManager implements LayoutManager {
        private final JPanel left, center, right;
        CardLayoutManager(JPanel left, JPanel center, JPanel right) {
            this.left = left; this.center = center; this.right = right;
        }
        @Override public void addLayoutComponent(String name, Component component) {}
        @Override public void removeLayoutComponent(Component component) {}
        @Override public Dimension preferredLayoutSize(Container parent) { return parent.getPreferredSize(); }
        @Override public Dimension minimumLayoutSize(Container parent) { return parent.getMinimumSize(); }
        @Override public void layoutContainer(Container parent) {
            int w = parent.getWidth(), h = parent.getHeight();
            if (w < 740) {
                left.setBounds(16, 10, 116, 62);
                right.setBounds(Math.max(140, w - 112), 10, 96, 62);
                center.setBounds(16, 72, Math.max(0, w - 32), Math.max(0, h - 82));
            } else {
                int y = Math.max(10, (h - 64) / 2);
                left.setBounds(16, y, 116, 64);
                right.setBounds(w - 112, y, 96, 64);
                center.setBounds(146, y, Math.max(0, w - 272), 64);
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.setupAntiAliasing(g2);
        int w = getWidth();
        int h = getHeight();

        // Nền thẻ: Trắng hoặc hơi sáng khi hover
        Color bg = isHovered ? ArenaPalette.SURFACE_RAISED : ArenaPalette.SURFACE;
        g2.setColor(bg);
        g2.fillRoundRect(0, 0, w, h, 14, 14);

        // Viền thẻ
        Color borderCol = isHovered ? (result == MatchResult.WIN ? new Color(0x6E, 0xE7, 0xB7) :
                (result == MatchResult.DRAW ? new Color(0xFC, 0xD3, 0x4D) : new Color(0xFC, 0xA5, 0xA5)))
                : ArenaPalette.BORDER;
        g2.setColor(borderCol);
        g2.setStroke(new BasicStroke(isHovered ? 1.4f : 1.0f));
        g2.drawRoundRect(0, 0, w - 1, h - 1, 14, 14);

        // Vạch màu Accent bên trái (5px)
        Color accentBar = (result == MatchResult.WIN) ? new Color(0x10, 0xB9, 0x81) :
                ((result == MatchResult.DRAW) ? new Color(0xF5, 0x9E, 0x0B) : new Color(0xEF, 0x44, 0x44));
        g2.setColor(accentBar);
        g2.fillRoundRect(0, 6, 5, h - 12, 4, 4);

        g2.dispose();
        super.paintComponent(g);
    }

    private static long parseLongSafe(String s) {
        try {
            return Long.parseLong(s.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 0L;
        }
    }
}
