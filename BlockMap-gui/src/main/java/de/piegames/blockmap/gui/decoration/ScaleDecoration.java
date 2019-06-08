package de.piegames.blockmap.gui.decoration;

import java.util.Objects;

import de.piegames.blockmap.gui.CanvasHelper;
import de.piegames.blockmap.gui.DisplayViewport;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;

public class ScaleDecoration extends CanvasHelper {

	protected final DisplayViewport viewport;

	public ScaleDecoration(DisplayViewport viewport) {
		this.viewport = Objects.requireNonNull(viewport);
		viewport.frustumProperty.addListener(e -> repaint());
		visibleProperty().addListener(e -> repaint());
		setCache(true);
		setMouseTransparent(true);
	}

	@Override
	protected void render() {
		double[] scale1 = new double[] { 0, 0.1, 1, 5, 10, 50, 100, 500, 1000, 2000, 5000, 10000, 20000, 50000 };
		String[] text1 = new String[] { "", "10cm", "1m", "5m", "10m", "50m", "100m", "500m", "1km", "2km", "5km", "10km", "20km", "50km" };
		double[] scale2 = new double[] { 0, 1, 16, 512, 1024, 2048, 4096 };
		String[] text2 = new String[] { "", "1 block", "1 chunk", "1 region", "2 regions", "4 regions", "8 regions" };

		if (!isVisible())
			return;
		gc.clearRect(0, 0, getWidth(), getHeight());

		double scale = viewport.scaleProperty.get();
		gc.save();

		gc.setEffect(new GaussianBlur(20));
		// gc.setFill(Color.WHITE.deriveColor(0, 1, 1, 0.5));
		gc.setFill(new LinearGradient(0, 0.5, 1, 0.5, true, CycleMethod.NO_CYCLE, new Stop(0, Color.TRANSPARENT), new Stop(0.8, Color.grayRgb(255, 0.5))));
		gc.fillRect(getWidth() - 500, getHeight() - 130, 500 - 30, 130);
		gc.setEffect(null);
		gc.setFill(Color.BLACK);
		gc.setLineWidth(3);
		final double MAX_POS = Math.min(500, Math.max(getWidth() - 100, 0));

		DropShadow ds = new DropShadow();
		ds.setRadius(50);
		ds.setSpread(0.9);
		ds.setBlurType(BlurType.GAUSSIAN);
		ds.setColor(Color.WHITE);
		// Text t = new Text();
		// t.setEffect(ds);
		// t.setCache(true);
		// t.setX(10.0f);
		// t.setY(270.0f);
		// t.setFill(Color.RED);
		// t.setText("JavaFX drop shadow...");
		// t.setFont(Font.font(null, FontWeight.BOLD, 32));

		double max = 0;
		for (int i = 0; i < scale1.length; i++) {
			double pos = scale * scale1[i];
			if (pos < MAX_POS) {
				gc.strokeLine(getWidth() - 50 - pos, getHeight() - 50, getWidth() - 50 - pos, getHeight() - 40);
				if (pos > 8) {
					gc.save();
					gc.translate(getWidth() - 60 - pos, getHeight() - 30);
					gc.rotate(50);
					gc.fillText(text1[i], 0, 0);
					gc.restore();
				}
				max = Math.max(max, pos);
			}
		}
		for (int i = 0; i < scale2.length; i++) {
			double pos = scale * scale2[i];
			if (pos < MAX_POS) {
				gc.strokeLine(getWidth() - 50 - pos, getHeight() - 60, getWidth() - 50 - pos, getHeight() - 50);
				if (pos > 8) {
					gc.save();
					gc.translate(getWidth() - 60 - pos, getHeight() - 70);
					gc.rotate(-50);
					gc.fillText(text2[i], 0, 0);
					gc.restore();
				}
				max = Math.max(max, pos);
			}
		}

		gc.strokeLine(getWidth() - 50, getHeight() - 50, getWidth() - 50 - max, getHeight() - 50);
		gc.restore();
	}
}
