package togos.minecraft.maprend.gui;

import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;

public abstract class CanvasHelper extends CanvasContainer {

	protected Canvas			canvas;
	protected GraphicsContext	gc;

	public CanvasHelper() {
		this(null);
	}

	/* A little hack to pass arguments in super() and also store them as field. Java is weird sometimes */
	private CanvasHelper(Canvas canvas) {
		super(canvas = new Canvas());
		this.canvas = canvas;
		gc = canvas.getGraphicsContext2D();
	}

	public final void repaint() {
		if (Platform.isFxApplicationThread())
			render();
		else
			Platform.runLater(this::render);
	}

	protected abstract void render();
}