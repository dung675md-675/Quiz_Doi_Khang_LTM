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
public class AuthScreen extends KahootBackgroundPanel {

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
        super(new GridBagLayout());

        // Card chính với kích thước chuẩn, viền bo góc mượt mà và đổ bóng nhẹ
        card = new ModernCard(new BorderLayout(0, 14));
        card.setPreferredSize(new Dimension(460, 560));
        card.setMinimumSize(new Dimension(420, 520));
        card.setMaximumSize(new Dimension(480, 580));
        card.setCardPadding(28, 36, 26, 36);
        card.setCornerRadius(20);
        card.setBorderColor(new Color(225, 232, 245));

        // ==========================================
        // 1. HEADER (Logo Badge, Title & Mode Tabs)
        // ==========================================
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setOpaque(false);

        // Logo icon / badge
        lblLogoBadge = new JLabel("", SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setupAntiAliasing(g2);
                int size = Math.min(getWidth(), getHeight()) - 4;
                int x = (getWidth() - size) / 2;
                int y = (getHeight() - size) / 2;
                // Vòng gradient tròn
                GradientPaint gp = new GradientPaint(x, y, new Color(0x3B, 0x82, 0xF6), x + size, y + size, new Color(0x8B, 0x5C, 0xF6));
                g2.setPaint(gp);
                g2.fillOval(x, y, size, size);

                // Vẽ tia sét vector màu trắng sắc nét (không dùng emoji tránh lỗi tofu font)
                java.awt.geom.Path2D bolt = new java.awt.geom.Path2D.Float();
                bolt.moveTo(x + size * 0.54f, y + size * 0.20f);
                bolt.lineTo(x + size * 0.32f, y + size * 0.54f);
                bolt.lineTo(x + size * 0.49f, y + size * 0.54f);
                bolt.lineTo(x + size * 0.44f, y + size * 0.82f);
                bolt.lineTo(x + size * 0.68f, y + size * 0.46f);
                bolt.lineTo(x + size * 0.52f, y + size * 0.46f);
                bolt.closePath();
                g2.setColor(Color.WHITE);
                g2.fill(bolt);

                g2.dispose();
            }
        };
        lblLogoBadge.setPreferredSize(new Dimension(56, 56));
        lblLogoBadge.setMaximumSize(new Dimension(56, 56));
        lblLogoBadge.setAlignmentX(Component.CENTER_ALIGNMENT);

        lblTitle = new JLabel("QUIZ ARENA");
        lblTitle.setFont(new Font(Theme.TITLE_XL.getFamily(), Font.BOLD, 26));
        lblTitle.setForeground(new Color(0x1E, 0x29, 0x3B));
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        lblSubtitle = new JLabel("Đấu Trường Trí Tuệ Đối Kháng 1v1");
        lblSubtitle.setFont(Theme.BODY);
        lblSubtitle.setForeground(Theme.MUTED_GRAY);
        lblSubtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        headerPanel.add(lblLogoBadge);
        headerPanel.add(Box.createVerticalStrut(8));
        headerPanel.add(lblTitle);
        headerPanel.add(Box.createVerticalStrut(3));
        headerPanel.add(lblSubtitle);
        headerPanel.add(Box.createVerticalStrut(16));

        // TAB SWITCHER: Thiết kế dạng Pill Capsule bo tròn hiện đại
        JPanel tabWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        tabWrapper.setOpaque(false);

        JPanel tabPill = new JPanel(new GridLayout(1, 2, 4, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setupAntiAliasing(g2);
                g2.setColor(new Color(0xEE, 0xF2, 0xF6));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 16, 16));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        tabPill.setOpaque(false);
        tabPill.setPreferredSize(new Dimension(360, 42));
        tabPill.setBorder(new EmptyBorder(3, 4, 3, 4));

        tabLogin = new TabButton("ĐĂNG NHẬP", true);
        tabRegister = new TabButton("ĐĂNG KÝ", false);

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
        formPanel.setBorder(new EmptyBorder(4, 4, 4, 4));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;
        gbc.insets = new Insets(0, 0, 0, 0);

        // Header Username
        lblUserHeader = new JLabel("Tên đăng nhập");
        lblUserHeader.setFont(Theme.BODY_BOLD);
        lblUserHeader.setForeground(new Color(0x33, 0x41, 0x55));
        gbc.gridy = 0;
        gbc.insets = new Insets(2, 0, 3, 0);
        formPanel.add(lblUserHeader, gbc);

        // Field Username
        txtUsername = new ModernTextField("Nhập tên từ 3-24 ký tự...", 20);
        txtUsername.setPreferredSize(new Dimension(360, 40));
        txtUsername.setFont(new Font(Theme.BODY.getFamily(), Font.PLAIN, 14));
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 2, 0);
        formPanel.add(txtUsername, gbc);

        // Error Username
        lblUserError = new JLabel(" ");
        lblUserError.setFont(Theme.SMALL_BOLD);
        lblUserError.setForeground(Theme.RED);
        gbc.gridy = 2;
        gbc.insets = new Insets(0, 2, 6, 0);
        formPanel.add(lblUserError, gbc);

        // Header Password
        lblPassHeader = new JLabel("Mật khẩu");
        lblPassHeader.setFont(Theme.BODY_BOLD);
        lblPassHeader.setForeground(new Color(0x33, 0x41, 0x55));
        gbc.gridy = 3;
        gbc.insets = new Insets(0, 0, 3, 0);
        formPanel.add(lblPassHeader, gbc);

        // Field Password
        txtPassword = new ModernPasswordField("Nhập mật khẩu từ 4 ký tự...", 20);
        txtPassword.setPreferredSize(new Dimension(360, 40));
        txtPassword.setFont(new Font(Theme.BODY.getFamily(), Font.PLAIN, 14));
        gbc.gridy = 4;
        gbc.insets = new Insets(0, 0, 2, 0);
        formPanel.add(txtPassword, gbc);

        // Error Password
        lblPassError = new JLabel(" ");
        lblPassError.setFont(Theme.SMALL_BOLD);
        lblPassError.setForeground(Theme.RED);
        gbc.gridy = 5;
        gbc.insets = new Insets(0, 2, 6, 0);
        formPanel.add(lblPassError, gbc);

        // Header Confirm Password (cho chế độ Đăng Ký)
        lblConfirmHeader = new JLabel("Nhập lại mật khẩu");
        lblConfirmHeader.setFont(Theme.BODY_BOLD);
        lblConfirmHeader.setForeground(new Color(0x33, 0x41, 0x55));
        gbc.gridy = 6;
        gbc.insets = new Insets(0, 0, 3, 0);
        formPanel.add(lblConfirmHeader, gbc);

        // Field Confirm Password
        txtConfirmPassword = new ModernPasswordField("Xác nhận lại mật khẩu...", 20);
        txtConfirmPassword.setPreferredSize(new Dimension(360, 40));
        txtConfirmPassword.setFont(new Font(Theme.BODY.getFamily(), Font.PLAIN, 14));
        gbc.gridy = 7;
        gbc.insets = new Insets(0, 0, 2, 0);
        formPanel.add(txtConfirmPassword, gbc);

        // Error Confirm Password
        lblConfirmError = new JLabel(" ");
        lblConfirmError.setFont(Theme.SMALL_BOLD);
        lblConfirmError.setForeground(Theme.RED);
        gbc.gridy = 8;
        gbc.insets = new Insets(0, 2, 4, 0);
        formPanel.add(lblConfirmError, gbc);

        // Thông báo toàn cục
        lblGlobalMessage = new JLabel(" ", SwingConstants.CENTER);
        lblGlobalMessage.setFont(Theme.BODY_BOLD);
        lblGlobalMessage.setForeground(Theme.RED);
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
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        footerPanel.setOpaque(false);
        footerPanel.setBorder(new EmptyBorder(4, 0, 4, 0));

        btnSubmit = new ModernButton("VÀO ĐẤU TRƯỜNG", ModernButton.Style.PRIMARY);
        btnSubmit.setPreferredSize(new Dimension(368, 46));
        btnSubmit.setFont(new Font(Theme.SUBTITLE.getFamily(), Font.BOLD, 15));
        btnSubmit.setCornerRadius(12);
        btnSubmit.addActionListener(e -> handleSubmit());

        footerPanel.add(btnSubmit);
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

        // Đặt Card vào trung tâm màn hình
        GridBagConstraints centerGbc = new GridBagConstraints();
        centerGbc.gridx = 0;
        centerGbc.gridy = 0;
        centerGbc.anchor = GridBagConstraints.CENTER;
        add(card, centerGbc);
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
                setForeground(currentMode == Mode.LOGIN ? Theme.BLUE : Theme.PURPLE);
            } else {
                setForeground(new Color(0x64, 0x74, 0x8B));
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.setupAntiAliasing(g2);
            int w = getWidth();
            int h = getHeight();
            if (active) {
                // Nền trắng bo góc nổi lên giống tab capsule
                g2.setColor(Theme.WHITE);
                g2.fill(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, 14, 14));
                g2.setColor(new Color(0, 0, 0, 20));
                g2.draw(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, 14, 14));
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
            btnSubmit.setText("VÀO ĐẤU TRƯỜNG");
            btnSubmit.setStyle(ModernButton.Style.PRIMARY);
            card.setPreferredSize(new Dimension(460, 540));
        } else {
            btnSubmit.setText("TẠO TÀI KHOẢN MỚI");
            btnSubmit.setStyle(ModernButton.Style.ACCENT_PURPLE);
            card.setPreferredSize(new Dimension(460, 610));
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
        lblGlobalMessage.setForeground(Theme.RED);
        lblGlobalMessage.setText(message != null ? message : "Đã xảy ra lỗi");
    }

    public void showSuccess(String message) {
        setLoading(false);
        lblGlobalMessage.setForeground(Theme.GREEN);
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

