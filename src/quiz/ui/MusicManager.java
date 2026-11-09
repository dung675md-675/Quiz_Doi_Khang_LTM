package quiz.ui;

import java.io.*;
import java.net.URL;
import java.util.concurrent.atomic.*;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.sound.sampled.*;

/**
 * MusicManager — Quản lý nhạc nền (BGM) cho Quiz Arena.
 *
 * - Phát nhạc nền sảnh (lobby_music) và trận đấu (battle_music).
 * - Tự động lặp vô hạn trong khi người chơi ở màn hình tương ứng.
 * - Chuyển bài không gây chồng lấn (dừng bài cũ trước khi phát bài mới).
 * - Không khởi động lại bài nếu cùng track đang được phát.
 * - Hoàn toàn độc lập với SoundManager (SFX). Tắt nhạc không ảnh hưởng SFX.
 * - Hỗ trợ bật/tắt và điều chỉnh âm lượng riêng, thay đổi gain tức thời theo slider.
 * - Lưu và khôi phục cài đặt qua Preferences.
 * - Không block EDT hoặc TCP thread.
 */
public final class MusicManager {

    private static final Logger LOG = Logger.getLogger(MusicManager.class.getName());
    private static final Preferences PREFS = Preferences.userNodeForPackage(MusicManager.class);

    private static final String SOUND_DIR = "sound_effect/";

    // ==========================================
    // TRẠNG THÁI & LƯU TRỮ
    // ==========================================
    private static volatile boolean musicMuted = false;
    private static volatile float musicVolume = 0.6f; // 0.0 – 1.0

    static {
        try {
            musicMuted = PREFS.getBoolean("music_muted", false);
            musicVolume = PREFS.getFloat("music_volume", 0.60f);
        } catch (Exception ignored) {}
    }

    public static void savePreferences() {
        try {
            PREFS.putBoolean("music_muted", musicMuted);
            PREFS.putFloat("music_volume", musicVolume);
            PREFS.flush();
        } catch (Exception ignored) {}
    }

    /** Tên track hiện tại đang phát (null nếu không phát) */
    private static volatile String currentTrack = null;

    /** Thread đang phát nhạc */
    private static volatile Thread musicThread = null;

    /** Line đang phát hiện tại (để cập nhật âm lượng real-time khi kéo slider) */
    private static volatile SourceDataLine activeLine = null;

    /** Signal dừng thread hiện tại */
    private static final AtomicBoolean stopRequested = new AtomicBoolean(false);

    // ==========================================
    // API NHẠC NỀN
    // ==========================================

    public static boolean isMusicMuted() {
        return musicMuted;
    }

    public static void setMusicMuted(boolean muted) {
        musicMuted = muted;
        savePreferences();
        if (muted) {
            stopMusicInternal(false); // dừng playback nhưng giữ currentTrack nếu muốn bật lại
        } else {
            String track = currentTrack;
            if (track != null) {
                playTrack(track);
            }
        }
    }

    public static void toggleMusicMute() {
        setMusicMuted(!musicMuted);
    }

    public static float getMusicVolume() {
        return musicVolume;
    }

    public static void setMusicVolume(float vol) {
        musicVolume = Math.max(0f, Math.min(1f, vol));
        savePreferences();
        SourceDataLine line = activeLine;
        if (line != null && line.isOpen()) {
            applyGain(line, musicVolume);
        }
    }

    /**
     * Phát nhạc sảnh. Nếu forceRestart = false và đang phát rồi thì không làm gì.
     */
    public static void playLobbyMusic() {
        playLobbyMusic(false);
    }

    public static void playLobbyMusic(boolean forceRestart) {
        currentTrack = "lobby_music.mp3";
        if (musicMuted) return;
        playTrack("lobby_music.mp3", forceRestart);
    }

    /**
     * Phát nhạc trận đấu. Nếu forceRestart = false và đang phát rồi thì không làm gì.
     */
    public static void playBattleMusic() {
        playBattleMusic(false);
    }

    public static void playBattleMusic(boolean forceRestart) {
        currentTrack = "battle_music.mp3";
        if (musicMuted) return;
        playTrack("battle_music.mp3", forceRestart);
    }

    /**
     * Dừng nhạc nền hiện tại và xóa track.
     */
    public static void stopMusic() {
        stopMusicInternal(true);
    }

    private static void stopMusicInternal(boolean clearTrack) {
        stopRequested.set(true);
        SourceDataLine line = activeLine;
        if (line != null) {
            try {
                line.stop();
                line.flush();
                line.close();
            } catch (Exception ignored) {}
            activeLine = null;
        }
        Thread t = musicThread;
        if (t != null && t.isAlive()) {
            t.interrupt();
        }
        musicThread = null;
        if (clearTrack) {
            currentTrack = null;
        }
    }

    /**
     * Kiểm tra có đang phát nhạc không.
     */
    public static boolean isPlaying() {
        return musicThread != null && musicThread.isAlive();
    }

    /**
     * Giải phóng toàn bộ tài nguyên khi đóng ứng dụng.
     */
    public static void shutdown() {
        stopMusic();
    }

    // ==========================================
    // ENGINE PHÁT NHẠC NỀN (loop)
    // ==========================================

    /**
     * Phát một track liên tục (loop) trong thread riêng.
     * Dừng track cũ trước.
     */
    private static void playTrack(String filename) {
        playTrack(filename, false);
    }

    private static void playTrack(String filename, boolean forceRestart) {
        // Nếu thread cũ đang phát đúng track này và không yêu cầu phát lại thì giữ nguyên
        Thread old = musicThread;
        if (!forceRestart && old != null && old.isAlive() && filename.equals(currentTrack) && !stopRequested.get()) {
            return;
        }

        stopRequested.set(true);
        SourceDataLine oldLine = activeLine;
        if (oldLine != null) {
            try {
                oldLine.stop();
                oldLine.flush();
                oldLine.close();
            } catch (Exception ignored) {}
            activeLine = null;
        }
        if (old != null && old.isAlive()) {
            old.interrupt();
            try { old.join(250); } catch (InterruptedException ignored) {}
        }

        stopRequested.set(false);
        currentTrack = filename;

        Thread t = new Thread(() -> {
            while (!stopRequested.get() && !Thread.currentThread().isInterrupted()) {
                boolean played = playFileSingle(filename);
                if (!played) {
                    if (!stopRequested.get() && !Thread.currentThread().isInterrupted()) {
                        LOG.warning("[MusicManager] Không thể phát nhạc nền: " + filename);
                    }
                    break;
                }
                if (stopRequested.get() || Thread.currentThread().isInterrupted()) {
                    break;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "QuizArena-BGM-" + filename);

        t.setDaemon(true);
        musicThread = t;
        t.start();
    }

    /**
     * Phát một lần toàn bộ file âm thanh.
     * Hỗ trợ WAV và MP3 (qua mp3spi).
     *
     * @return true nếu phát xong bình thường, false nếu bị dừng hoặc lỗi
     */
    private static boolean playFileSingle(String filename) {
        AudioInputStream rawStream = openAudioStream(filename);
        if (rawStream == null) {
            return false;
        }

        try (AudioInputStream inStream = rawStream) {
            AudioFormat baseFormat = inStream.getFormat();
            AudioFormat targetFormat = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                baseFormat.getSampleRate(),
                16,
                baseFormat.getChannels(),
                baseFormat.getChannels() * 2,
                baseFormat.getSampleRate(),
                false
            );

            AudioInputStream pcmStream;
            if (AudioSystem.isConversionSupported(targetFormat, baseFormat)) {
                pcmStream = AudioSystem.getAudioInputStream(targetFormat, inStream);
            } else {
                pcmStream = inStream;
                targetFormat = baseFormat;
            }

            DataLine.Info info = new DataLine.Info(SourceDataLine.class, targetFormat);
            if (!AudioSystem.isLineSupported(info)) {
                LOG.warning("[MusicManager] Định dạng không hỗ trợ: " + filename);
                return false;
            }

            try (SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
                 AudioInputStream finalStream = pcmStream) {

                line.open(targetFormat, 16384);
                applyGain(line, musicVolume);
                line.start();
                activeLine = line;

                byte[] buf = new byte[16384];
                int bytesRead;
                while ((bytesRead = finalStream.read(buf)) != -1) {
                    if (stopRequested.get() || Thread.currentThread().isInterrupted()) {
                        line.stop();
                        line.flush();
                        return false;
                    }
                    line.write(buf, 0, bytesRead);
                }
                line.drain();
                return true;
            } finally {
                activeLine = null;
            }

        } catch (Exception e) {
            LOG.warning("[MusicManager] Lỗi phát nhạc nền " + filename + ": " + e.getMessage());
            return false;
        }
    }

    private static AudioInputStream openAudioStream(String filename) {
        File f = new File(SOUND_DIR + filename);
        if (f.exists()) {
            try {
                return AudioSystem.getAudioInputStream(f);
            } catch (Exception ignored) {}
        }

        // Thử alternate extension
        String alt = filename.endsWith(".mp3")
            ? filename.substring(0, filename.length() - 4) + ".wav"
            : (filename.endsWith(".wav") ? filename.substring(0, filename.length() - 4) + ".mp3" : null);
        if (alt != null) {
            File altFile = new File(SOUND_DIR + alt);
            if (altFile.exists()) {
                try {
                    return AudioSystem.getAudioInputStream(altFile);
                } catch (Exception ignored) {}
            }
        }

        // Thử Classpath
        String[] cpPaths = {
            "/sound_effect/" + filename,
            "/sounds/" + filename,
            "/" + filename
        };
        for (String path : cpPaths) {
            URL url = MusicManager.class.getResource(path);
            if (url != null) {
                try {
                    return AudioSystem.getAudioInputStream(url);
                } catch (Exception ignored) {}
            }
        }

        return null;
    }

    private static void applyGain(SourceDataLine line, float vol) {
        try {
            if (line != null && line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gain = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
                float dB = (vol <= 0.0001f) ? gain.getMinimum() : (float) (Math.log10(vol) * 20.0);
                dB = Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), dB));
                gain.setValue(dB);
            }
        } catch (Exception ignored) {}
    }

    private MusicManager() {}
}
