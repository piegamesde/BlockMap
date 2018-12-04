package de.piegames.blockmap.guistandalone;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import de.piegames.blockmap.MinecraftDimension;
import de.piegames.blockmap.RegionFolder;
import de.piegames.blockmap.RegionFolder.SavedRegionFolder;
import de.piegames.blockmap.RegionFolder.WorldRegionFolder;
import de.piegames.blockmap.renderer.RegionRenderer;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.util.StringConverter;

public abstract class RegionFolderProvider {

	private static Log								log		= LogFactory.getLog(RegionFolderProvider.class);

	protected ReadOnlyObjectWrapper<RegionFolder>	folder	= new ReadOnlyObjectWrapper<>();

	public ReadOnlyObjectProperty<RegionFolder> folderProperty() {
		return folder.getReadOnlyProperty();
	}

	public abstract boolean hideSettings();

	public abstract List<Node> getGUI();

	public abstract void reload();

	public abstract String getLocation();

	public static class RegionFolderProviderImpl extends RegionFolderProvider {

		protected Path				regionFolder;
		protected RegionRenderer	renderer;
		protected List<Node>		gui	= new ArrayList<>();

		public RegionFolderProviderImpl(Path regionFolder, RegionRenderer renderer) {
			this.regionFolder = regionFolder;
			this.renderer = Objects.requireNonNull(renderer);
			reload();
		}

		@Override
		public List<Node> getGUI() {
			return gui;
		}

		@Override
		public void reload() {
			try {
				folder.set(WorldRegionFolder.load(regionFolder, renderer));
			} catch (IOException e) {
				folder.set(null);
				log.warn("Could not load world " + regionFolder, e);
			}
		}

		@Override
		public String getLocation() {
			return regionFolder.toString();
		}

		@Override
		public boolean hideSettings() {
			return false;
		}
	}

	public static abstract class SavedFolderProvider<T> extends RegionFolderProvider {
		protected T							file;
		protected RegionRenderer			renderer;
		protected List<Node>				gui	= new ArrayList<>();
		protected ChoiceBox<String>			worldBox;
		protected Map<String, JsonObject>	worlds;

		public SavedFolderProvider(T file) {
			this.file = file;
			worldBox = new ChoiceBox<>();
			worldBox.valueProperty().addListener((o, old, val) -> {
				if (old != val)
					folder.set(load(val));
			});
			gui.add(worldBox);
			reload();
		}

		@Override
		public List<Node> getGUI() {
			return gui;
		}

		@Override
		public void reload() {
			try {
				worlds = SavedRegionFolder.parseSaved(load());
				String selected = worldBox.getValue();

				worldBox.setItems(FXCollections.observableList(new ArrayList<>(worlds.keySet())));
				if (worlds.keySet().contains(selected))
					worldBox.setValue(selected);
				else if (!worlds.isEmpty())
					worldBox.setValue(worldBox.getItems().get(0));
			} catch (IOException e) {
				folder.set(null);
				log.warn("Could not load world " + file, e);
			}
		}

		protected abstract JsonElement load() throws IOException;

		protected abstract SavedRegionFolder<T> load(String world);

		@Override
		public String getLocation() {
			return file.toString();
		}

		@Override
		public boolean hideSettings() {
			return true;
		}
	}

	public static class LocalFolderProvider extends SavedFolderProvider<Path> {

		public LocalFolderProvider(Path file) {
			super(file);
		}

		@Override
		protected JsonElement load() throws IOException {
			return new JsonParser().parse(new InputStreamReader(Files.newInputStream(file)));
		}

		@Override
		protected SavedRegionFolder<Path> load(String world) {
			try {
				return new RegionFolder.LocalRegionFolder(file, world);
			} catch (IOException e) {
				log.warn("Could not load world " + world + " from file " + file);
				return null;
			}
		}
	}

	public static class RemoteFolderProvider extends SavedFolderProvider<URI> {

		public RemoteFolderProvider(URI file) {
			super(file);
		}

		@Override
		protected JsonElement load() throws IOException {
			return new JsonParser().parse(new InputStreamReader(file.toURL().openStream()));
		}

		@Override
		protected SavedRegionFolder<URI> load(String world) {
			try {
				return new RegionFolder.RemoteRegionFolder(file, world);
			} catch (IOException e) {
				log.warn("Could not load world " + world + " from remote file " + file);
				return null;
			}
		}
	}

	public static class WorldRegionFolderProvider extends RegionFolderProvider {

		private static Log						log	= LogFactory.getLog(WorldRegionFolderProvider.class);

		protected Path							worldPath;
		protected RegionRenderer				renderer;
		protected List<MinecraftDimension>		available;
		protected ChoiceBox<MinecraftDimension>	dimensionBox;
		protected List<Node>					gui	= new ArrayList<>();

		public WorldRegionFolderProvider(Path worldPath, RegionRenderer renderer) {
			this.worldPath = worldPath;
			this.renderer = renderer;
			available = new ArrayList<>(3);
			for (MinecraftDimension d : MinecraftDimension.values())
				if (Files.exists(d.resolve(worldPath)) && Files.isDirectory(d.resolve(worldPath)))
					available.add(d);
			if (available.isEmpty())
				throw new IllegalArgumentException("Not a vaild world folder");
			dimensionBox = new ChoiceBox<MinecraftDimension>();
			dimensionBox.setItems(FXCollections.observableList(available));
			dimensionBox.valueProperty().addListener((observable, oldValue, newValue) -> {
				if (oldValue != newValue)
					reload();
			});
			dimensionBox.setValue(available.get(0));
			dimensionBox.setConverter(new StringConverter<MinecraftDimension>() {

				@Override
				public String toString(MinecraftDimension object) {
					return object.name;
				}

				@Override
				public MinecraftDimension fromString(String string) {
					return null;
				}
			});
			gui.add(new Label("Dimension:"));
			gui.add(dimensionBox);

			reload();
		}

		@Override
		public List<Node> getGUI() {
			return gui;
		}

		@Override
		public void reload() {
			try {
				folder.set(WorldRegionFolder.load(worldPath, dimensionBox.getValue(), renderer));
			} catch (IOException e) {
				folder.set(null);
				log.warn("Could not load world " + worldPath, e);
			}
		}

		@Override
		public String getLocation() {
			return worldPath.toString();
		}

		@Override
		public boolean hideSettings() {
			return false;
		}
	}

	public static RegionFolderProvider byPath(Path path, RegionRenderer renderer) {
		if (Files.isDirectory(path) && Files.exists(path.resolve("level.dat")))
			return new WorldRegionFolderProvider(path, renderer);
		else
			return new RegionFolderProviderImpl(path, renderer);
	}
}