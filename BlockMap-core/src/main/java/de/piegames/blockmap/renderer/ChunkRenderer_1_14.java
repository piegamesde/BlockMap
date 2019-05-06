package de.piegames.blockmap.renderer;

import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.flowpowered.nbt.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joml.Vector2ic;
import org.joml.Vector3i;
import org.joml.Vector3ic;

import com.flowpowered.nbt.regionfile.Chunk;

import de.piegames.blockmap.MinecraftVersion;
import de.piegames.blockmap.color.BiomeColorMap.BiomeColor;
import de.piegames.blockmap.color.BlockColorMap;
import de.piegames.blockmap.color.BlockColorMap.BlockColor;
import de.piegames.blockmap.color.Color;
import de.piegames.blockmap.world.ChunkMetadata;
import de.piegames.blockmap.world.ChunkMetadata.ChunkMetadataFailed;
import de.piegames.blockmap.world.ChunkMetadata.ChunkMetadataRendered;

/**
 * Use this class to transform a Minecraft region file into a top-down image view of it.
 * 
 * @author piegames
 */
class ChunkRenderer_1_14 extends ChunkRenderer {

	private static Log log = LogFactory.getLog(ChunkRenderer_1_14.class);

	public ChunkRenderer_1_14(RenderSettings settings) {
		super(MinecraftVersion.MC_1_14, settings);
	}

	@Override
	ChunkMetadata renderChunk(Vector2ic chunkPosRegion, Vector2ic chunkPosWorld, CompoundMap level, Color[] map, int[] height, int[] regionBiomes) {
		blockColors = settings.blockColors.get(version);

		try {
			/* Check chunk status */
			String generationStatus = ((String) level.get("Status").getValue());
			if (generationStatus == null) {
				log.warn("Could not parse generation status: " + level.get("Status").getValue());
				generationStatus = "empty";
			}
			if (ChunkMetadataRendered.STATUS_EMPTY.contains(generationStatus))
				return new ChunkMetadataRendered(chunkPosWorld, generationStatus);

			Map<String, Vector3ic> structureCenters = new HashMap<>();
			if (level.containsKey("Structures") && ((CompoundTag) level.get("Structures")).getValue().containsKey("Starts")) {// Load saved structures
				CompoundMap structures = ((CompoundTag) ((CompoundTag) level.get("Structures")).getValue().get("Starts")).getValue();
				for (Tag<?> structureTag : structures.values()) {
					CompoundMap structure = ((CompoundTag) structureTag).getValue();
					String id = ((StringTag) structure.get("id")).getValue();
					if (!id.equals("INVALID")) {
						int[] bb = ((IntArrayTag) structure.get("BB")).getValue();
						Vector3i center = new Vector3i(bb[0], bb[1], bb[2]).add(bb[3], bb[4], bb[5]);
						// JOML has no Vector3i#div function, why?
						center.x /= 2;
						center.y /= 2;
						center.z /= 2;
						structureCenters.put(id, center);
					}
				}
			}

			int[] biomes = ((IntArrayTag) level.get("Biomes")).getValue();

			/*
			 * The height of the lowest section that has already been loaded. Section are loaded lazily from top to bottom and this value gets decreased
			 * each time a new one has been loaded
			 */
			int lowestLoadedSection = 16;
			/* Null entries indicate a section full of air */
			BlockColor[][] loadedSections = new BlockColor[16][];

			// Get the list of all sections and map them to their y coordinate using streams
			@SuppressWarnings("unchecked")
			Map<Byte, CompoundMap> sections = ((ListTag<CompoundTag>) level.getOrDefault("Sections",
					new ListTag<>("sections", TagType.TAG_COMPOUND, Collections.emptyList()))).getValue().stream()
							.collect(Collectors.toMap(s -> (Byte) s.getValue().get("Y").getValue(), s -> s.getValue()));

			/*
			 * Save the final color of this pixel. It starts with transparent and will be modified over time through overlay operations. The last color
			 * is saved with the amount of times it was present in a row. This way, overlaying the same color over and over again can be optimized into
			 * one operation with specialized alpha calculation.
			 */
			class ColorColumn {
				Color		color			= Color.TRANSPARENT;
				BlockColor	lastColor		= BlockColor.TRANSPARENT;
				int			biome;
				int			lastColorTimes	= 0;
				boolean		needStop		= false;

				ColorColumn(int biome) {
					this.biome = biome;
				}

				void putColor(BlockColor currentColor, int times) {
					// if (currentColor.equals(lastColor))
					if (currentColor == lastColor)
						lastColorTimes += times;
					else {
						Color color = lastColor.color;
						BiomeColor biomeColor = settings.biomeColors.getBiomeColor(biome);
						if (lastColor.isGrass)
							color = Color.multiplyRGB(color, biomeColor.grassColor);
						if (lastColor.isFoliage)
							color = Color.multiplyRGB(color, biomeColor.foliageColor);
						if (lastColor.isWater)
							color = Color.multiplyRGB(color, biomeColor.waterColor);
						this.color = Color.alphaUnder(this.color, color, lastColorTimes);
						lastColorTimes = times;
						lastColor = currentColor;
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
					putColor(BlockColor.TRANSPARENT, 1);
					return color;
				}
			}

			// Traverse the chunk in YXZ order
			for (byte z = 0; z < 16; z++)
				for (byte x = 0; x < 16; x++) {
					if (x < settings.minX || x > settings.maxX || z < settings.minZ || z > settings.maxZ)
						continue;

					/* xz index relative to the chunk */
					int xz = x | z << 4;
					regionBiomes[chunkPosRegion.x() << 4 | x | chunkPosRegion.y() << 13 | z << 9] = biomes[xz];

					/* Once the height calculation is completed (we found a non-translucent block), set this flag to stop searching. */
					boolean heightSet = false;
					ColorColumn color = new ColorColumn(biomes[xz & 0xFF]);
					height: for (byte s = 15; s >= 0; s--) {
						if ((s << 4) > settings.maxY)
							continue;
						if (s < lowestLoadedSection) {
							// log.debug("Loading section " + s);
							try {
								loadedSections[s] = renderSection(sections.get(s), blockColors);
							} catch (Exception e) {
								log.warn("Failed to render chunk (" + chunkPosRegion.x() + ", " + chunkPosRegion.y() + ") section " + s
										+ ". This is very likely because your chunk is corrupt. If possible, please verify it "
										+ "manually before sending a bug report.", e);
								return new ChunkMetadataFailed(chunkPosWorld, e);
							}
							lowestLoadedSection = s;
						}
						if (loadedSections[s] == null) {
							// Sector is full of air
							color.putColor(blockColors.getAirColor(), 16);
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

							BlockColor colorData = loadedSections[s][xzy];
							if (!colorData.isTranslucent && !heightSet) {
								height[chunkPosRegion.x() << 4 | x | chunkPosRegion.y() << 13 | z << 9] = h;
								heightSet = true;
							}

							color.putColor(colorData, 1);
							if (color.needStop)
								break height;
						}
					}
					map[chunkPosRegion.x() << 4 | x | chunkPosRegion.y() << 13 | z << 9] = color.getFinal();
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
		/* Sometimes sections only contain "Y" and "SkyLight", but no "Palette"?! */
		if (section == null || !section.containsKey("Palette"))
			return null;

		// Parse palette
		@SuppressWarnings("unchecked")
		List<BlockColor> palette = ((ListTag<CompoundTag>) section.get("Palette"))
				.getValue()
				.stream()
				.map(tag -> tag.getValue())
				.map(map -> blockColors.getBlockColor(
						((StringTag) map.get("Name")).getValue(),
						new Supplier<BitSet>() {
							BitSet memoize;

							@Override
							public BitSet get() {
								return memoize == null
										? memoize = parseBlockState((CompoundTag) map.get("Properties"), version.getBlockStates())
										: memoize;
							}
						}))
				.collect(Collectors.toList());

		long[] blocks = ((LongArrayTag) section.get("BlockStates")).getValue();

		int bitsPerIndex = blocks.length * 64 / 4096;
		BlockColor[] ret = new BlockColor[16 * 16 * 16];

		for (int i = 0; i < 4096; i++) {
			long blockIndex = Chunk.extractFromLong(blocks, i, bitsPerIndex);

			if (blockIndex >= palette.size()) {
				log.warn("Block " + i + " " + blockIndex + " was out of bounds, is this world corrupt?");
				continue;
			}
			BlockColor block = palette.get((int) blockIndex);

			ret[i] = block;
		}
		return ret;
	}

}
