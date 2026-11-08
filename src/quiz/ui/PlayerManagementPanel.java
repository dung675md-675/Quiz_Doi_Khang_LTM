package quiz.ui;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import quiz.Rank;

/**
 * PlayerManagementPanel — Dashboard Quản Lý Người Chơi hiện đại & tinh giản:
 * - 4 Thẻ Analytics với biểu tượng Java2D đa lớp sắc nét (Tổng tài khoản, Đang online, Admin, Điểm trung bình).
 * - Bảng danh sách người chơi Zebra Style, hiệu ứng hover từng dòng, avatar tròn có viền kim loại.
 * - Thanh tìm kiếm tích hợp icon kính lúp Java2D và bộ lọc theo Vai trò / Trạng thái.
 * - Thao tác quản trị tập trung: Thêm mới, Xem hồ sơ Inspector, Sửa thông tin, Khóa / Mở khóa, Xóa an toàn.
 */
public class PlayerManagementPanel extends JPanel {

    public record PlayerRecord(
            String username,
            int score,
            String avatarId,
            String rankLabel,
            String status,
            String fullName,
            String email,
            String bio,
            String role) {
        public PlayerRecord(String username, int score, String avatarId, String rankLabel, String status, String fullName, String email, String bio) {
            this(username, score, avatarId, rankLabel, status, fullName, email, bio, "admin".equalsIgnoreCase(username) ? "ADMIN" : "PLAYER");
        }
    }

    public interface UserActionListener {
        void onAddUser(String username, String password, int score, String avatarId, String fullName, String email, String bio, String role);
        void onUpdateUser(String username, String newPassword, int score, String avatarId, String fullName, String email, String bio, String role);
        void onDeleteUser(String username);
        void onRefresh();

        default void onAddUser(String username, String password, int score, String avatarId, String fullName, String email, String bio) {
            onAddUser(username, password, score, avatarId, fullName, email, bio, "PLAYER");
        }
        default void onUpdateUser(String username, String newPassword, int score, String avatarId, String fullName, String email, String bio) {
            onUpdateUser(username, newPassword, score, avatarId, fullName, email, bio, "");
        }
    }

    private final JTable table;
    private final DefaultTableModel model;
    private final JTextField txtSearch;
    private final JComboBox<String> cboRoleFilter;
    private final JComboBox<String> cboStatusFilter;
    private final JLabel lblTotalCount;

    // 4 Analytics KPI Labels
    private final JLabel lblTotalUsersVal = new JLabel("0");
    private final JLabel lblOnlineUsersVal = new JLabel("0");
    private final JLabel lblAdminCountVal = new JLabel("0");
    private final JLabel lblAvgScoreVal = new JLabel("0 đ");

    private final List<PlayerRecord> allRecords = new ArrayList<>();
    private final List<PlayerRecord> filteredRecords = new ArrayList<>();
    private int hoveredRow = -1;
    private String currentUsername = "";
    private boolean isAdminMode = false;
    private UserActionListener actionListener;

    public void setAdminMode(boolean isAdminMode) {
        this.isAdminMode = isAdminMode;
    }

    public boolean isAdminMode() {
        return isAdminMode;
    }

    public PlayerManagementPanel() {
        setLayout(new BorderLayout(0, 12));
        setOpaque(false);
        setBorder(new EmptyBorder(14, 20, 16, 20));

        // ==========================================
        // 1. TOP CONTAINER: TITLE + KPI + TOOLBAR
        // ==========================================
        JPanel topContainer = new JPanel();
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
        topContainer.setOpaque(false);

        // Header Title
        JPanel titleBox = new JPanel(new BorderLayout());
        titleBox.setOpaque(false);

        JLabel lblTitle = new JLabel("QUẢN LÝ THÔNG TIN NGƯỜI CHƠI & TÀI KHOẢN");
        lblTitle.setFont(Theme.TITLE);
        lblTitle.setForeground(Theme.BLUE);

        JLabel lblDesc = new JLabel("Giám sát danh sách người dùng, trạng thái trực tuyến và phân quyền quản trị");
        lblDesc.setFont(Theme.SMALL);
        lblDesc.setForeground(Theme.MUTED_GRAY);

        titleBox.add(lblTitle, BorderLayout.NORTH);
        titleBox.add(lblDesc, BorderLayout.SOUTH);
        topContainer.add(titleBox);
        topContainer.add(Box.createVerticalStrut(10));

        // 4 User Analytics KPI Cards với Icon Vector nghệ thuật
        JPanel kpiRow = new JPanel(new GridLayout(1, 4, 12, 0));
        kpiRow.setOpaque(false);

        kpiRow.add(createKpiCard("TỔNG TÀI KHOẢN", "Toàn bộ server", lblTotalUsersVal,
                new Color(0xEE, 0xF2, 0xFF), new Color(0xDB, 0xEA, 0xFE), new Color(0xBF, 0xDB, 0xFE), new Color(0x1D, 0x4E, 0xD8), 0));
        kpiRow.add(createKpiCard("ĐANG TRỰC TUYẾN", "Sẵn sàng thi đấu", lblOnlineUsersVal,
                new Color(0xEC, 0xFD, 0xF5), new Color(0xD1, 0xFA, 0xE5), new Color(0xA7, 0xF3, 0xD0), new Color(0x04, 0x78, 0x57), 1));
        kpiRow.add(createKpiCard("QUẢN TRỊ VIÊN", "Quyền hạn Admin", lblAdminCountVal,
                new Color(0xFA, 0xF5, 0xFF), new Color(0xED, 0xE9, 0xFE), new Color(0xDD, 0xD6, 0xFE), new Color(0x6D, 0x28, 0xD9), 2));
        kpiRow.add(createKpiCard("ĐIỂM TRUNG BÌNH", "Mức điểm bình quân", lblAvgScoreVal,
                new Color(0xFF, 0xFB, 0xEB), new Color(0xFE, 0xF3, 0xC7), new Color(0xFD, 0xE6, 0x8A), new Color(0xB4, 0x53, 0x09), 3));

        topContainer.add(kpiRow);
        topContainer.add(Box.createVerticalStrut(10));

        // Unified Toolbar Card (Actions + Search + Filters)
        ModernCard toolbarCard = new ModernCard(new BorderLayout(10, 10));
        toolbarCard.setCardPadding(12, 16, 12, 16);
        toolbarCard.setCardBackground(Theme.WHITE);
        toolbarCard.setBorderColor(Theme.BORDER_LIGHT);

        // Actions Row (Top)
        JPanel actionRow = new JPanel(new BorderLayout(10, 0));
        actionRow.setOpaque(false);

        lblTotalCount = new JLabel("Hiển thị: 0 người chơi");
        lblTotalCount.setFont(Theme.BODY_BOLD);
        lblTotalCount.setForeground(Theme.DARK_TEXT);
        actionRow.add(lblTotalCount, BorderLayout.WEST);

        JPanel btnGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnGroup.setOpaque(false);

        ModernButton btnInspector = new ModernButton("Xem Hồ Sơ", ModernButton.Style.PRIMARY);
        btnInspector.setPreferredSize(new Dimension(110, 36));
        btnInspector.addActionListener(e -> {
            SoundManager.playClick();
            inspectSelected();
        });

        ModernButton btnAdd = new ModernButton("+ Thêm Mới", ModernButton.Style.EMERALD);
        btnAdd.setPreferredSize(new Dimension(115, 36));
        btnAdd.addActionListener(e -> {
            SoundManager.playClick();
            openAddDialog();
        });

        ModernButton btnEdit = new ModernButton("Sửa", ModernButton.Style.PRIMARY);
        btnEdit.setPreferredSize(new Dimension(80, 36));
        btnEdit.addActionListener(e -> {
            SoundManager.playClick();
            editSelectedRow();
        });

        ModernButton btnToggleLock = new ModernButton("Khóa / Mở Khóa", ModernButton.Style.ORANGE);
        btnToggleLock.setPreferredSize(new Dimension(140, 36));
        btnToggleLock.addActionListener(e -> {
            SoundManager.playClick();
            quickToggleLockSelected();
        });

        ModernButton btnDelete = new ModernButton("Xóa", ModernButton.Style.DANGER);
        btnDelete.setPreferredSize(new Dimension(80, 36));
        btnDelete.addActionListener(e -> {
            SoundManager.playClick();
            deleteSelectedRow();
        });

        ModernButton btnRefresh = new ModernButton("Làm Mới", ModernButton.Style.SECONDARY);
        btnRefresh.setPreferredSize(new Dimension(95, 36));
        btnRefresh.addActionListener(e -> {
            SoundManager.playClick();
            if (actionListener != null) actionListener.onRefresh();
        });

        btnGroup.add(btnInspector);
        btnGroup.add(btnAdd);
        btnGroup.add(btnEdit);
        btnGroup.add(btnToggleLock);
        btnGroup.add(btnDelete);
        btnGroup.add(btnRefresh);
        actionRow.add(btnGroup, BorderLayout.EAST);
        toolbarCard.add(actionRow, BorderLayout.NORTH);

        // Filter & Search Row (Bottom)
        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        filterRow.setOpaque(false);

        // Custom Search Box with integrated Java2D Magnifying Glass icon
        JPanel searchBox = new JPanel(new BorderLayout(8, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setupAntiAliasing(g2);
                int w = getWidth();
                int h = getHeight();
                g2.setColor(new Color(0xF8, 0xFA, 0xFC));
                g2.fillRoundRect(0, 0, w, h, 12, 12);
                g2.setColor(new Color(0xCB, 0xD5, 0xE1));
                g2.setStroke(new BasicStroke(1.0f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 12, 12);

                // Magnifying glass icon on left
                g2.setColor(new Color(0x64, 0x74, 0x8B));
                g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int cx = 16;
                int cy = h / 2 - 2;
                g2.drawOval(cx - 5, cy - 5, 10, 10);
                g2.drawLine(cx + 3, cy + 3, cx + 7, cy + 7);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        searchBox.setOpaque(false);
        searchBox.setPreferredSize(new Dimension(280, 34));
        searchBox.setBorder(new EmptyBorder(0, 28, 0, 8));

        txtSearch = new JTextField();
        txtSearch.setFont(Theme.BODY);
        txtSearch.setBorder(null);
        txtSearch.setOpaque(false);
        txtSearch.putClientProperty("JTextField.placeholderText", "Tìm theo tên đăng nhập, họ tên, email...");
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { applyFilter(); }
            @Override public void removeUpdate(DocumentEvent e) { applyFilter(); }
            @Override public void changedUpdate(DocumentEvent e) { applyFilter(); }
        });
        searchBox.add(txtSearch, BorderLayout.CENTER);

        cboRoleFilter = createStyledComboBox(new String[]{"Tất cả vai trò", "ADMIN (Quản trị)", "PLAYER (Người chơi)", "LOCKED (Bị khóa)"}, 175);
        cboRoleFilter.addActionListener(e -> applyFilter());

        cboStatusFilter = createStyledComboBox(new String[]{"Tất cả trạng thái", "Trực tuyến", "Ngoại tuyến", "Đang đấu", "Đã khóa"}, 160);
        cboStatusFilter.addActionListener(e -> applyFilter());

        filterRow.add(searchBox);
        filterRow.add(createFilterLabel("Vai trò:"));
        filterRow.add(cboRoleFilter);
        filterRow.add(createFilterLabel("Trạng thái:"));
        filterRow.add(cboStatusFilter);
        toolbarCard.add(filterRow, BorderLayout.SOUTH);

        topContainer.add(toolbarCard);
        add(topContainer, BorderLayout.NORTH);

        // ==========================================
        // 2. DATA TABLE WITH ZEBRA & HOVER & BADGES
        // ==========================================
        String[] cols = {
                "Avatar", "Tài khoản", "Vai trò", "Điểm số", "Bậc Rank", "Trạng thái", "Họ và tên", "Email"
        };
        model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(model);
        table.setRowHeight(46);
        table.setFont(Theme.BODY);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setSelectionBackground(new Color(0xEE, 0xF2, 0xFF));
        table.setSelectionForeground(Theme.DARK_TEXT);

        table.getTableHeader().setFont(Theme.BODY_BOLD);
        table.getTableHeader().setBackground(new Color(0xF8, 0xFA, 0xFC));
        table.getTableHeader().setForeground(Theme.DARK_TEXT);
        table.getTableHeader().setPreferredSize(new Dimension(0, 40));
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xCB, 0xD5, 0xE1)));

        // Column 0: Circular Avatar with metallic border
        table.getColumnModel().getColumn(0).setPreferredWidth(64);
        table.getColumnModel().getColumn(0).setMaxWidth(72);
        table.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            private String currentAv = "avatar_01";
            private final JPanel avCell = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g.create();
                    Theme.setupAntiAliasing(g2);
                    int size = 32;
                    int ox = (getWidth() - size) / 2;
                    int oy = (getHeight() - size) / 2;
                    AvatarPicker.paintAvatar(g2, currentAv, ox, oy, size);
                    g2.setColor(new Color(0x94, 0xA3, 0xB8, 140));
                    g2.setStroke(new BasicStroke(1.2f));
                    g2.drawOval(ox, oy, size - 1, size - 1);
                    g2.dispose();
                }
            };
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                currentAv = val != null ? val.toString() : "avatar_01";
                if (sel) {
                    avCell.setBackground(t.getSelectionBackground());
                } else if (row == hoveredRow) {
                    avCell.setBackground(new Color(0xF1, 0xF5, 0xF9));
                } else {
                    avCell.setBackground(row % 2 == 0 ? Theme.WHITE : new Color(0xF8, 0xFA, 0xFC));
                }
                return avCell;
            }
        });

        // General Cell Renderer: Zebra, Hover, Badges
        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                String strVal = (val != null) ? val.toString() : "";

                if (sel) {
                    c.setBackground(t.getSelectionBackground());
                } else if (row == hoveredRow) {
                    c.setBackground(new Color(0xF1, 0xF5, 0xF9));
                } else {
                    c.setBackground(row % 2 == 0 ? Theme.WHITE : new Color(0xF8, 0xFA, 0xFC));
                }

                setFont((col == 1 || col == 2 || col == 3 || col == 4 || col == 5) ? Theme.BODY_BOLD : Theme.BODY);
                setHorizontalAlignment((col == 2 || col == 3 || col == 4 || col == 5) ? SwingConstants.CENTER : SwingConstants.LEFT);
                setBorder(new EmptyBorder(0, 12, 0, 12));

                if (col == 1) {
                    setForeground(strVal.contains("(Bạn)") ? Theme.BLUE : Theme.DARK_TEXT);
                } else if (col == 2) {
                    // Role badge
                    if ("ADMIN".equalsIgnoreCase(strVal)) setForeground(new Color(0x7C, 0x3A, 0xED));
                    else if ("LOCKED".equalsIgnoreCase(strVal)) setForeground(new Color(0xDC, 0x26, 0x26));
                    else setForeground(new Color(0x47, 0x55, 0x69));
                } else if (col == 3) {
                    // Score
                    setForeground(new Color(0x25, 0x63, 0xEB));
                } else if (col == 4) {
                    // Rank
                    setForeground(new Color(0x8B, 0x5C, 0xF6));
                } else if (col == 5) {
                    // Status
                    if (strVal.contains("Trực tuyến")) setForeground(new Color(0x05, 0x96, 0x69));
                    else if (strVal.contains("Đang đấu")) setForeground(new Color(0xD9, 0x77, 0x06));
                    else if (strVal.contains("Đã khóa")) setForeground(new Color(0xDC, 0x26, 0x26));
                    else setForeground(Theme.MUTED_GRAY);
                } else {
                    setForeground(Theme.DARK_TEXT);
                }
                return c;
            }
        };

        for (int c = 1; c < cols.length; c++) {
            table.getColumnModel().getColumn(c).setCellRenderer(cellRenderer);
        }

        table.getColumnModel().getColumn(1).setPreferredWidth(130);
        table.getColumnModel().getColumn(2).setPreferredWidth(90);
        table.getColumnModel().getColumn(2).setMaxWidth(110);
        table.getColumnModel().getColumn(3).setPreferredWidth(95);
        table.getColumnModel().getColumn(3).setMaxWidth(115);
        table.getColumnModel().getColumn(4).setPreferredWidth(110);
        table.getColumnModel().getColumn(4).setMaxWidth(130);
        table.getColumnModel().getColumn(5).setPreferredWidth(110);
        table.getColumnModel().getColumn(5).setMaxWidth(130);
        table.getColumnModel().getColumn(6).setPreferredWidth(140);
        table.getColumnModel().getColumn(7).setPreferredWidth(160);

        // Row rollover tracking
        table.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int r = table.rowAtPoint(e.getPoint());
                if (r != hoveredRow) {
                    hoveredRow = r;
                    table.repaint();
                }
            }
        });
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                hoveredRow = -1;
                table.repaint();
            }
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) {
                    inspectSelected();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(Theme.BORDER_LIGHT, 1));
        scrollPane.getViewport().setBackground(Theme.WHITE);
        scrollPane.getVerticalScrollBar().setUI(new ModernScrollBarUI());
        scrollPane.getVerticalScrollBar().setUnitIncrement(18);
        add(scrollPane, BorderLayout.CENTER);
    }

    private JPanel createKpiCard(String title, String subtitle, JLabel lblValue,
                                 Color bgTop, Color bgBottom, Color borderColor, Color primaryColor, int iconType) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setupAntiAliasing(g2);
                int w = getWidth();
                int h = getHeight();

                GradientPaint gp = new GradientPaint(0, 0, bgTop, 0, h, bgBottom);
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, w, h, 14, 14);

                g2.setColor(borderColor);
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 14, 14);

                // Polished Icon Squircle (30x30)
                int icSize = 30;
                int icX = w - icSize - 12;
                int icY = 10;

                g2.setColor(new Color(primaryColor.getRed(), primaryColor.getGreen(), primaryColor.getBlue(), 32));
                g2.fillRoundRect(icX, icY, icSize, icSize, 8, 8);
                g2.setColor(new Color(primaryColor.getRed(), primaryColor.getGreen(), primaryColor.getBlue(), 80));
                g2.setStroke(new BasicStroke(1.0f));
                g2.drawRoundRect(icX, icY, icSize - 1, icSize - 1, 8, 8);

                g2.setColor(primaryColor);
                g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int cx = icX + icSize / 2;
                int cy = icY + icSize / 2;

                if (iconType == 0) {
                    // Polished Clean User Profile Silhouette (Crisp, perfectly centered, zero ghosting)
                    g2.drawOval(cx - 4, cy - 7, 8, 8);
                    Path2D.Float body = new Path2D.Float();
                    body.moveTo(cx - 7, cy + 7);
                    body.curveTo(cx - 7, cy + 2, cx - 4, cy + 1, cx, cy + 1);
                    body.curveTo(cx + 4, cy + 1, cx + 7, cy + 2, cx + 7, cy + 7);
                    g2.draw(body);
                } else if (iconType == 1) {
                    // Modern Wifi / Broadcast Signal Wave (Signal dot + clear waves)
                    g2.setColor(new Color(0x10, 0xB9, 0x81));
                    g2.fillOval(cx - 2, cy + 3, 4, 4);
                    g2.setColor(new Color(0x05, 0x96, 0x69));
                    g2.drawArc(cx - 6, cy - 1, 12, 12, 45, 90);
                    g2.drawArc(cx - 10, cy - 5, 20, 20, 45, 90);
                } else if (iconType == 2) {
                    // Executive Security Shield + Inner Star
                    Path2D.Float sh = new Path2D.Float();
                    sh.moveTo(cx - 6, cy - 6);
                    sh.lineTo(cx + 6, cy - 6);
                    sh.lineTo(cx + 6, cy);
                    sh.quadTo(cx, cy + 7, cx - 6, cy);
                    sh.closePath();
                    g2.draw(sh);
                    g2.fillOval(cx - 2, cy - 2, 4, 4);
                } else {
                    // Average Score / Medal Star
                    g2.drawOval(cx - 6, cy - 6, 12, 12);
                    g2.drawLine(cx, cy - 4, cx, cy + 4);
                    g2.drawLine(cx - 4, cy, cx + 4, cy);
                }

                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(10, 14, 10, 14));
        card.setPreferredSize(new Dimension(170, 78));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(Theme.SMALL_BOLD);
        lblTitle.setForeground(primaryColor);
        lblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        lblValue.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        lblValue.setForeground(primaryColor);
        lblValue.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblSub = new JLabel(subtitle);
        lblSub.setFont(Theme.SMALL);
        lblSub.setForeground(new Color(primaryColor.getRed(), primaryColor.getGreen(), primaryColor.getBlue(), 180));
        lblSub.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(lblTitle);
        card.add(Box.createVerticalStrut(2));
        card.add(lblValue);
        card.add(Box.createVerticalStrut(2));
        card.add(lblSub);
        return card;
    }

    private static JComboBox<String> createStyledComboBox(String[] items, int width) {
        JComboBox<String> cb = new JComboBox<>(items);
        cb.setFont(Theme.BODY);
        cb.setPreferredSize(new Dimension(width, 34));
        cb.setBackground(new Color(0xF8, 0xFA, 0xFC));
        return cb;
    }

    private static JLabel createFilterLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.SMALL_BOLD);
        l.setForeground(new Color(0x47, 0x55, 0x69));
        return l;
    }

    public void setUserActionListener(UserActionListener listener) {
        this.actionListener = listener;
    }

    public void updateData(String rawData, String currentUsername) {
        this.currentUsername = currentUsername != null ? currentUsername : "";
        allRecords.clear();

        if (rawData != null && !rawData.isBlank()) {
            String[] rows = rawData.split("\\|");
            for (String r : rows) {
                if (r.isBlank()) continue;
                String[] f = r.split(";", -1);
                if (f.length >= 5) {
                    String uName = f[0];
                    int score = 0;
                    try { score = Integer.parseInt(f[1]); } catch (Exception ignored) {}
                    String avatar = f[2].isBlank() ? "avatar_01" : f[2];
                    String rank = f[3].isBlank() ? Rank.fromScore(score).getDisplayName() : f[3];
                    String status = f[4].isBlank() ? "Ngoại tuyến" : f[4];
                    String fullName = f.length > 5 ? f[5] : "";
                    String email = f.length > 6 ? f[6] : "";
                    String bio = f.length > 7 ? f[7] : "";
                    String role = f.length > 8 && !f[8].isBlank() ? f[8] : ("admin".equalsIgnoreCase(uName) ? "ADMIN" : "PLAYER");
                    allRecords.add(new PlayerRecord(uName, score, avatar, rank, status, fullName, email, bio, role));
                }
            }
        }
        updateAnalytics();
        applyFilter();
    }

    private void updateAnalytics() {
        int total = allRecords.size();
        int onlineCount = 0;
        int adminCount = 0;
        long sumScore = 0;

        for (PlayerRecord r : allRecords) {
            if (r.status().contains("Trực tuyến") || r.status().contains("Đang đấu")) {
                onlineCount++;
            }
            if ("ADMIN".equalsIgnoreCase(r.role())) {
                adminCount++;
            }
            sumScore += r.score();
        }

        lblTotalUsersVal.setText(total + " người");
        lblOnlineUsersVal.setText(onlineCount + " online");
        lblAdminCountVal.setText(adminCount + " tài khoản");
        lblAvgScoreVal.setText((total > 0 ? (sumScore / total) : 0) + " đ");
    }

    public PlayerRecord findRecord(String username) {
        if (username == null) return null;
        for (PlayerRecord r : allRecords) {
            if (r.username().equalsIgnoreCase(username)) return r;
        }
        return null;
    }

    private void applyFilter() {
        String q = txtSearch.getText() != null ? txtSearch.getText().trim().toLowerCase() : "";
        String roleFilter = (String) cboRoleFilter.getSelectedItem();
        String statusFilter = (String) cboStatusFilter.getSelectedItem();

        filteredRecords.clear();
        model.setRowCount(0);

        for (PlayerRecord r : allRecords) {
            if (roleFilter != null && !roleFilter.equals("Tất cả vai trò")) {
                if (roleFilter.contains("ADMIN") && !"ADMIN".equalsIgnoreCase(r.role())) continue;
                if (roleFilter.contains("PLAYER") && !"PLAYER".equalsIgnoreCase(r.role())) continue;
                if (roleFilter.contains("LOCKED") && !"LOCKED".equalsIgnoreCase(r.role())) continue;
            }

            if (statusFilter != null && !statusFilter.equals("Tất cả trạng thái")) {
                if (!r.status().equalsIgnoreCase(statusFilter)) continue;
            }

            if (!q.isEmpty()
                    && !r.username().toLowerCase().contains(q)
                    && !r.fullName().toLowerCase().contains(q)
                    && !r.email().toLowerCase().contains(q)
                    && !r.rankLabel().toLowerCase().contains(q)) {
                continue;
            }

            filteredRecords.add(r);
            model.addRow(new Object[]{
                    r.avatarId(),
                    r.username() + (r.username().equalsIgnoreCase(currentUsername) ? " (Bạn)" : ""),
                    r.role(),
                    r.score() + " đ",
                    r.rankLabel(),
                    r.status(),
                    r.fullName().isBlank() ? "—" : r.fullName(),
                    r.email().isBlank() ? "—" : r.email()
            });
        }
        lblTotalCount.setText("Hiển thị: " + filteredRecords.size() + " / " + allRecords.size() + " người chơi");
    }

    private PlayerRecord getSelectedRecord() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= filteredRecords.size()) {
            return null;
        }
        return filteredRecords.get(row);
    }

    private void inspectSelected() {
        PlayerRecord rec = getSelectedRecord();
        if (rec == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 người chơi trong bảng để xem hồ sơ!", "Chưa chọn người chơi", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        openInspector(rec);
    }

    private void editSelectedRow() {
        PlayerRecord rec = getSelectedRecord();
        if (rec == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 người chơi trong bảng để sửa!", "Chưa chọn người chơi", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        openEditDialog(rec);
    }

    private void deleteSelectedRow() {
        PlayerRecord rec = getSelectedRecord();
        if (rec == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 người chơi trong bảng để xóa!", "Chưa chọn người chơi", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        confirmAndDelete(rec.username());
    }

    private void quickToggleLockSelected() {
        PlayerRecord rec = getSelectedRecord();
        if (rec == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 người chơi trong bảng để Khóa/Mở khóa!", "Chưa chọn người chơi", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        quickToggleLock(rec);
    }

    public void quickToggleLock(PlayerRecord rec) {
        if (!isAdminMode) {
            JOptionPane.showMessageDialog(this, "Chỉ Quản trị viên (Admin) mới có quyền khóa/mở khóa tài khoản!", "Từ chối", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if ("admin".equalsIgnoreCase(rec.username())) {
            JOptionPane.showMessageDialog(this, "Không thể khóa tài khoản Quản trị viên gốc (admin)!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (rec.username().equalsIgnoreCase(currentUsername)) {
            JOptionPane.showMessageDialog(this, "Bạn không thể tự khóa tài khoản của chính mình!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        boolean isCurrentlyLocked = "LOCKED".equalsIgnoreCase(rec.role());
        String confirmMsg = isCurrentlyLocked
                ? "Bạn có chắc chắn muốn MỞ KHÓA tài khoản '" + rec.username() + "'?\nNgười này sẽ có thể đăng nhập và thi đấu trở lại."
                : "Bạn có chắc chắn muốn KHÓA tài khoản '" + rec.username() + "'?\nNgười này sẽ bị ngắt kết nối và không thể đăng nhập!";

        int cf = JOptionPane.showConfirmDialog(this, confirmMsg, isCurrentlyLocked ? "Mở Khóa Tài Khoản" : "Khóa Tài Khoản",
                JOptionPane.YES_NO_OPTION, isCurrentlyLocked ? JOptionPane.QUESTION_MESSAGE : JOptionPane.WARNING_MESSAGE);

        if (cf == JOptionPane.YES_OPTION && actionListener != null) {
            String newRole = isCurrentlyLocked ? "PLAYER" : "LOCKED";
            actionListener.onUpdateUser(rec.username(), "", rec.score(), rec.avatarId(), rec.fullName(), rec.email(), rec.bio(), newRole);
        }
    }

    public void confirmAndDelete(String targetUsername) {
        if (!isAdminMode) {
            JOptionPane.showMessageDialog(this, "Chỉ Quản trị viên (Admin) mới có quyền xóa tài khoản người chơi!", "Từ chối", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if ("admin".equalsIgnoreCase(targetUsername)) {
            JOptionPane.showMessageDialog(this, "Không thể xóa tài khoản Quản trị viên gốc (admin)!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        boolean isSelf = targetUsername.equalsIgnoreCase(currentUsername);
        String msg = isSelf
                ? "Bạn đang chọn XÓA TÀI KHOẢN CỦA CHÍNH MÌNH ('" + targetUsername + "')!\nSau khi xóa, bạn sẽ tự động đăng xuất. Bạn có chắc chắn không?"
                : "Bạn có chắc chắn muốn xóa người chơi '" + targetUsername + "' khỏi hệ thống không?\nHành động này không thể hoàn tác!";

        int opt = JOptionPane.showConfirmDialog(
                SwingUtilities.getWindowAncestor(this),
                msg,
                "Xác Nhận Xóa Người Chơi",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (opt == JOptionPane.YES_OPTION && actionListener != null) {
            actionListener.onDeleteUser(targetUsername);
        }
    }

    // =========================================================================
    // PLAYER INSPECTOR MODAL (HỒ SƠ NGƯỜI CHƠI ESPORTS)
    // =========================================================================
    public void openInspector(PlayerRecord r) {
        Window owner = SwingUtilities.getWindowAncestor(this);
        JDialog dlg = new JDialog(owner, "Hồ Sơ Đấu Thủ — " + r.username(), Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setSize(520, 500);
        dlg.setLocationRelativeTo(owner);
        dlg.setLayout(new BorderLayout());

        // Header Profile Banner
        JPanel header = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setupAntiAliasing(g2);
                int w = getWidth();
                int h = getHeight();
                GradientPaint gp = new GradientPaint(0, 0, new Color(0x1E, 0x29, 0x3B), w, h, new Color(0x0F, 0x17, 0x2A));
                g2.setPaint(gp);
                g2.fillRect(0, 0, w, h);
                g2.dispose();
            }
        };
        header.setLayout(new FlowLayout(FlowLayout.LEFT, 20, 16));
        header.setBorder(new EmptyBorder(10, 16, 10, 16));

        // Big Avatar (64x64)
        JPanel avBig = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setupAntiAliasing(g2);
                AvatarPicker.paintAvatar(g2, r.avatarId(), 0, 0, 64);
                g2.setColor(new Color(0x38, 0xBD, 0xF8));
                g2.setStroke(new BasicStroke(2.5f));
                g2.drawOval(0, 0, 63, 63);
                g2.dispose();
            }
        };
        avBig.setPreferredSize(new Dimension(64, 64));
        avBig.setOpaque(false);
        header.add(avBig);

        JPanel headerInfo = new JPanel();
        headerInfo.setLayout(new BoxLayout(headerInfo, BoxLayout.Y_AXIS));
        headerInfo.setOpaque(false);

        JLabel lblName = new JLabel(r.username());
        lblName.setFont(Theme.TITLE_XL);
        lblName.setForeground(Color.WHITE);

        JPanel tagRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        tagRow.setOpaque(false);

        JLabel lblRoleTag = new JLabel(" " + r.role() + " ");
        lblRoleTag.setFont(Theme.SMALL_BOLD);
        lblRoleTag.setOpaque(true);
        lblRoleTag.setBackground("ADMIN".equalsIgnoreCase(r.role()) ? new Color(0x7C, 0x3A, 0xED) : ("LOCKED".equalsIgnoreCase(r.role()) ? new Color(0xDC, 0x26, 0x26) : new Color(0x47, 0x55, 0x69)));
        lblRoleTag.setForeground(Color.WHITE);

        JLabel lblStatusTag = new JLabel(" " + r.status() + " ");
        lblStatusTag.setFont(Theme.SMALL_BOLD);
        lblStatusTag.setOpaque(true);
        lblStatusTag.setBackground(r.status().contains("Trực tuyến") ? new Color(0x05, 0x96, 0x69) : new Color(0x64, 0x74, 0x8B));
        lblStatusTag.setForeground(Color.WHITE);

        tagRow.add(lblRoleTag);
        tagRow.add(lblStatusTag);

        headerInfo.add(lblName);
        headerInfo.add(Box.createVerticalStrut(4));
        headerInfo.add(tagRow);
        header.add(headerInfo);
        dlg.add(header, BorderLayout.NORTH);

        // Body Content
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(Theme.WHITE);
        body.setBorder(new EmptyBorder(18, 24, 18, 24));

        // Stats Box (Score + Rank)
        JPanel statsBox = new JPanel(new GridLayout(1, 2, 14, 0));
        statsBox.setOpaque(false);

        JPanel cardScore = new JPanel(new BorderLayout(0, 4));
        cardScore.setBackground(new Color(0xEE, 0xF2, 0xFF));
        cardScore.setBorder(new EmptyBorder(12, 16, 12, 16));
        JLabel lblScTitle = new JLabel("ĐIỂM XẾP HẠNG");
        lblScTitle.setFont(Theme.SMALL_BOLD);
        lblScTitle.setForeground(Theme.BLUE);
        JLabel lblScVal = new JLabel(r.score() + " pts");
        lblScVal.setFont(Theme.TITLE_XL);
        lblScVal.setForeground(Theme.BLUE);
        cardScore.add(lblScTitle, BorderLayout.NORTH);
        cardScore.add(lblScVal, BorderLayout.CENTER);

        JPanel cardRank = new JPanel(new BorderLayout(0, 4));
        cardRank.setBackground(new Color(0xFA, 0xF5, 0xFF));
        cardRank.setBorder(new EmptyBorder(12, 16, 12, 16));
        JLabel lblRkTitle = new JLabel("BẬC RANK HIỆN TẠI");
        lblRkTitle.setFont(Theme.SMALL_BOLD);
        lblRkTitle.setForeground(new Color(0x7C, 0x3A, 0xED));
        JLabel lblRkVal = new JLabel(r.rankLabel());
        lblRkVal.setFont(Theme.TITLE_XL);
        lblRkVal.setForeground(new Color(0x7C, 0x3A, 0xED));
        cardRank.add(lblRkTitle, BorderLayout.NORTH);
        cardRank.add(lblRkVal, BorderLayout.CENTER);

        statsBox.add(cardScore);
        statsBox.add(cardRank);
        body.add(statsBox);
        body.add(Box.createVerticalStrut(16));

        // Info Details Rows
        body.add(createDetailRow("Họ và tên:", r.fullName().isBlank() ? "Chưa cập nhật" : r.fullName()));
        body.add(Box.createVerticalStrut(8));
        body.add(createDetailRow("Địa chỉ Email:", r.email().isBlank() ? "Chưa cập nhật" : r.email()));
        body.add(Box.createVerticalStrut(8));
        body.add(createDetailRow("Giới thiệu bản thân:", r.bio().isBlank() ? "Không có lời giới thiệu" : r.bio()));

        dlg.add(body, BorderLayout.CENTER);

        // Footer Actions Bar
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        footer.setBackground(new Color(0xF8, 0xFA, 0xFC));
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER_LIGHT));

        if (isAdminMode) {
            ModernButton btnLock = new ModernButton("LOCKED".equalsIgnoreCase(r.role()) ? "Mở Khóa" : "Khóa TK", ModernButton.Style.ORANGE);
            btnLock.setPreferredSize(new Dimension(105, 36));
            btnLock.addActionListener(e -> {
                dlg.dispose();
                quickToggleLock(r);
            });

            ModernButton btnEditFull = new ModernButton("Sửa Hồ Sơ", ModernButton.Style.PRIMARY);
            btnEditFull.setPreferredSize(new Dimension(110, 36));
            btnEditFull.addActionListener(e -> {
                dlg.dispose();
                openEditDialog(r);
            });

            footer.add(btnLock);
            footer.add(btnEditFull);
        }

        ModernButton btnClose = new ModernButton("Đóng", ModernButton.Style.SECONDARY);
        btnClose.setPreferredSize(new Dimension(85, 36));
        btnClose.addActionListener(e -> dlg.dispose());
        footer.add(btnClose);

        dlg.add(footer, BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    private JPanel createDetailRow(String label, String value) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        JLabel lblL = new JLabel(label);
        lblL.setFont(Theme.SMALL_BOLD);
        lblL.setForeground(new Color(0x64, 0x74, 0x8B));
        lblL.setPreferredSize(new Dimension(140, 24));

        JLabel lblV = new JLabel("<html><body style='font-family: Segoe UI, sans-serif; font-size: 13px; color: #1E293B;'>" + value + "</body></html>");
        lblV.setFont(Theme.BODY);

        row.add(lblL, BorderLayout.WEST);
        row.add(lblV, BorderLayout.CENTER);
        return row;
    }

    // =========================================================================
    // ADD / EDIT FORM DIALOG
    // =========================================================================
    public void openAddDialog() {
        if (!isAdminMode) {
            JOptionPane.showMessageDialog(this, "Chỉ Quản trị viên (Admin) mới có quyền thêm người chơi!", "Từ chối", JOptionPane.ERROR_MESSAGE);
            return;
        }
        showFormDialog(null);
    }

    public void openEditDialog(PlayerRecord record) {
        if (record == null) return;
        if (!isAdminMode && !record.username().equalsIgnoreCase(currentUsername)) {
            JOptionPane.showMessageDialog(this, "Bạn chỉ có quyền chỉnh sửa hồ sơ cá nhân của chính mình!", "Từ chối", JOptionPane.ERROR_MESSAGE);
            return;
        }
        showFormDialog(record);
    }

    private void showFormDialog(PlayerRecord existing) {
        boolean isEdit = (existing != null);
        Window owner = SwingUtilities.getWindowAncestor(this);
        String dlgTitle = isEdit
                ? (isAdminMode ? "Quản Trị Hồ Sơ: " + existing.username() : "Chỉnh Sửa Hồ Sơ Cá Nhân")
                : "Thêm Người Chơi Mới — Quiz Arena";

        JDialog dlg = new JDialog(owner, dlgTitle, Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setSize(520, 560);
        dlg.setLocationRelativeTo(owner);
        dlg.setResizable(false);
        dlg.setLayout(new BorderLayout());

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(isEdit ? Theme.BLUE : Theme.EMERALD);
        header.setBorder(new EmptyBorder(14, 20, 14, 20));
        JLabel lblH = new JLabel(isEdit ? "CẬP NHẬT HỒ SƠ: " + existing.username() : "THÊM NGƯỜI CHƠI MỚI");
        lblH.setFont(Theme.TITLE);
        lblH.setForeground(Theme.WHITE);
        header.add(lblH, BorderLayout.WEST);

        ModernBadge roleBadge = new ModernBadge(
                isEdit && "ADMIN".equalsIgnoreCase(existing.role()) ? "ADMIN" : (isAdminMode ? "QUẢN TRỊ" : "NGƯỜI CHƠI"),
                isAdminMode ? ModernBadge.Type.RANK : ModernBadge.Type.INFO);
        header.add(roleBadge, BorderLayout.EAST);
        dlg.add(header, BorderLayout.NORTH);

        // Form Body
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(Theme.WHITE);
        form.setBorder(new EmptyBorder(16, 24, 16, 24));

        final String[] chosenAvatar = { isEdit ? existing.avatarId() : "avatar_01" };

        // Avatar Row
        JPanel avRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        avRow.setOpaque(false);
        avRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel avPreview = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setupAntiAliasing(g2);
                AvatarPicker.paintAvatar(g2, chosenAvatar[0], 0, 0, 48);
                g2.setColor(new Color(0x38, 0xBD, 0xF8));
                g2.setStroke(new BasicStroke(2.0f));
                g2.drawOval(0, 0, 47, 47);
                g2.dispose();
            }
        };
        avPreview.setPreferredSize(new Dimension(48, 48));
        avPreview.setOpaque(false);

        ModernButton btnChangeAv = new ModernButton("Đổi Avatar", ModernButton.Style.OUTLINE_GRAY);
        btnChangeAv.setPreferredSize(new Dimension(110, 32));
        btnChangeAv.addActionListener(e -> {
            Frame frameOwner = (dlg.getOwner() instanceof Frame f) ? f : null;
            AvatarPicker picker = new AvatarPicker(frameOwner, chosenAvatar[0], (newAv) -> {
                chosenAvatar[0] = newAv;
                avPreview.repaint();
            });
            picker.setLocationRelativeTo(dlg);
            picker.setVisible(true);
        });

        avRow.add(avPreview);
        avRow.add(btnChangeAv);
        form.add(avRow);
        form.add(Box.createVerticalStrut(12));

        // Username
        JLabel lblU = new JLabel("Tên đăng nhập (3-24 ký tự):");
        lblU.setFont(Theme.SMALL_BOLD);
        lblU.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(lblU);
        form.add(Box.createVerticalStrut(4));

        JTextField tfU = new ModernTextField(isEdit ? existing.username() : "", 20);
        tfU.setEnabled(!isEdit);
        tfU.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        tfU.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(tfU);
        form.add(Box.createVerticalStrut(10));

        // Password
        JLabel lblP = new JLabel(isEdit ? "Mật khẩu mới (để trống nếu không đổi):" : "Mật khẩu (tối thiểu 4 ký tự):");
        lblP.setFont(Theme.SMALL_BOLD);
        lblP.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(lblP);
        form.add(Box.createVerticalStrut(4));

        JPasswordField pf = new JPasswordField();
        pf.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        pf.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(pf);
        form.add(Box.createVerticalStrut(10));

        // Role (Only selectable if Admin)
        JLabel lblRole = new JLabel("Vai trò người dùng:");
        lblRole.setFont(Theme.SMALL_BOLD);
        lblRole.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(lblRole);
        form.add(Box.createVerticalStrut(4));

        JComboBox<String> cboRole = new JComboBox<>(new String[]{"PLAYER", "ADMIN", "LOCKED"});
        cboRole.setFont(Theme.BODY);
        cboRole.setEnabled(isAdminMode && (!isEdit || !"admin".equalsIgnoreCase(existing.username())));
        if (isEdit) cboRole.setSelectedItem(existing.role());
        cboRole.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        cboRole.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(cboRole);
        form.add(Box.createVerticalStrut(10));

        // Full Name
        JLabel lblFn = new JLabel("Họ và tên:");
        lblFn.setFont(Theme.SMALL_BOLD);
        lblFn.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(lblFn);
        form.add(Box.createVerticalStrut(4));

        JTextField tfFn = new ModernTextField(isEdit ? existing.fullName() : "", 20);
        tfFn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        tfFn.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(tfFn);
        form.add(Box.createVerticalStrut(10));

        // Email
        JLabel lblEm = new JLabel("Email liên hệ:");
        lblEm.setFont(Theme.SMALL_BOLD);
        lblEm.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(lblEm);
        form.add(Box.createVerticalStrut(4));

        JTextField tfEm = new ModernTextField(isEdit ? existing.email() : "", 20);
        tfEm.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        tfEm.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(tfEm);

        dlg.add(new JScrollPane(form), BorderLayout.CENTER);

        // Footer
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        footer.setBackground(Theme.WHITE);
        ModernButton btnCancel = new ModernButton("Hủy", ModernButton.Style.OUTLINE_GRAY);
        btnCancel.setPreferredSize(new Dimension(90, 36));
        btnCancel.addActionListener(e -> dlg.dispose());

        ModernButton btnSave = new ModernButton(isEdit ? "Cập Nhật" : "Thêm Người Chơi", isEdit ? ModernButton.Style.PRIMARY : ModernButton.Style.EMERALD);
        btnSave.setPreferredSize(new Dimension(140, 36));
        btnSave.addActionListener(e -> {
            SoundManager.playClick();
            String uName = tfU.getText().trim();
            String pass = new String(pf.getPassword()).trim();
            String fn = tfFn.getText().trim();
            String em = tfEm.getText().trim();
            String selectedRole = (String) cboRole.getSelectedItem();

            int score = isEdit ? existing.score() : 0;

            if (!isEdit) {
                if (!uName.matches("[A-Za-z0-9_]{3,24}")) {
                    JOptionPane.showMessageDialog(dlg, "Tên đăng nhập phải từ 3-24 ký tự (chữ, số, _)", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (pass.length() < 4) {
                    JOptionPane.showMessageDialog(dlg, "Mật khẩu phải từ 4 ký tự trở lên!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (actionListener != null) {
                    actionListener.onAddUser(uName, pass, score, chosenAvatar[0], fn, em, "", selectedRole);
                }
            } else {
                if (!pass.isEmpty() && pass.length() < 4) {
                    JOptionPane.showMessageDialog(dlg, "Mật khẩu mới phải từ 4 ký tự trở lên!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (actionListener != null) {
                    actionListener.onUpdateUser(existing.username(), pass, score, chosenAvatar[0], fn, em, existing.bio(), selectedRole);
                }
            }
            dlg.dispose();
        });

        footer.add(btnCancel);
        footer.add(btnSave);
        dlg.add(footer, BorderLayout.SOUTH);
        dlg.setVisible(true);
    }
}
