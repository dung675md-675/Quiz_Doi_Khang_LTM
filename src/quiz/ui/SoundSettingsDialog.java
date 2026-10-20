package quiz.ui;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.RoundRectangle2D;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * SoundSettingsDialog — Hộp thoại cài đặt âm thanh toàn diện cho Quiz Arena.
 *
 * Đã tối ưu hoàn toàn:
 * - Không sử dụng emoji / ký tự đặc biệt gây lỗi ô vuông (tofu glyphs) trên Windows Swing.
 * - Sửa triệt để lỗi đè chữ ở hàng nút phát thử hiệu ứng (chuyển dòng trạng thái lên tiêu đề thẻ).
 * - Đổi tên nút nhạc theo yêu cầu: "Nhạc nhẹ" và "Nhạc sôi động".
 * - Bỏ nút dừng nhạc.
 * - Kích thước rộng rãi (530x600), bố cục phẳng mượt mà.
 */
public class SoundSettingsDialog extends JDialog {

    private final SwitchButton btnMasterSwitch;
    private final JLabel lblMasterStatus;

    private final SwitchButton btnBgmSwitch;
    private final JSlider sliderMusic;
    private final JLabel lblMusicVol;
    private final JLabel lblBgmSub;

    private final SwitchButton btnSfxSwitch;
    private final JSlider sliderSfx;
    private final JLabel lblSfxVol;
    private final JLabel lblSfxSub;

    public SoundSettingsDialog(Window owner) {
        super(owner, "Cài Đặt Âm Thanh", ModalityType.APPLICATION_MODAL);

        setSize(530, 600);
        setLocationRelativeTo(owner);
        setResizable(false);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Theme.WHITE);

        // Khởi tạo các trường điều khiển
        boolean isAllMuted = SoundManager.isMuted() && MusicManager.isMusicMuted();
        btnMasterSwitch = new SwitchButton(!isAllMuted, Theme.BLUE);
        lblMasterStatus = new JLabel(isAllMuted ? "Đã tắt toàn bộ âm thanh" : "Đang phát âm thanh bình thường");

        int initMusicVol = Math.round(MusicManager.getMusicVolume() * 100);
        sliderMusic = new JSlider(0, 100, initMusicVol);
        sliderMusic.setOpaque(false);
        sliderMusic.setEnabled(!MusicManager.isMusicMuted());
        sliderMusic.setUI(new ModernSliderUI(sliderMusic, Theme.BLUE));
        lblMusicVol = createPillLabel(initMusicVol + "%", Theme.BLUE);
        btnBgmSwitch = new SwitchButton(!MusicManager.isMusicMuted(), Theme.BLUE);
        lblBgmSub = new JLabel("Chọn giai điệu nhạc nhẹ hoặc nhạc sôi động khi thi đấu");

        int initSfxVol = Math.round(SoundManager.getSfxVolume() * 100);
        sliderSfx = new JSlider(0, 100, initSfxVol);
        sliderSfx.setOpaque(false);
        sliderSfx.setEnabled(!SoundManager.isMuted());
        sliderSfx.setUI(new ModernSliderUI(sliderSfx, Theme.PURPLE));
        lblSfxVol = createPillLabel(initSfxVol + "%", Theme.PURPLE);
        btnSfxSwitch = new SwitchButton(!SoundManager.isMuted(), Theme.PURPLE);
        lblSfxSub = new JLabel("Tiếng chuông, đếm ngược, trả lời đúng/sai");

        // ==========================================
        // 1. HEADER PANEL
        // ==========================================
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(Theme.WHITE);
        header.setBorder(new EmptyBorder(16, 24, 10, 24));

        JLabel title = new JLabel("CÀI ĐẶT ÂM THANH", SwingConstants.CENTER);
        title.setFont(Theme.TITLE);
        title.setForeground(Theme.BLUE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Tùy chỉnh nhạc nền, hiệu ứng âm thanh và âm lượng thi đấu", SwingConstants.CENTER);
        subtitle.setFont(Theme.SMALL);
        subtitle.setForeground(Theme.MUTED_GRAY);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        header.add(title);
        header.add(Box.createVerticalStrut(4));
        header.add(subtitle);
        add(header, BorderLayout.NORTH);

        // ==========================================
        // 2. BODY CONTENT (Master + BGM + SFX)
        // ==========================================
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(4, 20, 8, 20));

        // ------------------------------------------
        // A. MASTER SWITCH CARD
        // ------------------------------------------
        ModernCard masterCard = new ModernCard(new BorderLayout(12, 0));
        masterCard.setCardBackground(new Color(0xF8, 0xFA, 0xFC));
        masterCard.setBorderColor(new Color(0xCB, 0xD5, 0xE1));
        masterCard.setCardPadding(12, 16, 12, 16);
        masterCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));

        JPanel masterInfo = new JPanel();
        masterInfo.setLayout(new BoxLayout(masterInfo, BoxLayout.Y_AXIS));
        masterInfo.setOpaque(false);

        JLabel lblMasterTitle = new JLabel("Toàn Bộ Âm Thanh (Master)");
        lblMasterTitle.setFont(Theme.BODY_BOLD);
        lblMasterTitle.setForeground(Theme.DARK_TEXT);

        lblMasterStatus.setFont(Theme.SMALL);
        lblMasterStatus.setForeground(Theme.MUTED_GRAY);

        masterInfo.add(lblMasterTitle);
        masterInfo.add(Box.createVerticalStrut(2));
        masterInfo.add(lblMasterStatus);

        btnMasterSwitch.addActionListener(e -> {
            boolean turnOn = !btnMasterSwitch.isActive();
            btnMasterSwitch.setActive(turnOn);
            applyMasterMute(!turnOn);
        });

        masterCard.add(masterInfo, BorderLayout.CENTER);
        masterCard.add(btnMasterSwitch, BorderLayout.EAST);
        body.add(masterCard);

        body.add(Box.createVerticalStrut(10));

        // ------------------------------------------
        // B. BGM CARD (Nhạc nền)
        // ------------------------------------------
        ModernCard bgmCard = new ModernCard(new BorderLayout(0, 8));
        bgmCard.setCardBackground(Theme.WHITE);
        bgmCard.setBorderColor(new Color(0xE2, 0xE8, 0xF0));
        bgmCard.setCardPadding(12, 16, 12, 16);
        bgmCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));

        // Top: Title + Switch
        JPanel bgmTop = new JPanel(new BorderLayout());
        bgmTop.setOpaque(false);

        JPanel bgmTitleCol = new JPanel();
        bgmTitleCol.setLayout(new BoxLayout(bgmTitleCol, BoxLayout.Y_AXIS));
        bgmTitleCol.setOpaque(false);

        JLabel lblBgmHeader = new JLabel("Nhạc Nền (BGM)");
        lblBgmHeader.setFont(Theme.BODY_BOLD);
        lblBgmHeader.setForeground(Theme.DARK_TEXT);

        lblBgmSub.setFont(Theme.SMALL);
        lblBgmSub.setForeground(Theme.MUTED_GRAY);

        bgmTitleCol.add(lblBgmHeader);
        bgmTitleCol.add(Box.createVerticalStrut(2));
        bgmTitleCol.add(lblBgmSub);

        btnBgmSwitch.addActionListener(e -> {
            boolean active = !btnBgmSwitch.isActive();
            btnBgmSwitch.setActive(active);
            MusicManager.setMusicMuted(!active);
            sliderMusic.setEnabled(active);
            if (!active) {
                lblBgmSub.setText("Nhạc nền: Đã tắt tiếng");
                lblBgmSub.setForeground(Theme.MUTED_GRAY);
            } else {
                lblBgmSub.setText("Nhạc nền: Đang bật");
                lblBgmSub.setForeground(Theme.GREEN);
            }
            updateMasterStateFromChildren();
        });

        bgmTop.add(bgmTitleCol, BorderLayout.CENTER);
        bgmTop.add(btnBgmSwitch, BorderLayout.EAST);

        // Center: Slider Row
        JPanel bgmSliderRow = new JPanel(new BorderLayout(10, 0));
        bgmSliderRow.setOpaque(false);
        JLabel lblBgmVolIcon = new JLabel("Âm lượng:");
        lblBgmVolIcon.setFont(Theme.SMALL_BOLD);
        lblBgmVolIcon.setForeground(Theme.DARK_TEXT);

        bgmSliderRow.add(lblBgmVolIcon, BorderLayout.WEST);
        bgmSliderRow.add(sliderMusic, BorderLayout.CENTER);
        bgmSliderRow.add(lblMusicVol, BorderLayout.EAST);

        sliderMusic.addChangeListener(e -> {
            int val = sliderMusic.getValue();
            lblMusicVol.setText(val + "%");
            MusicManager.setMusicVolume(val / 100f);
        });

        // Bottom: BGM Preview Controls (Nhạc nhẹ & Nhạc sôi động)
        JPanel bgmBottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bgmBottom.setOpaque(false);

        JLabel lblBgmTestLabel = new JLabel("Nghe thử:");
        lblBgmTestLabel.setFont(Theme.SMALL_BOLD);
        lblBgmTestLabel.setForeground(Theme.DARK_TEXT);
        bgmBottom.add(lblBgmTestLabel);

        JButton btnUpbeatMusic = createChipButton("Nhạc sôi động", new Color(0xFF, 0xF1, 0xF2), Theme.RED, () -> {
            if (MusicManager.isMusicMuted()) {
                MusicManager.setMusicMuted(false);
                btnBgmSwitch.setActive(true);
                sliderMusic.setEnabled(true);
                updateMasterStateFromChildren();
            }
            MusicManager.playLobbyMusic(true);
            lblBgmSub.setText("Đang phát thử: Nhạc sôi động");
            lblBgmSub.setForeground(Theme.RED);
        });

        JButton btnSoftMusic = createChipButton("Nhạc nhẹ", new Color(0xEE, 0xF2, 0xFF), Theme.BLUE, () -> {
            if (MusicManager.isMusicMuted()) {
                MusicManager.setMusicMuted(false);
                btnBgmSwitch.setActive(true);
                sliderMusic.setEnabled(true);
                updateMasterStateFromChildren();
            }
            MusicManager.playBattleMusic(true);
            lblBgmSub.setText("Đang phát thử: Nhạc nhẹ");
            lblBgmSub.setForeground(Theme.BLUE);
        });

        bgmBottom.add(btnUpbeatMusic);
        bgmBottom.add(btnSoftMusic);

        bgmCard.add(bgmTop, BorderLayout.NORTH);
        bgmCard.add(bgmSliderRow, BorderLayout.CENTER);
        bgmCard.add(bgmBottom, BorderLayout.SOUTH);
        body.add(bgmCard);

        body.add(Box.createVerticalStrut(10));

        // ------------------------------------------
        // C. SFX CARD (Hiệu ứng âm thanh)
        // ------------------------------------------
        ModernCard sfxCard = new ModernCard(new BorderLayout(0, 8));
        sfxCard.setCardBackground(Theme.WHITE);
        sfxCard.setBorderColor(new Color(0xE2, 0xE8, 0xF0));
        sfxCard.setCardPadding(12, 16, 12, 16);
        sfxCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));

        // Top: Title + Switch
        JPanel sfxTop = new JPanel(new BorderLayout());
        sfxTop.setOpaque(false);

        JPanel sfxTitleCol = new JPanel();
        sfxTitleCol.setLayout(new BoxLayout(sfxTitleCol, BoxLayout.Y_AXIS));
        sfxTitleCol.setOpaque(false);

        JLabel lblSfxHeader = new JLabel("Hiệu Ứng Âm Thanh (SFX)");
        lblSfxHeader.setFont(Theme.BODY_BOLD);
        lblSfxHeader.setForeground(Theme.DARK_TEXT);

        lblSfxSub.setFont(Theme.SMALL);
        lblSfxSub.setForeground(Theme.MUTED_GRAY);

        sfxTitleCol.add(lblSfxHeader);
        sfxTitleCol.add(Box.createVerticalStrut(2));
        sfxTitleCol.add(lblSfxSub);

        btnSfxSwitch.addActionListener(e -> {
            boolean active = !btnSfxSwitch.isActive();
            btnSfxSwitch.setActive(active);
            SoundManager.setMuted(!active);
            sliderSfx.setEnabled(active);
            if (!active) {
                lblSfxSub.setText("Hiệu ứng: Đã tắt tiếng");
                lblSfxSub.setForeground(Theme.MUTED_GRAY);
            } else {
                lblSfxSub.setText("Hiệu ứng: Đang bật");
                lblSfxSub.setForeground(Theme.GREEN);
            }
            updateMasterStateFromChildren();
        });

        sfxTop.add(sfxTitleCol, BorderLayout.CENTER);
        sfxTop.add(btnSfxSwitch, BorderLayout.EAST);

        // Center: Slider Row
        JPanel sfxSliderRow = new JPanel(new BorderLayout(10, 0));
        sfxSliderRow.setOpaque(false);
        JLabel lblSfxVolIcon = new JLabel("Âm lượng:");
        lblSfxVolIcon.setFont(Theme.SMALL_BOLD);
        lblSfxVolIcon.setForeground(Theme.DARK_TEXT);

        sfxSliderRow.add(lblSfxVolIcon, BorderLayout.WEST);
        sfxSliderRow.add(sliderSfx, BorderLayout.CENTER);
        sfxSliderRow.add(lblSfxVol, BorderLayout.EAST);

        sliderSfx.addChangeListener(e -> {
            int val = sliderSfx.getValue();
            lblSfxVol.setText(val + "%");
            SoundManager.setSfxVolume(val / 100f);
        });

        // Bottom: SFX Test Chips (Không dùng East component để triệt để chống đè chữ)
        JPanel sfxBottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        sfxBottom.setOpaque(false);

        JLabel lblSfxTest = new JLabel("Phát thử:");
        lblSfxTest.setFont(Theme.SMALL_BOLD);
        lblSfxTest.setForeground(Theme.DARK_TEXT);
        sfxBottom.add(lblSfxTest);

        JButton btnTestCorrect = createChipButton("Đúng", new Color(0xDC, 0xFC, 0xE7), Theme.GREEN, () -> {
            ensureSfxEnabled();
            SoundManager.playCorrect();
            lblSfxSub.setText("Đã phát: Trả lời đúng");
            lblSfxSub.setForeground(Theme.GREEN);
        });
        JButton btnTestWrong = createChipButton("Sai", new Color(0xFF, 0xE4, 0xE6), Theme.RED, () -> {
            ensureSfxEnabled();
            SoundManager.playWrong();
            lblSfxSub.setText("Đã phát: Trả lời sai");
            lblSfxSub.setForeground(Theme.RED);
        });
        JButton btnTestTick = createChipButton("Tích tắc", new Color(0xFE, 0xF3, 0xC7), new Color(0xB4, 0x53, 0x09), () -> {
            ensureSfxEnabled();
            SoundManager.playTick();
            lblSfxSub.setText("Đã phát: Tích tắc đồng hồ");
            lblSfxSub.setForeground(new Color(0xB4, 0x53, 0x09));
        });
        JButton btnTestCountdown = createChipButton("Đếm ngược", new Color(0xFF, 0xED, 0xD5), new Color(0xC2, 0x41, 0x0C), () -> {
            ensureSfxEnabled();
            SoundManager.playCountdown(0);
            lblSfxSub.setText("Đang phát: Đếm ngược 3s");
            lblSfxSub.setForeground(new Color(0xC2, 0x41, 0x0C));
        });
        JButton btnTestWin = createChipButton("Chiến thắng", new Color(0xF3, 0xE8, 0xFF), Theme.PURPLE, () -> {
            ensureSfxEnabled();
            SoundManager.playVictory();
            lblSfxSub.setText("Đã phát: Chiến thắng");
            lblSfxSub.setForeground(Theme.PURPLE);
        });

        sfxBottom.add(btnTestCorrect);
        sfxBottom.add(btnTestWrong);
        sfxBottom.add(btnTestTick);
        sfxBottom.add(btnTestCountdown);
        sfxBottom.add(btnTestWin);

        sfxCard.add(sfxTop, BorderLayout.NORTH);
        sfxCard.add(sfxSliderRow, BorderLayout.CENTER);
        sfxCard.add(sfxBottom, BorderLayout.SOUTH);
        body.add(sfxCard);

        add(body, BorderLayout.CENTER);

        // ==========================================
        // 3. BOTTOM ACTION BAR
        // ==========================================
        JPanel bottomBar = new JPanel(new BorderLayout());
        bottomBar.setBackground(Theme.WHITE);
        bottomBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER_LIGHT),
                new EmptyBorder(12, 20, 12, 20)));

        ModernButton btnReset = new ModernButton("Khôi phục mặc định", ModernButton.Style.OUTLINE_GRAY);
        btnReset.setPreferredSize(new Dimension(160, 38));
        btnReset.addActionListener(e -> resetToDefaults());

        ModernButton btnClose = new ModernButton("Lưu & Đóng", ModernButton.Style.PRIMARY);
        btnClose.setPreferredSize(new Dimension(130, 38));
        btnClose.addActionListener(e -> closeAndSave());

        bottomBar.add(btnReset, BorderLayout.WEST);
        bottomBar.add(btnClose, BorderLayout.EAST);
        add(bottomBar, BorderLayout.SOUTH);

        // Phím ESC để đóng
        getRootPane().registerKeyboardAction(e -> closeAndSave(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        // Lưu cài đặt khi người dùng bấm nút [X] ở thanh tiêu đề
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeAndSave();
            }
        });
    }

    private void ensureSfxEnabled() {
        if (SoundManager.isMuted()) {
            SoundManager.setMuted(false);
            btnSfxSwitch.setActive(true);
            sliderSfx.setEnabled(true);
            updateMasterStateFromChildren();
        }
    }

    /**
     * Bật hoặc tắt toàn bộ âm thanh (Master Mute).
     */
    private void applyMasterMute(boolean muteAll) {
        SoundManager.setMuted(muteAll);
        MusicManager.setMusicMuted(muteAll);

        btnBgmSwitch.setActive(!muteAll);
        btnSfxSwitch.setActive(!muteAll);

        sliderMusic.setEnabled(!muteAll);
        sliderSfx.setEnabled(!muteAll);

        lblMasterStatus.setText(muteAll ? "Đã tắt toàn bộ âm thanh" : "Đang phát âm thanh bình thường");
        lblBgmSub.setText(muteAll ? "Nhạc nền: Đã tắt tiếng" : "Nhạc nền: Đang bật");
        lblBgmSub.setForeground(Theme.MUTED_GRAY);
        lblSfxSub.setText(muteAll ? "Hiệu ứng: Đã tắt tiếng" : "Hiệu ứng: Đang bật");
        lblSfxSub.setForeground(Theme.MUTED_GRAY);
    }

    /**
     * Đồng bộ lại công tắc Master khi BGM hoặc SFX thay đổi riêng rẽ.
     */
    private void updateMasterStateFromChildren() {
        boolean allMuted = SoundManager.isMuted() && MusicManager.isMusicMuted();
        btnMasterSwitch.setActive(!allMuted);
        lblMasterStatus.setText(allMuted ? "Đã tắt toàn bộ âm thanh" : "Đang phát âm thanh bình thường");
    }

    /**
     * Khôi phục âm lượng & trạng thái về giá trị mặc định tối ưu.
     */
    private void resetToDefaults() {
        SoundManager.playClick();

        SoundManager.setMuted(false);
        MusicManager.setMusicMuted(false);
        SoundManager.setSfxVolume(0.85f);
        MusicManager.setMusicVolume(0.60f);

        btnMasterSwitch.setActive(true);
        lblMasterStatus.setText("Đang phát âm thanh bình thường");

        btnBgmSwitch.setActive(true);
        sliderMusic.setEnabled(true);
        sliderMusic.setValue(60);
        lblMusicVol.setText("60%");
        lblBgmSub.setText("Đã về mặc định (60%)");
        lblBgmSub.setForeground(Theme.MUTED_GRAY);

        btnSfxSwitch.setActive(true);
        sliderSfx.setEnabled(true);
        sliderSfx.setValue(85);
        lblSfxVol.setText("85%");
        lblSfxSub.setText("Đã về mặc định (85%)");
        lblSfxSub.setForeground(Theme.MUTED_GRAY);
    }

    /**
     * Lưu cài đặt vào Preferences và đóng hộp thoại.
     */
    private void closeAndSave() {
        SoundManager.playClick();
        SoundManager.savePreferences();
        MusicManager.savePreferences();
        dispose();
    }

    // ==========================================
    // CÁC COMPONENT GIAO DIỆN HELPER (KHÔNG DÙNG EMOJI)
    // ==========================================

    private JLabel createPillLabel(String text, Color textColor) {
        JLabel lbl = new JLabel(text, SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setupAntiAliasing(g2);
                g2.setColor(new Color(0xF1, 0xF5, 0xF9));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                g2.setColor(new Color(0xE2, 0xE8, 0xF0));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 8, 8));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        lbl.setFont(Theme.SMALL_BOLD);
        lbl.setForeground(textColor);
        lbl.setPreferredSize(new Dimension(52, 26));
        return lbl;
    }

    private JButton createChipButton(String text, Color bg, Color fg, Runnable action) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.setupAntiAliasing(g2);
                Color fill = getModel().isPressed() ? bg.darker() : (getModel().isRollover() ? bg.brighter() : bg);
                g2.setColor(fill);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.setColor(new Color(0, 0, 0, 20));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 10, 10));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(Theme.SMALL_BOLD);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(5, 9, 5, 9));
        btn.addActionListener(e -> action.run());
        return btn;
    }

    /**
     * Nút công tắc Switch chuẩn chữ tiếng Việt (BẬT / TẮT), không dùng ký tự Unicode đặc biệt.
     */
    public static class SwitchButton extends JButton {
        private boolean active;
        private final Color activeColor;

        public SwitchButton(boolean initial, Color activeColor) {
            this.active = initial;
            this.activeColor = activeColor;
            setFont(Theme.SMALL_BOLD);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(74, 30));
            updateText();
        }

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
            updateText();
            repaint();
        }

        private void updateText() {
            setText(active ? "BẬT" : "TẮT");
            setForeground(active ? Theme.WHITE : Theme.MUTED_GRAY);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.setupAntiAliasing(g2);
            int w = getWidth();
            int h = getHeight();
            if (active) {
                g2.setColor(activeColor);
            } else {
                g2.setColor(new Color(0xE2, 0xE8, 0xF0));
            }
            g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 14, 14));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /**
     * Giao diện thanh kéo (Slider) hiện đại.
     */
    private static class ModernSliderUI extends javax.swing.plaf.basic.BasicSliderUI {
        private final Color activeColor;

        public ModernSliderUI(JSlider b, Color activeColor) {
            super(b);
            this.activeColor = activeColor;
        }

        @Override
        public Dimension getThumbSize() {
            return new Dimension(20, 20);
        }

        @Override
        public void paintTrack(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.setupAntiAliasing(g2);
            Rectangle r = trackRect;
            int trackHeight = 8;
            int cy = r.y + (r.height - trackHeight) / 2;

            // Track nền
            g2.setColor(new Color(0xE2, 0xE8, 0xF0));
            g2.fillRoundRect(r.x, cy, r.width, trackHeight, trackHeight, trackHeight);

            // Track đã kéo (fill)
            int thumbX = thumbRect.x + thumbRect.width / 2;
            int filledWidth = Math.max(0, Math.min(r.width, thumbX - r.x));
            g2.setColor(slider.isEnabled() ? activeColor : new Color(0x94, 0xA3, 0xB8));
            g2.fillRoundRect(r.x, cy, filledWidth, trackHeight, trackHeight, trackHeight);
            g2.dispose();
        }

        @Override
        public void paintThumb(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.setupAntiAliasing(g2);
            Rectangle r = thumbRect;
            int size = 18;
            int x = r.x + (r.width - size) / 2;
            int y = r.y + (r.height - size) / 2;

            // Bóng đổ nhẹ
            g2.setColor(new Color(0, 0, 0, 25));
            g2.fillOval(x - 1, y, size + 2, size + 2);

            // Khối tròn
            g2.setColor(slider.isEnabled() ? Theme.WHITE : new Color(0xF1, 0xF5, 0xF9));
            g2.fillOval(x, y, size, size);

            // Viền màu chủ đạo
            g2.setColor(slider.isEnabled() ? activeColor : new Color(0x94, 0xA3, 0xB8));
            g2.setStroke(new BasicStroke(2.5f));
            g2.drawOval(x + 1, y + 1, size - 2, size - 2);
            g2.dispose();
        }

        @Override
        public void paintFocus(Graphics g) {
            // Không vẽ viền focus
        }
    }
}
