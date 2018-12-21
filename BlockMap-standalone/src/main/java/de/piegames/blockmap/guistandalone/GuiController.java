package de.piegames.blockmap.guistandalone;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ResourceBundle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.controlsfx.control.RangeSlider;
import org.controlsfx.control.StatusBar;

import de.piegames.blockmap.DotMinecraft;
import de.piegames.blockmap.RegionFolder;
import de.piegames.blockmap.color.BlockColorMap;
import de.piegames.blockmap.gui.MapPane;
import de.piegames.blockmap.gui.WorldRendererCanvas;
import de.piegames.blockmap.gui.decoration.DragScrollDecoration;
import de.piegames.blockmap.gui.decoration.GridDecoration;
import de.piegames.blockmap.gui.decoration.Pin;
import de.piegames.blockmap.gui.decoration.PinDecoration;
import de.piegames.blockmap.guistandalone.RegionFolderProvider.LocalFolderProvider;
import de.piegames.blockmap.guistandalone.RegionFolderProvider.RemoteFolderProvider;
import de.piegames.blockmap.renderer.RegionRenderer;
import de.piegames.blockmap.renderer.RegionShader;
import de.piegames.blockmap.renderer.RenderSettings;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

public class GuiController implements Initializable {

	private static Log								log						= LogFactory.getLog(GuiController.class);

	protected RegionRenderer						regionRenderer;
	public WorldRendererCanvas						renderer;
	protected ObjectProperty<RegionFolder>			regionFolder			= new SimpleObjectProperty<>();
	protected ObjectProperty<RegionFolderProvider>	regionFolderProvider	= new SimpleObjectProperty<>();

	protected Path									lastBrowsedPath;
	protected URL									lastBrowsedURL;

	@FXML
	private BorderPane								root;
	@FXML
	private Button									browseButton;
	@FXML
	private StatusBar								statusBar;
	@FXML
	private Label									minHeight, maxHeight;
	@FXML
	private RangeSlider								heightSlider;
	@FXML
	private HBox									regionSettings;
	@FXML
	private ChoiceBox<String>						shadingBox;
	@FXML
	private ChoiceBox<String>						colorBox;
	@FXML
	private CheckBox								gridBox;

	protected MapPane								pane;
	protected ObjectProperty<Path>					currentPath				= new SimpleObjectProperty<>();
	protected PinDecoration							pins;

	public GuiController() {
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		log.debug("Initializing GUI");
		RenderSettings settings = new RenderSettings();
		settings.loadDefaultColors();
		regionRenderer = new RegionRenderer(settings);

		renderer = new WorldRendererCanvas(null);
		root.setCenter(pane = new MapPane(renderer));
		pane.decorationLayers.add(new DragScrollDecoration(renderer.viewport));
		GridDecoration grid = new GridDecoration(renderer.viewport);
		pane.decorationLayers.add(grid);
		grid.visibleProperty().bind(gridBox.selectedProperty());
		pins = new PinDecoration(renderer.viewport);
		pane.decorationLayers.add(pins);

		{
			statusBar.setSkin(new StatusBarSkin2(statusBar));
			statusBar.progressProperty().bind(renderer.getProgress());
			statusBar.setText(null);
			statusBar.textProperty().bind(renderer.getStatus());

			Label pathLabel = new Label();
			pathLabel.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
			pathLabel.textProperty().bind(Bindings.createStringBinding(
					() -> regionFolderProvider.get() == null ? "" : regionFolderProvider.get().getLocation(),
					regionFolderProvider));
			statusBar.getLeftItems().add(pathLabel);

			Label mouseLabel = new Label();
			mouseLabel.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
			mouseLabel.textProperty().bind(Bindings.createStringBinding(
					() -> "(" + (int) renderer.viewport.mouseWorldProperty.get().x() + ", " + (int) renderer.viewport.mouseWorldProperty.get().y() + ")",
					renderer.viewport.mouseWorldProperty));
			statusBar.getRightItems().add(mouseLabel);
		}

		minHeight.textProperty().bind(Bindings.format("Min: %3.0f", heightSlider.lowValueProperty()));
		maxHeight.textProperty().bind(Bindings.format("Max: %3.0f", heightSlider.highValueProperty()));
		ChangeListener<? super Boolean> heightListener = (e, oldVal, newVal) -> {
			if (oldVal && !newVal) {
				if (e == heightSlider.lowValueChangingProperty())
					settings.minY = (int) Math.round(heightSlider.lowValueProperty().getValue().doubleValue());
				else if (e == heightSlider.highValueChangingProperty())
					settings.maxY = (int) Math.round(heightSlider.highValueProperty().getValue().doubleValue());
				renderer.invalidateTextures();
				renderer.repaint();
			}
		};
		heightSlider.lowValueChangingProperty().addListener(heightListener);
		heightSlider.highValueChangingProperty().addListener(heightListener);

		colorBox.valueProperty().addListener((observer, old, value) -> {
			settings.blockColors = BlockColorMap
					.loadInternal(new String[] { "default", "caves", "foliage", "water" }[colorBox.getSelectionModel().getSelectedIndex()]);
			renderer.invalidateTextures();
			renderer.repaint();
		});
		shadingBox.valueProperty().addListener((observer, old, value) -> {
			settings.shader = RegionShader.DEFAULT_SHADERS[shadingBox.getSelectionModel().getSelectedIndex()];
			renderer.invalidateTextures();
			renderer.repaint();
		});

		regionFolderProvider.addListener((observable, previous, val) -> {
			regionSettings.getChildren().clear();
			if (val == null) {
				regionFolder.unbind();
				regionFolder.set(null);
			} else {
				regionFolder.bind(val.folderProperty());
				regionSettings.getChildren().addAll(val.getGUI());
			}
			boolean disabled = val == null ? true : val.hideSettings();
			heightSlider.setDisable(disabled);
			colorBox.setDisable(disabled);
			shadingBox.setDisable(disabled);
		});
		regionFolderProvider.set(null); /* Force listener update */
		renderer.regionFolder.bind(regionFolder);
		renderer.regionFolder.addListener((observable, previous, val) -> {
			if (val != null)
				pins.pins.set(FXCollections.observableList(Pin.convert(val.getPins())));
		});
	}

	@FXML
	public void browseFolder() {
		DirectoryChooser dialog = new DirectoryChooser();
		File f = (lastBrowsedPath == null) ? DotMinecraft.DOTMINECRAFT.resolve("saves").toFile() : lastBrowsedPath.getParent().toFile();
		if (!f.isDirectory())
			f = DotMinecraft.DOTMINECRAFT.resolve("saves").toFile();
		if (!f.isDirectory())
			f = null;
		dialog.setInitialDirectory(f);
		f = dialog.showDialog(null);
		if (f != null) {
			lastBrowsedPath = f.toPath();
			regionFolderProvider.set(RegionFolderProvider.byPath(lastBrowsedPath, regionRenderer));
		}
	}

	@FXML
	public void browseFile() {
		FileChooser dialog = new FileChooser();
		File f = (lastBrowsedPath == null) ? DotMinecraft.DOTMINECRAFT.resolve("saves").toFile() : lastBrowsedPath.getParent().toFile();
		if (!f.isDirectory())
			f = DotMinecraft.DOTMINECRAFT.resolve("saves").toFile();
		if (!f.isDirectory())
			f = null;
		dialog.setInitialDirectory(f);
		f = dialog.showOpenDialog(null);
		if (f != null) {
			lastBrowsedPath = f.toPath();
			regionFolderProvider.set(new LocalFolderProvider(lastBrowsedPath));
		}
	}

	@FXML
	public void loadRemote() {
		TextInputDialog dialog = new TextInputDialog();
		dialog.setTitle("Load remote world");
		dialog.setHeaderText("Enter the URL to the remote world you want to load");
		dialog.setGraphic(null);
		dialog.showAndWait().ifPresent(s -> {
			try {
				regionFolderProvider.set(new RemoteFolderProvider(new URI(s)));
			} catch (URISyntaxException | IllegalArgumentException e) {
				log.warn("Malformed input uri", e);
			}
		});
	}

	@FXML
	public void reloadWorld() {
		if (regionFolderProvider.get() != null)
			regionFolderProvider.get().reload();
	}

	@FXML
	public void unload() {
		regionFolderProvider.set(null);
	}

	public void load(RegionFolderProvider world) {
		regionFolderProvider.set(world);
	}

	@FXML
	public void exit() {
		Platform.exit();
	}
}