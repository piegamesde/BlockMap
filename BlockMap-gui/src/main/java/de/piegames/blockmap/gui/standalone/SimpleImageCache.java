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

public class SimpleImageCache implements IImageCache {
    private static Log log = LogFactory.getLog(SimpleImageCache.class);
    private final ProjectDirectories directories = ProjectDirectories.from("de", "piegames", "blockmap");
    private final Path cacheDir	= Paths.get(directories.cacheDir + "/images");

    public SimpleImageCache() {
        try {
            Files.createDirectories(cacheDir);
        } catch (IOException exception) {
            log.warn("Could not create image cache directory!", exception);
        }
    }

    @Override
    public Image get(String url) {
        String fileName = getFileName(url);

        BufferedImage bufferedImage = null;

        try {
            bufferedImage = ImageIO.read(cacheDir.resolve(fileName).toFile());
            log.info("Loaded image from cache: " + fileName);
        } catch (IOException exception) {
            log.info("Image could not be fetched from cache...");
            log.error("Ex: ", exception);
            return fresh(url);
        }

        return SwingFXUtils.toFXImage(bufferedImage, null);
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

        Image result = new Image(url);
        log.info("Fetched new image: " + fileName);

        if (!result.isError()) {
            writeToCache(result, fileName);
        }

        return result;
    }

    @Override
    public void flush() {

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
