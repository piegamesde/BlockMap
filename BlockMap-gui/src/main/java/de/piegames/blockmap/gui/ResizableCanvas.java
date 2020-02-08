package de.piegames.blockmap.gui;

import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;

public abstract class ResizableCanvas extends Canvas {

	protected GraphicsContext	gc;

	public ResizableCanvas() {
		gc = getGraphicsContext2D();
	}

	/** Queues in a repaint event calling {@link render} from the JavaFX Application Thread */
	public final void repaint() {
		if (Platform.isFxApplicationThread())
			render();
		else
			Platform.runLater(this::render);
	}

	/** Requires to be called from the JavaFX Application Thread. */
	protected abstract void render();

	@Override
	public boolean isResizable() {
		return true;
	}

	@Override
	public double maxHeight(double width) {
		return Double.POSITIVE_INFINITY;
	}

	@Override
	public double maxWidth(double height) {
		return Double.POSITIVE_INFINITY;
	}

	@Override
	public double minWidth(double height) {
		return 0;
	}

	@Override
	public double minHeight(double width) {
		return 0;
	}

	@Override
	public void resize(double width, double height) {
		this.setWidth(width);
		this.setHeight(height);
	}
}