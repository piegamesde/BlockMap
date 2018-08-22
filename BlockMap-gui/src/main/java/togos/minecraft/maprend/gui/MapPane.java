package togos.minecraft.maprend.gui;

import java.util.LinkedList;
import java.util.Objects;

import com.sun.javafx.collections.ObservableListWrapper;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventType;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

/***/
@SuppressWarnings("restriction")
public class MapPane extends StackPane {

	/***/
	public final ObservableList<Node> settingsLayers = new ObservableListWrapper<Node>(new LinkedList<>());
	/***/
	public final ObservableList<Node> decorationLayers = new ObservableListWrapper<Node>(new LinkedList<>());

	protected Region catchEvents;

	public final WorldRendererCanvas renderer;

	public MapPane(WorldRendererCanvas renderer) {
		this.renderer = Objects.requireNonNull(renderer);
		catchEvents = new Region();
		ListChangeListener<Node> l = c -> {
			getChildren().clear();
			getChildren().addAll(decorationLayers);
			getChildren().add(catchEvents);
			getChildren().addAll(settingsLayers);
			// System.out.println(Arrays.toString(getChildren().toArray()));
		};
		settingsLayers.addListener(l);
		decorationLayers.addListener(l);

		catchEvents.setPickOnBounds(true);
		catchEvents.addEventFilter(EventType.ROOT, event -> decorationLayers.forEach(l1 -> l1.fireEvent(event)));

		decorationLayers.add(new CanvasContainer(renderer));
	}
}