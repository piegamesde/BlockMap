package de.piegames.blockmap.renderer;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joml.Vector2ic;
import org.joml.Vector3i;
import org.joml.Vector3ic;

import de.piegames.blockmap.MinecraftVersion;
import de.piegames.blockmap.color.BiomeColorMap.BiomeColor;
import de.piegames.blockmap.color.BlockColorMap;
import de.piegames.blockmap.color.BlockColorMap.BlockColor;
import de.piegames.blockmap.color.Color;
import de.piegames.blockmap.world.ChunkMetadata;
import de.piegames.blockmap.world.ChunkMetadata.ChunkMetadataFailed;
import de.piegames.blockmap.world.ChunkMetadata.ChunkMetadataRendered;
import de.piegames.nbt.CompoundMap;
import de.piegames.nbt.CompoundTag;
import de.piegames.nbt.ListTag;
import de.piegames.nbt.StringTag;
import de.piegames.nbt.Tag;
import de.piegames.nbt.regionfile.Chunk;

/**
 * Use this class to transform a Minecraft region file into a top-down image view of it.
 *
 * @author piegames
 */
class ChunkRenderer_1_18 extends ChunkRenderer {

	private static Log log = LogFactory.getLog(ChunkRenderer_1_18.class);

	public ChunkRenderer_1_18(RenderSettings settings) {
		super(MinecraftVersion.MC_1_18, settings);
	}

	@Override
	ChunkMetadata renderChunk(Vector2ic chunkPosRegion, Vector2ic chunkPosWorld, CompoundTag level, Color[] map, int[] height, String[] regionBiomes) {
		blockColors = settings.blockColors.get(version);

		try {
			/* Check chunk status */
			String generationStatus = level.getAsStringTag("Status").map(Tag::getValue).orElse("empty");
			if (ChunkMetadataRendered.STATUS_EMPTY.contains(generationStatus))
				return new ChunkMetadataRendered(chunkPosWorld, generationStatus);

			/* Load saved structures. See https://minecraft.fandom.com/de/wiki/Bauwerksdaten. */
			Map<String, Vector3ic> structureCenters = new HashMap<>();

			level.getAsCompoundTag("structures")
					.flatMap(t -> t.getAsCompoundTag("starts"))
					.stream()
					.flatMap(tag -> tag.getValue().values().stream())
					.map(Tag::getAsCompoundTag)
					.flatMap(structure -> structure.stream())
					// TODO replace this with a flatMap once Java has tuples
					// Also, rename that variable once Java has name shadowing
					.forEach((CompoundTag structure2) -> {
						String id = structure2.getStringValue("id").orElse("INVALID");
						if (id.equals("INVALID"))
							return;

						Stream.of(structure2)
								/* Each structure has a list of child elements */
								.flatMap((CompoundTag structure) -> structure.getAsListTag("Children").stream())
								.flatMap(children -> children.getAsCompoundTagList().stream())
								.flatMap(children -> children.getValue().stream())
								/* Each child has a GD, showing the distance to center. We are only interested in children with GD 0. */
								.filter((CompoundTag child) -> child.getIntValue("GD").map(gd -> gd == 0).orElse(false))
								.forEach(child -> {
									int[] bb = child.getIntArrayValue("BB").get();
									Vector3i center = new Vector3i(bb[0], bb[1], bb[2]).add(bb[3], bb[4], bb[5]);
									// JOML has no Vector3i#div function, why?
									center.x /= 2;
									center.y /= 2;
									center.z /= 2;
									structureCenters.put(id, center);
								});
					});

			/*
			 * The height of the lowest section that has already been loaded. Section are loaded lazily from top to bottom and this value gets decreased
			 * each time a new one has been loaded
			 */
			int lowestLoadedSection = 20;
			/* Lazily load from top to bottom */
			BlockColor[][] loadedSections = new BlockColor[24][];
			/*
			 * Every section is divided into 4x4x4 4x4x4 subvolumes, each of them has a biome. But for now, we simply extract the top 4x4 layer of a
			 * chunk (16 items) and ignore the rest
			 */
			String[] loadedTopBiomes = null;

			/* Get the list of all sections and map them to their y coordinate using streams */
			Map<Byte, CompoundMap> sections = level.getAsListTag("sections")
					.flatMap(ListTag::getAsCompoundTagList)
					.map(ListTag::getValue)
					.stream().flatMap(Collection::stream)
					.collect(Collectors.toMap(section -> section.getByteValue("Y").get(), Tag::getValue));

			/*
			 * Save the final color of this pixel. It starts with transparent and will be modified over time through overlay operations. The last color
			 * is saved with the amount of times it was present in a row. This way, overlaying the same color over and over again can be optimized into
			 * one operation with specialized alpha calculation.
			 */
			class ColorColumn {
				Color		color			= Color.TRANSPARENT;
				BlockColor	lastColor		= BlockColor.TRANSPARENT;
				String		lastBiome		= null;
				int			lastColorTimes	= 0;
				boolean		needStop		= false;

				ColorColumn() {
				}

				void putColor(BlockColor currentColor, int times, String biome) {
					// if (currentColor.equals(lastColor))
					if (currentColor == lastColor && biome == lastBiome)
						lastColorTimes += times;
					else {
						Color color = lastColor.color;
						if (lastColor.isGrass || lastColor.isFoliage || lastColor.isWater) {
							BiomeColor biomeColor = settings.biomeColors.getBiomeColor(lastBiome);
							if (lastColor.isGrass)
								color = Color.multiplyRGB(color, biomeColor.grassColor);
							if (lastColor.isFoliage)
								color = Color.multiplyRGB(color, biomeColor.foliageColor);
							if (lastColor.isWater)
								color = Color.multiplyRGB(color, biomeColor.waterColor);
						}
						this.color = Color.alphaUnder(this.color, color, lastColorTimes);
						lastColorTimes = times;
						lastColor = currentColor;
						lastBiome = biome;
						if (currentColor.color.a > 0.9999)
							needStop = true;
					}
				}

				Color getFinal() {
					/*
					 * Due to the alpha optimizations, putColor will only update the color when that one changes. This means that color will never contain the
					 * latest results. Putting a different color (transparent here) will trigger it to apply the last remaining color. If the last color is
					 * already transparent, this will do nothing which doesn't matter since it wouldn't make any effect anyway.
					 */
					putColor(BlockColor.TRANSPARENT, 1, null);
					return color;
				}
			}

			boolean mayCull = (chunkPosWorld.x() << 4) < settings.minX
					|| (chunkPosWorld.x() << 4) + 16 > settings.maxX
					|| (chunkPosWorld.y() << 4) < settings.minZ
					|| (chunkPosWorld.y() << 4) + 16 > settings.maxZ;
			/* Traverse the chunk in YXZ order */
			for (byte z = 0; z < 16; z++)
				for (byte x = 0; x < 16; x++) {
					if (mayCull) {
						if (x + (chunkPosWorld.x() << 4) < settings.minX
								|| x + (chunkPosWorld.x() << 4) > settings.maxX
								|| z + (chunkPosWorld.y() << 4) < settings.minZ
								|| z + (chunkPosWorld.y() << 4) > settings.maxZ)
							continue;
					}

					/* xz index relative to the chunk */
					int xz = x | z << 4;
					/* Index of the biome information, but without y coordinate yet */
					int biomeXZ = (z & 12) | (x >> 2);
					int regionXZ = chunkPosRegion.x() << 4 | x | chunkPosRegion.y() << 13 | z << 9;

					/* Once the height calculation is completed (we found a non-translucent block), set this flag to stop searching. */
					boolean heightSet = false;
					/* If we discard all solid block until we hit a translucent one, we'll get a nice cave view effect */
					boolean discardTop = blockColors.isCaveView();
					ColorColumn color = new ColorColumn();
					height: for (byte s = 19; s >= -4; s--) {
						if ((s << 4) > settings.maxY)
							continue;
						if (s < lowestLoadedSection) {
							try {
								var section = sections.get(s);
								if (section != null) {
									loadedSections[s + 4] = renderSection(section, blockColors);
								}
								/* Read the biome for the top layer */
								if (loadedTopBiomes == null && section != null) {
									loadedTopBiomes = new String[16];
									CompoundTag biomes = section.get("biomes").getAsCompoundTag().get();

									/* Parse palette */
									List<String> palette = biomes.getAsListTag("palette")
											.flatMap(ListTag::getAsStringTagList)
											.get()
											.getValue()
											.stream()
											.map(StringTag::getValue)
											.collect(Collectors.toList());

									/* Omitting the data means that everything is the same block */
									if (!biomes.getValue().containsKey("data")) {
										if (palette.size() > 1)
											log.warn("Palette has more than one element, but no index data?!");
										var biome = palette.get(0);
										for (int i = 0; i < 16; i++)
											loadedTopBiomes[i] = biome;
									} else {
										var dataArray = biomes.getLongArrayValue("data").get();
										int paletteSize = palette.size();

										/* This is just a rather weird integer log2 */
										int bitsPerIndex = Integer.SIZE - Integer.numberOfLeadingZeros(paletteSize - 1);
										int shortsPerLong = Math.floorDiv(64, bitsPerIndex);
										int mask = (1 << bitsPerIndex) - 1;

										/*
										 * Special case low palette sizes because they are common and we only need the first 16 entries, which makes things
										 * easier The cutoff is a paletteSize of 16, because after that the first 16 entries won't fit into the first long
										 */
										if (paletteSize <= 16) {
											var data = dataArray[0];
											for (int i = 0; i < 16; i++) {
												loadedTopBiomes[i] = palette.get((int) (data & mask));
												data >>= bitsPerIndex;
											}
										} else {
											/* Generic implementation */

											/* 4×4×4 resolution within a chunk section (16×16×16). The usual ordering (XZY IIRC) */
											int index = 0;
											for (long l : dataArray) {
												/* We only care about the top layer, i.e. 16 instead of 64 entries */
												for (int i = 0; i < shortsPerLong && index < 16; i++) {
													loadedTopBiomes[index++] = palette.get((int) (l & mask));
													l >>= bitsPerIndex;
												}
											}
										}
									}

								}
							} catch (Exception e) {
								log.warn("Failed to render chunk (" + chunkPosRegion.x() + ", " + chunkPosRegion.y() + ") section " + s
										+ ". This is very likely because your chunk is corrupt. If possible, please verify it "
										+ "manually before sending a bug report.", e);
								return new ChunkMetadataFailed(chunkPosWorld, e);
							}
							lowestLoadedSection = s;
						}
						var section = loadedSections[s + 4];
						if (section == null) {
							discardTop = false;
							continue;
						}
						for (int y = 15; y >= 0; y--) {
							int h = s << 4 | y;
							if (h < settings.minY)
								break height;
							if (h > settings.maxY)
								continue;

							/* xzy index relative to the current section */
							int xzy = xz | y << 8;

							BlockColor colorData = section[xzy];
							if (discardTop && colorData.isTranslucent)
								discardTop = false;
							if (!discardTop && !colorData.isTranslucent && !heightSet) {
								height[regionXZ] = h;
								heightSet = true;
							}

							// /* This is in section-local coordinates */
							// int biomeXYZ = (h >> 2) << 4 | biomeXZ;
							if (!discardTop)
								color.putColor(colorData, 1, loadedTopBiomes[biomeXZ]);
							if (color.needStop)
								break height;
						}
					}
					regionBiomes[regionXZ] = loadedTopBiomes[biomeXZ];
					map[regionXZ] = color.getFinal();
				}
			return new ChunkMetadataRendered(chunkPosWorld, generationStatus, structureCenters);
		} catch (Exception e) {
			log.warn("Failed to render chunk (" + chunkPosRegion.x() + ", " + chunkPosRegion.y() + ")", e);
			return new ChunkMetadataFailed(chunkPosWorld, e);
		}
	}

	/**
	 * Takes in the NBT data for a section and returns an int[] containing the color of each block in that section. The returned array thus has
	 * a length of 16³=4096 items and the blocks are mapped to them in XZY order.
	 */
	private BlockColor[] renderSection(CompoundMap section, BlockColorMap blockColors) {
		CompoundTag blockStates = section.get("block_states").getAsCompoundTag().get();

		/* Parse palette */
		List<BlockColor> palette = blockStates.getAsListTag("palette")
				.flatMap(ListTag::getAsCompoundTagList)
				.map(Tag::getValue)
				.stream().flatMap(Collection::stream)
				.map(map -> blockColors.getBlockColor(
						map.getStringValue("Name").get(),
						() -> parseBlockState(map.getAsCompoundTag("Properties").get(), version.getBlockStates())))
				.collect(Collectors.toList());

		BlockColor[] ret = new BlockColor[4096];

		/* Omitting the data means that everything is the same block */
		if (!blockStates.getValue().containsKey("data")) {
			if (palette.size() > 1)
				log.warn("Palette has more than one element, but no index data?!");
			var state = palette.get(0);
			for (int i = 0; i < 4096; i++)
				ret[i] = state;
			return ret;
		}

		long[] blocks = blockStates.getLongArrayValue("data").get();

		long[] blocksParsed = Chunk.extractFromLong1_16(blocks, palette.size());
		for (int i = 0; i < 4096; i++) {
			int b = (int) blocksParsed[i];
			if (b >= palette.size()) {
				log.warn("Block " + i + " " + b + " was out of bounds, is this world corrupt?");
				continue;
			}
			ret[i] = palette.get(b);
		}

		return ret;
	}
}
