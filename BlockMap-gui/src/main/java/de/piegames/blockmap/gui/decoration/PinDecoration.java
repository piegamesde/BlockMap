package de.piegames.blockmap.gui.decoration;

import java.util.Objects;

import org.joml.AABBd;
import org.joml.Vector2dc;

import de.piegames.blockmap.gui.CanvasHelper;
import de.piegames.blockmap.gui.DisplayViewport;
import de.piegames.blockmap.gui.decoration.Pin.ChunkPin;
import javafx.beans.InvalidationListener;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

public class PinDecoration extends CanvasHelper {

	protected final DisplayViewport		viewport;

	public final ListProperty<Pin>		pins		= new SimpleListProperty<>(FXCollections.emptyObservableList());
	public final ListProperty<ChunkPin>	chunkPins	= new SimpleListProperty<>(FXCollections.observableArrayList());

	public PinDecoration(DisplayViewport viewport) {
		this.viewport = Objects.requireNonNull(viewport);
		InvalidationListener l = e -> repaint();
		viewport.frustumProperty.addListener(l);
		visibleProperty().addListener(l);
		pins.addListener(l);
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

		for (ChunkPin p : chunkPins) {
			// TODO check bounds
			p.draw(gc);
		}
		for (Pin p : pins) {
			if (!frustum.testPoint(p.position.x, p.position.y, 0))
				continue;
			p.drawBackground(gc);
		}
		for (Pin p : pins) {
			if (!frustum.testPoint(p.position.x, p.position.y, 0))
				continue;
			gc.drawImage(p.image, p.position.x - 16 / scale, p.position.y - 16 / scale, 32 / scale, 32 / scale);
		}

		gc.restore();
	}
}
