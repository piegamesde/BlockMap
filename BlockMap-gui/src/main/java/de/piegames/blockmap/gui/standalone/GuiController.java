package de.piegames.blockmap.gui.standalone;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.controlsfx.control.CheckTreeView;
import org.controlsfx.control.StatusBar;
import org.controlsfx.dialog.ExceptionDialog;
import org.joml.Vector2d;
import org.joml.Vector2ic;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

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
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
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
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TitledPane;
import javafx.scene.control.TreeItem;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.util.Duration;
import javafx.util.Pair;

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
	HistoryManager historyManager = new HistoryManager(backgroundThread);

	public GuiController() {
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		log.debug("Initializing GUI");

		renderer = new WorldRendererCanvas();
		root.setCenter(pane = new MapPane(renderer));
		pane.decorationLayers.add(new DragScrollDecoration(renderer.viewport, MouseButton.PRIMARY, 0.01d));
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

		{ /* Status bar initialization */
			statusBar.setSkin(new StatusBarSkin2(statusBar));
			statusBar.progressProperty().bind(renderer.getProgress());
			statusBar.setText(null);
			statusBar.textProperty().bind(renderer.getStatus());
			
			{
				var timeCounter = new SimpleIntegerProperty(0);
				var timeline = new Timeline(1,
						new KeyFrame(Duration.seconds(60), e -> timeCounter.set(timeCounter.intValue() + 1)));
				timeline.setCycleCount(Animation.INDEFINITE);
				timeline.play();

				Label timestampLabel = new Label();
				timestampLabel.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
				timestampLabel.textProperty().bind(Bindings.createStringBinding(() -> {
					if (regionFolder.get() != null) {
						return "Rendered " + DateConverter.toRelative(regionFolder.get().getValue().getTimestamp());
					} else
						return "";
				}, regionFolder, timeCounter));
				statusBar.getRightItems().add(timestampLabel);
			}
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
			/* Reload pins */
			if (val != null)
				this.pins.loadWorld(val.listRegions(), val.getPins().map(pins -> Pin.convertStatic(pins, backgroundThread, renderer.viewport)).orElse(
						Collections.emptySet()));
			else
				this.pins.loadWorld(Collections.emptyList(), Collections.emptyList());

			/*
			 * Change zoom factor for the nether. We're not using the normal zooming API because a transition
			 * animation is not desired while switching worlds. Also normally, zooming is done around the mouse
			 * cursor, but we need it around the world's origin. So we backup the translation beforehand and
			 * then do the necessary calculations manually.
			 */
			boolean wasNether = previous != null && previous.isNether();
			boolean isNether = val != null && val.isNether();
			final double DELTA_ZOOM = 3; /* = ln2(8) */
			if (isNether && !wasNether) {
				var translation = renderer.viewport.translationProperty.get();
				var oldScale = renderer.viewport.scaleProperty.get();
				renderer.viewport.zoomProperty.set(renderer.viewport.zoomProperty.get() + DELTA_ZOOM);
				var newScale = renderer.viewport.scaleProperty.get();
				renderer.viewport.translationProperty.set(translation.mul(oldScale / newScale, new Vector2d()));
			}
			if (wasNether && !isNether) {
				var translation = renderer.viewport.translationProperty.get();
				var oldScale = renderer.viewport.scaleProperty.get();
				renderer.viewport.zoomProperty.set(renderer.viewport.zoomProperty.get() - DELTA_ZOOM);
				var newScale = renderer.viewport.scaleProperty.get();
				renderer.viewport.translationProperty.set(translation.mul(oldScale / newScale, new Vector2d()));
			}
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

	public void load(String input) {
		if (input == null || input.isBlank()) {
			unload();
			return;
		}
		log.info("Loading world from '" + input + "'");

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
						alert.setHeaderText("Could not load world at '" + input + "'");
						alert.showAndWait();
						return;
					}
				} else if (!Files.exists(path.resolve("level.dat"))) {
					Alert alert = new Alert(AlertType.ERROR, "A world folder must contain a level.dat", ButtonType.OK);
					alert.setHeaderText("Could not load world at '" + input + "'");
					alert.showAndWait();
					return;
				}
				/* Load the world */
				try {
					loadLocal(path);
				} catch (RuntimeException e) {
					Alert alert = new Alert(AlertType.ERROR, "Failed to load world – " + e.getMessage(), ButtonType.OK);
					alert.setHeaderText("Could not load world at '" + input + "'");
					alert.showAndWait();
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
			log.debug("'" + input + "' is not a valid URI");
		} catch (IllegalArgumentException e) {
			log.warn("Malformed input uri", e);
			ExceptionDialog d = new ExceptionDialog(e);
			d.setHeaderText("Malformed input");
			d.showAndWait();
			return;
		}

		/* Total failure */
		Alert alert = new Alert(AlertType.ERROR, "Please specify the path to a world or the URL to a server", ButtonType.OK);
		alert.setHeaderText("Could not load world at '" + input + "'");
		alert.showAndWait();
	}

	@FXML
	public void showLoadDialog() {
		try {
			new OpenDialog(historyManager).showAndWait().ifPresent(this::load);
		} catch (Exception e) {
			log.error("Could not show 'open' dialog, please file a bug report", e);
			ExceptionDialog d = new ExceptionDialog(e);
			d.setTitle("Error");
			d.setHeaderText("Could not show 'open' dialog, please file a bug report");
			d.showAndWait();
			return;
		}
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
			String name = regionFolderCached.get()
					.getPins()
					.flatMap(LevelMetadata::getWorldName)
					.orElse(path.getFileName().toString());
			String imageURL = null;
			if (Files.exists(path.resolve("icon.png")))
				imageURL = path.resolve("icon.png").toUri().toString();
			historyManager.onWorldLoaded(new HistoryItem(false, name, path.toAbsolutePath().toString(), imageURL, System.currentTimeMillis()));
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
			String name = serverSettingsController.getMetadata().name.orElse("<unknown server>");
			String imageURL = serverSettingsController.getMetadata().iconLocation.orElse(null);
			historyManager.onWorldLoaded(new HistoryItem(true, name, file.toString(), imageURL, System.currentTimeMillis()));
		}
	}

	@FXML
	public void unload() {
		log.info("Unloading current world");
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