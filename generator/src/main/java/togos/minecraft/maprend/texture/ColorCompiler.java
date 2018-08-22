package togos.minecraft.maprend.texture;

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
import togos.minecraft.maprend.Color;
import togos.minecraft.maprend.renderer.Block;
import togos.minecraft.maprend.renderer.BlockColorMap;

public class ColorCompiler {

	private static Log		log		= LogFactory.getLog(ColorCompiler.class);

	public static final int	MISSING	= 0xFFFF00FF;

	public static BlockColorMap compile(Path minecraftJar, Path colorInstructions) throws IOException {
		log.info("Compiling " + colorInstructions.toAbsolutePath() + " to color map");
		log.debug("Minecraft jar: " + minecraftJar.toAbsolutePath());

		Map<Block, Integer> blockColors = new HashMap<>();
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

	private static void compileTexture(String key, List<String> value, Set<Block> grassBlocks, FileSystem jarFile, Map<Block, Integer> blockColors, Set<Block> foliageBlocks, Set<Block> waterBlocks) throws IOException {
		log.debug("Compiling texture " + key);
		for (Block block : Block.byCompactForm("minecraft:" + key)) {
			int color = 0;
			if (value.get(0).equals("transparent"))
				;
			else if (value.get(0).equals("fixed")) {
				if (value.get(1).equals("TODO")) {
					color = MISSING;
					log.warn("Block " + key + " has the color 'TODO' assigned with it, please replace this");
				} else
					color = Integer.decode(value.get(1));
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
					throw new IOException("Block " + key + ": " + value + " is malformed. The third entry, if present, must be one of 'grass', 'foliage' or 'water'");
			}
			// System.out.println("Adding " + block);
			blockColors.put(block, color);
		}
	}

	private static int getAverageColor(Path path) throws IOException {
		// TODO proper alpha handling
		BufferedImage image = ImageIO.read(Files.newInputStream(path));
		int a = 0, r = 0, g = 0, b = 0;
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				int color = image.getRGB(x, y);
				a += Color.component(color, 24);
				r += Color.component(color, 16);
				g += Color.component(color, 8);
				b += Color.component(color, 0);
			}
		}
		int pixels = image.getWidth() * image.getHeight();
		return Color.color(a / pixels, r / pixels, g / pixels, b / pixels);
	}
}
