package de.piegames.blockmap.gui.standalone;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
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

import de.piegames.blockmap.gui.VersionProvider;
import de.piegames.blockmap.world.RegionFolder;
import de.piegames.blockmap.world.ServerMetadata;
import de.piegames.blockmap.world.ServerMetadata.ServerLevel;
import io.gsonfire.GsonFireBuilder;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ChangeListener;
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

	private static Log log = LogFactory.getLog(GuiControllerServer.class);

	public static final Gson GSON = new GsonFireBuilder()
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
	Label serverName, serverDescription, onlinePlayers;
	@FXML
	ImageView serverIcon;
	@FXML
	ChoiceBox<String> worldBox;

	/* The String is a hash code used for caching */
	protected ReadOnlyObjectWrapper<Pair<String, RegionFolder>> folder = new ReadOnlyObjectWrapper<>();
	protected URI file;
	protected ServerMetadata metadata;
	protected List<ServerLevel> worlds;
	protected String lastBrowsedURL;

	private ChangeListener<String> listener = (o, old, val) -> {
		try {
			String path = worlds.stream()
					.filter(p -> p.name.equals(val))
					.findFirst()
					.get().path;
			folder.set(new Pair<String, RegionFolder>(
					Integer.toHexString(Objects.hash(
							file.toString(),
							worldBox.getValue(),
							VersionProvider.VERSION)),
					new RegionFolder.RemoteRegionFolder(
							fixupURI(file.resolve(path)))));
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
		worldBox.valueProperty().addListener(listener);
	}

	public void load(URI file) {
		if ("blockmap".equals(file.getScheme())) {
			file = URI.create(file.getSchemeSpecificPart());
		}
		this.file = file;
		reload();
	}

	public void reload() {
		try (Reader reader = new InputStreamReader(file.toURL().openStream())) {
			metadata = GSON.fromJson(reader, ServerMetadata.class);
			worlds = metadata.levels;
			String selected = worldBox.getValue();

			/* Temporarily disable listener to avoid premature triggers */
			worldBox.valueProperty().removeListener(listener);
			worldBox.setItems(FXCollections.observableList(worlds.stream().map(l -> l.name).collect(Collectors.toList())));
			if (worldBox.getItems().contains(selected))
				worldBox.setValue(selected);
			else if (!worlds.isEmpty())
				worldBox.setValue(worldBox.getItems().get(0));
			worldBox.valueProperty().addListener(listener);

			serverName.setText(metadata.name.orElse("(unknown server)"));
			serverDescription.setText(metadata.description.orElse("(unknown description)"));
			onlinePlayers.setText("Players online: " +
					metadata.onlinePlayers.map(Collection::size).map(Object::toString).orElse("? ") +
					(metadata.maxPlayers == -1 ? "" : "/" + metadata.maxPlayers));

			serverIcon.setImage(metadata.iconLocation.map(url -> new Image(url)).orElse(new Image(getClass().getResourceAsStream("/unknown_server.png"))));

			/* Force listener update to reload world */
			listener.changed(worldBox.valueProperty(), null, worldBox.getValue());
		} catch (IOException e) {
			folder.set(null);
			log.warn("Could not load server world at '" + file + "'", e);
			ExceptionDialog d = new ExceptionDialog(e);
			d.setHeaderText("Could not load server world at '" + file + "'");
			d.showAndWait();
		}
	}

	public ServerMetadata getMetadata() {
		return metadata;
	}

	/*
	 * The Java URI API has a bug where resolving a relative path to an absolute one without path and
	 * missing trailing '/' (as `https://example.com`) will return in an URI with that slash missing,
	 * and thus with an invalid host name. (See the respective links for more examples)
	 *
	 * https://stackoverflow.com/questions/2534124/java-uri-resolve
	 * https://bugs.openjdk.java.net/browse/JDK-4666701
	 *
	 * The easiest fix for this is to fix the `toString` method to include that missing slash.
	 */

	private URI fixupURI(URI uri) {
		return URI.create(uriToStringFixed(uri));
	}

	private static String uriToStringFixed(URI uri) {
		var scheme = uri.getScheme();
		var schemeSpecificPart = uri.getSchemeSpecificPart();
		var host = uri.getHost();
		var userInfo = uri.getUserInfo();
		var port = uri.getPort();
		var authority = uri.getAuthority();
		var path = uri.getPath();
		var query = uri.getQuery();
		var fragment = uri.getFragment();

		StringBuilder sb = new StringBuilder();
		if (scheme != null) {
			sb.append(scheme);
			sb.append(':');
		}
		if (uri.isOpaque()) {
			sb.append(schemeSpecificPart);
		} else {
			if (host != null) {
				sb.append("//");
				if (userInfo != null) {
					sb.append(userInfo);
					sb.append('@');
				}
				boolean needBrackets = ((host.indexOf(':') >= 0)
						&& !host.startsWith("[")
						&& !host.endsWith("]"));
				if (needBrackets)
					sb.append('[');
				sb.append(host);
				if (needBrackets)
					sb.append(']');
				if (port != -1) {
					sb.append(':');
					sb.append(port);
				}
			} else if (authority != null) {
				sb.append("//");
				sb.append(authority);
			}
			if (path != null) {
				// PATCH START
				if (!path.startsWith("/")) {
					if ((host != null && !host.endsWith("/"))
							|| (authority != null && !authority.endsWith("/"))) {
						sb.append("/");
					}
				}
				// PATCH END
				sb.append(path);
			}
			if (query != null) {
				sb.append('?');
				sb.append(query);
			}
		}
		if (fragment != null) {
			sb.append('#');
			sb.append(fragment);
		}
		return sb.toString();
	}
}
