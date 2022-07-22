package de.piegames.blockmap.gui;

import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.joml.AABBd;

import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

/**
 * Handles the creation of high-resolution screenshots of maps.
 */
public class ScreenshotHandler {

    /**
     * Take a high-resolution screenshot of a section of them map
     * @param map Map to use.
     * @param frustum Bounds of the image.
     * @return The captured image.
     */
    public static WritableImage takeScreenshot(RenderedMap map, AABBd frustum) {
        double width = frustum.maxX - frustum.minX;
        double height = frustum.maxY - frustum.minY;

        Canvas canvas = new Canvas(width, height);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.translate(-frustum.minX, -frustum.minY);

        map.draw(gc, 0, frustum, 1);

        SnapshotParameters parameters = new SnapshotParameters();
        parameters.setFill(Color.TRANSPARENT);

        return canvas.snapshot(parameters, null);
    }
    
    /**
     * Save a JavaFX image out to a file in PNG format.
     * 
     * @param image Image to save.
     * @param file  File to save to.
     * @throws IOException If an IO exception occurs while writing the file.
     */
    public static void saveImage(Image image, File file) throws IOException {
        ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
    }

    public static Task<Void> saveImageTask(Image image, File file) {
        Task<Void> task = new Task<Void>() {

            @Override
            protected Void call() throws Exception {
                saveImage(image, file);
                return null;
            }
            
        };
        return task;
    }
    
}
