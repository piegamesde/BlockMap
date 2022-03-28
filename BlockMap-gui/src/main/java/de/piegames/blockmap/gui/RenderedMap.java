package de.piegames.blockmap.gui;

import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import org.joml.AABBd;
import org.joml.Vector2dc;
import org.joml.Vector2ic;
import org.joml.Vector3d;

import de.piegames.blockmap.world.ChunkMetadata;
import de.piegames.blockmap.world.Region;
import de.piegames.blockmap.world.RegionFolder;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyFloatProperty;
import javafx.beans.property.ReadOnlyFloatWrapper;
import javafx.beans.property.ReadOnlyMapProperty;
import javafx.beans.property.ReadOnlyMapWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlySetProperty;
import javafx.beans.property.ReadOnlySetWrapper;
import javafx.collections.FXCollections;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

public class RenderedMap implements Runnable {

	private RegionFolder													regionFolder;
	private final Set<Vector2ic>											notRendered		= ConcurrentHashMap.newKeySet();
	private final Map<Vector2ic, RenderedRegion>							regions;
	private int																regionsCount, regionsRendered;
	protected ReadOnlyFloatWrapper											progress		= new ReadOnlyFloatWrapper();
	protected ReadOnlyMapWrapper<Vector2ic, Map<Vector2ic, ChunkMetadata>>	chunkMetadata	= new ReadOnlyMapWrapper<>(FXCollections.observableHashMap());
	protected ReadOnlySetWrapper<Vector2ic>									rendering		= new ReadOnlySetWrapper<>(FXCollections.observableSet());
	/** Where the mouse currently points to, in world coordinates */
	protected ReadOnlyObjectProperty<Vector2dc>								mouseWorldProperty;
	private final AtomicBoolean cancelRendering = new AtomicBoolean(false);

	public RenderedMap(RegionFolder regionFolder, ExecutorService executor, ReadOnlyObjectProperty<Vector2dc> mouseWorldProperty) {
		this.regionFolder = Objects.requireNonNull(regionFolder);
		this.mouseWorldProperty = Objects.requireNonNull(mouseWorldProperty);
		Set<Vector2ic> regions = regionFolder.listRegions();
		if (regions.isEmpty())
			throw new IllegalArgumentException("World can not be empty");
		this.notRendered.addAll(regions);
		this.regions = this.notRendered.stream().collect(Collectors.toMap(v -> v, RenderedRegion::new));

		regionsRendered = 0;
		regionsCount = regions.size();
		/* Create one task per region file. Don't specify which one yet, this will be determined later on the fly */
		for (int i = 0; i < regionsCount; i++)
			executor.submit(this);
	}

	public void draw(GraphicsContext gc, int level, AABBd frustum, double scale) {
		/* Fore the background */
		gc.setFill(new Color(0.3f, 0.3f, 0.9f, 1.0f));

		/* Draw images */
		regions.values()
				.stream()
				.filter(r -> r.isVisible(frustum))
				.forEach(r -> r.draw(gc, frustum, scale, level));

		/* Draw currently rendering */
		gc.setFill(new Color(0.9f, 0.9f, 0.15f, 1.0f));
		rendering.stream()
				.map(regions::get)
				.filter(r -> r.isVisible(frustum))
				.forEach(r -> r.drawForeground(gc, frustum, scale));
	}
	
	/* Cancel existing rendering tasks and already free up some memory */
	public void cancel() {
		cancelRendering.set(true);
		regions.clear();
	}

	/* Render the next region file on a worker thread */
	@Override
	public void run() {
		if (cancelRendering.get())
			return;
		RenderedRegion region = nextRegion();
		Platform.runLater(() -> rendering.getValue().add(region.position));
		try {
			Vector2ic position = region.position;
			Region renderedRegion = regionFolder.render(position);
			WritableImage texture = SwingFXUtils.toFXImage(renderedRegion.getImage(), null);
			region.setImage(texture);

			Platform.runLater(() -> chunkMetadata.put(position, Collections.unmodifiableMap(renderedRegion.getChunkMetadata())));
			Platform.runLater(() -> progress.set((float) regionsRendered++ / regionsCount));
		} catch (Throwable e) {
			e.printStackTrace();
		} finally {
			Platform.runLater(() -> rendering.getValue().remove(region.position));
		}
	}

	/** Returns the next Region to render */
	protected synchronized RenderedRegion nextRegion() {
		try {
			// In region coordinates
			Vector3d cursorPos = new Vector3d(mouseWorldProperty.get(), 0).div(512).sub(.5, .5, 0);

			Comparator<Vector2ic> comp = (a, b) -> Double.compare(new Vector3d(a.x(), a.y(), 0).sub(cursorPos).length(),
					new Vector3d(b.x(), b.y(), 0).sub(cursorPos).length());
			Vector2ic min = null;
			for (Vector2ic r : notRendered)
				if (min == null || comp.compare(min, r) > 0)
					min = r;
			notRendered.remove(min);
			return regions.get(min);
		} catch (Throwable e) {
			e.printStackTrace();
			throw new Error(e);
		}
	}

	public ReadOnlyFloatProperty getProgress() {
		return progress.getReadOnlyProperty();
	}

	public ReadOnlyMapProperty<Vector2ic, Map<Vector2ic, ChunkMetadata>> getChunkMetadata() {
		return chunkMetadata.getReadOnlyProperty();
	}

	public ReadOnlySetProperty<Vector2ic> getCurrentlyRendering() {
		return rendering.getReadOnlyProperty();
	}
}