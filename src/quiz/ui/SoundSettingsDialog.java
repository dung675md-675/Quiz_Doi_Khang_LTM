package quiz.ui;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * SoundSettingsDialog — Hộp thoại điều khiển & cài đặt âm thanh cho Quiz Arena.
 *
 * Cung cấp:
 * - Bật/Tắt nhạc nền (BGM) độc lập.
 * - Điều chỉnh âm lượng nhạc nền (0% - 100%) theo thời gian thực.
 * - Bật/Tắt hiệu ứng âm thanh (SFX) độc lập.
 * - Điều chỉnh âm lượng hiệu ứng (0% - 100%).
 * - Nút phát thử hiệu ứng âm thanh để kiểm tra thiết bị.
 * - Tự động lưu và khôi phục cài đặt qua Preferences.
 */
public class SoundSettingsDialog extends JDialog {

    private final JCheckBox chkMusic;
    private final JSlider sliderMusic;
    private final JLabel lblMusicVol;

    private final JCheckBox chkSfx;
    private final JSlider sliderSfx;
    private final JLabel lblSfxVol;

    public SoundSettingsDialog(Window owner) {
        super(owner, "Cài Đặt Âm Thanh — Quiz Arena", ModalityType.APPLICATION_MODAL);

        setSize(460, 480);
        setLocationRelativeTo(owner);
        setResizable(false);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Theme.WHITE);

        // ==========================================
        // 1. HEADER
        // ==========================================
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(Theme.WHITE);
        header.setBorder(new EmptyBorder(20, 24, 12, 24));

        JLabel title = new JLabel("CÀI ĐẶT ÂM THANH", SwingConstants.CENTER);
        title.setFont(Theme.TITLE);
        title.setForeground(Theme.PURPLE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Tùy chỉnh nhạc nền và hiệu ứng âm thanh thi đấu", SwingConstants.CENTER);
        subtitle.setFont(Theme.SMALL);
        subtitle.setForeground(Theme.MUTED_GRAY);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        header.add(title);
        header.add(Box.createVerticalStrut(4));
        header.add(subtitle);
        add(header, BorderLayout.NORTH);

        // ==========================================
        // 2. BODY (BGM Card + SFX Card)
        // ==========================================
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(6, 24, 10, 24));

        // --- CARD 1: NHẠC NỀN (BGM) ---
        ModernCard bgmCard = new ModernCard(new BorderLayout(0, 8));
        bgmCard.setCardBackground(Theme.OFF_WHITE);
        bgmCard.setCardPadding(14, 18, 14, 18);
        bgmCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));

        JPanel bgmTop = new JPanel(new BorderLayout());
        bgmTop.setOpaque(false);
        JLabel lblBgmTitle = new JLabel("Nhạc Nền (BGM)");
        lblBgmTitle.setFont(Theme.BODY_BOLD);
        lblBgmTitle.setForeground(Theme.DARK_TEXT);

        chkMusic = new JCheckBox("Bật nhạc", !MusicManager.isMusicMuted());
        chkMusic.setFont(Theme.SMALL_BOLD);
        chkMusic.setOpaque(false);
        chkMusic.setFocusPainted(false);
        chkMusic.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        bgmTop.add(lblBgmTitle, BorderLayout.WEST);
        bgmTop.add(chkMusic, BorderLayout.EAST);

        int initMusicVol = Math.round(MusicManager.getMusicVolume() * 100);
        sliderMusic = new JSlider(0, 100, initMusicVol);
        sliderMusic.setOpaque(false);
        sliderMusic.setEnabled(!MusicManager.isMusicMuted());
        lblMusicVol = new JLabel(initMusicVol + "%", SwingConstants.RIGHT);
        lblMusicVol.setFont(Theme.SMALL_BOLD);
        lblMusicVol.setForeground(Theme.MUTED_GRAY);
        lblMusicVol.setPreferredSize(new Dimension(42, 20));

        JPanel bgmSliderRow = new JPanel(new BorderLayout(8, 0));
        bgmSliderRow.setOpaque(false);
        bgmSliderRow.add(sliderMusic, BorderLayout.CENTER);
        bgmSliderRow.add(lblMusicVol, BorderLayout.EAST);

        chkMusic.addActionListener(e -> {
            boolean enabled = chkMusic.isSelected();
            MusicManager.setMusicMuted(!enabled);
            sliderMusic.setEnabled(enabled);
        });

        sliderMusic.addChangeListener(e -> {
            int val = sliderMusic.getValue();
            lblMusicVol.setText(val + "%");
            MusicManager.setMusicVolume(val / 100f);
        });

        bgmCard.add(bgmTop, BorderLayout.NORTH);
        bgmCard.add(bgmSliderRow, BorderLayout.CENTER);
        body.add(bgmCard);

        body.add(Box.createVerticalStrut(14));

        // --- CARD 2: HIỆU ỨNG ÂM THANH (SFX) ---
        ModernCard sfxCard = new ModernCard(new BorderLayout(0, 8));
        sfxCard.setCardBackground(Theme.OFF_WHITE);
        sfxCard.setCardPadding(14, 18, 14, 18);
        sfxCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));

        JPanel sfxTop = new JPanel(new BorderLayout());
        sfxTop.setOpaque(false);
        JLabel lblSfxTitle = new JLabel("Hiệu Ứng (SFX)");
        lblSfxTitle.setFont(Theme.BODY_BOLD);
        lblSfxTitle.setForeground(Theme.DARK_TEXT);

        chkSfx = new JCheckBox("Bật âm hiệu ứng", !SoundManager.isMuted());
        chkSfx.setFont(Theme.SMALL_BOLD);
        chkSfx.setOpaque(false);
        chkSfx.setFocusPainted(false);
        chkSfx.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        sfxTop.add(lblSfxTitle, BorderLayout.WEST);
        sfxTop.add(chkSfx, BorderLayout.EAST);

        int initSfxVol = Math.round(SoundManager.getSfxVolume() * 100);
        sliderSfx = new JSlider(0, 100, initSfxVol);
        sliderSfx.setOpaque(false);
        sliderSfx.setEnabled(!SoundManager.isMuted());
        lblSfxVol = new JLabel(initSfxVol + "%", SwingConstants.RIGHT);
        lblSfxVol.setFont(Theme.SMALL_BOLD);
        lblSfxVol.setForeground(Theme.MUTED_GRAY);
        lblSfxVol.setPreferredSize(new Dimension(42, 20));

        JPanel sfxSliderRow = new JPanel(new BorderLayout(8, 0));
        sfxSliderRow.setOpaque(false);
        sfxSliderRow.add(sliderSfx, BorderLayout.CENTER);
        sfxSliderRow.add(lblSfxVol, BorderLayout.EAST);

        chkSfx.addActionListener(e -> {
            boolean enabled = chkSfx.isSelected();
            SoundManager.setMuted(!enabled);
            sliderSfx.setEnabled(enabled);
        });

        sliderSfx.addChangeListener(e -> {
            int val = sliderSfx.getValue();
            lblSfxVol.setText(val + "%");
            SoundManager.setSfxVolume(val / 100f);
        });

        // Nút thử âm thanh
        JPanel sfxTestRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        sfxTestRow.setOpaque(false);
        ModernButton btnTestSfx = new ModernButton("Phát thử SFX", ModernButton.Style.OUTLINE_GRAY);
        btnTestSfx.setPreferredSize(new Dimension(120, 32));
        btnTestSfx.setFont(Theme.SMALL_BOLD);
        btnTestSfx.addActionListener(e -> SoundManager.playCorrect());
        sfxTestRow.add(btnTestSfx);

        sfxCard.add(sfxTop, BorderLayout.NORTH);
        sfxCard.add(sfxSliderRow, BorderLayout.CENTER);
        sfxCard.add(sfxTestRow, BorderLayout.SOUTH);
        body.add(sfxCard);

        add(body, BorderLayout.CENTER);

        // ==========================================
        // 3. BOTTOM ACTION BAR
        // ==========================================
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 12));
        bottomBar.setBackground(Theme.WHITE);
        bottomBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER_LIGHT));

        ModernButton btnClose = new ModernButton("Lưu & Đóng", ModernButton.Style.PRIMARY);
        btnClose.setPreferredSize(new Dimension(160, 40));
        btnClose.addActionListener(e -> {
            SoundManager.playClick();
            SoundManager.savePreferences();
            MusicManager.savePreferences();
            dispose();
        });

        bottomBar.add(btnClose);
        add(bottomBar, BorderLayout.SOUTH);
    }
}
