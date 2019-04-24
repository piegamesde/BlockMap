package de.piegames.blockmap.renderer;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joml.Vector2i;
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
import com.flowpowered.nbt.regionfile.Chunk;
import com.flowpowered.nbt.regionfile.RegionFile;

import de.piegames.blockmap.color.Color;
import de.piegames.blockmap.world.ChunkMetadata;
import de.piegames.blockmap.world.ChunkMetadata.ChunkGenerationStatus;
import de.piegames.blockmap.world.ChunkMetadata.ChunkRenderState;
import de.piegames.blockmap.world.Region.BufferedRegion;

/**
 * Use this class to transform a Minecraft region file into a top-down image view of it.
 * 
 * @author piegames
 */
public class RegionRenderer {

	private static Log			log						= LogFactory.getLog(RegionRenderer.class);

	public final RenderSettings	settings;

	/* Only keep track of this so that the respecting warning is only logged once. */
	private Set<Block>			blocksWithMissingColor	= new HashSet<>();

	public RegionRenderer(RenderSettings settings) {
		this.settings = Objects.requireNonNull(settings);
	}

	/**
	 * Render a given {@link RegionFile} to a {@link BufferedImage}. The image will always have a width and height of 512 pixels.
	 * 
	 * @param file
	 *            The file to render. Should not be {@code null}
	 * @param regionPos
	 *            The position of the region file in region coordinates. Used to check if blocks are within the bounds of the area to render.
	 * @return An array of colors representing the final image. The image is square and 512x512 wide. The array sorted in XZ order.
	 */
	public BufferedRegion render(Vector2ic regionPos, RegionFile file) {
		log.info("Rendering region file " + regionPos.x() + " " + regionPos.y());
		BufferedImage image = new BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB);
		Map<Vector2ic, ChunkMetadata> metadata = new HashMap<>();
		Color[] colors = renderRaw(regionPos, file, metadata);
		// image.setRGB(0, 0, 512, 512, colors, 0, 512);
		for (int x = 0; x < 512; x++)
			for (int z = 0; z < 512; z++)
				if (colors[x | (z << 9)] != null)
					image.setRGB(x, z, colors[x | (z << 9)].toRGB());
		return new BufferedRegion(regionPos, image, metadata);
	}

	/**
	 * Render a given {@link RegionFile} to an image, represented as color array.
	 * 
	 * @param file
	 *            The file to render. Should not be {@code null}
	 * @param regionPos
	 *            The position of the region file in region coordinates. Used to check if blocks are within the bounds of the area to render.
	 * @return An array of colors representing the final image. The image is square and 512x512 wide. The array sorted in XZ order.
	 * @see #render(Vector2ic, RegionFile)
	 * @see Color
	 * @see RegionFile
	 */
	public Color[] renderRaw(Vector2ic regionPos, RegionFile file, Map<Vector2ic, ChunkMetadata> metadata) {
		/* The final map of the chunk, 512*512 pixels, XZ */
		Color[] map = new Color[512 * 512];
		/* If nothing is set otherwise, the height map is set to the minimum height. */
		int[] height = new int[512 * 512];
		int[] regionBiomes = new int[512 * 512];
		Arrays.fill(height, settings.minY);
		Arrays.fill(regionBiomes, -1);

		chunk: for (Chunk chunk : file) {
			if (chunk == null)
				continue;
			int chunkX = ((regionPos.x() << 5) | chunk.x);
			int chunkZ = ((regionPos.y() << 5) | chunk.z);
			Vector2ic chunkPos = new Vector2i(chunkX, chunkZ);
			try {
				if ((chunkX + 16 < settings.minX || chunkX > settings.maxX)
						&& (chunkZ + 16 < settings.minZ || chunkZ > settings.maxZ)) {
					metadata.put(chunkPos, new ChunkMetadata(chunkPos, ChunkRenderState.CULLED, null));
					continue;
				}

				CompoundMap root = chunk.readTag().getValue();

				{ // Check data version
					if (root.containsKey("DataVersion")) {
						// 1519 is the internal version number of 1.13
						int dataVersion = ((Integer) root.get("DataVersion").getValue());
						if (dataVersion < 1519) {
							log.warn("Skipping chunk because it is too old");
							metadata.put(chunkPos, new ChunkMetadata(chunkPos, ChunkRenderState.TOO_OLD, null));
							continue;
						}
					} else {
						log.warn("Skipping chunk because it is way too old (pre 1.9)");
						metadata.put(chunkPos, new ChunkMetadata(chunkPos, ChunkRenderState.TOO_OLD, null));
						continue;
					}
				}

				CompoundMap level = ((CompoundTag) root.get("Level")).getValue();

				/* Check chunk status */
				ChunkGenerationStatus generationStatus = ChunkGenerationStatus.forName(((String) level.get("Status").getValue()));
				if (generationStatus == ChunkGenerationStatus.EMPTY) {
					metadata.put(chunkPos, new ChunkMetadata(chunkPos, ChunkRenderState.RENDERED, generationStatus));
					continue;
				}

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
				Block[][] loadedSections = new Block[16][];

				// Get the list of all sections and map them to their y coordinate using streams
				@SuppressWarnings("unchecked")
				Map<Byte, CompoundMap> sections = ((ListTag<CompoundTag>) level.getOrDefault("Sections",
						new ListTag<>("sections", CompoundTag.class, Collections.emptyList()))).getValue().stream()
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
						 * Due to the alpha optimizations, putColor will only update the color when that one changes. This means that color will never contain
						 * the latest results. Putting a different color (transparent here) will trigger it to apply the last remaining color. If the last color
						 * is already transparent, this will do nothing which doesn't matter since it wouldn't make any effect anyway.
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

						regionBiomes[chunk.x << 4 | x | chunk.z << 13 | z << 9] = biomes[x | z << 4];

						/* Once the height calculation is completed (we found a non-translucent block), set this flag to stop searching. */
						boolean heightSet = false;
						ColorColumn color = new ColorColumn();
						height: for (byte s = 15; s >= 0; s--) {
							if ((s << 4) > settings.maxY)
								continue;
							if (s < lowestLoadedSection) {
								// log.debug("Loading section " + s);
								try {
									loadedSections[s] = renderSection(sections.get(s));
								} catch (Exception e) {
									log.warn("Failed to render chunk (" + chunk.x + ", " + chunk.z + ") section " + s
											+ ". This is very likely because your chunk is corrupt. If possible, please verify it "
											+ "manually before sending a bug report.", e);
									metadata.put(chunkPos, new ChunkMetadata(chunkPos, ChunkRenderState.FAILED, generationStatus));
									continue chunk;
								}
								lowestLoadedSection = s;
							}
							if (loadedSections[s] == null) {
								// Sector is full of air
								color.putColor(settings.blockColors.getBlockColor(Block.AIR), 16);
								continue;
							}
							for (int y = 15; y >= 0; y--) {
								if ((y | s << 4) < settings.minY)
									break height;
								if ((y | s << 4) > settings.maxY)
									continue;

								int i = x | z << 4 | y << 8;
								Block block = loadedSections[s][i];

								Color currentColor = settings.blockColors.getBlockColor(block);
								if (currentColor == Color.MISSING && blocksWithMissingColor.add(block)) // == is correct here
									log.warn("Missing color for " + block);
								if (settings.blockColors.isGrassBlock(block))
									currentColor = Color.multiplyRGB(currentColor, settings.biomeColors.getGrassColor(biomes[i & 0xFF]));
								if (settings.blockColors.isFoliageBlock(block))
									currentColor = Color.multiplyRGB(currentColor, settings.biomeColors.getFoliageColor(biomes[i & 0xFF]));
								if (settings.blockColors.isWaterBlock(block))
									currentColor = Color.multiplyRGB(currentColor, settings.biomeColors.getWaterColor(biomes[i & 0xFF]));
								if (!settings.blockColors.isTranslucentBlock(block) && !heightSet) {
									height[chunk.x << 4 | x | chunk.z << 13 | z << 9] = s << 4 | y;
									heightSet = true;
								}

								color.putColor(currentColor);
								if (color.needStop)
									break height;
							}
						}
						map[chunk.x << 4 | x | chunk.z << 13 | z << 9] = color.getFinal();
					}
				metadata.put(chunkPos, new ChunkMetadata(chunkPos, ChunkRenderState.RENDERED, generationStatus, structureCenters));
			} catch (Exception e) {
				log.warn("Failed to render chunk (" + chunk.x + ", " + chunk.z + ")", e);
				metadata.put(chunkPos, new ChunkMetadata(chunkPos, ChunkRenderState.FAILED, null));
				continue;
			}
		}

		settings.shader.shade(map, height, regionBiomes, settings.biomeColors);
		return map;
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
				.map(tag -> tag.getValue())
				.map(map -> new Block(((StringTag) map.get("Name")).getValue(), parseBlockState((CompoundTag) map.get("Properties"))))
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

	public static EnumSet<BlockState> parseBlockState(CompoundTag properties) {
		EnumSet<BlockState> ret = EnumSet.noneOf(BlockState.class);
		if (properties != null)
			for (Entry<String, Tag<?>> entry : properties.getValue().entrySet())
				ret.add(BlockState.valueOf(entry.getKey(), ((StringTag) entry.getValue()).getValue()));
		return ret;
	}
}
