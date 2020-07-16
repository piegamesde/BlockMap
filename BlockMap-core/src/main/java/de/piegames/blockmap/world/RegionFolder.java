package de.piegames.blockmap.world;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
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
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.imageio.ImageIO;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joml.Vector2d;
import org.joml.Vector2dc;
import org.joml.Vector2i;
import org.joml.Vector2ic;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3i;
import org.joml.Vector3ic;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

import de.piegames.blockmap.MinecraftDimension;
import de.piegames.blockmap.renderer.RegionRenderer;
import de.piegames.blockmap.world.ChunkMetadata.ChunkRenderState;
import de.piegames.blockmap.world.RegionFolder.SavedRegionHelper.RegionHelper;
import de.piegames.nbt.regionfile.RegionFile;
import io.gsonfire.GsonFireBuilder;
import io.gsonfire.annotations.Exclude;
import io.gsonfire.annotations.ExposeMethodParam;
import io.gsonfire.annotations.ExposeMethodResult;
import net.dongliu.gson.GsonJava8TypeAdapterFactory;

/**
 * This class represents a mapping from region file positions in a world to {@link BufferedImage}s
 * of that rendered region. How this is done is up to the implementation.
 */
public abstract class RegionFolder {

	private static Log log = LogFactory.getLog(RegionFolder.class);

	public static final Gson GSON = new GsonFireBuilder()
			.enableExposeMethodParam()
			.enableExposeMethodResult()
			.enableExcludeByAnnotation()
			.enableHooks(RegionHelper.class)
			.enableHooks(LevelMetadata.MapPin.class)
			.registerTypeSelector(Vector2ic.class, e -> Vector2i.class)
			.registerTypeSelector(Vector3ic.class, e -> Vector3i.class)
			.registerTypeSelector(Vector2dc.class, e -> Vector2d.class)
			.registerTypeSelector(Vector3dc.class, e -> Vector3d.class)
			.registerTypeSelector(ChunkMetadata.class, e -> ChunkRenderState.valueOf(e.getAsJsonObject().getAsJsonPrimitive("renderState").getAsString()).clazz)
			.createGsonBuilder()
			.registerTypeAdapterFactory(
					new GsonJava8TypeAdapterFactory())
			.disableHtmlEscaping()
			// .setPrettyPrinting()
			.create();

	/**
	 * Lists all existing region file in this RegionFolder. If one of the returned positions is passed
	 * to {@link #render(Vector2ic)}, it must not return {@code null}.
	 */
	public abstract Set<Vector2ic> listRegions();

	/**
	 * Generates an image of the region file at the given position, however this might be done. Will
	 * return {@code null} if the passed position is not contained in {@link #listRegions()}. This
	 * method will block until the image data is retrieved and may throw an exception if it fails to do
	 * so.
	 * 
	 * @param pos
	 *            the position of the region file to render
	 * @return the rendered region file as {@link BufferedImage} or {@code null} if
	 *         {@code listRegions().contains(pos)} evaluates to {@code false}
	 * @throws IOException
	 *             if the image could not be retrieved
	 */
	public abstract Region render(Vector2ic pos) throws IOException;

	/**
	 * Get the time the region file at {@code pos} was last modified. This will be used to determine if
	 * a cached rendered image of that file is still valid or not.
	 * 
	 * @see Files#getLastModifiedTime(Path, java.nio.file.LinkOption...)
	 */
	public abstract long getTimestamp(Vector2ic pos) throws IOException;

	/**
	 * Get a timestamp that represents when the world was rendered.
	 */
	public abstract long getTimestamp();

	/** Returns the pins of this specific world or {@code Optional.empty()} if they are not loaded. */
	public abstract Optional<LevelMetadata> getPins();

	/**
	 * Whether the render process of this region folder may need caching or not.
	 */
	public abstract boolean needsCaching();

	/**
	 * This {@link RegionFolder} implementation will render region files using a {@link RegionRenderer}.
	 * Calling {@link #render(Vector2ic)} repeatedly on the same location will render the same image
	 * multiple times.
	 */
	public static class WorldRegionFolder extends RegionFolder {

		static final Pattern rfpat = Pattern.compile("^r\\.(-?\\d+)\\.(-?\\d+)\\.mca$");

		protected final Map<Vector2ic, Path> regions;
		protected final RegionRenderer renderer;
		protected LevelMetadata pins;
		protected final long timestamp;

		/**
		 * @param file
		 *            map region coordinates to paths, which point to the respective file. Those are treated
		 *            as the "world" represented by this RegionFolder. May not be {@code null}.
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
			this.timestamp = System.currentTimeMillis();
		}

		@Override
		public Set<Vector2ic> listRegions() {
			return Collections.unmodifiableSet(regions.keySet());
		}

		@Override
		public Region render(Vector2ic pos) throws IOException {
			if (regions.containsKey(pos)) {
				var path = regions.get(pos);
				try (RegionFile file = new RegionFile(path, StandardOpenOption.READ)) {
					return renderer.render(pos, file);
				} catch (RuntimeException | IOException e) {
					if (Files.size(path) == 0) {
						log.warn(path.getFileName() + " is empty?!");
						return new Region(pos, new BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB), new HashMap<>());
					} else
						throw e;
				}
			} else
				return null;
		}

		@Override
		public long getTimestamp(Vector2ic pos) throws IOException {
			return Files.getLastModifiedTime(regions.get(pos)).toMillis();
		}

		@Override
		public long getTimestamp() {
			return timestamp;
		}

		@Override
		public boolean needsCaching() {
			return true;
		}

		@Override
		public Optional<LevelMetadata> getPins() {
			return Optional.ofNullable(pins);
		}

		public void setPins(LevelMetadata pins) {
			this.pins = pins;
		}

		/**
		 * Loads a region folder from a given world path.
		 * 
		 * @param world
		 *            the path to the world folder. It has to be a directory pointing to a valid Minecraft
		 *            world. World folders usually contain a {@code level.dat} file.
		 * @param dimension
		 *            the Minecraft dimension to render. It will be used to resolve the region folder path
		 *            from the world path.
		 * @param loadPins
		 *            if the pins should be loaded too
		 * @see #load(Path, MinecraftDimension, RegionRenderer)
		 */
		public static WorldRegionFolder load(Path world, MinecraftDimension dimension, RegionRenderer renderer, boolean loadPins) throws IOException {
			WorldRegionFolder folder = load(world.resolve(dimension.getRegionPath()), renderer);
			if (loadPins)
				folder.setPins(LevelMetadata.loadFromWorld(world, dimension));
			return folder;
		}

		/**
		 * Loads a region folder from a given path. All region files found in this folder (not searching
		 * recursively) will be added to the returned object. Files added later on won't be recognized.
		 * Removing files will lead to errors when trying to render them. All files whose name matches
		 * {@code ^r\.(-?\d+)\.(-?\d+)\.mca$} are taken. If one of them isn't a proper region file,
		 * rendering it will fail.
		 * 
		 * @param regionFolder
		 *            the path to the folder containing all region files. This folder is commonly called
		 *            {@code region} and is situated inside a Minecraft world, but this is not a hard
		 *            requirement. It has to be a directory.
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
	 * A RegionFolder implementation that loads already rendered images from the disk. To find them, a
	 * save file is passed in the constructor. It is abstract to work on local systems as well as on
	 * remote servers.
	 * 
	 * @see LocalRegionFolder LocalRegionFolder for loading files on your hard drive
	 * @see RemoteRegionFolder RemoteRegionFolder for loading files via uri, either local or on servers
	 * @param T
	 *            the type of the file mapping, like URL, URI, Path, File, etc.
	 */
	public static abstract class SavedRegionFolder<T> extends RegionFolder {

		/** The path to the metadata.json file. All paths are relative to this. */
		protected final T basePath;
		protected final Map<Vector2ic, RegionHelper> regions;
		protected final Optional<LevelMetadata> pins;
		protected final long timestamp;

		/**
		 * Loads a json file that contains the information about all rendered files.
		 * 
		 * @see #parseSaved(JsonElement)
		 */
		protected SavedRegionFolder(T file) throws IOException {
			this.basePath = file;
			SavedRegionHelper helper = load(file);
			pins = Optional.ofNullable(helper.pins);
			regions = Optional.ofNullable(helper.regions)
					.stream().flatMap(Collection::stream)
					.collect(Collectors.toMap(r -> new Vector2i(r.x, r.z), Function.identity()));
			timestamp = helper.timestamp;
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

		protected abstract SavedRegionHelper load(T basePath) throws IOException;

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
		public long getTimestamp() {
			return timestamp;
		}

		@Override
		public Optional<LevelMetadata> getPins() {
			return pins;
		}
	}

	/**
	 * An implementation of {@link SavedRegionFolder} based on the Java {@link Path} API. Use it for
	 * accessing files from your local file system, but Java paths work with other URI schemata as well.
	 * Check {@link FileSystemProvider#installedProviders()} for more information. (There is even an
	 * URLSystemProvider somewhere on GitHub …)
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
		public SavedRegionHelper load(Path basePath) throws IOException {
			try (Reader reader = new InputStreamReader(new GZIPInputStream(getInputStream(basePath)))) {
				return GSON.fromJson(reader, SavedRegionHelper.class);
			}
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
			if (regions.containsKey(pos))
				return getSibling(basePath, regions.get(pos).image);
			else
				return null;
		}
	}

	/**
	 * An implementation of {@link SavedRegionFolder} based on URIs. It is intended for primary use on
	 * remote servers, but with the {@code file} schema it can open local files as well.
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
		protected SavedRegionHelper load(URI basePath) throws IOException {
			try (Reader reader = new InputStreamReader(new GZIPInputStream(getInputStream(basePath), 8192))) {
				return GSON.fromJson(reader, SavedRegionHelper.class);
			}
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
	 * This {@link RegionFolder} wraps a {@link WorldRegionFolder} in a way that each rendered image
	 * will be written to disk to avoid re-rendering. It can be used to create save files to load in
	 * {@link SavedRegionFolder}s.
	 */
	public static class CachedRegionFolder extends LocalRegionFolder {

		protected RegionFolder world;
		protected boolean lazy;

		/**
		 * @param cached
		 *            the renderer used to create images of region files if they haven't been rendered yet
		 * @param lazy
		 *            if set to false, no cached files will be returned for re-rendering. If set to true, a
		 *            re-render will load the image from disk if the respective region file has not been
		 *            modified since then (based on the timestamp). Laziness has the effect that changing
		 *            the render settings will not cause already rendered files to be updated.
		 * @throws IOException
		 */
		protected CachedRegionFolder(RegionFolder cached, boolean lazy, Path file) throws IOException {
			super(file);
			this.lazy = lazy;
			this.world = Objects.requireNonNull(cached);
		}

		/**
		 * If the image folder already contains a matching image for this position <b>and</b> the
		 * {@code lazy} flag was set in the constructor <b>and</b> the saved file is newer than the region
		 * file, this image will be returned. Otherwise, it will be rendered again and written to disk.
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
					&& world.getTimestamp(pos) < helper.lastModified) {
				return new Region(pos,
						super.render(helper),
						helper.metadata);
			} else {
				Region rendered = world.render(pos);
				String imageName = "r." + pos.x() + "." + pos.y() + ".png";
				Path imagePath = getSibling(basePath, imageName);
				ImageIO.write(rendered.getImage(), "png", Files.newOutputStream(imagePath));
				synchronized (regions) {
					regions.put(pos, new RegionHelper(pos.x(), pos.y(), Files.getLastModifiedTime(imagePath).toMillis(), imageName, rendered.metadata));
				}
				return rendered;
			}
		}

		@Override
		public Set<Vector2ic> listRegions() {
			return world.listRegions();
		}

		@Override
		public Optional<LevelMetadata> getPins() {
			return world.getPins();
		}

		@Override
		public long getTimestamp() {
			return world.getTimestamp();
		}

		public void save() throws IOException {
			synchronized (regions) {
				try (Writer writer = new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(basePath,
						StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING), 8192, true))) {
					GSON.toJson(new SavedRegionHelper(regions.values(), getPins().orElse(null), getTimestamp()), writer);
					writer.flush();
				}
			}
		}

		public static CachedRegionFolder create(RegionFolder cached, boolean lazy, Path folder) throws IOException {
			if (!Files.exists(folder))
				Files.createDirectories(folder);
			Path file = folder.resolve("rendered.json.gz");
			if (!Files.exists(file))
				try (Writer writer = new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(file, StandardOpenOption.CREATE,
						StandardOpenOption.WRITE), 8192, true))) {
					writer.write("{regions:[]}");
					writer.flush();
				}
			return new CachedRegionFolder(cached, lazy, file);
		}

	}

	/** Object representation of the content of the {@code rendered.json} metadata file. */
	static class SavedRegionHelper {
		Collection<RegionHelper> regions;
		LevelMetadata pins;
		long timestamp;

		public SavedRegionHelper(Collection<RegionHelper> regions, LevelMetadata pins, long timestamp) {
			this.regions = regions;
			this.pins = pins;
			this.timestamp = timestamp;
		}

		static class RegionHelper {
			int x, z;
			long lastModified;
			String image;
			@Exclude
			Map<? extends Vector2ic, ChunkMetadata> metadata;

			RegionHelper() {

			}

			public RegionHelper(int x, int z, long lastModified, String image, Map<? extends Vector2ic, ChunkMetadata> metadata) {
				this.x = x;
				this.z = z;
				this.lastModified = lastModified;
				this.image = image;
				this.metadata = metadata;
			}

			@ExposeMethodResult("metadata")
			private Collection<ChunkMetadata> postSerialize() {
				return metadata != null ? metadata.values() : Collections.emptyList();
			}

			@ExposeMethodParam("metadata")
			private void postDeserialize(Collection<ChunkMetadata> metadata) {
				this.metadata = Optional.ofNullable(metadata).stream().flatMap(Collection::stream)
						.collect(Collectors.toMap(meta -> meta.position, Function.identity()));
			}
		}
	}
}