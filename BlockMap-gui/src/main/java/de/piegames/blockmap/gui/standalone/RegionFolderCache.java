package de.piegames.blockmap.gui.standalone;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import de.piegames.blockmap.world.RegionFolder;
import de.piegames.blockmap.world.RegionFolder.CachedRegionFolder;
import io.github.soc.directories.ProjectDirectories;

public class RegionFolderCache {

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static Log log = LogFactory.getLog(RegionFolderCache.class);

	private final ProjectDirectories directories = ProjectDirectories.from("de", "piegames", "blockmap");
	private final Path cacheDir = Paths.get(directories.cacheDir).resolve("regions");
	private final Path cacheIndex = cacheDir.resolve("cache.json");

	private final Map<String, CachedRegionFolder>	inUse		= new HashMap<>();

	@SuppressWarnings("serial")
	public RegionFolderCache() {
		try {
			Files.createDirectories(cacheDir);
			Map<String, Long> cache;
			try {
				cache = GSON.fromJson(Files.newBufferedReader(cacheIndex), new TypeToken<Map<String, Long>>() {
				}.getType());
			} catch (NoSuchFileException e) {
				cache = new HashMap<>();
			}
			int before = cache.size();
			cache.values().removeIf(l -> Duration.between(Instant.ofEpochMilli(l), Instant.now()).toDays() > 14);
			cache.keySet().removeIf(path -> Files.notExists(cacheDir.resolve(path)) || !Files.isDirectory(cacheDir.resolve(path)));

			for (Path path : Files.newDirectoryStream(cacheDir)) {
				if (!cache.containsKey(path.getFileName().toString())) {
					log.debug("Removing world " + path + " from cache");
					Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
						@Override
						public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
							Files.delete(file);
							return FileVisitResult.CONTINUE;
						}

						@Override
						public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
							Files.delete(dir);
							return FileVisitResult.CONTINUE;
						}
					});
				}
			}

			log.info("Removed " + (before - cache.size()) + " worlds from cache");
			try (Writer writer = Files.newBufferedWriter(cacheIndex)) {
				GSON.toJson(cache, writer);
				writer.flush();
			}
		} catch (IOException e) {
			log.warn("Could not initialize cache", e);
		}
	}

	/** Wrap a given {@link RegionFolder} in a {@link RegionFolderCache} if needed and mark this cache id as used. */
	public synchronized RegionFolder cache(RegionFolder input, String id) {
		if (input == null || !input.needsCaching()) {
			if (input != null)
				log.debug("No caching needed for the world (id: " + id + ")");
			return input;
		}
		if (inUse.containsKey(id)) {
			log.warn("Could not cache world with id '" + id + "' because it is already in use");
			return input;
		}

		try {
			log.debug("Caching world with id: " + id);
			@SuppressWarnings("serial")
			Map<String, Long> cache = GSON.fromJson(Files.newBufferedReader(cacheIndex), new TypeToken<Map<String, Long>>() {
			}.getType());

			cache.put(id, Instant.now().toEpochMilli());
			input = CachedRegionFolder.create(input, true, cacheDir.resolve(id));

			try (Writer writer = Files.newBufferedWriter(cacheIndex)) {
				GSON.toJson(cache, writer);
				writer.flush();
			}
			inUse.put(id, (CachedRegionFolder) input);
		} catch (RuntimeException | IOException e) {
			log.warn("Could not cache world with id '" + id + "'", e);
		}
		return input;
	}

	public synchronized void saveAll() {
		inUse.values().forEach(t -> {
			try {
				t.save();
			} catch (IOException e) {
				log.warn("Could not save changes to cache", e);
			}
		});
	}

	/** Mark this cache id as not used anymore so it may be re-used again. */
	public synchronized void releaseCache(String id) {
		if (inUse.containsKey(id))
			try {
				inUse.remove(id).save();
			} catch (IOException e) {
				log.warn("Could not save changes to cache", e);
			}
	}
}
