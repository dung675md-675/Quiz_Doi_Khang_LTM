package quiz.ui;

import java.io.*;
import java.net.URL;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.sound.sampled.*;

/**
 * SoundManager — Quản lý hiệu ứng âm thanh (SFX) cho Quiz Arena.
 *
 * Ưu tiên phát từ file thực tế trong thư mục sound_effect/.
 * Nếu file không tồn tại hoặc lỗi, tự động fallback sang PCM synthesis.
 *
 * Hỗ trợ:
 * - WAV: javax.sound.sampled trực tiếp.
 * - MP3: thông qua mp3spi + tritonus (nếu có trên classpath).
 *
 * Hoàn toàn bất đồng bộ, không block EDT hoặc TCP reader thread.
 * Quản lý vòng đời an toàn, hỗ trợ ngắt âm countdown khi chuyển câu hỏi.
 */
public final class SoundManager {

    private static final Logger LOG = Logger.getLogger(SoundManager.class.getName());
    private static final Preferences PREFS = Preferences.userNodeForPackage(SoundManager.class);

    // ==========================================
    // TRẠNG THÁI TOÀN CỤC & LƯU TRỮ
    // ==========================================
    private static volatile boolean sfxMuted = false;
    private static volatile float sfxVolume = 0.85f; // 0.0 – 1.0

    static {
        try {
            sfxMuted = PREFS.getBoolean("sfx_muted", false);
            sfxVolume = PREFS.getFloat("sfx_volume", 0.85f);
        } catch (Exception ignored) {}
    }

    public static void savePreferences() {
        try {
            PREFS.putBoolean("sfx_muted", sfxMuted);
            PREFS.putFloat("sfx_volume", sfxVolume);
            PREFS.flush();
        } catch (Exception ignored) {}
    }

    /** Thread pool riêng cho SFX — daemon, tối đa 3 thread song song */
    private static final ExecutorService sfxPool = Executors.newFixedThreadPool(3, r -> {
        Thread t = new Thread(r, "QuizArena-SFX");
        t.setDaemon(true);
        return t;
    });

    // ==========================================
    // ĐƯỜNG DẪN TÀI NGUYÊN
    // ==========================================
    /** Thư mục chứa file âm thanh (relative to working dir, tức là gốc project) */
    private static final String SOUND_DIR = "sound_effect/";

    // ==========================================
    // API BẬT/TẮT ÂM THANH
    // ==========================================
    public static boolean isMuted() {
        return sfxMuted;
    }

    public static void setMuted(boolean muted) {
        sfxMuted = muted;
        savePreferences();
    }

    public static void toggleMute() {
        sfxMuted = !sfxMuted;
        savePreferences();
    }

    public static float getSfxVolume() {
        return sfxVolume;
    }

    public static void setSfxVolume(float vol) {
        sfxVolume = Math.max(0f, Math.min(1f, vol));
        savePreferences();
    }

    // ==========================================
    // API PHÁT HIỆU ỨNG — dùng file thực
    // ==========================================

    private static volatile long lastClickTime = 0;
    private static volatile long lastSelectTime = 0;

    /** Phát hiệu ứng theo tên hoặc ID */
    public static void playSound(String soundId) {
        if (soundId == null || sfxMuted) return;
        switch (soundId.toLowerCase()) {
            case "click", "button_click" -> playClick();
            case "answer_select", "select" -> playAnswerSelect();
            case "correct" -> playCorrect();
            case "wrong" -> playWrong();
            case "match_found", "match" -> playMatchFound();
            case "victory", "win" -> playVictory();
            case "defeat", "lose" -> playDefeat();
            case "rank_up" -> playRankUp();
            case "tick" -> playTick();
            case "pop" -> playPop();
            case "score_point", "point" -> playScorePoint();
            case "emote" -> playEmote();
            case "invite", "invite_alert" -> playInviteAlert();
            default -> sfxPool.submit(() -> playFile(soundId));
        }
    }

    /** Tiếng click nút bấm (có debounce 60ms chống spam click) */
    public static void playClick() {
        if (sfxMuted) return;
        long now = System.currentTimeMillis();
        if (now - lastClickTime < 60) return;
        lastClickTime = now;
        sfxPool.submit(() -> {
            if (!playFile("button_click.mp3"))
                playTone(750, 35, 0.25 * sfxVolume, ToneShape.DECAY);
        });
    }

    /** Tiếng chọn đáp án (có debounce 50ms) */
    public static void playAnswerSelect() {
        if (sfxMuted) return;
        long now = System.currentTimeMillis();
        if (now - lastSelectTime < 50) return;
        lastSelectTime = now;
        sfxPool.submit(() -> {
            if (!playFile("answer_select.mp3"))
                playTone(600, 40, 0.3 * sfxVolume, ToneShape.SMOOTH);
        });
    }

    /** Trả lời đúng */
    public static void playCorrect() {
        if (sfxMuted) return;
        sfxPool.submit(() -> {
            if (!playFile("correct.wav")) {
                playTone(587, 80, 0.45 * sfxVolume, ToneShape.SMOOTH);
                sleep(20);
                playTone(880, 140, 0.5 * sfxVolume, ToneShape.DECAY);
            }
        });
    }

    /** Trả lời sai */
    public static void playWrong() {
        if (sfxMuted) return;
        sfxPool.submit(() -> {
            if (!playFile("wrong.mp3")) {
                playTone(260, 90, 0.45 * sfxVolume, ToneShape.DECAY);
                sleep(15);
                playTone(200, 150, 0.45 * sfxVolume, ToneShape.DECAY);
            }
        });
    }

    /** Tìm thấy đối thủ / ghép trận thành công */
    public static void playMatchFound() {
        if (sfxMuted) return;
        sfxPool.submit(() -> {
            if (!playFile("match_found.wav"))
                playInviteAlertFallback();
        });
    }

    /** Chiến thắng */
    public static void playVictory() {
        if (sfxMuted) return;
        sfxPool.submit(() -> {
            if (!playFile("victory.wav"))
                playVictoryFallback();
        });
    }

    /** Thất bại */
    public static void playDefeat() {
        if (sfxMuted) return;
        sfxPool.submit(() -> {
            if (!playFile("defeat.wav"))
                playDefeatFallback();
        });
    }

    /** Lên hạng */
    public static void playRankUp() {
        if (sfxMuted) return;
        sfxPool.submit(() -> {
            if (!playFile("rank_up.wav"))
                playScorePointFallback();
        });
    }

    /** Nhận lời mời thách đấu */
    public static void playInviteAlert() {
        if (sfxMuted) return;
        sfxPool.submit(() -> {
            if (!playFile("match_found.wav"))
                playInviteAlertFallback();
        });
    }

    /** Tiếng tích tắc đồng hồ (khi còn <= 3 giây) */
    public static void playTick() {
        if (sfxMuted) return;
        sfxPool.submit(() -> playTone(1100, 30, 0.35 * sfxVolume, ToneShape.CLICK));
    }

    /** Cảnh báo hết giờ (fallback khi không có countdown) */
    public static void playTimeWarning() {
        if (sfxMuted) return;
        sfxPool.submit(() -> {
            playTone(440, 70, 0.4 * sfxVolume, ToneShape.SMOOTH);
            sleep(30);
            playTone(330, 120, 0.45 * sfxVolume, ToneShape.DECAY);
        });
    }

    /** Phát âm thanh "pop" khi câu hỏi xuất hiện */
    public static void playPop() {
        if (sfxMuted) return;
        sfxPool.submit(() -> {
            playTone(480, 25, 0.3 * sfxVolume, ToneShape.SMOOTH);
            playTone(720, 45, 0.4 * sfxVolume, ToneShape.DECAY);
        });
    }

    /** Khi ghi được điểm real-time */
    public static void playScorePoint() {
        if (sfxMuted) return;
        sfxPool.submit(() -> playScorePointFallback());
    }

    /** Khi gửi/nhận emote */
    public static void playEmote() {
        if (sfxMuted) return;
        sfxPool.submit(() -> {
            playTone(600, 40, 0.35 * sfxVolume, ToneShape.SMOOTH);
            playTone(950, 60, 0.4 * sfxVolume, ToneShape.DECAY);
        });
    }

    // ==========================================
    // COUNTDOWN — quản lý riêng (chống phát trùng)
    // ==========================================

    /** ID câu hỏi đang phát countdown, -1 nếu không có */
    private static volatile int countdownQuestionId = -1;
    private static final AtomicBoolean countdownPlaying = new AtomicBoolean(false);
    private static volatile Thread countdownThread = null;
    private static volatile SourceDataLine activeCountdownLine = null;

    /**
     * Phát file countdown một lần cho câu hỏi có chỉ số questionIndex.
     * Tự động dừng countdown cũ nếu đang phát cho câu hỏi khác.
     * Gọi lại với cùng questionIndex sẽ không phát lại.
     *
     * @param questionIndex chỉ số câu hỏi hiện tại (0-based)
     */
    public static void playCountdown(int questionIndex) {
        if (sfxMuted) return;
        // Nếu đang phát cho câu hỏi này rồi thì không làm gì (chống phát lặp mỗi giây)
        if (countdownQuestionId == questionIndex && countdownPlaying.get()) return;

        // Dừng countdown cũ
        stopCountdown();

        countdownQuestionId = questionIndex;
        Thread t = new Thread(() -> {
            countdownPlaying.set(true);
            try {
                playCountdownInternal("countdown_3-0s.mp3");
            } finally {
                countdownPlaying.set(false);
                countdownQuestionId = -1;
                activeCountdownLine = null;
            }
        }, "QuizArena-Countdown");
        t.setDaemon(true);
        countdownThread = t;
        t.start();
    }

    /**
     * Dừng âm thanh countdown đang phát (nếu có).
     * Gọi khi câu hỏi kết thúc sớm, người chơi trả lời, hoặc chuyển câu hỏi.
     */
    public static void stopCountdown() {
        countdownQuestionId = -1;
        countdownPlaying.set(false);
        SourceDataLine line = activeCountdownLine;
        if (line != null) {
            try {
                line.stop();
                line.flush();
            } catch (Exception ignored) {}
        }
        Thread t = countdownThread;
        if (t != null && t.isAlive()) {
            t.interrupt();
        }
        countdownThread = null;
        activeCountdownLine = null;
    }

    /**
     * Phát stream countdown chuyên biệt, cho phép dừng tức thời qua activeCountdownLine.
     */
    private static boolean playCountdownInternal(String filename) {
        AudioInputStream rawStream = openAudioStream(filename);
        if (rawStream == null) return false;

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
            if (!AudioSystem.isLineSupported(info)) return false;

            try (SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
                 AudioInputStream finalStream = pcmStream) {

                line.open(targetFormat, 8192);
                applyGain(line, sfxVolume);
                line.start();
                activeCountdownLine = line;

                byte[] buf = new byte[8192];
                int bytesRead;
                while ((bytesRead = finalStream.read(buf)) != -1) {
                    if (Thread.currentThread().isInterrupted() || countdownQuestionId == -1) {
                        line.stop();
                        line.flush();
                        return false;
                    }
                    line.write(buf, 0, bytesRead);
                }
                line.drain();
                return true;
            } finally {
                activeCountdownLine = null;
            }
        } catch (Exception e) {
            LOG.warning("[SoundManager] Lỗi phát countdown: " + e.getMessage());
            return false;
        }
    }

    // ==========================================
    // CACHE & ENGINE PHÁT FILE HIỆU ỨNG (SFX)
    // ==========================================

    private static final class SoundData {
        final AudioFormat format;
        final byte[] pcmData;

        SoundData(AudioFormat format, byte[] pcmData) {
            this.format = format;
            this.pcmData = pcmData;
        }
    }

    /** Cache dữ liệu PCM đã decode của các hiệu ứng SFX ngắn */
    private static final ConcurrentHashMap<String, SoundData> audioCache = new ConcurrentHashMap<>();

    /**
     * Phát một file âm thanh từ cache hoặc nạp từ đĩa/classpath.
     * Tự động cache dữ liệu PCM để các lần phát tiếp theo không có độ trễ I/O hay decode.
     *
     * @return true nếu phát thành công, false nếu thất bại (để fallback)
     */
    static boolean playFile(String filename) {
        SoundData cached = audioCache.get(filename);
        if (cached == null) {
            cached = loadAndCacheAudio(filename);
        }
        if (cached != null) {
            return playCachedData(cached, filename);
        }
        return false;
    }

    private static SoundData loadAndCacheAudio(String filename) {
        AudioInputStream rawStream = openAudioStream(filename);
        if (rawStream == null) {
            return null;
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

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int r;
            while ((r = pcmStream.read(buf)) != -1) {
                baos.write(buf, 0, r);
            }
            byte[] pcmBytes = baos.toByteArray();
            SoundData data = new SoundData(targetFormat, pcmBytes);
            audioCache.put(filename, data);
            return data;
        } catch (Exception e) {
            LOG.warning("[SoundManager] Lỗi nạp và cache âm thanh " + filename + ": " + e.getMessage());
            return null;
        }
    }

    private static boolean playCachedData(SoundData data, String filename) {
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, data.format);
        if (!AudioSystem.isLineSupported(info)) {
            LOG.warning("[SoundManager] Định dạng không được hỗ trợ: " + filename);
            return false;
        }

        try (SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info)) {
            line.open(data.format, Math.min(data.pcmData.length, 16384));
            applyGain(line, sfxVolume);
            line.start();
            line.write(data.pcmData, 0, data.pcmData.length);
            line.drain();
            return true;
        } catch (Exception e) {
            LOG.warning("[SoundManager] Lỗi phát file " + filename + ": " + e.getMessage());
            return false;
        }
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

    /** Giải phóng toàn bộ tài nguyên khi đóng ứng dụng */
    public static void shutdown() {
        stopCountdown();
        audioCache.clear();
        try {
            sfxPool.shutdownNow();
        } catch (Exception ignored) {}
    }

    /**
     * Mở AudioInputStream từ file hoặc classpath:
     * 1. Thử file trực tiếp tại sound_effect/<filename>
     * 2. Thử với extension thay thế (.wav <-> .mp3)
     * 3. Thử alias countdown (nếu gọi "countdown")
     * 4. Thử nạp từ Classpath (/sounds/ hoặc /sound_effect/)
     */
    private static AudioInputStream openAudioStream(String filename) {
        // 1. Thử File hệ thống
        File f = resolveAudioFile(filename);
        if (f != null && f.exists()) {
            try {
                return AudioSystem.getAudioInputStream(f);
            } catch (Exception ignored) {}
        }

        // 2. Thử Classpath
        String[] cpPaths = {
            "/sound_effect/" + filename,
            "/sounds/" + filename,
            "/" + filename
        };
        for (String path : cpPaths) {
            URL url = SoundManager.class.getResource(path);
            if (url != null) {
                try {
                    return AudioSystem.getAudioInputStream(url);
                } catch (Exception ignored) {}
            }
        }

        LOG.warning("[SoundManager] Không tìm thấy file âm thanh: " + filename);
        return null;
    }

    /** Tìm file trên ổ đĩa */
    private static File resolveAudioFile(String filename) {
        File f = new File(SOUND_DIR + filename);
        if (f.exists()) return f;

        // Alias countdown
        if (filename.toLowerCase().contains("countdown")) {
            File c = new File(SOUND_DIR + "countdown_3-0s.mp3");
            if (c.exists()) return c;
        }

        // Thử alternate extension
        f = tryAlternateExtension(filename);
        if (f != null && f.exists()) return f;

        // Thử thêm đuôi .mp3 hoặc .wav nếu chưa có
        if (!filename.contains(".")) {
            File mp3 = new File(SOUND_DIR + filename + ".mp3");
            if (mp3.exists()) return mp3;
            File wav = new File(SOUND_DIR + filename + ".wav");
            if (wav.exists()) return wav;
        }

        return null;
    }

    /** Thử tìm file cùng tên nhưng đổi extension (wav<->mp3) */
    private static File tryAlternateExtension(String filename) {
        String alt;
        if (filename.endsWith(".mp3")) {
            alt = filename.substring(0, filename.length() - 4) + ".wav";
        } else if (filename.endsWith(".wav")) {
            alt = filename.substring(0, filename.length() - 4) + ".mp3";
        } else {
            return null;
        }
        File f = new File(SOUND_DIR + alt);
        return f.exists() ? f : null;
    }

    // ==========================================
    // PCM SYNTHESIS FALLBACK ENGINE
    // ==========================================
    private enum ToneShape {
        SMOOTH, DECAY, CLICK
    }

    private static final float SAMPLE_RATE = 44100f;

    private static void playTone(int frequency, int durationMs, double maxVolume, ToneShape shape) {
        try {
            int numSamples = (int) (SAMPLE_RATE * durationMs / 1000.0);
            byte[] buffer = new byte[numSamples];

            for (int i = 0; i < numSamples; i++) {
                double time = i / SAMPLE_RATE;
                double angle = 2.0 * Math.PI * frequency * time;
                double sample = Math.sin(angle);

                double envelope = 1.0;
                double progress = (double) i / numSamples;
                switch (shape) {
                    case DECAY -> envelope = 1.0 - progress;
                    case CLICK -> envelope = Math.exp(-progress * 8.0);
                    case SMOOTH -> {
                        if (progress < 0.1) envelope = progress / 0.1;
                        else if (progress > 0.8) envelope = (1.0 - progress) / 0.2;
                    }
                }

                sample *= envelope * maxVolume;
                buffer[i] = (byte) Math.max(-127, Math.min(127, (int) (sample * 127.0)));
            }

            AudioFormat format = new AudioFormat(SAMPLE_RATE, 8, 1, true, false);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

            if (!AudioSystem.isLineSupported(info)) return;

            try (SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info)) {
                line.open(format, buffer.length);
                line.start();
                line.write(buffer, 0, buffer.length);
                line.drain();
            }

        } catch (Exception ignored) {}
    }

    private static void playVictoryFallback() {
        int[] notes = {523, 659, 784, 1046}; // C5, E5, G5, C6
        int[] durations = {100, 100, 100, 300};
        for (int i = 0; i < notes.length; i++) {
            playTone(notes[i], durations[i], 0.5 * sfxVolume, ToneShape.SMOOTH);
            sleep(15);
        }
    }

    private static void playDefeatFallback() {
        int[] notes = {392, 370, 349, 311}; // G4, F#4, F4, Eb4
        int[] durations = {120, 120, 120, 280};
        for (int i = 0; i < notes.length; i++) {
            playTone(notes[i], durations[i], 0.45 * sfxVolume, ToneShape.DECAY);
            sleep(20);
        }
    }

    private static void playInviteAlertFallback() {
        playTone(659, 80, 0.45 * sfxVolume, ToneShape.SMOOTH);
        sleep(30);
        playTone(880, 120, 0.5 * sfxVolume, ToneShape.SMOOTH);
        sleep(40);
        playTone(1046, 180, 0.55 * sfxVolume, ToneShape.DECAY);
    }

    private static void playScorePointFallback() {
        playTone(523, 50, 0.4 * sfxVolume, ToneShape.SMOOTH);
        sleep(10);
        playTone(659, 70, 0.45 * sfxVolume, ToneShape.DECAY);
    }

    private static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private SoundManager() {}
}
