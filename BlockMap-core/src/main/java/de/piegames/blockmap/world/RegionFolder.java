package de.piegames.blockmap.world;

import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.spi.FileSystemProvider;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import org.joml.Vector2i;
import org.joml.Vector2ic;
import org.joml.Vector3i;
import org.joml.Vector3ic;

import com.flowpowered.nbt.regionfile.RegionFile;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

import de.piegames.blockmap.MinecraftDimension;
import de.piegames.blockmap.renderer.RegionRenderer;
import de.piegames.blockmap.world.ChunkMetadata.ChunkRenderState;
import de.piegames.blockmap.world.RegionFolder.SavedRegionHelper.RegionHelper;
import io.gsonfire.GsonFireBuilder;
import io.gsonfire.annotations.PostDeserialize;
import io.gsonfire.annotations.PreSerialize;

/**
 * This class represents a mapping from region file positions in a world to {@link BufferedImage}s of that rendered region. How this is done
 * is up to the implementation.
 */
public abstract class RegionFolder {

	public static final Gson GSON = new GsonFireBuilder()
			.enableExposeMethodResult()
			.enableHooks(RegionHelper.class)
			.registerTypeSelector(Vector2ic.class, e -> Vector2i.class)
			.registerTypeSelector(Vector3ic.class, e -> Vector3i.class)
			.registerTypeSelector(ChunkMetadata.class, e -> ChunkRenderState.valueOf(e.getAsJsonObject().getAsJsonPrimitive("renderState").getAsString()).clazz)
			.createGsonBuilder()
			.disableHtmlEscaping()
			.addSerializationExclusionStrategy(new ExclusionStrategy() {

				@Override
				public boolean shouldSkipField(FieldAttributes f) {
					return f.hasModifier(Modifier.TRANSIENT);
				}

				@Override
				public boolean shouldSkipClass(Class<?> clazz) {
					return false;
				}
			})
			.setPrettyPrinting()
			.create();

	/**
	 * Lists all existing region file in this RegionFolder. If one of the returned positions is passed to {@link #render(Vector2ic)}, it must
	 * not return {@code null}.
	 */
	public abstract Set<Vector2ic> listRegions();

	/**
	 * Generates an image of the region file at the given position, however this might be done. Will return {@code null} if the passed position
	 * is not contained in {@link #listRegions()}. This method will block until the image data is retrieved and may throw an exception if it
	 * fails to do so.
	 * 
	 * @param pos
	 *            the position of the region file to render
	 * @return the rendered region file as {@link BufferedImage} or {@code null} if {@code listRegions().contains(pos)} evaluates to
	 *         {@code false}
	 * @throws IOException
	 *             if the image could not be retrieved
	 */
	public abstract Region render(Vector2ic pos) throws IOException;

	/**
	 * TODO
	 */
	public abstract long getTimestamp(Vector2ic pos) throws IOException;

	/** Returns the pins of this specific world or {@code Optional.empty()} if they are not loaded. */
	public abstract Optional<WorldPins> getPins();

	/**
	 * TODO
	 */
	public abstract boolean needsCaching();

	/**
	 * This {@link RegionFolder} implementation will render region files using a {@link RegionRenderer}. Calling {@link #render(Vector2ic)}
	 * repeatedly on the same location will render the same image multiple times.
	 */
	public static class WorldRegionFolder extends RegionFolder {

		static final Pattern					rfpat	= Pattern.compile("^r\\.(-?\\d+)\\.(-?\\d+)\\.mca$");

		protected final Map<Vector2ic, Path>	regions;
		protected final RegionRenderer			renderer;
		protected WorldPins						pins;

		/**
		 * @param file
		 *            map region coordinates to paths, which point to the respective file. Those are treated as the "world" represented by this
		 *            RegionFolder. May not be {@code null}.
		 * @param pins
		 *            The pins of this world. May not be {@code null}.
		 * @param renderer
		 *            the {@link RegionRenderer} used to render the files. May not be {@code null}.
		 * @see #load(Path, RegionRenderer)
		 * @see #load(Path, MinecraftDimension, RegionRenderer)
		 * @throws NullPointerException
		 *             if any of the arguments is {@code null}
		 */
		public WorldRegionFolder(Map<Vector2ic, Path> files, RegionRenderer renderer) {
			this.regions = Objects.requireNonNull(files);
			this.renderer = Objects.requireNonNull(renderer);
		}

		@Override
		public Set<Vector2ic> listRegions() {
			return Collections.unmodifiableSet(regions.keySet());
		}

		@Override
		public Region render(Vector2ic pos) throws IOException {
			if (regions.containsKey(pos))
				return renderer.render(pos, new RegionFile(regions.get(pos)));
			else
				return null;
		}

		@Override
		public long getTimestamp(Vector2ic pos) throws IOException {
			return Files.getLastModifiedTime(regions.get(pos)).toMillis();
		}

		@Override
		public boolean needsCaching() {
			return true;
		}

		@Override
		public Optional<WorldPins> getPins() {
			return Optional.ofNullable(pins);
		}

		public void setPins(WorldPins pins) {
			this.pins = pins;
		}

		/**
		 * Loads a region folder from a given world path.
		 * 
		 * @param world
		 *            the path to the world folder. It has to be a directory pointing to a valid Minecraft world. World folders usually contain a
		 *            {@code level.dat} file.
		 * @param dimension
		 *            the Minecraft dimension to render. It will be used to resolve the region folder path from the world path.
		 * @param loadPins
		 *            if the pins should be loaded too
		 * @see #load(Path, MinecraftDimension, RegionRenderer)
		 */
		public static WorldRegionFolder load(Path world, MinecraftDimension dimension, RegionRenderer renderer, boolean loadPins) throws IOException {
			WorldRegionFolder folder = load(world.resolve(dimension.getRegionPath()), renderer);
			if (loadPins)
				folder.setPins(WorldPins.loadFromWorld(world, dimension));
			return folder;
		}

		/**
		 * Loads a region folder from a given path. All region files found in this folder (not searching recursively) will be added to the returned
		 * object. Files added later on won't be recognized. Removing files will lead to errors when trying to render them. All files whose name
		 * matches {@code ^r\.(-?\d+)\.(-?\d+)\.mca$} are taken. If one of them isn't a proper region file, rendering it will fail.
		 * 
		 * @param regionFolder
		 *            the path to the folder containing all region files. This folder is commonly called {@code region} and is situated inside a
		 *            Minecraft world, but this is not a hard requirement. It has to be a directory.
		 */
		public static WorldRegionFolder load(Path regionFolder, RegionRenderer renderer) throws IOException {
			Map<Vector2ic, Path> files = new HashMap<>();
			try (Stream<Path> stream = Files.list(regionFolder)) {
				for (Path p : (Iterable<Path>) stream::iterator) {
					Matcher m = rfpat.matcher(p.getFileName().toString());
					if (m.matches())
						files.put(new Vector2i(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2))), p);
				}
			}
			return new WorldRegionFolder(files, renderer);
		}
	}

	/**
	 * A RegionFolder implementation that loads already rendered images from the disk. To find them, a save file is passed in the constructor.
	 * It is abstract to work on local systems as well as on remote servers.
	 * 
	 * @see LocalRegionFolder LocalRegionFolder for loading files on your hard drive
	 * @see RemoteRegionFolder RemoteRegionFolder for loading files via uri, either local or on servers
	 * @param T
	 *            the type of the file mapping, like URL, URI, Path, File, etc.
	 */
	public static abstract class SavedRegionFolder<T> extends RegionFolder {

		/** The path to the metadata.json file. All paths are relative to this. */
		protected final T								basePath;
		protected final Map<Vector2ic, RegionHelper>	regions;
		protected final Optional<WorldPins>				pins;

		/**
		 * Loads a json file that contains the information about all rendered files.
		 * 
		 * @see #parseSaved(JsonElement)
		 */
		protected SavedRegionFolder(T file) throws IOException {
			this.basePath = file;
			SavedRegionHelper helper = GSON.fromJson(new InputStreamReader(getInputStream(file)), SavedRegionHelper.class);
			pins = Optional.ofNullable(helper.pins);
			regions = Optional.ofNullable(helper.regions)
					.stream().flatMap(Collection::stream)
					.collect(Collectors.toMap(r -> new Vector2i(r.x, r.z), Function.identity()));
		}

		@Override
		public Region render(Vector2ic pos) throws IOException {
			RegionHelper helper = regions.get(pos);
			if (helper == null)
				return null;
			return new Region(pos, render(helper), helper.metadata);
		}

		/** Mapping from the path type T to an input stream. */
		protected abstract InputStream getInputStream(T basePath) throws IOException;

		protected abstract T getSibling(T basePath, String sibling);

		protected BufferedImage render(RegionHelper rawRegion) throws IOException {
			return ImageIO.read(getInputStream(getSibling(basePath, rawRegion.image)));
		}

		@Override
		public Set<Vector2ic> listRegions() {
			return Collections.unmodifiableSet(regions.keySet());
		}

		@Override
		public long getTimestamp(Vector2ic pos) throws IOException {
			return regions.get(pos).lastModified;
		}

		@Override
		public Optional<WorldPins> getPins() {
			return pins;
		}
	}

	/**
	 * An implementation of {@link SavedRegionFolder} based on the Java {@link Path} API. Use it for accessing files from your local file
	 * system, but Java paths work with other URI schemata as well. Check {@link FileSystemProvider#installedProviders()} for more information.
	 * (There is even an URLSystemProvider somewhere on GitHub ...)
	 */
	public static class LocalRegionFolder extends SavedRegionFolder<Path> {

		public LocalRegionFolder(Path file) throws IOException {
			super(file);
		}

		@Override
		protected InputStream getInputStream(Path path) throws IOException {
			return Files.newInputStream(path);
		}

		@Override
		protected Path getSibling(Path basePath, String sibling) {
			return basePath.resolveSibling(sibling);
		}

		@Override
		public boolean needsCaching() {
			return false;
		}

		public Path getPath(Vector2ic pos) {
			return getSibling(basePath, regions.get(pos).image);
		}
	}

	/**
	 * An implementation of {@link SavedRegionFolder} based on URIs. It is intended for primary use on remote servers, but with the {@code file}
	 * schema it can open local files as well.
	 */
	public static class RemoteRegionFolder extends SavedRegionFolder<URI> {

		public RemoteRegionFolder(URI file) throws IOException {
			super(file);
		}

		@Override
		protected InputStream getInputStream(URI path) throws IOException {
			return path.toURL().openStream();
		}

		@Override
		protected URI getSibling(URI basePath, String sibling) {
			return basePath.resolve(sibling);
		}

		@Override
		public boolean needsCaching() {
			return true;
		}
	}

	/**
	 * This {@link RegionFolder} wraps a {@link WorldRegionFolder} in a way that each rendered image will be written to disk to avoid
	 * re-rendering. It can be used to create save files to load in {@link SavedRegionFolder}s.
	 */
	public static class CachedRegionFolder extends LocalRegionFolder {

		protected RegionFolder	world;
		protected boolean		lazy;

		/**
		 * @param cached
		 *            the renderer used to create images of region files if they haven't been rendered yet
		 * @param lazy
		 *            if set to false, no cached files will be returned for re-rendering. If set to true, a re-render will load the image from disk
		 *            if the respective region file has not been modified since then (based on the timestamp). Laziness has the effect that changing
		 *            the render settings will not cause already rendered files to be updated.
		 * @param imageFolder
		 *            the folder where all the rendered images will be stored. The images will be named like their region file name, but with the
		 *            {@code .mca} replaced with {@code .png}.
		 * @throws IOException
		 */
		protected CachedRegionFolder(RegionFolder cached, boolean lazy, Path file) throws IOException {
			super(file);
			this.lazy = lazy;
			this.world = Objects.requireNonNull(cached);
		}

		/**
		 * If the image folder already contains a matching image for this position <b>and</b> the {@code lazy} flag was set in the constructor
		 * <b>and</b> the saved file is newer than the region file, this image will be returned. Otherwise, it will be rendered again and written to
		 * disk.
		 * 
		 * @see SavedRegionFolder#render(Vector2ic)
		 */
		@Override
		public Region render(Vector2ic pos) throws IOException {
			if (!listRegions().contains(pos))
				return null;
			RegionHelper helper = regions.get(pos);
			if (helper != null
					&& lazy
					&& world.getTimestamp(pos) > helper.lastModified) {
				return new Region(pos,
						super.render(helper),
						helper.metadata);
			} else {
				Region rendered = world.render(pos);
				String imageName = "r." + pos.x() + "." + pos.y() + ".png";
				Path imagePath = getSibling(basePath, imageName);
				ImageIO.write(rendered.getImage(), "png", Files.newOutputStream(imagePath));
				regions.put(pos, new RegionHelper(pos.x(), pos.y(), Files.getLastModifiedTime(imagePath).toMillis(), imageName, rendered.metadata));
				return rendered;
			}
		}

		@Override
		public Set<Vector2ic> listRegions() {
			return world.listRegions();
		}

		@Override
		public Optional<WorldPins> getPins() {
			return world.getPins();
		}

		public void save() throws IOException {
			try (BufferedWriter writer = Files.newBufferedWriter(basePath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
				GSON.toJson(new SavedRegionHelper(regions.values(), getPins().orElse(null)), writer);
				writer.flush();
			}
		}

		public static CachedRegionFolder create(RegionFolder cached, boolean lazy, Path folder) throws IOException {
			if (!Files.exists(folder))
				Files.createDirectories(folder);
			Path file = folder.resolve("rendered.json");
			if (!Files.exists(file))
				Files.writeString(file, "{regions:[]}");
			return new CachedRegionFolder(cached, lazy, file);
		}

	}

	static class SavedRegionHelper {
		Collection<RegionHelper>	regions;
		WorldPins					pins;

		public SavedRegionHelper(Collection<RegionHelper> regions, WorldPins pins) {
			this.regions = regions;
			this.pins = pins;
		}

		static class RegionHelper {
			int													x, z;
			long												lastModified;
			String												image;
			transient Map<? extends Vector2ic, ChunkMetadata>	metadata;
			private Collection<ChunkMetadata>					metadata2;

			RegionHelper() {

			}

			public RegionHelper(int x, int z, long lastModified, String image, Map<? extends Vector2ic, ChunkMetadata> metadata) {
				this.x = x;
				this.z = z;
				this.lastModified = lastModified;
				this.image = image;
				this.metadata = metadata;
			}

			@PreSerialize
			private void preSerialize() {
				metadata2 = metadata != null ? metadata.values() : Collections.emptyList();
				metadata = null;
			}

			@PostDeserialize
			private void postDeserialize() {
				metadata = Optional.ofNullable(metadata2).stream().flatMap(Collection::stream)
						.collect(Collectors.toMap(meta -> meta.position, Function.identity()));
				metadata2 = null;
			}
		}
	}
}