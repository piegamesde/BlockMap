package de.piegames.blockmap.gui.decoration;

import java.util.Objects;

import org.joml.AABBd;
import org.joml.Vector2dc;

import de.piegames.blockmap.gui.CanvasHelper;
import de.piegames.blockmap.gui.DisplayViewport;
import javafx.scene.paint.Color;

public class GridDecoration extends CanvasHelper {

	protected final DisplayViewport viewport;

	public GridDecoration(DisplayViewport viewport) {
		this.viewport = Objects.requireNonNull(viewport);
		viewport.frustumProperty.addListener(e -> repaint());
		visibleProperty().addListener(e -> repaint());
	}

	@Override
	protected void render() {
		if (!isVisible())
			return;
		gc.clearRect(0, 0, getWidth(), getHeight());

		double scale = viewport.scaleProperty.get();
		gc.save();
		gc.scale(scale, scale);
		Vector2dc translation = viewport.getTranslation();
		gc.translate(translation.x(), translation.y());

		AABBd frustum = viewport.frustumProperty.get();

		if (scale >= 1.5 * 0.9) {
			double w = Math.min(2 / scale, 0.5);
			double a = 1 - Math.min(1.5 / scale, 1);
			gc.setStroke(new Color(0.4f, 0.4f, 0.4f, 0.6f * a));
			gc.setLineWidth(w);
			for (int x = (int) frustum.minX - 1; x <= frustum.maxX; x++)
				gc.strokeLine(x, frustum.minY, x, frustum.maxY);
			for (int y = (int) frustum.minY - 1; y <= frustum.maxY; y++)
				gc.strokeLine(frustum.minX, y, frustum.maxX, y);
		}

		if (scale > 0.2 * 0.9) {
			double w = Math.min(3 / scale, 3);
			double a = 1 - Math.min(0.2 / scale, 1);
			gc.setStroke(new Color(0.3f, 0.3f, 0.3f, 0.6f * a));
			gc.setLineWidth(w);
			for (int x = (int) frustum.minX & 0xFFFFFFF0; x <= frustum.maxX; x += 16)
				gc.strokeLine(x, frustum.minY, x, frustum.maxY);
			for (int y = (int) frustum.minY & 0xFFFFFFF0; y <= frustum.maxY; y += 16)
				gc.strokeLine(frustum.minX, y, frustum.maxX, y);
		}

		{
			double w = Math.min(5 / scale, 30);
			gc.setStroke(new Color(0.2f, 0.2f, 0.2f, 0.6f));
			gc.setLineWidth(w);
			for (int x = (int) frustum.minX & 0xFFFFFE00; x <= frustum.maxX; x += 512)
				for (int y = (int) frustum.minY & 0xFFFFFE00; y <= frustum.maxY; y += 512) {
					gc.strokeRect(x, y, 512, 512);
				}
		}

		gc.restore();
	}
}
