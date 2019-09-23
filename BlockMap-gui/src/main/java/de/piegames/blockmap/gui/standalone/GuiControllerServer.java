package de.piegames.blockmap.gui.standalone;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.controlsfx.dialog.ExceptionDialog;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import de.piegames.blockmap.world.RegionFolder;
import de.piegames.blockmap.world.ServerMetadata;
import de.piegames.blockmap.world.ServerMetadata.ServerLevel;
import io.gsonfire.GsonFireBuilder;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Pair;
import net.dongliu.gson.GsonJava8TypeAdapterFactory;

public class GuiControllerServer implements Initializable {

	private static Log											log			= LogFactory.getLog(GuiControllerServer.class);

	public static final Gson									GSON		= new GsonFireBuilder()
			.enableExposeMethodParam()
			.createGsonBuilder()
			.registerTypeAdapterFactory(new GsonJava8TypeAdapterFactory())
			.registerTypeHierarchyAdapter(Path.class, new TypeAdapter<Path>() {

																						@Override
																						public void write(JsonWriter out, Path value) throws IOException {
																							out.value(value.toString());
																						}

																						@Override
																						public Path read(JsonReader in) throws IOException {
																							return Paths.get(in.nextString());
																						}
																					})
			.disableHtmlEscaping()
			.setPrettyPrinting()
			.create();
	@FXML
	Label														serverName, serverDescription;
	@FXML
	ImageView													serverIcon;
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
			metadata = GSON.fromJson(reader, ServerMetadata.class);
			worlds = metadata.levels;
			String selected = worldBox.getValue();

			worldBox.setItems(FXCollections.observableList(worlds.stream().map(l -> l.name).collect(Collectors.toList())));
			if (worldBox.getItems().contains(selected))
				worldBox.setValue(selected);
			else if (!worlds.isEmpty())
				worldBox.setValue(worldBox.getItems().get(0));

			serverName.setText(metadata.name.orElse("(unknown server)"));
			serverDescription.setText(metadata.description.orElse("(unknown description)"));

			serverIcon.setImage(metadata.iconLocation.map(url -> new Image(url)).orElse(new Image(getClass().getResourceAsStream("/unknown_server.png"))));
		} catch (RuntimeException | IOException e) {
			folder.set(null);
			log.warn("Could not load server world " + file, e);
			ExceptionDialog d = new ExceptionDialog(e);
			d.setTitle("Could not load server world");
			d.showAndWait();
		}
	}
}
