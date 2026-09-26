package quiz.ui;

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.function.BiConsumer;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * AuthScreen - Màn hình Đăng nhập / Đăng ký hiện đại.
 * Hỗ trợ chuyển tab mượt mà, validation ngay tại field, hiển thị lỗi rõ ràng
 * và trạng thái loading khi gửi yêu cầu lên Server.
 */
public class AuthScreen extends JPanel {

    public enum Mode {
        LOGIN, REGISTER
    }

    private Mode currentMode = Mode.LOGIN;

    // Components
    private final ModernCard card;
    private final JLabel lblTitle;
    private final JLabel lblSubtitle;
    private final JButton tabLogin;
    private final JButton tabRegister;
    private final ModernTextField txtUsername;
    private final ModernPasswordField txtPassword;
    private final JLabel lblUserError;
    private final JLabel lblPassError;
    private final JLabel lblGlobalMessage;
    private final ModernButton btnSubmit;

    // Callbacks
    private BiConsumer<String, String> onLogin;
    private BiConsumer<String, String> onRegister;

    public AuthScreen() {
        setLayout(new GridBagLayout());
        setBackground(Theme.OFF_WHITE);

        // Khởi tạo Card chính
        card = new ModernCard(new BorderLayout(0, 16));
        card.setPreferredSize(new Dimension(420, 520));
        card.setCardPadding(28, 32, 28, 32);

        // 1. HEADER (Title & Subtitle)
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setOpaque(false);

        lblTitle = new JLabel("QUIZ ARENA");
        lblTitle.setFont(Theme.TITLE_XL);
        lblTitle.setForeground(Theme.BLUE);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        lblSubtitle = new JLabel("Game Đối Kháng 1v1 — Tri Thức & Tốc Độ");
        lblSubtitle.setFont(Theme.BODY);
        lblSubtitle.setForeground(Theme.MUTED_GRAY);
        lblSubtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        headerPanel.add(lblTitle);
        headerPanel.add(Box.createVerticalStrut(4));
        headerPanel.add(lblSubtitle);
        headerPanel.add(Box.createVerticalStrut(18));

        // 2. TAB SWITCHER (Đăng nhập / Đăng ký)
        JPanel tabContainer = new JPanel(new GridLayout(1, 2, 8, 0));
        tabContainer.setOpaque(false);
        tabContainer.setMaximumSize(new Dimension(360, 38));

        tabLogin = createTabButton("Đăng nhập", true);
        tabRegister = createTabButton("Đăng ký", false);

        tabLogin.addActionListener(e -> setMode(Mode.LOGIN));
        tabRegister.addActionListener(e -> setMode(Mode.REGISTER));

        tabContainer.add(tabLogin);
        tabContainer.add(tabRegister);
        headerPanel.add(tabContainer);

        card.add(headerPanel, BorderLayout.NORTH);

        // 3. FORM BODY (Inputs & Validation messages)
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setOpaque(false);

        // Field Username
        JLabel lblUserHeader = new JLabel("Tên người dùng");
        lblUserHeader.setFont(Theme.BODY_BOLD);
        lblUserHeader.setForeground(Theme.DARK_TEXT);
        lblUserHeader.setAlignmentX(Component.LEFT_ALIGNMENT);

        txtUsername = new ModernTextField("Ví dụ: player_one", 15);
        txtUsername.setMaximumSize(new Dimension(360, 42));
        txtUsername.setAlignmentX(Component.LEFT_ALIGNMENT);

        lblUserError = new JLabel(" ");
        lblUserError.setFont(Theme.SMALL_BOLD);
        lblUserError.setForeground(Theme.RED);
        lblUserError.setAlignmentX(Component.LEFT_ALIGNMENT);

        formPanel.add(lblUserHeader);
        formPanel.add(Box.createVerticalStrut(4));
        formPanel.add(txtUsername);
        formPanel.add(Box.createVerticalStrut(2));
        formPanel.add(lblUserError);
        formPanel.add(Box.createVerticalStrut(8));

        // Field Password
        JLabel lblPassHeader = new JLabel("Mật khẩu");
        lblPassHeader.setFont(Theme.BODY_BOLD);
        lblPassHeader.setForeground(Theme.DARK_TEXT);
        lblPassHeader.setAlignmentX(Component.LEFT_ALIGNMENT);

        txtPassword = new ModernPasswordField("Nhập mật khẩu...", 15);
        txtPassword.setMaximumSize(new Dimension(360, 42));
        txtPassword.setAlignmentX(Component.LEFT_ALIGNMENT);

        lblPassError = new JLabel(" ");
        lblPassError.setFont(Theme.SMALL_BOLD);
        lblPassError.setForeground(Theme.RED);
        lblPassError.setAlignmentX(Component.LEFT_ALIGNMENT);

        formPanel.add(lblPassHeader);
        formPanel.add(Box.createVerticalStrut(4));
        formPanel.add(txtPassword);
        formPanel.add(Box.createVerticalStrut(2));
        formPanel.add(lblPassError);
        formPanel.add(Box.createVerticalStrut(8));

        // Thông báo lỗi chung từ Server
        lblGlobalMessage = new JLabel(" ", SwingConstants.CENTER);
        lblGlobalMessage.setFont(Theme.BODY_BOLD);
        lblGlobalMessage.setForeground(Theme.RED);
        lblGlobalMessage.setAlignmentX(Component.CENTER_ALIGNMENT);
        formPanel.add(lblGlobalMessage);

        card.add(formPanel, BorderLayout.CENTER);

        // 4. FOOTER (Submit Button)
        JPanel footerPanel = new JPanel(new BorderLayout());
        footerPanel.setOpaque(false);
        footerPanel.setBorder(new EmptyBorder(8, 0, 0, 0));

        btnSubmit = new ModernButton("ĐĂNG NHẬP", ModernButton.Style.PRIMARY);
        btnSubmit.setPreferredSize(new Dimension(360, 44));
        btnSubmit.setFont(Theme.SUBTITLE);
        btnSubmit.addActionListener(e -> handleSubmit());

        footerPanel.add(btnSubmit, BorderLayout.CENTER);
        card.add(footerPanel, BorderLayout.SOUTH);

        // Bắt sự kiện phím Enter
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

        // Đặt Card vào trung tâm màn hình
        add(card);
    }

    private JButton createTabButton(String text, boolean active) {
        JButton btn = new JButton(text);
        btn.setFont(Theme.BODY_BOLD);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        applyTabStyle(btn, active);
        return btn;
    }

    private void applyTabStyle(JButton btn, boolean active) {
        if (active) {
            btn.setBackground(Theme.BLUE);
            btn.setForeground(Theme.WHITE);
            btn.setOpaque(true);
        } else {
            btn.setBackground(Theme.OFF_WHITE);
            btn.setForeground(Theme.MUTED_GRAY);
            btn.setOpaque(true);
        }
    }

    public void setMode(Mode mode) {
        this.currentMode = mode;
        clearErrors();

        if (mode == Mode.LOGIN) {
            applyTabStyle(tabLogin, true);
            applyTabStyle(tabRegister, false);
            btnSubmit.setText("ĐĂNG NHẬP");
            btnSubmit.setStyle(ModernButton.Style.PRIMARY);
        } else {
            applyTabStyle(tabLogin, false);
            applyTabStyle(tabRegister, true);
            btnSubmit.setText("TẠO TÀI KHOẢN");
            btnSubmit.setStyle(ModernButton.Style.ACCENT_PURPLE);
        }
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
        btnSubmit.setEnabled(!loading);
        tabLogin.setEnabled(!loading);
        tabRegister.setEnabled(!loading);

        if (loading) {
            btnSubmit.setText("Đang xử lý...");
        } else {
            btnSubmit.setText(currentMode == Mode.LOGIN ? "ĐĂNG NHẬP" : "TẠO TÀI KHOẢN");
        }
    }

    public void showError(String message) {
        setLoading(false);
        lblGlobalMessage.setForeground(Theme.RED);
        lblGlobalMessage.setText(message);
    }

    public void showSuccess(String message) {
        setLoading(false);
        lblGlobalMessage.setForeground(Theme.GREEN);
        lblGlobalMessage.setText(message);
    }

    public void clearErrors() {
        txtUsername.setError(false);
        txtPassword.setError(false);
        lblUserError.setText(" ");
        lblPassError.setText(" ");
        lblGlobalMessage.setText(" ");
    }

    private void handleSubmit() {
        SoundManager.playClick();
        clearErrors();

        String user = txtUsername.getText().trim();
        String pass = new String(txtPassword.getPassword());

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
            lblUserError.setText("Chỉ cho phép chữ, số và ký tự _");
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
    }
}
