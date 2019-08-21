package de.piegames.blockmap.gui.standalone;

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
import org.controlsfx.dialog.ExceptionDialog;

import com.google.common.reflect.TypeToken;

import de.piegames.blockmap.MinecraftDimension;
import de.piegames.blockmap.color.BiomeColorMap;
import de.piegames.blockmap.color.BlockColorMap.InternalColorMap;
import de.piegames.blockmap.renderer.RegionRenderer;
import de.piegames.blockmap.renderer.RegionShader;
import de.piegames.blockmap.renderer.RenderSettings;
import de.piegames.blockmap.world.RegionFolder;
import de.piegames.blockmap.world.RegionFolder.WorldRegionFolder;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.FXCollections;
import javafx.util.Pair;

public abstract class RegionFolderProvider {

	private static Log								log				= LogFactory.getLog(RegionFolderProvider.class);

	static final byte								BIT_HEIGHT		= 0x01;
	static final byte								BIT_COLOR		= 0x02;
	static final byte								BIT_SHADING		= 0x04;
	static final byte								BIT_DIMENSION	= 0x08;
	static final byte								BIT_WORLD		= 0x10;

	protected GuiController							controller;
	/* The String is a hash code used for caching */
	protected ReadOnlyObjectWrapper<Pair<String, RegionFolder>>	folder			= new ReadOnlyObjectWrapper<>();

	public RegionFolderProvider(GuiController controller) {
		this.controller = controller;
	}

	public ReadOnlyObjectProperty<Pair<String, RegionFolder>> folderProperty() {
		return folder.getReadOnlyProperty();
	}

	/** Bit mask which settings controls should be enabled and shown in the GUI */
	public abstract byte getGuiBitmask();

	/**
	 * Reload the region folder. Cached data may still be used, but at least the metadata must be updated to check if the cached data is still
	 * valid.
	 */
	public abstract void reload();

	/** This is used to represent the path to the region folder. It may be used as default directory for file chooser dialogs */
	public abstract String getLocation();

	/** A single local region folder */
	public static class LocalRegionFolderProvider extends RegionFolderProvider {

		protected Path					regionFolder;
		protected RenderSettings		settings;

		private ChangeListener<Boolean>	heightListener	= (e, oldVal, newVal) -> {
															if (oldVal && !newVal)
																reload();
														};
		private ChangeListener<String>	reloadListener	= (observer, old, value) -> {
															reload();
														};

		public LocalRegionFolderProvider(GuiController controller, Path regionFolder) {
			super(controller);
			this.regionFolder = regionFolder;

			controller.heightSlider.lowValueChangingProperty().addListener(new WeakChangeListener<>(heightListener));
			controller.heightSlider.highValueChangingProperty().addListener(new WeakChangeListener<>(heightListener));

			controller.colorBox.valueProperty().addListener(new WeakChangeListener<>(reloadListener));
			controller.shadingBox.valueProperty().addListener(new WeakChangeListener<>(reloadListener));

			reload();
		}

		@Override
		public void reload() {
			try {
				settings = new RenderSettings(
						Integer.MIN_VALUE,
						Integer.MAX_VALUE,
						(int) Math.round(controller.heightSlider.lowValueProperty().getValue().doubleValue()),
						(int) Math.round(controller.heightSlider.highValueProperty().getValue().doubleValue()),
						Integer.MIN_VALUE,
						Integer.MAX_VALUE,
						InternalColorMap.values()[controller.colorBox.getSelectionModel().getSelectedIndex()].getColorMap(),
						BiomeColorMap.loadDefault(),
						RegionShader.DEFAULT_SHADERS[controller.shadingBox.getSelectionModel().getSelectedIndex()]);
				RegionRenderer renderer = new RegionRenderer(settings);
				folder.set(new Pair<>(
						Integer.toHexString(Objects.hash(regionFolder.toAbsolutePath().toString(), settings)),
						WorldRegionFolder.load(regionFolder, renderer)));
			} catch (IOException e) {
				folder.set(null);
				log.warn("Could not load world " + regionFolder, e);
				ExceptionDialog d = new ExceptionDialog(e);
				d.setTitle("Could not load world");
				d.showAndWait();
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

		private static Log					log				= LogFactory.getLog(LocalWorldProvider.class);

		protected Path						worldPath;
		protected RenderSettings			settings;
		protected List<MinecraftDimension>	available;

		private ChangeListener<Boolean>		heightListener	= (e, oldVal, newVal) -> {
																if (oldVal && !newVal)
																	reload();
															};
		private ChangeListener<Object>		reloadListener	= (observer, old, value) -> {
																if (old != value)
																	reload();
															};

		public LocalWorldProvider(GuiController controller, Path worldPath) {
			super(controller);
			this.worldPath = worldPath;
			available = new ArrayList<>(3);
			for (MinecraftDimension d : MinecraftDimension.values())
				if (Files.exists(worldPath.resolve(d.getRegionPath())) && Files.isDirectory(worldPath.resolve(d.getRegionPath())))
					available.add(d);
			if (available.isEmpty())
				throw new IllegalArgumentException("Not a vaild world folder");
			controller.dimensionBox.setItems(FXCollections.observableList(available));
			controller.dimensionBox.valueProperty().addListener(new WeakChangeListener<>(reloadListener));
			controller.dimensionBox.setValue(available.get(0));

			controller.heightSlider.lowValueChangingProperty().addListener(new WeakChangeListener<>(heightListener));
			controller.heightSlider.highValueChangingProperty().addListener(new WeakChangeListener<>(heightListener));

			controller.colorBox.valueProperty().addListener(new WeakChangeListener<>(reloadListener));
			controller.shadingBox.valueProperty().addListener(new WeakChangeListener<>(reloadListener));

			reload();
		}

		@Override
		public void reload() {
			try {
				settings = new RenderSettings(
						Integer.MIN_VALUE,
						Integer.MAX_VALUE,
						(int) Math.round(controller.heightSlider.lowValueProperty().getValue().doubleValue()),
						(int) Math.round(controller.heightSlider.highValueProperty().getValue().doubleValue()),
						Integer.MIN_VALUE,
						Integer.MAX_VALUE,
						InternalColorMap.values()[controller.colorBox.getSelectionModel().getSelectedIndex()].getColorMap(),
						BiomeColorMap.loadDefault(),
						RegionShader.DEFAULT_SHADERS[controller.shadingBox.getSelectionModel().getSelectedIndex()]);
				RegionRenderer renderer = new RegionRenderer(settings);
				folder.set(new Pair<>(
						Integer.toHexString(Objects.hash(worldPath.toAbsolutePath().toString(), settings, controller.dimensionBox.getValue())),
						WorldRegionFolder.load(worldPath,
						controller.dimensionBox.getValue(), renderer, true)));
			} catch (IOException e) {
				folder.set(null);
				log.warn("Could not load world " + worldPath, e);
				ExceptionDialog d = new ExceptionDialog(e);
				d.setTitle("Could not load world");
				d.showAndWait();
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
		protected URI file;

		public SavedRegionFolderProvider(GuiController controller, URI file) {
			super(controller);
			this.file = file;
			reload();
		}

		@Override
		public void reload() {
			try {
				folder.set(new Pair<>(
						Integer.toHexString(file.toString().hashCode()),
						new RegionFolder.RemoteRegionFolder(file)));
			} catch (IOException e) {
				log.warn("Could not load from remote file " + file);
				folder.set(null);
				ExceptionDialog d = new ExceptionDialog(e);
				d.setTitle("Could not load world");
				d.showAndWait();
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
		protected Map<String, String>	worlds;

		/* Strong reference */
		private ChangeListener<String>	listener	= (o, old, val) -> {
														try {
															folder.set(new Pair<>(
																	Integer.toHexString(Objects.hash(file.toString(), controller.worldBox.getValue())),
																	new RegionFolder.RemoteRegionFolder(file.resolve(worlds.get(val)))));
														} catch (IOException e) {
															folder.set(null);
															log.warn("Could not load world " + val + " from remote file " + file);
															ExceptionDialog d = new ExceptionDialog(e);
															d.setTitle("Could not load world from remote file " + file);
															d.showAndWait();
														}
													};

		public SavedWorldProvider(GuiController controller, URI file) {
			super(controller);
			this.file = file;
			controller.worldBox.valueProperty().addListener(new WeakChangeListener<>(listener));
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
				ExceptionDialog d = new ExceptionDialog(e);
				d.setTitle("Could not load world");
				d.showAndWait();
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

	public static RegionFolderProvider create(GuiController controller, Path path) {
		if (Files.isDirectory(path)) {
			if (Files.exists(path.resolve("level.dat")))
				return new LocalWorldProvider(controller, path);
			else
				return new LocalRegionFolderProvider(controller, path);
		} else if (Files.exists(path) && path.getFileName().toString().equals("rendered.json.gz"))
			return new SavedRegionFolderProvider(controller, path.toUri());
		else if (Files.exists(path) && path.getFileName().toString().equals("index.json"))
			return new SavedWorldProvider(controller, path.toUri());
		else
			return null;
	}

	public static RegionFolderProvider create(GuiController controller, URI uri) {
		if (uri.toString().endsWith("rendered.json.gz"))
			return new SavedRegionFolderProvider(controller, uri);
		else if (uri.toString().endsWith("index.json"))
			return new SavedWorldProvider(controller, uri);
		else
			return null;
	}
}