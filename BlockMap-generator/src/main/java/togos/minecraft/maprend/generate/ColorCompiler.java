package togos.minecraft.maprend.generate;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import togos.minecraft.maprend.color.BiomeColorMap;
import togos.minecraft.maprend.color.BlockColorMap;
import togos.minecraft.maprend.color.Color;
import togos.minecraft.maprend.renderer.Block;

public class ColorCompiler {

	private static Log log = LogFactory.getLog(ColorCompiler.class);

	public static final int MISSING = 0xFFFF00FF;

	public static BlockColorMap compileBlockColors(Path minecraftJar, Path colorInstructions) throws IOException {
		log.info("Compiling " + colorInstructions.toAbsolutePath() + " to color map");
		log.debug("Minecraft jar: " + minecraftJar.toAbsolutePath());

		Map<Block, Color> blockColors = new HashMap<>();
		Set<Block> grassBlocks = new HashSet<>();
		Set<Block> foliageBlocks = new HashSet<>();
		Set<Block> waterBlocks = new HashSet<>();

		FileSystem jarFile = FileSystems.newFileSystem(minecraftJar, null);
		try (JsonReader reader = new JsonReader(Files.newBufferedReader(colorInstructions))) {
			reader.beginObject();
			reader.skipValue();

			// Replaceable categories
			Map<String, List<String>> replace = new HashMap<>();
			reader.beginObject();
			while (reader.hasNext()) {
				// Parse key/value pair
				String key = reader.nextName();
				if (key.contains("comment")) {
					reader.skipValue();
					continue;
				}
				List<String> list = new LinkedList<>();
				reader.beginArray();
				while (reader.hasNext())
					list.add(reader.nextString());
				reader.endArray();
				replace.put(key, list);
			}
			reader.endObject();

			// Actual color map
			reader.skipValue();
			reader.beginObject();
			while (reader.hasNext()) {
				// Parse key/value pair
				String key = reader.nextName();
				if (key.contains("comment")) {
					reader.skipValue();
					continue;
				}
				List<String> value = new LinkedList<>();
				value.add(key);
				if (reader.peek() == JsonToken.BEGIN_ARRAY) {
					reader.beginArray();
					while (reader.hasNext())
						value.add(reader.nextString());
					reader.endArray();
				} else {
					value.add("texture");
					value.add(reader.nextString());
				}
				int count = value.size();

				// Find anything we need to replace
				List<String> toReplace = new LinkedList<>();
				for (String s : value) {
					if (s.contains("$")) {
						for (String r : replace.keySet())
							if (s.contains("${" + r + "}"))
								toReplace.add(r);
					}
				}

				// Replace all needed values
				for (String search : toReplace) {
					List<String> oldValue = value;
					value = new LinkedList<>();
					for (String r : replace.get(search))
						for (String s : oldValue)
							value.add(s.replace("${" + search + "}", r));
				}

				// Take together our list and compile the texture
				for (int i = 0; i < value.size(); i += count) {
					String actualKey = value.get(i);
					List<String> actualValue = new LinkedList<>();
					for (int j = 1; j < count; j++)
						actualValue.add(value.get(i + j));
					compileTexture(actualKey, actualValue, grassBlocks, jarFile, blockColors, foliageBlocks, waterBlocks);
				}
			}
			reader.endObject();
		}
		log.debug("Grass blocks " + grassBlocks);
		log.debug("Foliage blocks " + foliageBlocks);
		log.debug("Water blocks " + waterBlocks);

		return new BlockColorMap(blockColors, grassBlocks, foliageBlocks, waterBlocks);
	}

	private static void compileTexture(String key, List<String> value, Set<Block> grassBlocks, FileSystem jarFile, Map<Block, Color> blockColors,
			Set<Block> foliageBlocks, Set<Block> waterBlocks) throws IOException {
		log.debug("Compiling texture " + key);
		for (Block block : Block.byCompactForm("minecraft:" + key)) {
			Color color = new Color(0, 0, 0, 0);
			if (value.get(0).equals("transparent"))
				;
			else if (value.get(0).equals("fixed")) {
				if (value.get(1).equals("TODO")) {
					color = Color.MISSING;
					log.warn("Block " + key + " has the color 'TODO' assigned with it, please replace this");
				} else
					color = Color.fromRGB(Integer.decode(value.get(1)));
			} else if (value.get(0).equals("texture")) {
				color = getAverageColor(jarFile.getPath("assets/minecraft/textures/block", value.get(1)));
			} else
				throw new IOException("Block " + key + ": " + value + " is malformed. The first entry must be one of 'transparent', 'fixed' or 'texture'");
			if (value.size() > 2) {
				if (value.get(2).equals("grass"))
					grassBlocks.add(block);
				else if (value.get(2).equals("foliage"))
					foliageBlocks.add(block);
				else if (value.get(2).equals("water"))
					waterBlocks.add(block);
				else
					throw new IOException(
							"Block " + key + ": " + value + " is malformed. The third entry, if present, must be one of 'grass', 'foliage' or 'water'");
			}
			// System.out.println("Adding " + block);
			blockColors.put(block, color);
		}
	}

	private static Color getAverageColor(Path path) throws IOException {
		BufferedImage image = ImageIO.read(Files.newInputStream(path));
		float a = 0, r = 0, g = 0, b = 0;
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				Color color = Color.fromRGB(image.getRGB(x, y));
				a += color.a;
				r += color.r * color.a;
				g += color.g * color.a;
				b += color.b * color.a;
			}
		}
		int pixels = image.getWidth() * image.getHeight();
		return new Color(a / pixels, r / a, g / a, b / a);
	}

	public static BiomeColorMap compileBiomeColors(Path minecraftJar, Path colorInstructions) throws IOException {
		log.info("Compiling " + colorInstructions.toAbsolutePath() + " to color map");
		log.debug("Minecraft jar: " + minecraftJar.toAbsolutePath());
		FileSystem jarFile = FileSystems.newFileSystem(minecraftJar, null);

		Map<Integer, Color> waterColorMap = new HashMap<>();
		Map<Integer, Color> grassColorMap = new HashMap<>();
		Map<Integer, Color> foliageColorMap = new HashMap<>();

		int[] grassColors;
		int[] foliageColors;
		{ // Read grass.png to int array
			BufferedImage bufferedimage = ImageIO.read(Files.newInputStream(jarFile.getPath("assets/minecraft/textures/colormap/grass.png")));
			int w = bufferedimage.getWidth();
			int h = bufferedimage.getHeight();
			grassColors = new int[w * h];
			bufferedimage.getRGB(0, 0, w, h, grassColors, 0, w);
		}
		{ // Read foliage.png to int array
			BufferedImage bufferedimage = ImageIO.read(Files.newInputStream(jarFile.getPath("assets/minecraft/textures/colormap/foliage.png")));
			int w = bufferedimage.getWidth();
			int h = bufferedimage.getHeight();
			foliageColors = new int[w * h];
			bufferedimage.getRGB(0, 0, w, h, foliageColors, 0, w);
		}

		try (JsonReader reader = new JsonReader(Files.newBufferedReader(colorInstructions))) {
			reader.beginObject();
			while (reader.hasNext()) {
				int id = Integer.parseInt(reader.nextName());
				reader.beginObject();
				reader.skipValue();// waterColor
				// TODO use default int again
				int waterColor = (int) (Long.decode(reader.nextString()) & 0xFFFFFFFF);
				reader.skipValue(); // rainfall
				double rainfall = reader.nextDouble();
				reader.skipValue();// temperature
				double temperature = reader.nextDouble();
				reader.endObject();

				waterColorMap.put(id, Color.fromRGB(waterColor));
				grassColorMap.put(id, Color.fromRGB(grassColor(temperature, rainfall, grassColors)));
				foliageColorMap.put(id, Color.fromRGB(foliageColor(temperature, rainfall, foliageColors)));
			}
			reader.endObject();
		}
		return new BiomeColorMap(waterColorMap, grassColorMap, foliageColorMap);
	}

	private static int grassColor(double temperature, double humidity, int[] grassColors) {
		// Clamp values to [0..1]
		if (temperature > 1)
			temperature = 1;
		if (temperature < 0)
			temperature = 0;
		if (humidity > 1)
			humidity = 1;
		if (humidity < 0)
			humidity = 0;

		// Actual calculation
		humidity = humidity * temperature;
		int i = (int) ((1.0D - temperature) * 255.0D);
		int j = (int) ((1.0D - humidity) * 255.0D);
		int k = j << 8 | i;
		return k > grassColors.length ? -65281 : grassColors[k];
	}

	private static int foliageColor(double temperature, double humidity, int[] foliageColors) {
		// Clamp values to [0..1]
		if (temperature > 1)
			temperature = 1;
		if (temperature < 0)
			temperature = 0;
		if (humidity > 1)
			humidity = 1;
		if (humidity < 0)
			humidity = 0;

		humidity = humidity * temperature;
		int i = (int) ((1.0D - temperature) * 255.0D);
		int j = (int) ((1.0D - humidity) * 255.0D);
		return foliageColors[j << 8 | i];
	}
}
