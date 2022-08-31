package de.piegames.blockmap.generate;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

import de.piegames.blockmap.MinecraftBlocks;
import de.piegames.blockmap.color.BiomeColorMap;
import de.piegames.blockmap.color.BiomeColorMap.BiomeColor;
import de.piegames.blockmap.color.BlockColorMap;
import de.piegames.blockmap.color.Color;
import de.piegames.blockmap.generate.ColorMapBuilder.ColorInstruction;
import de.piegames.blockmap.renderer.BlockState;

public class ColorCompiler {

	private static Log log = LogFactory.getLog(ColorCompiler.class);

	/**
	 * Takes in a path to the Minecraft jar file and the path to the json file with the color instructions and compiles all color maps specified
	 * in it.
	 */
	public static Map<String, BlockColorMap> compileBlockColors(Path minecraftJar, Path colorInstructions, MinecraftBlocks minecraftBlocks, BlockState states)
			throws IOException {
		log.info("Compiling " + colorInstructions.toAbsolutePath() + " to color maps");
		log.debug("Minecraft jar: " + minecraftJar.toAbsolutePath());

		FileSystem jarFile = FileSystems.newFileSystem(minecraftJar, (ClassLoader) null);

		Map<String, BlockColorMap> colorMaps = new HashMap<>();
		Map<String, ColorMapBuilder> colorMapHelpers = new HashMap<>();

		/* Parse the whole json file and expand all wildcards and placeholders as well as color map inheritance. */
		try (JsonReader reader = new JsonReader(Files.newBufferedReader(colorInstructions))) {
			JsonObject root = new JsonParser().parse(reader).getAsJsonObject();
			for (Entry<String, JsonElement> map : root.entrySet()) {
				ColorMapBuilder colorMap = new ColorMapBuilder(minecraftBlocks, states);
				log.info("Compiling color map " + map.getKey());

				JsonObject data = map.getValue().getAsJsonObject();
				if (data.has("inherit"))
					colorMap.inherit(colorMapHelpers.get(data.getAsJsonPrimitive("inherit").getAsString()));
				if (data.has("override")) {
					List<String> l = new ArrayList<>();
					for (JsonElement e : data.getAsJsonArray("override"))
						l.add(e.getAsString());
					String toOverride = l.remove(0);
					colorMap.override(colorMapHelpers.get(toOverride), l);
				}
				if (data.has("placeholders")) {
					JsonObject placeholders = data.getAsJsonObject("placeholders");
					placeholders.remove("__comment");// if present
					for (Entry<String, JsonElement> placeholder : placeholders.entrySet()) {
						List<String> replace = new ArrayList<>();
						for (JsonElement s : placeholder.getValue().getAsJsonArray())
							replace.add(s.getAsString());
						colorMap.placeholders.put(placeholder.getKey(), Collections.unmodifiableList(replace));
					}
				}
				if (data.has("colormap")) {
					JsonObject colormap = data.getAsJsonObject("colormap");
					colormap.remove("__comment");// if present
					for (Entry<String, JsonElement> e : colormap.entrySet()) {
						List<String> colorInfo = new ArrayList<>();
						if (e.getValue().isJsonPrimitive())
							colorInfo.addAll(Arrays.asList("texture", e.getValue().getAsString()));
						else {
							for (JsonElement s : e.getValue().getAsJsonArray())
								colorInfo.add(s.getAsString());
						}
						// log.debug("Adding block " + e.getKey() + " " + colorInfo);
						colorMap.addBlock(new ColorInstruction(e.getKey(), colorInfo));
					}
				}
				if (data.has("discardTop"))
					colorMap.discardTop = data.get("discardTop").getAsBoolean();
				colorMapHelpers.put(map.getKey(), colorMap);

				/* Take in the parsed color maps and actually compile the colors to real color maps */
				colorMaps.put(map.getKey(), colorMap.compileColorMap(jarFile));
			}
		}
		return colorMaps;
	}

	/** Compile a texture to a color, removing commands from {@code value} as they are consumed. */
	static Color compileTexture(String key, Queue<String> value, FileSystem jarFile) throws IOException {
		Color color = Color.TRANSPARENT;
		String cmd = value.remove();

		switch (cmd) {
		case "transparent":
			break;
		case "fixed":
			String colorText = value.remove();
			if (colorText.equals("TODO")) {
				color = Color.MISSING;
				log.warn("Block " + key + " has the color 'TODO' assigned with it, please replace this");
			} else
				color = Color.fromRGB((int) (Long.decode(colorText) & 0xFFFFFFFF));
			break;

		case "texture":
			color = getAverageColor(jarFile.getPath("assets/minecraft/textures/block", value.remove()));
			break;

		case "multiply":
			color = Color.multiplyRGBA(compileTexture(key, value, jarFile), compileTexture(key, value, jarFile));
			break;

		default:
			throw new IOException("Block " + key + ": " + value + " is malformed.");
		}
		return color;
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

	// From the Minecraft generated data
	public static class BiomeInfo {
		public static class BiomeInfoEffects {
			Long water_color, foliage_color, grass_color;
		}

		float				temperature, downfall;
		BiomeInfoEffects	effects;
	}

	// Our json file
	public static class BiomeInstructions {
		public static class BiomeColor {
			int r, g, b;
		}

		public String		waterColor;
		public Double		rainfall, temperature;
		public BiomeColor	biomeColor;

		/* For compat with older versions (previously, biomes had a numeric ID) */
		public String		legacyId;
		/* This biome has been removed in 1.18 */
		public boolean		legacy;
	}

	public static BiomeColorMap compileBiomeColors(Path minecraftJar, Path colorInstructions, Path biomeDataDirectory) throws IOException {
		log.info("Compiling " + colorInstructions.toAbsolutePath() + " to color map");
		log.debug("Minecraft jar: " + minecraftJar.toAbsolutePath());
		FileSystem jarFile = FileSystems.newFileSystem(minecraftJar, (ClassLoader) null);

		Map<String, BiomeColor> colorMap = new HashMap<>();

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

		Map<String, BiomeInstructions> instructions = BiomeColorMap.GSON.fromJson(
			Files.newBufferedReader(colorInstructions),
			new TypeToken<Map<String, BiomeInstructions>>() {
			}.getType()
		);
		
		for (var e : instructions.entrySet()) {
			String biomeId = e.getKey();
			String biomeName = biomeId.substring("minecraft:".length());
			BiomeInstructions inst = e.getValue();

			int waterColor, grassColor, foliageColor, biomeColor;
			
			if (inst.legacy) {
				log.debug("Biome " + biomeName + " is historic");
				waterColor = (int) (Long.decode(inst.waterColor) & 0xFFFFFFFF);
				grassColor = grassColor(inst.temperature.doubleValue(), inst.rainfall.doubleValue(), grassColors);
				foliageColor = foliageColor(inst.temperature.doubleValue(), inst.rainfall.doubleValue(), foliageColors);
			} else {
				log.debug("Loading file " + biomeDataDirectory.resolve(biomeName + ".json").toAbsolutePath());
				BiomeInfo biome = BiomeColorMap.GSON.fromJson(Files.newBufferedReader(biomeDataDirectory.resolve(biomeName + ".json")), BiomeInfo.class);

				waterColor = (int) (biome.effects.water_color.longValue() & 0xFFFFFFFF) | 0xFF000000;
				if (biome.effects.grass_color != null) {
					grassColor = (int) (biome.effects.grass_color & 0xFFFFFFFF) | 0xFF000000;
				} else {
					log.debug("Using generic grass color for " + biomeName);
					grassColor = grassColor(biome.temperature, biome.downfall, grassColors);
				}
				if (biome.effects.foliage_color != null) {
					foliageColor = (int) (biome.effects.foliage_color & 0xFFFFFFFF) | 0xFF000000;
				} else {
					log.debug("Using generic foliage color for " + biomeName);
					foliageColor = foliageColor(biome.temperature, biome.downfall, foliageColors);
				}
			}

			biomeColor = 0xFF000000;
			biomeColor |= (inst.biomeColor.r & 0xFF) << 16;
			biomeColor |= (inst.biomeColor.g & 0xFF) << 8;
			biomeColor |= (inst.biomeColor.b & 0xFF);

			BiomeColor color = new BiomeColor(Color.fromRGB(waterColor), Color.fromRGB(grassColor), Color.fromRGB(foliageColor), Color.fromRGB(biomeColor));
			if (!inst.legacy)
				colorMap.put(biomeId, color);
			if (inst.legacyId != null)
				colorMap.put(inst.legacyId, color);
		}

		return new BiomeColorMap(colorMap);
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

	public static List<Color> compileHeightMap(Path heightMapFile) throws IOException {
		BufferedImage image = ImageIO.read(Files.newInputStream(heightMapFile));
		List<Color> ret = new ArrayList<>(image.getWidth());
		for (int x = 0; x < image.getWidth(); x++)
			ret.add(Color.fromRGB(image.getRGB(x, 0)));
		return ret;
	}
}
