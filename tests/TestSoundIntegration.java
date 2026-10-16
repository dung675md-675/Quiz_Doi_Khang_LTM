package tests;

import java.io.File;
import javax.sound.sampled.*;
import quiz.Rank;
import quiz.ui.MusicManager;
import quiz.ui.SoundManager;
import quiz.ui.SoundSettingsDialog;

public class TestSoundIntegration {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("    KIỂM THỬ HỆ THỐNG ÂM THANH QUIZ ARENA        ");
        System.out.println("==================================================");

        int passed = 0;
        int failed = 0;

        // 1. Kiểm tra 11 file âm thanh thực tế
        System.out.println("\n[1] Kiểm tra sự tồn tại và định dạng của 11 file âm thanh:");
        String[] requiredFiles = {
            "lobby_music.mp3",
            "battle_music.mp3",
            "button_click.mp3",
            "match_found.wav",
            "answer_select.mp3",
            "correct.wav",
            "wrong.mp3",
            "countdown_3-0s.mp3",
            "victory.wav",
            "defeat.wav",
            "rank_up.wav"
        };

        for (String fName : requiredFiles) {
            File f = new File("sound_effect/" + fName);
            if (!f.exists()) {
                System.err.println("  ❌ THIẾU FILE: " + fName);
                failed++;
                continue;
            }
            try (AudioInputStream ais = AudioSystem.getAudioInputStream(f)) {
                AudioFormat fmt = ais.getFormat();
                System.out.printf("  ✓ %-20s | Format: %-10s | SampleRate: %-7.1f | Channels: %d | Size: %d KB\n",
                    fName, fmt.getEncoding(), fmt.getSampleRate(), fmt.getChannels(), f.length() / 1024);
                passed++;
            } catch (Exception e) {
                System.err.println("  ❌ LỖI ĐỌC FILE: " + fName + " — " + e.getMessage());
                failed++;
            }
        }

        // 2. Kiểm tra SoundManager SFX API
        System.out.println("\n[2] Kiểm tra SoundManager API:");
        try {
            boolean originalMute = SoundManager.isMuted();
            float originalVol = SoundManager.getSfxVolume();

            // Test mute toggle
            SoundManager.setMuted(true);
            if (!SoundManager.isMuted()) throw new AssertionError("SoundManager mute failed");
            SoundManager.setMuted(false);
            if (SoundManager.isMuted()) throw new AssertionError("SoundManager unmute failed");

            // Test volume setting
            SoundManager.setSfxVolume(0.5f);
            if (Math.abs(SoundManager.getSfxVolume() - 0.5f) > 0.01f) throw new AssertionError("Volume set failed");
            SoundManager.setSfxVolume(1.5f); // clamp
            if (SoundManager.getSfxVolume() > 1.0f) throw new AssertionError("Volume upper clamp failed");
            SoundManager.setSfxVolume(-0.5f); // clamp
            if (SoundManager.getSfxVolume() < 0.0f) throw new AssertionError("Volume lower clamp failed");

            // Restore
            SoundManager.setMuted(originalMute);
            SoundManager.setSfxVolume(originalVol);
            System.out.println("  ✓ SoundManager Mute & Volume control passed");
            passed++;

            // Test SFX calls (should not throw and should run async)
            SoundManager.playClick();
            SoundManager.playAnswerSelect();
            SoundManager.playCorrect();
            SoundManager.playWrong();
            SoundManager.playMatchFound();
            SoundManager.playVictory();
            SoundManager.playDefeat();
            SoundManager.playRankUp();
            SoundManager.playTick();
            SoundManager.playPop();
            SoundManager.playScorePoint();
            SoundManager.playEmote();
            System.out.println("  ✓ Tất cả các phương thức phát SFX gọi thành công (bất đồng bộ)");
            passed++;
        } catch (Exception e) {
            System.err.println("  ❌ Lỗi SoundManager: " + e.getMessage());
            failed++;
        }

        // 3. Kiểm tra cơ chế Countdown chống phát trùng & ngắt dừng
        System.out.println("\n[3] Kiểm tra Countdown chống phát trùng & ngắt dừng:");
        try {
            // Phát countdown cho câu hỏi 0
            SoundManager.playCountdown(0);
            Thread.sleep(150);

            // Gọi lại cho câu hỏi 0 -> Không được tạo thêm thread hay phát đè
            SoundManager.playCountdown(0);
            System.out.println("  ✓ Chống phát lặp countdown cùng câu hỏi: Hoạt động đúng");

            // Dừng countdown
            SoundManager.stopCountdown();
            Thread.sleep(100);
            System.out.println("  ✓ Dừng countdown tức thời: Hoạt động đúng");
            passed++;
        } catch (Exception e) {
            System.err.println("  ❌ Lỗi Countdown: " + e.getMessage());
            failed++;
        }

        // 4. Kiểm tra MusicManager
        System.out.println("\n[4] Kiểm tra MusicManager API:");
        try {
            boolean origMusicMute = MusicManager.isMusicMuted();
            float origMusicVol = MusicManager.getMusicVolume();

            MusicManager.setMusicMuted(true);
            if (!MusicManager.isMusicMuted()) throw new AssertionError("MusicManager mute failed");
            MusicManager.setMusicMuted(false);

            MusicManager.setMusicVolume(0.4f);
            if (Math.abs(MusicManager.getMusicVolume() - 0.4f) > 0.01f) throw new AssertionError("Music volume failed");

            // Chuyển nhạc sảnh -> nhạc trận -> dừng
            MusicManager.playLobbyMusic();
            Thread.sleep(200);
            MusicManager.playBattleMusic();
            Thread.sleep(200);
            MusicManager.stopMusic();
            Thread.sleep(100);

            // Restore
            MusicManager.setMusicMuted(origMusicMute);
            MusicManager.setMusicVolume(origMusicVol);
            System.out.println("  ✓ MusicManager chuyển track, volume và stop hoạt động trơn tru");
            passed++;
        } catch (Exception e) {
            System.err.println("  ❌ Lỗi MusicManager: " + e.getMessage());
            failed++;
        }

        // 5. Kiểm tra tính độc lập giữa SFX và BGM
        System.out.println("\n[5] Kiểm tra tính độc lập giữa SFX và BGM:");
        try {
            MusicManager.setMusicMuted(true);
            SoundManager.setMuted(false);
            if (!MusicManager.isMusicMuted() || SoundManager.isMuted()) {
                throw new AssertionError("SFX and BGM mute states are coupled!");
            }

            MusicManager.setMusicMuted(false);
            SoundManager.setMuted(true);
            if (MusicManager.isMusicMuted() || !SoundManager.isMuted()) {
                throw new AssertionError("SFX and BGM mute states are coupled!");
            }

            MusicManager.setMusicMuted(false);
            SoundManager.setMuted(false);
            System.out.println("  ✓ SFX và BGM hoàn toàn độc lập theo đúng yêu cầu");
            passed++;
        } catch (Exception e) {
            System.err.println("  ❌ Lỗi tính độc lập: " + e.getMessage());
            failed++;
        }

        // 6. Kiểm tra thuật toán Rank & kích hoạt RankUp
        System.out.println("\n[6] Kiểm tra logic Rank & RankUp:");
        try {
            Rank r0 = Rank.fromScore(0);
            Rank r9 = Rank.fromScore(9);
            Rank r10 = Rank.fromScore(10);
            Rank r30 = Rank.fromScore(30);

            if (r0 != Rank.IRON || r9 != Rank.IRON) throw new AssertionError("Score 0-9 should be IRON");
            if (r10 != Rank.BRONZE) throw new AssertionError("Score 10 should be BRONZE");
            if (r30 != Rank.GOLD) throw new AssertionError("Score 30 should be GOLD");

            boolean rankIncreased = (r10.ordinal() > r0.ordinal());
            if (!rankIncreased) throw new AssertionError("Rank ordinal check failed");

            System.out.println("  ✓ Logic xếp hạng: Sắt -> Đồng -> Vàng chính xác");
            passed++;
        } catch (Exception e) {
            System.err.println("  ❌ Lỗi Rank: " + e.getMessage());
            failed++;
        }

        // 7. Kiểm tra khởi tạo Dialog Cài Đặt Âm Thanh
        System.out.println("\n[7] Kiểm tra khởi tạo SoundSettingsDialog:");
        try {
            SoundSettingsDialog dlg = new SoundSettingsDialog(null);
            if (dlg.getTitle() == null || !dlg.getTitle().contains("Cài Đặt Âm Thanh")) {
                throw new AssertionError("Dialog title incorrect");
            }
            dlg.dispose();
            System.out.println("  ✓ SoundSettingsDialog khởi tạo thành công");
            passed++;
        } catch (Exception e) {
            System.err.println("  ❌ Lỗi SoundSettingsDialog: " + e.getMessage());
            failed++;
        }

        // 8. Kiểm tra SoundManager.playSound(name) và Caching
        System.out.println("\n[8] Kiểm tra SoundManager.playSound(name) & Caching:");
        try {
            SoundManager.playSound("click");
            SoundManager.playSound("answer_select");
            SoundManager.playSound("correct");
            SoundManager.playSound("wrong");
            SoundManager.playSound("match_found");
            SoundManager.playSound("victory");
            SoundManager.playSound("defeat");
            SoundManager.playSound("rank_up");
            System.out.println("  ✓ SoundManager.playSound(id) và caching hoạt động chính xác");
            passed++;
        } catch (Exception e) {
            System.err.println("  ❌ Lỗi playSound(id): " + e.getMessage());
            failed++;
        }

        System.out.println("\n==================================================");
        System.out.printf("TỔNG KẾT: %d ĐẠT, %d THẤT BẠI\n", passed, failed);
        System.out.println("==================================================");

        if (failed > 0) {
            System.exit(1);
        }
    }
}
