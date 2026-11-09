package quiz.ui;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.imageio.ImageIO;

/** Loads the two scene images once, away from the Swing event thread. */
final class ArenaArtwork {
    private static final Map<ArenaBackgroundPanel.Scene, CompletableFuture<BufferedImage>> CACHE =
            new ConcurrentHashMap<>();
    private static final ExecutorService LOADER = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "QuizArena-Artwork");
        thread.setDaemon(true);
        return thread;
    });

    static CompletableFuture<BufferedImage> load(ArenaBackgroundPanel.Scene scene) {
        return CACHE.computeIfAbsent(scene,
                key -> CompletableFuture.supplyAsync(() -> read(key), LOADER));
    }

    private static BufferedImage read(ArenaBackgroundPanel.Scene scene) {
        String filename = scene == ArenaBackgroundPanel.Scene.AUTH
                ? "auth-arena-v1.png" : "lobby-arena-v1.png";
        Set<Path> candidates = new LinkedHashSet<>();

        String configured = System.getProperty("quiz.assetsDir", "").trim();
        if (!configured.isEmpty()) {
            try {
                Path directory = Path.of(configured).toAbsolutePath().normalize();
                candidates.add(directory.resolve(filename));
                candidates.add(directory.resolve("backgrounds").resolve(filename));
            } catch (RuntimeException ignored) {
                // A bad optional path must not prevent the built-in fallback from rendering.
            }
        }

        BufferedImage override = readCandidates(candidates);
        if (override != null) {
            return override;
        }

        try (InputStream stream = ArenaArtwork.class.getResourceAsStream(
                "/assets/backgrounds/" + filename)) {
            if (stream != null) {
                BufferedImage packaged = ImageIO.read(stream);
                if (packaged != null) {
                    return packaged;
                }
            }
        } catch (IOException | RuntimeException ignored) {
            // Manual javac launches normally use the project asset directory instead.
        }

        candidates.clear();
        addProjectCandidates(candidates, Path.of("").toAbsolutePath().normalize(), filename);
        try {
            Path location = Path.of(ArenaArtwork.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
            if (!Files.isDirectory(location)) {
                location = location.getParent();
            }
            addProjectCandidates(candidates, location, filename);
        } catch (URISyntaxException | RuntimeException ignored) {
            // A restricted runtime can still use classpath images or the gradient fallback.
        }
        return readCandidates(candidates);
    }

    private static void addProjectCandidates(Set<Path> candidates, Path directory, String filename) {
        for (int level = 0; directory != null && level < 5; level++) {
            candidates.add(directory.resolve("assets").resolve("backgrounds").resolve(filename));
            directory = directory.getParent();
        }
    }

    private static BufferedImage readCandidates(Set<Path> candidates) {
        for (Path candidate : candidates) {
            if (!Files.isRegularFile(candidate)) {
                continue;
            }
            try (InputStream stream = Files.newInputStream(candidate)) {
                BufferedImage image = ImageIO.read(stream);
                if (image != null) {
                    return image;
                }
            } catch (IOException | RuntimeException ignored) {
                // Try another location; the UI remains usable when artwork is unavailable.
            }
        }
        return null;
    }

    private ArenaArtwork() {}
}
