package de.piegames.blockmap.gui.standalone;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.OverrunStyle;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

public class AutoCompleteItem extends ListCell<HistoryItem> {

	private ImageView	graphic;
	private Label		name;
	private Label		path;

	public AutoCompleteItem() {
		name = new Label();
		name.setAlignment(Pos.CENTER_LEFT);
		name.setStyle("-fx-font-weight: bold;");

		path = new Label();
		path.setMaxWidth(Double.POSITIVE_INFINITY);
		HBox.setHgrow(path, Priority.ALWAYS);
		HBox.setMargin(path, new Insets(0, 8, 0, 0));
		path.setAlignment(Pos.CENTER_RIGHT);
		path.setTextOverrun(OverrunStyle.CENTER_ELLIPSIS);

		graphic = new ImageView();
		graphic.setPreserveRatio(true);
		graphic.fitHeightProperty().bind(Bindings.createDoubleBinding(() -> name.getFont().getSize() * 1.2, name.fontProperty()));

		setText(null);
		setGraphic(new HBox(2, graphic, name, path));
		setPrefWidth(0);
	}

	/** {@inheritDoc} */
	@Override
	public void updateItem(HistoryItem item, boolean empty) {
		super.updateItem(item, empty);
		if (empty) {
			name.setText(null);
			path.setText(null);
			setText(null);
			graphic.setImage(null);
		} else {
			graphic.setImage(item.iconURL.map(url -> new Image(url, 20, 20, true, false, true))
					.orElse(new Image(getClass().getResourceAsStream("/unknown_server.png"))));
			name.setText(item.name);
			path.
			setText(item.path);
		}
	}
}
