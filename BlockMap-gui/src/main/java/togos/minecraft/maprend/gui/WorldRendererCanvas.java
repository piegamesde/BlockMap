package togos.minecraft.maprend.gui;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.joml.Vector2dc;
import org.joml.Vector3d;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import togos.minecraft.maprend.BoundingRect;
import togos.minecraft.maprend.RegionMap;
import togos.minecraft.maprend.gui.RenderedRegion.RenderingState;
import togos.minecraft.maprend.io.RegionFile;
import togos.minecraft.maprend.renderer.RegionRendererOld;

public class WorldRendererCanvas extends Canvas implements Runnable {

	public static final int					THREAD_COUNT	= 4;

	protected RegionRendererOld				renderer;
	protected RenderedMap					map;

	protected ScheduledThreadPoolExecutor	executor;
	protected final List<Future<?>>			submitted		= Collections.synchronizedList(new LinkedList<>());

	protected GraphicsContext				gc				= getGraphicsContext2D();

	public final DisplayViewport			viewport		= new DisplayViewport();

	public WorldRendererCanvas(RegionRendererOld renderer) {
		this.renderer = Objects.requireNonNull(renderer);

		{// Executor
			executor = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(THREAD_COUNT);
			map = new RenderedMap(executor);// Executors.newScheduledThreadPool(THREAD_COUNT / 2));
			executor.scheduleAtFixedRate(() -> {
				try {
					// TODO execute more often if something changes and less often if not
					// update upscaled/downscaled images of chunks
					if (map.updateImage(viewport.getZoomLevel(), viewport.getFrustum()))
						repaint();
				} catch (Throwable e) {
					e.printStackTrace();
					throw e;
				}
			}, 1000, 1000, TimeUnit.MILLISECONDS);
			executor.scheduleWithFixedDelay(map::evictCache, 10, 10, TimeUnit.SECONDS);

			executor.setKeepAliveTime(20, TimeUnit.SECONDS);
			executor.allowCoreThreadTimeOut(true);
		}

		viewport.widthProperty.bind(widthProperty());
		viewport.heightProperty.bind(heightProperty());
		invalidateTextures();
		viewport.frustumProperty.addListener(e -> repaint());
		repaint();
	}

	public void loadWorld(File file) {
		map.clearReload(RegionMap.load(file, BoundingRect.INFINITE).regions);
		invalidateTextures();
	}

	public void invalidateTextures() {
		map.invalidateAll();
		for (int i = 0; i < THREAD_COUNT; i++)
			executor.submit(this);
	}

	public void shutDown() {
		executor.shutdownNow();
		try {
			executor.awaitTermination(1, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		map.close();
	}

	/** Queues in a repaint event calling {@link renderWorld} from the JavaFX Application Thread */
	public void repaint() {
		if (Platform.isFxApplicationThread())
			renderWorld();
		else
			Platform.runLater(this::renderWorld);
	}

	/** Requires to be called from the JavaFX Application Thread. */
	public void renderWorld() {
		gc = getGraphicsContext2D();
		// gc.setStroke(Color.GREEN.deriveColor(0, 1, 1, .2));
		gc.setLineWidth(10);
		// gc.clearRect(0, 0, getWidth(), getHeight());
		gc.setFill(new Color(0.2f, 0.2f, 0.6f, 1.0f));
		gc.fillRect(0, 0, getWidth(), getHeight());

		double scale = viewport.scaleProperty.get();
		gc.save();
		gc.scale(scale, scale);
		Vector2dc translation = viewport.getTranslation();
		gc.translate(translation.x(), translation.y());

		map.draw(gc, viewport.getZoomLevel(), viewport.getFrustum(), scale);
		gc.restore();

		if (map.isNothingLoaded()) {
			gc.setFill(Color.WHITE);
			gc.setFont(Font.font(20));
			gc.fillText("No regions loaded", 10, getHeight() - 10);
		}

		// gc.strokeRect(100, 100, getWidth() - 200, getHeight() - 200);
		// gc.strokeRect(0, 0, getWidth() - 0, getHeight() - 0);
	}

	public RegionRendererOld getRegionRenderer() {
		return renderer;
	}

	@Override
	public void run() {
		RenderedRegion region = null;
		region = nextRegion();
		if (region == null)
			return;
		repaint();
		try {
			try (RegionFile rf = new RegionFile(region.region.regionFile)) {
				BufferedImage texture2 = null;
				do {
					texture2 = renderer.render(rf);
				} while (region.valid.compareAndSet(RenderingState.REDRAW, RenderingState.DRAWING) && !Thread.interrupted());

				WritableImage texture = SwingFXUtils.toFXImage(texture2, null);
				region.setImage(texture);
				repaint();
			}
		} catch (Throwable e) {
			e.printStackTrace();
		} finally {
			region.valid.set(RenderingState.VALID);
			executor.submit(this);
		}
	}

	/** Returns the next Region to render */
	protected synchronized RenderedRegion nextRegion() {
		// In region coordinates
		Vector3d cursorPos = new Vector3d(viewport.getMouseInWorld(), 0).div(512).sub(.5, .5, 0);

		Comparator<RenderedRegion> comp = (a, b) -> Double.compare(new Vector3d(a.position.x(), a.position.y(), 0).sub(cursorPos).length(), new Vector3d(b.position.x(), b.position.y(), 0).sub(cursorPos).length());
		RenderedRegion min = null;
		for (RenderedRegion r : map.get(0).values())
			if (r.valid.get() == RenderingState.INVALID && (min == null || comp.compare(min, r) > 0))
				min = r;
		if (min != null)
			// min got handled by another Thread already (while we were still searching), so get a new one
			if (!min.valid.compareAndSet(RenderingState.INVALID, RenderingState.DRAWING))
				return nextRegion();
		return min;
	}

	/** @return a*2^n using bit shifting */
	public static int pow2(int a, int n) {
		if (n > 0)
			return a << n;
		else if (n < 0)
			return a >> -n;
		else
			return a;
	}
}
