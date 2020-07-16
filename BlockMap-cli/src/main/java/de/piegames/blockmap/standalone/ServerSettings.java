package de.piegames.blockmap.standalone;

import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.gson.annotations.SerializedName;

import de.piegames.blockmap.MinecraftDimension;
import de.piegames.blockmap.renderer.RenderSettings;
import de.piegames.blockmap.standalone.DeserializeNullChecker.DeserializeNonNull;
import de.piegames.blockmap.world.LevelMetadata;
import de.piegames.blockmap.world.ServerMetadata;

public class ServerSettings {

	public RegionFolderSettings[] worlds = new RegionFolderSettings[0];
	@SerializedName("output dir")
	@DeserializeNonNull
	public Path outputDir;
	@SerializedName("server")
	public Optional<ServerMetadata> serverMetadata = Optional.empty();
	@SerializedName("show pins")
	public Optional<PinSettings> pinSettings = Optional.empty();

	public static class PinSettings {
		public static enum ShowPlayers {
			ALL, ONLINE_ONLY, NONE;
		}

		@SerializedName("players")
		public ShowPlayers showPlayers = ShowPlayers.ALL;
		@SerializedName("maps")
		public boolean showMaps = true;
		@SerializedName("slime chunks")
		public boolean showSlimeChunks = true;
		@SerializedName("force-loaded chunks")
		public boolean showLoadedChunks = true;
		@SerializedName("barrier")
		public boolean showBarrier = true;
		@SerializedName("world spawn")
		public boolean showWorldSpawn = true;
		@SerializedName("POIs")
		public Optional<Set<String>> showPOIs = Optional.empty();

		@SerializedName("structures")
		public Optional<Set<String>> showStructures = Optional.empty();

		/**
		 * Return a new {@link LevelMetadata} instance of a current one, but with all undesired entries
		 * stripped.
		 */
		public LevelMetadata apply(LevelMetadata metadata, Set<String> onlinePlayers) {
			return new LevelMetadata(
					metadata.getWorldName(),
					metadata.getPlayers().flatMap(players -> {
						switch (showPlayers) {
						case ALL:
							return Optional.of(players);
						case ONLINE_ONLY:
							return Optional.of(
									players.stream()
											.filter(player -> player.getUUID().map(onlinePlayers::contains).orElse(true))
											.collect(Collectors.toList()));
						case NONE:
							return Optional.empty();
								default:
									throw new InternalError();
						}
					}),
					metadata.getMaps().filter(__ -> showMaps),
					metadata.getVillageObjects().map(pins -> {
						if (showPOIs.isEmpty())
							return pins;
						else {
							var showPOIs = this.showPOIs.get();
							return pins.stream()
									.filter(pin -> showPOIs.contains(pin.getType()))
									.collect(Collectors.toList());
						}
					}),
					metadata.getSlimeChunks().filter(__ -> showSlimeChunks),
					metadata.getLoadedChunks().filter(__ -> showLoadedChunks),
					metadata.getBarrier().filter(__ -> showBarrier),
					metadata.getWorldSpawn().filter(__ -> showBarrier));
		}
	}

	public static class RegionFolderSettings {

		@DeserializeNonNull
		public String name;
		@SerializedName("input dir")
		@DeserializeNonNull
		public Path inputDir;
		public MinecraftDimension dimension = MinecraftDimension.OVERWORLD;
		public boolean force = false;
		@SerializedName("render settings")
		public RenderSettings renderSettings = new RenderSettings();
	}
}
