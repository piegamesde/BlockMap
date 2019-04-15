package de.piegames.blockmap.gui.decoration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joml.Vector2d;
import org.joml.Vector2dc;
import org.joml.Vector2i;
import org.joml.Vector2ic;

import de.piegames.blockmap.gui.DisplayViewport;
import de.piegames.blockmap.gui.decoration.Pin.MergedPin;
import de.piegames.blockmap.gui.decoration.Pin.PinType;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.FXCollections;
import javafx.geometry.Bounds;
import javafx.scene.layout.AnchorPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import javafx.util.Duration;
import smile.clustering.HierarchicalClustering;
import smile.clustering.linkage.Linkage;

public class PinDecoration extends AnchorPane implements ChangeListener<Number> {

	private static Log					log				= LogFactory.getLog(PinDecoration.class);
	static final double					MAX_VISIBILITY	= 1000, MAX_MERGE = 500;

	protected final DisplayViewport		viewport;

	protected AnchorPane				world;

	public final SetProperty<PinType>	visiblePins		= new SimpleSetProperty<>(FXCollections.observableSet());

	private Collection<Pin>				staticPins;
	private Map<Vector2ic, PinRegion>	byRegion		= Collections.emptyMap();
	private final List<PinGroup>		byGroup			= new ArrayList<>();

	// private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat(
	// "pin-background-thread-%d").build());
	// private LimitedExecutionHandler executeUpdate = new LimitedExecutionHandler(this::updateVisible, (r) -> executor.schedule(r, 50,
	// TimeUnit.MILLISECONDS));
	private LimitedExecutionHandler		executeUpdate	= new LimitedExecutionHandler(this::updateVisible,
			(r) -> new Timeline(new KeyFrame(Duration.millis(50), e -> r.run())).play());
	// private LimitedExecutionHandler executeZoom = new LimitedExecutionHandler(this::updateZoomImpl,
	// (r) -> new Timeline(new KeyFrame(Duration.millis(50), e -> r.run())).play());

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
		// visiblePins.addListener((InvalidationListener) e -> updateVisible());
	}

	class PinRegion {

		Vector2ic		position;
		List<Pin>		pins		= new ArrayList<>();
		List<PinGroup>	clusters	= new ArrayList<>();
		PinRegion[]		neighbors;
		boolean			valid		= false, loaded = false;

		PinRegion(Vector2ic position) {
			this.position = Objects.requireNonNull(position);
		}

		void mergeGroups() {
			if (valid && Arrays.stream(neighbors).allMatch(r -> r.valid)) {
				/* Merge nearby clusters */
				while (true) {
					double minDist = Double.POSITIVE_INFINITY;
					PinGroup minG = null, minH = null;
					PinRegion minR = null, minS = null;

					for (PinRegion r : neighbors)
						for (PinGroup g : r.clusters)
							for (PinRegion s : neighbors)
								for (PinGroup h : s.clusters) {
									if (g == h)
										continue;
									double dist = g.center.distance(h.center);
									if (dist < minDist) {
										minDist = dist;
										minG = g;
										minH = h;
										minR = r;
										minS = s;
									}
								}
					if (minDist < MAX_MERGE) {
						minR.clusters.remove(minG);
						minS.clusters.remove(minH);
						PinGroup merged = new PinGroup(minG, minH);
						if (minR == minS)
							minR.clusters.add(merged);
						else if (minR == this || minS == this)
							this.clusters.add(merged);
						else {
							Vector2i regionPos = new Vector2i((int) merged.center.x() >> 9, (int) merged.center.y() >> 9);
							Arrays.stream(neighbors)
									.filter(n -> n.position.equals(regionPos))
									.findAny()
									.orElseThrow(() -> new InternalError("regionPos should be the same as one of the merged pin's parents")).clusters.add(
											merged);
						}
					} else
						break;
				}

				/* Put all clusters to the GUI */
				clusters.forEach(PinGroup::add);
				byGroup.addAll(clusters);
				clusters.clear();
			}
		}

		/** Cluster all visible pins in this region */
		void updatePins() {
			if (!loaded)
				return;
			List<Pin> visiblePins = pins.stream().filter(p -> PinDecoration.this.visiblePins.contains(p.type)).collect(Collectors.toList());

			/* Clustering */

			final int n = visiblePins.size();
			if (n > 0) {
				double[][] dist = new double[n][];
				for (int row = 0; row < n; row++) {
					dist[row] = new double[row + 1];
					for (int col = 0; col < row; col++)
						dist[row][col] = visiblePins.get(row).position.distance(visiblePins.get(col).position);
				}
				Linkage linkage = new smile.clustering.linkage.UPGMCLinkage(dist);
				HierarchicalClustering cluster = new HierarchicalClustering(linkage);

				/* Split at height MAX_MERGE */

				clusters.clear();
				{
					/* Temporary working queue containing subtree roots */
					Queue<Integer> top = new LinkedList<>();
					List<Integer> topCut = new ArrayList<>();
					top.add(2 * n - 2);
					while (!top.isEmpty()) {
						int current = top.remove();
						if (current < n || cluster.getHeight()[current - n] < MAX_MERGE)
							topCut.add(current);
						else {
							top.add(cluster.getTree()[current - n][0]);
							top.add(cluster.getTree()[current - n][1]);
						}
					}
					for (int i : topCut) {
						top.clear();
						top.add(i);
						List<Pin> group = new ArrayList<>();
						while (!top.isEmpty()) {
							int current = top.remove();
							if (current < n) {
								group.add(visiblePins.get(current));
							} else {
								top.add(cluster.getTree()[current - n][0]);
								top.add(cluster.getTree()[current - n][1]);
							}
						}
						clusters.add(new PinGroup(group));
					}
				}
			}
			valid = true;

			/* Notify all neighbors in a 3×3 area so they may merge nearby groups */

			for (PinRegion r : neighbors)
				r.mergeGroups();
		}
	}

	class PinGroup {
		Bounds		bounds;
		Vector2dc	center;
		List<Pin>	pins;
		boolean		added;

		PinGroup(List<Pin> pins) {
			this.pins = Objects.requireNonNull(pins);
			center = pins.stream().map(p -> p.position).collect(Vector2d::new, Vector2d::add, Vector2d::add).mul(1.0 / pins.size());
		}

		PinGroup(PinGroup a, PinGroup b) {
			this.pins = new ArrayList<>();
			pins.addAll(a.pins);
			pins.addAll(b.pins);
			int sizeLeft = a.pins.size(), sizeRight = b.pins.size();
			center = new Vector2d(
					a.center.x() * sizeLeft + b.center.x() * sizeRight,
					a.center.y() * sizeLeft + b.center.y() * sizeRight)
							.mul(1.0 / (sizeLeft + sizeRight));
		}

		public void add() {
			/* Clustering */

			final int n = pins.size();
			if (n == 0)
				return;
			double[][] dist = new double[n][];
			for (int row = 0; row < n; row++) {
				dist[row] = new double[row + 1];
				for (int col = 0; col < row; col++)
					dist[row][col] = pins.get(row).position.distance(pins.get(col).position);
			}
			Linkage linkage = new smile.clustering.linkage.UPGMCLinkage(dist);
			HierarchicalClustering cluster = new HierarchicalClustering(linkage);
			double[] height = cluster.getHeight();

			/* Cluster analysis */

			List<MergedPin> clusters = new ArrayList<>();
			for (int i = 0; i < n - 1; i++) {
				Map<PinType, Long> subTypes = new HashMap<>();

				int mergedLeft = cluster.getTree()[i][0];
				Pin subLeft;
				int sizeLeft = 1;
				if (mergedLeft < n) {
					subLeft = pins.get(mergedLeft);
					subLeft.minHeight = 0;
					subTypes.put(subLeft.type, 1L);
				} else {
					MergedPin m = clusters.get(mergedLeft - n);
					sizeLeft = m.subCount + 1;
					subLeft = m;
					subTypes.putAll(m.pinCount);
				}
				subLeft.maxHeight = Math.min(height[i], MAX_VISIBILITY);

				int mergedRight = cluster.getTree()[i][1];
				Pin subRight;
				int sizeRight = 1;
				if (mergedRight < n) {
					subRight = pins.get(mergedRight);
					subTypes.compute(subRight.type, (k, v) -> (v == null) ? 1 : v + 1);
				} else {
					MergedPin m = clusters.get(mergedRight - n);
					sizeRight = m.subCount + 1;
					m.pinCount.forEach((k, v) -> subTypes.put(k, v + subTypes.getOrDefault(k, 0L)));
					subRight = m;
				}
				subRight.maxHeight = Math.min(height[i], MAX_VISIBILITY);

				Vector2dc position = new Vector2d(
						subLeft.position.x() * sizeLeft + subRight.position.x() * sizeRight,
						subLeft.position.y() * sizeLeft + subRight.position.y() * sizeRight)
								.mul(1.0 / (sizeLeft + sizeRight));
				MergedPin mergedPin = new MergedPin(subLeft, subRight, sizeLeft + sizeRight, position, subTypes, viewport);
				mergedPin.minHeight = Math.min(height[i], MAX_VISIBILITY);
				mergedPin.maxHeight = MAX_VISIBILITY; /* We'll set this to a lower value if there is a parent. */
				clusters.add(mergedPin);
				pins.add(mergedPin);
			}

			added = true;
			// Platform.runLater(this::updateAnimation);
			updateAnimation();
		}

		public void remove() {
			added = false;
			// Platform.runLater(this::updateAnimation);
			updateAnimation();
		}

		void updateAnimation() {
			double height = 60 / viewport.scaleProperty.get();
			pins.forEach(p -> p.updateAnimation(height, added, world));
		}
	}

	private static final Vector2ic[] connectivity8 = new Vector2ic[] {
			new Vector2i(-1, -1), new Vector2i(0, -1), new Vector2i(1, -1),
			new Vector2i(-1, 0), new Vector2i(0, 0), new Vector2i(1, 0),
			new Vector2i(-1, 1), new Vector2i(0, 1), new Vector2i(1, 1)
	};

	public void loadWorld(Collection<Vector2ic> regions, Collection<Pin> staticPins) {
		// executor.execute(() -> {
		this.staticPins = Objects.requireNonNull(staticPins);

		byRegion = regions.stream().collect(Collectors.toMap(Function.identity(), PinRegion::new));
		for (Vector2ic r : byRegion.keySet())
			byRegion.get(r).neighbors = Arrays.stream(connectivity8)
					.map(v -> v.add(r, new Vector2i()))
					.filter(byRegion::containsKey)
					.map(byRegion::get)
					.collect(Collectors.toList())
					.toArray(new PinRegion[0]);
		reloadWorld();
		// });
	}

	public void reloadWorld() {
		// executor.execute(() -> {
		byGroup.forEach(PinGroup::remove);
		byGroup.clear();
		byRegion.values().forEach(r -> {
			r.pins.clear();
			r.loaded = r.valid = false;
		});
		for (Pin p : staticPins) {
			Vector2i pos = new Vector2i((int) (p.position.x()) >> 9, (int) (p.position.y()) >> 9);
			if (!byRegion.containsKey(pos))
				log.warn("Pin " + p + " is outside of the world's bounds and will be ignored");
			else
				byRegion.get(pos).pins.add(p);
		}
		// });
	}

	public void loadRegion(Vector2ic region, Collection<Pin> dynamicPins) {
		// executor.execute(() -> {
		if (byRegion.containsKey(region)) {
			PinRegion r = byRegion.get(region);
			r.pins.addAll(dynamicPins);
			r.loaded = true;
			r.updatePins();
		} else
			log.warn("Dynamic pins for region " + region + " are out of the world's boudns and will be ignored");
		// });
	}

	private void updateVisible() {
		// executor.execute(() -> {
		byGroup.forEach(PinGroup::remove);
		byGroup.clear();
		byRegion.values().forEach(r -> r.valid = false);
		byRegion.values().stream().filter(r -> r.loaded).forEach(PinRegion::updatePins);
		// });
	}

	@Override
	public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
		// executeZoom.requestExecution();
		byGroup.forEach(group -> group.updateAnimation());
	}

	public void shutDown() {
		// executor.shutdownNow();
		// try {
		// executor.awaitTermination(1, TimeUnit.MINUTES);
		// } catch (InterruptedException e) {
		// log.warn("Pin background thread did not finish", e);
		// }
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
}
