package de.piegames.blockmap.renderer;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joml.Vector2ic;
import org.joml.Vector3i;
import org.joml.Vector3ic;

import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.IntArrayTag;
import com.flowpowered.nbt.ListTag;
import com.flowpowered.nbt.LongArrayTag;
import com.flowpowered.nbt.StringTag;
import com.flowpowered.nbt.Tag;
import com.flowpowered.nbt.TagType;
import com.flowpowered.nbt.regionfile.Chunk;

import de.piegames.blockmap.MinecraftVersion;
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
public class ChunkRenderer_1_13 extends ChunkRenderer {

	private static Log log = LogFactory.getLog(ChunkRenderer_1_13.class);

	public ChunkRenderer_1_13(RenderSettings settings) {
		super(MinecraftVersion.MC_1_13, settings);
	}

	@Override
	ChunkMetadata renderChunk(Vector2ic chunkPosRegion, Vector2ic chunkPosWorld, CompoundTag level, Color[] map, int[] height, int[] regionBiomes) {
		blockColors = settings.blockColors.get(version);

		try {
			/* Check chunk status */
			String generationStatus = ((String) level.getValue().get("Status").getValue());
			if (generationStatus == null) {
				log.warn("Could not parse generation status: " + level.getValue().get("Status").getValue());
				generationStatus = "empty";
			}
			if (ChunkMetadataRendered.STATUS_EMPTY.contains(generationStatus))
				return new ChunkMetadataRendered(chunkPosWorld, generationStatus);

			Map<String, Vector3ic> structureCenters = new HashMap<>();
			if (level.getValue().containsKey("Structures") && ((CompoundTag) level.getValue().get("Structures")).getValue().containsKey("Starts")) {// Load
																																					// saved
																																					// structures
				CompoundMap structures = ((CompoundTag) ((CompoundTag) level.getValue().get("Structures")).getValue().get("Starts")).getValue();
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

			int[] biomes = ((IntArrayTag) level.getValue().get("Biomes")).getValue();

			/*
			 * The height of the lowest section that has already been loaded. Section are loaded lazily from top to bottom and this value gets decreased
			 * each time a new one has been loaded
			 */
			int lowestLoadedSection = 16;
			/* Null entries indicate a section full of air */
			Block[][] loadedSections = new Block[16][];

			// Get the list of all sections and map them to their y coordinate using streams
			@SuppressWarnings("unchecked")
			Map<Byte, CompoundMap> sections = ((ListTag<CompoundTag>) level.getValue().getOrDefault("Sections",
					new ListTag<>("sections", TagType.TAG_COMPOUND, Collections.emptyList()))).getValue().stream()
							.collect(Collectors.toMap(s -> (Byte) s.getValue().get("Y").getValue(), s -> s.getValue()));

			/*
			 * Save the final color of this pixel. It starts with transparent and will be modified over time through overlay operations. The last color
			 * is saved with the amount of times it was present in a row. This way, overlaying the same color over and over again can be optimized into
			 * one operation with specialized alpha calculation.
			 */
			class ColorColumn {
				Color	color			= Color.TRANSPARENT, lastColor = Color.TRANSPARENT;
				int		lastColorTimes	= 0;
				boolean	needStop		= false;

				void putColor(Color currentColor) {
					putColor(currentColor, 1);
				}

				void putColor(Color currentColor, int times) {
					if (currentColor.equals(lastColor))
						lastColorTimes += times;
					else {
						color = Color.alphaUnder(color, lastColor, lastColorTimes);
						lastColorTimes = times;
						lastColor = currentColor;
					}
					if (currentColor.a > 0.9999)
						needStop = true;
				}

				Color getFinal() {
					/*
					 * Due to the alpha optimizations, putColor will only update the color when that one changes. This means that color will never contain the
					 * latest results. Putting a different color (transparent here) will trigger it to apply the last remaining color. If the last color is
					 * already transparent, this will do nothing which doesn't matter since it wouldn't make any effect anyway.
					 */
					putColor(Color.TRANSPARENT);
					return color;
				}
			}

			// Traverse the chunk in YXZ order
			for (byte z = 0; z < 16; z++)
				for (byte x = 0; x < 16; x++) {
					if (x < settings.minX || x > settings.maxX || z < settings.minZ || z > settings.maxZ)
						continue;

					regionBiomes[chunkPosRegion.x() << 4 | x | chunkPosRegion.y() << 13 | z << 9] = biomes[x | z << 4];

					/* Once the height calculation is completed (we found a non-translucent block), set this flag to stop searching. */
					boolean heightSet = false;
					ColorColumn color = new ColorColumn();
					height: for (byte s = 15; s >= 0; s--) {
						if ((s << 4) > settings.maxY)
							continue;
						if (s < lowestLoadedSection) {
							try {
								loadedSections[s] = renderSection(sections.get(s));
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
							color.putColor(blockColors.getAirColor().color, 16);
							continue;
						}
						for (int y = 15; y >= 0; y--) {
							if ((y | s << 4) < settings.minY)
								break height;
							if ((y | s << 4) > settings.maxY)
								continue;

							int i = x | z << 4 | y << 8;
							Block block = loadedSections[s][i];

							BlockColor colorData = blockColors.getBlockColor(block.name, block.state);
							Color currentColor = colorData.color;
							if (colorData.isGrass)
								currentColor = Color.multiplyRGB(currentColor, settings.biomeColors.getBiomeColor(biomes[i & 0xFF]).grassColor);
							if (colorData.isFoliage)
								currentColor = Color.multiplyRGB(currentColor, settings.biomeColors.getBiomeColor(biomes[i & 0xFF]).foliageColor);
							if (colorData.isWater)
								currentColor = Color.multiplyRGB(currentColor, settings.biomeColors.getBiomeColor(biomes[i & 0xFF]).waterColor);
							if (!colorData.isTranslucent && !heightSet) {
								height[chunkPosRegion.x() << 4 | x | chunkPosRegion.y() << 13 | z << 9] = s << 4 | y;
								heightSet = true;
							}

							color.putColor(currentColor);
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
	private Block[] renderSection(CompoundMap section) {
		if (section == null)
			return null;

		// Parse palette
		@SuppressWarnings("unchecked")
		List<Block> palette = ((ListTag<CompoundTag>) section.get("Palette"))
				.getValue()
				.stream()
				.map(Tag::getValue)
				.map(map -> new Block(
						((StringTag) map.get("Name")).getValue(),
						parseBlockState((CompoundTag) map.get("Properties"), version.getBlockStates())))
				.collect(Collectors.toList());

		long[] blocks = ((LongArrayTag) section.get("BlockStates")).getValue();

		int bitsPerIndex = blocks.length * 64 / 4096;
		Block[] ret = new Block[16 * 16 * 16];

		for (int i = 0; i < 4096; i++) {
			long blockIndex = Chunk.extractFromLong(blocks, i, bitsPerIndex);

			if (blockIndex >= palette.size()) {
				log.warn("Block " + i + " " + blockIndex + " was out of bounds, is this world corrupt?");
				continue;
			}
			Block block = palette.get((int) blockIndex);

			ret[i] = block;
		}
		return ret;
	}
}
