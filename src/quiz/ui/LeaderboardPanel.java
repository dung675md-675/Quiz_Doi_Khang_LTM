package quiz.ui;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import quiz.Rank;

/**
 * LeaderboardPanel — Giao diện Bảng Xếp Hạng cao thủ đối kháng Esports hiện đại & rực rỡ:
 * - Bục vinh danh Top 3 (Podium Top 1 - 2 - 3) hiệu ứng Esports cao cấp với vương miện Java2D,
 *   hào quang ánh sáng (Sunburst glow), khung avatar kim loại quý và bục 3D đa sắc.
 * - Bảng xếp hạng bên dưới với huy hiệu huy chương #1/#2/#3, avatar người chơi, huy hiệu Rank gradient nhiều màu.
 * - Highlight dòng của người chơi hiện tại với thanh chỉ báo rực rỡ.
 * - Thanh thứ hạng cá nhân (Sticky "My Rank" Bar) ở đáy bảng với thông báo khoảng cách điểm (HOÀN TOÀN KHÔNG DÙNG EMOJI).
 * - Bộ lọc nhanh: [Toàn thời gian] / [Tuần này].
 */
public class LeaderboardPanel extends JPanel {

    public static class PlayerRankEntry {
        public final int rank;
        public final String name;
        public final int score;
        public final String avgOpponent;
        public final String speed;
        public final String totalMatches;
        public final String winRate;
        public final String rankLabel;
        public final String avatarId;

        public PlayerRankEntry(int rank, String name, int score, String avgOpponent, String speed, String totalMatches, String winRate, String rankLabel, String avatarId) {
            this.rank = rank;
            this.name = name;
            this.score = score;
            this.avgOpponent = avgOpponent;
            this.speed = speed;
            this.totalMatches = totalMatches;
            this.winRate = winRate;
            this.rankLabel = rankLabel;
            this.avatarId = avatarId != null ? avatarId : ("avatar_0" + ((Math.abs(name.hashCode()) % 12) + 1));
        }
    }

    private final JTable table;
    private final DefaultTableModel model;
    private final JScrollPane scrollPane;
    private final PodiumPanel podiumPanel;
    private final StickyMyRankBar myRankBar;

    private final ModernButton btnFilterAll;
    private final ModernButton btnFilterWeekly;
    private boolean isWeeklyFilter = false;

    private final List<PlayerRankEntry> allEntries = new ArrayList<>();
    private String currentUsername = "";

    public LeaderboardPanel() {
        setLayout(new BorderLayout(0, 10));
        setOpaque(false);
        setBorder(new EmptyBorder(12, 18, 12, 18));

        // ==========================================
        // 1. TOP CONTAINER: HEADER & PODIUM TOP 1-2-3
        // ==========================================
        JPanel topContainer = new JPanel();
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
        topContainer.setOpaque(false);

        // Header Title Row + Filter Buttons
        JPanel headerRow = new JPanel(new BorderLayout(12, 0));
        headerRow.setOpaque(false);

        JLabel title = new JLabel("BẢNG XẾP HẠNG CAO THỦ ĐỐI KHÁNG");
        title.setFont(Theme.TITLE);
        title.setForeground(Theme.BLUE);
        headerRow.add(title, BorderLayout.WEST);

        // Filter: [Toàn thời gian] [Tuần này]
        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        filterRow.setOpaque(false);

        btnFilterAll = new ModernButton("Toàn thời gian", ModernButton.Style.PRIMARY);
        btnFilterAll.setPreferredSize(new Dimension(120, 30));
        btnFilterAll.setFont(Theme.SMALL_BOLD);
        btnFilterAll.addActionListener(e -> {
            isWeeklyFilter = false;
            updateFilterButtons();
            populateData();
        });

        btnFilterWeekly = new ModernButton("Tuần này", ModernButton.Style.OUTLINE_GRAY);
        btnFilterWeekly.setPreferredSize(new Dimension(100, 30));
        btnFilterWeekly.setFont(Theme.SMALL_BOLD);
        btnFilterWeekly.addActionListener(e -> {
            isWeeklyFilter = true;
            updateFilterButtons();
            populateData();
        });

        filterRow.add(btnFilterAll);
        filterRow.add(btnFilterWeekly);
        headerRow.add(filterRow, BorderLayout.EAST);

        topContainer.add(headerRow);
        topContainer.add(Box.createVerticalStrut(8));

        // Bục vinh danh Top 3 (Podium)
        podiumPanel = new PodiumPanel();
        topContainer.add(podiumPanel);
        topContainer.add(Box.createVerticalStrut(10));

        add(topContainer, BorderLayout.NORTH);

        // ==========================================
        // 2. TABLE LEADERBOARD BÊN DƯỚI
        // ==========================================
        String[] columnNames = { "Hạng", "Người chơi", "Bậc Rank", "Điểm số", "Tổng trận", "Tỉ lệ thắng" };
        model = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(model);
        table.setRowHeight(44);
        table.setFont(Theme.BODY);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFillsViewportHeight(true);

        table.getTableHeader().setFont(Theme.BODY_BOLD);
        table.getTableHeader().setBackground(new Color(0xF1, 0xF5, 0xF9));
        table.getTableHeader().setForeground(Theme.DARK_TEXT);
        table.getTableHeader().setPreferredSize(new Dimension(0, 38));

        // Setup Custom Renderers
        setupTableRenderers();

        scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, Theme.BORDER_LIGHT));
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(18);
        scrollPane.getVerticalScrollBar().setUI(new ModernScrollBarUI());
        add(scrollPane, BorderLayout.CENTER);

        // ==========================================
        // 3. STICKY "MY RANK" BAR Ở ĐÁY BẢNG
        // ==========================================
        myRankBar = new StickyMyRankBar();
        add(myRankBar, BorderLayout.SOUTH);
    }

    private void updateFilterButtons() {
        btnFilterAll.setStyle(isWeeklyFilter ? ModernButton.Style.OUTLINE_GRAY : ModernButton.Style.PRIMARY);
        btnFilterWeekly.setStyle(isWeeklyFilter ? ModernButton.Style.PRIMARY : ModernButton.Style.OUTLINE_GRAY);
        btnFilterAll.repaint();
        btnFilterWeekly.repaint();
    }

    private void setupTableRenderers() {
        // Cột 0: Hạng (Rank column with Medallion badges)
        table.getColumnModel().getColumn(0).setPreferredWidth(85);
        table.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                JPanel cell = new JPanel(new GridBagLayout()) {
                    @Override
                    protected void paintComponent(Graphics g) {
                        super.paintComponent(g);
                        Graphics2D g2 = (Graphics2D) g.create();
                        Theme.setupAntiAliasing(g2);
                        int w = getWidth();
                        int h = getHeight();
                        int pillW = 56;
                        int pillH = 26;
                        int px = (w - pillW) / 2;
                        int py = (h - pillH) / 2;

                        if (row == 0) {
                            // Gold Medallion #1
                            g2.setPaint(new GradientPaint(px, py, new Color(0xFB, 0xBF, 0x24), px + pillW, py + pillH, new Color(0xD9, 0x77, 0x06)));
                            g2.fillRoundRect(px, py, pillW, pillH, 12, 12);
                            g2.setColor(Theme.WHITE);
                            g2.setFont(Theme.SMALL_BOLD);
                            drawCenteredText(g2, "TOP 1", px, py, pillW, pillH);
                        } else if (row == 1) {
                            // Silver Medallion #2
                            g2.setPaint(new GradientPaint(px, py, new Color(0xCB, 0xD5, 0xE1), px + pillW, py + pillH, new Color(0x64, 0x74, 0x8B)));
                            g2.fillRoundRect(px, py, pillW, pillH, 12, 12);
                            g2.setColor(Theme.WHITE);
                            g2.setFont(Theme.SMALL_BOLD);
                            drawCenteredText(g2, "TOP 2", px, py, pillW, pillH);
                        } else if (row == 2) {
                            // Bronze Medallion #3
                            g2.setPaint(new GradientPaint(px, py, new Color(0xFD, 0xBA, 0x74), px + pillW, py + pillH, new Color(0xC2, 0x41, 0x0C)));
                            g2.fillRoundRect(px, py, pillW, pillH, 12, 12);
                            g2.setColor(Theme.WHITE);
                            g2.setFont(Theme.SMALL_BOLD);
                            drawCenteredText(g2, "TOP 3", px, py, pillW, pillH);
                        } else {
                            // Normal Rank Badge #4+
                            g2.setColor(new Color(0xF1, 0xF5, 0xF9));
                            g2.fillRoundRect(px + 6, py, pillW - 12, pillH, 10, 10);
                            g2.setColor(Theme.MUTED_GRAY);
                            g2.setFont(Theme.SMALL_BOLD);
                            drawCenteredText(g2, "#" + (row + 1), px + 6, py, pillW - 12, pillH);
                        }
                        g2.dispose();
                    }

                    private void drawCenteredText(Graphics2D g2, String text, int x, int y, int w, int h) {
                        FontMetrics fm = g2.getFontMetrics();
                        int tx = x + (w - fm.stringWidth(text)) / 2;
                        int ty = y + (h - fm.getHeight()) / 2 + fm.getAscent();
                        g2.drawString(text, tx, ty);
                    }
                };
                cell.setOpaque(false);
                return cell;
            }
        });

        // Cột 1: Người chơi (Avatar + Name)
        table.getColumnModel().getColumn(1).setPreferredWidth(230);
        table.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                JPanel cell = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
                cell.setOpaque(false);

                if (row < allEntries.size()) {
                    PlayerRankEntry entry = allEntries.get(row);

                    // Mini Avatar (28x28)
                    JPanel avatar = new JPanel() {
                        @Override
                        protected void paintComponent(Graphics g) {
                            super.paintComponent(g);
                            Graphics2D g2 = (Graphics2D) g.create();
                            Theme.setupAntiAliasing(g2);
                            AvatarPicker.paintAvatar(g2, entry.avatarId, 0, 0, 26);
                            g2.dispose();
                        }
                    };
                    avatar.setPreferredSize(new Dimension(26, 26));
                    avatar.setOpaque(false);
                    cell.add(avatar);

                    boolean isMe = entry.name.equalsIgnoreCase(currentUsername);
                    JLabel lblName = new JLabel(entry.name + (isMe ? " (Bạn)" : ""));
                    lblName.setFont(isMe ? Theme.BODY_BOLD : Theme.BODY);
                    lblName.setForeground(isMe ? Theme.BLUE : Theme.DARK_TEXT);
                    cell.add(lblName);
                }
                return cell;
            }
        });

        // Cột 2: Bậc Rank (Vibrant Pill Badge)
        table.getColumnModel().getColumn(2).setPreferredWidth(140);
        table.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                JPanel cell = new JPanel(new GridBagLayout()) {
                    @Override
                    protected void paintComponent(Graphics g) {
                        super.paintComponent(g);
                        Graphics2D g2 = (Graphics2D) g.create();
                        Theme.setupAntiAliasing(g2);
                        int w = getWidth();
                        int h = getHeight();
                        int pillW = Math.min(w - 16, 110);
                        int pillH = 26;
                        int px = (w - pillW) / 2;
                        int py = (h - pillH) / 2;

                        String rankText = (val != null) ? val.toString() : "Sắt";
                        Color rankColor = getRankColor(rankText);

                        // Pill background with soft pastel tone
                        g2.setColor(new Color(rankColor.getRed(), rankColor.getGreen(), rankColor.getBlue(), 30));
                        g2.fillRoundRect(px, py, pillW, pillH, 12, 12);
                        g2.setColor(new Color(rankColor.getRed(), rankColor.getGreen(), rankColor.getBlue(), 120));
                        g2.setStroke(new BasicStroke(1.2f));
                        g2.drawRoundRect(px, py, pillW, pillH, 12, 12);

                        // Pill text
                        g2.setColor(rankColor);
                        g2.setFont(Theme.SMALL_BOLD);
                        FontMetrics fm = g2.getFontMetrics();
                        int tx = px + (pillW - fm.stringWidth(rankText)) / 2;
                        int ty = py + (pillH - fm.getHeight()) / 2 + fm.getAscent();
                        g2.drawString(rankText, tx, ty);
                        g2.dispose();
                    }
                };
                cell.setOpaque(false);
                return cell;
            }
        });

        // Cột 3: Điểm số (Vibrant Points Chip)
        table.getColumnModel().getColumn(3).setPreferredWidth(120);
        table.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                setHorizontalAlignment(SwingConstants.CENTER);
                setFont(Theme.BODY_BOLD);
                setForeground(Theme.PURPLE);
                return c;
            }
        });

        // Cột 4 & 5: Tổng trận & Tỉ lệ thắng
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(5).setCellRenderer(centerRenderer);

        // Highlight dòng của chính người chơi (Zebra + Glowing Left Notch)
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                if (row < allEntries.size()) {
                    PlayerRankEntry entry = allEntries.get(row);
                    if (entry.name.equalsIgnoreCase(currentUsername)) {
                        c.setBackground(new Color(0xEE, 0xF2, 0xFF)); // Light Indigo
                    } else {
                        c.setBackground(row % 2 == 0 ? Theme.WHITE : new Color(0xF8, 0xFA, 0xFC));
                    }
                }
                return c;
            }
        });
    }

    public static Color getRankColor(String rankLabel) {
        if (rankLabel == null) return Theme.MUTED_GRAY;
        String r = rankLabel.toLowerCase(Locale.ROOT);
        if (r.contains("thách đấu") || r.contains("challenger")) return new Color(0xDC, 0x26, 0x26);
        if (r.contains("cao thủ") || r.contains("master")) return new Color(0x7C, 0x3A, 0xED);
        if (r.contains("kim cương") || r.contains("diamond")) return new Color(0x02, 0x84, 0xC7);
        if (r.contains("vàng") || r.contains("gold")) return new Color(0xD9, 0x77, 0x06);
        if (r.contains("bạc") || r.contains("silver")) return new Color(0x64, 0x74, 0x8B);
        if (r.contains("đồng") || r.contains("bronze")) return new Color(0xB4, 0x53, 0x09);
        return new Color(0x64, 0x74, 0x8B);
    }

    public void updateData(String rawData, String currentUsername) {
        this.currentUsername = currentUsername != null ? currentUsername : "";
        allEntries.clear();

        if (rawData != null && !rawData.isBlank()) {
            String[] rows = rawData.split("\\|");
            int rank = 1;
            for (String r : rows) {
                if (r.isBlank()) continue;
                String[] f = r.split(";");
                if (f.length >= 2) {
                    String name = f[0].trim();
                    int score = parseScore(f[1]);
                    String avgOpp = f.length >= 3 ? f[2].trim() : "—";
                    String speed = f.length >= 4 ? f[3].trim() : "—";
                    String totalMatches = f.length >= 5 ? f[4].trim() : "—";
                    String winRate = f.length >= 6 ? f[5].trim() : "—";
                    String rankLabel = f.length >= 7 ? f[6].trim() : Rank.fromScore(score).getDisplayName();
                    String avatarId = f.length >= 8 ? f[7].trim() : null;

                    allEntries.add(new PlayerRankEntry(rank++, name, score, avgOpp, speed, totalMatches, winRate, rankLabel, avatarId));
                }
            }
        }

        populateData();
    }

    private void populateData() {
        model.setRowCount(0);

        // Cập nhật bục Top 1-2-3
        PlayerRankEntry top1 = allEntries.size() > 0 ? allEntries.get(0) : null;
        PlayerRankEntry top2 = allEntries.size() > 1 ? allEntries.get(1) : null;
        PlayerRankEntry top3 = allEntries.size() > 2 ? allEntries.get(2) : null;
        podiumPanel.updatePodium(top1, top2, top3, currentUsername);

        // Điền dữ liệu vào bảng
        for (PlayerRankEntry e : allEntries) {
            String rankText = switch (e.rank) {
                case 1 -> "TOP 1";
                case 2 -> "TOP 2";
                case 3 -> "TOP 3";
                default -> "#" + e.rank;
            };
            model.addRow(new Object[]{
                    rankText,
                    e.name + (e.name.equalsIgnoreCase(currentUsername) ? " (Bạn)" : ""),
                    e.rankLabel,
                    e.score + " pts",
                    e.totalMatches,
                    e.winRate
            });
        }

        // Cập nhật thanh Sticky "My Rank" Bar
        myRankBar.updateMyRank(allEntries, currentUsername);

        table.revalidate();
        table.repaint();
    }

    private int parseScore(String s) {
        try {
            return Integer.parseInt(s.replaceAll("[^0-9-]", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    // ==========================================
    // KHU VỰC VINH DANH TOP 3 (PODIUM PANEL ESPORTS)
    // ==========================================
    private static class PodiumPanel extends JPanel {
        private PlayerRankEntry top1;
        private PlayerRankEntry top2;
        private PlayerRankEntry top3;
        private String currentUsername = "";

        public PodiumPanel() {
            setPreferredSize(new Dimension(640, 235));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 235));
            setOpaque(false);
        }

        public void updatePodium(PlayerRankEntry top1, PlayerRankEntry top2, PlayerRankEntry top3, String currentUsername) {
            this.top1 = top1;
            this.top2 = top2;
            this.top3 = top3;
            this.currentUsername = currentUsername != null ? currentUsername : "";
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.setupAntiAliasing(g2);
            int w = getWidth();
            int h = getHeight();
            int cx = w / 2;

            // Nền Card chứa Podium với hiệu ứng gradient hiện đại
            RoundRectangle2D bgCard = new RoundRectangle2D.Float(0, 0, w, h, 16, 16);
            g2.setPaint(new GradientPaint(0, 0, new Color(0xF8, 0xFA, 0xFC), 0, h, new Color(0xEE, 0xF2, 0xFF)));
            g2.fill(bgCard);
            g2.setColor(new Color(0xCB, 0xD5, 0xE1));
            g2.setStroke(new BasicStroke(1.2f));
            g2.draw(bgCard);

            int baselineY = h - 12;
            int p1X = cx;
            int p2X = cx - 165;
            int p3X = cx + 165;

            // Vẽ Bục Top 2 (Bạc Silver Cyber)
            drawPodiumColumn(g2, p2X, baselineY, 74, 130, 2, top2, new Color(0xE2, 0xE8, 0xF0), new Color(0x94, 0xA3, 0xB8), new Color(0x64, 0x74, 0x8B));

            // Vẽ Bục Top 3 (Đồng Bronze Metallic)
            drawPodiumColumn(g2, p3X, baselineY, 54, 130, 3, top3, new Color(0xFD, 0xBA, 0x74), new Color(0xC2, 0x41, 0x0C), new Color(0x9A, 0x34, 0x12));

            // Vẽ Bục Top 1 (Vàng Gold Radiant - cao nhất)
            drawPodiumColumn(g2, p1X, baselineY, 96, 146, 1, top1, new Color(0xFB, 0xBF, 0x24), new Color(0xD9, 0x77, 0x06), new Color(0xB4, 0x53, 0x09));

            g2.dispose();
        }

        private void drawPodiumColumn(Graphics2D g2, int centerX, int baselineY, int pedestalH, int colW, int rankPos, PlayerRankEntry player, Color startC, Color endC, Color borderC) {
            int pedW = colW;
            int pedX = centerX - pedW / 2;
            int pedY = baselineY - pedestalH;

            // 1. Bục Pedestal 3D Gradient
            RoundRectangle2D pedRect = new RoundRectangle2D.Float(pedX, pedY, pedW, pedestalH, 12, 12);
            g2.setPaint(new GradientPaint(pedX, pedY, startC, pedX, pedY + pedestalH, endC));
            g2.fill(pedRect);
            g2.setColor(borderC);
            g2.setStroke(new BasicStroke(1.5f));
            g2.draw(pedRect);

            // Số thứ hạng lớn trên mặt bục (1, 2, 3)
            g2.setColor(new Color(255, 255, 255, 240));
            g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, rankPos == 1 ? 30 : 24));
            FontMetrics fmNum = g2.getFontMetrics();
            String numStr = String.valueOf(rankPos);
            int tx = centerX - fmNum.stringWidth(numStr) / 2;
            int ty = pedY + (pedestalH + fmNum.getAscent() - fmNum.getDescent()) / 2;
            g2.drawString(numStr, tx, ty);

            // Nếu chưa có đấu thủ
            if (player == null) {
                g2.setColor(Theme.MUTED_GRAY);
                g2.setFont(Theme.SMALL_BOLD);
                String waitingStr = "Chờ cao thủ";
                FontMetrics fmW = g2.getFontMetrics();
                g2.drawString(waitingStr, centerX - fmW.stringWidth(waitingStr) / 2, pedY - 14);
                return;
            }

            // 2. Avatar & Vương Miện bên trên bục
            int avSize = (rankPos == 1) ? 56 : 46;
            int avY = pedY - avSize - 24;
            int avX = centerX - avSize / 2;

            // Hào quang tỏa sáng cho Top 1 (Sunburst Glow Java2D)
            if (rankPos == 1) {
                g2.setColor(new Color(255, 235, 120, 50));
                g2.fillOval(centerX - 42, avY - 12, 84, 84);
                g2.setColor(new Color(255, 220, 80, 70));
                g2.fillOval(centerX - 34, avY - 4, 68, 68);
            }

            // Vẽ Vương Miện Java2D (KHÔNG DÙNG EMOJI)
            drawCrown(g2, centerX, avY - 10, rankPos);

            // Vẽ Avatar
            AvatarPicker.paintAvatar(g2, player.avatarId, avX, avY, avSize);

            // Viền kim loại Avatar
            g2.setColor(borderC);
            g2.setStroke(new BasicStroke(rankPos == 1 ? 3.5f : 2.5f));
            g2.drawOval(avX, avY, avSize, avSize);

            // 3. Thông tin người chơi (Tên, Điểm, Rank)
            g2.setFont(Theme.BODY_BOLD);
            boolean isMe = player.name.equalsIgnoreCase(currentUsername);
            g2.setColor(isMe ? Theme.BLUE : Theme.DARK_TEXT);
            FontMetrics fmName = g2.getFontMetrics();
            String nameStr = player.name + (isMe ? " (Bạn)" : "");
            if (nameStr.length() > 14) nameStr = nameStr.substring(0, 12) + "...";
            g2.drawString(nameStr, centerX - fmName.stringWidth(nameStr) / 2, pedY - 14);

            g2.setFont(Theme.SMALL_BOLD);
            g2.setColor(rankPos == 1 ? new Color(0xB4, 0x53, 0x09) : Theme.TEXT_BODY);
            FontMetrics fmPts = g2.getFontMetrics();
            String ptsStr = player.score + " pts";
            g2.drawString(ptsStr, centerX - fmPts.stringWidth(ptsStr) / 2, pedY - 2);
        }

        private void drawCrown(Graphics2D g2, int cx, int cy, int rankPos) {
            if (rankPos == 1) {
                // Vương miện Vàng Quán Quân 5 đỉnh với gradient & ngọc đỏ/xanh
                Path2D.Float crown = new Path2D.Float();
                crown.moveTo(cx - 15, cy + 4);
                crown.lineTo(cx - 17, cy - 8);
                crown.lineTo(cx - 8, cy - 2);
                crown.lineTo(cx, cy - 13);
                crown.lineTo(cx + 8, cy - 2);
                crown.lineTo(cx + 17, cy - 8);
                crown.lineTo(cx + 15, cy + 4);
                crown.closePath();

                g2.setPaint(new GradientPaint(cx - 15, cy - 13, new Color(0xFF, 0xEE, 0x55), cx + 15, cy + 4, new Color(0xF5, 0x9E, 0x0B)));
                g2.fill(crown);
                g2.setColor(new Color(0xB4, 0x53, 0x09));
                g2.setStroke(new BasicStroke(1.2f));
                g2.draw(crown);

                // Ngọc ruby & emerald trên đỉnh vương miện
                g2.setColor(Theme.RED);
                g2.fillOval(cx - 19, cy - 10, 4, 4);
                g2.fillOval(cx + 15, cy - 10, 4, 4);
                g2.setColor(Theme.EMERALD);
                g2.fillOval(cx - 2, cy - 15, 5, 5);

            } else if (rankPos == 2) {
                // Vương miện Bạc Á Quân 3 đỉnh
                Path2D.Float crown = new Path2D.Float();
                crown.moveTo(cx - 12, cy + 3);
                crown.lineTo(cx - 14, cy - 6);
                crown.lineTo(cx, cy - 2);
                crown.lineTo(cx + 14, cy - 6);
                crown.lineTo(cx + 12, cy + 3);
                crown.closePath();

                g2.setPaint(new GradientPaint(cx - 12, cy - 6, new Color(0xF1, 0xF5, 0xF9), cx + 12, cy + 3, new Color(0x94, 0xA3, 0xB8)));
                g2.fill(crown);
                g2.setColor(new Color(0x47, 0x55, 0x69));
                g2.setStroke(new BasicStroke(1.2f));
                g2.draw(crown);

            } else {
                // Ngôi sao / Huân chương Đồng Quý Quân
                g2.setColor(new Color(0xEA, 0x58, 0x0C));
                Path2D.Float star = new Path2D.Float();
                double angle = -Math.PI / 2.0;
                double step = Math.PI / 5.0;
                for (int i = 0; i < 10; i++) {
                    double r = (i % 2 == 0) ? 7.5 : 3.8;
                    double x = cx + Math.cos(angle) * r;
                    double y = cy + Math.sin(angle) * r;
                    if (i == 0) star.moveTo(x, y);
                    else star.lineTo(x, y);
                    angle += step;
                }
                star.closePath();
                g2.setPaint(new GradientPaint(cx - 7, cy - 7, new Color(0xFD, 0xBA, 0x74), cx + 7, cy + 7, new Color(0xC2, 0x41, 0x0C)));
                g2.fill(star);
                g2.setColor(new Color(0x9A, 0x34, 0x12));
                g2.setStroke(new BasicStroke(1.0f));
                g2.draw(star);
            }
        }
    }

    // ==========================================
    // THANH THỨ HẠNG CÁ NHÂN (STICKY "MY RANK" BAR)
    // HOÀN TOÀN KHÔNG DÙNG EMOJI - DÙNG JAVA2D CROWN/BADGE
    // ==========================================
    private static class StickyMyRankBar extends ModernCard {
        private final JLabel lblAvatar;
        private final JLabel lblMyRank;
        private final JLabel lblDifferenceNotice;
        private final JLabel lblStatsSummary;
        private boolean isTop1User = false;

        public StickyMyRankBar() {
            super(new BorderLayout(14, 0));
            setCardBackground(Theme.WHITE);
            setBorderColor(new Color(0xCB, 0xD5, 0xE1));
            setCornerRadius(12);
            setCardPadding(8, 18, 8, 18);
            setPreferredSize(new Dimension(600, 50));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

            // Left: Avatar & Rank
            JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            leftPanel.setOpaque(false);

            lblAvatar = new JLabel();
            lblAvatar.setPreferredSize(new Dimension(30, 30));

            lblMyRank = new JLabel("Hạng: —");
            lblMyRank.setFont(Theme.BODY_BOLD);
            lblMyRank.setForeground(Theme.DARK_TEXT);

            leftPanel.add(lblAvatar);
            leftPanel.add(lblMyRank);
            add(leftPanel, BorderLayout.WEST);

            // Center: Status Notice (Custom painted badge, NO EMOJIS!)
            JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4)) {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    if (isTop1User) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        Theme.setupAntiAliasing(g2);
                        // Mini Golden Crown icon painted Java2D
                        int cy = getHeight() / 2;
                        int cx = 18;
                        drawMiniCrown(g2, cx, cy);
                        g2.dispose();
                    }
                }

                private void drawMiniCrown(Graphics2D g2, int cx, int cy) {
                    Path2D.Float crown = new Path2D.Float();
                    crown.moveTo(cx - 7, cy + 3);
                    crown.lineTo(cx - 8, cy - 4);
                    crown.lineTo(cx - 4, cy - 1);
                    crown.lineTo(cx, cy - 6);
                    crown.lineTo(cx + 4, cy - 1);
                    crown.lineTo(cx + 8, cy - 4);
                    crown.lineTo(cx + 7, cy + 3);
                    crown.closePath();
                    g2.setPaint(new GradientPaint(cx - 7, cy - 6, new Color(0xFF, 0xEE, 0x55), cx + 7, cy + 3, new Color(0xF5, 0x9E, 0x0B)));
                    g2.fill(crown);
                    g2.setColor(new Color(0xB4, 0x53, 0x09));
                    g2.setStroke(new BasicStroke(1.0f));
                    g2.draw(crown);
                }
            };
            centerPanel.setOpaque(false);

            lblDifferenceNotice = new JLabel("Đang tải thứ hạng...", SwingConstants.CENTER);
            lblDifferenceNotice.setFont(Theme.SMALL_BOLD);
            lblDifferenceNotice.setForeground(Theme.TEXT_BODY);
            centerPanel.add(lblDifferenceNotice);
            add(centerPanel, BorderLayout.CENTER);

            // Right: Matches & WinRate
            lblStatsSummary = new JLabel("—", SwingConstants.RIGHT);
            lblStatsSummary.setFont(Theme.SMALL);
            lblStatsSummary.setForeground(Theme.MUTED_GRAY);
            add(lblStatsSummary, BorderLayout.EAST);
        }

        public void updateMyRank(List<PlayerRankEntry> entries, String currentUsername) {
            if (currentUsername == null || currentUsername.isBlank()) {
                isTop1User = false;
                lblMyRank.setText("Chưa đăng nhập");
                lblDifferenceNotice.setText("");
                lblStatsSummary.setText("");
                repaint();
                return;
            }

            int myIndex = -1;
            for (int i = 0; i < entries.size(); i++) {
                if (entries.get(i).name.equalsIgnoreCase(currentUsername)) {
                    myIndex = i;
                    break;
                }
            }

            if (myIndex == -1) {
                isTop1User = false;
                lblMyRank.setText("Hạng: Chưa xếp");
                lblDifferenceNotice.setText("Bạn chưa có thứ hạng • Hãy tham gia thi đấu đối kháng để ghi tên lên Bảng Vàng!");
                lblDifferenceNotice.setForeground(Theme.MUTED_GRAY);
                lblStatsSummary.setText("");
            } else {
                PlayerRankEntry me = entries.get(myIndex);
                lblMyRank.setText("Hạng #" + me.rank + " (" + me.name + ")");

                if (myIndex == 0) {
                    isTop1User = true;
                    // SỬ DỤNG CHỮ THUẦN KHÔNG CÓ EMOJI ĐỂ TRÁNH LỖI Ô VUÔNG TRÊN WINDOWS:
                    lblDifferenceNotice.setText("QUÁN QUÂN • Bạn đang dẫn đầu Bảng Xếp Hạng! Tiếp tục giữ vững phong độ.");
                    lblDifferenceNotice.setForeground(new Color(0xB4, 0x53, 0x09));
                } else {
                    isTop1User = false;
                    PlayerRankEntry playerAbove = entries.get(myIndex - 1);
                    int ptsDiff = Math.max(1, (playerAbove.score - me.score) + 1);
                    lblDifferenceNotice.setText("Điểm: " + me.score + " pts • Cần thêm " + ptsDiff + " pts để vượt " + playerAbove.name + " (#" + playerAbove.rank + ")");
                    lblDifferenceNotice.setForeground(Theme.TEXT_BODY);
                }

                lblStatsSummary.setText(me.totalMatches + " trận • Thắng " + me.winRate);
            }
            repaint();
        }
    }
}
