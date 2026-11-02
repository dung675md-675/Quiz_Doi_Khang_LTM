package quiz.ui;

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import quiz.Bank;
import quiz.Model;

/**
 * QuestionManagementPanel — Tab Quản Lý Ngân Hàng Câu Hỏi dành riêng cho Admin.
 * Hỗ trợ tìm kiếm, lọc theo Chủ đề / Mức độ / Loại câu hỏi, thống kê số câu hợp lệ
 * và Thêm / Sửa / Xóa câu hỏi trực tiếp trên Server (lưu bền vững vào data/questions.txt).
 *
 * Cải tiến Admin thêm câu hỏi:
 * - Chỉ hiển thị 5 loại câu hỏi thực tế được game hỗ trợ (MULTIPLE_CHOICE, TRUE_FALSE, MULTIPLE_SELECT, FILL_IN, DRAG_AND_DROP).
 * - Biểu mẫu nhập liệu thay đổi động (dynamic form) phù hợp với loại câu hỏi được chọn.
 * - Khung hướng dẫn & quy tắc trực quan kèm ví dụ minh họa và placeholder thực tế.
 * - Kiểm tra dữ liệu nhiều lớp (Client-side & Bank.validateForAdmin) trước khi lưu.
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

    /**
     * 5 loại câu hỏi thực sự được hệ thống đối kháng hỗ trợ và sử dụng trong trận đấu.
     */
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

    private final DefaultTableModel tableModel;
    private final JTable table;
    private final ModernTextField txtSearch;
    private final JComboBox<String> cboTopicFilter;
    private final JComboBox<String> cboLevelFilter;
    private final JComboBox<String> cboTypeFilter;
    private final JLabel lblSummary;
    private final List<QuestionRecord> allQuestions = new ArrayList<>();
    private final List<QuestionRecord> filteredQuestions = new ArrayList<>();
    private QuestionActionListener actionListener;

    public QuestionManagementPanel() {
        setLayout(new BorderLayout(0, 12));
        setOpaque(false);
        setBorder(new EmptyBorder(14, 24, 14, 24));

        // 1. TOP TOOLBAR CARD
        ModernCard topCard = new ModernCard(new BorderLayout(12, 8));
        topCard.setCardPadding(14, 18, 14, 18);
        topCard.setCardBackground(Theme.WHITE);
        topCard.setBorderColor(Theme.BORDER_LIGHT);

        JPanel titleRow = new JPanel(new BorderLayout(12, 0));
        titleRow.setOpaque(false);

        JPanel leftTitle = new JPanel();
        leftTitle.setLayout(new BoxLayout(leftTitle, BoxLayout.Y_AXIS));
        leftTitle.setOpaque(false);
        JLabel lblTitle = new JLabel("QUẢN LÝ NGÂN HÀNG CÂU HỎI (ADMIN)");
        lblTitle.setFont(Theme.SUBTITLE);
        lblTitle.setForeground(Theme.DARK_TEXT);
        lblSummary = new JLabel("Tổng số: 0 câu hỏi");
        lblSummary.setFont(Theme.SMALL);
        lblSummary.setForeground(Theme.MUTED_GRAY);
        leftTitle.add(lblTitle);
        leftTitle.add(lblSummary);
        titleRow.add(leftTitle, BorderLayout.WEST);

        JPanel actionBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionBtns.setOpaque(false);

        ModernButton btnAdd = new ModernButton("+ Thêm Câu Hỏi Mới", ModernButton.Style.SUCCESS);
        btnAdd.setPreferredSize(new Dimension(175, 36));
        btnAdd.addActionListener(e -> {
            SoundManager.playClick();
            showQuestionDialog(null);
        });

        ModernButton btnEdit = new ModernButton("Sửa", ModernButton.Style.PRIMARY);
        btnEdit.setPreferredSize(new Dimension(85, 36));
        btnEdit.addActionListener(e -> {
            SoundManager.playClick();
            editSelected();
        });

        ModernButton btnDelete = new ModernButton("Xóa", ModernButton.Style.DANGER);
        btnDelete.setPreferredSize(new Dimension(85, 36));
        btnDelete.addActionListener(e -> {
            SoundManager.playClick();
            deleteSelected();
        });

        ModernButton btnRefresh = new ModernButton("Làm Mới", ModernButton.Style.SECONDARY);
        btnRefresh.setPreferredSize(new Dimension(95, 36));
        btnRefresh.addActionListener(e -> {
            SoundManager.playClick();
            if (actionListener != null) actionListener.onRefresh();
        });

        actionBtns.add(btnAdd);
        actionBtns.add(btnEdit);
        actionBtns.add(btnDelete);
        actionBtns.add(btnRefresh);
        titleRow.add(actionBtns, BorderLayout.EAST);
        topCard.add(titleRow, BorderLayout.NORTH);

        // FILTER ROW
        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        filterRow.setOpaque(false);

        txtSearch = new ModernTextField("Tìm theo nội dung hoặc mã câu hỏi...", 20);
        txtSearch.setPreferredSize(new Dimension(240, 36));
        txtSearch.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                applyFilter();
            }
        });

        List<String> topicOptions = new ArrayList<>();
        topicOptions.add("Tất cả chủ đề");
        topicOptions.addAll(Bank.STANDARD_TOPICS);
        cboTopicFilter = new JComboBox<>(topicOptions.toArray(new String[0]));
        cboTopicFilter.setFont(Theme.BODY);
        cboTopicFilter.setPreferredSize(new Dimension(200, 36));
        cboTopicFilter.addActionListener(e -> applyFilter());

        cboLevelFilter = new JComboBox<>(new String[]{"Tất cả mức độ", "Dễ", "Trung bình", "Khó"});
        cboLevelFilter.setFont(Theme.BODY);
        cboLevelFilter.setPreferredSize(new Dimension(135, 36));
        cboLevelFilter.addActionListener(e -> applyFilter());

        cboTypeFilter = new JComboBox<>(new String[]{
                "Tất cả loại",
                "Trắc nghiệm 4 đáp án",
                "Đúng / Sai",
                "Chọn nhiều đáp án",
                "Điền từ vào chỗ trống",
                "Kéo thả đáp án"
        });
        cboTypeFilter.setFont(Theme.BODY);
        cboTypeFilter.setPreferredSize(new Dimension(175, 36));
        cboTypeFilter.addActionListener(e -> applyFilter());

        filterRow.add(txtSearch);
        filterRow.add(new JLabel("Chủ đề:"));
        filterRow.add(cboTopicFilter);
        filterRow.add(new JLabel("Mức độ:"));
        filterRow.add(cboLevelFilter);
        filterRow.add(new JLabel("Loại:"));
        filterRow.add(cboTypeFilter);
        topCard.add(filterRow, BorderLayout.SOUTH);

        add(topCard, BorderLayout.NORTH);

        // 2. TABLE CARD
        ModernCard tableCard = new ModernCard(new BorderLayout());
        tableCard.setCardPadding(10, 14, 10, 14);
        tableCard.setCardBackground(Theme.WHITE);
        tableCard.setBorderColor(Theme.BORDER_LIGHT);

        String[] cols = {"Mã", "Chủ đề", "Mức độ", "Loại câu hỏi", "Nội dung câu hỏi", "Đáp án / Lựa chọn"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        table = new JTable(tableModel);
        table.setRowHeight(36);
        table.setFont(Theme.BODY);
        table.getTableHeader().setFont(Theme.BODY_BOLD);
        table.getTableHeader().setBackground(new Color(0xF1, 0xF5, 0xF9));
        table.getTableHeader().setForeground(Theme.DARK_TEXT);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setGridColor(Theme.BORDER_LIGHT);
        table.setShowVerticalLines(false);

        table.getColumnModel().getColumn(0).setPreferredWidth(75);
        table.getColumnModel().getColumn(0).setMaxWidth(95);
        table.getColumnModel().getColumn(1).setPreferredWidth(160);
        table.getColumnModel().getColumn(2).setPreferredWidth(85);
        table.getColumnModel().getColumn(2).setMaxWidth(100);
        table.getColumnModel().getColumn(3).setPreferredWidth(130);
        table.getColumnModel().getColumn(3).setMaxWidth(155);
        table.getColumnModel().getColumn(4).setPreferredWidth(340);
        table.getColumnModel().getColumn(5).setPreferredWidth(210);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editSelected();
                }
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Theme.WHITE);
        tableCard.add(scroll, BorderLayout.CENTER);
        add(tableCard, BorderLayout.CENTER);
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
        applyFilter();
    }

    /** Nhận gói QUESTION_LIST: a[0]="QUESTION_LIST", mỗi phần tử a[1..] là 1 câu hỏi (tab-separated). */
    public void updateFromWire(String[] a) {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; a != null && i < a.length; i++) {
            if (a[i] == null || a[i].isBlank()) continue;
            sb.append(a[i]).append('\n');
        }
        updateData(sb.toString());
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
                    q.choicesRaw + " [Đ/A: " + q.answerRaw + "]"
            });
        }

        if (topicSel != null && !topicSel.equals("Tất cả chủ đề")) {
            int topicCount = 0;
            for (QuestionRecord q : allQuestions) {
                if (q.topic.equalsIgnoreCase(topicSel)) topicCount++;
            }
            String warn = topicCount < 10 ? " ([!] Thiếu câu hỏi: cần ít nhất 10 câu để tạo trận!)" : " ([OK] Đủ điều kiện tạo trận)";
            lblSummary.setText("Hiển thị: " + filteredQuestions.size() + " câu | Chủ đề '" + topicSel + "': " + topicCount + " câu" + warn);
            lblSummary.setForeground(topicCount < 10 ? Theme.RED : Theme.GREEN);
        } else {
            lblSummary.setText("Hiển thị: " + filteredQuestions.size() + " / " + allQuestions.size() + " câu hỏi trong ngân hàng");
            lblSummary.setForeground(Theme.MUTED_GRAY);
        }
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

    private void editSelected() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= filteredQuestions.size()) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 câu hỏi trong bảng để sửa!", "Chưa chọn câu hỏi", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        showQuestionDialog(filteredQuestions.get(row));
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
                "Bạn có chắc chắn muốn xóa câu hỏi [" + rec.id + "]?\n\"" + rec.text + "\"",
                "Xác nhận xóa câu hỏi",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (cf == JOptionPane.YES_OPTION && actionListener != null) {
            actionListener.onDeleteQuestion(rec.id);
        }
    }

    // =========================================================================
    // HỘP THOẠI THÊM / SỬA CÂU HỎI ĐỘNG (DYNAMIC ADMIN QUESTION DIALOG)
    // =========================================================================
    private void showQuestionDialog(QuestionRecord existing) {
        Window owner = SwingUtilities.getWindowAncestor(this);
        boolean isEdit = (existing != null);
        JDialog dlg = new JDialog(owner, isEdit ? ("Chỉnh Sửa Câu Hỏi [" + existing.id + "]") : "Thêm Câu Hỏi Mới — Quiz Arena", Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setSize(740, 720);
        dlg.setMinimumSize(new Dimension(680, 620));
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout());

        // 1. TOP HEADER BANNER
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBackground(new Color(0x1F, 0x2A, 0x44)); // Deep Navy
        headerPanel.setBorder(new EmptyBorder(16, 22, 16, 22));

        JLabel lblDlgTitle = new JLabel(isEdit ? ("CHỈNH SỬA CÂU HỎI [" + existing.id + "]") : "THÊM CÂU HỎI MỚI VÀO NGÂN HÀNG ĐỀ");
        lblDlgTitle.setFont(Theme.SUBTITLE);
        lblDlgTitle.setForeground(Theme.WHITE);

        JLabel lblDlgSubtitle = new JLabel("Chỉ sử dụng 5 loại câu hỏi chuẩn được hệ thống đối kháng hỗ trợ và thi đấu trực tiếp.");
        lblDlgSubtitle.setFont(Theme.SMALL);
        lblDlgSubtitle.setForeground(new Color(0x94, 0xA3, 0xB8)); // Slate Light

        headerPanel.add(lblDlgTitle);
        headerPanel.add(Box.createVerticalStrut(4));
        headerPanel.add(lblDlgSubtitle);
        dlg.add(headerPanel, BorderLayout.NORTH);

        // 2. MAIN SCROLLABLE FORM
        JPanel formContainer = new JPanel();
        formContainer.setLayout(new BoxLayout(formContainer, BoxLayout.Y_AXIS));
        formContainer.setBackground(new Color(0xF8, 0xFA, 0xFC));
        formContainer.setBorder(new EmptyBorder(18, 22, 18, 22));

        // Group 1: Thông tin cơ bản (Chủ đề, Mức độ, Loại câu hỏi)
        JPanel metaCard = new JPanel(new GridBagLayout());
        metaCard.setBackground(Theme.WHITE);
        metaCard.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(Theme.BORDER_LIGHT, 1, true),
                new EmptyBorder(14, 16, 14, 16)
        ));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Danh sách chủ đề (gộp các chủ đề chuẩn và chủ đề đã có)
        Set<String> topicsSet = new LinkedHashSet<>(Bank.STANDARD_TOPICS);
        for (QuestionRecord q : allQuestions) {
            if (q.topic != null && !q.topic.isBlank()) topicsSet.add(q.topic);
        }
        JComboBox<String> cboTopic = new JComboBox<>(topicsSet.toArray(new String[0]));
        cboTopic.setFont(Theme.BODY);
        cboTopic.setEditable(true);
        if (isEdit) cboTopic.setSelectedItem(existing.topic);

        JComboBox<String> cboLevel = new JComboBox<>(new String[]{"Dễ", "Trung bình", "Khó"});
        cboLevel.setFont(Theme.BODY);
        if (isEdit) cboLevel.setSelectedItem(existing.level);

        // Chỉ hiển thị 5 loại câu hỏi được hệ thống hỗ trợ
        JComboBox<SupportedType> cboType = new JComboBox<>(SupportedType.values());
        cboType.setFont(Theme.BODY_BOLD);
        if (isEdit) {
            cboType.setSelectedItem(SupportedType.fromString(existing.type));
        } else {
            cboType.setSelectedItem(SupportedType.MULTIPLE_CHOICE);
        }

        // Dòng 1: Chủ đề & Mức độ
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.14;
        metaCard.add(createBoldLabel("Chủ đề:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 0.46;
        metaCard.add(cboTopic, gbc);

        gbc.gridx = 2; gbc.gridy = 0; gbc.weightx = 0.14;
        metaCard.add(createBoldLabel("Mức độ:"), gbc);
        gbc.gridx = 3; gbc.gridy = 0; gbc.weightx = 0.26;
        metaCard.add(cboLevel, gbc);

        // Dòng 2: Loại câu hỏi
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.14;
        metaCard.add(createBoldLabel("Loại câu hỏi:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.gridwidth = 3; gbc.weightx = 0.86;
        metaCard.add(cboType, gbc);
        gbc.gridwidth = 1; // reset

        formContainer.add(metaCard);
        formContainer.add(Box.createVerticalStrut(14));

        // Group 2: Khung Hướng dẫn & Quy tắc định dạng (Thay đổi theo loại được chọn)
        JPanel guideCard = new JPanel(new BorderLayout(8, 6));
        guideCard.setBackground(new Color(0xF0, 0xF9, 0xFF)); // Soft Sky Light
        guideCard.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(0xBA, 0xE6, 0xFD), 1, true),
                new EmptyBorder(10, 14, 10, 14)
        ));

        JLabel lblGuideTitle = new JLabel("HƯỚNG DẪN & QUY TẮC ĐỊNH DẠNG");
        lblGuideTitle.setFont(Theme.SMALL_BOLD);
        lblGuideTitle.setForeground(new Color(0x03, 0x69, 0xA1)); // Sky Dark

        JTextArea txtGuideInfo = new JTextArea();
        txtGuideInfo.setEditable(false);
        txtGuideInfo.setFocusable(false);
        txtGuideInfo.setLineWrap(true);
        txtGuideInfo.setWrapStyleWord(true);
        txtGuideInfo.setFont(Theme.SMALL);
        txtGuideInfo.setForeground(new Color(0x0F, 0x17, 0x2A));
        txtGuideInfo.setOpaque(false);
        txtGuideInfo.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));

        guideCard.add(lblGuideTitle, BorderLayout.NORTH);
        guideCard.add(txtGuideInfo, BorderLayout.CENTER);
        formContainer.add(guideCard);
        formContainer.add(Box.createVerticalStrut(14));

        // Group 3: Nội dung câu hỏi
        JPanel questionTextCard = new JPanel(new BorderLayout(0, 8));
        questionTextCard.setBackground(Theme.WHITE);
        questionTextCard.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(Theme.BORDER_LIGHT, 1, true),
                new EmptyBorder(14, 16, 14, 16)
        ));

        JPanel qTextHeader = new JPanel(new BorderLayout());
        qTextHeader.setOpaque(false);
        JLabel lblQText = createBoldLabel("Nội dung câu hỏi:");
        qTextHeader.add(lblQText, BorderLayout.WEST);

        // Nút chèn chỗ trống (chỉ dùng cho câu DRAG_AND_DROP)
        ModernButton btnInsertBlank = new ModernButton("+ Chèn Chỗ Trống '____'", ModernButton.Style.SECONDARY_SKY);
        btnInsertBlank.setPreferredSize(new Dimension(190, 28));
        btnInsertBlank.setFont(Theme.SMALL_BOLD);
        btnInsertBlank.setVisible(false);
        qTextHeader.add(btnInsertBlank, BorderLayout.EAST);
        questionTextCard.add(qTextHeader, BorderLayout.NORTH);

        JTextArea txtQuestion = new JTextArea(isEdit ? existing.text : "", 3, 30);
        txtQuestion.setLineWrap(true);
        txtQuestion.setWrapStyleWord(true);
        txtQuestion.setFont(Theme.BODY);
        txtQuestion.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(Theme.BORDER_LIGHT, 1),
                new EmptyBorder(6, 8, 6, 8)
        ));
        questionTextCard.add(new JScrollPane(txtQuestion), BorderLayout.CENTER);

        btnInsertBlank.addActionListener(e -> {
            int pos = txtQuestion.getCaretPosition();
            txtQuestion.insert("____", pos);
            txtQuestion.requestFocusInWindow();
        });

        formContainer.add(questionTextCard);
        formContainer.add(Box.createVerticalStrut(14));

        // Group 4: Cấu hình đáp án động theo CardLayout
        JPanel dynamicCardWrapper = new JPanel(new BorderLayout(0, 8));
        dynamicCardWrapper.setBackground(Theme.WHITE);
        dynamicCardWrapper.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(Theme.BORDER_LIGHT, 1, true),
                new EmptyBorder(14, 16, 14, 16)
        ));

        JLabel lblAnswerSection = createBoldLabel("Cấu hình đáp án & phương án trả lời:");
        dynamicCardWrapper.add(lblAnswerSection, BorderLayout.NORTH);

        CardLayout cardLayout = new CardLayout();
        JPanel pnlCards = new JPanel(cardLayout);
        pnlCards.setOpaque(false);

        // -------------------------------------------------------------
        // CARD 1: MULTIPLE_CHOICE (4 đáp án A, B, C, D)
        // -------------------------------------------------------------
        JPanel cardMC = new JPanel(new GridLayout(4, 1, 0, 8));
        cardMC.setOpaque(false);

        JTextField txtChoiceA = new JTextField();
        JTextField txtChoiceB = new JTextField();
        JTextField txtChoiceC = new JTextField();
        JTextField txtChoiceD = new JTextField();
        JRadioButton rbMcA = new JRadioButton("Đáp án đúng");
        JRadioButton rbMcB = new JRadioButton("Đáp án đúng");
        JRadioButton rbMcC = new JRadioButton("Đáp án đúng");
        JRadioButton rbMcD = new JRadioButton("Đáp án đúng");
        ButtonGroup bgMc = new ButtonGroup();
        bgMc.add(rbMcA); bgMc.add(rbMcB); bgMc.add(rbMcC); bgMc.add(rbMcD);
        rbMcA.setSelected(true);

        cardMC.add(createChoiceRow("A", Theme.RED, txtChoiceA, rbMcA, "Nhập phương án A..."));
        cardMC.add(createChoiceRow("B", Theme.BLUE, txtChoiceB, rbMcB, "Nhập phương án B..."));
        cardMC.add(createChoiceRow("C", Theme.YELLOW, txtChoiceC, rbMcC, "Nhập phương án C..."));
        cardMC.add(createChoiceRow("D", Theme.GREEN, txtChoiceD, rbMcD, "Nhập phương án D..."));
        pnlCards.add(cardMC, SupportedType.MULTIPLE_CHOICE.name());

        // -------------------------------------------------------------
        // CARD 2: TRUE_FALSE (Đúng / Sai)
        // -------------------------------------------------------------
        JPanel cardTF = new JPanel();
        cardTF.setLayout(new BoxLayout(cardTF, BoxLayout.Y_AXIS));
        cardTF.setOpaque(false);

        JLabel lblTfHint = new JLabel("Lựa chọn nhận định trong câu hỏi trên là Đúng hay Sai:");
        lblTfHint.setFont(Theme.BODY);
        lblTfHint.setForeground(Theme.MUTED_GRAY);

        JPanel tfOptions = new JPanel(new FlowLayout(FlowLayout.LEFT, 24, 8));
        tfOptions.setOpaque(false);
        JRadioButton rbTfTrue = new JRadioButton("ĐÚNG (Khẳng định trong câu hỏi là chính xác)");
        JRadioButton rbTfFalse = new JRadioButton("SAI (Khẳng định trong câu hỏi là sai)");
        rbTfTrue.setFont(Theme.BODY_BOLD);
        rbTfTrue.setForeground(new Color(0x13, 0x68, 0xCE));
        rbTfFalse.setFont(Theme.BODY_BOLD);
        rbTfFalse.setForeground(new Color(0xD9, 0x77, 0x06));
        ButtonGroup bgTf = new ButtonGroup();
        bgTf.add(rbTfTrue); bgTf.add(rbTfFalse);
        rbTfTrue.setSelected(true);
        tfOptions.add(rbTfTrue);
        tfOptions.add(rbTfFalse);

        cardTF.add(lblTfHint);
        cardTF.add(tfOptions);
        pnlCards.add(cardTF, SupportedType.TRUE_FALSE.name());

        // -------------------------------------------------------------
        // CARD 3: MULTIPLE_SELECT (Chọn nhiều đáp án)
        // -------------------------------------------------------------
        JPanel cardMS = new JPanel(new GridLayout(4, 1, 0, 8));
        cardMS.setOpaque(false);

        JTextField txtMsA = new JTextField();
        JTextField txtMsB = new JTextField();
        JTextField txtMsC = new JTextField();
        JTextField txtMsD = new JTextField();
        JCheckBox chkMsA = new JCheckBox("Là đáp án đúng");
        JCheckBox chkMsB = new JCheckBox("Là đáp án đúng");
        JCheckBox chkMsC = new JCheckBox("Là đáp án đúng");
        JCheckBox chkMsD = new JCheckBox("Là đáp án đúng");

        cardMS.add(createCheckboxRow("A", Theme.RED, txtMsA, chkMsA, "Nhập phương án A..."));
        cardMS.add(createCheckboxRow("B", Theme.BLUE, txtMsB, chkMsB, "Nhập phương án B..."));
        cardMS.add(createCheckboxRow("C", Theme.YELLOW, txtMsC, chkMsC, "Nhập phương án C..."));
        cardMS.add(createCheckboxRow("D", Theme.GREEN, txtMsD, chkMsD, "Nhập phương án D..."));
        pnlCards.add(cardMS, SupportedType.MULTIPLE_SELECT.name());

        // -------------------------------------------------------------
        // CARD 4: FILL_IN (Điền từ vào chỗ trống)
        // -------------------------------------------------------------
        JPanel cardFill = new JPanel();
        cardFill.setLayout(new BoxLayout(cardFill, BoxLayout.Y_AXIS));
        cardFill.setOpaque(false);

        JLabel lblFillPrompt = new JLabel("Danh sách các đáp án đúng được chấp nhận (phân tách bởi dấu ';'):");
        lblFillPrompt.setFont(Theme.BODY_BOLD);
        lblFillPrompt.setForeground(Theme.DARK_TEXT);

        JTextField txtFillInAnswers = new JTextField();
        txtFillInAnswers.setFont(Theme.BODY);
        txtFillInAnswers.setPreferredSize(new Dimension(500, 36));

        JLabel lblFillHint = new JLabel("Hệ thống tự động so khớp không phân biệt hoa thường và không phân biệt dấu tiếng Việt.");
        lblFillHint.setFont(Theme.SMALL);
        lblFillHint.setForeground(Theme.MUTED_GRAY);

        cardFill.add(lblFillPrompt);
        cardFill.add(Box.createVerticalStrut(6));
        cardFill.add(txtFillInAnswers);
        cardFill.add(Box.createVerticalStrut(4));
        cardFill.add(lblFillHint);
        pnlCards.add(cardFill, SupportedType.FILL_IN.name());

        // -------------------------------------------------------------
        // CARD 5: DRAG_AND_DROP (Kéo thả từ vào câu)
        // -------------------------------------------------------------
        JPanel cardDnD = new JPanel(new GridBagLayout());
        cardDnD.setOpaque(false);
        GridBagConstraints dndGbc = new GridBagConstraints();
        dndGbc.insets = new Insets(5, 4, 5, 4);
        dndGbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField txtDndSlot1 = new JTextField();
        JTextField txtDndSlot2 = new JTextField();
        JTextField txtDndSlot3 = new JTextField();
        JTextField txtDndDistractors = new JTextField();

        txtDndSlot1.setFont(Theme.BODY);
        txtDndSlot2.setFont(Theme.BODY);
        txtDndSlot3.setFont(Theme.BODY);
        txtDndDistractors.setFont(Theme.BODY);

        int dRow = 0;
        addDndRow(cardDnD, dndGbc, dRow++, "Đáp án Chỗ trống 1 (____ đầu tiên):", txtDndSlot1, "Ví dụ: Kim tự tháp Giza");
        addDndRow(cardDnD, dndGbc, dRow++, "Đáp án Chỗ trống 2 (____ thứ hai):", txtDndSlot2, "Ví dụ: Ai Cập");
        addDndRow(cardDnD, dndGbc, dRow++, "Đáp án Chỗ trống 3 (nếu có):", txtDndSlot3, "Để trống nếu câu chỉ có 1 hoặc 2 chỗ trống");
        addDndRow(cardDnD, dndGbc, dRow++, "Từ gây nhiễu thêm vào ngân hàng từ (cách nhau ';'):", txtDndDistractors, "Ví dụ: Hy Lạp;Ấn Độ;Ý;Thổ Nhĩ Kỳ");

        pnlCards.add(cardDnD, SupportedType.DRAG_AND_DROP.name());
        dynamicCardWrapper.add(pnlCards, BorderLayout.CENTER);
        formContainer.add(dynamicCardWrapper);
        formContainer.add(Box.createVerticalStrut(14));

        // Group 5: Giải thích chi tiết
        JPanel expCard = new JPanel(new BorderLayout(0, 6));
        expCard.setBackground(Theme.WHITE);
        expCard.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(Theme.BORDER_LIGHT, 1, true),
                new EmptyBorder(14, 16, 14, 16)
        ));
        JLabel lblExp = createBoldLabel("Giải thích chi tiết / Kiến thức mở rộng (tùy chọn):");
        JTextField txtExplanation = new JTextField(isEdit ? existing.explanation : "");
        txtExplanation.setFont(Theme.BODY);
        txtExplanation.setPreferredSize(new Dimension(500, 36));
        expCard.add(lblExp, BorderLayout.NORTH);
        expCard.add(txtExplanation, BorderLayout.CENTER);
        formContainer.add(expCard);

        JScrollPane formScroll = new JScrollPane(formContainer);
        formScroll.setBorder(BorderFactory.createEmptyBorder());
        formScroll.getVerticalScrollBar().setUnitIncrement(16);
        dlg.add(formScroll, BorderLayout.CENTER);

        // 3. LOGIC CẬP NHẬT GIAO DIỆN KHI THAY ĐỔI LOẠI CÂU HỎI
        Runnable updateTypeUI = () -> {
            SupportedType selType = (SupportedType) cboType.getSelectedItem();
            if (selType == null) selType = SupportedType.MULTIPLE_CHOICE;
            cardLayout.show(pnlCards, selType.name());

            // Ẩn/hiện nút chèn chỗ trống
            btnInsertBlank.setVisible(selType == SupportedType.DRAG_AND_DROP);

            // Cập nhật hướng dẫn & ví dụ mẫu
            switch (selType) {
                case MULTIPLE_CHOICE -> {
                    txtGuideInfo.setText(
                            "• Cách thức hiển thị: Người chơi chọn 1 trong 4 đáp án A, B, C, D trên giao diện trận đấu.\n" +
                            "• Quy tắc nhập: Nhập đầy đủ nội dung cho cả 4 lựa chọn A, B, C, D và chọn nút tròn tương ứng với đáp án đúng duy nhất.\n" +
                            "• Ví dụ: Câu hỏi: 'Thủ đô của Việt Nam là gì?' -> A: Hà Nội (chọn là đáp án đúng), B: TP Hồ Chí Minh, C: Đà Nẵng, D: Hải Phòng.");
                }
                case TRUE_FALSE -> {
                    txtGuideInfo.setText(
                            "• Cách thức hiển thị: Người chơi bấm 1 trong 2 nút: ĐÚNG (màu xanh) hoặc SAI (màu vàng).\n" +
                            "• Quy tắc nhập: Nhập một nhận định/mệnh đề vào ô câu hỏi, sau đó chọn xem nhận định đó là ĐÚNG hay SAI. Hệ thống sẽ tự động gán 2 lựa chọn 'Đúng' và 'Sai'.\n" +
                            "• Ví dụ: 'Mặt Trời mọc ở hướng Đông và lặn ở hướng Tây.' -> Chọn: ĐÚNG.");
                }
                case MULTIPLE_SELECT -> {
                    txtGuideInfo.setText(
                            "• Cách thức hiển thị: Người chơi có thể tích chọn nhiều đáp án cùng lúc trong trận đấu.\n" +
                            "• Quy tắc nhập: Nhập đầy đủ 4 lựa chọn A, B, C, D và tích chọn các ô vuông đáp án đúng (từ 1 đến 4 đáp án). Người chơi phải chọn đúng và đủ tất cả các đáp án đúng để ghi điểm.\n" +
                            "• Ví dụ: 'Những thành phố nào trực thuộc trung ương của Việt Nam?' -> Tích chọn A: Hà Nội, B: Đà Nẵng, C: Hải Phòng.");
                }
                case FILL_IN -> {
                    txtGuideInfo.setText(
                            "• Cách thức hiển thị: Người chơi tự nhập câu trả lời từ bàn phím vào ô nhập liệu.\n" +
                            "• Quy tắc nhập: Nhập các cách viết đáp án đúng được chấp nhận, phân tách bởi dấu chấm phẩy (;). Hệ thống tự động so khớp không phân biệt chữ hoa/thường và không phân biệt dấu tiếng Việt.\n" +
                            "• Ví dụ: 'Dòng sông nào dài nhất thế giới?' -> Đáp án: Sông Nin;Sông Nile;Nin;Nile");
                }
                case DRAG_AND_DROP -> {
                    txtGuideInfo.setText(
                            "• Cách thức hiển thị: Người chơi kéo các thẻ từ ngân hàng bên dưới để ghép vào các chỗ trống trong câu.\n" +
                            "• Quy tắc nhập: Trong câu hỏi, đặt ký hiệu '____' (4 dấu gạch dưới) tại các vị trí cần điền. Nhập đáp án đúng tương ứng cho từng ô và thêm các từ gây nhiễu để tăng tính đối kháng.\n" +
                            "• Ví dụ: 'Kim tự tháp Giza nằm ở ____, còn đền Taj Mahal nằm ở ____.' -> Ô 1: Ai Cập, Ô 2: Ấn Độ, Từ gây nhiễu: Hy Lạp;Ý;Thổ Nhĩ Kỳ.");
                }
            }
            formContainer.revalidate();
            formContainer.repaint();
        };

        cboType.addActionListener(e -> updateTypeUI.run());

        // Đổ dữ liệu có sẵn nếu đang ở chế độ Sửa (Edit)
        if (isEdit) {
            SupportedType currentType = SupportedType.fromString(existing.type);
            cboType.setSelectedItem(currentType);
            String[] choices = existing.choicesRaw != null ? existing.choicesRaw.split(";", -1) : new String[0];

            switch (currentType) {
                case MULTIPLE_CHOICE -> {
                    if (choices.length > 0) txtChoiceA.setText(choices[0].trim());
                    if (choices.length > 1) txtChoiceB.setText(choices[1].trim());
                    if (choices.length > 2) txtChoiceC.setText(choices[2].trim());
                    if (choices.length > 3) txtChoiceD.setText(choices[3].trim());
                    int ansIdx = 0;
                    try { ansIdx = Integer.parseInt(existing.answerRaw.trim()); } catch (Exception ignored) {}
                    switch (ansIdx) {
                        case 1 -> rbMcB.setSelected(true);
                        case 2 -> rbMcC.setSelected(true);
                        case 3 -> rbMcD.setSelected(true);
                        default -> rbMcA.setSelected(true);
                    }
                }
                case TRUE_FALSE -> {
                    if ("1".equals(existing.answerRaw.trim())) {
                        rbTfFalse.setSelected(true);
                    } else {
                        rbTfTrue.setSelected(true);
                    }
                }
                case MULTIPLE_SELECT -> {
                    if (choices.length > 0) txtMsA.setText(choices[0].trim());
                    if (choices.length > 1) txtMsB.setText(choices[1].trim());
                    if (choices.length > 2) txtMsC.setText(choices[2].trim());
                    if (choices.length > 3) txtMsD.setText(choices[3].trim());

                    String ansStr = existing.answerRaw != null ? existing.answerRaw : "";
                    Set<Integer> checkedIndices = new HashSet<>();
                    for (String part : ansStr.split("[,;]")) {
                        try { checkedIndices.add(Integer.parseInt(part.trim())); } catch (Exception ignored) {}
                    }
                    chkMsA.setSelected(checkedIndices.contains(0));
                    chkMsB.setSelected(checkedIndices.contains(1));
                    chkMsC.setSelected(checkedIndices.contains(2));
                    chkMsD.setSelected(checkedIndices.contains(3));
                }
                case FILL_IN -> {
                    txtFillInAnswers.setText(existing.choicesRaw != null ? existing.choicesRaw : "");
                }
                case DRAG_AND_DROP -> {
                    Map<String, String> fm = new LinkedHashMap<>();
                    if (existing.fillMapRaw != null && !existing.fillMapRaw.isBlank()) {
                        for (String pair : existing.fillMapRaw.split(";")) {
                            String[] kv = pair.split("=", 2);
                            if (kv.length == 2) fm.put(kv[0].trim(), kv[1].trim());
                        }
                    }
                    if (fm.containsKey("b1")) txtDndSlot1.setText(fm.get("b1"));
                    if (fm.containsKey("b2")) txtDndSlot2.setText(fm.get("b2"));
                    if (fm.containsKey("b3")) txtDndSlot3.setText(fm.get("b3"));

                    List<String> distractors = new ArrayList<>();
                    if (choices != null) {
                        for (String c : choices) {
                            String trimmed = c.trim();
                            if (!trimmed.isEmpty() && !fm.containsValue(trimmed)) {
                                distractors.add(trimmed);
                            }
                        }
                    }
                    txtDndDistractors.setText(String.join(";", distractors));
                }
            }
        }
        updateTypeUI.run();

        // 4. FOOTER BUTTONS
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        footer.setBackground(new Color(0xF8, 0xFA, 0xFC));
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER_LIGHT));

        ModernButton btnCancel = new ModernButton("Hủy", ModernButton.Style.SECONDARY);
        btnCancel.setPreferredSize(new Dimension(100, 38));
        btnCancel.addActionListener(e -> dlg.dispose());

        ModernButton btnSave = new ModernButton(isEdit ? "Cập Nhật Câu Hỏi" : "Lưu Câu Hỏi Mới", ModernButton.Style.PRIMARY);
        btnSave.setPreferredSize(new Dimension(160, 38));

        btnSave.addActionListener(e -> {
            String topic = cboTopic.getSelectedItem() != null ? cboTopic.getSelectedItem().toString().trim() : "";
            String level = (String) cboLevel.getSelectedItem();
            SupportedType selType = (SupportedType) cboType.getSelectedItem();
            String qText = txtQuestion.getText().trim();
            String exp = txtExplanation.getText().trim();

            if (topic.isEmpty()) {
                showValidationError(dlg, "Vui lòng chọn hoặc nhập Chủ đề cho câu hỏi!", cboTopic);
                return;
            }
            if (level == null || level.isBlank()) {
                showValidationError(dlg, "Vui lòng chọn Mức độ (Dễ, Trung bình, Khó)!", cboLevel);
                return;
            }
            if (qText.isEmpty()) {
                showValidationError(dlg, "Vui lòng nhập Nội dung câu hỏi!", txtQuestion);
                return;
            }

            String choicesRaw = "";
            String answerRaw = "0";
            String blanksRaw = "";
            String fillMapRaw = "";

            switch (selType) {
                case MULTIPLE_CHOICE -> {
                    String ca = txtChoiceA.getText().trim();
                    String cb = txtChoiceB.getText().trim();
                    String cc = txtChoiceC.getText().trim();
                    String cd = txtChoiceD.getText().trim();

                    if (ca.isEmpty()) { showValidationError(dlg, "Vui lòng nhập nội dung cho Phương án A!", txtChoiceA); return; }
                    if (cb.isEmpty()) { showValidationError(dlg, "Vui lòng nhập nội dung cho Phương án B!", txtChoiceB); return; }
                    if (cc.isEmpty()) { showValidationError(dlg, "Vui lòng nhập nội dung cho Phương án C!", txtChoiceC); return; }
                    if (cd.isEmpty()) { showValidationError(dlg, "Vui lòng nhập nội dung cho Phương án D!", txtChoiceD); return; }

                    choicesRaw = ca + ";" + cb + ";" + cc + ";" + cd;
                    int ansIdx = rbMcA.isSelected() ? 0 : (rbMcB.isSelected() ? 1 : (rbMcC.isSelected() ? 2 : 3));
                    answerRaw = String.valueOf(ansIdx);
                }
                case TRUE_FALSE -> {
                    choicesRaw = "Đúng;Sai";
                    answerRaw = rbTfTrue.isSelected() ? "0" : "1";
                }
                case MULTIPLE_SELECT -> {
                    String ca = txtMsA.getText().trim();
                    String cb = txtMsB.getText().trim();
                    String cc = txtMsC.getText().trim();
                    String cd = txtMsD.getText().trim();

                    if (ca.isEmpty()) { showValidationError(dlg, "Vui lòng nhập nội dung cho Phương án A!", txtMsA); return; }
                    if (cb.isEmpty()) { showValidationError(dlg, "Vui lòng nhập nội dung cho Phương án B!", txtMsB); return; }
                    if (cc.isEmpty()) { showValidationError(dlg, "Vui lòng nhập nội dung cho Phương án C!", txtMsC); return; }
                    if (cd.isEmpty()) { showValidationError(dlg, "Vui lòng nhập nội dung cho Phương án D!", txtMsD); return; }

                    choicesRaw = ca + ";" + cb + ";" + cc + ";" + cd;
                    List<String> checked = new ArrayList<>();
                    if (chkMsA.isSelected()) checked.add("0");
                    if (chkMsB.isSelected()) checked.add("1");
                    if (chkMsC.isSelected()) checked.add("2");
                    if (chkMsD.isSelected()) checked.add("3");

                    if (checked.isEmpty()) {
                        showValidationError(dlg, "Vui lòng tích chọn ít nhất 1 đáp án đúng cho câu hỏi chọn nhiều!", chkMsA);
                        return;
                    }
                    answerRaw = String.join(",", checked);
                }
                case FILL_IN -> {
                    String rawAns = txtFillInAnswers.getText().trim();
                    if (rawAns.isEmpty()) {
                        showValidationError(dlg, "Vui lòng nhập ít nhất 1 đáp án đúng được chấp nhận!", txtFillInAnswers);
                        return;
                    }
                    List<String> cleanAnswers = new ArrayList<>();
                    for (String part : rawAns.split("[,;]")) {
                        String trimmed = part.trim();
                        if (!trimmed.isEmpty()) cleanAnswers.add(trimmed);
                    }
                    if (cleanAnswers.isEmpty()) {
                        showValidationError(dlg, "Vui lòng nhập danh sách đáp án đúng hợp lệ!", txtFillInAnswers);
                        return;
                    }
                    choicesRaw = String.join(";", cleanAnswers);
                    answerRaw = "1";
                }
                case DRAG_AND_DROP -> {
                    if (!qText.matches(".*_{2,}.*")) {
                        showValidationError(dlg, "Nội dung câu hỏi kéo thả phải chứa ít nhất một vị trí chỗ trống dạng '____'!\n(Bấm nút '+ Chèn Chỗ Trống' ở phía trên để chèn nhanh)", txtQuestion);
                        return;
                    }
                    int blankCount = qText.split("_{2,}", -1).length - 1;
                    String s1 = txtDndSlot1.getText().trim();
                    String s2 = txtDndSlot2.getText().trim();
                    String s3 = txtDndSlot3.getText().trim();

                    if (s1.isEmpty()) {
                        showValidationError(dlg, "Vui lòng nhập đáp án đúng cho Chỗ trống 1!", txtDndSlot1);
                        return;
                    }
                    if (blankCount >= 2 && s2.isEmpty()) {
                        showValidationError(dlg, "Câu hỏi có từ 2 chỗ trống trở lên, vui lòng nhập đáp án đúng cho Chỗ trống 2!", txtDndSlot2);
                        return;
                    }
                    if (blankCount >= 3 && s3.isEmpty()) {
                        showValidationError(dlg, "Câu hỏi có 3 chỗ trống, vui lòng nhập đáp án đúng cho Chỗ trống 3!", txtDndSlot3);
                        return;
                    }

                    List<String> blanksList = new ArrayList<>();
                    List<String> fillPairs = new ArrayList<>();
                    List<String> allChoices = new ArrayList<>();

                    blanksList.add("b1");
                    fillPairs.add("b1=" + s1);
                    allChoices.add(s1);

                    if (!s2.isEmpty()) {
                        blanksList.add("b2");
                        fillPairs.add("b2=" + s2);
                        allChoices.add(s2);
                    }
                    if (!s3.isEmpty()) {
                        blanksList.add("b3");
                        fillPairs.add("b3=" + s3);
                        allChoices.add(s3);
                    }

                    String dist = txtDndDistractors.getText().trim();
                    if (!dist.isEmpty()) {
                        for (String d : dist.split("[,;]")) {
                            String trimmed = d.trim();
                            if (!trimmed.isEmpty() && !allChoices.contains(trimmed)) {
                                allChoices.add(trimmed);
                            }
                        }
                    }

                    if (allChoices.size() < 2) {
                        showValidationError(dlg, "Ngân hàng thẻ từ kéo thả cần ít nhất 2 lựa chọn!\nHãy thêm từ gây nhiễu vào ô tương ứng bên dưới.", txtDndDistractors);
                        return;
                    }

                    blanksRaw = String.join(";", blanksList);
                    fillMapRaw = String.join(";", fillPairs);
                    choicesRaw = String.join(";", allChoices);
                    answerRaw = "1";
                }
            }

            // Kiểm tra tính hợp lệ chặt chẽ với Bank.validateForAdmin trước khi gửi đi
            String typeName = selType.getModelType().name();
            String[] fields = new String[]{
                    isEdit ? existing.id : "TEMP",
                    topic, level, typeName, qText, choicesRaw, answerRaw, exp, blanksRaw, fillMapRaw
            };
            Model.Question parsedQ = Bank.parseFields(fields);
            String bankErr = Bank.validateForAdmin(parsedQ);
            if (bankErr != null) {
                showValidationError(dlg, "Dữ liệu câu hỏi chưa hợp lệ: " + bankErr, null);
                return;
            }

            // Gửi dữ liệu về Server thông qua actionListener
            if (actionListener != null) {
                if (isEdit) {
                    actionListener.onUpdateQuestion(existing.id, topic, level, typeName, qText, choicesRaw, answerRaw, exp, blanksRaw, fillMapRaw);
                } else {
                    actionListener.onAddQuestion(topic, level, typeName, qText, choicesRaw, answerRaw, exp, blanksRaw, fillMapRaw);
                }
            }
            dlg.dispose();
        });

        footer.add(btnCancel);
        footer.add(btnSave);
        dlg.add(footer, BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    // =========================================================================
    // UI HELPER METHODS FOR DYNAMIC FORM
    // =========================================================================
    private JLabel createBoldLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(Theme.BODY_BOLD);
        lbl.setForeground(Theme.DARK_TEXT);
        return lbl;
    }

    private JPanel createChoiceRow(String badgeText, Color badgeBg, JTextField textField, JRadioButton radioButton, String placeholder) {
        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.setOpaque(false);

        JLabel lblBadge = new JLabel(badgeText, SwingConstants.CENTER);
        lblBadge.setOpaque(true);
        lblBadge.setBackground(badgeBg);
        lblBadge.setForeground(Theme.WHITE);
        lblBadge.setFont(Theme.BODY_BOLD);
        lblBadge.setPreferredSize(new Dimension(30, 30));
        p.add(lblBadge, BorderLayout.WEST);

        textField.setFont(Theme.BODY);
        textField.setPreferredSize(new Dimension(340, 32));
        p.add(textField, BorderLayout.CENTER);

        radioButton.setFont(Theme.BODY);
        radioButton.setOpaque(false);
        radioButton.setPreferredSize(new Dimension(115, 32));
        p.add(radioButton, BorderLayout.EAST);

        return p;
    }

    private JPanel createCheckboxRow(String badgeText, Color badgeBg, JTextField textField, JCheckBox checkBox, String placeholder) {
        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.setOpaque(false);

        JLabel lblBadge = new JLabel(badgeText, SwingConstants.CENTER);
        lblBadge.setOpaque(true);
        lblBadge.setBackground(badgeBg);
        lblBadge.setForeground(Theme.WHITE);
        lblBadge.setFont(Theme.BODY_BOLD);
        lblBadge.setPreferredSize(new Dimension(30, 30));
        p.add(lblBadge, BorderLayout.WEST);

        textField.setFont(Theme.BODY);
        textField.setPreferredSize(new Dimension(340, 32));
        p.add(textField, BorderLayout.CENTER);

        checkBox.setFont(Theme.BODY);
        checkBox.setOpaque(false);
        checkBox.setPreferredSize(new Dimension(135, 32));
        p.add(checkBox, BorderLayout.EAST);

        return p;
    }

    private void addDndRow(JPanel panel, GridBagConstraints gbc, int row, String label, JTextField textField, String hint) {
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.42;
        JLabel lbl = new JLabel(label);
        lbl.setFont(Theme.BODY_BOLD);
        lbl.setForeground(Theme.DARK_TEXT);
        panel.add(lbl, gbc);

        gbc.gridx = 1; gbc.gridy = row; gbc.weightx = 0.58;
        textField.setPreferredSize(new Dimension(320, 32));
        textField.setToolTipText(hint);
        panel.add(textField, gbc);
    }

    private void showValidationError(Component parent, String message, Component focusTarget) {
        JOptionPane.showMessageDialog(
                parent,
                message,
                "Thông Tin Chưa Hợp Lệ",
                JOptionPane.WARNING_MESSAGE
        );
        if (focusTarget != null) {
            focusTarget.requestFocusInWindow();
        }
    }
}
