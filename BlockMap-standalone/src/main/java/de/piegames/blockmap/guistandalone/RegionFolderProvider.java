package de.piegames.blockmap.guistandalone;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.reflect.TypeToken;

import de.piegames.blockmap.MinecraftDimension;
import de.piegames.blockmap.renderer.RegionRenderer;
import de.piegames.blockmap.world.RegionFolder;
import de.piegames.blockmap.world.RegionFolder.WorldRegionFolder;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.FXCollections;

// World:
// - Dimension
// - Shader
// - Color pack
// - Height
// Saved, local:
// - World?
// Saved, remote:
// - World

public abstract class RegionFolderProvider {

	private static Log								log				= LogFactory.getLog(RegionFolderProvider.class);

	static final byte								BIT_HEIGHT		= 0x01;
	static final byte								BIT_COLOR		= 0x02;
	static final byte								BIT_SHADING		= 0x04;
	static final byte								BIT_DIMENSION	= 0x08;
	static final byte								BIT_WORLD		= 0x10;

	protected GuiController							controller;
	protected ReadOnlyObjectWrapper<RegionFolder>	folder			= new ReadOnlyObjectWrapper<>();

	public RegionFolderProvider(GuiController controller) {
		this.controller = controller;
	}

	public ReadOnlyObjectProperty<RegionFolder> folderProperty() {
		return folder.getReadOnlyProperty();
	}

	/** Bit mask which settings controls should be enabled and shown in the GUI */
	public abstract byte getGuiBitmask();

	public abstract void reload();

	public abstract String getLocation();

	/** A single local region folder */
	public static class LocalRegionFolderProvider extends RegionFolderProvider {

		protected Path				regionFolder;
		protected RegionRenderer	renderer;

		public LocalRegionFolderProvider(GuiController controller, Path regionFolder, RegionRenderer renderer) {
			super(controller);
			this.regionFolder = regionFolder;
			this.renderer = Objects.requireNonNull(renderer);
			reload();
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
		public byte getGuiBitmask() {
			return BIT_HEIGHT | BIT_COLOR | BIT_SHADING;
		}
	}

	/** A complete local world */
	public static class LocalWorldProvider extends RegionFolderProvider {

		private static Log							log	= LogFactory.getLog(LocalWorldProvider.class);

		protected Path								worldPath;
		protected RegionRenderer					renderer;
		protected List<MinecraftDimension>			available;

		/* Strong reference */
		@SuppressWarnings("unused")
		private ChangeListener<MinecraftDimension>	listener;

		public LocalWorldProvider(GuiController controller, Path worldPath, RegionRenderer renderer) {
			super(controller);
			this.worldPath = worldPath;
			this.renderer = renderer;
			available = new ArrayList<>(3);
			for (MinecraftDimension d : MinecraftDimension.values())
				if (Files.exists(worldPath.resolve(d.getRegionPath())) && Files.isDirectory(worldPath.resolve(d.getRegionPath())))
					available.add(d);
			if (available.isEmpty())
				throw new IllegalArgumentException("Not a vaild world folder");
			controller.dimensionBox.setItems(FXCollections.observableList(available));
			controller.dimensionBox.valueProperty().addListener(new WeakChangeListener<>(listener = (observable, oldValue, newValue) -> {
				if (oldValue != newValue)
					reload();
			}));
			controller.dimensionBox.setValue(available.get(0));
			reload();
		}

		@Override
		public void reload() {
			try {
				folder.set(WorldRegionFolder.load(worldPath, controller.dimensionBox.getValue(), renderer, true));
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
		public byte getGuiBitmask() {
			return BIT_HEIGHT | BIT_COLOR | BIT_SHADING | BIT_DIMENSION;
		}
	}

	/** A single region folder, pre-rendered. May be local or remote */
	public static class SavedRegionFolderProvider extends RegionFolderProvider {
		protected URI				file;
		protected RegionRenderer	renderer;

		public SavedRegionFolderProvider(GuiController controller, URI file) {
			super(controller);
			this.file = file;
			reload();
		}

		@Override
		public void reload() {
			try {
				folder.set(new RegionFolder.RemoteRegionFolder(file));
			} catch (IOException e) {
				log.warn("Could not load  from remote file " + file);
				folder.set(null);
			}
		}

		@Override
		public String getLocation() {
			return "file".equals(file.getScheme()) ? Paths.get(file).toString() : file.toString();
		}

		@Override
		public byte getGuiBitmask() {
			return 0;
		}
	}

	/** Multiple rendered region folder, possibly rendered with different settings. May be local or remote */
	public static class SavedWorldProvider extends RegionFolderProvider {
		protected URI					file;
		protected RegionRenderer		renderer;
		protected Map<String, String>	worlds;

		/* Strong reference */
		@SuppressWarnings("unused")
		private ChangeListener<String>	listener;

		public SavedWorldProvider(GuiController controller, URI file) {
			super(controller);
			this.file = file;
			controller.worldBox.valueProperty().addListener(new WeakChangeListener<>(listener = (o, old, val) -> {
				try {
					folder.set(new RegionFolder.RemoteRegionFolder(file));
				} catch (IOException e) {
					log.warn("Could not load world " + val + " from remote file " + file);
					folder.set(null);
				}
			}));
			reload();
		}

		@SuppressWarnings("serial")
		@Override
		public void reload() {
			try (Reader reader = new InputStreamReader(file.toURL().openStream())) {
				worlds = RegionFolder.GSON.fromJson(reader, new TypeToken<Map<String, String>>() {
				}.getType());
				String selected = controller.worldBox.getValue();

				controller.worldBox.setItems(FXCollections.observableList(new ArrayList<>(worlds.keySet())));
				if (worlds.keySet().contains(selected))
					controller.worldBox.setValue(selected);
				else if (!worlds.isEmpty())
					controller.worldBox.setValue(controller.worldBox.getItems().get(0));
			} catch (IOException e) {
				folder.set(null);
				log.warn("Could not load world " + file, e);
			}
		}

		@Override
		public String getLocation() {
			return "file".equals(file.getScheme()) ? Paths.get(file).toString() : file.toString();
		}

		@Override
		public byte getGuiBitmask() {
			return BIT_WORLD;
		}
	}

	public static RegionFolderProvider create(GuiController controller, Path path, RegionRenderer renderer) {
		if (Files.isDirectory(path) && Files.exists(path.resolve("level.dat")))
			return new LocalWorldProvider(controller, path, renderer);
		else if (Files.exists(path) && path.getFileName().toString().equals("rendered.json"))
			return new SavedRegionFolderProvider(controller, path.toUri());
		else if (Files.exists(path) && path.getFileName().toString().equals("index.json"))
			return new SavedWorldProvider(controller, path.toUri());
		else
			return new LocalRegionFolderProvider(controller, path, renderer);
	}

	public static RegionFolderProvider create(GuiController controller, URI uri) {
		if (uri.toString().endsWith("rendered.json"))
			return new SavedRegionFolderProvider(controller, uri);
		else if (uri.toString().endsWith("index.json"))
			return new SavedWorldProvider(controller, uri);
		else
			return null;
	}
}