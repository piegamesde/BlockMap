package de.piegames.blockmap.gui;

import java.util.Objects;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

/**
 * This {@link StackPane} wraps a {@link WorldRendererCanvas} together with nodes overlaid on top of it. These nodes are grouped into event
 * classes depending on their functionality.
 * 
 * @see settingsLayers
 * @see pinLayers
 * @see decorationLayers
 */
public class MapPane extends StackPane {

	/**
	 * Nodes placed here are normal children of the stack pane. This is intended to be used for GUI elements that are shown above the map, but
	 * are not affected by the map's position and scale. This set of layers is the topmost one.
	 */
	public final ObservableList<Node>	settingsLayers		= FXCollections.observableArrayList();
	/**
	 * (Mouse) events that reach these nodes are caught, duplicated and sent to all nodes in the decoration layers. This is intended to be used
	 * for GUI elements that are shown on the map (e.g. pins) without interfering with the map's navigation. If node A obstructs node B, events
	 * accepted by node A will be passed on to decoration layers but not to B. This set of layers is between {@link #settingsLayers} and
	 * {@link #decorationLayers}.
	 */
	public final ObservableList<Node>	pinLayers			= FXCollections.observableArrayList();
	/**
	 * Nodes placed here receive all (mouse) events that target the map or any of the {@link #settingsLayers}. This is intended to be used for
	 * decorative map overlays and map navigation through event listener (e.g. selecting an area on the map). Note that since all of the events
	 * that land here are duplicated ones, they won't reach any children of the nodes added here. This set of layers is between
	 * {@link #settingsLayers} and the map.
	 */
	public final ObservableList<Node>	decorationLayers	= FXCollections.observableArrayList();

	protected Region					catchEvents;

	public final WorldRendererCanvas	renderer;

	public MapPane(WorldRendererCanvas renderer) {
		this.renderer = Objects.requireNonNull(renderer);
		catchEvents = new Region();
		ListChangeListener<Node> l = c -> {
			getChildren().clear();
			getChildren().addAll(decorationLayers);
			getChildren().add(catchEvents);
			getChildren().addAll(pinLayers);
			getChildren().addAll(settingsLayers);
		};
		settingsLayers.addListener(l);
		pinLayers.addListener(l);
		decorationLayers.addListener(l);

		catchEvents.setPickOnBounds(true);
		EventHandler<? super Event> refireEvent = event -> decorationLayers.forEach(l1 -> l1.fireEvent(event));
		catchEvents.addEventFilter(EventType.ROOT, refireEvent);
		pinLayers.addListener((ListChangeListener<Node>) e -> {
			while (e.next()) {
				e.getRemoved().forEach(n -> n.removeEventFilter(EventType.ROOT, refireEvent));
				e.getAddedSubList().forEach(n -> n.addEventFilter(EventType.ROOT, refireEvent));
			}
		});

		decorationLayers.add(new CanvasContainer(renderer));
	}
}