package quiz.ui;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import quiz.Rank;

/**
 * PlayerManagementPanel — Giao diện Quản lý Thêm / Sửa / Xóa thông tin người chơi
 * và chỉnh sửa Hồ sơ cá nhân.
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
        void onAddUser(String username, String password, int score, String avatarId, String fullName, String email, String bio);
        void onUpdateUser(String username, String newPassword, int score, String avatarId, String fullName, String email, String bio);
        void onDeleteUser(String username);
        void onRefresh();
    }

    private final JTable table;
    private final DefaultTableModel model;
    private final ModernTextField txtSearch;
    private final JLabel lblTotalCount;
    private final List<PlayerRecord> allRecords = new ArrayList<>();
    private final List<PlayerRecord> filteredRecords = new ArrayList<>();
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
        setBorder(new EmptyBorder(16, 24, 20, 24));

        // ==========================================
        // 1. HEADER & ACTION TOOLBAR
        // ==========================================
        JPanel topContainer = new JPanel(new BorderLayout(16, 12));
        topContainer.setOpaque(false);

        JPanel titleBox = new JPanel();
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        titleBox.setOpaque(false);

        JLabel lblTitle = new JLabel("QUẢN LÝ THÔNG TIN NGƯỜI CHƠI");
        lblTitle.setFont(Theme.TITLE);
        lblTitle.setForeground(Theme.BLUE);
        lblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblDesc = new JLabel("Thêm người chơi mới, cập nhật thông tin hồ sơ, điểm số, mật khẩu hoặc xóa người chơi (Nhấp đúp vào dòng để sửa nhanh).");
        lblDesc.setFont(Theme.SMALL);
        lblDesc.setForeground(Theme.MUTED_GRAY);
        lblDesc.setAlignmentX(Component.LEFT_ALIGNMENT);

        titleBox.add(lblTitle);
        titleBox.add(Box.createVerticalStrut(4));
        titleBox.add(lblDesc);
        topContainer.add(titleBox, BorderLayout.NORTH);

        // Toolbar: Search + CRUD Buttons
        ModernCard toolbarCard = new ModernCard(new BorderLayout(12, 0));
        toolbarCard.setCardPadding(10, 16, 10, 16);
        toolbarCard.setCardBackground(Theme.WHITE);
        toolbarCard.setBorderColor(Theme.BORDER_LIGHT);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        searchPanel.setOpaque(false);
        txtSearch = new ModernTextField("Tìm theo tên đăng nhập, họ tên, email...");
        txtSearch.setPreferredSize(new Dimension(280, 38));
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { applyFilter(); }
            @Override public void removeUpdate(DocumentEvent e) { applyFilter(); }
            @Override public void changedUpdate(DocumentEvent e) { applyFilter(); }
        });
        lblTotalCount = new JLabel("0 người chơi");
        lblTotalCount.setFont(Theme.SMALL_BOLD);
        lblTotalCount.setForeground(Theme.MUTED_GRAY);
        searchPanel.add(txtSearch);
        searchPanel.add(lblTotalCount);
        toolbarCard.add(searchPanel, BorderLayout.WEST);

        JPanel btnGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnGroup.setOpaque(false);

        ModernButton btnAdd = new ModernButton("+ Thêm Mới", ModernButton.Style.EMERALD);
        btnAdd.setPreferredSize(new Dimension(125, 38));
        btnAdd.addActionListener(e -> {
            SoundManager.playClick();
            openAddDialog();
        });

        ModernButton btnEdit = new ModernButton("Sửa Thông Tin", ModernButton.Style.PRIMARY);
        btnEdit.setPreferredSize(new Dimension(135, 38));
        btnEdit.addActionListener(e -> {
            SoundManager.playClick();
            editSelectedRow();
        });

        ModernButton btnDelete = new ModernButton("Xóa", ModernButton.Style.DANGER);
        btnDelete.setPreferredSize(new Dimension(90, 38));
        btnDelete.addActionListener(e -> {
            SoundManager.playClick();
            deleteSelectedRow();
        });

        ModernButton btnRefresh = new ModernButton("Làm Mới", ModernButton.Style.SECONDARY);
        btnRefresh.setPreferredSize(new Dimension(100, 38));
        btnRefresh.addActionListener(e -> {
            SoundManager.playClick();
            if (actionListener != null) actionListener.onRefresh();
        });

        btnGroup.add(btnAdd);
        btnGroup.add(btnEdit);
        btnGroup.add(btnDelete);
        btnGroup.add(btnRefresh);
        toolbarCard.add(btnGroup, BorderLayout.EAST);

        topContainer.add(toolbarCard, BorderLayout.SOUTH);
        add(topContainer, BorderLayout.NORTH);

        // ==========================================
        // 2. DATA TABLE
        // ==========================================
        String[] cols = {
            "Avatar", "Tài khoản", "Họ và tên", "Email", "Điểm số", "Hạng", "Trạng thái", "Giới thiệu"
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
        table.setSelectionBackground(new Color(0xDB, 0xEA, 0xFE));
        table.setSelectionForeground(Theme.DARK_TEXT);

        table.getTableHeader().setFont(Theme.BODY_BOLD);
        table.getTableHeader().setBackground(Theme.OFF_WHITE);
        table.getTableHeader().setForeground(Theme.DARK_TEXT);
        table.getTableHeader().setPreferredSize(new Dimension(0, 38));

        // Avatar column renderer
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
                    int size = 34;
                    int ox = (getWidth() - size) / 2;
                    int oy = (getHeight() - size) / 2;
                    AvatarPicker.paintAvatar(g2, currentAv, ox, oy, size);
                    g2.dispose();
                }
            };
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                currentAv = val != null ? val.toString() : "avatar_01";
                String rowUser = (String) model.getValueAt(row, 1);
                boolean isMe = rowUser != null && rowUser.equals(currentUsername);
                avCell.setBackground(sel ? t.getSelectionBackground() : (isMe ? new Color(235, 245, 255) : Theme.WHITE));
                return avCell;
            }
        });

        // General cell renderer
        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                String rowUser = (String) model.getValueAt(row, 1);
                boolean isMe = rowUser != null && rowUser.equals(currentUsername);

                if (sel) {
                    c.setBackground(t.getSelectionBackground());
                } else if (isMe) {
                    c.setBackground(new Color(235, 245, 255));
                } else {
                    c.setBackground(row % 2 == 0 ? Theme.WHITE : new Color(0xF8, 0xFA, 0xFC));
                }

                setFont((col == 1 || col == 4 || col == 5 || isMe) ? Theme.BODY_BOLD : Theme.BODY);
                setHorizontalAlignment((col == 4 || col == 5 || col == 6) ? SwingConstants.CENTER : SwingConstants.LEFT);

                if (col == 4) {
                    setForeground(Theme.BLUE);
                } else if (col == 5) {
                    setForeground(Theme.PURPLE);
                } else if (col == 6) {
                    String st = val != null ? val.toString() : "";
                    if (st.contains("Trực tuyến")) setForeground(Theme.GREEN);
                    else if (st.contains("Đang đấu")) setForeground(Theme.ORANGE_DARK);
                    else setForeground(Theme.MUTED_GRAY);
                } else {
                    setForeground(Theme.DARK_TEXT);
                }
                setBorder(new EmptyBorder(0, 10, 0, 10));
                return c;
            }
        };

        for (int c = 1; c < cols.length; c++) {
            table.getColumnModel().getColumn(c).setCellRenderer(cellRenderer);
        }
        table.getColumnModel().getColumn(1).setPreferredWidth(120);
        table.getColumnModel().getColumn(2).setPreferredWidth(140);
        table.getColumnModel().getColumn(3).setPreferredWidth(150);
        table.getColumnModel().getColumn(4).setPreferredWidth(85);
        table.getColumnModel().getColumn(5).setPreferredWidth(90);
        table.getColumnModel().getColumn(6).setPreferredWidth(100);
        table.getColumnModel().getColumn(7).setPreferredWidth(160);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) {
                    editSelectedRow();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(Theme.BORDER_LIGHT, 1));
        scrollPane.getViewport().setBackground(Theme.WHITE);
        add(scrollPane, BorderLayout.CENTER);
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
        applyFilter();
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
        filteredRecords.clear();
        model.setRowCount(0);

        for (PlayerRecord r : allRecords) {
            if (q.isEmpty()
                    || r.username().toLowerCase().contains(q)
                    || r.fullName().toLowerCase().contains(q)
                    || r.email().toLowerCase().contains(q)
                    || r.rankLabel().toLowerCase().contains(q)) {
                filteredRecords.add(r);
                String roleTag = "ADMIN".equalsIgnoreCase(r.role()) ? " [ADMIN]" : "";
                model.addRow(new Object[]{
                    r.avatarId(),
                    r.username() + roleTag + (r.username().equals(currentUsername) ? " (Bạn)" : ""),
                    r.fullName().isBlank() ? "—" : r.fullName(),
                    r.email().isBlank() ? "—" : r.email(),
                    r.score() + " đ",
                    r.rankLabel(),
                    r.status(),
                    r.bio().isBlank() ? "—" : r.bio()
                });
            }
        }
        lblTotalCount.setText(filteredRecords.size() + " / " + allRecords.size() + " người chơi");
    }

    private void editSelectedRow() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= filteredRecords.size()) {
            Window w = SwingUtilities.getWindowAncestor(this);
            if (w instanceof JFrame jf) ToastManager.warning(jf, "Vui lòng chọn một người chơi trong bảng để sửa!");
            return;
        }
        PlayerRecord rec = filteredRecords.get(row);
        openEditDialog(rec);
    }

    private void deleteSelectedRow() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= filteredRecords.size()) {
            Window w = SwingUtilities.getWindowAncestor(this);
            if (w instanceof JFrame jf) ToastManager.warning(jf, "Vui lòng chọn một người chơi trong bảng để xóa!");
            return;
        }
        PlayerRecord rec = filteredRecords.get(row);
        confirmAndDelete(rec.username());
    }

    public void confirmAndDelete(String targetUsername) {
        if (!isAdminMode) {
            Window w = SwingUtilities.getWindowAncestor(this);
            if (w instanceof JFrame jf) ToastManager.error(jf, "Chỉ Quản trị viên (Admin) mới có quyền xóa tài khoản người chơi!");
            return;
        }
        if ("admin".equalsIgnoreCase(targetUsername)) {
            Window w = SwingUtilities.getWindowAncestor(this);
            if (w instanceof JFrame jf) ToastManager.error(jf, "Không thể xóa tài khoản Quản trị viên gốc (admin)!");
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

    public void openAddDialog() {
        if (!isAdminMode) {
            Window w = SwingUtilities.getWindowAncestor(this);
            if (w instanceof JFrame jf) ToastManager.error(jf, "Chỉ Quản trị viên (Admin) mới có quyền thêm người chơi!");
            return;
        }
        showFormDialog(null);
    }

    public void openEditDialog(PlayerRecord record) {
        if (record == null) return;
        if (!isAdminMode && !record.username().equalsIgnoreCase(currentUsername)) {
            Window w = SwingUtilities.getWindowAncestor(this);
            if (w instanceof JFrame jf) ToastManager.error(jf, "Bạn chỉ có quyền chỉnh sửa hồ sơ cá nhân của chính mình!");
            return;
        }
        showFormDialog(record);
    }

    private void showFormDialog(PlayerRecord existing) {
        boolean isEdit = (existing != null);
        Window owner = SwingUtilities.getWindowAncestor(this);
        String dlgTitle = isEdit
                ? (isAdminMode ? "Quản Trị Hồ Sơ Người Chơi" : "Chỉnh Sửa Hồ Sơ Cá Nhân")
                : "Thêm Người Chơi Mới (Admin)";
        JDialog dlg = new JDialog(owner, dlgTitle, Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setSize(490, 560);
        dlg.setLocationRelativeTo(owner);
        dlg.setResizable(false);
        dlg.setLayout(new BorderLayout());

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(isEdit ? Theme.BLUE : Theme.EMERALD);
        header.setBorder(new EmptyBorder(14, 20, 14, 20));
        JLabel lblH = new JLabel(isEdit
                ? (isAdminMode ? "CẬP NHẬT HỒ SƠ: " + existing.username() : "HỒ SƠ CÁ NHÂN: " + existing.username())
                : "THÊM NGƯỜI CHƠI MỚI");
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

        // Avatar Picker Row
        JPanel avRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        avRow.setOpaque(false);
        avRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel avPreview = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setupAntiAliasing(g2);
                AvatarPicker.paintAvatar(g2, chosenAvatar[0], 0, 0, 52);
                g2.dispose();
            }
        };
        avPreview.setPreferredSize(new Dimension(52, 52));
        avPreview.setOpaque(false);

        ModernButton btnPickAv = new ModernButton("Đổi Avatar", ModernButton.Style.SECONDARY);
        btnPickAv.setPreferredSize(new Dimension(120, 34));
        btnPickAv.addActionListener(e -> {
            Frame fOwner = (owner instanceof Frame f) ? f : null;
            new AvatarPicker(fOwner, chosenAvatar[0], newId -> {
                chosenAvatar[0] = newId;
                avPreview.repaint();
            }).setVisible(true);
        });

        avRow.add(avPreview);
        avRow.add(btnPickAv);
        form.add(avRow);
        form.add(Box.createVerticalStrut(12));

        // Username
        form.add(createFieldLabel("Tên đăng nhập (3-24 ký tự chữ, số, _):"));
        ModernTextField txtUser = new ModernTextField("Nhập tên đăng nhập...");
        txtUser.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        txtUser.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (isEdit) {
            txtUser.setText(existing.username());
            txtUser.setEditable(false);
            txtUser.setEnabled(false);
        }
        form.add(txtUser);
        form.add(Box.createVerticalStrut(10));

        // Password
        form.add(createFieldLabel(isEdit ? "Mật khẩu mới (để trống nếu giữ nguyên):" : "Mật khẩu (tối thiểu 4 ký tự):"));
        ModernPasswordField txtPass = new ModernPasswordField(isEdit ? "Nhập mật khẩu mới nếu muốn đổi..." : "Nhập mật khẩu...");
        txtPass.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        txtPass.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(txtPass);
        form.add(Box.createVerticalStrut(10));

        // Full Name
        form.add(createFieldLabel("Họ và tên / Biệt danh hiển thị:"));
        ModernTextField txtFullName = new ModernTextField("Ví dụ: Nguyễn Văn A");
        txtFullName.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        txtFullName.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (isEdit) txtFullName.setText(existing.fullName());
        form.add(txtFullName);
        form.add(Box.createVerticalStrut(10));

        // Email & Score Row
        JPanel twoCol = new JPanel(new GridLayout(1, 2, 12, 0));
        twoCol.setOpaque(false);
        twoCol.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        twoCol.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel emailCol = new JPanel();
        emailCol.setLayout(new BoxLayout(emailCol, BoxLayout.Y_AXIS));
        emailCol.setOpaque(false);
        emailCol.add(createFieldLabel("Email liên hệ:"));
        ModernTextField txtEmail = new ModernTextField("example@mail.com");
        txtEmail.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (isEdit) txtEmail.setText(existing.email());
        emailCol.add(txtEmail);

        JPanel scoreCol = new JPanel();
        scoreCol.setLayout(new BoxLayout(scoreCol, BoxLayout.Y_AXIS));
        scoreCol.setOpaque(false);
        scoreCol.add(createFieldLabel(isAdminMode ? "Điểm xếp hạng (Admin):" : "Điểm xếp hạng hiện tại:"));
        JSpinner spnScore = new JSpinner(new SpinnerNumberModel(isEdit ? existing.score() : 0, 0, 100000, 1));
        spnScore.setFont(Theme.BODY_BOLD);
        spnScore.setAlignmentX(Component.LEFT_ALIGNMENT);
        spnScore.setEnabled(isAdminMode);
        scoreCol.add(spnScore);

        twoCol.add(emailCol);
        twoCol.add(scoreCol);
        form.add(twoCol);
        form.add(Box.createVerticalStrut(10));

        // Bio
        form.add(createFieldLabel("Giới thiệu bản thân (Bio):"));
        ModernTextField txtBio = new ModernTextField("Sở thích, khẩu hiệu thi đấu...");
        txtBio.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        txtBio.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (isEdit) txtBio.setText(existing.bio());
        form.add(txtBio);

        dlg.add(form, BorderLayout.CENTER);

        // Footer Actions
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(Theme.OFF_WHITE);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER_LIGHT),
                new EmptyBorder(10, 20, 10, 20)));

        if (isEdit && isAdminMode && !"admin".equalsIgnoreCase(existing.username())) {
            ModernButton btnDelInModal = new ModernButton("Xóa Tài Khoản", ModernButton.Style.DANGER);
            btnDelInModal.setPreferredSize(new Dimension(130, 38));
            btnDelInModal.addActionListener(e -> {
                SoundManager.playClick();
                dlg.dispose();
                confirmAndDelete(existing.username());
            });
            footer.add(btnDelInModal, BorderLayout.WEST);
        }

        JPanel rightBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightBtns.setOpaque(false);

        ModernButton btnCancel = new ModernButton("Hủy", ModernButton.Style.OUTLINE_GRAY);
        btnCancel.setPreferredSize(new Dimension(90, 38));
        btnCancel.addActionListener(e -> dlg.dispose());

        ModernButton btnSave = new ModernButton(isEdit ? "Lưu Thay Đổi" : "Tạo Người Chơi", ModernButton.Style.SUCCESS);
        btnSave.setPreferredSize(new Dimension(145, 38));
        btnSave.addActionListener(e -> {
            String u = txtUser.getText().trim();
            String p = new String(txtPass.getPassword());
            int sc = isAdminMode ? (Integer) spnScore.getValue() : (existing != null ? existing.score() : 0);
            String fn = txtFullName.getText().trim();
            String em = txtEmail.getText().trim();
            String bio = txtBio.getText().trim();

            if (!u.matches("[A-Za-z0-9_]{3,24}")) {
                JOptionPane.showMessageDialog(dlg, "Tên đăng nhập phải từ 3-24 ký tự (chữ, số, _)!", "Lỗi dữ liệu", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!isEdit && p.length() < 4) {
                JOptionPane.showMessageDialog(dlg, "Mật khẩu phải từ 4 ký tự trở lên!", "Lỗi dữ liệu", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (isEdit && !p.isEmpty() && p.length() < 4) {
                JOptionPane.showMessageDialog(dlg, "Mật khẩu mới phải từ 4 ký tự trở lên!", "Lỗi dữ liệu", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (actionListener != null) {
                if (isEdit) {
                    actionListener.onUpdateUser(u, p, sc, chosenAvatar[0], fn, em, bio);
                } else {
                    actionListener.onAddUser(u, p, sc, chosenAvatar[0], fn, em, bio);
                }
            }
            dlg.dispose();
        });

        rightBtns.add(btnCancel);
        rightBtns.add(btnSave);
        footer.add(rightBtns, BorderLayout.EAST);

        dlg.add(footer, BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    private JLabel createFieldLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(Theme.SMALL_BOLD);
        lbl.setForeground(Theme.DARK_TEXT);
        lbl.setBorder(new EmptyBorder(0, 0, 4, 0));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }
}
