package de.piegames.blockmap;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.joml.Vector2i;
import org.joml.Vector2ic;

import com.flowpowered.nbt.regionfile.RegionFile;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonWriter;

import de.piegames.blockmap.renderer.RegionRenderer;

public abstract class RegionFolder {

	public static Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public abstract Set<Vector2ic> listRegions();

	public abstract BufferedImage render(Vector2ic pos) throws IOException;

	public static class WorldRegionFolder extends RegionFolder {

		static final Pattern					rfpat	= Pattern.compile("^r\\.(-?\\d+)\\.(-?\\d+)\\.mca$");

		protected final Map<Vector2ic, Path>	regions;
		protected final RegionRenderer			renderer;

		WorldRegionFolder(Map<Vector2ic, Path> files, RegionRenderer renderer) {
			this.regions = Objects.requireNonNull(files);
			this.renderer = Objects.requireNonNull(renderer);
		}

		@Override
		public Set<Vector2ic> listRegions() {
			return Collections.unmodifiableSet(regions.keySet());
		}

		@Override
		public BufferedImage render(Vector2ic pos) throws IOException {
			if (regions.containsKey(pos))
				return renderer.render(pos, new RegionFile(regions.get(pos)));
			else
				return null;
		}

		public Path getPath(Vector2ic pos) {
			return regions.get(pos);
		}

		public static WorldRegionFolder load(Path world, MinecraftDimension dimension, RegionRenderer renderer) throws IOException {
			return load(dimension.resolve(world), renderer);
		}

		public static WorldRegionFolder load(Path regionFolder, RegionRenderer renderer) throws IOException {
			Map<Vector2ic, Path> files = new HashMap<>();
			for (Path p : Files.list(regionFolder).collect(Collectors.toList())) {
				Matcher m = rfpat.matcher(p.getFileName().toString());
				if (m.matches())
					files.put(new Vector2i(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2))), p);
			}
			return new WorldRegionFolder(files, renderer);
		}
	}

	public static abstract class SavedRegionFolder<T> extends RegionFolder {

		protected final Map<Vector2ic, T> regions;

		protected SavedRegionFolder(Map<Vector2ic, T> regions) {
			this.regions = Collections.unmodifiableMap(regions);
		}

		@SuppressWarnings("unchecked")
		protected SavedRegionFolder(T file, String name) throws IOException {
			Map<String, JsonObject> saved = parseSaved(new JsonParser().parse(new InputStreamReader(getInputStream(file))));
			/* Parse the saved file and stream it to a map */
			JsonObject rawFile = null;
			if (name != null) {
				rawFile = saved.get(name);
				Objects.requireNonNull(rawFile, "The specified map was not found in this file");
			} else {
				if (saved.size() != 1)
					throw new IllegalArgumentException("The specified file contains more than one saved map, but no name was given");
				rawFile = saved.values().iterator().next();
			}
			regions = ((List<RegionHelper>) GSON.fromJson(rawFile.getAsJsonArray("regions"), new TypeToken<List<RegionHelper>>() {
			}.getType())).stream().collect(Collectors.toMap(r -> new Vector2i(r.x, r.z), r -> resolve(file, r.image)));
		}

		@Override
		public BufferedImage render(Vector2ic pos) throws IOException {
			if (regions.containsKey(pos))
				return ImageIO.read(getInputStream(regions.get(pos)));
			else
				return null;
		}

		protected abstract InputStream getInputStream(T path) throws IOException;

		protected abstract T resolve(T file, String relative);

		@Override
		public Set<Vector2ic> listRegions() {
			return Collections.unmodifiableSet(regions.keySet());
		}

		public T getPath(Vector2ic pos) {
			return regions.get(pos);
		}

		public static Map<String, JsonObject> parseSaved(JsonElement parsed) {
			Map<String, JsonObject> saved = new HashMap<>();
			if (parsed.isJsonArray()) {
				for (JsonElement e : parsed.getAsJsonArray()) {
					JsonObject o = e.getAsJsonObject();
					saved.put(o.getAsJsonPrimitive("name").getAsString(), o);
				}
			} else {
				JsonObject o = parsed.getAsJsonObject();
				saved.put(o.getAsJsonPrimitive("name").getAsString(), o);
			}
			return saved;
		}
	}

	public static class LocalRegionFolder extends SavedRegionFolder<Path> {
		protected LocalRegionFolder(Map<Vector2ic, Path> regions) {
			super(regions);
		}

		public LocalRegionFolder(Path file, String name) throws IOException {
			super(file, name);
		}

		@Override
		protected InputStream getInputStream(Path path) throws IOException {
			return Files.newInputStream(path);
		}

		@Override
		protected Path resolve(Path file, String relative) {
			return file.resolveSibling(Paths.get(relative));
		}
	}

	public static class RemoteRegionFolder extends SavedRegionFolder<URI> {

		protected RemoteRegionFolder(Map<Vector2ic, URI> regions) {
			super(regions);
		}

		public RemoteRegionFolder(URI file, String name) throws IOException {
			super(file, name);
		}

		@Override
		protected InputStream getInputStream(URI path) throws IOException {
			return path.toURL().openStream();
		}

		@Override
		protected URI resolve(URI file, String relative) {
			return file.resolve(relative);
		}
	}

	public static class CachedRegionFolder extends RegionFolder {

		protected WorldRegionFolder	world;
		protected boolean			lazy;
		protected Path				imageFolder;

		public CachedRegionFolder(WorldRegionFolder world, boolean lazy, Path imageFolder) {
			this.lazy = lazy;
			this.world = Objects.requireNonNull(world);
			this.imageFolder = Objects.requireNonNull(imageFolder);
		}

		@Override
		public BufferedImage render(Vector2ic pos) throws IOException {
			Path region = world.getPath(pos);
			if (region == null)
				return null;
			Path image = imageFolder.resolve(region.getFileName().toString().replace(".mca", ".png"));
			if (Files.exists(image)
					&& (!lazy || Files.getLastModifiedTime(image).compareTo(Files.getLastModifiedTime(region)) > 0)) {
				return ImageIO.read(Files.newInputStream(image));
			} else {
				BufferedImage rendered = world.render(pos);
				ImageIO.write(rendered, "png", Files.newOutputStream(image));
				return rendered;
			}
		}

		@Override
		public Set<Vector2ic> listRegions() {
			return world.listRegions();
		}

		public LocalRegionFolder save() {
			Map<Vector2ic, Path> regions = new HashMap<>();
			for (Entry<Vector2ic, Path> e : world.regions.entrySet())
				regions.put(new Vector2i(e.getKey().x(), e.getKey().y()), imageFolder.resolve(e.getValue().getFileName().toString().replace(".mca", ".png")));
			return new LocalRegionFolder(regions);
		}

		public void save(Path file, String name) throws IOException {
			save(file, name, true);
		}

		public void save(Path file, String name, boolean relativePaths) throws IOException {
			Collection<JsonObject> existing = Files.exists(file) ? SavedRegionFolder.parseSaved(new JsonParser().parse(new String(Files.readAllBytes(file))))
					.values() : Collections.emptyList();
			try (JsonWriter writer = RegionFolder.GSON.newJsonWriter(Files.newBufferedWriter(file, StandardOpenOption.CREATE,
					StandardOpenOption.TRUNCATE_EXISTING))) {
				if (!existing.isEmpty()) {
					writer.beginArray();
					for (JsonElement e : existing)
						writer.jsonValue(new Gson().toJson(e));
				}
				writer.beginObject();
				writer.name("name");
				writer.value(name);
				writer.name("regions");
				writer.beginArray();
				for (Entry<Vector2ic, Path> e : world.regions.entrySet()) {
					writer.beginObject();

					writer.name("x");
					writer.value(e.getKey().x());
					writer.name("z");
					writer.value(e.getKey().y());
					writer.name("image");
					Path value = imageFolder.resolve(e.getValue().getFileName().toString().replace(".mca", ".png"));
					if (relativePaths) {
						value = file.getParent().relativize(value);
					}
					writer.value(value.toString());

					writer.endObject();
				}
				writer.endArray();
				writer.beginObject();
				if (!existing.isEmpty())
					writer.endArray();
				writer.flush();
			}
		}
	}

	public static class RegionHelper {
		int		x, z;
		String	image;
	}
}