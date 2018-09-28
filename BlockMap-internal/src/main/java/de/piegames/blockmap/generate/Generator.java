package de.piegames.blockmap.generate;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import de.piegames.blockmap.BlockStateHelper;
import de.piegames.blockmap.BlockStateHelper.BlockStateHelperBlock;
import de.piegames.blockmap.BlockStateHelper.BlockStateHelperState;
import de.piegames.blockmap.color.BiomeColorMap;
import de.piegames.blockmap.color.BlockColorMap;
import de.piegames.blockmap.color.Color;

public class Generator {

	private static Log log = LogFactory.getLog(Generator.class);

	public static void generateBlockColors() throws IOException {
		log.info("Generating block colors");
		Path minecraftJarfile = Paths.get(URI.create(Generator.class.getResource("/minecraft.jar").toString()));

		for (Entry<String, BlockColorMap> map : ColorCompiler.compileBlockColors(minecraftJarfile,
				Paths.get(URI.create(Generator.class.getResource("/block-color-instructions.json").toString()))).entrySet()) {
			log.info("Writing block-colors-" + map.getKey() + ".json");
			try (BufferedWriter writer = Files.newBufferedWriter(Paths.get("./output", "block-colors-" + map.getKey() + ".json"))) {
				BlockColorMap.GSON.toJson(map.getValue(), writer);
				writer.flush();
			}
			// try (Writer writer = new OutputStreamWriter(new DeflaterOutputStream(new BufferedOutputStream(
			// Files.newOutputStream(Paths.get("./output", "block-colors-" + map.getKey() + ".json.zip")))))) {
			// BlockColorMap.GSON.toJson(map.getValue(), writer);
			// writer.flush();
			// }
			// try (Writer writer = new OutputStreamWriter(new GZIPOutputStream(new BufferedOutputStream(
			// Files.newOutputStream(Paths.get("./output", "block-colors-" + map.getKey() + ".json.zip")))))) {
			// BlockColorMap.GSON.toJson(map.getValue(), writer);
			// writer.flush();
			// }
		}
	}

	public static void generateBiomeColors() throws IOException {
		log.info("Generating biome colors");
		Path minecraftJarfile = Paths.get(URI.create(Generator.class.getResource("/minecraft.jar").toString()));

		BiomeColorMap map = ColorCompiler.compileBiomeColors(minecraftJarfile,
				Paths.get(URI.create(Generator.class.getResource("/biome-color-instructions.json").toString())));
		try (BufferedWriter writer = Files.newBufferedWriter(Paths.get("./output", "biome-colors.json"))) {
			BlockColorMap.GSON.toJson(map, writer);
			writer.flush();
		}
	}

	public static void generateHeightmap () throws IOException {
		log.info("Generating heightmap colors");
		List<Color> colors = ColorCompiler.compileHeightMap(Paths.get(URI.create(Generator.class.getResource("/heightmap.png").toString())));

		try (BufferedWriter writer = Files.newBufferedWriter(Paths.get("./output", "heightmap.json"))) {
			Color.GSON.toJson(colors, writer);
			writer.flush();
		}
	}

	public static void generateBlockStates() throws IOException {
		log.info("Generating BlockState enum class");
		Type type = new TypeToken<Map<String, BlockStateHelperBlock>>() {
		}.getType();
		Map<String, BlockStateHelperBlock> blocks = new Gson().fromJson(new InputStreamReader(Generator.class.getResourceAsStream("/blocks.json")), type);

		Comparator<BlockStateHelper> c = Comparator.comparing(s -> s.name);

		// Map each block id to a set of possible block states
		Map<String, Set<BlockStateHelper>> statesByBlock = new HashMap<>();

		for (Entry<String, BlockStateHelperBlock> e : blocks.entrySet()) {
			if (!statesByBlock.containsKey(e.getKey()))
				statesByBlock.put(e.getKey(), new HashSet<>());
			Set<BlockStateHelper> states = statesByBlock.get(e.getKey());
			for (BlockStateHelperState state : e.getValue().states) {
				if (state.properties != null)
					state.properties.forEach((k, v) -> states.add(new BlockStateHelper(k, v)));
			}
		}

		// Reverse map each block state to the blocks that are allowed to use it (maybe a BiMap would help?)
		Map<BlockStateHelper, Set<String>> blocksByState = new TreeMap<>(c.thenComparing(Comparator.comparing(s -> s.value)));
		for (Entry<String, Set<BlockStateHelper>> e : statesByBlock.entrySet()) {
			for (BlockStateHelper state : e.getValue()) {
				if (blocksByState.containsKey(state)) {
					blocksByState.get(state).add(e.getKey());
				} else {
					Set<String> allowed = new HashSet<>();
					allowed.add(e.getKey());
					blocksByState.put(state, allowed);
				}
			}
		}

		// We can now start generating our actual enum code
		StringBuilder builder = new StringBuilder();
		for (Entry<BlockStateHelper, Set<String>> e : blocksByState.entrySet()) {
			String key = e.getKey().name;
			String value = e.getKey().value;
			builder.append('\t');
			builder.append(key.toUpperCase());
			builder.append("_");
			builder.append(value.toUpperCase());
			builder.append("(\"");
			builder.append(key);
			builder.append("\", \"");
			builder.append(value);
			builder.append("\"");
			if (!e.getValue().isEmpty()) {
				for (String s : e.getValue()) {
					builder.append(", \"");
					builder.append(s);
					builder.append('"');
				}
			}
			builder.append("),");
			builder.append(System.getProperty("line.separator"));
		}
		// Hardcode the map property of item frames since it does not show up in this list
		builder.append("MAP_TRUE(\"map\", \"true\"),");
		builder.append(System.getProperty("line.separator"));
		builder.append("MAP_FALSE(\"map\", \"false\");");

		// Load template file and replace original
		// String original = new String(Files.readAllBytes(Paths.get(URI.create(Generator.class.getResource("/BlockState.java").toString()))));
		// original = original.replace("$REPLACE", builder.toString());
		// Files.write(Paths.get("./src/main/java", "togos/minecraft/maprend/renderer", "BlockState.java"), original.getBytes());
		Files.write(Paths.get("./output", "BlockState.java"), builder.toString().getBytes());
	}

	public static void main(String[] args) throws IOException {
		log.info("Output path " + Paths.get("./output").toAbsolutePath());
		Files.createDirectories(Paths.get("./output"));
		generateBlockColors();
		generateBiomeColors();
		generateHeightmap();
		generateBlockStates();
		log.info("Done.");
	}
}
