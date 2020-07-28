package de.piegames.blockmap.gui.standalone;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.collect.Streams;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ListBinding;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.util.Callback;
import me.xdrop.fuzzywuzzy.FuzzySearch;

public class OpenDialog extends Dialog<String> implements Initializable {

	private static Log log = LogFactory.getLog(OpenDialog.class);

	@FXML
	private TextField input;
	@FXML
	private ListView<ListItem> worlds;

	private HistoryManager history;

	public OpenDialog(HistoryManager history) throws IOException {
		this.history = Objects.requireNonNull(history);

		setTitle("Open world");
		setResizable(true);
		initModality(Modality.APPLICATION_MODAL);
		final DialogPane dialogPane = getDialogPane();

		FXMLLoader loader = new FXMLLoader(getClass().getResource("opendialog.fxml"));
		loader.setController(this);
		dialogPane.setMinSize(650, 500);
		dialogPane.setContent(loader.load());
		setResizable(true);
		dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

		setResultConverter(buttonType -> {
			if (buttonType == ButtonType.OK) {
				return convert(worlds.getSelectionModel().getSelectedItem());
			}
			return null;
		});
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		worlds.setCellFactory(new Callback<ListView<ListItem>, ListCell<ListItem>>() {

			@Override
			public ListCell<ListItem> call(ListView<ListItem> param) {
				return new MinecraftListCell();
			}
		});
		input.setOnKeyPressed(e -> {
			if (e.getCode() == KeyCode.DOWN) {
				worlds.requestFocus();
				worlds.getSelectionModel().select(0);
			}
		});
		worlds.setOnKeyPressed(e -> {
			if (e.getCode() == KeyCode.UP && worlds.getSelectionModel().getSelectedIndex() == 0) {
				input.requestFocus();
			}
		});

		worlds.setItems(new ListBinding<ListItem>() {

			{
				bind(history.recentWorlds, history.otherWorlds, input.textProperty());
			}

			@Override
			protected ObservableList<ListItem> computeValue() {
				List<ListItem> items = Streams.concat(history.recentWorlds.stream(), history.otherWorlds.stream())
						/* Group all items with same path */
						.collect(Collectors.groupingBy(HistoryItem::getPath))
						.values()
						.stream()
						/* Of those with same path, take only the newest item */
						.map(
								sameItems -> sameItems.stream()
										.max(Comparator.comparingLong(HistoryItem::lastAccessed))
										.get()
						)
						.filter(item -> {
							var text = input.getText();
							if (text.isBlank() || text.length() < 3)
								return true;
							return FuzzySearch.weightedRatio(text, item.getName()) > 70;
						})
						.map(ListHistoryItem::new)
						.collect(Collectors.toList());

				/* Add the special items at the top */
				if (input.getText().contains("/"))
					items.add(0, new LoadTextItem(input.getText()));
				items.add(0, new BrowseHistoryItem());

				return FXCollections.observableArrayList(items);
			}
		}
		);
		worlds.setEditable(true);
		worlds.setOnEditStart(e -> load());

		Platform.runLater(() -> input.requestFocus());
	}

	@FXML
	public void load() {
		setResult(convert(worlds.getSelectionModel().getSelectedItem()));
		close();
	}

	public String convert(ListItem item) {
		if (item == null)
			return null;
		if (item instanceof ListHistoryItem) {
			return ((ListHistoryItem) item).item.path;
		} else if (item instanceof BrowseHistoryItem) {
			return browseFolder();
		} else if (item instanceof LoadTextItem) {
			return ((LoadTextItem) item).text;
		} else {
			throw new InternalError("Unreachable code");
		}
	}

	private String browseFolder() {
		DirectoryChooser dialog = new DirectoryChooser();
		File f = history.getSaveSearchPath().get(0).toFile();
		if (!f.isDirectory())
			f = null;
		dialog.setInitialDirectory(f);
		f = dialog.showDialog(null);
		if (f != null) {
			return f.toString();
		} else {
			return null;
		}
	}

	/* How do I miss Rust's enums in Java … */
	static class ListItem {
	}

	static class ListHistoryItem extends ListItem {
		HistoryItem item;

		private ListHistoryItem(HistoryItem item) {
			this.item = Objects.requireNonNull(item);
		}
	}

	static class BrowseHistoryItem extends ListItem {

	}

	static class LoadTextItem extends ListItem {
		String text;

		private LoadTextItem(String text) {
			this.text = Objects.requireNonNull(text);
		}
	}

	static class MinecraftListCell extends ListCell<ListItem> {

		private ImageView graphic;
		private Label name;
		private Label path;

		public MinecraftListCell() {
			name = new Label();
			name.setAlignment(Pos.CENTER_LEFT);

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
		public void updateItem(ListItem item2, boolean empty) {
			super.updateItem(item2, empty);
			if (item2 instanceof ListHistoryItem) {
				var item = ((ListHistoryItem) item2).item;
				name.setStyle("-fx-font-weight: bold;");
				if (empty) {
					name.setText(null);
					path.setText(null);
					setText(null);
					graphic.setImage(null);
				} else {
					graphic.setImage(
							item.iconURL.map(url -> new Image(url, 20, 20, true, false, true))
									.orElse(new Image(getClass().getResourceAsStream("/unknown_server.png")))
					);
					name.setText(item.name);
					path.setText(item.path);
				}
			} else if (item2 instanceof BrowseHistoryItem) {
				graphic.setImage(null);
				name.setStyle("-fx-font-style: italic;");
				name.setText("Browse folder");
				path.setText(null);
			} else if (item2 instanceof LoadTextItem) {
				var item = ((LoadTextItem) item2);
				graphic.setImage(null);
				name.setStyle("-fx-font-style: italic;");
				name.setText("Open");
				path.setText(item.text);
			} else {
				graphic.setImage(null);
				name.setText(null);
				path.setText(null);
			}
		}

		@Override
		public void startEdit() {
			super.startEdit();
			super.cancelEdit();
		}
	}
}
