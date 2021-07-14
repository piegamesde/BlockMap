package de.piegames.blockmap.gui.standalone;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
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
import net.dongliu.gson.GsonJava8TypeAdapterFactory;

public class HistoryManager {

	private static final Gson GSON = new GsonBuilder()
			.registerTypeAdapterFactory(new GsonJava8TypeAdapterFactory())
			.disableHtmlEscaping()
			.setPrettyPrinting()
			.create();
	private static Log log = LogFactory.getLog(HistoryManager.class);

	private final ProjectDirectories directories = ProjectDirectories.from("de", "piegames", "blockmap");
	private final Path cacheDir = Paths.get(directories.cacheDir);
	private final Path recentFile = cacheDir.resolve("recent.json");
	private final Path configDir = Paths.get(directories.configDir);
	private final Path configFile = configDir.resolve("config.json");

	/* TODO add saving and loading */
	private Config config = new Config();
	private ScheduledExecutorService backgroundThread;

	/** Recently loaded worlds and servers */
	public final ObservableList<HistoryItem> recentWorlds = FXCollections.observableArrayList();
	/** Whatever found in {@code .minecraft} for autocomplete purposes */
	public final ObservableList<HistoryItem> otherWorlds = FXCollections.observableArrayList();

	public HistoryManager(ScheduledExecutorService backgroundThread) {
		this.backgroundThread = Objects.requireNonNull(backgroundThread);

		try {
			Files.createDirectories(cacheDir);
			if (Files.exists(recentFile)) {
				@SuppressWarnings("serial")
				List<HistoryItem> list = GSON.fromJson(Files.newBufferedReader(recentFile), new TypeToken<List<HistoryItem>>() {
				}.getType());
				/* Evict outdated items */
				list.removeIf(item -> Duration.between(Instant.ofEpochMilli(item.timestamp), Instant.now()).toDays() > 14);

				recentWorlds.addAll(list);
			}
		} catch (RuntimeException | IOException e) {
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
									} catch (RuntimeException | IOException e) {
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
					} catch (RuntimeException | IOException e) {
						log.warn("Could not load worlds from saves folder", e);
					}
			}
		});
	}

	/**
	 * Callback when a world was loaded from the client. The given item will be added to {@link #recentWorlds}, which in turn will be saved to
	 * cache.
	 */
	public void onWorldLoaded(HistoryItem item) {
		/* Implementation note: deduplicating items (the same world loaded multiple times) is not required, as it will be done on loading. */
		recentWorlds.add(item);

		backgroundThread.execute(() -> {
			try (Writer writer = Files.newBufferedWriter(recentFile)) {
				GSON.toJson(recentWorlds, writer);
				writer.flush();
			} catch (RuntimeException | IOException e) {
				log.warn("Could not write recent loaded history", e);
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
