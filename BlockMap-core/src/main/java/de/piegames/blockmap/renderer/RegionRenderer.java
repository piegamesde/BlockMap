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
import org.joml.Vector2i;

import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.IntArrayTag;
import com.flowpowered.nbt.ListTag;
import com.flowpowered.nbt.LongArrayTag;
import com.flowpowered.nbt.StringTag;
import com.flowpowered.nbt.Tag;
import com.flowpowered.nbt.regionfile.RegionFile;
import com.flowpowered.nbt.regionfile.RegionFile.RegionChunk;

import de.piegames.blockmap.color.BiomeColorMap;
import de.piegames.blockmap.color.BlockColorMap;
import de.piegames.blockmap.color.Color;

public class RegionRenderer {

	private static Log			log	= LogFactory.getLog(RegionRenderer.class);

	public final RenderSettings	settings;

	protected BlockColorMap		blockColors;
	protected BiomeColorMap		biomeColors;

	public RegionRenderer(RenderSettings settings) {
		this.settings = Objects.requireNonNull(settings);
		this.blockColors = Objects.requireNonNull(settings.blockColors);
		this.biomeColors = Objects.requireNonNull(settings.biomeColors);
	}

	public BufferedImage render(Vector2i regionPos, RegionFile file) throws IOException {
		log.info("Rendering region file " + regionPos.x + " " + regionPos.y);
		BufferedImage image = new BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB);
		Color[] colors = renderRaw(regionPos, file);
		// image.setRGB(0, 0, 512, 512, colors, 0, 512);
		for (int x = 0; x < 512; x++)
			for (int z = 0; z < 512; z++)
				if (colors[x | (z << 9)] != null)
					image.setRGB(x, z, colors[x | (z << 9)].toRGB());
		return image;
	}

	public Color[] renderRaw(Vector2i regionPos, RegionFile file) throws IOException {
		// The final map of the chunk, 512*512 pixels, XZ
		Color[] map = new Color[512 * 512];
		int[] height = new int[512 * 512];
		Arrays.fill(height, 256);

		for (RegionChunk chunk : file.listExistingChunks()) {
			try {
				int chunkX = ((regionPos.x << 5) | chunk.x);
				int chunkZ = ((regionPos.y << 5) | chunk.z);
				if ((chunkX + 16 < settings.minX || chunkX > settings.maxX)
						&& (chunkZ + 16 < settings.minZ || chunkZ > settings.maxZ)) {
					log.debug("Skipping chunk: out of bounds " + chunkX + " " + chunkZ);
					continue;
				}
				log.debug("Rendering chunk " + chunkX + " " + chunkZ);
				chunk.load();

				CompoundMap root = chunk.readTag().getValue();
				chunk.unload();

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
						// log.warn("Skipping chunk because status is " + status);
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
				Color[][] loadedSections = new Color[16][];

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
						if (currentColor.equals(lastColor))
							lastColorTimes++;
						else {
							color = Color.alphaUnder(color, lastColor, lastColorTimes);
							lastColorTimes = 1;
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
						ColorColumn color = new ColorColumn();
						height: for (byte s = 15; s >= 0; s--) {
							if ((s << 4) + 15 > settings.maxY)
								continue;
							if (s < lowestLoadedSection) {
								// log.debug("Loading section " + s);
								loadedSections[s] = renderSection(biomes, sections.get(s));
								lowestLoadedSection = s;
							}
							if (loadedSections[s] == null) {
								// Sector is full of air
								// TODO use air16color
								continue;
							}
							for (int y = 15; y >= 0; y--) {
								if ((y | s << 4) < settings.minY)
									break height;
								if ((y | s << 4) > settings.maxY)
									continue;
								// color = Color.alphaOver(loadedSections[s][x | z << 4 | y << 8], color);

								Color currentColor = loadedSections[s][x | z << 4 | y << 8];
								color.putColor(currentColor);
								/* As long as the blocks are transparent enough, we keep track of the height */
								// TODO use the actual height maps
								// if (color.a < 0.2)
								// height[chunk.x << 4 | x | chunk.z << 13 | z << 9] = y | s << 4;
								if (color.needStop) {
									break height;
								}
							}
						}
						map[chunk.x << 4 | x | chunk.z << 13 | z << 9] = color.getFinal();
					}
			} catch (Exception e) {
				log.warn("Skipping chunk", e);
				throw e;
			}
		}

		{// Shading
			for (int z = 0; z < 512; z++)
				for (int x = 0; x < 512; x++) {
					if (map[z << 9 | x] == null)
						continue;
					int centerHeight = height[z << 9 | x];
					int westHeight = height[z << 9 | Math.max(x - 1, 0)];
					int eastHeight = height[z << 9 | Math.min(x + 1, 511)];
					int northHeight = height[Math.max(z - 1, 0) << 9 | x];
					int southHeight = height[Math.min(z + 1, 511) << 9 | x];
					int northWestHeight = height[Math.max(z - 1, 0) << 9 | Math.max(x - 1, 0)];
					int northEastHeight = height[Math.max(z - 1, 0) << 9 | Math.min(x + 1, 511)];
					int southWestHeight = height[Math.min(z + 1, 511) << 9 | Math.max(x - 1, 0)];
					int southEastHeight = height[Math.min(z + 1, 511) << 9 | Math.min(x + 1, 511)];
					double gX = (northWestHeight * 0 + 2 * westHeight + southWestHeight * 0 - eastHeight * 0 - 2 * northEastHeight - southEastHeight * 0);
					double gY = (northWestHeight * 0 + 2 * northHeight + northEastHeight * 0 - southWestHeight * 0 - 2 * southHeight - southEastHeight * 0);
					double g = Math.sqrt(gX * gX + gY * gY);
					// Vector3f gradientX = new Vector3f(2, (northWestHeight + westHeight + southWestHeight - eastHeight - northEastHeight - southEastHeight) /
					// 3,
					// 0);
					// Vector3f gradientZ = new Vector3f(0,
					// (northHeight + northWestHeight + northEastHeight - southHeight - southWestHeight - southEastHeight) / 3, 2);
					// Vector3f gradient = gradientZ.cross(gradientX, new Vector3f());
					// Vector3f sunlight = new Vector3f(0, 1, 0);
					// double angle = (sunlight.angle(gradient) / (2 * Math.PI));

					// Calculate the angle of the slope [0..2PI]
					// double factor = ((Math.atan2(gY, gX) + Math.PI + 7 / 4d * Math.PI) % (2 * Math.PI)) - Math.PI;
					// Offset it by 135° (3/4 PI)
					// factor = ((factor + 3 / 4f * Math.PI) % (2 * Math.PI));
					// Map to [-1..1]
					// factor /= 2 * Math.PI;
					// Map to [0..1]
					// factor = (Math.abs(factor));
					// factor = Color.linearRGBTosRGB(factor);
					// factor = 2 * (factor - 0.5);

					// factor *= Math.tanh(g / 5);
					// Map to [-1..1]
					// factor = (factor + 1) / 2;
					double factor = -Math.tanh((gX + gY) / 10);
					factor *= 0.3;

					// if (g == 0)
					// map[z << 9 | x] = Color.fromRGB(java.awt.Color.HSBtoRGB((float) factor, 1, 0));
					// else
					// map[z << 9 | x] = Color.fromRGB(java.awt.Color.HSBtoRGB((float) factor, 1, 1));

					// map[z << 9 | x] = Color.fromRGB(java.awt.Color.HSBtoRGB(0, 0, (float) factor));

					// factor = (1.0 * (centerHeight - settings.minY) / (settings.maxY - settings.minY));
					// map[z << 9 | x] = new Color(1, (float) factor, (float) factor, (float) factor);
					// map[z << 9 | x] = new Color(1, (float) factor, (float) factor, (float) factor);
					// map[z << 9 | x] = Color.shade(map[z << 9 | x], (float) (x / 512d - 0.5) * 2f);

					map[z << 9 | x] = Color.shade(map[z << 9 | x], (float) factor);

					// Test 3
					// int maxHeight = Math.max(Math.max(northHeight, southHeight), Math.max(westHeight, eastHeight));
					// int minHeight = Math.min(Math.min(northHeight, southHeight), Math.min(westHeight, eastHeight));
					// if (maxHeight >>> 3 != minHeight >>> 3)
					// map[z << 9 | x] = Color.alphaOver(map[z << 9 | x],
					// new Color((float) Color.sRGBToLinear(Math.min(((maxHeight - minHeight + 8) >>> 3) / 10.0, 1)), 0, 0, 0));

					// Test 1
					// map[z << 9 | x] = Color.alphaOver(map[z << 9 | x], new Color((float) Math.tanh(g / 20) * 0.6f, 0, 0, 0));

					// Test 2
					// if ((height[z << 9 | x] & 7) == 0 && g > 0.05)
					// map[z << 9 | x] = Color.alphaOver(map[z << 9 | x], new Color(0.1f, 0, 0, 0));
				}
		}
		return map;
	}

	/**
	 * Takes in the NBT data for a section and returns an int[] containing the color of each block in that section. The returned array thus has
	 * a length of 16³=4096 items and the blocks are mapped to them in XZY order.
	 */
	private Color[] renderSection(int[] biomes, CompoundMap section) {
		if (section == null)
			return null;

		// System.out.println(section.get("Palette"));
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
		Color[] ret = new Color[16 * 16 * 16];

		for (int i = 0; i < 4096; i++) {
			long blockIndex = RegionFile.extractFromLong(blocks, i, bitsPerIndex);

			if (blockIndex >= palette.size()) {
				ret[i] = Color.MISSING;
				log.warn("Block " + i + " " + blockIndex + " was out of bounds, is this world corrupt?");
				continue;
			}
			Block block = palette.get((int) blockIndex);
			Color color = blockColors.getBlockColor(block);
			if (color == Color.MISSING) // == is correct here
				log.warn("Missing color for " + block);

			if (blockColors.isGrassBlock(block))
				color = Color.multiplyRGB(color, biomeColors.getGrassColor(biomes[i & 0xFF]));
			if (blockColors.isFoliageBlock(block))
				color = Color.multiplyRGB(color, biomeColors.getFoliageColor(biomes[i & 0xFF]));
			if (blockColors.isWaterBlock(block))
				color = Color.multiplyRGB(color, biomeColors.getWaterColor(biomes[i & 0xFF]));
			ret[i] = color;
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
