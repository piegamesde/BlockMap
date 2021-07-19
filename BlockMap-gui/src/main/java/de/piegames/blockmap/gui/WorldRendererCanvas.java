package de.piegames.blockmap.gui;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.joml.Vector2dc;
import org.joml.Vector2ic;

import de.piegames.blockmap.world.ChunkMetadata;
import de.piegames.blockmap.world.RegionFolder;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyFloatProperty;
import javafx.beans.property.ReadOnlyFloatWrapper;
import javafx.beans.property.ReadOnlyMapProperty;
import javafx.beans.property.ReadOnlyMapWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class WorldRendererCanvas extends ResizableCanvas {

	public static final int													THREAD_COUNT	= 4;

	protected RenderedMap													map;
	protected ThreadPoolExecutor											executor;
	protected GraphicsContext												gc				= getGraphicsContext2D();
	public final DisplayViewport											viewport		= new DisplayViewport();
	public final ObjectProperty<RegionFolder>								regionFolder	= new SimpleObjectProperty<>();
	protected ReadOnlyObjectWrapper<String>									status			= new ReadOnlyObjectWrapper<String>();
	protected ReadOnlyMapWrapper<Vector2ic, Map<Vector2ic, ChunkMetadata>>	chunkMetadata	= new ReadOnlyMapWrapper<>(FXCollections.observableHashMap());
	protected ReadOnlyFloatWrapper											progress		= new ReadOnlyFloatWrapper();

	public WorldRendererCanvas() {
		{// Executor
			executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(THREAD_COUNT);
			executor.setKeepAliveTime(20, TimeUnit.SECONDS);
			executor.allowCoreThreadTimeOut(true);
		}

		progress.addListener(e -> repaint());

		this.regionFolder.addListener((obs, prev, val) -> {
			if (val == null || val.listRegions().isEmpty()) {
				map = null;
				status.set("No regions loaded");
				chunkMetadata.unbind();
				chunkMetadata.clear();
			} else {
				map = new RenderedMap(val, executor, viewport.mouseWorldProperty);
				map.getCurrentlyRendering().addListener((InvalidationListener) e -> repaint());
				progress.bind(map.getProgress());
				chunkMetadata.bind(map.getChunkMetadata());
				status.set("Rendering");
				executor.execute(() -> Platform.runLater(() -> status.set("Done")));
			}
			repaint();
		});
		this.regionFolder.set(null);

		viewport.widthProperty.bind(widthProperty());
		viewport.heightProperty.bind(heightProperty());
		viewport.frustumProperty.addListener(e -> repaint());

		repaint();
	}

	public void shutDown() {
		status.set("Stopping");
		executor.shutdownNow();
		try {
			executor.awaitTermination(1, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		status.set("Stopped");
	}

	@Override
	public void render() {
		gc.setImageSmoothing(false);
		// gc.setStroke(Color.GREEN.deriveColor(0, 1, 1, .2));y
		gc.setLineWidth(10);
		// gc.clearRect(0, 0, getWidth(), getHeight());
		gc.setFill(new Color(0.2f, 0.2f, 0.6f, 1.0f));
		gc.fillRect(0, 0, getWidth(), getHeight());

		if (map != null) {
			double scale = viewport.scaleProperty.get();
			gc.save();
			gc.scale(scale, scale);
			Vector2dc translation = viewport.getTranslation();
			gc.translate(translation.x(), translation.y());

			map.draw(gc, Math.min(Math.max(0, -viewport.getZoomLevel()), 5), viewport.getFrustum(), scale);
			gc.restore();
		}

		// gc.strokeRect(100, 100, getWidth() - 200, getHeight() - 200);
		// gc.strokeRect(0, 0, getWidth() - 0, getHeight() - 0);
	}

	public ReadOnlyMapProperty<Vector2ic, Map<Vector2ic, ChunkMetadata>> getChunkMetadata() {
		return chunkMetadata.getReadOnlyProperty();
	}

	public ReadOnlyObjectProperty<String> getStatus() {
		return status.getReadOnlyProperty();
	}

	public ReadOnlyFloatProperty getProgress() {
		return progress.getReadOnlyProperty();
	}
}
