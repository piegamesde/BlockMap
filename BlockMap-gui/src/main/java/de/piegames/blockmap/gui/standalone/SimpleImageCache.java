package de.piegames.blockmap.gui.standalone;

import io.github.soc.directories.ProjectDirectories;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.Arrays;

public class SimpleImageCache implements IImageCache {
    private static final Log log = LogFactory.getLog(SimpleImageCache.class);
    private final ProjectDirectories directories = ProjectDirectories.from("de", "piegames", "blockmap");
    private final Path cacheDir	= Paths.get(directories.cacheDir + "/images");

    private static final long MAX_CACHE_TIME = 60000L;

    public SimpleImageCache() {
        try {
            Files.createDirectories(cacheDir);
        } catch (IOException exception) {
            log.warn("Could not create image cache directory!", exception);
        }

        purge();
    }

    @Override
    public Image get(String url) {
        String fileName = getFileName(url);

        try {
            File cachedFile = cacheDir.resolve(fileName).toFile();
            if (Instant.now().toEpochMilli() - cachedFile.lastModified() <= MAX_CACHE_TIME) {
                BufferedImage bufferedImage = ImageIO.read(cachedFile);
                log.info("Loaded image from cache: " + fileName);

                return SwingFXUtils.toFXImage(bufferedImage, null);
            } else {
                log.info("Image will be removed from cache: " + fileName);
                cachedFile.delete();
            }
        } catch (IOException exception) {
            log.info("Image could not be fetched from cache...");
        }

        log.info("Fetching image from source instead: " + url);

        return fresh(url);
    }

    /**
     *
     * @param url
     *
     * @return
     *
     * @throws NullPointerException if URL is null
     * @throws IllegalArgumentException if URL is invalid or unsupported
     */
    @Override
    public Image fresh(String url) {
        String fileName = getFileName(url);

        Image result = new Image(url, 0, 0, true, false);
        log.info("Fetched new image: " + fileName);

        if (!result.isError()) {
            writeToCache(result, fileName);
        }

        return result;
    }

    @Override
    public void flush() {
        File directory = cacheDir.toFile();
        File[] cachedFiles = directory.listFiles();
        if (cachedFiles != null)
            Arrays.stream(cachedFiles).forEach(File::delete);

        log.info("Flushed cache...");
    }

    @Override
    public void purge() {
        File directory = cacheDir.toFile();
        File[] cachedFiles = directory.listFiles();

        if (cachedFiles != null)
            Arrays.stream(cachedFiles).filter(x -> Instant.now().toEpochMilli() - x.lastModified() > MAX_CACHE_TIME)
                    .forEach(File::delete);

        log.info("Purged cache...");
    }

    private void writeToCache(Image image, String fileName) {
        BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);

        try {
            ImageIO.write(bufferedImage, "png", cacheDir.resolve(fileName).toFile());
            log.info("Wrote file to image cache: " + fileName);
        } catch (IOException exception) {
            log.error("Could not write to file for image cache: " + fileName);
        }
    }

    private String getFileName(String url) {
        Path location = Paths.get(url);

        return location.getFileName().toString() + ".png";
    }

}
