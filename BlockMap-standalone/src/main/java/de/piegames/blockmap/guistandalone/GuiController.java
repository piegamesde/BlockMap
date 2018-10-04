package de.piegames.blockmap.guistandalone;

import java.io.File;
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
import de.piegames.blockmap.renderer.RegionRenderer;
import de.piegames.blockmap.renderer.RegionShader;
import de.piegames.blockmap.renderer.RenderSettings;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.DirectoryChooser;

public class GuiController implements Initializable {

	private static Log						log				= LogFactory.getLog(RegionRenderer.class);

	public WorldRendererCanvas				renderer;

	@FXML
	private BorderPane						root;
	@FXML
	private Button							browseButton;
	@FXML
	private StatusBar						statusBar;
	@FXML
	private Label							minHeight, maxHeight;
	@FXML
	private RangeSlider						heightSlider;
	@FXML
	private HBox							regionSettings;
	@FXML
	private ChoiceBox<String>				shadingBox;
	@FXML
	private ChoiceBox<String>				colorBox;

	protected MapPane						pane;
	protected ObjectProperty<Path>			currentPath		= new SimpleObjectProperty<>();
	protected ObjectProperty<RegionFolder>	regionFolder	= new SimpleObjectProperty<>();

	public GuiController() {
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		log.debug("Initializing GUI");
		RenderSettings settings = new RenderSettings();
		settings.loadDefaultColors();
		renderer = new WorldRendererCanvas(new RegionRenderer(settings));
		root.setCenter(pane = new MapPane(renderer));
		pane.decorationLayers.add(new DragScrollDecoration(renderer.viewport));

		currentPath.addListener(e -> reloadWorld());

		// statusBar.textProperty().bind(renderer.getStatus());
		statusBar.setText(null);
		statusBar.progressProperty().bind(renderer.getProgress());
		statusBar.setSkin(new StatusBarSkin2(statusBar));
		Label pathLabel = new Label();
		pathLabel.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
		statusBar.textProperty().bind(renderer.getStatus());
		pathLabel.textProperty().bind(Bindings.createStringBinding(() -> currentPath.get() == null ? "" : currentPath.get().toString(), currentPath));
		statusBar.getLeftItems().add(pathLabel);

		minHeight.textProperty().bind(Bindings.format("Min: %3.0f", heightSlider.lowValueProperty()));
		maxHeight.textProperty().bind(Bindings.format("Max: %3.0f", heightSlider.highValueProperty()));
		ChangeListener<? super Boolean> heightListener = (e, oldVal, newVal) -> {
			if (oldVal && !newVal) {
				if (e == heightSlider.lowValueChangingProperty())
					renderer.getRegionRenderer().settings.minY = (int) Math.round(heightSlider.lowValueProperty().getValue().doubleValue());
				else if (e == heightSlider.highValueChangingProperty())
					renderer.getRegionRenderer().settings.maxY = (int) Math.round(heightSlider.highValueProperty().getValue().doubleValue());
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
			System.out.println(settings.shader);
			renderer.invalidateTextures();
			renderer.repaint();
		});

		regionFolder.addListener((observable, previous, val) -> renderer.loadWorld(val));
	}

	@FXML
	public void reloadWorld() {
		RegionFolderProvider folder = RegionFolderProvider.byPath(currentPath.get());
		regionFolder.bind(folder.folderProperty());
		regionSettings.getChildren().clear();
		regionSettings.getChildren().addAll(folder.getGUI());
	}

	@FXML
	public void browse() {
		DirectoryChooser dialog = new DirectoryChooser();
		File f = (currentPath.get() == null) ? DotMinecraft.DOTMINECRAFT.resolve("saves").toFile() : currentPath.get().getParent().toFile();
		if (!f.isDirectory())
			f = DotMinecraft.DOTMINECRAFT.resolve("saves").toFile();
		if (!f.isDirectory())
			f = null;
		dialog.setInitialDirectory(f);
		f = dialog.showDialog(null);
		if (f != null)
			currentPath.set(f.toPath());
	}

	@FXML
	public void exit() {
		Platform.exit();
	}
}