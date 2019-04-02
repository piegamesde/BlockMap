package de.piegames.blockmap.gui.decoration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joml.Vector2d;
import org.joml.Vector2dc;
import org.joml.Vector2ic;

import com.google.common.collect.Streams;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import de.piegames.blockmap.gui.DisplayViewport;
import de.piegames.blockmap.gui.decoration.Pin.MergedPin;
import de.piegames.blockmap.gui.decoration.Pin.PinType;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.FXCollections;
import javafx.scene.layout.AnchorPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import javafx.util.Duration;
import smile.clustering.HierarchicalClustering;
import smile.clustering.linkage.Linkage;

public class PinDecoration extends AnchorPane implements ChangeListener<Number> {

	private static Log					log				= LogFactory.getLog(PinDecoration.class);

	protected final DisplayViewport		viewport;

	protected AnchorPane				world;

	public final SetProperty<PinType>	visiblePins		= new SimpleSetProperty<>(FXCollections.observableSet());

	protected List<Pin>					staticPins		= new ArrayList<>();
	protected Map<Vector2ic, List<Pin>>	dynamicPins		= new HashMap<>();

	protected Map<DistanceItem, Double>	distanceMatrix	= new HashMap<>();
	private List<Pin>					allPins			= new ArrayList<>();
	private double[]					height			= new double[] {};

	private Timeline					timeline;
	private int							lastLevel		= 0;

	private ScheduledExecutorService	executor		= Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat(
			"pin-background-thread-%d").build());
	private LimitedExecutionHandler		executeUpdate	= new LimitedExecutionHandler(this::updatePinsImpl, (r) -> executor.schedule(r, 50,
			TimeUnit.MILLISECONDS));
	private LimitedExecutionHandler		executeZoom		= new LimitedExecutionHandler(this::updateZoomImpl,
			(r) -> new Timeline(new KeyFrame(Duration.millis(50), e -> r.run())).play());

	public PinDecoration(DisplayViewport viewport) {
		this.viewport = Objects.requireNonNull(viewport);
		this.viewport.scaleProperty.addListener(new WeakChangeListener<>(this));

		world = new AnchorPane();
		{
			Scale s = new Scale();
			s.xProperty().bind(viewport.scaleProperty);
			s.yProperty().bind(viewport.scaleProperty);
			Translate t = new Translate();
			t.xProperty().bind(Bindings.createDoubleBinding(() -> viewport.translationProperty.get().x(), viewport.translationProperty));
			t.yProperty().bind(Bindings.createDoubleBinding(() -> viewport.translationProperty.get().y(), viewport.translationProperty));
			world.getTransforms().add(s);
			world.getTransforms().add(t);
		}
		world.setPickOnBounds(false);
		getChildren().add(world);

		setMinSize(0, 0);
		setPrefSize(0, 0);
		setPickOnBounds(false);

		{
			final Rectangle outputClip = new Rectangle();
			this.setClip(outputClip);

			this.layoutBoundsProperty().addListener((ov, oldValue, newValue) -> {
				outputClip.setWidth(newValue.getWidth());
				outputClip.setHeight(newValue.getHeight());
			});
		}

		visiblePins.addListener((InvalidationListener) e -> executeUpdate.requestExecution());
	}

	public void clearPins() {
		executor.submit(() -> {
			staticPins.clear();
			dynamicPins.clear();
			allPins.clear();
			distanceMatrix.clear();
		});
		world.getChildren().clear();
	}

	public void clearDynamic() {
		executor.submit(() -> {
			dynamicPins.clear();
			executeUpdate.requestExecution();
		});
	}

	public void setStaticPins(Set<Pin> pins) {
		executor.submit(() -> {
			staticPins.clear();
			staticPins.addAll(pins);
			executeUpdate.requestExecution();
		});
	}

	public void setDynamicPins(Vector2ic regionPos, List<Pin> pins) {
		executor.submit(() -> {
			dynamicPins.put(regionPos, Objects.requireNonNull(pins));
			executeUpdate.requestExecution();
		});
	}

	private void updatePinsImpl() {
		allPins = Streams.concat(
				dynamicPins.entrySet().stream().flatMap(e -> e.getValue().stream().filter(p -> visiblePins.contains(p.type))),
				staticPins.stream().filter(p -> visiblePins.contains(p.type)))
				.collect(Collectors.toList());

		/* Clustering */

		final int n = allPins.size();
		if (n == 0) // TODO cleanup
			return;
		double[][] dist = new double[n][];
		for (int row = 0; row < n; row++) {
			dist[row] = new double[row + 1];
			for (int col = 0; col < row; col++)
				// dist[row][col] = distanceMatrix.computeIfAbsent(new DistanceItem(allPins.get(row).position, allPins.get(col).position), e ->
				// e.a.distance(e.b));
				dist[row][col] = allPins.get(row).position.distance(allPins.get(col).position);
		}

		Linkage linkage = new smile.clustering.linkage.UPGMCLinkage(dist);
		HierarchicalClustering cluster = new HierarchicalClustering(linkage);
		this.height = cluster.getHeight();

		/* Cluster analysis */

		List<MergedPin> clusters = new ArrayList<>();
		for (int i = 0; i < n - 1; i++) {
			Map<PinType, Long> subTypes = new HashMap<>();

			int mergedLeft = cluster.getTree()[i][0];
			Pin subLeft;
			int sizeLeft = 1;
			if (mergedLeft < n) {
				subLeft = allPins.get(mergedLeft);
				subLeft.level = 0;
				subTypes.put(subLeft.type, 1L);
			} else {
				MergedPin m = clusters.get(mergedLeft - n);
				sizeLeft = m.subCount;
				subLeft = m;
				subTypes.putAll(m.pinCount);
			}
			subLeft.parentLevel = i + 1;

			int mergedRight = cluster.getTree()[i][1];
			Pin subRight;
			int sizeRight = 1;
			if (mergedRight < n) {
				subRight = allPins.get(mergedRight);
				subTypes.compute(subRight.type, (k, v) -> (v == null) ? 1 : v + 1);
			} else {
				MergedPin m = clusters.get(mergedRight - n);
				sizeRight = m.subCount;
				m.pinCount.forEach((k, v) -> subTypes.put(k, v + subTypes.getOrDefault(k, 0L)));
				subRight = m;
			}
			subRight.parentLevel = i + 1;

			Vector2dc position = new Vector2d(
					subLeft.position.x() * sizeLeft + subRight.position.x() * sizeRight,
					subLeft.position.y() * sizeLeft + subRight.position.y() * sizeRight)
							.mul(1.0 / (sizeLeft + sizeRight));
			MergedPin mergedPin = new MergedPin(subLeft, subRight, sizeLeft + sizeRight, position, subTypes, viewport);
			mergedPin.level = i + 1;
			mergedPin.parentLevel = n; /* We'll set this to a lower value if there is a parent. */
			clusters.add(mergedPin);
			allPins.add(mergedPin);
		}

		Platform.runLater(() -> {
			world.getChildren().clear();
			world.getChildren().addAll(allPins.stream().map(Pin::getBottomGui).filter(Objects::nonNull).collect(Collectors.toList()));
			world.getChildren().addAll(allPins.stream().map(Pin::getTopGui).filter(Objects::nonNull).collect(Collectors.toList()));

			/* Invalidate animation and recalculate */
			lastLevel = -1;
			allPins.forEach(pin -> pin.zoomLevel = -1);
			updateZoomImpl();
			if (timeline != null)
				timeline.jumpTo("end");
		});
	}

	@Override
	public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
		executeZoom.requestExecution();
	}

	private void updateZoomImpl() {
		double height = 60 / viewport.scaleProperty.get();

		/* Determine which tree heights to switch between */
		for (int i = 0; i < this.height.length - 1; i++) {
			if (this.height[i] >= height) {
				if (i != lastLevel) {
					/* The zoom switched between two heights */
					if (timeline != null) {
						timeline.pause();
					}
					final int newLevel = i;
					List<KeyValue> values = allPins.stream().flatMap(pin -> pin.setZoomLevel(newLevel).stream()).collect(Collectors.toList());

					timeline = new Timeline(
							new KeyFrame(
									Duration.millis(500),
									null,
									e -> allPins.forEach(pin -> pin.zoomLevel = newLevel),
									values));
					timeline.playFromStart();

					lastLevel = newLevel;
				}
				break;
			}
		}
	}

	public void shutDown() {
		executor.shutdownNow();
		try {
			executor.awaitTermination(1, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
			log.warn("Pin background thread did not finish", e);
		}
	}

	private class LimitedExecutionHandler {
		Runnable			execute;
		Consumer<Runnable>	queueDelayed;
		long				lastRequest	= -1;

		public LimitedExecutionHandler(Runnable execute, Consumer<Runnable> queueDelayed) {
			this.execute = execute;
			this.queueDelayed = queueDelayed;
		}

		private void requestExecution() {
			long old = lastRequest;
			lastRequest = System.nanoTime();
			if (old == -1)
				execute();
		}

		private void execute() {
			long time = System.nanoTime();
			if (time - lastRequest > 50_000_000) {
				execute.run();
				lastRequest = -1;
			} else {
				queueDelayed.accept(this::execute);
			}
		}
	}

	private static final class DistanceItem {
		private Vector2dc a, b;

		private DistanceItem(Vector2dc a, Vector2dc b) {
			this.a = a;
			this.b = b;
		}

		@Override
		public int hashCode() {
			return ((a == null) ? 0 : a.hashCode()) ^ ((b == null) ? 0 : b.hashCode());
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			DistanceItem other = (DistanceItem) obj;
			return (a == other.a && b == other.b) || (a == other.b && b == other.a);
		}
	}
}
