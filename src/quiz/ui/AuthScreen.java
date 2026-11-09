package quiz.ui;

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.function.BiConsumer;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * AuthScreen - Màn hình Đăng nhập / Đăng ký phong cách Quiz Battle hiện đại.
 * - Sử dụng GridBagLayout đảm bảo các trường nhập và nút bấm luôn giữ đúng kích thước, không lệch lạc khi chuyển màn hình/logout.
 * - Bảng màu & hiệu ứng Game Quiz đối kháng sinh động: Logo huy hiệu, Tab Pill chuyển chế độ, Viền focus sáng, Validation trực quan.
 */
public class AuthScreen extends ArenaBackgroundPanel {

    public enum Mode {
        LOGIN, REGISTER
    }

    private Mode currentMode = Mode.LOGIN;

    // Components
    private final ModernCard card;
    private final JLabel lblLogoBadge;
    private final JLabel lblTitle;
    private final JLabel lblSubtitle;
    private final TabButton tabLogin;
    private final TabButton tabRegister;
    private final JLabel lblUserHeader;
    private final ModernTextField txtUsername;
    private final JLabel lblUserError;
    private final JLabel lblPassHeader;
    private final ModernPasswordField txtPassword;
    private final JLabel lblPassError;
    private final JLabel lblConfirmHeader;
    private final ModernPasswordField txtConfirmPassword;
    private final JLabel lblConfirmError;
    private final JLabel lblGlobalMessage;
    private final ModernButton btnSubmit;

    // Callbacks
    private BiConsumer<String, String> onLogin;
    private BiConsumer<String, String> onRegister;

    public AuthScreen() {
        super(new GridBagLayout(), Scene.AUTH);

        // Card chính với kích thước chuẩn, viền bo góc mượt mà và đổ bóng nhẹ
        card = new ModernCard(new BorderLayout(0, 20));
        card.setPreferredSize(new Dimension(424, 500));
        card.setMinimumSize(new Dimension(360, 480));
        card.setCardPadding(28, 30, 25, 30);
        card.setCornerRadius(30);
        card.setCardBackground(new Color(14, 24, 44));
        card.setBorderColor(ArenaPalette.BORDER);
        card.setHasShadow(false);

        // ==========================================
        // 1. HEADER (Logo Badge, Title & Mode Tabs)
        // ==========================================
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setOpaque(false);

        lblLogoBadge = welcomeLabel("TÀI KHOẢN QUIZ ARENA", 11, ArenaPalette.ACCENT);

        lblTitle = new JLabel("Chào mừng trở lại");
        lblTitle.setFont(new Font(Theme.TITLE_XL.getFamily(), Font.BOLD, 25));
        lblTitle.setForeground(ArenaPalette.TEXT);
        lblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        lblSubtitle = new JLabel("Đấu Trường Trí Tuệ Đối Kháng 1v1");
        lblSubtitle.setFont(Theme.BODY);
        lblSubtitle.setForeground(ArenaPalette.MUTED);
        lblSubtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        headerPanel.add(lblLogoBadge);
        headerPanel.add(Box.createVerticalStrut(8));
        headerPanel.add(lblTitle);
        headerPanel.add(Box.createVerticalStrut(4));
        headerPanel.add(lblSubtitle);
        headerPanel.add(Box.createVerticalStrut(14));

        // TAB SWITCHER: Thiết kế dạng Pill Capsule bo tròn hiện đại
        JPanel tabWrapper = new JPanel(new BorderLayout());
        tabWrapper.setOpaque(false);
        tabWrapper.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel tabPill = new JPanel(new GridLayout(1, 2, 4, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setupAntiAliasing(g2);
                g2.setColor(new Color(9, 17, 34));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 20, 20));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        tabPill.setOpaque(false);
        tabPill.setPreferredSize(new Dimension(360, 40));
        tabPill.setBorder(new EmptyBorder(4, 4, 4, 4));

        tabLogin = new TabButton("Đăng nhập", true);
        tabRegister = new TabButton("Tạo tài khoản", false);

        tabLogin.addActionListener(e -> {
            SoundManager.playClick();
            setMode(Mode.LOGIN);
        });
        tabRegister.addActionListener(e -> {
            SoundManager.playClick();
            setMode(Mode.REGISTER);
        });

        tabPill.add(tabLogin);
        tabPill.add(tabRegister);
        tabWrapper.add(tabPill);
        headerPanel.add(tabWrapper);

        card.add(headerPanel, BorderLayout.NORTH);

        // ==========================================
        // 2. FORM BODY (GridBagLayout: chống lệch ô, chuẩn kích thước)
        // ==========================================
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        formPanel.setBorder(new EmptyBorder(0, 0, 0, 0));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;
        gbc.insets = new Insets(0, 0, 0, 0);

        // Header Username
        lblUserHeader = new JLabel("Tên đăng nhập");
        lblUserHeader.setFont(Theme.BODY_BOLD);
        lblUserHeader.setForeground(ArenaPalette.TEXT);
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 6, 0);
        formPanel.add(lblUserHeader, gbc);

        // Field Username
        txtUsername = new ModernTextField("Tên tài khoản của bạn", 20);
        txtUsername.setPreferredSize(new Dimension(360, 44));
        txtUsername.setFont(new Font(Theme.BODY.getFamily(), Font.PLAIN, 14));
        txtUsername.setArenaStyle(true);
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 2, 0);
        formPanel.add(txtUsername, gbc);

        // Error Username
        lblUserError = new JLabel(" ");
        lblUserError.setFont(Theme.SMALL_BOLD);
        lblUserError.setForeground(new Color(255, 142, 164));
        gbc.gridy = 2;
        gbc.insets = new Insets(0, 2, 5, 0);
        formPanel.add(lblUserError, gbc);

        // Header Password
        lblPassHeader = new JLabel("Mật khẩu");
        lblPassHeader.setFont(Theme.BODY_BOLD);
        lblPassHeader.setForeground(ArenaPalette.TEXT);
        gbc.gridy = 3;
        gbc.insets = new Insets(0, 0, 6, 0);
        formPanel.add(lblPassHeader, gbc);

        // Field Password
        txtPassword = new ModernPasswordField("Nhập mật khẩu", 20);
        txtPassword.setPreferredSize(new Dimension(360, 44));
        txtPassword.setFont(new Font(Theme.BODY.getFamily(), Font.PLAIN, 14));
        txtPassword.setArenaStyle(true);
        gbc.gridy = 4;
        gbc.insets = new Insets(0, 0, 2, 0);
        formPanel.add(txtPassword, gbc);

        // Error Password
        lblPassError = new JLabel(" ");
        lblPassError.setFont(Theme.SMALL_BOLD);
        lblPassError.setForeground(new Color(255, 142, 164));
        gbc.gridy = 5;
        gbc.insets = new Insets(0, 2, 5, 0);
        formPanel.add(lblPassError, gbc);

        // Header Confirm Password (cho chế độ Đăng Ký)
        lblConfirmHeader = new JLabel("Nhập lại mật khẩu");
        lblConfirmHeader.setFont(Theme.BODY_BOLD);
        lblConfirmHeader.setForeground(ArenaPalette.TEXT);
        gbc.gridy = 6;
        gbc.insets = new Insets(0, 0, 6, 0);
        formPanel.add(lblConfirmHeader, gbc);

        // Field Confirm Password
        txtConfirmPassword = new ModernPasswordField("Nhập lại mật khẩu của bạn", 20);
        txtConfirmPassword.setPreferredSize(new Dimension(360, 44));
        txtConfirmPassword.setFont(new Font(Theme.BODY.getFamily(), Font.PLAIN, 14));
        txtConfirmPassword.setArenaStyle(true);
        gbc.gridy = 7;
        gbc.insets = new Insets(0, 0, 2, 0);
        formPanel.add(txtConfirmPassword, gbc);

        // Error Confirm Password
        lblConfirmError = new JLabel(" ");
        lblConfirmError.setFont(Theme.SMALL_BOLD);
        lblConfirmError.setForeground(new Color(255, 142, 164));
        gbc.gridy = 8;
        gbc.insets = new Insets(0, 2, 4, 0);
        formPanel.add(lblConfirmError, gbc);

        // Thông báo toàn cục
        lblGlobalMessage = new JLabel(" ", SwingConstants.CENTER);
        lblGlobalMessage.setFont(Theme.SMALL_BOLD);
        lblGlobalMessage.setForeground(new Color(255, 142, 164));
        lblGlobalMessage.setPreferredSize(new Dimension(360, 20));
        gbc.gridy = 9;
        gbc.insets = new Insets(0, 0, 2, 0);
        formPanel.add(lblGlobalMessage, gbc);

        // Mặc định ẩn ô Nhập lại mật khẩu khi ở chế độ Đăng Nhập
        lblConfirmHeader.setVisible(false);
        txtConfirmPassword.setVisible(false);
        lblConfirmError.setVisible(false);

        card.add(formPanel, BorderLayout.CENTER);

        // ==========================================
        // 3. FOOTER (Action Button với Gradient / Hiệu ứng nổi bật)
        // ==========================================
        JPanel footerPanel = new JPanel(new BorderLayout(0, 10));
        footerPanel.setOpaque(false);

        btnSubmit = new ModernButton("VÀO ĐẤU TRƯỜNG", ModernButton.Style.PRIMARY);
        btnSubmit.setPreferredSize(new Dimension(340, 46));
        btnSubmit.setFont(new Font(Theme.SUBTITLE.getFamily(), Font.BOLD, 14));
        btnSubmit.setCornerRadius(14);
        btnSubmit.setArenaStyle(true);
        btnSubmit.setIcon(new ArenaIcon(ArenaIcon.Type.ARROW, 18, null));
        btnSubmit.addActionListener(e -> handleSubmit());

        footerPanel.add(btnSubmit, BorderLayout.CENTER);
        JLabel footerNote = new JLabel("KIẾN THỨC CỦA BẠN. DẤU ẤN CỦA BẠN.", SwingConstants.CENTER);
        footerNote.setFont(new Font(Theme.BODY.getFamily(), Font.BOLD, 9));
        footerNote.setForeground(ArenaPalette.MUTED);
        footerPanel.add(footerNote, BorderLayout.SOUTH);
        card.add(footerPanel, BorderLayout.SOUTH);

        // Lắng nghe phím Enter
        KeyAdapter enterSubmit = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    handleSubmit();
                }
            }
        };
        txtUsername.addKeyListener(enterSubmit);
        txtPassword.addKeyListener(enterSubmit);
        txtConfirmPassword.addKeyListener(enterSubmit);

        // Artwork stays on the left; the form remains a solid, readable surface.
        JPanel welcome = createWelcomePanel();
        welcome.setPreferredSize(new Dimension(340, 480));
        welcome.setMinimumSize(new Dimension(240, 200));
        GridBagConstraints welcomeGbc = new GridBagConstraints();
        welcomeGbc.gridx = 0;
        welcomeGbc.gridy = 0;
        welcomeGbc.weightx = 1.0;
        welcomeGbc.weighty = 1.0;
        welcomeGbc.fill = GridBagConstraints.BOTH;
        welcomeGbc.insets = new Insets(32, 40, 30, 16);
        add(welcome, welcomeGbc);

        GridBagConstraints centerGbc = new GridBagConstraints();
        centerGbc.gridx = 1;
        centerGbc.gridy = 0;
        centerGbc.weighty = 1.0;
        centerGbc.anchor = GridBagConstraints.CENTER;
        centerGbc.insets = new Insets(12, 14, 12, 36);
        add(card, centerGbc);

        lblTitle.setFont(new Font(Theme.BODY.getFamily(), Font.BOLD, 25));
        lblTitle.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        lblTitle.setHorizontalAlignment(SwingConstants.LEFT);
        lblSubtitle.setFont(new Font(Theme.BODY.getFamily(), Font.PLAIN, 12));
        lblSubtitle.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        lblSubtitle.setHorizontalAlignment(SwingConstants.LEFT);
        setLayout(new AuthLayout(welcome, card));
        setMode(Mode.LOGIN);
    }

    /** Keeps the two-column composition together on a maximized desktop. */
    private static final class AuthLayout implements LayoutManager {
        private final JPanel welcome;
        private final ModernCard form;
        AuthLayout(JPanel welcome, ModernCard form) { this.welcome = welcome; this.form = form; }
        @Override public void addLayoutComponent(String name, Component child) {}
        @Override public void removeLayoutComponent(Component child) {}
        @Override public Dimension preferredLayoutSize(Container parent) { return new Dimension(1080, 760); }
        @Override public Dimension minimumLayoutSize(Container parent) { return new Dimension(720, 520); }
        @Override public void layoutContainer(Container parent) {
            int width = parent.getWidth();
            int height = parent.getHeight();
            int content = Math.min(1240, Math.max(0, width - 64));
            int formWidth = Math.min(440, Math.max(360, (content - 32) / 2));
            int left = Math.max(24, (width - content) / 2);
            int gap = width >= 1200 ? 56 : 24;
            int welcomeWidth = Math.max(0, content - formWidth - gap);
            int formHeight = Math.min(form.getPreferredSize().height, Math.max(0, height - 24));
            welcome.setBounds(left, 32, welcomeWidth, Math.max(0, height - 64));
            form.setCardPadding(height < 580 ? 12 : 28, height < 580 ? 22 : 30,
                    height < 580 ? 12 : 25, height < 580 ? 22 : 30);
            ((BorderLayout) form.getLayout()).setVgap(height < 580 ? 6 : 20);
            form.setBounds(left + welcomeWidth + gap, (height - formHeight) / 2, formWidth, formHeight);
            resizeHeadline(welcome, width >= 1400 ? 46 : width < 840 ? 28 : 36);
        }
        private void resizeHeadline(Container root, int size) {
            for (Component child : root.getComponents()) {
                if (child instanceof JLabel label && (label.getText().equals("Đấu trí hôm nay.")
                        || label.getText().equals("Tỏa sáng mỗi ngày."))) {
                    label.setFont(ArenaPalette.font(size, Font.BOLD));
                    label.setMaximumSize(new Dimension(Integer.MAX_VALUE, label.getPreferredSize().height));
                }
                if (child instanceof Container nested) resizeHeadline(nested, size);
            }
            root.invalidate();
        }
    }

    private JPanel createWelcomePanel() {
        JPanel welcome = new JPanel(new BorderLayout());
        welcome.setOpaque(false);

        JPanel copy = new JPanel();
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        copy.setOpaque(false);

        JLabel brand = welcomeLabel("QUIZ ARENA", 17, Color.WHITE);
        brand.setIcon(new ArenaIcon(ArenaIcon.Type.BRAND, 32, new Color(125, 224, 255)));
        brand.setIconTextGap(12);
        copy.add(brand);
        copy.add(Box.createVerticalStrut(22));
        copy.add(welcomeLabel("Đấu trí hôm nay.", 36, Color.WHITE));
        copy.add(Box.createVerticalStrut(1));
        copy.add(welcomeLabel("Tỏa sáng mỗi ngày.", 36, new Color(139, 224, 255)));
        copy.add(Box.createVerticalStrut(12));

        JLabel description = new JLabel(
                "<html>Thách đấu bạn bè. Chinh phục kiến thức.<br>Ghi tên mình trên bảng xếp hạng.</html>") {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D shade = (Graphics2D) graphics.create();
                Theme.setupAntiAliasing(shade);
                shade.setColor(new Color(6, 14, 33, 195));
                shade.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                shade.dispose();
                super.paintComponent(graphics);
            }
        };
        description.setBorder(new EmptyBorder(8, 10, 8, 10));
        description.setFont(new Font(Theme.BODY.getFamily(), Font.PLAIN, 13));
        description.setForeground(new Color(198, 215, 240));
        description.setAlignmentX(Component.LEFT_ALIGNMENT);
        copy.add(description);
        welcome.add(copy, BorderLayout.NORTH);

        JPanel footer = new JPanel();
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));
        footer.setOpaque(false);
        JPanel features = new JPanel(new GridLayout(1, 2, 18, 0));
        features.setOpaque(false);
        features.setAlignmentX(Component.LEFT_ALIGNMENT);
        features.setMaximumSize(new Dimension(335, 50));
        features.add(featureLabel("1v1", "ĐỐI KHÁNG TRỰC TUYẾN"));
        features.add(featureLabel("10", "CÂU HỎI MỖI TRẬN"));
        footer.add(features);
        footer.add(Box.createVerticalStrut(18));

        JToggleButton motion = new JToggleButton() {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D background = (Graphics2D) graphics.create();
                Theme.setupAntiAliasing(background);
                background.setColor(new Color(7, 16, 39, 190));
                background.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                background.dispose();
                super.paintComponent(graphics);
            }
        };
        motion.setSelected(isMotionEnabled());
        motion.setText(isMotionEnabled() ? "Hiệu ứng: Bật" : "Hiệu ứng: Tắt");
        motion.setFont(Theme.SMALL_BOLD);
        motion.setForeground(new Color(224, 238, 255));
        motion.setOpaque(false);
        motion.setContentAreaFilled(false);
        motion.setFocusPainted(true);
        motion.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(120, 165, 220, 100), 1, true),
                new EmptyBorder(7, 12, 7, 12)));
        motion.setIcon(new ArenaIcon(ArenaIcon.Type.SETTINGS, 13, new Color(171, 215, 250)));
        motion.setIconTextGap(7);
        motion.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        motion.setToolTipText("Bật hoặc tắt chuyển động nền");
        motion.setAlignmentX(Component.LEFT_ALIGNMENT);
        motion.addActionListener(e -> {
            setMotionEnabled(motion.isSelected());
            motion.setText(isMotionEnabled() ? "Hiệu ứng: Bật" : "Hiệu ứng: Tắt");
        });
        footer.add(motion);
        welcome.add(footer, BorderLayout.SOUTH);
        return welcome;
    }

    private static JPanel featureLabel(String value, String caption) {
        JPanel feature = new JPanel();
        feature.setLayout(new BoxLayout(feature, BoxLayout.Y_AXIS));
        feature.setOpaque(false);
        feature.setBorder(new EmptyBorder(0, 0, 0, 0));
        feature.add(welcomeLabel(value, 24, Color.WHITE));
        feature.add(Box.createVerticalStrut(3));
        feature.add(welcomeLabel(caption, 9, new Color(180, 202, 233)));
        return feature;
    }

    private static JLabel welcomeLabel(String text, int size, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(new Font(Theme.BODY.getFamily(), Font.BOLD, size));
        label.setForeground(color);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private class TabButton extends JButton {
        private boolean active;

        public TabButton(String text, boolean active) {
            super(text);
            this.active = active;
            setFont(new Font(Theme.BODY_BOLD.getFamily(), Font.BOLD, 13));
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            updateStyle();
        }

        public void setActive(boolean active) {
            this.active = active;
            updateStyle();
            repaint();
        }

        public boolean isActive() {
            return active;
        }

        private void updateStyle() {
            if (active) {
                setForeground(ArenaPalette.ACCENT);
            } else {
                setForeground(ArenaPalette.MUTED);
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.setupAntiAliasing(g2);
            int w = getWidth();
            int h = getHeight();
            if (active) {
                g2.setColor(new Color(38, 57, 51));
                g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 14, 14));
            } else if (getModel().isRollover()) {
                g2.setColor(ArenaPalette.SURFACE_RAISED);
                g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 14, 14));
            }
            if (hasFocus()) {
                g2.setColor(active ? new Color(190, 220, 255) : Theme.INDIGO);
                g2.drawRoundRect(2, 2, w - 5, h - 5, 12, 12);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    public void setMode(Mode mode) {
        this.currentMode = mode;
        clearErrors();

        boolean isLogin = (mode == Mode.LOGIN);
        tabLogin.setActive(isLogin);
        tabRegister.setActive(!isLogin);

        // Bật / tắt ô Nhập lại mật khẩu để người dùng thấy rõ sự khác biệt giữa 2 chế độ
        lblConfirmHeader.setVisible(!isLogin);
        txtConfirmPassword.setVisible(!isLogin);
        lblConfirmError.setVisible(!isLogin);

        if (isLogin) {
            lblTitle.setText("Chào mừng trở lại");
            lblSubtitle.setText("Đăng nhập để bắt đầu trận đấu của bạn");
            btnSubmit.setText("VÀO ĐẤU TRƯỜNG");
            btnSubmit.setStyle(ModernButton.Style.PRIMARY);
            card.setPreferredSize(new Dimension(424, 500));
        } else {
            lblTitle.setText("Gia nhập đấu trường");
            lblSubtitle.setText("Tạo tài khoản và khám phá thử thách mới");
            btnSubmit.setText("TẠO TÀI KHOẢN MỚI");
            btnSubmit.setStyle(ModernButton.Style.PRIMARY);
            card.setPreferredSize(new Dimension(424, 574));
        }
        card.revalidate();
        card.repaint();
        revalidate();
        repaint();
    }

    public void setOnLogin(BiConsumer<String, String> handler) {
        this.onLogin = handler;
    }

    public void setOnRegister(BiConsumer<String, String> handler) {
        this.onRegister = handler;
    }

    public void setLoading(boolean loading) {
        txtUsername.setEnabled(!loading);
        txtPassword.setEnabled(!loading);
        txtConfirmPassword.setEnabled(!loading);
        btnSubmit.setEnabled(!loading);
        tabLogin.setEnabled(!loading);
        tabRegister.setEnabled(!loading);

        if (loading) {
            btnSubmit.setText("Đang kết nối máy chủ...");
        } else {
            btnSubmit.setText(currentMode == Mode.LOGIN ? "VÀO ĐẤU TRƯỜNG" : "TẠO TÀI KHOẢN MỚI");
        }
    }

    public void showError(String message) {
        setLoading(false);
        lblGlobalMessage.setForeground(new Color(255, 142, 164));
        lblGlobalMessage.setText(message != null ? message : "Đã xảy ra lỗi");
    }

    public void showSuccess(String message) {
        setLoading(false);
        lblGlobalMessage.setForeground(new Color(105, 226, 178));
        lblGlobalMessage.setText(message != null ? message : "Thành công!");
    }

    public void clearErrors() {
        txtUsername.setError(false);
        txtPassword.setError(false);
        txtConfirmPassword.setError(false);
        lblUserError.setText(" ");
        lblPassError.setText(" ");
        lblConfirmError.setText(" ");
        lblGlobalMessage.setText(" ");
    }

    private void handleSubmit() {
        SoundManager.playClick();
        clearErrors();

        String user = txtUsername.getText().trim();
        String pass = new String(txtPassword.getPassword());
        String confirmPass = new String(txtConfirmPassword.getPassword());

        boolean hasError = false;

        // 1. Validate Username
        if (user.isEmpty()) {
            txtUsername.setError(true);
            lblUserError.setText("Vui lòng nhập tên người dùng");
            hasError = true;
        } else if (user.length() < 3 || user.length() > 24) {
            txtUsername.setError(true);
            lblUserError.setText("Tên phải từ 3 đến 24 ký tự");
            hasError = true;
        } else if (!user.matches("[A-Za-z0-9_]+")) {
            txtUsername.setError(true);
            lblUserError.setText("Chỉ cho phép chữ cái, chữ số và dấu _");
            hasError = true;
        }

        // 2. Validate Password
        if (pass.isEmpty()) {
            txtPassword.setError(true);
            lblPassError.setText("Vui lòng nhập mật khẩu");
            hasError = true;
        } else if (pass.length() < 4) {
            txtPassword.setError(true);
            lblPassError.setText("Mật khẩu phải từ 4 ký tự trở lên");
            hasError = true;
        }

        // 3. Validate Confirm Password (chỉ khi Đăng Ký)
        if (currentMode == Mode.REGISTER) {
            if (confirmPass.isEmpty()) {
                txtConfirmPassword.setError(true);
                lblConfirmError.setText("Vui lòng xác nhận lại mật khẩu");
                hasError = true;
            } else if (!pass.equals(confirmPass)) {
                txtConfirmPassword.setError(true);
                lblConfirmError.setText("Mật khẩu xác nhận không khớp!");
                hasError = true;
            }
        }

        if (hasError) {
            return;
        }

        // Bật loading state
        setLoading(true);

        if (currentMode == Mode.LOGIN) {
            if (onLogin != null) {
                onLogin.accept(user, pass);
            }
        } else {
            if (onRegister != null) {
                onRegister.accept(user, pass);
            }
        }
    }

    public String getUsername() {
        return txtUsername.getText().trim();
    }

    public void setUsername(String username) {
        txtUsername.setText(username);
    }

    public void clearPassword() {
        txtPassword.setText("");
        txtConfirmPassword.setText("");
    }

    /**
     * Reset toàn bộ trạng thái AuthScreen để dùng lại sau logout hoặc disconnect.
     * Xóa sạch input fields, thông báo lỗi, loading state và đưa layout về trạng thái chuẩn xác.
     */
    public void resetForReuse() {
        txtUsername.setText("");
        txtPassword.setText("");
        txtConfirmPassword.setText("");
        lblUserError.setText(" ");
        lblPassError.setText(" ");
        lblConfirmError.setText(" ");
        lblGlobalMessage.setText(" ");
        txtUsername.setError(false);
        txtPassword.setError(false);
        txtConfirmPassword.setError(false);
        setLoading(false);
        setMode(Mode.LOGIN);
        card.revalidate();
        card.repaint();
        revalidate();
        repaint();
    }
}

