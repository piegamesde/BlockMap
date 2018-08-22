package togos.minecraft.maprend.gui.decoration;

import java.util.Objects;
import org.joml.Rectangled;
import org.joml.Vector2d;
import org.joml.Vector2dc;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;
import togos.minecraft.maprend.gui.CanvasHelper;
import togos.minecraft.maprend.gui.DisplayViewport;

public class SelectRectangleDecoration extends CanvasHelper {

	protected final DisplayViewport	frustum;

	protected final Vector2d		dragStart	= new Vector2d(), dragEnd = new Vector2d();

	public SelectRectangleDecoration(DisplayViewport frustum, Rectangled initialSelection) {
		this(frustum, MouseButton.PRIMARY, initialSelection);
	}

	public SelectRectangleDecoration(DisplayViewport frustum, MouseButton selectButton, Rectangled initialSelection) {
		this.frustum = Objects.requireNonNull(frustum);
		Objects.requireNonNull(selectButton);
		frustum.frustumProperty.addListener(e -> repaint());

		if (initialSelection == null)
			initialSelection = new Rectangled();
		dragStart.set(initialSelection.minX, initialSelection.minY);
		dragEnd.set(initialSelection.maxX, initialSelection.maxY);

		setOnMousePressed(e -> {
			if (e.getButton() == selectButton) {
				dragStart.set(frustum.getMouseInWorld());
				dragEnd.set(dragStart);
				repaint();
			}
		});
		setOnMouseDragged(e -> {
			if (e.getButton() == selectButton)
				dragEnd.set(frustum.getMouseInWorld());
			repaint();
		});
		gc.setLineWidth(2);
		gc.setFill(Color.BLUE.deriveColor(-35, .6, 1.3, 0.5));
		gc.setStroke(Color.BLUE.deriveColor(0, 0.8, 1.3, 0.8));
	}

	@Override
	public void render() {
		gc.clearRect(0, 0, getWidth(), getHeight());

		double scale = frustum.scaleProperty.get();
		gc.save();
		gc.scale(scale, scale);
		Vector2dc translation = frustum.getTranslation();
		gc.translate(translation.x(), translation.y());

		gc.fillRect(Math.min(dragStart.x, dragEnd.x), Math.min(dragStart.y, dragEnd.y), Math.abs(dragStart.x - dragEnd.x), Math.abs(dragStart.y -
				dragEnd.y));
		gc.strokeRect(Math.min(dragStart.x, dragEnd.x), Math.min(dragStart.y, dragEnd.y), Math.abs(dragStart.x - dragEnd.x), Math.abs(dragStart.y -
				dragEnd.y));

		gc.restore();
	}

	public Rectangled getSelected() {
		return new Rectangled(Math.min(dragStart.x, dragEnd.x), Math.min(dragStart.y, dragEnd.y), Math.max(dragStart.x, dragEnd.x), Math.max(dragStart.y, dragEnd.y));
	}
}