package de.piegames.blockmap.world;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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

import com.flowpowered.nbt.ByteArrayTag;
import com.flowpowered.nbt.ByteTag;
import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.DoubleTag;
import com.flowpowered.nbt.IntTag;
import com.flowpowered.nbt.ListTag;
import com.flowpowered.nbt.LongTag;
import com.flowpowered.nbt.StringTag;
import com.flowpowered.nbt.Tag;
import com.flowpowered.nbt.regionfile.RegionFile;
import com.flowpowered.nbt.stream.NBTInputStream;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import de.piegames.blockmap.MinecraftDimension;
import de.piegames.blockmap.world.WorldPins.MapPin.BannerPin;
import io.gsonfire.annotations.Exclude;
import io.gsonfire.annotations.PostDeserialize;
import io.gsonfire.annotations.PostSerialize;

/**
 * Each world may be annotated with a set of pins, represented by instances of this class. Actually, the pins will be generated from this
 * information much later on in the GUI (or any third-party applications). This class is more an abstract representation of all information
 * contained in a world that may be of interest for this purpose.
 */
public class WorldPins {

	private static Log					log		= LogFactory.getLog(WorldPins.class);

	public static final Gson			GSON	= new GsonBuilder().create();

	/** This will be used in the future to keep track of old serialized files. */
	int									version	= 0;
	Optional<List<PlayerPin>>			players;
	Optional<List<MapPin>>				maps;
	Optional<List<VillageObjectPin>>	villageObjects;
	Optional<List<ChunkPin>>			slimeChunks, loadedChunks;
	Optional<BorderPin>					barrier;
	Optional<WorldSpawnPin>				worldSpawn;

	@SuppressWarnings("unused")
	private WorldPins() {
		// Used by GSON
	}

	public WorldPins(List<PlayerPin> players, List<MapPin> maps, List<VillageObjectPin> villageObjects, List<ChunkPin> slimeChunks,
			List<ChunkPin> loadedChunks, BorderPin barrier, WorldSpawnPin worldSpawn) {
		this(Optional.ofNullable(players), Optional.ofNullable(maps), Optional.ofNullable(villageObjects), Optional.ofNullable(slimeChunks), Optional
				.ofNullable(
						loadedChunks), Optional.ofNullable(barrier), Optional.ofNullable(worldSpawn));
	}

	public WorldPins(Optional<List<PlayerPin>> players, Optional<List<MapPin>> maps, Optional<List<VillageObjectPin>> villageObjects,
			Optional<List<ChunkPin>> slimeChunks,
			Optional<List<ChunkPin>> loadedChunks, Optional<BorderPin> barrier, Optional<WorldSpawnPin> worldSpawn) {
		this.players = players;
		this.maps = maps;
		this.villageObjects = villageObjects;
		this.slimeChunks = slimeChunks;
		this.loadedChunks = loadedChunks;
		this.barrier = barrier;
		this.worldSpawn = worldSpawn;
	}

	public Optional<List<PlayerPin>> getPlayers() {
		return players;
	}

	public Optional<List<MapPin>> getMaps() {
		return maps;
	}

	public Optional<List<VillageObjectPin>> getVillages() {
		return villageObjects;
	}

	public Optional<List<ChunkPin>> getSlimeChunks() {
		return slimeChunks;
	}

	public Optional<List<ChunkPin>> getLoadedChunks() {
		return loadedChunks;
	}

	public Optional<BorderPin> getBarrier() {
		return barrier;
	}

	public Optional<WorldSpawnPin> getWorldSpawn() {
		return worldSpawn;
	}

	public Optional<List<VillageObjectPin>> getVillageObjects() {
		return villageObjects;
	}

	public static class PlayerPin {
		Vector3dc			position;
		MinecraftDimension	dimension;

		Optional<String>	UUID;
		Optional<Vector3ic>	spawnpoint;
		Optional<Integer>	gamemode;

		@SuppressWarnings("unused")
		private PlayerPin() {
			// Used by GSON
		}

		public PlayerPin(Vector3dc position, MinecraftDimension dimension, String UUID, Vector3ic spawnpoint, int gamemode) {
			this(position, dimension, Optional.ofNullable(UUID), Optional.ofNullable(spawnpoint), Optional.ofNullable(gamemode));
		}

		public PlayerPin(Vector3dc position, MinecraftDimension dimension, Optional<String> UUID, Optional<Vector3ic> spawnpoint, Optional<Integer> gamemode) {
			this.position = position;
			this.dimension = dimension;
			this.UUID = UUID;
			this.spawnpoint = spawnpoint;
			this.gamemode = gamemode;
		}

		public Vector3dc getPosition() {
			return position;
		}

		public MinecraftDimension getDimension() {
			return dimension;
		}

		public Optional<String> getUUID() {
			return UUID;
		}

		public Optional<Vector3ic> getSpawnpoint() {
			return spawnpoint;
		}

		public Optional<Integer> getGamemode() {
			return gamemode;
		}
	}

	public static class MapPin {
		Vector2ic					position;
		MinecraftDimension			dimension;
		byte						scale;

		Optional<List<BannerPin>>	banners;
		@Exclude
		Optional<byte[]>			colors;

		@SuppressWarnings("unused")
		private MapPin() {
			// Used by GSON
		}

		public MapPin(byte scale, Vector2ic position, MinecraftDimension dimension, List<BannerPin> banners, byte[] colors) {
			this(scale, position, dimension, Optional.ofNullable(banners), Optional.ofNullable(colors));
		}

		public MapPin(byte scale, Vector2ic position, MinecraftDimension dimension, Optional<List<BannerPin>> banners, Optional<byte[]> colors) {
			this.scale = scale;
			this.position = position;
			this.dimension = dimension;
			this.banners = banners;
			this.colors = colors;
		}

		@PostSerialize
		private void postSerialize(JsonElement src, Gson gson) {
			colors.ifPresent(c -> src.getAsJsonObject().addProperty("colors", Base64.getEncoder().encodeToString(c)));
		}

		@PostDeserialize
		private void postDeserialize(JsonElement src, Gson gson) {
			colors = Optional.ofNullable(src.getAsJsonObject().getAsJsonPrimitive("colors"))
					.map(JsonPrimitive::getAsString)
					.map(Base64.getDecoder()::decode);
		}

		public byte getScale() {
			return scale;
		}

		public Vector2ic getPosition() {
			return position;
		}

		public MinecraftDimension getDimension() {
			return dimension;
		}

		public Optional<List<BannerPin>> getBanners() {
			return banners;
		}

		public Optional<byte[]> getColors() {
			return colors;
		}

		public static class BannerPin {
			Vector3ic			position;
			Optional<String>	color;
			Optional<String>	name;

			@SuppressWarnings("unused")
			private BannerPin() {
				// Used by GSON
			}

			public BannerPin(Vector3ic position, String color, String name) {
				this(position, Optional.ofNullable(color), Optional.ofNullable(name));
			}

			public BannerPin(Vector3ic position, Optional<String> color, Optional<String> name) {
				this.position = position;
				this.color = color;
				this.name = name;
			}

			public Vector3ic getPosition() {
				return position;
			}

			public Optional<String> getColor() {
				return color;
			}

			public Optional<String> getName() {
				return name;
			}
		}
	}

	public static class VillageObjectPin {
		Vector3ic	position;
		int			freeTickets;
		String		type;

		public VillageObjectPin(Vector3ic position, int freeTickets, String type) {
			this.position = position;
			this.freeTickets = freeTickets;
			this.type = type;
		}

		public Vector3ic getPosition() {
			return position;
		}

		public int getFreeTickets() {
			return freeTickets;
		}

		public String getType() {
			return type;
		}
	}

	public static class ChunkPin {
		Vector2ic position;

		@SuppressWarnings("unused")
		private ChunkPin() {
			// Used by GSON
		}

		public ChunkPin(Vector2ic position) {
			this.position = position;
		}

		public Vector2ic getPosition() {
			return position;
		}
	}

	public static class BorderPin {
		Vector2dc	center;
		double		size;

		@SuppressWarnings("unused")
		private BorderPin() {
			// Used by GSON
		}

		public BorderPin(Vector2dc center, double size) {
			this.center = center;
			this.size = size;
		}

		public Vector2dc getCenter() {
			return center;
		}

		public double getRadius() {
			return size;
		}
	}

	public static class WorldSpawnPin {
		Vector3ic spawnpoint;

		@SuppressWarnings("unused")
		private WorldSpawnPin() {
			// Used by GSON
		}

		public WorldSpawnPin(Vector3ic spawnpoint) {
			this.spawnpoint = spawnpoint;
		}

		public Vector3ic getSpawnpoint() {
			return spawnpoint;
		}
	}

	@SuppressWarnings("unchecked")
	public static WorldPins loadFromWorld(Path worldPath, MinecraftDimension filterDimension) {
		List<PlayerPin> players = new ArrayList<>();
		// Players
		try (DirectoryStream<Path> d = Files.newDirectoryStream(worldPath.resolve("playerdata"))) {
			for (Path p : d) {
				if (!p.getFileName().toString().endsWith(".dat"))
					continue;
				try (NBTInputStream in = new NBTInputStream(Files.newInputStream(p), NBTInputStream.GZIP_COMPRESSION)) {
					CompoundMap map = (CompoundMap) in.readTag().getValue();
					List<DoubleTag> pos = ((ListTag<DoubleTag>) map.get("Pos")).getValue();
					Vector3d position = new Vector3d(pos.get(0).getValue(), pos.get(1).getValue(), pos.get(2).getValue());
					int dimension = ((IntTag) map.get("Dimension")).getValue();
					if (filterDimension != null && dimension != filterDimension.index)
						continue;
					String UUID = BigInteger.valueOf(((LongTag) map.get("UUIDMost")).getValue())
							.and(new BigInteger("FFFFFFFFFFFFFFFF", 16))
							.shiftLeft(64)
							.or(BigInteger.valueOf(((LongTag) map.get("UUIDLeast")).getValue()).and(new BigInteger("FFFFFFFFFFFFFFFF", 16)))
							.toString(16);
					Vector3i spawnpoint = null;
					if (map.containsKey("SpawnX"))
						spawnpoint = new Vector3i(
								((IntTag) map.get("SpawnX")).getValue(),
								((IntTag) map.get("SpawnY")).getValue(),
								((IntTag) map.get("SpawnZ")).getValue());
					int gamemode = ((IntTag) map.get("playerGameType")).getValue();
					players.add(new PlayerPin(position, MinecraftDimension.byID(dimension), UUID, spawnpoint, gamemode));
				}
			}
		} catch (IOException e) {
			log.warn("Could not access player data", e);
		}

		// Village 2.0
		ArrayList<VillageObjectPin> villageObjects = new ArrayList<>();
		try (DirectoryStream<Path> d = Files.newDirectoryStream(worldPath.resolve("poi"))) {
			for (Path p : d) {
				if (!p.toString().endsWith(".mca"))
					continue;

				try (RegionFile file = new RegionFile(p);) {
					for (int i : file.listChunks()) {
						for (CompoundTag section : (Iterable<CompoundTag>) file.loadChunk(i)
								.readTag().getAsCompoundTag("Sections")
								.map(tag -> tag.getValue().values())
								.stream().flatMap(Collection::stream)
								.map(t -> (CompoundTag) t)::iterator) {

							if (section.getByteValue("Valid").orElse((byte) 0) != 1) {
								log.warn("Found invalid records during village loading in region file " + file.getPath().toString() + " in chunk " + i);
								continue;
							}

							List<CompoundTag> records = section.getAsListTag("Records")
									.flatMap(ListTag::getAsCompoundTagList)
									.map(Tag::getValue)
									.orElse(Collections.emptyList());

							for (CompoundTag j : records) {
								int freeTickets = j.getIntValue("free_tickets").orElse(-1);
								String type = j.getStringValue("type").orElse("<unknown>");
								int[] pos = j.getIntArrayValue("pos").orElse(new int[] { 0, 0, 0, });

								Vector3ic position = new Vector3i(pos[0], pos[1], pos[2]);

								villageObjects.add(new VillageObjectPin(position, freeTickets, type));
							}
						}
					}
				}
			}
		} catch (IOException | RuntimeException e) {
			log.warn("Could not access village data", e);
		}

		List<MapPin> maps = new ArrayList<>();
		// Maps
		try (DirectoryStream<Path> d = Files.newDirectoryStream(worldPath.resolve("data"))) {
			for (Path p : d) {
				if (!p.getFileName().toString().endsWith(".dat")
						|| !p.getFileName().toString().startsWith("map_"))
					continue;
				try (NBTInputStream in = new NBTInputStream(Files.newInputStream(p), NBTInputStream.GZIP_COMPRESSION)) {
					CompoundMap map = (CompoundMap) ((CompoundMap) in.readTag().getValue()).get("data").getValue();
					byte scale = ((ByteTag) map.get("scale")).getValue();
					Vector2i center = new Vector2i(
							((IntTag) map.get("xCenter")).getValue(),
							((IntTag) map.get("zCenter")).getValue());
					List<BannerPin> banners = null;
					if (map.containsKey("banners")) {
						banners = new ArrayList<>();
						for (CompoundTag banner : ((ListTag<CompoundTag>) map.get("banners")).getValue()) {
							CompoundMap bannerMap = banner.getValue();
							String color = bannerMap.containsKey("Color") ? ((StringTag) bannerMap.get("Color")).getValue() : null;
							String name = bannerMap.containsKey("Name") ? ((StringTag) bannerMap.get("Name")).getValue() : null;
							CompoundMap pos = ((CompoundTag) bannerMap.get("Pos")).getValue();
							banners.add(new BannerPin(new Vector3i(
									((IntTag) pos.get("X")).getValue(),
									((IntTag) pos.get("Y")).getValue(),
									((IntTag) pos.get("Z")).getValue()), color, name));
						}
					}
					byte[] colors = null;
					if (map.containsKey("colors"))
						colors = ((ByteArrayTag) map.get("colors")).getValue();

					int dimension = ((Number) map.get("dimension").getValue()).intValue();
					if (filterDimension != null && dimension != filterDimension.index)
						continue;
					maps.add(new MapPin(scale, center, MinecraftDimension.byID(dimension), banners, colors));
				} catch (RuntimeException | IOException e) {
					log.warn("Could not access map " + p.getFileName(), e);
				}
			}
		} catch (IOException e) {
			log.warn("Could not access map data", e);
		}

		{ // Loaded chunks

		}
		WorldSpawnPin worldSpawn = null;
		BorderPin barrier = null;
		// World spawn and World border
		try (NBTInputStream in = new NBTInputStream(Files.newInputStream(worldPath.resolve("level.dat")), NBTInputStream.GZIP_COMPRESSION)) {
			CompoundMap level = ((CompoundTag) ((CompoundTag) in.readTag()).getValue().get("Data")).getValue();
			worldSpawn = new WorldSpawnPin(new Vector3i(
					((IntTag) level.get("SpawnX")).getValue(),
					((IntTag) level.get("SpawnY")).getValue(),
					((IntTag) level.get("SpawnZ")).getValue()));
			barrier = new BorderPin(
					new Vector2d(((DoubleTag) level.get("BorderCenterX")).getValue(), ((DoubleTag) level.get("BorderCenterZ")).getValue()),
					((DoubleTag) level.get("BorderSize")).getValue());
		} catch (IOException e) {
			log.warn("Could not access level data", e);
		}
		return new WorldPins(players, maps, villageObjects, null, null, barrier, worldSpawn);
	}
}
