package quiz.ui;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import quiz.Bank;
import quiz.Model;

/**
 * QuestionManagementPanel — Giao diện Quản Lý Ngân Hàng Câu Hỏi cao cấp dành cho Admin:
 * - Dashboard thống kê: Tổng số câu, Phân bố độ khó (Dễ/Vừa/Khó), và Cảnh báo thiếu câu theo chủ đề (< 10 câu).
 * - Bảng quản lý câu hỏi Zebra Style, hiệu ứng hover từng dòng, capsule badge màu sắc phân loại.
 * - Thanh tìm kiếm tích hợp kính lúp Java2D và bộ lọc thông minh (Chủ đề, Mức độ, Loại câu hỏi).
 * - Live Preview 2 cột: Biên soạn câu hỏi bên trái, Khung xem trước thời gian thực chuẩn MatchScreen bên phải.
 */
public class QuestionManagementPanel extends JPanel {

    public interface QuestionActionListener {
        void onRefresh();
        void onAddQuestion(String topic, String level, String type, String questionText,
                           String choicesRaw, String answerRaw, String explanation,
                           String blanksRaw, String fillMapRaw);
        void onUpdateQuestion(String id, String topic, String level, String type, String questionText,
                              String choicesRaw, String answerRaw, String explanation,
                              String blanksRaw, String fillMapRaw);
        void onDeleteQuestion(String id);
    }

    public static class QuestionRecord {
        public final String id;
        public final String topic;
        public final String level;
        public final String type;
        public final String text;
        public final String choicesRaw;
        public final String answerRaw;
        public final String explanation;
        public final String blanksRaw;
        public final String fillMapRaw;

        public QuestionRecord(String id, String topic, String level, String type, String text,
                              String choicesRaw, String answerRaw, String explanation,
                              String blanksRaw, String fillMapRaw) {
            this.id = id != null ? id : "";
            this.topic = topic != null ? topic : "Tổng hợp";
            this.level = level != null ? level : "Dễ";
            this.type = type != null ? type : "MULTIPLE_CHOICE";
            this.text = text != null ? text : "";
            this.choicesRaw = choicesRaw != null ? choicesRaw : "";
            this.answerRaw = answerRaw != null ? answerRaw : "0";
            this.explanation = explanation != null ? explanation : "";
            this.blanksRaw = blanksRaw != null ? blanksRaw : "";
            this.fillMapRaw = fillMapRaw != null ? fillMapRaw : "";
        }
    }

    public enum SupportedType {
        MULTIPLE_CHOICE(Model.QuestionType.MULTIPLE_CHOICE, "Trắc nghiệm 4 đáp án (A, B, C, D)"),
        TRUE_FALSE(Model.QuestionType.TRUE_FALSE, "Đúng / Sai (2 lựa chọn)"),
        MULTIPLE_SELECT(Model.QuestionType.MULTIPLE_SELECT, "Chọn nhiều đáp án (1 đến 4 đáp án đúng)"),
        FILL_IN(Model.QuestionType.FILL_IN, "Điền từ vào chỗ trống (Tự gõ bàn phím)"),
        DRAG_AND_DROP(Model.QuestionType.DRAG_AND_DROP, "Kéo thả đáp án vào câu (Ghép từ vào ô trống)");

        private final Model.QuestionType modelType;
        private final String displayName;

        SupportedType(Model.QuestionType modelType, String displayName) {
            this.modelType = modelType;
            this.displayName = displayName;
        }

        public Model.QuestionType getModelType() {
            return modelType;
        }

        public String getDisplayName() {
            return displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }

        public static SupportedType fromModelType(Model.QuestionType t) {
            if (t == null) return MULTIPLE_CHOICE;
            for (SupportedType st : values()) {
                if (st.modelType == t) return st;
            }
            return MULTIPLE_CHOICE;
        }

        public static SupportedType fromString(String raw) {
            if (raw == null || raw.isBlank()) return MULTIPLE_CHOICE;
            for (SupportedType st : values()) {
                if (st.name().equalsIgnoreCase(raw) || st.modelType.name().equalsIgnoreCase(raw) || st.displayName.equalsIgnoreCase(raw)) {
                    return st;
                }
            }
            return MULTIPLE_CHOICE;
        }
    }

    // --- UI Controls ---
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JTextField txtSearch;
    private final JComboBox<String> cboTopicFilter;
    private final JComboBox<String> cboLevelFilter;
    private final JComboBox<String> cboTypeFilter;
    private final JLabel lblSummary;

    // --- Dashboard KPI Labels ---
    private final JLabel lblTotalCountVal = new JLabel("0");
    private final JLabel lblEasyCountVal = new JLabel("0 (0%)");
    private final JLabel lblMediumCountVal = new JLabel("0 (0%)");
    private final JLabel lblHardCountVal = new JLabel("0 (0%)");
    private final JPanel pnlTopicAlerts = new JPanel();

    private final List<QuestionRecord> allQuestions = new ArrayList<>();
    private final List<QuestionRecord> filteredQuestions = new ArrayList<>();
    private int hoveredRow = -1;
    private QuestionActionListener actionListener;

    public QuestionManagementPanel() {
        setLayout(new BorderLayout(0, 12));
        setOpaque(false);
        setBorder(new EmptyBorder(14, 20, 16, 20));

        // ==========================================
        // 1. TOP CONTAINER: HEADER + KPI + ALERTS + TOOLBAR
        // ==========================================
        JPanel topContainer = new JPanel();
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
        topContainer.setOpaque(false);

        // Header Title
        JPanel headerTitle = new JPanel(new BorderLayout());
        headerTitle.setOpaque(false);

        JLabel title = new JLabel("QUẢN LÝ NGÂN HÀNG CÂU HỎI");
        title.setFont(Theme.TITLE);
        title.setForeground(ArenaPalette.CYAN);

        JLabel subtitle = new JLabel("Biên soạn câu hỏi đối kháng, kiểm soát độ phủ chủ đề và chuẩn bị ngân hàng đề thi");
        subtitle.setFont(Theme.SMALL);
        subtitle.setForeground(ArenaPalette.MUTED);

        headerTitle.add(title, BorderLayout.NORTH);
        headerTitle.add(subtitle, BorderLayout.SOUTH);
        topContainer.add(headerTitle);
        topContainer.add(Box.createVerticalStrut(10));

        // 4 KPI Cards với Icon Vector nghệ thuật
        JPanel kpiRow = new JPanel(new GridLayout(1, 4, 12, 0));
        kpiRow.setOpaque(false);

        kpiRow.add(createKpiCard("TỔNG SỐ CÂU HỎI", "Ngân hàng đề thi", lblTotalCountVal,
                ArenaPalette.SURFACE_RAISED, new Color(29, 45, 70), ArenaPalette.BORDER, ArenaPalette.CYAN, 0));
        kpiRow.add(createKpiCard("MỨC ĐỘ DỄ", "Cơ bản & Nhận biết", lblEasyCountVal,
                new Color(23, 54, 47), new Color(30, 67, 56), new Color(52, 99, 80), new Color(105, 226, 178), 1));
        kpiRow.add(createKpiCard("MỨC ĐỘ VỪA", "Thông hiểu & Vận dụng", lblMediumCountVal,
                new Color(57, 45, 31), new Color(67, 51, 32), new Color(112, 90, 49), new Color(246, 200, 121), 2));
        kpiRow.add(createKpiCard("MỨC ĐỘ KHÓ", "Thử thách đỉnh cao", lblHardCountVal,
                new Color(55, 28, 42), new Color(66, 32, 48), new Color(107, 50, 70), new Color(255, 141, 165), 3));

        topContainer.add(kpiRow);
        topContainer.add(Box.createVerticalStrut(10));

        // Topic Health Alert Bar
        pnlTopicAlerts.setLayout(new BorderLayout());
        pnlTopicAlerts.setOpaque(false);
        topContainer.add(pnlTopicAlerts);
        topContainer.add(Box.createVerticalStrut(10));

        // Unified Toolbar Card (Actions + Search + Filters)
        ModernCard toolbarCard = new ModernCard(new BorderLayout(10, 10));
        toolbarCard.setCardPadding(12, 16, 12, 16);
        toolbarCard.setCardBackground(ArenaPalette.SURFACE);
        toolbarCard.setBorderColor(ArenaPalette.BORDER);

        // Actions Row (Top)
        JPanel actionsRow = new JPanel(new BorderLayout(10, 0));
        actionsRow.setOpaque(false);

        lblSummary = new JLabel("Hiển thị: 0 câu hỏi");
        lblSummary.setFont(Theme.BODY_BOLD);
        lblSummary.setForeground(ArenaPalette.TEXT);
        actionsRow.add(lblSummary, BorderLayout.NORTH);

        JPanel btnGroup = new JPanel(new FlowRows(FlowLayout.LEFT, 8, 8));
        btnGroup.setOpaque(false);

        ModernButton btnAdd = new ModernButton("+ Thêm Câu Hỏi Mới", ModernButton.Style.EMERALD);
        btnAdd.setArenaStyle(true);
        btnAdd.setPreferredSize(new Dimension(175, 36));
        btnAdd.addActionListener(e -> {
            SoundManager.playClick();
            showLiveQuestionDialog(null);
        });

        ModernButton btnEdit = new ModernButton("Sửa", ModernButton.Style.PRIMARY);
        btnEdit.setArenaStyle(true);
        btnEdit.setPreferredSize(new Dimension(80, 36));
        btnEdit.addActionListener(e -> {
            SoundManager.playClick();
            editSelected();
        });

        ModernButton btnDelete = new ModernButton("Xóa", ModernButton.Style.DANGER);
        btnDelete.setArenaStyle(true);
        btnDelete.setPreferredSize(new Dimension(80, 36));
        btnDelete.addActionListener(e -> {
            SoundManager.playClick();
            deleteSelected();
        });

        ModernButton btnRefresh = new ModernButton("Làm Mới", ModernButton.Style.SECONDARY);
        btnRefresh.setArenaStyle(true);
        btnRefresh.setPreferredSize(new Dimension(95, 36));
        btnRefresh.addActionListener(e -> {
            SoundManager.playClick();
            if (actionListener != null) actionListener.onRefresh();
        });

        btnGroup.add(btnAdd);
        btnGroup.add(btnEdit);
        btnGroup.add(btnDelete);
        btnGroup.add(btnRefresh);
        actionsRow.add(btnGroup, BorderLayout.CENTER);
        toolbarCard.add(actionsRow, BorderLayout.NORTH);

        // Filter & Search Row (Bottom)
        JPanel filterRow = new JPanel(new FlowRows(FlowLayout.LEFT, 10, 8));
        filterRow.setOpaque(false);

        // Custom Search Box with integrated Java2D Magnifying Glass icon
        JPanel searchBox = new JPanel(new BorderLayout(8, 0)) {
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

                // Magnifying glass icon on left
                g2.setColor(ArenaPalette.MUTED);
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
        txtSearch.putClientProperty("JTextField.placeholderText", "Tìm theo nội dung, mã câu...");
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { applyFilter(); }
            @Override public void removeUpdate(DocumentEvent e) { applyFilter(); }
            @Override public void changedUpdate(DocumentEvent e) { applyFilter(); }
        });
        searchBox.add(txtSearch, BorderLayout.CENTER);

        List<String> topicOptions = new ArrayList<>();
        topicOptions.add("Tất cả chủ đề");
        topicOptions.addAll(Bank.STANDARD_TOPICS);
        cboTopicFilter = createStyledComboBox(topicOptions.toArray(new String[0]), 200);
        cboTopicFilter.addActionListener(e -> applyFilter());

        cboLevelFilter = createStyledComboBox(new String[]{"Tất cả mức độ", "Dễ", "Trung bình", "Khó"}, 135);
        cboLevelFilter.addActionListener(e -> applyFilter());

        cboTypeFilter = createStyledComboBox(new String[]{
                "Tất cả loại",
                "Trắc nghiệm 4 đáp án",
                "Đúng / Sai",
                "Chọn nhiều đáp án",
                "Điền từ vào chỗ trống",
                "Kéo thả đáp án"
        }, 180);
        cboTypeFilter.addActionListener(e -> applyFilter());

        filterRow.add(searchBox);
        filterRow.add(createFilterLabel("Chủ đề:"));
        filterRow.add(cboTopicFilter);
        filterRow.add(createFilterLabel("Mức độ:"));
        filterRow.add(cboLevelFilter);
        filterRow.add(createFilterLabel("Loại:"));
        filterRow.add(cboTypeFilter);
        toolbarCard.add(filterRow, BorderLayout.SOUTH);

        topContainer.add(toolbarCard);
        add(topContainer, BorderLayout.NORTH);

        // ==========================================
        // 2. DATA TABLE WITH ZEBRA & CAPSULE BADGES
        // ==========================================
        String[] cols = {"Mã ID", "Chủ đề", "Mức độ", "Loại câu hỏi", "Nội dung câu hỏi", "Đáp án đúng"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        table = new JTable(tableModel);
        table.setRowHeight(44);
        table.setFont(Theme.BODY);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setSelectionBackground(ArenaPalette.SURFACE_RAISED);
        table.setSelectionForeground(ArenaPalette.TEXT);

        table.getTableHeader().setFont(Theme.BODY_BOLD);
        table.getTableHeader().setBackground(ArenaPalette.SURFACE_RAISED);
        table.getTableHeader().setForeground(ArenaPalette.TEXT);
        table.getTableHeader().setPreferredSize(new Dimension(0, 40));
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ArenaPalette.BORDER));

        table.getColumnModel().getColumn(0).setPreferredWidth(85);
        table.getColumnModel().getColumn(0).setMaxWidth(105);
        table.getColumnModel().getColumn(1).setPreferredWidth(160);
        table.getColumnModel().getColumn(2).setPreferredWidth(100);
        table.getColumnModel().getColumn(2).setMaxWidth(115);
        table.getColumnModel().getColumn(3).setPreferredWidth(140);
        table.getColumnModel().getColumn(3).setMaxWidth(160);
        table.getColumnModel().getColumn(4).setPreferredWidth(360);
        table.getColumnModel().getColumn(5).setPreferredWidth(180);

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
                if (e.getClickCount() == 2) {
                    editSelected();
                }
            }
        });

        // Table Cell Renderer: Zebra, Hover, Badges
        DefaultTableCellRenderer modernRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                String strVal = (val != null) ? val.toString() : "";

                if (sel) {
                    c.setBackground(t.getSelectionBackground());
                } else if (row == hoveredRow) {
                    c.setBackground(ArenaPalette.SURFACE);
                } else {
                    c.setBackground(row % 2 == 0 ? ArenaPalette.SURFACE : ArenaPalette.SURFACE_RAISED);
                }

                setFont((col == 0 || col == 2 || col == 3) ? Theme.SMALL_BOLD : Theme.BODY);
                setHorizontalAlignment((col == 0 || col == 2 || col == 3) ? SwingConstants.CENTER : SwingConstants.LEFT);
                setBorder(new EmptyBorder(0, 12, 0, 12));

                if (col == 0) {
                    setForeground(ArenaPalette.MUTED);
                } else if (col == 2) {
                    // Level badge
                    if ("Dễ".equalsIgnoreCase(strVal)) setForeground(new Color(105, 226, 178));
                    else if ("Trung bình".equalsIgnoreCase(strVal)) setForeground(new Color(246, 200, 121));
                    else setForeground(new Color(255, 141, 165));
                } else if (col == 3) {
                    // Type badge
                    if (strVal.contains("Trắc nghiệm")) setForeground(ArenaPalette.CYAN);
                    else if (strVal.contains("Đúng / Sai")) setForeground(new Color(188, 174, 255));
                    else if (strVal.contains("Chọn nhiều")) setForeground(new Color(246, 200, 121));
                    else if (strVal.contains("Điền từ")) setForeground(new Color(105, 226, 178));
                    else setForeground(new Color(0xEA, 0x58, 0x0C));
                } else if (col == 5) {
                    setForeground(new Color(105, 226, 178));
                    setFont(Theme.BODY_BOLD);
                } else {
                    setForeground(ArenaPalette.TEXT);
                }
                return c;
            }
        };

        for (int c = 0; c < cols.length; c++) {
            table.getColumnModel().getColumn(c).setCellRenderer(modernRenderer);
        }

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(ArenaPalette.BORDER, 1));
        scroll.getViewport().setBackground(ArenaPalette.SURFACE);
        scroll.getVerticalScrollBar().setUI(new ModernScrollBarUI());
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        add(scroll, BorderLayout.CENTER);
        arrangeDashboard(topContainer, scroll);
        ArenaPalette.styleTree(this);
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

                GradientPaint gp = new GradientPaint(0, 0, ArenaPalette.SURFACE, 0, h, ArenaPalette.SURFACE_RAISED);
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, w, h, 14, 14);

                g2.setColor(ArenaPalette.BORDER);
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

                g2.setColor(readableAccent(primaryColor));
                g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int cx = icX + icSize / 2;
                int cy = icY + icSize / 2;

                if (iconType == 0) {
                    // Open Book / Quiz catalog
                    g2.drawRect(cx - 6, cy - 6, 12, 12);
                    g2.drawLine(cx, cy - 6, cx, cy + 6);
                    g2.drawLine(cx - 4, cy - 2, cx - 1, cy - 2);
                    g2.drawLine(cx + 1, cy - 2, cx + 4, cy - 2);
                } else if (iconType == 1) {
                    // Shield Check (Easy)
                    Path2D.Float sh = new Path2D.Float();
                    sh.moveTo(cx - 6, cy - 5);
                    sh.lineTo(cx + 6, cy - 5);
                    sh.lineTo(cx + 6, cy);
                    sh.quadTo(cx, cy + 7, cx - 6, cy);
                    sh.closePath();
                    g2.draw(sh);
                    g2.drawLine(cx - 3, cy, cx - 1, cy + 2);
                    g2.drawLine(cx - 1, cy + 2, cx + 3, cy - 2);
                } else if (iconType == 2) {
                    // Diamond scale (Medium)
                    Path2D.Float dm = new Path2D.Float();
                    dm.moveTo(cx, cy - 6);
                    dm.lineTo(cx + 6, cy);
                    dm.lineTo(cx, cy + 6);
                    dm.lineTo(cx - 6, cy);
                    dm.closePath();
                    g2.draw(dm);
                    g2.fillOval(cx - 2, cy - 2, 4, 4);
                } else {
                    // Flame challenge (Hard)
                    int[] xf = {cx, cx + 5, cx + 2, cx + 6, cx - 1, cx - 6, cx - 3};
                    int[] yf = {cy - 7, cy - 1, cy + 1, cy + 6, cy + 7, cy + 3, cy - 2};
                    g2.drawPolygon(xf, yf, 7);
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
        lblTitle.setForeground(readableAccent(primaryColor));
        lblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        lblValue.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        lblValue.setForeground(readableAccent(primaryColor));
        lblValue.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblSub = new JLabel(subtitle);
        lblSub.setFont(Theme.SMALL);
        lblSub.setForeground(ArenaPalette.MUTED);
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
        cb.setBackground(ArenaPalette.SURFACE_RAISED);
        return cb;
    }

    private static JLabel createFilterLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.SMALL_BOLD);
        l.setForeground(ArenaPalette.MUTED);
        return l;
    }

    public void setQuestionActionListener(QuestionActionListener listener) {
        this.actionListener = listener;
    }

    public void updateData(String rawPayload) {
        allQuestions.clear();
        if (rawPayload != null && !rawPayload.isBlank()) {
            String[] rows = rawPayload.split("\n");
            for (String r : rows) {
                if (r.isBlank()) continue;
                String[] f = r.split("\t", -1);
                if (f.length >= 7) {
                    allQuestions.add(new QuestionRecord(
                            f[0], f[1], f[2], f[3], f[4], f[5], f[6],
                            f.length > 7 ? f[7] : "",
                            f.length > 8 ? f[8] : "",
                            f.length > 9 ? f[9] : ""));
                }
            }
        }
        updateDashboardStats();
        applyFilter();
    }

    public void updateFromWire(String[] a) {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; a != null && i < a.length; i++) {
            if (a[i] == null || a[i].isBlank()) continue;
            sb.append(a[i]).append('\n');
        }
        updateData(sb.toString());
    }

    private void updateDashboardStats() {
        int total = allQuestions.size();
        int easy = 0;
        int med = 0;
        int hard = 0;
        Map<String, Integer> topicCounts = new LinkedHashMap<>();

        for (String st : Bank.STANDARD_TOPICS) topicCounts.put(st, 0);

        for (QuestionRecord q : allQuestions) {
            String lvl = q.level != null ? q.level.trim() : "Dễ";
            if ("Khó".equalsIgnoreCase(lvl)) hard++;
            else if ("Trung bình".equalsIgnoreCase(lvl)) med++;
            else easy++;

            String top = q.topic != null && !q.topic.isBlank() ? q.topic.trim() : "Tổng hợp";
            topicCounts.put(top, topicCounts.getOrDefault(top, 0) + 1);
        }

        lblTotalCountVal.setText(total + " câu");
        lblEasyCountVal.setText(easy + " (" + (total > 0 ? (easy * 100 / total) : 0) + "%)");
        lblMediumCountVal.setText(med + " (" + (total > 0 ? (med * 100 / total) : 0) + "%)");
        lblHardCountVal.setText(hard + " (" + (total > 0 ? (hard * 100 / total) : 0) + "%)");

        // Topic Health Alert Bar
        pnlTopicAlerts.removeAll();
        List<String> lowTopics = new ArrayList<>();
        for (var entry : topicCounts.entrySet()) {
            if (entry.getValue() < 10) {
                lowTopics.add(entry.getKey() + " (" + entry.getValue() + "/10 câu, thiếu " + (10 - entry.getValue()) + ")");
            }
        }

        JPanel alertBanner = new JPanel(new BorderLayout(8, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setupAntiAliasing(g2);
                int w = getWidth();
                int h = getHeight();
                Color bg = lowTopics.isEmpty() ? new Color(23, 54, 47) : new Color(57, 45, 31);
                Color border = lowTopics.isEmpty() ? new Color(52, 99, 80) : new Color(112, 90, 49);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, w, h, 10, 10);
                g2.setColor(border);
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        alertBanner.setOpaque(false);
        alertBanner.setBorder(new EmptyBorder(8, 14, 8, 14));

        if (lowTopics.isEmpty()) {
            JLabel lblOk = new JLabel("CHUẨN BỊ ĐẦU ĐỦ: Tất cả các chủ đề thi đấu đều đạt từ 10 câu trở lên (Đủ điều kiện tạo trận đấu 1v1).");
            lblOk.setFont(Theme.SMALL_BOLD);
            lblOk.setForeground(new Color(105, 226, 178));
            alertBanner.add(lblOk, BorderLayout.WEST);
        } else {
            JLabel lblWarn = new JLabel("CẢNH BÁO THIẾU CÂU HỎI (" + lowTopics.size() + " chủ đề chưa đủ 10 câu): " + String.join(" • ", lowTopics));
            lblWarn.setFont(Theme.SMALL_BOLD);
            lblWarn.setForeground(new Color(246, 200, 121));
            alertBanner.add(lblWarn, BorderLayout.CENTER);
        }

        pnlTopicAlerts.add(alertBanner, BorderLayout.CENTER);
        pnlTopicAlerts.revalidate();
        pnlTopicAlerts.repaint();
    }

    private void applyFilter() {
        filteredQuestions.clear();
        tableModel.setRowCount(0);

        String kw = txtSearch.getText() != null ? txtSearch.getText().trim().toLowerCase() : "";
        String topicSel = (String) cboTopicFilter.getSelectedItem();
        String levelSel = (String) cboLevelFilter.getSelectedItem();
        String typeSel = (String) cboTypeFilter.getSelectedItem();

        for (QuestionRecord q : allQuestions) {
            if (topicSel != null && !topicSel.equals("Tất cả chủ đề") && !q.topic.equalsIgnoreCase(topicSel)) {
                continue;
            }
            if (levelSel != null && !levelSel.equals("Tất cả mức độ") && !q.level.equalsIgnoreCase(levelSel)) {
                continue;
            }
            if (typeSel != null && !typeSel.equals("Tất cả loại")) {
                String label = formatTypeLabel(q.type);
                if (!label.equalsIgnoreCase(typeSel)) {
                    continue;
                }
            }
            if (!kw.isEmpty()) {
                boolean match = q.id.toLowerCase().contains(kw)
                        || q.text.toLowerCase().contains(kw)
                        || q.topic.toLowerCase().contains(kw)
                        || q.choicesRaw.toLowerCase().contains(kw);
                if (!match) continue;
            }
            filteredQuestions.add(q);
            tableModel.addRow(new Object[]{
                    q.id,
                    q.topic,
                    q.level,
                    formatTypeLabel(q.type),
                    q.text,
                    formatAnswerPreview(q)
            });
        }

        lblSummary.setText("Hiển thị: " + filteredQuestions.size() + " / " + allQuestions.size() + " câu hỏi trong ngân hàng đề");
    }

    private String formatAnswerPreview(QuestionRecord q) {
        String type = q.type != null ? q.type.toUpperCase() : "MULTIPLE_CHOICE";
        return switch (type) {
            case "TRUE_FALSE" -> "0".equals(q.answerRaw) ? "ĐÚNG" : "SAI";
            case "MULTIPLE_SELECT" -> "Lựa chọn: " + q.answerRaw;
            case "FILL_IN" -> q.choicesRaw.isBlank() ? "Theo fillMap" : q.choicesRaw;
            case "DRAG_AND_DROP" -> q.fillMapRaw.isBlank() ? "Ghép theo blanks" : formatFillMapForUser(q.fillMapRaw);
            default -> {
                try {
                    int ans = Integer.parseInt(q.answerRaw.trim());
                    String[] ch = q.choicesRaw.split(";");
                    char prefix = (char) ('A' + ans);
                    if (ans >= 0 && ans < ch.length) {
                        yield prefix + ". " + ch[ans];
                    }
                    yield String.valueOf(prefix);
                } catch (Exception e) {
                    yield q.answerRaw;
                }
            }
        };
    }

    private String formatTypeLabel(String type) {
        if (type == null) return "Trắc nghiệm 4 đáp án";
        return switch (type.toUpperCase()) {
            case "TRUE_FALSE" -> "Đúng / Sai";
            case "MULTIPLE_SELECT" -> "Chọn nhiều đáp án";
            case "FILL_IN" -> "Điền từ vào chỗ trống";
            case "DRAG_AND_DROP" -> "Kéo thả đáp án";
            default -> "Trắc nghiệm 4 đáp án";
        };
    }

    private static String formatFillMapForUser(String raw) {
        if (raw == null || raw.isBlank()) return "";
        if (raw.contains("=")) {
            List<String> values = new ArrayList<>();
            for (String pair : raw.split(";")) {
                int eq = pair.indexOf('=');
                if (eq >= 0) {
                    values.add(pair.substring(eq + 1).trim());
                } else if (!pair.trim().isEmpty()) {
                    values.add(pair.trim());
                }
            }
            return String.join("; ", values);
        }
        return raw;
    }

    private void editSelected() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= filteredQuestions.size()) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 câu hỏi trong bảng để sửa!", "Chưa chọn câu hỏi", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        showLiveQuestionDialog(filteredQuestions.get(row));
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= filteredQuestions.size()) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 câu hỏi trong bảng để xóa!", "Chưa chọn câu hỏi", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        QuestionRecord rec = filteredQuestions.get(row);
        int cf = JOptionPane.showConfirmDialog(
                this,
                "Bạn có chắc chắn muốn xóa câu hỏi [" + rec.id + "] khỏi ngân hàng đề?\n\"" + rec.text + "\"",
                "Xác nhận xóa câu hỏi",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (cf == JOptionPane.YES_OPTION && actionListener != null) {
            actionListener.onDeleteQuestion(rec.id);
        }
    }

    // =========================================================================
    // LIVE PREVIEW MODAL — THÊM / SỬA CÂU HỎI 2 CỘT HIỆN ĐẠI (FULL MÀN HÌNH)
    // =========================================================================
    private void showLiveQuestionDialog(QuestionRecord existing) {
        Window owner = SwingUtilities.getWindowAncestor(this);
        boolean isEdit = (existing != null);
        JDialog dlg = new JDialog(owner, isEdit ? ("Chỉnh Sửa Câu Hỏi [" + existing.id + "]") : "Thêm Câu Hỏi Mới — Quiz Arena", Dialog.ModalityType.APPLICATION_MODAL);

        // Mở FULL MÀN HÌNH ngay lập tức (tính toán chuẩn theo thanh Taskbar của Windows)
        GraphicsConfiguration gc = (owner != null && owner.getGraphicsConfiguration() != null)
                ? owner.getGraphicsConfiguration()
                : GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
        Rectangle maxBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        if (gc != null) {
            Rectangle screenBounds = gc.getBounds();
            Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(gc);
            maxBounds = new Rectangle(
                    screenBounds.x + insets.left,
                    screenBounds.y + insets.top,
                    screenBounds.width - insets.left - insets.right,
                    screenBounds.height - insets.top - insets.bottom
            );
        }
        dlg.setBounds(maxBounds);
        dlg.setMinimumSize(new Dimension(980, 640));
        dlg.setResizable(true);
        dlg.setLayout(new BorderLayout());

        // Header Banner
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(0x1E, 0x29, 0x3B));
        header.setBorder(new EmptyBorder(14, 22, 14, 22));

        JLabel lblTitle = new JLabel(isEdit ? ("CHỈNH SỬA CÂU HỎI [" + existing.id + "]") : "THÊM CÂU HỎI MỚI VÀO NGÂN HÀNG ĐỀ");
        lblTitle.setFont(Theme.SUBTITLE);
        lblTitle.setForeground(Theme.WHITE);

        JLabel lblSub = new JLabel("Form biên tập bên trái và Khung Live Preview trực tiếp bên phải mô phỏng giao diện trận đấu");
        lblSub.setFont(Theme.SMALL);
        lblSub.setForeground(new Color(0x94, 0xA3, 0xB8));

        header.add(lblTitle, BorderLayout.NORTH);
        header.add(lblSub, BorderLayout.SOUTH);
        dlg.add(header, BorderLayout.NORTH);

        // Main 2-column Container
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.46);
        splitPane.setBorder(null);
        splitPane.setDividerSize(6);
        final int initialDivider = (int) (maxBounds.width * 0.46);
        splitPane.setDividerLocation(initialDivider);

        // --- CỘT TRÁI: FORM NHẬP LIỆU ---
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBackground(ArenaPalette.SURFACE_RAISED);
        formPanel.setBorder(new EmptyBorder(18, 22, 18, 22));

        // Meta row: Topic, Level, Type sử dụng GridBagLayout chống tràn vỡ giao diện
        JPanel metaCard = new JPanel(new GridBagLayout());
        metaCard.setOpaque(false);
        metaCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        Set<String> topicsSet = new LinkedHashSet<>(Bank.STANDARD_TOPICS);
        for (QuestionRecord q : allQuestions) {
            if (q.topic != null && !q.topic.isBlank()) topicsSet.add(q.topic);
        }
        JComboBox<String> cboTopic = new JComboBox<>(topicsSet.toArray(new String[0]));
        cboTopic.setFont(Theme.BODY);
        cboTopic.setEditable(true);
        cboTopic.setPreferredSize(new Dimension(0, 36));
        if (isEdit) cboTopic.setSelectedItem(existing.topic);

        JComboBox<String> cboLevel = new JComboBox<>(new String[]{"Dễ", "Trung bình", "Khó"});
        cboLevel.setFont(Theme.BODY);
        cboLevel.setPreferredSize(new Dimension(0, 36));
        if (isEdit) cboLevel.setSelectedItem(existing.level);

        JComboBox<SupportedType> cboType = new JComboBox<>(SupportedType.values());
        cboType.setFont(Theme.BODY_BOLD);
        cboType.setPreferredSize(new Dimension(0, 36));
        if (isEdit) cboType.setSelectedItem(SupportedType.fromString(existing.type));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 4, 5, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0;
        metaCard.add(createBoldLabel("Chủ đề:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0;
        metaCard.add(cboTopic, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.0;
        metaCard.add(createBoldLabel("Mức độ:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1.0;
        metaCard.add(cboLevel, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.0;
        metaCard.add(createBoldLabel("Loại câu hỏi:"), gbc);
        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 1.0;
        metaCard.add(cboType, gbc);

        formPanel.add(metaCard);
        formPanel.add(Box.createVerticalStrut(12));

        // Question text
        JLabel lblQ = createBoldLabel("Nội dung câu hỏi:");
        lblQ.setAlignmentX(Component.LEFT_ALIGNMENT);
        formPanel.add(lblQ);
        formPanel.add(Box.createVerticalStrut(4));

        JTextArea txtQuestion = new JTextArea(isEdit ? existing.text : "", 3, 20);
        txtQuestion.setLineWrap(true);
        txtQuestion.setWrapStyleWord(true);
        txtQuestion.setFont(Theme.BODY);
        JScrollPane scrollQ = new JScrollPane(txtQuestion);
        scrollQ.setPreferredSize(new Dimension(0, 80));
        scrollQ.setAlignmentX(Component.LEFT_ALIGNMENT);
        formPanel.add(scrollQ);
        formPanel.add(Box.createVerticalStrut(10));

        // Dynamic Choices Panel (CardLayout)
        CardLayout cardLayout = new CardLayout();
        JPanel pnlCards = new JPanel(cardLayout);
        pnlCards.setOpaque(false);

        // Card 1: Multiple Choice (4 choices + radio)
        JPanel cardMC = new JPanel(new GridLayout(4, 1, 0, 6));
        cardMC.setOpaque(false);
        JTextField[] tfMC = new JTextField[4];
        JRadioButton[] rbMC = new JRadioButton[4];
        ButtonGroup bgMC = new ButtonGroup();
        String[] exChoices = (isEdit && existing.choicesRaw != null) ? existing.choicesRaw.split(";") : new String[0];
        int exAns = 0;
        try { if (isEdit) exAns = Integer.parseInt(existing.answerRaw.trim()); } catch (Exception ignored) {}

        for (int i = 0; i < 4; i++) {
            JPanel row = new JPanel(new BorderLayout(8, 0));
            row.setOpaque(false);
            char pfx = (char) ('A' + i);
            rbMC[i] = new JRadioButton(pfx + ":");
            rbMC[i].setFont(Theme.BODY_BOLD);
            rbMC[i].setOpaque(false);
            bgMC.add(rbMC[i]);
            if (i == exAns) rbMC[i].setSelected(true);

            tfMC[i] = new JTextField(i < exChoices.length ? exChoices[i].trim() : "");
            tfMC[i].setFont(Theme.BODY);

            row.add(rbMC[i], BorderLayout.WEST);
            row.add(tfMC[i], BorderLayout.CENTER);
            cardMC.add(row);
        }
        pnlCards.add(cardMC, SupportedType.MULTIPLE_CHOICE.name());

        // Card 2: True / False
        JPanel cardTF = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 6));
        cardTF.setOpaque(false);
        JRadioButton rbTrue = new JRadioButton("ĐÚNG (Chính xác)", isEdit ? "0".equals(existing.answerRaw) : true);
        JRadioButton rbFalse = new JRadioButton("SAI (Không đúng)", isEdit ? "1".equals(existing.answerRaw) : false);
        ButtonGroup bgTF = new ButtonGroup();
        bgTF.add(rbTrue);
        bgTF.add(rbFalse);
        rbTrue.setFont(Theme.BODY_BOLD);
        rbFalse.setFont(Theme.BODY_BOLD);
        cardTF.add(rbTrue);
        cardTF.add(rbFalse);
        pnlCards.add(cardTF, SupportedType.TRUE_FALSE.name());

        // Card 3: Multiple Select
        JPanel cardMS = new JPanel(new GridLayout(4, 1, 0, 6));
        cardMS.setOpaque(false);
        JTextField[] tfMS = new JTextField[4];
        JCheckBox[] cbMS = new JCheckBox[4];
        int exMask = 0;
        try {
            if (isEdit) {
                for (String p : existing.answerRaw.split("[,;]")) {
                    exMask |= (1 << Integer.parseInt(p.trim()));
                }
            }
        } catch (Exception ignored) {}

        for (int i = 0; i < 4; i++) {
            JPanel row = new JPanel(new BorderLayout(8, 0));
            row.setOpaque(false);
            char pfx = (char) ('A' + i);
            cbMS[i] = new JCheckBox(pfx + ":");
            cbMS[i].setFont(Theme.BODY_BOLD);
            cbMS[i].setOpaque(false);
            if ((exMask & (1 << i)) != 0) cbMS[i].setSelected(true);

            tfMS[i] = new JTextField(i < exChoices.length ? exChoices[i].trim() : "");
            tfMS[i].setFont(Theme.BODY);

            row.add(cbMS[i], BorderLayout.WEST);
            row.add(tfMS[i], BorderLayout.CENTER);
            cardMS.add(row);
        }
        pnlCards.add(cardMS, SupportedType.MULTIPLE_SELECT.name());

        // Card 4: Fill In
        JPanel cardFI = new JPanel(new BorderLayout(0, 6));
        cardFI.setOpaque(false);
        JLabel lblFIInfo = new JLabel("Nhập các đáp án đúng được chấp nhận (phân cách bằng dấu chấm phẩy ';'):");
        lblFIInfo.setFont(Theme.SMALL);
        JTextField tfFI = new JTextField(isEdit ? existing.choicesRaw : "");
        tfFI.setFont(Theme.BODY);
        cardFI.add(lblFIInfo, BorderLayout.NORTH);
        cardFI.add(tfFI, BorderLayout.CENTER);
        pnlCards.add(cardFI, SupportedType.FILL_IN.name());

        // Card 5: Drag & Drop (Đơn giản hóa: Không cần gõ b1, b2)
        JPanel cardDD = new JPanel(new GridLayout(3, 1, 0, 6));
        cardDD.setOpaque(false);
        JTextField tfDDPool = new JTextField(isEdit ? existing.choicesRaw : "");
        JTextField tfDDMap = new JTextField(isEdit ? formatFillMapForUser(existing.fillMapRaw) : "");
        JButton btnInsertBlank = new JButton("+ Chèn ô trống '____'");
        btnInsertBlank.setFont(Theme.SMALL_BOLD);
        btnInsertBlank.addActionListener(e -> {
            int pos = txtQuestion.getCaretPosition();
            txtQuestion.insert("____", pos);
            txtQuestion.requestFocusInWindow();
        });

        JPanel ddRow1 = new JPanel(new BorderLayout(8, 0));
        ddRow1.setOpaque(false);
        ddRow1.add(new JLabel("Từ trong Pool (cách nhau ';'):"), BorderLayout.WEST);
        ddRow1.add(tfDDPool, BorderLayout.CENTER);

        JPanel ddRow2 = new JPanel(new BorderLayout(8, 0));
        ddRow2.setOpaque(false);
        ddRow2.add(new JLabel("Đáp án đúng theo thứ tự (cách nhau ';'):"), BorderLayout.WEST);
        ddRow2.add(tfDDMap, BorderLayout.CENTER);

        cardDD.add(btnInsertBlank);
        cardDD.add(ddRow1);
        cardDD.add(ddRow2);
        pnlCards.add(cardDD, SupportedType.DRAG_AND_DROP.name());

        formPanel.add(createBoldLabel("Cấu hình đáp án & phương án:"));
        formPanel.add(Box.createVerticalStrut(4));
        formPanel.add(pnlCards);
        formPanel.add(Box.createVerticalStrut(10));

        // Explanation text
        JLabel lblExp = createBoldLabel("Giải thích kiến thức (tùy chọn):");
        formPanel.add(lblExp);
        formPanel.add(Box.createVerticalStrut(4));
        JTextArea txtExp = new JTextArea(isEdit ? existing.explanation : "", 2, 20);
        txtExp.setFont(Theme.BODY);
        txtExp.setLineWrap(true);
        txtExp.setWrapStyleWord(true);
        JScrollPane scrollExp = new JScrollPane(txtExp);
        scrollExp.setPreferredSize(new Dimension(0, 55));
        formPanel.add(scrollExp);

        JScrollPane leftScroll = new JScrollPane(formPanel);
        leftScroll.setBorder(null);
        leftScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        leftScroll.getVerticalScrollBar().setUI(new ModernScrollBarUI());
        leftScroll.getVerticalScrollBar().setUnitIncrement(16);
        leftScroll.setMinimumSize(new Dimension(520, 200));
        leftScroll.setPreferredSize(new Dimension(initialDivider, 600));
        splitPane.setLeftComponent(leftScroll);

        // --- CỘT PHẢI: KHUNG LIVE PREVIEW TRỰC TIẾP ---
        LivePreviewPanel previewPanel = new LivePreviewPanel();
        previewPanel.setMinimumSize(new Dimension(480, 200));
        splitPane.setRightComponent(previewPanel);
        dlg.add(splitPane, BorderLayout.CENTER);

        // Sync Live Preview function
        Runnable updatePreview = () -> {
            String topic = (String) cboTopic.getSelectedItem();
            String level = (String) cboLevel.getSelectedItem();
            SupportedType st = (SupportedType) cboType.getSelectedItem();
            String qText = txtQuestion.getText();
            String explanation = txtExp.getText();

            List<String> choices = new ArrayList<>();
            String correctAns = "";

            if (st == SupportedType.MULTIPLE_CHOICE) {
                for (int i = 0; i < 4; i++) {
                    choices.add(tfMC[i].getText());
                    if (rbMC[i].isSelected()) correctAns = String.valueOf(i);
                }
            } else if (st == SupportedType.TRUE_FALSE) {
                choices.add("Đúng");
                choices.add("Sai");
                correctAns = rbTrue.isSelected() ? "0" : "1";
            } else if (st == SupportedType.MULTIPLE_SELECT) {
                List<String> correctIdxs = new ArrayList<>();
                for (int i = 0; i < 4; i++) {
                    choices.add(tfMS[i].getText());
                    if (cbMS[i].isSelected()) correctIdxs.add(String.valueOf(i));
                }
                correctAns = String.join(",", correctIdxs);
            } else if (st == SupportedType.FILL_IN) {
                correctAns = tfFI.getText();
            } else if (st == SupportedType.DRAG_AND_DROP) {
                String pool = tfDDPool.getText();
                for (String p : pool.split(";")) {
                    if (!p.trim().isEmpty()) choices.add(p.trim());
                }
                correctAns = tfDDMap.getText();
            }

            previewPanel.renderPreview(topic, level, st, qText, choices, correctAns, explanation);
        };

        // Wire Document & Item Listeners
        DocumentListener docSync = new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { updatePreview.run(); }
            @Override public void removeUpdate(DocumentEvent e) { updatePreview.run(); }
            @Override public void changedUpdate(DocumentEvent e) { updatePreview.run(); }
        };

        txtQuestion.getDocument().addDocumentListener(docSync);
        txtExp.getDocument().addDocumentListener(docSync);
        tfFI.getDocument().addDocumentListener(docSync);
        tfDDPool.getDocument().addDocumentListener(docSync);
        tfDDMap.getDocument().addDocumentListener(docSync);

        for (int i = 0; i < 4; i++) {
            tfMC[i].getDocument().addDocumentListener(docSync);
            tfMS[i].getDocument().addDocumentListener(docSync);
            rbMC[i].addActionListener(e -> updatePreview.run());
            cbMS[i].addActionListener(e -> updatePreview.run());
        }
        rbTrue.addActionListener(e -> updatePreview.run());
        rbFalse.addActionListener(e -> updatePreview.run());

        cboTopic.addActionListener(e -> updatePreview.run());
        cboLevel.addActionListener(e -> updatePreview.run());
        cboType.addActionListener(e -> {
            SupportedType st = (SupportedType) cboType.getSelectedItem();
            if (st != null) {
                cardLayout.show(pnlCards, st.name());
                updatePreview.run();
            }
        });

        SupportedType initialType = (SupportedType) cboType.getSelectedItem();
        if (initialType != null) {
            cardLayout.show(pnlCards, initialType.name());
        }
        updatePreview.run();

        // Footer Bar
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        footer.setBackground(ArenaPalette.SURFACE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, ArenaPalette.BORDER));

        ModernButton btnCancel = new ModernButton("Hủy Bỏ", ModernButton.Style.OUTLINE_GRAY);
        btnCancel.setArenaStyle(true);
        btnCancel.setPreferredSize(new Dimension(100, 36));
        btnCancel.addActionListener(e -> dlg.dispose());

        ModernButton btnSave = new ModernButton(isEdit ? "Cập Nhật Câu Hỏi" : "Lưu Câu Hỏi Mới", ModernButton.Style.EMERALD);
        btnSave.setArenaStyle(true);
        btnSave.setPreferredSize(new Dimension(170, 36));
        btnSave.addActionListener(e -> {
            SoundManager.playClick();
            String topic = ((String) cboTopic.getSelectedItem()).trim();
            String level = ((String) cboLevel.getSelectedItem()).trim();
            SupportedType st = (SupportedType) cboType.getSelectedItem();
            String qText = txtQuestion.getText().trim();
            String exp = txtExp.getText().trim();

            if (topic.isEmpty()) {
                JOptionPane.showMessageDialog(dlg, "Vui lòng chọn hoặc nhập Chủ đề câu hỏi!", "Thiếu thông tin", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (qText.isEmpty()) {
                JOptionPane.showMessageDialog(dlg, "Vui lòng nhập Nội dung câu hỏi!", "Thiếu thông tin", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String choicesRaw = "";
            String answerRaw = "0";
            String blanksRaw = "";
            String fillMapRaw = "";

            if (st == SupportedType.MULTIPLE_CHOICE) {
                List<String> chs = new ArrayList<>();
                int ansIdx = 0;
                for (int i = 0; i < 4; i++) {
                    String t = tfMC[i].getText().trim();
                    if (t.isEmpty()) {
                        JOptionPane.showMessageDialog(dlg, "Vui lòng nhập đầy đủ 4 phương án A, B, C, D!", "Thiếu thông tin", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    chs.add(t);
                    if (rbMC[i].isSelected()) ansIdx = i;
                }
                choicesRaw = String.join(";", chs);
                answerRaw = String.valueOf(ansIdx);
            } else if (st == SupportedType.TRUE_FALSE) {
                choicesRaw = "Đúng;Sai";
                answerRaw = rbTrue.isSelected() ? "0" : "1";
            } else if (st == SupportedType.MULTIPLE_SELECT) {
                List<String> chs = new ArrayList<>();
                List<String> correctIdxs = new ArrayList<>();
                for (int i = 0; i < 4; i++) {
                    String t = tfMS[i].getText().trim();
                    if (t.isEmpty()) {
                        JOptionPane.showMessageDialog(dlg, "Vui lòng nhập đầy đủ 4 phương án A, B, C, D!", "Thiếu thông tin", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    chs.add(t);
                    if (cbMS[i].isSelected()) correctIdxs.add(String.valueOf(i));
                }
                if (correctIdxs.isEmpty()) {
                    JOptionPane.showMessageDialog(dlg, "Vui lòng tick chọn ít nhất 1 đáp án đúng!", "Chưa chọn đáp án đúng", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                choicesRaw = String.join(";", chs);
                answerRaw = String.join(",", correctIdxs);
            } else if (st == SupportedType.FILL_IN) {
                choicesRaw = tfFI.getText().trim();
                if (choicesRaw.isEmpty()) {
                    JOptionPane.showMessageDialog(dlg, "Vui lòng nhập ít nhất 1 từ đáp án chấp nhận!", "Thiếu đáp án", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                answerRaw = "1";
            } else if (st == SupportedType.DRAG_AND_DROP) {
                choicesRaw = tfDDPool.getText().trim();
                String rawAns = tfDDMap.getText().trim();
                if (choicesRaw.isEmpty()) {
                    JOptionPane.showMessageDialog(dlg, "Vui lòng nhập các từ trong Pool!", "Thiếu thông tin", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                if (rawAns.isEmpty()) {
                    JOptionPane.showMessageDialog(dlg, "Vui lòng nhập các đáp án đúng cho ô trống!", "Thiếu thông tin", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                String[] parts = rawAns.split(";");
                List<String> pairs = new ArrayList<>();
                List<String> bList = new ArrayList<>();
                for (int i = 0; i < parts.length; i++) {
                    String p = parts[i].trim();
                    if (p.isEmpty()) continue;
                    if (p.contains("=")) {
                        pairs.add(p);
                        int eq = p.indexOf('=');
                        bList.add(p.substring(0, eq).trim());
                    } else {
                        String bKey = "b" + (i + 1);
                        pairs.add(bKey + "=" + p);
                        bList.add(bKey);
                    }
                }
                fillMapRaw = String.join(";", pairs);
                blanksRaw = String.join(";", bList);
                answerRaw = "1";
            }

            if (isEdit) {
                if (actionListener != null) {
                    actionListener.onUpdateQuestion(existing.id, topic, level, st.getModelType().name(),
                            qText, choicesRaw, answerRaw, exp, blanksRaw, fillMapRaw);
                }
            } else {
                if (actionListener != null) {
                    actionListener.onAddQuestion(topic, level, st.getModelType().name(),
                            qText, choicesRaw, answerRaw, exp, blanksRaw, fillMapRaw);
                }
            }
            dlg.dispose();
        });

        footer.add(btnCancel);
        footer.add(btnSave);
        dlg.add(footer, BorderLayout.SOUTH);

        dlg.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
                int div = (int) (dlg.getWidth() * 0.46);
                splitPane.setDividerLocation(Math.max(520, div));
            }
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                int div = (int) (dlg.getWidth() * 0.46);
                splitPane.setDividerLocation(Math.max(520, div));
            }
        });

        SwingUtilities.invokeLater(() -> splitPane.setDividerLocation(initialDivider));
        ArenaPalette.styleTree(dlg.getContentPane());
        dlg.setVisible(true);
    }

    /**
     * LivePreviewPanel — Bảng hiển thị Live Preview mô phỏng giao diện MatchScreen.
     */
    private static class LivePreviewPanel extends JPanel {
        private String topic = "Chủ đề";
        private String level = "Dễ";
        private SupportedType type = SupportedType.MULTIPLE_CHOICE;
        private String questionText = "";
        private List<String> choices = new ArrayList<>();
        private String correctAns = "";
        private String explanation = "";

        public LivePreviewPanel() {
            setOpaque(false);
            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(16, 16, 16, 16));
        }

        public void renderPreview(String topic, String level, SupportedType type,
                                  String qText, List<String> choices, String correctAns, String exp) {
            this.topic = (topic != null && !topic.isBlank()) ? topic : "Chủ đề mẫu";
            this.level = (level != null && !level.isBlank()) ? level : "Dễ";
            this.type = type != null ? type : SupportedType.MULTIPLE_CHOICE;
            this.questionText = (qText != null && !qText.isBlank()) ? qText : "Nội dung câu hỏi hiển thị tại đây...";
            this.choices = choices != null ? new ArrayList<>(choices) : new ArrayList<>();
            this.correctAns = correctAns != null ? correctAns : "";
            this.explanation = exp != null ? exp : "";

            removeAll();

            JPanel card = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    Theme.setupAntiAliasing(g2);
                    int w = getWidth();
                    int h = getHeight();

                    g2.setColor(new Color(0x18, 0x22, 0x34));
                    g2.fillRoundRect(0, 0, w, h, 16, 16);
                    g2.setColor(new Color(0x38, 0xBD, 0xF8));
                    g2.setStroke(new BasicStroke(1.2f));
                    g2.drawRoundRect(0, 0, w - 1, h - 1, 16, 16);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            card.setOpaque(false);
            card.setBorder(new EmptyBorder(16, 20, 16, 20));

            // Top Header: Tag Bar
            JPanel topTagBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            topTagBar.setOpaque(false);
            topTagBar.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel tagPreview = new JLabel("[XEM TRƯỚC TRẬN ĐẤU]");
            tagPreview.setFont(Theme.SMALL_BOLD);
            tagPreview.setForeground(new Color(0x38, 0xBD, 0xF8));

            JLabel tagTopic = new JLabel(topic + " • " + level + " • " + type.getDisplayName());
            tagTopic.setFont(Theme.SMALL);
            tagTopic.setForeground(ArenaPalette.BORDER);

            topTagBar.add(tagPreview);
            topTagBar.add(tagTopic);
            card.add(topTagBar);
            card.add(Box.createVerticalStrut(14));

            // Question Text Label
            JLabel lblQ = new JLabel("<html><body style='width: 620px; font-family: Segoe UI, sans-serif; font-size: 16px; font-weight: bold; color: #FFFFFF; line-height: 1.45;'>"
                    + questionText + "</body></html>");
            lblQ.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.add(lblQ);
            card.add(Box.createVerticalStrut(18));

            // Choices Area
            JPanel choicesBox = new JPanel();
            choicesBox.setOpaque(false);
            choicesBox.setAlignmentX(Component.LEFT_ALIGNMENT);

            if (type == SupportedType.MULTIPLE_CHOICE) {
                choicesBox.setLayout(new GridLayout(2, 2, 12, 12));
                choicesBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));
                int correctIdx = 0;
                try { correctIdx = Integer.parseInt(correctAns.trim()); } catch (Exception ignored) {}

                for (int i = 0; i < 4; i++) {
                    String text = (i < choices.size() && !choices.get(i).isBlank()) ? choices.get(i) : ("Đáp án " + (char) ('A' + i));
                    boolean isCorrect = (i == correctIdx);
                    choicesBox.add(createPreviewChoicePill((char) ('A' + i), text, isCorrect));
                }
            } else if (type == SupportedType.TRUE_FALSE) {
                choicesBox.setLayout(new GridLayout(1, 2, 12, 0));
                choicesBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
                boolean trueIsCorrect = "0".equals(correctAns);
                choicesBox.add(createPreviewChoicePill('1', "ĐÚNG", trueIsCorrect));
                choicesBox.add(createPreviewChoicePill('0', "SAI", !trueIsCorrect));
            } else if (type == SupportedType.MULTIPLE_SELECT) {
                choicesBox.setLayout(new GridLayout(2, 2, 12, 12));
                choicesBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));
                Set<Integer> cSet = new HashSet<>();
                for (String p : correctAns.split("[,;]")) {
                    try { cSet.add(Integer.parseInt(p.trim())); } catch (Exception ignored) {}
                }
                for (int i = 0; i < 4; i++) {
                    String text = (i < choices.size() && !choices.get(i).isBlank()) ? choices.get(i) : ("Lựa chọn " + (char) ('A' + i));
                    boolean isCorrect = cSet.contains(i);
                    choicesBox.add(createPreviewChoicePill((char) ('A' + i), text, isCorrect));
                }
            } else if (type == SupportedType.FILL_IN) {
                choicesBox.setLayout(new BoxLayout(choicesBox, BoxLayout.Y_AXIS));
                JLabel lblInputSim = new JLabel("Ô nhập người chơi: [ Gõ câu trả lời vào đây... ]");
                lblInputSim.setFont(Theme.BODY);
                lblInputSim.setForeground(new Color(0x94, 0xA3, 0xB8));

                JLabel lblAnsAccept = new JLabel("Đáp án đúng được chấp nhận: " + (correctAns.isBlank() ? "Chưa cấu hình" : correctAns));
                lblAnsAccept.setFont(Theme.BODY_BOLD);
                lblAnsAccept.setForeground(new Color(0x10, 0xB9, 0x81));

                choicesBox.add(lblInputSim);
                choicesBox.add(Box.createVerticalStrut(6));
                choicesBox.add(lblAnsAccept);
            } else {
                // DRAG_AND_DROP
                choicesBox.setLayout(new BoxLayout(choicesBox, BoxLayout.Y_AXIS));
                JLabel lblDropSlots = new JLabel("Khung câu hỏi với ô thả: [ ? ]");
                lblDropSlots.setFont(Theme.BODY);
                lblDropSlots.setForeground(new Color(0x94, 0xA3, 0xB8));

                JPanel tokensPool = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
                tokensPool.setOpaque(false);
                for (String w : choices) {
                    JLabel tok = new JLabel(" " + w + " ");
                    tok.setFont(Theme.SMALL_BOLD);
                    tok.setOpaque(true);
                    tok.setBackground(new Color(0x33, 0x41, 0x55));
                    tok.setForeground(Color.WHITE);
                    tok.setBorder(new LineBorder(ArenaPalette.MUTED, 1, true));
                    tokensPool.add(tok);
                }

                choicesBox.add(lblDropSlots);
                choicesBox.add(Box.createVerticalStrut(6));
                choicesBox.add(tokensPool);
            }

            card.add(choicesBox);
            card.add(Box.createVerticalStrut(14));

            // Explanation box (giữ chiều cao tự nhiên, không bị kéo dãn)
            if (!explanation.isBlank()) {
                JPanel expBox = new JPanel(new BorderLayout(6, 6));
                expBox.setOpaque(true);
                expBox.setBackground(new Color(0x0F, 0x1E, 0x36));
                expBox.setBorder(BorderFactory.createCompoundBorder(
                        new LineBorder(new Color(0x02, 0x84, 0xC7), 1, true),
                        new EmptyBorder(10, 14, 10, 14)));
                JLabel lblExpTitle = new JLabel("Giải thích chi tiết:");
                lblExpTitle.setFont(Theme.SMALL_BOLD);
                lblExpTitle.setForeground(new Color(0x38, 0xBD, 0xF8));
                JLabel lblExpVal = new JLabel("<html><body style='width: 580px; font-family: Segoe UI, sans-serif; font-size: 13px; color: #BAE6FD; line-height: 1.45;'>"
                        + explanation + "</body></html>");
                expBox.add(lblExpTitle, BorderLayout.NORTH);
                expBox.add(lblExpVal, BorderLayout.CENTER);
                expBox.setAlignmentX(Component.LEFT_ALIGNMENT);
                expBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, expBox.getPreferredSize().height + 30));
                card.add(expBox);
            }

            card.add(Box.createVerticalGlue());

            add(card, BorderLayout.CENTER);
            revalidate();
            repaint();
        }

        private JPanel createPreviewChoicePill(char prefix, String text, boolean isCorrect) {
            JPanel pill = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    Theme.setupAntiAliasing(g2);
                    int w = getWidth();
                    int h = getHeight();

                    Color bg = isCorrect ? new Color(0x06, 0x4E, 0x3B) : new Color(0x1E, 0x29, 0x3B);
                    Color border = isCorrect ? new Color(0x10, 0xB9, 0x81) : ArenaPalette.MUTED;

                    g2.setColor(bg);
                    g2.fillRoundRect(0, 0, w, h, 10, 10);
                    g2.setColor(border);
                    g2.setStroke(new BasicStroke(isCorrect ? 1.6f : 1.0f));
                    g2.drawRoundRect(0, 0, w - 1, h - 1, 10, 10);

                    // Prefix Badge
                    g2.setColor(isCorrect ? new Color(0x10, 0xB9, 0x81) : ArenaPalette.MUTED);
                    g2.fillOval(8, (h - 22) / 2, 22, 22);

                    g2.setColor(Color.WHITE);
                    g2.setFont(Theme.SMALL_BOLD);
                    FontMetrics fm = g2.getFontMetrics();
                    String sPfx = String.valueOf(prefix);
                    g2.drawString(sPfx, 8 + (22 - fm.stringWidth(sPfx)) / 2, (h - fm.getHeight()) / 2 + fm.getAscent());

                    // Vector Checkmark when correct (Zero unicode emoji / font bug!)
                    if (isCorrect) {
                        int rx = w - 24;
                        int ry = h / 2;
                        g2.setColor(new Color(0x34, 0xD3, 0x99));
                        g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        g2.drawLine(rx - 4, ry, rx - 1, ry + 3);
                        g2.drawLine(rx - 1, ry + 3, rx + 4, ry - 3);
                    }

                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            pill.setLayout(new BorderLayout(8, 0));
            pill.setOpaque(false);
            pill.setBorder(new EmptyBorder(6, 36, 6, isCorrect ? 28 : 8));
            pill.setPreferredSize(new Dimension(200, 46));

            JLabel lbl = new JLabel("<html><body style='font-family: Segoe UI, sans-serif; font-size: 13px; color: #F1F5F9; font-weight: 500;'>" + text + "</body></html>");
            pill.add(lbl, BorderLayout.CENTER);

            return pill;
        }
    }

    private static JLabel createBoldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.SMALL_BOLD);
        l.setForeground(ArenaPalette.TEXT);
        return l;
    }

    /** The complete dashboard scrolls at small sizes while tables keep their own row scrolling. */
    private void arrangeDashboard(JPanel top, JScrollPane tableScroll) {
        remove(top);
        remove(tableScroll);
        class Dashboard extends JPanel implements Scrollable {
            Dashboard() { super(new BorderLayout(0, 12)); setOpaque(false); }
            @Override public void doLayout() { invalidateRows(top); super.doLayout(); }
            @Override public Dimension getPreferredSize() {
                return new Dimension(560, Math.max(580, top.getPreferredSize().height + 250));
            }
            @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
            @Override public int getScrollableUnitIncrement(Rectangle r, int orientation, int direction) { return 24; }
            @Override public int getScrollableBlockIncrement(Rectangle r, int orientation, int direction) { return Math.max(24, r.height - 48); }
            @Override public boolean getScrollableTracksViewportWidth() { return true; }
            @Override public boolean getScrollableTracksViewportHeight() {
                return getParent() instanceof JViewport viewport && viewport.getHeight() > getPreferredSize().height;
            }
        }
        JPanel dashboard = new Dashboard();
        dashboard.add(top, BorderLayout.NORTH);
        dashboard.add(tableScroll, BorderLayout.CENTER);
        JScrollPane page = new JScrollPane(dashboard);
        page.setBorder(null);
        page.setOpaque(false);
        page.getViewport().setOpaque(false);
        page.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        page.getVerticalScrollBar().setUnitIncrement(24);
        tableScroll.setColumnHeaderView(table.getTableHeader());
        table.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable grid, Object value,
                    boolean selected, boolean focused, int row, int column) {
                super.getTableCellRendererComponent(grid, value, selected, focused, row, column);
                setBackground(ArenaPalette.SURFACE_RAISED);
                setForeground(ArenaPalette.MUTED);
                setFont(Theme.SMALL_BOLD);
                setHorizontalAlignment(SwingConstants.CENTER);
                setBorder(new EmptyBorder(0, 10, 0, 10));
                return this;
            }
        });
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        add(page, BorderLayout.CENTER);
    }

    private static void invalidateRows(Container root) {
        root.invalidate();
        for (Component child : root.getComponents()) if (child instanceof Container nested) invalidateRows(nested);
    }

    private static final class FlowRows extends FlowLayout {
        FlowRows(int alignment, int horizontalGap, int verticalGap) {
            super(alignment, horizontalGap, verticalGap);
        }
        @Override public Dimension preferredLayoutSize(Container target) { return measured(target); }
        @Override public Dimension minimumLayoutSize(Container target) {
            Dimension size = measured(target);
            return new Dimension(0, size.height);
        }
        private Dimension measured(Container target) {
            synchronized (target.getTreeLock()) {
                int width = target.getWidth();
                Container ancestor = target.getParent();
                while (ancestor != null) {
                    if (ancestor.getWidth() > 0) width = width <= 0 ? ancestor.getWidth() : Math.min(width, ancestor.getWidth());
                    ancestor = ancestor.getParent();
                }
                if (width <= 0) width = 560;
                Insets insets = target.getInsets();
                int available = Math.max(1, width - insets.left - insets.right - getHgap() * 2);
                int rowWidth = 0, rowHeight = 0, height = getVgap() * 2;
                for (Component child : target.getComponents()) {
                    if (!child.isVisible()) continue;
                    Dimension size = child.getPreferredSize();
                    if (rowWidth > 0 && rowWidth + getHgap() + size.width > available) {
                        height += rowHeight + getVgap(); rowWidth = 0; rowHeight = 0;
                    }
                    rowWidth += (rowWidth == 0 ? 0 : getHgap()) + size.width;
                    rowHeight = Math.max(rowHeight, size.height);
                }
                return new Dimension(width, height + rowHeight + insets.top + insets.bottom);
            }
        }
    }

    private static Color readableAccent(Color color) {
        return new Color(Math.max(105, color.getRed()), Math.max(155, color.getGreen()), Math.max(155, color.getBlue()));
    }
}
