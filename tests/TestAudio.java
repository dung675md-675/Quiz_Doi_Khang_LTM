package tests;

import java.io.File;
import javax.sound.sampled.*;

public class TestAudio {
    public static void main(String[] args) {
        String dir = "sound_effect/";
        File folder = new File(dir);
        File[] files = folder.listFiles();
        if (files == null) {
            System.out.println("Thư mục sound_effect không tồn tại!");
            return;
        }

        System.out.println("=== KIỂM TRA ĐỊNH DẠNG VÀ THỜI LƯỢNG FILE ÂM THANH ===");
        for (File f : files) {
            if (f.isFile() && (f.getName().endsWith(".wav") || f.getName().endsWith(".mp3"))) {
                try (AudioInputStream ais = AudioSystem.getAudioInputStream(f)) {
                    AudioFormat baseFormat = ais.getFormat();
                    AudioFormat targetFormat = new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        baseFormat.getSampleRate(),
                        16,
                        baseFormat.getChannels(),
                        baseFormat.getChannels() * 2,
                        baseFormat.getSampleRate(),
                        false
                    );
                    AudioInputStream pcmStream = AudioSystem.isConversionSupported(targetFormat, baseFormat)
                        ? AudioSystem.getAudioInputStream(targetFormat, ais)
                        : ais;

                    long totalBytes = 0;
                    byte[] buf = new byte[8192];
                    int r;
                    while ((r = pcmStream.read(buf)) != -1) {
                        totalBytes += r;
                    }
                    float sampleRate = (pcmStream.getFormat().getSampleRate() > 0) ? pcmStream.getFormat().getSampleRate() : 44100f;
                    int frameSize = (pcmStream.getFormat().getFrameSize() > 0) ? pcmStream.getFormat().getFrameSize() : 4;
                    double durationSec = (double) totalBytes / (sampleRate * frameSize);

                    System.out.printf("File: %-22s | PCM Rate: %-7.1f | Channels: %d | Duration: %.3f s\n",
                        f.getName(), sampleRate, pcmStream.getFormat().getChannels(), durationSec);
                } catch (Exception e) {
                    System.out.printf("File: %-22s | ERROR: %s\n", f.getName(), e.getMessage());
                }
            }
        }
    }
}
