package de.piegames.blockmap.gui.standalone;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.controlsfx.dialog.ExceptionDialog;

import de.piegames.blockmap.MinecraftDimension;
import de.piegames.blockmap.color.BiomeColorMap;
import de.piegames.blockmap.color.BlockColorMap.InternalColorMap;
import de.piegames.blockmap.renderer.RegionRenderer;
import de.piegames.blockmap.renderer.RegionShader;
import de.piegames.blockmap.renderer.RenderSettings;
import de.piegames.blockmap.world.RegionFolder;
import de.piegames.blockmap.world.RegionFolder.WorldRegionFolder;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.GridPane;
import javafx.util.Pair;
import javafx.util.StringConverter;
import javafx.util.converter.IntegerStringConverter;

public class GuiControllerWorld implements Initializable {

	private static Log											log				= LogFactory.getLog(GuiControllerWorld.class);

	@FXML
	GridPane													content;
	@FXML
	TextField													minHeight, maxHeight;
	@FXML
	ChoiceBox<String>											shadingBox;
	@FXML
	ChoiceBox<String>											colorBox;
	@FXML
	ChoiceBox<MinecraftDimension>								dimensionBox;

	/* The String is a hash code used for caching */
	protected ReadOnlyObjectWrapper<Pair<String, RegionFolder>>	folder			= new ReadOnlyObjectWrapper<>();
	protected Path												worldPath;
	protected Path												lastBrowsedPath;
	protected RenderSettings									settings;
	protected List<MinecraftDimension>							available;

	private ChangeListener<Object>								reloadListener	= (observer, old, value) -> {
																					if (old != value)
																						reload();
																				};

	public ReadOnlyObjectProperty<Pair<String, RegionFolder>> folderProperty() {
		return folder.getReadOnlyProperty();
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		available = new ArrayList<>(3);

		// TODO add range checks
		minHeight.setTextFormatter(new TextFormatter<>(new IntegerStringConverter(), 0));
		maxHeight.setTextFormatter(new TextFormatter<>(new IntegerStringConverter(), 255));
		minHeight.textProperty().addListener(new WeakInvalidationListener(e -> reload()));
		maxHeight.textProperty().addListener(new WeakInvalidationListener(e -> reload()));

		colorBox.valueProperty().addListener(new WeakChangeListener<>(reloadListener));
		shadingBox.valueProperty().addListener(new WeakChangeListener<>(reloadListener));

		dimensionBox.setConverter(new StringConverter<MinecraftDimension>() {

			@Override
			public String toString(MinecraftDimension object) {
				return object.displayName;
			}

			@Override
			public MinecraftDimension fromString(String string) {
				return null;
			}
		});
	}

	public void load(Path worldPath) {
		for (MinecraftDimension d : MinecraftDimension.values())
			if (Files.exists(worldPath.resolve(d.getRegionPath())) && Files.isDirectory(worldPath.resolve(d.getRegionPath())))
				available.add(d);
		if (available.isEmpty())
			throw new IllegalArgumentException("Not a vaild world folder");
		dimensionBox.setItems(FXCollections.observableList(available));
		dimensionBox.valueProperty().addListener(new WeakChangeListener<>(reloadListener));
		dimensionBox.setValue(available.get(0));
		this.worldPath = worldPath;
		reload();
	}

	public void reload() {
		try {
			settings = new RenderSettings(
					Integer.MIN_VALUE,
					Integer.MAX_VALUE,
					new IntegerStringConverter().fromString(minHeight.getText()),
					new IntegerStringConverter().fromString(maxHeight.getText()),
					Integer.MIN_VALUE,
					Integer.MAX_VALUE,
					InternalColorMap.values()[colorBox.getSelectionModel().getSelectedIndex()].getColorMap(),
					BiomeColorMap.loadDefault(),
					RegionShader.DEFAULT_SHADERS[shadingBox.getSelectionModel().getSelectedIndex()]);
			RegionRenderer renderer = new RegionRenderer(settings);
			folder.set(new Pair<>(
					Integer.toHexString(Objects.hash(worldPath.toAbsolutePath().toString(), settings, dimensionBox.getValue())),
					WorldRegionFolder.load(worldPath,
							dimensionBox.getValue(), renderer, true)));
		} catch (IOException e) {
			folder.set(null);
			log.warn("Could not load world " + worldPath, e);
			ExceptionDialog d = new ExceptionDialog(e);
			d.setTitle("Could not load world");
			d.showAndWait();
		}
	}

	public String getLocation() {
		return worldPath.toString();
	}
}
