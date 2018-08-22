package togos.minecraft.maprend.gui;

import javafx.scene.canvas.Canvas;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;

/**
 * <p>
 * Wraps a {@link Canvas} into an AnchorPane and a BorderPane. This is because a Canvas needs to have a preferred size set to properly work in many layouts.
 * This wrapper will make sure that the canvas will always use exactly as much of the space available - no less and no more. It is recommended to use this class
 * in most layout situations when working with canvases.
 * </p>
 */
public class CanvasContainer extends AnchorPane {

	public CanvasContainer(Canvas canvas) {
		BorderPane wrapper = new BorderPane(canvas);
		wrapper.setPickOnBounds(true);
		canvas.widthProperty().bind(wrapper.widthProperty());
		canvas.heightProperty().bind(wrapper.heightProperty());
		AnchorPane.setTopAnchor(wrapper, 0D);
		AnchorPane.setBottomAnchor(wrapper, 0D);
		AnchorPane.setLeftAnchor(wrapper, 0D);
		AnchorPane.setRightAnchor(wrapper, 0D);
		getChildren().add(wrapper);
	}
}