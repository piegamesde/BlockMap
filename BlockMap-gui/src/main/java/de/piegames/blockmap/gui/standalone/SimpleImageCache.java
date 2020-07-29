package de.piegames.blockmap.gui.standalone;

import io.github.soc.directories.ProjectDirectories;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;

/**
 * A simple image cache implementation, which will save fetched images to disk and load them from disk when possible.
 *
 * @author saibotk
 */
public class SimpleImageCache extends FileCache {
    private static final Log log = LogFactory.getLog(SimpleImageCache.class);

    private static final long MAX_CACHE_TIME = 60000L;

    /**
     * Constructs the cache instance.
     */
    public SimpleImageCache() {
        super(Paths.get(ProjectDirectories.from("de", "piegames", "blockmap").cacheDir + "/images"));
    }

    /**
     * Gets an image from the cache if possible and will otherwise fetch the image using {@link #fetch(String)}.
     * This will also evict a file from the cache, if it is not fresh enough (deletes the file). It will also save
     * the file to the disk, if the file has been requested / fetched from the URL.
     *
     * @param url The URL of the Image to fetch.
     *
     * @return The requested image
     */
    public Image get(String url) {
        String fileName = getFileName(url);

        try {
            Path cachedFile = cacheDir.resolve(fileName);
            boolean exists = Files.exists(cachedFile);

            if (exists) {
                if (isFresh(cachedFile)) {
                    log.info("Loading image from cache: " + fileName);

                    return new Image(Files.newInputStream(cachedFile));
                } else {
                    log.info("Image will be removed from cache: " + fileName);

                    removeFile(cachedFile);
                }
            }
        } catch (IOException exception) {
            log.info("Image could not be fetched from cache...");
        }

        log.info("Fetching image from source instead: " + url);

        // retrieve the image from the URL
        Image image = fetch(url);

        // Save the image, if it is not an error
        if(!image.isError())
            writeToCache(image, fileName);

        return image;
    }

    /**
     *
     * Fetches an Image from an URL and returns the image.
     *
     * @param url The url that is used to fetch the object from.
     *
     * @return The requested image object.
     *
     * @throws NullPointerException if URL is null
     * @throws IllegalArgumentException if URL is invalid or unsupported
     */
    public Image fetch(String url) {
        String fileName = getFileName(url);

        Image result = new Image(url, 0, 0, true, false);

        log.info("Fetched image: " + fileName);

        return result;
    }

    @Override
    protected boolean isFresh(Path path) {
        try {
            return Instant.now().toEpochMilli() - Files.getLastModifiedTime(path).toMillis() <= MAX_CACHE_TIME;
        } catch (IOException e) {
            log.error("Could not access last modified date on file: " + path.getFileName(), e);

            return false;
        }
    }

    protected void writeToCache(Image image, String fileName) {
        BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);

        try {
            ImageIO.write(bufferedImage, "png", Files.newOutputStream(cacheDir.resolve(fileName)));

            log.info("Wrote file to image cache: " + fileName);
        } catch (IOException e) {
            log.error("Could not write to file for image cache: " + fileName, e);
        }
    }

    protected String getFileName(String url) {
        Path location = Paths.get(url);

        return location.getFileName().toString() + ".png";
    }

}
