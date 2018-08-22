package togos.minecraft.maprend.gui;

import org.joml.AABBd;
import org.joml.Vector2d;
import org.joml.Vector2dc;
import org.joml.Vector3d;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.util.Duration;


/**
 * An object of this class represents the camera viewport of a {@link WorldRendererCanvas}. All units are in world coordinates by default, meaning one unit is
 * equal to one block.<br/>
 * <b>Warning:</b> The properties used here may use mutable objects. It is strongly discouraged to modify their inner state; treat them as immutable. The system
 * does not check whether the values are mutable or not. Note that checking is important when reading as well as when writing the respecting property. Modifying
 * objects will result in wrong calculations of the viewport and may lead to undefined behaviour on third-party components using this.
 */
public class DisplayViewport {

	// up to 9 without rounding errors
	public static final int							MAX_ZOOM_LEVEL		= 7;

	/** The size of the viewport is needed to calculate the frustum's size in world coordinates. */
	public final DoubleProperty						widthProperty		= new SimpleDoubleProperty(800);
	/** The size of the viewport is needed to calculate the frustum's size in world coordinates. */
	public final DoubleProperty						heightProperty		= new SimpleDoubleProperty(500);

	/**
	 * A linear scaling factor to zoom in or out on the map. Larger values mean zoom in, smaller values mean zoom out. Avoid changing this property directly
	 * because it leads to inconsistency between scale and zoom. Use {@link #zoomProperty} instead. <br/>
	 * A change in this value will scale the map to the new scaling value accordingly. The center of the scaling operation will be the mouse's current position.
	 */
	public final DoubleProperty						scaleProperty		= new SimpleDoubleProperty(1);
	/**
	 * The zoom is unlike the scale not linear, but on an exponential basis. It is defined as <code>scale=Math.pow(2, zoom)</code>. A zoom of zero means a scale
	 * of 1:1, each unit increase means zooming out by a factor of 2, a each unit decrease results in zooming in by a factor of 2.
	 */
	public final DoubleProperty						zoomProperty		= new SimpleDoubleProperty(0);

	/** The current mouse position in local coordinates. Used for calculations when zooming around that point. */
	public final ObjectProperty<Vector2dc>			mousePosProperty	= new SimpleObjectProperty<>(new Vector2d());
	private final ReadOnlyObjectWrapper<Vector2dc>	mouseWorld			= new ReadOnlyObjectWrapper<>(new Vector2d());
	/** The current mouse position in world coordinates (1 unit == 1 block). */
	public final ReadOnlyObjectProperty<Vector2dc>	mouseWorldProperty	= mouseWorld.getReadOnlyProperty();
	/** The translation of the map relative to its origin, in blocks/pixels. */
	public final ObjectProperty<Vector2dc>			translationProperty	= new SimpleObjectProperty<>(new Vector2d());
	private final ReadOnlyObjectWrapper<AABBd>		frustum				= new ReadOnlyObjectWrapper<>(new AABBd());
	/**
	 * The camera frustum of this viewport. It represents the area of the world visible to the current camera. Translating, zooming or resizing the map result
	 * in an update of this object.
	 */
	public final ReadOnlyObjectProperty<AABBd>		frustumProperty		= frustum.getReadOnlyProperty();
	protected Timeline								zoomTimeline;

	public DisplayViewport() {
		scaleProperty.bind(Bindings.createDoubleBinding(() -> Math.pow(2, zoomProperty.get()), zoomProperty));
		scaleProperty.addListener((e, oldScale, newScale) -> {
			// In world coordinates
			Vector2d cursorPos = new Vector2d(mousePosProperty.get()).mul(1d / oldScale.doubleValue()).sub(translationProperty.get());
			Vector2d translation = new Vector2d(this.translationProperty.get());
			translation.add(cursorPos);
			translation.mul(oldScale.doubleValue() / newScale.doubleValue());
			translation.sub(cursorPos);
			this.translationProperty.set(translation);
		});

		// Use bindings because they are executed lazily
		frustum.bind(Bindings.createObjectBinding(() -> {
			Vector2dc translation = this.translationProperty.get();
			return new AABBd(// TODO optimise
					new Vector3d(-translation.x(), -translation.y(), 0),
					new Vector3d(widthProperty.get(), heightProperty.get(), 0).div(scaleProperty.get()).sub(translation.x(), translation.y(), 0));
		}, translationProperty, widthProperty, heightProperty));
		mouseWorld.bind(Bindings.createObjectBinding(() -> mousePosProperty.get().mul(1d / scaleProperty.get(), new Vector2d()).sub(translationProperty.get()), mousePosProperty, scaleProperty));
	}

	/**
	 * Offsets the view of the map by the given amount of pixels in the respective direction.
	 *
	 * @param dx how many pixels the map should move horizontally
	 * @param dy how many pixels the map should move vertically
	 */
	public void pan(double dx, double dy) {
		double scale = scaleProperty.get();
		translationProperty.set(new Vector2d(dx / scale, dy / scale).add(translationProperty.get()));
	}

	/**
	 * Zooms in or out around the position currently shown by the mouse cursor. The zooming will be animated with an eased interpolation for 100 milliseconds.
	 * The resulting scale is clamped to <code>[-MAX_ZOOM_LEVEL, MAX_ZOOM_LEVEL]</code>. If called while the animation from a previous call has not finished,
	 * that animation is stopped in its current state and both are combined to a new animation that is slightly faster than executing both animations
	 * sequentially.
	 *
	 * @param deltaZoom The amount of change to the zoom property. -1 means zoom out by a factor of 2, 1 means zoom in by a factor of 2.
	 * @see #zoomProperty
	 */
	public void mouseScroll(double deltaZoom) {
		double currentValue = zoomProperty.get();
		double missingTime = 0;
		if (zoomTimeline != null) {
			// Divide by 2 to fasten things up
			missingTime = (zoomTimeline.getTotalDuration().toMillis() - zoomTimeline.getCurrentTime().toMillis()) / 2;
			zoomTimeline.jumpTo("end");
		}
		double scale = zoomProperty.get();
		zoomProperty.set(currentValue);
		scale += deltaZoom;
		// Clamp
		if (scale > MAX_ZOOM_LEVEL)
			scale = MAX_ZOOM_LEVEL;
		if (scale < -MAX_ZOOM_LEVEL)
			scale = -MAX_ZOOM_LEVEL;
		zoomTimeline = new Timeline(new KeyFrame(Duration.millis(100 + missingTime), new KeyValue(zoomProperty, scale, Interpolator.EASE_BOTH)));
		zoomTimeline.play();
	}

	/**
	 * Returns the currently visible part of the world. The coordinates are world coordinates, relative to the world's origin with one unit corresponding to one
	 * block. The vertical range of this AABBd (the z axis) is from zero to zero. This object is <i>not</i> immutable and should not be changed.
	 */
	public AABBd getFrustum() {
		return frustum.get();
	}

	/**
	 * The zoom level is a rounded version of {@link #zoomProperty}. Use this to control the level of detail used for MIP-mapping.
	 */
	public int getZoomLevel() {
		return (int) Math.ceil(zoomProperty.get());
	}

	public Vector2dc getMousePos() {
		return mousePosProperty.get();
	}

	public Vector2dc getMouseInWorld() {
		return mouseWorldProperty.get();
	}

	public Vector2dc getTranslation() {
		return translationProperty.get();
	}
}