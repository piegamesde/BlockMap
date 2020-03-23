package de.piegames.blockmap.gui;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.joml.AABBd;
import org.joml.Vector2dc;
import org.joml.Vector2i;
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
	private Map<Vector2ic, RenderedRegion>									plainRegions	= new HashMap<>();
	private Set<RenderedRegion>												notRendered		= ConcurrentHashMap.newKeySet();
	private Map<Integer, Map<Vector2ic, RenderedRegion>>					regions			= new HashMap<>();
	private int																regionsCount, regionsRendered;
	protected ReadOnlyFloatWrapper											progress		= new ReadOnlyFloatWrapper();
	protected ReadOnlyMapWrapper<Vector2ic, Map<Vector2ic, ChunkMetadata>>	chunkMetadata	= new ReadOnlyMapWrapper<>(FXCollections.observableHashMap());
	protected ReadOnlySetWrapper<Vector2ic>									rendering		= new ReadOnlySetWrapper<>(FXCollections.observableSet());
	/** Where the mouse currently points to, in world coordinates */
	protected ReadOnlyObjectProperty<Vector2dc>								mouseWorldProperty;

	public RenderedMap(RegionFolder regionFolder, ExecutorService executor, ReadOnlyObjectProperty<Vector2dc> mouseWorldProperty) {
		this.regionFolder = Objects.requireNonNull(regionFolder);
		this.mouseWorldProperty = Objects.requireNonNull(mouseWorldProperty);
		Collection<Vector2ic> regions = regionFolder.listRegions();
		if (regions.isEmpty())
			throw new IllegalArgumentException("World can not be empty");
		regionFolder.listRegions().stream().map(r -> new RenderedRegion(this, r)).forEach(r -> plainRegions.put(r.position, r));

		this.regions.put(0, plainRegions);
		for (int i = 1; i <= DisplayViewport.MAX_ZOOM_LEVEL; i++) {
			int j = i;
			this.regions.put(i, this.regions.get(i - 1)
					.keySet()
					.stream()
					.map(v -> new Vector2i(v.x() >> 1, v.y() >> 1))
					.distinct()
					.collect(Collectors.toMap(Function.identity(), k -> new RenderedRegion(this, j, k))));
		}

		regionsRendered = 0;
		regionsCount = regions.size();
		notRendered.addAll(plainRegions.values());
		/* Create one task per region file. Don't specify which one yet, this will be determined later on the fly */
		for (int i = 0; i < regionsCount; i++)
			executor.submit(this);
	}

	public void draw(GraphicsContext gc, int level, AABBd frustum, double scale) {
		/* Draw background */
		gc.setFill(new Color(0.3f, 0.3f, 0.9f, 1.0f));
		plainRegions.values().stream()
				.filter(r -> r.isVisible(frustum))
				.forEach(r -> r.drawBackground(gc, scale));

		/* Draw images */
		Map<Vector2ic, RenderedRegion> map = regions.get(level);
		map.values().stream()
				.filter(r -> r.isVisible(frustum))
				.forEach(r -> r.draw(gc, frustum, scale));

		/* Draw currently rendering */
		gc.setFill(new Color(0.9f, 0.9f, 0.15f, 1.0f));
		rendering.stream()
				.map(plainRegions::get)
				.filter(r -> r.isVisible(frustum))
				.forEach(r -> r.drawForeground(gc, frustum, scale));
	}

	@Override
	public void run() {
		RenderedRegion region = nextRegion();
		Platform.runLater(() -> rendering.getValue().add(region.position));
		try {
			Vector2ic position = region.position;
			Region renderedRegion = regionFolder.render(position);
			WritableImage texture = SwingFXUtils.toFXImage(renderedRegion.getImage(), null);
			region.setImage(texture);
			updateMipMaps(position, texture);

			Platform.runLater(() -> chunkMetadata.put(position, Collections.unmodifiableMap(renderedRegion.getChunkMetadata())));
			Platform.runLater(() -> progress.set((float) regionsRendered++ / regionsCount));
		} catch (Throwable e) {
			e.printStackTrace();
		} finally {
			Platform.runLater(() -> rendering.getValue().remove(region.position));
		}
	}

	private void updateMipMaps(Vector2ic position, WritableImage image) {
		for (int i = 1; i <= DisplayViewport.MAX_ZOOM_LEVEL; i++) {
			int dstX = (position.x() & 1) * 256;
			int dstY = (position.y() & 1) * 256;
			position = new Vector2i(position.x() >> 1, position.y() >> 1);
			RenderedRegion region = regions.get(i).get(position);
			region.setSubImage(image, dstX, dstY);
			image = region.getImage();
		}
	}

	/** Returns the next Region to render */
	protected synchronized RenderedRegion nextRegion() {
		try {
			// In region coordinates
			Vector3d cursorPos = new Vector3d(mouseWorldProperty.get(), 0).div(512).sub(.5, .5, 0);

			Comparator<RenderedRegion> comp = (a, b) -> Double.compare(new Vector3d(a.position.x(), a.position.y(), 0).sub(cursorPos).length(),
					new Vector3d(b.position.x(), b.position.y(), 0).sub(cursorPos).length());
			RenderedRegion min = null;
			for (RenderedRegion r : notRendered)
				if (r.getImage() == null && (min == null || comp.compare(min, r) > 0))
					min = r;
			notRendered.remove(min);
			return min;
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