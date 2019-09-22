package de.piegames.blockmap.gui.standalone;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.controlsfx.dialog.ExceptionDialog;

import com.google.common.reflect.TypeToken;

import de.piegames.blockmap.world.RegionFolder;
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

	private static Log											log		= LogFactory.getLog(GuiControllerServer.class);

	@FXML
	VBox														content;
	@FXML
	ChoiceBox<String>											worldBox;

	/* The String is a hash code used for caching */
	protected ReadOnlyObjectWrapper<Pair<String, RegionFolder>>	folder	= new ReadOnlyObjectWrapper<>();
	protected URI												file;
	protected Map<String, String>								worlds;
	protected String											lastBrowsedURL;

	/* Strong reference */
	private ChangeListener<String>								listener	= (o, old, val) -> {
																				try {
																					folder.set(new Pair<>(
																							Integer.toHexString(Objects.hash(file.toString(),
																									worldBox.getValue())),
																							new RegionFolder.RemoteRegionFolder(file.resolve(worlds.get(
																									val)))));
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

	@SuppressWarnings("serial")
	public void reload() {
		try (Reader reader = new InputStreamReader(file.toURL().openStream())) {
			worlds = RegionFolder.GSON.fromJson(reader, new TypeToken<Map<String, String>>() {
			}.getType());
			String selected = worldBox.getValue();

			worldBox.setItems(FXCollections.observableList(new ArrayList<>(worlds.keySet())));
			if (worlds.keySet().contains(selected))
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
