package togos.minecraft.maprend.renderer;

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
import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.IntArrayTag;
import com.flowpowered.nbt.ListTag;
import com.flowpowered.nbt.LongArrayTag;
import com.flowpowered.nbt.StringTag;
import com.flowpowered.nbt.Tag;
import com.flowpowered.nbt.regionfile.RegionFile;
import com.flowpowered.nbt.regionfile.RegionFile.RegionChunk;
import togos.minecraft.maprend.Color;
import togos.minecraft.maprend.renderer.BiomeColorMap.Biome;
import togos.minecraft.maprend.texture.ColorCompiler;

public class RegionRenderer {

	private static Log			log					= LogFactory.getLog(RegionRenderer.class);

	/**
	 * Alpha below which blocks are considered transparent for purposes of shading (i.e. blocks with alpha < this will not be shaded, but blocks below them will be)
	 */
	private int					shadeOpacityCutoff	= 0x20;										// TODO use it

	public final RenderSettings	settings;

	protected BlockColorMap		blockColors;
	protected BiomeColorMap		biomeColors			= BiomeColorMap.loadDefault();

	public final int			air16Color			= -1;										// Color of 16 air blocks stacked TODO use it

	public RegionRenderer(RenderSettings settings) {
		this.settings = Objects.requireNonNull(settings);
		blockColors = BlockColorMap.loadDefault();
	}

	public int[] render(RegionFile file) throws IOException {
		// The final map of the chunk, 512*512 pixels, XZ
		int[] map = new int[512 * 512];
		int[] height = new int[512 * 512];
		Arrays.fill(height, 256);

		for (RegionChunk chunk : file.listExistingChunks()) {
			try {
				// log.debug("Rendering chunk " + chunk.x + " " + chunk.z);
				chunk.load();

				// System.out.println(chunk.readTag());
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
						// throw new IllegalArgumentException("Only chunks saved in 1.13+ are supported. Please optimize your world in Minecraft before rendering. Maybe pre 1.13 worlds
						// will be accepted again one day");
					} else {
						throw new IllegalArgumentException("Only chunks saved in 1.13+ are supported, this is pre 1.9!. Please optimize your world in Minecraft before rendering");
					}
				}

				CompoundMap level = ((CompoundTag) root.get("Level")).getValue();

				{// Check chunk status
					String status = ((String) level.get("Status").getValue());
					if (!status.equals("postprocessed") && !status.equals("fullchunk") && !status.equals("mobs_spawned")) {
						log.warn("Skipping chunk because status is " + status);
						continue;
					}
				}

				int[] biomes = ((IntArrayTag) level.get("Biomes")).getValue();

				/* The height of the lowest section that has already been loaded. Section are loaded lazily from top to bottom and this value gets decreased each time a new one
				 * has been loaded */
				int lowestLoadedSection = 16;
				/* Null entries indicate a section full of air */
				int[][] loadedSections = new int[16][];

				// Get the list of all sections and map them to their y coordinate using streams
				@SuppressWarnings("unchecked")
				Map<Byte, CompoundMap> sections = ((ListTag<CompoundTag>) level.getOrDefault("Sections", new ListTag<>("sections", CompoundTag.class, Collections.emptyList()))).getValue().stream()
						.collect(Collectors.toMap(s -> (Byte) s.getValue().get("Y").getValue(), s -> s.getValue()));

				// Traverse the chunk in YXZ order
				for (byte z = 0; z < 16; z++)
					for (byte x = 0; x < 16; x++) {
						int color = 0;
						height: for (byte s = 15; s >= 0; s--) {
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
								color = loadedSections[s][x | z << 4 | y << 8];
								// color = Color.alpha_over(loadedSections[s][x | z << 4 | y << 8], color);
								height[chunk.x << 4 | x | chunk.z << 13 | z << 9] = y | s << 4;
								if (Color.alpha(color) > 0) {// TODO use threshold
									break height;
								}
							}
						}
						// Alpha over black to get rid of remaining opacity
						// TODO remove?
						color = Color.overlay(0xFF000000, color);
						map[chunk.x << 4 | x | chunk.z << 13 | z << 9] = color;
					}
			} catch (Exception e) {
				log.warn("Skipping chunk", e);
				throw e;
			}
		}
		shade(map, height);
		return map;
	}

	private void shade(int[] color, int[] height) {
		int width = 512, depth = 512;

		int idx = 0;
		for (int z = 0; z < depth; ++z) {
			for (int x = 0; x < width; ++x, ++idx) {
				float dyx, dyz;

				if (color[idx] == 0)
					continue;

				if (x == 0)
					dyx = height[idx + 1] - height[idx];
				else if (x == width - 1)
					dyx = height[idx] - height[idx - 1];
				else
					dyx = (height[idx + 1] - height[idx - 1]) * 2;

				if (z == 0)
					dyz = height[idx + width] - height[idx];
				else if (z == depth - 1)
					dyz = height[idx] - height[idx - width];
				else
					dyz = (height[idx + width] - height[idx - width]) * 2;

				float shade = dyx + dyz;
				if (shade > 10)
					shade = 10;
				if (shade < -10)
					shade = -10;

				int altShade = settings.altitudeShadingFactor * (height[idx] - settings.shadingReferenceAltitude) / 255;
				if (altShade < settings.minAltitudeShading)
					altShade = settings.minAltitudeShading;
				if (altShade > settings.maxAltitudeShading)
					altShade = settings.maxAltitudeShading;

				shade += altShade;

				// color[idx] = java.awt.Color.HSBtoRGB(((height[idx] / 255f) - 64) * 2 + 64, 1, 1);
				// color[idx] = height[idx] | height[idx] << 8 | height[idx] << 16 | 0xFF000000;
				color[idx] = Color.shade(color[idx], (int) (shade * 8));
			}
		}
	}

	/**
	 * Takes in the nbt data for a section and returns an int[] containing the color of each block in that section. The returned array thus has a length of 16³=4096
	 * items and the blocks are mapped to them in XZY order.
	 */
	private int[] renderSection(int[] biomes, CompoundMap section) {
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
		int[] ret = new int[16 * 16 * 16];

		for (int i = 0; i < 4096; i++) {
			long blockIndex = extractFromLong(blocks, i, bitsPerIndex);

			if (blockIndex >= palette.size()) {
				ret[i] = 0xFF00FFFF;
				System.out.println("Block " + i + " " + blockIndex + " was out of bounds, size " + bitsPerIndex);
				continue;
			}
			Block block = palette.get((int) blockIndex);
			int color = blockColors.getBlockColor(block);
			if (color == ColorCompiler.MISSING)
				log.warn("Missing color for " + block);
			Biome biome = biomeColors.getBiome(biomes[i & 0xFF]);
			if (blockColors.isGrassBlock(block))
				color = Color.multiplySolid(color, biome.grassColor);
			if (blockColors.isFoliageBlock(block))
				color = Color.multiplySolid(color, biome.foliageColor);
			if (blockColors.isWaterBlock(block))
				color = Color.multiplySolid(color, biome.waterColor);
			ret[i] = color;
		}

		return ret;
	}

	public static void printLong(long l) {
		String s = Long.toBinaryString(l);
		s = "0000000000000000000000000000000000000000000000000000000000000000".substring(s.length()) + s;
		System.out.println(s);
	}

	public static EnumSet<BlockState> parseBlockState(CompoundTag properties) {
		EnumSet<BlockState> ret = EnumSet.noneOf(BlockState.class);
		if (properties != null)
			for (Entry<String, Tag<?>> entry : properties.getValue().entrySet())
				ret.add(BlockState.valueOf(entry.getKey(), ((StringTag) entry.getValue()).getValue()));
		return ret;
	}

	public static long extractFromLong2(long[] blocks, int i, int bitsPerIndex) {
		StringBuffer number = new StringBuffer();
		// Reverse all longs, convert them to a binary string, zero pad them and concatenate them
		for (long l : blocks)
			number.append(RegionRenderer.convertLong(Long.reverse(l)));
		return Long.parseLong(new StringBuilder(number.substring(i * bitsPerIndex, i * bitsPerIndex + bitsPerIndex)).reverse().toString(), 2);
	}

	public static String convertLong(long l) {
		String s = Long.toBinaryString(l);
		// Fancy way of zero padding :)
		s = "0000000000000000000000000000000000000000000000000000000000000000".substring(s.length()) + s;
		return s;
	}

	public static long extractFromLong(long[] blocks, int i, int bitsPerIndex) {
		int startByte = (bitsPerIndex * i) >> 6; // >> 6 equals / 64
		int endByte = (bitsPerIndex * (i + 1)) >> 6;
		// The bit within the long where our value starts. Counting from the right LSB (!).
		int startByteBit = ((bitsPerIndex * i)) & 63; // % 64 equals & 63
		int endByteBit = ((bitsPerIndex * (i + 1))) & 63;

		// Use bit shifting and & bit masking to extract bit sequences out of longs as numbers
		// -1L is the value with every bit set
		long blockIndex;
		if (startByte == endByte) {
			// Normal case: the bit string we need is within a single long
			blockIndex = (blocks[startByte] << (64 - endByteBit)) >>> (64 + startByteBit - endByteBit);
		} else if (endByteBit == 0) {
			// The bit string is exactly at the beginning of a long
			blockIndex = blocks[startByte] >>> startByteBit;
		} else {
			// The bit string is overlapping two longs
			blockIndex = ((blocks[startByte] >>> startByteBit))
					| ((blocks[endByte] << (64 - endByteBit)) >>> (startByteBit - endByteBit));
		}
		return blockIndex;
	}
}
