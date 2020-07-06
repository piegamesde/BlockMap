package de.piegames.blockmap.gui.standalone;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.controlsfx.control.CheckTreeView;
import org.controlsfx.control.StatusBar;
import org.controlsfx.control.textfield.AutoCompletionBinding;
import org.controlsfx.control.textfield.TextFields;
import org.controlsfx.dialog.ExceptionDialog;
import org.joml.Vector2ic;

import com.google.common.collect.Streams;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import de.piegames.blockmap.DotMinecraft;
import de.piegames.blockmap.gui.MapPane;
import de.piegames.blockmap.gui.WorldRendererCanvas;
import de.piegames.blockmap.gui.decoration.DragScrollDecoration;
import de.piegames.blockmap.gui.decoration.GridDecoration;
import de.piegames.blockmap.gui.decoration.Pin;
import de.piegames.blockmap.gui.decoration.Pin.PinType;
import de.piegames.blockmap.gui.decoration.PinDecoration;
import de.piegames.blockmap.gui.decoration.ScaleDecoration;
import de.piegames.blockmap.gui.standalone.about.AboutDialog;
import de.piegames.blockmap.world.ChunkMetadata;
import de.piegames.blockmap.world.LevelMetadata;
import de.piegames.blockmap.world.RegionFolder;
import de.piegames.nbt.CompoundTag;
import de.piegames.nbt.stream.NBTInputStream;
import impl.org.controlsfx.skin.AutoCompletePopup;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TitledPane;
import javafx.scene.control.TreeItem;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.stage.DirectoryChooser;
import javafx.util.Callback;
import javafx.util.Duration;
import javafx.util.Pair;
import javafx.util.StringConverter;
import me.xdrop.fuzzywuzzy.FuzzySearch;
import me.xdrop.fuzzywuzzy.model.BoundExtractedResult;

public class GuiController implements Initializable {

	private static Log log = LogFactory.getLog(GuiController.class);

	static enum WorldType {
		LOCAL, REMOTE, NONE;
	}

	public WorldRendererCanvas renderer;
	protected WorldType loaded = WorldType.NONE;
	protected ObjectProperty<Pair<String, RegionFolder>> regionFolder = new SimpleObjectProperty<>();
	protected ObjectProperty<RegionFolder> regionFolderCached = new SimpleObjectProperty<>();

	@FXML
	private BorderPane root;

	/* Top */

	@FXML
	protected TextField worldInput;
	/** Recently loaded worlds and servers */
	private List<HistoryItem> recentWorlds = new LinkedList<>();
	/** Whatever found in {@code .minecraft} for autocomplete purposes */
	private List<HistoryItem> otherWorlds = new LinkedList<>();

	/* Bottom */

	@FXML
	private StatusBar statusBar;

	/* Other (external) settings */
	@FXML
	protected TitledPane worldSettings;
	@FXML
	protected GuiControllerWorld worldSettingsController;
	@FXML
	protected TitledPane serverSettings;
	@FXML
	protected GuiControllerServer serverSettingsController;

	/* View settings */

	@FXML
	private TitledPane viewSettings;
	@FXML
	private CheckBox gridBox;
	@FXML
	private CheckBox scaleBox;
	@FXML
	public CheckBox pinBox;
	@FXML
	public CheckTreeView<PinType> pinView;
	public Map<PinType, TreeItem<PinType>> checkedPins = new HashMap<>();

	protected MapPane pane;
	public PinDecoration pins;

	protected ScheduledExecutorService backgroundThread = Executors
			.newSingleThreadScheduledExecutor(
			new ThreadFactoryBuilder().setNameFormat("pin-background-thread-%d").build());
	RegionFolderCache cache = new RegionFolderCache();

	public GuiController() {
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		log.debug("Initializing GUI");

		renderer = new WorldRendererCanvas();
		root.setCenter(pane = new MapPane(renderer));
		pane.decorationLayers.add(new DragScrollDecoration(renderer.viewport));
		{
			GridDecoration grid = new GridDecoration(renderer.viewport);
			pane.decorationLayers.add(grid);
			grid.visibleProperty().bind(gridBox.selectedProperty());
		}
		{
			ScaleDecoration scale = new ScaleDecoration(renderer.viewport);
			pane.settingsLayers.add(scale);
			scale.visibleProperty().bind(scaleBox.selectedProperty());
		}
		pins = new PinDecoration(renderer.viewport);
		pane.pinLayers.add(pins);

		/* Load list of worlds in .minecraft/saves (in background) */
		backgroundThread.execute(() -> {
			Path saves = DotMinecraft.DOTMINECRAFT.resolve("saves");
			if (Files.exists(saves) && Files.isDirectory(saves))
				try {
					List<HistoryItem> toAdd = Files.list(saves)
							.filter(Files::exists)
							.filter(Files::isDirectory)
							.filter(p -> Files.exists(p.resolve("level.dat")))
							.map(save -> {
								String name = save.getFileName().toString();
								long timestamp = 0;
								try (NBTInputStream in = new NBTInputStream(Files.newInputStream(save.resolve("level.dat")), NBTInputStream.GZIP_COMPRESSION)) {
									Optional<CompoundTag> data = in.readTag().getAsCompoundTag().flatMap(t -> t.getAsCompoundTag("Data"));
									name = data.flatMap(t -> t.getStringValue("LevelName")).orElse(null);
									timestamp = data.flatMap(t -> t.getLongValue("LastPlayed")).orElse(0L);
								} catch (IOException e) {
									log.warn("Could not read world name for " + save, e);
								}

								String imageURL = null;
								if (Files.exists(save.resolve("icon.png")))
									imageURL = save.resolve("icon.png").toUri().toString();
								return new HistoryItem(false, name, save.toAbsolutePath().toString(), imageURL, timestamp);
							})
							.sorted(Comparator.comparingLong(HistoryItem::lastAccessed).reversed())
							.collect(Collectors.toList());
					Platform.runLater(() -> otherWorlds.addAll(toAdd));
				} catch (IOException e) {
					log.warn("Could not load worlds from saves folder", e);
				}
		});
		{/* Input auto completion */
			/* TODO tweak once history saving is implemented */
			AutoCompletionBinding<HistoryItem> autoComplete = TextFields.bindAutoCompletion(worldInput, request -> {
				String text = request.getUserText();
				if (text.length() < 3)
					return Streams.concat(recentWorlds.stream().limit(5), otherWorlds.stream()).collect(Collectors.toList());
				return Streams.concat(
						FuzzySearch.extractAll(text, recentWorlds, HistoryItem::getName, 20).stream()
								.sorted()
								.limit(5)
								.map(BoundExtractedResult::getReferent)
								.sorted(Comparator.comparingLong(HistoryItem::lastAccessed).reversed()),
						FuzzySearch.extractAll(text, otherWorlds, HistoryItem::getName, 50)
								.stream()
								.map(BoundExtractedResult::getReferent)
								.sorted(Comparator.comparingLong(HistoryItem::lastAccessed).reversed()))
						.collect(Collectors.toList());
			},
					new StringConverter<HistoryItem>() {

						@Override
						public String toString(HistoryItem object) {
							return object.path;
						}

						@Override
						public HistoryItem fromString(String string) {
							return null;
						}
					});
			try {
				/* Access a private field (the actual popup) to set a custom skin */
				Field field = AutoCompletionBinding.class.getDeclaredField("autoCompletionPopup");
				field.setAccessible(true);
				@SuppressWarnings("unchecked")
				AutoCompletePopup<HistoryItem> popup = (AutoCompletePopup<HistoryItem>) field.get(autoComplete);
				popup.setSkin(new AutoCompletePopupSkin2<HistoryItem>(popup, new Callback<ListView<HistoryItem>, ListCell<HistoryItem>>() {

					@Override
					public ListCell<HistoryItem> call(ListView<HistoryItem> param) {
						return new AutoCompleteItem();
					}
				}));
			} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e1) {
				e1.printStackTrace();
			}

			autoComplete.maxWidthProperty().bind(worldInput.widthProperty());
			autoComplete.prefWidthProperty().bind(worldInput.widthProperty());

			worldInput.focusedProperty().addListener((property, old, val) -> {
				if (!old && val && worldInput.getText().isBlank())
					new Timeline(new KeyFrame(Duration.seconds(1), e -> {
						if (worldInput.isFocused())
							autoComplete.setUserInput("");
					})).play();
			});
		}

		{ /* Status bar initialization */
			statusBar.setSkin(new StatusBarSkin2(statusBar));
			statusBar.progressProperty().bind(renderer.getProgress());
			statusBar.setText(null);
			statusBar.textProperty().bind(renderer.getStatus());

			Label zoomLabel = new Label();
			zoomLabel.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
			zoomLabel.textProperty().bind(Bindings.createStringBinding(() -> {
				double scale = renderer.viewport.scaleProperty.get();
				boolean zoomIn = false;
				if (scale < 1) {
					zoomIn = true;
					scale = 1 / scale;
				}
				String text = scale < 3 ? Double.toString((int) (scale * 10) / 10d) : Integer.toString((int) scale);
				if (zoomIn)
					return "1:" + text;
				else
					return text + ":1";
			}, renderer.viewport.scaleProperty));
			statusBar.getRightItems().add(zoomLabel);

			Label mouseLabel = new Label();
			mouseLabel.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
			mouseLabel.textProperty().bind(Bindings.createStringBinding(
					() -> "(" + (int) renderer.viewport.mouseWorldProperty.get().x() + ", " + (int) renderer.viewport.mouseWorldProperty.get().y() + ")",
					renderer.viewport.mouseWorldProperty));
			statusBar.getRightItems().add(mouseLabel);
		}

		{ /* Pin checkbox icon */
			ImageView image = new ImageView(PinType.ANY_PIN.image);
			image.fitHeightProperty().bind(Bindings.createDoubleBinding(() -> pinBox.getFont().getSize() * 1.5, pinBox.fontProperty()));
			image.setSmooth(true);
			image.setPreserveRatio(true);
			pinBox.setGraphic(image);
		}
		{ /* Pin tree */
			initPinCheckboxes(PinType.ANY_PIN, null, pinView);
			pinView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
			/* Map the set of selected tree items to pins.visiblePins */
			pins.visiblePins.bind(Bindings.createObjectBinding(() -> pinBox.isSelected() ? pinView.getCheckModel().getCheckedItems().stream().map(t -> t
					.getValue()).collect(Collectors.toCollection(FXCollections::observableSet)) : FXCollections.emptyObservableSet(), pinView.getCheckModel()
							.getCheckedItems(), pinBox.selectedProperty()));
			/*
			 * Disable the pin view if either pins are disabled or settings are disabled (indicated through pinBox.disabledProperty, which is set in the
			 * following code block).
			 */
			pinView.disableProperty().bind(Bindings.createBooleanBinding(() -> pinBox.isDisabled() || !pinBox.isSelected(), pinBox.selectedProperty(), pinBox
					.disabledProperty()));
		}

		/* Cache wrapper */
		regionFolder.addListener((ChangeListener<? super Pair<String, RegionFolder>>) (e, old, val) -> {
			if (old != null)
				cache.releaseCache(old.getKey());
			if (val != null)
				regionFolderCached.set(cache.cache(val.getValue(), val.getKey()));
			else
				regionFolderCached.set(null);
			renderer.repaint();
		});

		renderer.regionFolder.bind(regionFolderCached);
		renderer.regionFolder.addListener((observable, previous, val) -> {
			if (val != null)
				this.pins.loadWorld(val.listRegions(), val.getPins().map(pins -> Pin.convertStatic(pins, backgroundThread, renderer.viewport)).orElse(
						Collections.emptySet()));
			else
				this.pins.loadWorld(Collections.emptyList(), Collections.emptyList());
		});
		renderer.getChunkMetadata().addListener((MapChangeListener<Vector2ic, Map<Vector2ic, ChunkMetadata>>) change -> {
			if (change.getValueAdded() != null)
				GuiController.this.pins.loadRegion(change.getKey(), Pin.convertDynamic(change.getValueAdded(), renderer.viewport));
		});
	}

	/**
	 * Recursive pre-order traversal of the pin type hierarchy tree. Generated items are added automatically.
	 *
	 * @param type
	 *            the current type to add
	 * @param parent
	 *            the parent tree item to add this one to. <code>null</code> if {@code type} is the root type, in this case the generated tree
	 *            item will be used as root for the tree directly.
	 * @param tree
	 *            the tree containing the items
	 */
	private void initPinCheckboxes(PinType type, CheckBoxTreeItem<PinType> parent, CheckTreeView<PinType> tree) {
		ImageView image = new ImageView(type.image);
		/*
		 * The only way so set the size of an image relative to the text of the label is to bind its height to a font size. Since tree items don't
		 * possess a fontProperty (it's hidden behind a cell factory implementation), we have to use the next best labeled node (pinBox in this
		 * case). This will only work if we don't change any font sizes.
		 */
		image.fitHeightProperty().bind(Bindings.createDoubleBinding(() -> pinBox.getFont().getSize() * 1.5, pinBox.fontProperty()));
		image.setSmooth(true);
		image.setPreserveRatio(true);
		CheckBoxTreeItem<PinType> ret = new CheckBoxTreeItem<>(type, image);

		if (parent == null)
			tree.setRoot(ret);
		else
			parent.getChildren().add(ret);

		for (PinType sub : type.getChildren())
			initPinCheckboxes(sub, ret, tree);

		ret.setExpanded(type.expandedByDefault);
		if (type.selectedByDefault) {
			pins.visiblePins.add(type);
			tree.getCheckModel().check(ret);
		}
		checkedPins.put(type, ret);
	}

	public void load() {
		String input = worldInput.getText();
		if (input.isBlank()) {
			unload();
			return;
		}

		/* Try to load it as local world first */
		try {
			/* Try parsing as local world folder */
			Path path = Paths.get(input);
			/* Make sure path is an existing directory containing a level.dat. Show an error message otherwise. */
			if (Files.exists(path)) {
				if (!Files.isDirectory(path)) {
					if (path.getFileName().toString().equals("level.dat"))
						path = path.getParent();
					else {
						Alert alert = new Alert(AlertType.ERROR, "Path to a world must either be a folder or a level.dat file", ButtonType.OK);
						alert.showAndWait();
						worldInput.selectAll();
						return;
					}
				} else if (!Files.exists(path.resolve("level.dat"))) {
					Alert alert = new Alert(AlertType.ERROR, "A world folder must contain a level.dat", ButtonType.OK);
					alert.showAndWait();
					worldInput.selectAll();
					return;
				}
				/* Load the world */
				try {
					loadLocal(path);
				} catch (RuntimeException e) {
					Alert alert = new Alert(AlertType.ERROR, "Failed to load world – " + e.getMessage(), ButtonType.OK);
					alert.showAndWait();
					worldInput.selectAll();
				}
				return;
			}
		} catch (InvalidPathException e) {
		}

		/* Try to parse as server URI */
		try {
			loadRemote(new URI(input));
			return;
		} catch (URISyntaxException e) {
		}

		/* Total failure */
		Alert alert = new Alert(AlertType.ERROR, "Please specify the path to a world or the URL to a server", ButtonType.OK);
		alert.showAndWait();
		worldInput.selectAll();
	}

	@FXML
	public void showFolderDialog() {
		DirectoryChooser dialog = new DirectoryChooser();
		File f = (worldSettingsController.lastBrowsedPath == null) ? DotMinecraft.DOTMINECRAFT.resolve("saves").toFile()
				: worldSettingsController.lastBrowsedPath.getParent().toFile();
		if (!f.isDirectory())
			f = DotMinecraft.DOTMINECRAFT.resolve("saves").toFile();
		if (!f.isDirectory())
			f = null;
		dialog.setInitialDirectory(f);
		f = dialog.showDialog(null);
		if (f != null) {
			worldSettingsController.lastBrowsedPath = f.toPath();
			loadLocal(worldSettingsController.lastBrowsedPath);
			worldInput.setText(f.toString());
		}
	}

	@FXML
	public void showUrlDialog() {
		TextInputDialog dialog = new TextInputDialog();
		dialog.setTitle("Load remote world");
		dialog.setHeaderText("Enter the URL to the remote world you want to load");
		dialog.setGraphic(null);
		dialog.setResult(serverSettingsController.lastBrowsedURL);
		dialog.showAndWait().ifPresent(s -> {
			try {
				loadRemote(new URI(s));
				serverSettingsController.lastBrowsedURL = s;
				worldInput.setText(s);
			} catch (URISyntaxException | IllegalArgumentException e) {
				log.warn("Malformed input uri", e);
				ExceptionDialog d = new ExceptionDialog(e);
				d.setTitle("Malformed input");
				d.showAndWait();
			}
		});
	}

	@FXML
	public void reloadWorld() {
		switch (loaded) {
		case LOCAL:
			worldSettingsController.reload();
			break;
		case REMOTE:
			serverSettingsController.reload();
			break;
		default:
		}
	}

	public void loadLocal(Path path) {
		worldSettingsController.load(path);

		worldSettings.setDisable(false);
		serverSettings.setExpanded(false);
		worldSettings.setExpanded(true);
		serverSettings.setDisable(true);

		regionFolder.bind(worldSettingsController.folderProperty());
		loaded = WorldType.LOCAL;

		{ /* Update history */
			recentWorlds.removeIf(w -> w.path.equals(path.toAbsolutePath().toString()));
			String name = regionFolderCached.get()
					.getPins()
					.flatMap(LevelMetadata::getWorldName)
					.orElse(path.getFileName().toString());
			String imageURL = null;
			if (Files.exists(path.resolve("icon.png")))
				imageURL = path.resolve("icon.png").toUri().toString();
			recentWorlds.add(0, new HistoryItem(false, name, path.toAbsolutePath().toString(), imageURL, System.currentTimeMillis()));
		}
	}

	public void loadRemote(URI file) {
		serverSettingsController.load(file);

		serverSettings.setDisable(false);
		worldSettings.setExpanded(false);
		serverSettings.setExpanded(true);
		worldSettings.setDisable(true);

		regionFolder.bind(serverSettingsController.folderProperty());
		loaded = WorldType.REMOTE;

		if (serverSettingsController.getMetadata() != null) { /* Update history */
			recentWorlds.removeIf(w -> w.path.equals(file.toString()));
			String name = serverSettingsController.getMetadata().name.orElse("<unknown server>");
			String imageURL = serverSettingsController.getMetadata().iconLocation.orElse(null);
			recentWorlds.add(0, new HistoryItem(true, name, file.toString(), imageURL, System.currentTimeMillis()));
		}
	}

	@FXML
	public void unload() {
		serverSettings.setDisable(true);
		serverSettings.setExpanded(false);
		worldSettings.setDisable(true);
		worldSettings.setExpanded(false);

		loaded = WorldType.NONE;
		regionFolder.unbind();
		regionFolder.setValue(null);
	}

	@FXML
	public void exit() {
		Platform.exit();
	}

	@FXML
	public void showAbout() {
		try {
			new AboutDialog().showAndWait();
		} catch (Exception e) {
			log.error("Could not show 'about' dialog, please file a bug report", e);
			ExceptionDialog d = new ExceptionDialog(e);
			d.setTitle("Error");
			d.setHeaderText("Could not show 'about' dialog, please file a bug report");
			d.showAndWait();
		}
	}

	public void shutDown() {
		renderer.shutDown();
		backgroundThread.shutdownNow();
		try {
			backgroundThread.awaitTermination(1, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
			log.warn("Background thread did not finish", e);
		}
	}
}