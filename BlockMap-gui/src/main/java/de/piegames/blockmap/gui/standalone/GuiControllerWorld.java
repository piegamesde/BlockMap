package de.piegames.blockmap.gui.standalone;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.controlsfx.dialog.ExceptionDialog;

import de.piegames.blockmap.MinecraftDimension;
import de.piegames.blockmap.color.BiomeColorMap;
import de.piegames.blockmap.color.BlockColorMap.InternalColorMap;
import de.piegames.blockmap.gui.VersionProvider;
import de.piegames.blockmap.renderer.RegionRenderer;
import de.piegames.blockmap.renderer.RegionShader;
import de.piegames.blockmap.renderer.RenderSettings;
import de.piegames.blockmap.world.RegionFolder;
import de.piegames.blockmap.world.RegionFolder.WorldRegionFolder;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.util.Pair;
import javafx.util.StringConverter;
import javafx.util.converter.IntegerStringConverter;

public class GuiControllerWorld implements Initializable {

	private static Log log = LogFactory.getLog(GuiControllerWorld.class);

	@FXML
	TextField minHeight, maxHeight;
	@FXML
	ChoiceBox<String> shadingBox;
	@FXML
	ChoiceBox<String> colorBox;
	@FXML
	ChoiceBox<MinecraftDimension> dimensionBox;

	/* The String is a hash code used for caching */
	protected ReadOnlyObjectWrapper<Pair<String, RegionFolder>> folder = new ReadOnlyObjectWrapper<>();
	protected Path worldPath;
	protected RenderSettings settings;

	private ChangeListener<Object> reloadListener = (observer, old, value) -> {
		if (old != value)
			reload();
	};

	public ReadOnlyObjectProperty<Pair<String, RegionFolder>> folderProperty() {
		return folder.getReadOnlyProperty();
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		minHeight.setTextFormatter(new TextFormatter<>(new IntegerStringConverter(), -64));
		maxHeight.setTextFormatter(new TextFormatter<>(new IntegerStringConverter(), 319));

		EventHandler<ActionEvent> onHeightChange = e -> {
			if (minHeight.getText().isEmpty())
				minHeight.setText("-64");
			if (maxHeight.getText().isEmpty())
				maxHeight.setText("319");
			if (new IntegerStringConverter().fromString(minHeight.getText()) > new IntegerStringConverter().fromString(maxHeight.getText())) {
				String tmp = minHeight.getText();
				minHeight.setText(maxHeight.getText());
				maxHeight.setText(tmp);
			}
			reload();
		};
		minHeight.setOnAction(onHeightChange);
		maxHeight.setOnAction(onHeightChange);

		colorBox.valueProperty().addListener(reloadListener);
		shadingBox.valueProperty().addListener(reloadListener);

		dimensionBox.setConverter(new StringConverter<MinecraftDimension>() {

			@Override
			public String toString(MinecraftDimension object) {
				if (object == null)
					return null;
				return object.displayName;
			}

			@Override
			public MinecraftDimension fromString(String string) {
				return null;
			}
		});
	}

	public void load(Path worldPath) {
		List<MinecraftDimension> available = Arrays.stream(MinecraftDimension.values())
				.filter(d -> Files.exists(worldPath.resolve(d.getRegionPath())) && Files.isDirectory(worldPath.resolve(d.getRegionPath())))
				.collect(Collectors.toList());
		if (available.isEmpty())
			throw new IllegalArgumentException("Not a vaild world folder");
		/* Temporarily disable listener to avoid premature triggers */
		dimensionBox.valueProperty().removeListener(reloadListener);
		dimensionBox.setItems(FXCollections.observableList(available));
		dimensionBox.setValue(available.get(0));
		dimensionBox.valueProperty().addListener(reloadListener);
		this.worldPath = worldPath;
		reload();
	}

	public void reload() {
		if (worldPath == null) {
			folder.set(null);
			return;
		}
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
					Integer.toHexString(Objects.hash(
							worldPath.toAbsolutePath().toString(),
							settings.minY,
							settings.maxY,
							colorBox.getSelectionModel().getSelectedItem(),
							shadingBox.getSelectionModel().getSelectedItem(),
							dimensionBox.getValue().ordinal(),
							VersionProvider.VERSION)),
					WorldRegionFolder.load(worldPath,
							dimensionBox.getValue(), renderer, true)));
		} catch (RuntimeException | IOException e) {
			folder.set(null);
			log.warn("Could not load world " + worldPath, e);
			ExceptionDialog d = new ExceptionDialog(e);
			d.setTitle("Could not load world");
			d.showAndWait();
		}
	}
}
