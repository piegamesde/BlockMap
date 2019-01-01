package de.piegames.blockmap.world;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

import com.flowpowered.nbt.ByteTag;
import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.DoubleTag;
import com.flowpowered.nbt.IntTag;
import com.flowpowered.nbt.ListTag;
import com.flowpowered.nbt.LongTag;
import com.flowpowered.nbt.StringTag;
import com.flowpowered.nbt.stream.NBTInputStream;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import de.piegames.blockmap.MinecraftDimension;
import de.piegames.blockmap.world.WorldPins.MapPin.BannerPin;

/** Each world may be annotated with a set of pins, represented by instances of this class */
public class WorldPins {

	private static Log log = LogFactory.getLog(WorldPins.class);

	public static final Gson	GSON	= new GsonBuilder().create();

	Optional<List<PlayerPin>>	players;
	Optional<List<MapPin>>		maps;
	Optional<List<VillagePin>>	villages;
	Optional<List<ChunkPin>>	slimeChunks, loadedChunks;
	Optional<BorderPin>			barrier;
	Optional<WorldSpawnPin>		worldSpawn;
	Optional<Map<Vector2ic, ChunkMetadata>>	chunkMetadata;

	@SuppressWarnings("unused")
	private WorldPins() {
		// Used by GSON
	}

	public WorldPins(List<PlayerPin> players, List<MapPin> maps, List<VillagePin> villages, List<ChunkPin> slimeChunks,
			List<ChunkPin> loadedChunks, BorderPin barrier, WorldSpawnPin worldSpawn) {
		this(Optional.ofNullable(players), Optional.ofNullable(maps), Optional.ofNullable(villages), Optional.ofNullable(slimeChunks), Optional.ofNullable(
				loadedChunks), Optional.ofNullable(barrier), Optional.ofNullable(worldSpawn));
	}

	public WorldPins(Optional<List<PlayerPin>> players, Optional<List<MapPin>> maps, Optional<List<VillagePin>> villages, Optional<List<ChunkPin>> slimeChunks,
			Optional<List<ChunkPin>> loadedChunks, Optional<BorderPin> barrier, Optional<WorldSpawnPin> worldSpawn) {
		this.players = players;
		this.maps = maps;
		this.villages = villages;
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

	public Optional<List<VillagePin>> getVillages() {
		return villages;
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

		@SuppressWarnings("unused")
		private MapPin() {
			// Used by GSON
		}

		public MapPin(byte scale, Vector2ic position, MinecraftDimension dimension, List<BannerPin> banners) {
			this(scale, position, dimension, Optional.ofNullable(banners));
		}

		public MapPin(byte scale, Vector2ic position, MinecraftDimension dimension, Optional<List<BannerPin>> banners) {
			this.scale = scale;
			this.position = position;
			this.dimension = dimension;
			this.banners = banners;
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

	public static class VillagePin {
		Vector3ic					position;
		MinecraftDimension			dimension;

		Optional<Integer>			radius, population, golems;
		Optional<List<Vector3ic>>	doors;

		@SuppressWarnings("unused")
		private VillagePin() {
			// Used by GSON
		}

		public VillagePin(Vector3ic position, MinecraftDimension dimension, Integer radius, Integer population, Integer golems,
				List<Vector3ic> doors) {
			this(position, dimension, Optional.ofNullable(radius), Optional.ofNullable(population), Optional.ofNullable(golems), Optional.ofNullable(doors));
		}

		public VillagePin(Vector3ic position, MinecraftDimension dimension, Optional<Integer> radius, Optional<Integer> population, Optional<Integer> golems,
				Optional<List<Vector3ic>> doors) {
			this.position = position;
			this.dimension = dimension;
			this.radius = radius;
			this.population = population;
			this.golems = golems;
			this.doors = doors;
		}

		public Vector3ic getPosition() {
			return position;
		}

		public MinecraftDimension getDimension() {
			return dimension;
		}

		public Optional<Integer> getRadius() {
			return radius;
		}

		public Optional<Integer> getPopulation() {
			return population;
		}

		public Optional<Integer> getGolems() {
			return golems;
		}

		public Optional<List<Vector3ic>> getDoors() {
			return doors;
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

	// public static WorldPins filter(WorldPins loaded, Set<String> filter) {
	// Map<String, Field> fields = listFields(loaded.getClass(), "");
	// fields.keySet().removeIf(s -> {
	// for (String f : filter)
	// if (s.startsWith(f))
	// return true;
	// return false;
	// });
	// return null;
	// }
	//
	// private static Map<String, Field> listFields(Class<?> c, String prefix) {
	// Map<String, Field> ret = new HashMap<>();
	// for (Field f : c.getDeclaredFields())
	// if (f.isAnnotationPresent(Property.class)) {
	// Property p = f.getAnnotation(Property.class);
	// String name = prefix + f.getName();
	// ret.put(name, f);
	// ret.putAll(listFields(p.value(), name + "."));
	// }
	// return ret;
	// }

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
		List<VillagePin> villages = new ArrayList<>();
		// Villages
		for (MinecraftDimension dimension : MinecraftDimension.values()) {
			if (filterDimension != null && dimension != filterDimension)
				continue;
			try (NBTInputStream in = new NBTInputStream(Files.newInputStream(worldPath.resolve(dimension.villagePath)), NBTInputStream.GZIP_COMPRESSION)) {
				// TODO check data version
				CompoundMap villageMap = (CompoundMap) ((CompoundMap) in.readTag().getValue()).get("data").getValue();
				villageMap.entrySet().forEach(System.out::println);
				for (CompoundTag village : ((ListTag<CompoundTag>) villageMap.get("Villages")).getValue()) {
					CompoundMap map = village.getValue();
					// map.entrySet().forEach(System.out::println);
					Vector3i posVec = new Vector3i(
							((IntTag) map.get("CX")).getValue(),
							((IntTag) map.get("CY")).getValue(),
							((IntTag) map.get("CZ")).getValue());
					int population = ((IntTag) map.get("PopSize")).getValue();
					int radius = ((IntTag) map.get("Radius")).getValue();
					int golems = ((IntTag) map.get("Golems")).getValue();
					List<Vector3ic> doors = new ArrayList<>();
					for (CompoundTag door : ((ListTag<CompoundTag>) map.get("Doors")).getValue()) {
						CompoundMap doorMap = door.getValue();
						doors.add(new Vector3i(
								((IntTag) doorMap.get("X")).getValue(),
								((IntTag) doorMap.get("Y")).getValue(),
								((IntTag) doorMap.get("Z")).getValue()));
					}

					villages.add(new VillagePin(posVec, dimension, radius, population, golems, doors));
				}
			} catch (IOException e) {
				log.warn("Could not access village data", e);
			}
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
					int dimension = ((IntTag) map.get("dimension")).getValue();
					if (filterDimension != null && dimension != filterDimension.index)
						continue;
					maps.add(new MapPin(scale, center, MinecraftDimension.byID(dimension), banners));
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
		return new WorldPins(players, maps, villages, null, null, barrier, worldSpawn);
	}
}
