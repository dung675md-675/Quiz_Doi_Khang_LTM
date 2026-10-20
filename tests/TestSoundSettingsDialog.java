package tests;

import java.awt.GraphicsEnvironment;
import java.lang.reflect.Field;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import quiz.ui.MusicManager;
import quiz.ui.SoundManager;
import quiz.ui.SoundSettingsDialog;

public class TestSoundSettingsDialog {
    public static void main(String[] args) throws Exception {
        System.out.println("=== BAT DAU KIEM TRA SOUND SETTINGS DIALOG ===");

        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("Moi truong headless, bo qua test UI Swing.");
            return;
        }

        SwingUtilities.invokeAndWait(() -> {
            JFrame frame = new JFrame("Test Frame");
            frame.setSize(800, 600);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            try {
                for (int i = 1; i <= 5; i++) {
                    System.out.println("Lan " + i + ": Mo SoundSettingsDialog...");
                    SoundSettingsDialog dlg = new SoundSettingsDialog(frame);
                    dlg.setModal(false); // set non-modal de test dong tu dong
                    dlg.setVisible(true);

                    // Kiem tra gia tri ban dau
                    boolean initialMusicMuted = MusicManager.isMusicMuted();
                    boolean initialSfxMuted = SoundManager.isMuted();
                    float initialMusicVol = MusicManager.getMusicVolume();
                    float initialSfxVol = SoundManager.getSfxVolume();

                    System.out.printf("  Music muted=%b, vol=%.2f | SFX muted=%b, vol=%.2f%n",
                            initialMusicMuted, initialMusicVol, initialSfxMuted, initialSfxVol);

                    // Test toggle mute
                    MusicManager.setMusicMuted(!initialMusicMuted);
                    SoundManager.setMuted(!initialSfxMuted);
                    if (MusicManager.isMusicMuted() == initialMusicMuted) {
                        throw new RuntimeException("Music mute khong thay doi!");
                    }
                    if (SoundManager.isMuted() == initialSfxMuted) {
                        throw new RuntimeException("SFX mute khong thay doi!");
                    }

                    // Test set volume
                    MusicManager.setMusicVolume(0.75f);
                    SoundManager.setSfxVolume(0.50f);
                    if (Math.abs(MusicManager.getMusicVolume() - 0.75f) > 0.01f) {
                        throw new RuntimeException("Music volume khong dung!");
                    }
                    if (Math.abs(SoundManager.getSfxVolume() - 0.50f) > 0.01f) {
                        throw new RuntimeException("SFX volume khong dung!");
                    }

                    // Dong dialog
                    dlg.dispose();
                    System.out.println("  -> Dong SoundSettingsDialog thanh cong.");
                }

                System.out.println("=== TAT CA KIEM TRA SOUND SETTINGS DIALOG HOAN TAT THANH CONG! ===");
            } catch (Exception ex) {
                ex.printStackTrace();
                System.exit(1);
            } finally {
                frame.dispose();
            }
        });

        System.exit(0);
    }
}

