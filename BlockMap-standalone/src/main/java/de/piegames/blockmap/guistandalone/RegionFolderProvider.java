package de.piegames.blockmap.guistandalone;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.piegames.blockmap.MinecraftDimension;
import de.piegames.blockmap.RegionFolder;
import de.piegames.blockmap.RegionFolder.RemoteRegionFolder;
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

	public static class SavedFolderProvider extends RegionFolderProvider {

		protected Path				file;
		protected RegionRenderer	renderer;
		protected List<Node>		gui	= new ArrayList<>();

		public SavedFolderProvider(Path file) {
			this.file = file;
			reload();
		}

		@Override
		public List<Node> getGUI() {
			return gui;
		}

		@Override
		public void reload() {
			try {
				folder.set(new SavedRegionFolder(file));
			} catch (IOException e) {
				folder.set(null);
				log.warn("Could not load world " + file, e);
			}
		}

		@Override
		public String getLocation() {
			return file.toString();
		}

		@Override
		public boolean hideSettings() {
			return true;
		}
	}

	public static class RemoteFolderProvider extends RegionFolderProvider {

		protected URI				file;
		protected RegionRenderer	renderer;
		protected List<Node>		gui	= new ArrayList<>();

		public RemoteFolderProvider(URI file) {
			this.file = file;
			reload();
		}

		@Override
		public List<Node> getGUI() {
			return gui;
		}

		@Override
		public void reload() {
			try {
				folder.set(new RemoteRegionFolder(file));
			} catch (IOException e) {
				folder.set(null);
				log.warn("Could not load world " + file, e);
			}
		}

		@Override
		public String getLocation() {
			return file.toString();
		}

		@Override
		public boolean hideSettings() {
			return true;
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