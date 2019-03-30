package de.piegames.blockmap.gui.decoration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joml.Vector2ic;

import com.google.common.collect.Streams;

import de.piegames.blockmap.gui.DisplayViewport;
import de.piegames.blockmap.gui.decoration.Pin.MergedPin;
import de.piegames.blockmap.gui.decoration.Pin.PinType;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import javafx.util.Duration;
import smile.clustering.HierarchicalClustering;
import smile.clustering.linkage.Linkage;

public class PinDecoration extends AnchorPane implements ChangeListener<Number> {

	private static Log					log			= LogFactory.getLog(PinDecoration.class);

	protected final DisplayViewport		viewport;

	protected AnchorPane				world;

	public final ObservableSet<PinType>	visiblePins	= FXCollections.observableSet();

	protected Set<Pin>					staticPins	= new HashSet<>();
	protected Map<Vector2ic, Set<Pin>>	dynamicPins	= new HashMap<>();

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

		InvalidationListener l = e -> updatePins();
		visiblePins.addListener(l);
	}

	public void clear() {
		staticPins.clear();
		dynamicPins.clear();
		world.getChildren().clear();
	}

	public void clearDynamic() {
		dynamicPins.clear();
	}

	public void setStaticPins(Set<Pin> pins) {
		staticPins.clear();
		staticPins.addAll(pins);
		updatePins();
	}

	public void setDynamicPins(Vector2ic regionPos, Set<Pin> pins) {
		dynamicPins.put(regionPos, Objects.requireNonNull(pins));
		updatePins();
	}

	Timeline				timeline;
	List<List<KeyValue>>	keyvalues	= Collections.emptyList();
	double[]				height		= new double[] {};

	private void updatePins() {
		List<Pin> allPins = Streams.concat(
				dynamicPins.entrySet().stream().flatMap(e -> e.getValue().stream()).filter(p -> visiblePins.contains(p.type)),
				staticPins.stream().filter(p -> visiblePins.contains(p.type)))
				.collect(Collectors.toList());
		world.getChildren().clear();
		world.getChildren().addAll(allPins.stream().map(p -> p.getBottomGui()).filter(g -> g != null).collect(
				Collectors.toList()));
		world.getChildren().addAll(allPins.stream().map(p -> p.getTopGui()).filter(g -> g != null).collect(
				Collectors.toList()));

		/* Clustering */

		final int n = allPins.size();
		if (n == 0) // TODO cleanup
			return;
		double[][] dist = new double[n][];
		for (int row = 0; row < n; row++) {
			dist[row] = new double[row + 1];
			for (int col = 0; col < row; col++)
				dist[row][col] = allPins.get(row).position.distance(allPins.get(col).position);
		}

		Linkage linkage = new smile.clustering.linkage.UPGMCLinkage(dist);
		HierarchicalClustering cluster = new HierarchicalClustering(linkage);
		this.height = cluster.getHeight();

		/* Cluster analysis */

		List<List<Pin>> clusters = new ArrayList<>();
		List<Node> mergedPins = new ArrayList<>();

		/* First index: 0..n-1 -> time */
		keyvalues = IntStream.range(0, n - 1).mapToObj(i -> new ArrayList<KeyValue>()).collect(Collectors.toList());

		for (int i = 0; i < n - 1; i++) {
			int[] merged = cluster.getTree()[i];
			List<Pin> c = new ArrayList<>();

			if (merged[0] < n) {
				Pin pin = allPins.get(merged[0]);
				c.add(pin);
				for (int j = 0; j < n - 1; j++) {
					keyvalues.get(j).add(new KeyValue(pin.getTopGui().opacityProperty(), j <= i ? 1 : 0,
							Interpolator.EASE_BOTH));
					keyvalues.get(j).add(new KeyValue(pin.getTopGui().visibleProperty(), j <= i,
							Interpolator.EASE_BOTH));
				}
			} else {
				c.addAll(clusters.get(merged[0] - n));
				Node pin = mergedPins.get(merged[0] - n);
				for (int j = 0; j < n - 1; j++) {
					keyvalues.get(j).add(new KeyValue(pin.opacityProperty(), merged[0] - n < j && j <= i ? 1 : 0,
							Interpolator.EASE_BOTH));
					keyvalues.get(j).add(new KeyValue(pin.visibleProperty(), merged[0] - n < j && j <= i,
							Interpolator.EASE_BOTH));
				}
			}

			if (merged[1] < n) {
				Pin pin = allPins.get(merged[1]);
				c.add(pin);
				for (int j = 0; j < n - 1; j++) {
					keyvalues.get(j).add(new KeyValue(pin.getTopGui().opacityProperty(), j <= i ? 1 : 0,
							Interpolator.EASE_BOTH));
					keyvalues.get(j).add(new KeyValue(pin.getTopGui().visibleProperty(), j <= i,
							Interpolator.EASE_BOTH));
				}
			} else {
				c.addAll(clusters.get(merged[1] - n));
				Node pin = mergedPins.get(merged[1] - n);
				for (int j = 0; j < n - 1; j++) {
					keyvalues.get(j).add(new KeyValue(pin.opacityProperty(), merged[1] - n < j && j <= i ? 1 : 0,
							Interpolator.EASE_BOTH));
					keyvalues.get(j).add(new KeyValue(pin.visibleProperty(), merged[1] - n < j && j <= i,
							Interpolator.EASE_BOTH));
				}
			}

			clusters.add(c);

			MergedPin mergedPin = new MergedPin(viewport);
			mergedPin.subPins.setAll(c);
			mergedPin.getTopGui().setOpacity(0);
			mergedPins.add(mergedPin.getTopGui());

			/* add keyvalues for last merged pin here if needed */
		}
		world.getChildren().addAll(mergedPins);

		/*
		 * When regenerating the pins, their opacity is equivalent to level 0 (All pins shown, all merged pins hidden). We trigger an update to
		 * recalculate the level (since our height calculation is invalid now) which will start the animation if the calculated level changes.
		 */
		lastLevel = 0;
		changed(viewport.scaleProperty, viewport.scaleProperty.getValue(), viewport.scaleProperty.getValue());
		if (timeline != null)
			timeline.jumpTo("end");
	}

	int lastLevel = 0;

	@Override
	public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
		double height = 60 / newValue.doubleValue();

		// System.out.println(height + "\t" + Arrays.toString(this.height));

		/* Determine which tree heights to switch between */
		for (int i = 0; i < this.height.length - 1; i++) {
			if (this.height[i] >= height && i != lastLevel) {
				/* The zoom switched between two heights */
				if (timeline != null) {
					timeline.stop();
				}
				timeline = new Timeline(
						new KeyFrame(
								Duration.millis(500),
								null,
								null,
								keyvalues.get(i)));
				timeline.playFromStart();

				lastLevel = i;
				break;
			}
		}
	}
}
