package de.piegames.blockmap.gui.standalone;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.controlsfx.dialog.ExceptionDialog;

import de.piegames.blockmap.world.RegionFolder;
import de.piegames.blockmap.world.ServerMetadata;
import de.piegames.blockmap.world.ServerMetadata.ServerLevel;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ChoiceBox;
import javafx.scene.layout.VBox;
import javafx.util.Pair;

public class GuiControllerServer implements Initializable {

	private static Log											log			= LogFactory.getLog(GuiControllerServer.class);

	@FXML
	VBox														content;
	@FXML
	ChoiceBox<String>											worldBox;

	/* The String is a hash code used for caching */
	protected ReadOnlyObjectWrapper<Pair<String, RegionFolder>>	folder		= new ReadOnlyObjectWrapper<>();
	protected URI												file;
	protected ServerMetadata									metadata;
	protected List<ServerLevel>									worlds;
	protected String											lastBrowsedURL;

	/* Strong reference */
	private ChangeListener<String>								listener	= (o, old, val) -> {
																				try {
																					String path = worlds.stream()
																							.filter(p -> p.name.equals(val))
																							.findFirst()
																							.get().path;
																					folder.set(new Pair<String, RegionFolder>(
																							Integer.toHexString(Objects.hash(file.toString(),
																									worldBox.getValue())),
																							new RegionFolder.RemoteRegionFolder(file.resolve(path))));
																				} catch (IOException e) {
																					folder.set(null);
																					log.warn("Could not load world " + val + " from remote file " + file);
																					ExceptionDialog d = new ExceptionDialog(e);
																					d.setTitle("Could not load world from remote file " + file);
																					d.showAndWait();
																				}
																			};

	public ReadOnlyObjectProperty<Pair<String, RegionFolder>> folderProperty() {
		return folder.getReadOnlyProperty();
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		worldBox.valueProperty().addListener(new WeakChangeListener<>(listener));
	}

	public void load(URI file) {
		this.file = file;
		reload();
	}

	public void reload() {
		try (Reader reader = new InputStreamReader(file.toURL().openStream())) {
			metadata = RegionFolder.GSON.fromJson(reader, ServerMetadata.class);
			worlds = metadata.levels;
			String selected = worldBox.getValue();

			worldBox.setItems(FXCollections.observableList(worlds.stream().map(l -> l.name).collect(Collectors.toList())));
			if (worldBox.getItems().contains(selected))
				worldBox.setValue(selected);
			else if (!worlds.isEmpty())
				worldBox.setValue(worldBox.getItems().get(0));
		} catch (IOException e) {
			folder.set(null);
			log.warn("Could not load world " + file, e);
			ExceptionDialog d = new ExceptionDialog(e);
			d.setTitle("Could not load world");
			d.showAndWait();
		}
	}

	public String getLocation() {
		return "file".equals(file.getScheme()) ? Paths.get(file).toString() : file.toString();
	}
}
