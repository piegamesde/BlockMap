package togos.minecraft.maprend.gui.decoration;

import org.joml.Vector2d;
import org.joml.Vector2dc;

import com.sun.glass.ui.Application;
import com.sun.glass.ui.Robot;

import javafx.scene.input.MouseButton;
import javafx.scene.layout.Region;
import togos.minecraft.maprend.gui.DisplayViewport;

/**
 * This decoration provides basic drag and zoom support, where you can set the button used for dragging as well as zoom speed and direction.
 * Both functionalities are optional and can be disabled separately. If you disable both, this class will still forward mouse moved events
 * to the view frustum. This might be useful if you use manual zooming logic externally and still want to zoom around the mouse center as it
 * does normally.
 */
@SuppressWarnings("restriction")
public class DragScrollDecoration extends Region {

	protected Robot robot = Application.GetApplication().createRobot();
	protected int cooldown;

	/** Creates an instance of this class that will drag with the right mouse button and a scroll factor of 1/10. */
	public DragScrollDecoration(DisplayViewport frustum) {
		this(frustum, MouseButton.SECONDARY, 0.01d);
	}

	public DragScrollDecoration(DisplayViewport frustum, boolean allowDrag, boolean allowZoom) {
		this(frustum, allowDrag ? MouseButton.SECONDARY : null, allowZoom ? 0.01d : 0);
	}

	/**
	 * @param dragButton
	 *            The button that must be pressed to activate dragging. <code>null</code> will disable dragging.
	 * @param scrollFactor
	 *            Higher values will increase the zoomed amount per scroll. Zero deactivates scrolling. Negative values invert the scroll
	 *            direction.
	 */
	public DragScrollDecoration(DisplayViewport frustum, MouseButton dragButton, double scrollFactor) {
		setOnMouseMoved(e -> frustum.mousePosProperty.set(new Vector2d(e.getX(), e.getY())));
		setOnMouseDragged(e -> {
			Vector2dc old = frustum.mousePosProperty.get();
			Vector2d current = new Vector2d(e.getX(), e.getY());
			if (cooldown <= 0) {
				if (dragButton != null && e.getButton() == dragButton) {
					frustum.pan(current.x() - old.x(), current.y() - old.y());
					double width = getWidth();
					double height = getHeight();
					double dx = 0, dy = 0;
					if (current.x() < 1)
						dx = width - 3;
					if (current.x() > width - 2)
						dx = -width + 4;
					if (current.y() < 1)
						dy = height - 3;
					if (current.y() > height - 2)
						dy = -height + 4;
					if (dx != 0 || dy != 0) {
						current.add(dx, dy);
						robot.mouseMove((int) (e.getScreenX() + dx), (int) (e.getScreenY() + dy));
						cooldown = 3;
					}
				}
			} else if (cooldown > 0)
				cooldown--;
			frustum.mousePosProperty.set(current);
		});

		if (scrollFactor != 0)
			setOnScroll(e -> frustum.mouseScroll(e.getDeltaY() * scrollFactor));
	}
}