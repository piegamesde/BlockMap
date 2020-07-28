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
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import de.piegames.blockmap.DotMinecraft;
import de.piegames.nbt.CompoundTag;
import de.piegames.nbt.stream.NBTInputStream;
import io.github.soc.directories.ProjectDirectories;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class HistoryManager {

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static Log log = LogFactory.getLog(HistoryManager.class);

	private final ProjectDirectories directories = ProjectDirectories.from("de", "piegames", "blockmap");
	private final Path cacheDir = Paths.get(directories.cacheDir);
	private final Path cacheIndex = cacheDir.resolve("recent.json");
	private final Path configDir = Paths.get(directories.configDir);
	private final Path configFile = configDir.resolve("config.json");

	/* TODO add saving and loading */
	private Config config = new Config();

	/** Recently loaded worlds and servers */
	public final ObservableList<HistoryItem> recentWorlds = FXCollections.observableArrayList();
	/** Whatever found in {@code .minecraft} for autocomplete purposes */
	public final ObservableList<HistoryItem> otherWorlds = FXCollections.observableArrayList();

	@SuppressWarnings("serial")
	public HistoryManager(ScheduledExecutorService backgroundThread) {
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

		/* Load list of worlds in .minecraft/saves (in background) */
		backgroundThread.execute(() -> {
			for (Path saves : config.saveSearchPath) {
				if (Files.exists(saves) && Files.isDirectory(saves))
					try {
						List<HistoryItem> toAdd = Files.list(saves)
								.filter(Files::exists)
								.filter(Files::isDirectory)
								.filter(p -> Files.exists(p.resolve("level.dat")))
								.map(save -> {
									String name = save.getFileName().toString();
									long timestamp = 0;
									try (NBTInputStream in = new NBTInputStream(
											Files.newInputStream(save.resolve("level.dat")), NBTInputStream.GZIP_COMPRESSION
									)) {
										Optional<CompoundTag> data = in.readTag().getAsCompoundTag().flatMap(t -> t.getAsCompoundTag("Data"));
										name = data.flatMap(t -> t.getStringValue("LevelName")).orElse(null);
										timestamp = data.flatMap(t -> t.getLongValue("LastPlayed")).orElse(0L);
									} catch (IOException e) {
										log.warn("Could not read world name for " + save, e);
									}

									String imageURL = null;
									if (Files.exists(save.resolve("icon.png")))
										imageURL = save.resolve("icon.png").toUri().toString();
									return new HistoryItem(false, name, save.toAbsolutePath().toString(), imageURL, timestamp);
								})
								.sorted(Comparator.comparingLong(HistoryItem::lastAccessed).reversed())
								.collect(Collectors.toList());
						Platform.runLater(() -> otherWorlds.addAll(toAdd));
					} catch (IOException e) {
						log.warn("Could not load worlds from saves folder", e);
					}
			}
		});
	}

	public List<Path> getSaveSearchPath() {
		return config.saveSearchPath;
	}

	private static class Config {
		private List<Path> saveSearchPath;

		/** The default constructor gives the default config */
		Config() {
			saveSearchPath = Arrays.asList(
					DotMinecraft.DOTMINECRAFT.resolve("saves"),
					Paths.get("/srv/minecraft")
			);
		}
	}
}
