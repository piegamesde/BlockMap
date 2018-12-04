package de.piegames.blockmap.renderer;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joml.Vector2ic;

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

/**
 * Use this class to transform a Minecraft region file into a top-down image view of it.
 * 
 * @author piegames
 */
public class RegionRenderer {

	private static Log			log	= LogFactory.getLog(RegionRenderer.class);

	public final RenderSettings	settings;

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
	public BufferedImage render(Vector2ic regionPos, RegionFile file) throws IOException {
		log.info("Rendering region file " + regionPos.x() + " " + regionPos.y());
		BufferedImage image = new BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB);
		Color[] colors = renderRaw(regionPos, file);
		// image.setRGB(0, 0, 512, 512, colors, 0, 512);
		for (int x = 0; x < 512; x++)
			for (int z = 0; z < 512; z++)
				if (colors[x | (z << 9)] != null)
					image.setRGB(x, z, colors[x | (z << 9)].toRGB());
		return image;
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
	public Color[] renderRaw(Vector2ic regionPos, RegionFile file) throws IOException {
		/* The final map of the chunk, 512*512 pixels, XZ */
		Color[] map = new Color[512 * 512];
		/* If nothing is set otherwise, the height map is set to the minimum height. */
		int[] height = new int[512 * 512];
		int[] regionBiomes = new int[512 * 512];
		Arrays.fill(height, settings.minY);
		Arrays.fill(regionBiomes, -1);

		for (Chunk chunk : file) {
			if (chunk == null)
				continue;
			try {
				int chunkX = ((regionPos.x() << 5) | chunk.x);
				int chunkZ = ((regionPos.y() << 5) | chunk.z);
				if ((chunkX + 16 < settings.minX || chunkX > settings.maxX)
						&& (chunkZ + 16 < settings.minZ || chunkZ > settings.maxZ)) {
					continue;
				}

				CompoundMap root = chunk.readTag().getValue();

				{ // Check data version
					if (root.containsKey("DataVersion")) {
						// 1519 is the internal version number of 1.13
						int dataVersion = ((Integer) root.get("DataVersion").getValue());
						if (dataVersion < 1519) {
							log.warn("Skipping chunk because it is too old");
							continue;
						}
						// throw new IllegalArgumentException("Only chunks saved in 1.13+ are supported. Please optimize your world in Minecraft before
						// rendering. Maybe pre 1.13 worlds
						// will be accepted again one day");
					} else {
						log.warn("Skipping chunk because it is way too old (pre 1.9)");
						continue;
						// throw new IllegalArgumentException(
						// "Only chunks saved in 1.13+ are supported, this is pre 1.9!. Please optimize your world in Minecraft before rendering");
					}
				}

				CompoundMap level = ((CompoundTag) root.get("Level")).getValue();

				{// Check chunk status
					String status = ((String) level.get("Status").getValue());
					if (!status.equals("postprocessed") && !status.equals("fullchunk") && !status.equals("mobs_spawned")) {
						log.debug("Skipping chunk because status is " + status);
						continue;
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
								loadedSections[s] = renderSection(sections.get(s));
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
								if (currentColor == Color.MISSING) // == is correct here
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
			} catch (Exception e) {
				log.warn("Skipping chunk", e);
				throw e;
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

	// public static void printLong(long l) {
	// System.out.println(convertLong(l));
	// }
	//
	// public static String convertLong(long l) {
	// String s = Long.toBinaryString(l);
	// // Fancy way of zero padding :)
	// s = "0000000000000000000000000000000000000000000000000000000000000000".substring(s.length()) + s;
	// return s;
	// }
}
