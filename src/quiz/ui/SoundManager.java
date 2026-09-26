package quiz.ui;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.sound.sampled.*;

/**
 * SoundManager - Quản lý và phát hiệu ứng âm thanh thời gian thực (Audio Effects) cho Quiz Arena:
 * - Tự động tổng hợp âm thanh đa tầng (PCM Audio Synthesis) bằng Java Sound API chuẩn.
 * - Hoạt động độc lập 100%, không phụ thuộc file .wav bên ngoài, không lo lỗi thiếu file hay sai đường dẫn.
 * - Chạy bất đồng bộ qua Background Thread Pool, không bao giờ gây giật lag giao diện Swing (EDT).
 * - Hỗ trợ bật/tắt (Mute) âm thanh toàn hệ thống.
 */
public final class SoundManager {

    private static volatile boolean muted = false;
    private static final ExecutorService soundPool = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "QuizArena-AudioThread");
        t.setDaemon(true);
        return t;
    });

    private static final float SAMPLE_RATE = 44100f;

    public static boolean isMuted() {
        return muted;
    }

    public static void setMuted(boolean isMuted) {
        muted = isMuted;
    }

    public static void toggleMute() {
        muted = !muted;
    }

    /**
     * Tiếng click nút bấm nhẹ nhàng (Haptic feedback)
     */
    public static void playClick() {
        if (muted) return;
        soundPool.submit(() -> playTone(750, 35, 0.25, ToneShape.DECAY));
    }

    /**
     * Tiếng tích tắc đồng hồ đếm ngược (Tick) khi còn <= 10 giây
     */
    public static void playTick() {
        if (muted) return;
        soundPool.submit(() -> playTone(1100, 30, 0.35, ToneShape.CLICK));
    }

    /**
     * Âm thanh thông báo khi có lời mời thách đấu mới (Invite chime)
     */
    public static void playInviteAlert() {
        if (muted) return;
        soundPool.submit(() -> {
            playTone(587, 100, 0.4, ToneShape.SMOOTH); // D5
            sleep(40);
            playTone(880, 180, 0.45, ToneShape.SMOOTH); // A5
        });
    }

    /**
     * Âm thanh chiến thắng hoành tráng (Fanfare)
     */
    public static void playVictory() {
        if (muted) return;
        soundPool.submit(() -> {
            playTone(523, 120, 0.45, ToneShape.SMOOTH); // C5
            sleep(20);
            playTone(659, 120, 0.45, ToneShape.SMOOTH); // E5
            sleep(20);
            playTone(784, 120, 0.5, ToneShape.SMOOTH);  // G5
            sleep(20);
            playTone(1046, 320, 0.6, ToneShape.DECAY);  // C6
        });
    }

    /**
     * Âm thanh thất bại nhẹ nhàng (Defeat)
     */
    public static void playDefeat() {
        if (muted) return;
        soundPool.submit(() -> {
            playTone(493, 140, 0.45, ToneShape.SMOOTH); // B4
            sleep(30);
            playTone(415, 140, 0.45, ToneShape.SMOOTH); // G#4
            sleep(30);
            playTone(329, 280, 0.5, ToneShape.DECAY);   // E4
        });
    }

    /**
     * Âm thanh khi gửi/nhận reaction emote (Pop)
     */
    public static void playEmote() {
        if (muted) return;
        soundPool.submit(() -> {
            playTone(600, 40, 0.35, ToneShape.SMOOTH);
            playTone(950, 60, 0.4, ToneShape.DECAY);
        });
    }

    // ==========================================
    // PCM SYNTHESIS ENGINE
    // ==========================================
    private enum ToneShape {
        SMOOTH, DECAY, CLICK
    }

    private static void playTone(int frequency, int durationMs, double maxVolume, ToneShape shape) {
        try {
            int numSamples = (int) (SAMPLE_RATE * durationMs / 1000.0);
            byte[] buffer = new byte[numSamples];

            for (int i = 0; i < numSamples; i++) {
                double time = i / SAMPLE_RATE;
                double angle = 2.0 * Math.PI * frequency * time;
                double sample = Math.sin(angle);

                // Envelope calculation
                double envelope = 1.0;
                double progress = (double) i / numSamples;
                switch (shape) {
                    case DECAY -> envelope = 1.0 - progress;
                    case CLICK -> envelope = Math.exp(-progress * 8.0);
                    case SMOOTH -> {
                        if (progress < 0.15) {
                            envelope = progress / 0.15;
                        } else if (progress > 0.8) {
                            envelope = (1.0 - progress) / 0.2;
                        }
                    }
                }

                double val = sample * envelope * maxVolume;
                buffer[i] = (byte) (Math.max(-1.0, Math.min(1.0, val)) * 127);
            }

            AudioFormat format = new AudioFormat(SAMPLE_RATE, 8, 1, true, false);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

            if (!AudioSystem.isLineSupported(info)) {
                return;
            }

            try (SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info)) {
                line.open(format);
                line.start();
                line.write(buffer, 0, buffer.length);
                line.drain();
            }
        } catch (Exception ignored) {
            // Không làm gián đoạn ứng dụng nếu môi trường không có thiết bị âm thanh
        }
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {}
    }

    private SoundManager() {}
}
