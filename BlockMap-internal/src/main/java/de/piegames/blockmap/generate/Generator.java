package de.piegames.blockmap.generate;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.joml.Vector2i;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import de.piegames.blockmap.BlockStateHelper;
import de.piegames.blockmap.BlockStateHelper.BlockStateHelperBlock;
import de.piegames.blockmap.BlockStateHelper.BlockStateHelperState;
import de.piegames.blockmap.color.BiomeColorMap;
import de.piegames.blockmap.color.BlockColorMap;
import de.piegames.blockmap.color.Color;
import morlok8k.MinecraftLandGenerator.MinecraftLandGenerator;
import morlok8k.MinecraftLandGenerator.Server;
import morlok8k.MinecraftLandGenerator.World;
import morlok8k.MinecraftLandGenerator.World.Dimension;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command
public class Generator {

	private static Log			log						= LogFactory.getLog(Generator.class);

	/*
	 * All output paths are listed here. They are split into three categories: main-resources, test-resources, other. The resources get copied
	 * to the runtime resources folder through #processResources(). Those are in OUTPUTS.
	 */

	static final Path			OUTPUT					= Paths.get("./build/generated-resources");
	static final Path			OUTPUT_CORE				= OUTPUT.resolve("BlockMap-core/generated-resources-main");
	static final Path			OUTPUT_INTERNAL_MAIN	= OUTPUT.resolve("BlockMap-internal/generated-resources-main");
	static final Path			OUTPUT_INTERNAL_TEST	= OUTPUT.resolve("BlockMap-internal/generated-resources-test");
	static final Path			OUTPUT_STANDALONE		= OUTPUT.resolve("BlockMap-standalone/generated-resources-main");
	static final Path			OUTPUT_GUI				= OUTPUT.resolve("BlockMap-gui/generated-resources-main");
	static final Path			OUTPUT_SCREENSHOTS		= Paths.get("../screenshots");
	private static final Path[]	OUTPUTS					= { OUTPUT_CORE, OUTPUT_INTERNAL_MAIN, OUTPUT_INTERNAL_TEST, OUTPUT_STANDALONE, OUTPUT_GUI };

	@Command
	public void downloadFiles() throws IOException {
		Downloader.downloadMinecraft();
		Downloader.downloadLandGenerator();

		processResources();
	}

	@Command
	public void extractData() throws Exception {
		// Call the Minecraft data generator
		FileUtils.deleteDirectory(OUTPUT_INTERNAL_MAIN.resolve("data").toFile());
		try (URLClassLoader loader = new URLClassLoader(
				new URL[] { Generator.class.getResource("/server.jar") },
				Generator.class.getClassLoader())) {
			Class<?> MinecraftMain = Class.forName("net.minecraft.data.Main", true, loader);
			Method main = MinecraftMain.getDeclaredMethod("main", String[].class);
			main.invoke(null, new Object[] { new String[] { "--reports", "--output=" + OUTPUT_INTERNAL_MAIN.resolve("data") } });
		}

		processResources();
	}

	@Command
	public void generateTestWorld() throws IOException, InterruptedException {
		log.info("Generating test world");

		Path worldPath = OUTPUT_INTERNAL_MAIN.resolve("BlockMapWorld");

		FileUtils.copyDirectory(new File(URI.create(Generator.class.getResource("/BlockMapWorld").toString())), worldPath
				.toFile());
		Path serverFolder = Files.createTempDirectory("generateTestWorldServer");
		Path serverFile = serverFolder.resolve("server.jar");
		Files.copy(Generator.class.getResourceAsStream("/server.jar"), serverFile);
		Files.createSymbolicLink(serverFolder.resolve("world"), worldPath.toAbsolutePath());

		Server server = new Server(serverFile, null);
		World world = server.initWorld(Paths.get("world"), true);

		int SIZE = 256;// 256;
		ArrayList<Vector2i> chunks = new ArrayList<>(SIZE * SIZE * 4);
		for (int z = -SIZE; z < SIZE; z++)
			for (int x = -SIZE; x < SIZE; x++)
				chunks.add(new Vector2i(x, z));
		MinecraftLandGenerator.forceloadChunks(server, world, chunks, Dimension.OVERWORLD, true, 64 * 64, false);
		world.resetChanges();
		server.resetChanges();

		processResources();
	}

	@Command
	public void generateBlockColors() throws IOException {
		log.info("Generating block colors");
		Path minecraftJarfile = Paths.get(URI.create(Generator.class.getResource("/client.jar").toString()));

		for (Entry<String, BlockColorMap> map : ColorCompiler.compileBlockColors(minecraftJarfile,
				Paths.get(URI.create(Generator.class.getResource("/block-color-instructions.json").toString()))).entrySet()) {
			log.info("Writing block-colors-" + map.getKey() + ".json to " + OUTPUT_CORE.resolve("block-colors-" + map.getKey() + ".json"));
			try (BufferedWriter writer = Files.newBufferedWriter(OUTPUT_CORE.resolve("block-colors-" + map.getKey() + ".json"))) {
				BlockColorMap.GSON.toJson(map.getValue(), writer);
				writer.flush();
			}
		}

		processResources();
	}

	@Command
	public void generateBiomeColors() throws IOException {
		log.info("Generating biome colors");
		Path minecraftJarfile = Paths.get(URI.create(Generator.class.getResource("/client.jar").toString()));

		BiomeColorMap map = ColorCompiler.compileBiomeColors(minecraftJarfile,
				Paths.get(URI.create(Generator.class.getResource("/biome-color-instructions.json").toString())));
		try (BufferedWriter writer = Files.newBufferedWriter(OUTPUT_CORE.resolve("biome-colors.json"))) {
			BlockColorMap.GSON.toJson(map, writer);
			writer.flush();
		}

		processResources();
	}

	@Command
	public void generateHeightmap() throws IOException {
		log.info("Generating heightmap colors");
		List<Color> colors = ColorCompiler.compileHeightMap(Paths.get(URI.create(Generator.class.getResource("/heightmap.png").toString())));

		try (BufferedWriter writer = Files.newBufferedWriter(OUTPUT_CORE.resolve("heightmap.json"))) {
			Color.GSON.toJson(colors, writer);
			writer.flush();
		}

		processResources();
	}

	@Command
	public void generateBlockStates() throws IOException {
		log.info("Generating BlockState enum class");
		Type type = new TypeToken<Map<String, BlockStateHelperBlock>>() {
		}.getType();
		Map<String, BlockStateHelperBlock> blocks = new Gson().fromJson(new InputStreamReader(Generator.class.getResourceAsStream("/data/reports/blocks.json")),
				type);

		Comparator<BlockStateHelper> c = Comparator.comparing(s -> s.name);

		/* Map each block id to a set of possible block states */
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

		/* Reverse map each block state to the blocks that are allowed to use it (maybe a BiMap would help?) */
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

		/* We can now start generating our actual enum code */
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
		/* Hardcode the map property of item frames since it does not show up in this list */
		builder.append("\tMAP_TRUE(\"map\", \"true\"),");
		builder.append(System.getProperty("line.separator"));
		builder.append("\tMAP_FALSE(\"map\", \"false\");");

		{ /* Load template file and replace original */
			Path blockState = Paths.get("../BlockMap-core", "src/main/java", "de/piegames/blockmap", "renderer/BlockState.java");
			List<String> file = Files.readAllLines(blockState);
			int line = 0;
			while (!file.get(line).contains("$REPLACE_START"))
				line++;
			line++;
			while (!file.get(line).contains("$REPLACE_END"))
				file.remove(line);
			file.add(line, builder.toString());
			Files.write(blockState, file);
		}
		// String original = new String(Files.readAllBytes(Paths.get(URI.create(Generator.class.getResource("/BlockState.txt").toString()))));
		// original = original.replace("//$REPLACE_ME_HERE", builder.toString());
		// Files.write(OUTPUT_CORE_SRC.resolve("de/piegames/blockmap/renderer/BlockState.java"), original.getBytes());
		// Files.write(OUTPUT_OTHER.resolve("BlockState.java"), builder.toString().getBytes());

		processResources();
	}

	@Command
	public void generateVersion(String version) throws Exception {
		log.info("Generating VersionProvider for version " + version);
		Path versionProvider = Paths.get("../BlockMap-standalone/", "src/main/java", "de/piegames/blockmap/standalone", "VersionProvider.java");
		List<String> file = Files.readAllLines(versionProvider);
		int line = 0;
		while (!file.get(line).contains("$REPLACE_START"))
			line++;
		line++;
		while (!file.get(line).contains("$REPLACE_END"))
			file.remove(line);
		file.add(line, "\t\treturn new String[] { \"" + version + "\" };");
		Files.write(versionProvider, file);
	}

	@Command
	public void generateScreenshots() throws Exception {
		log.info("Generating screenshots");
		Screenshots.generateDemoRenders();
		Screenshots.generateScreenshots();
		processResources();
	}

	public static void main(String[] args) throws Exception {
		/* Without this, JOML will print vectors out in scientific notation which isn't the most human readable thing in the world */
		System.setProperty("joml.format", "false");

		Configurator.setRootLevel(Level.DEBUG);
		log.info("Output path " + OUTPUT.toAbsolutePath());
		log.debug("Local resources path: " + Generator.class.getResource("/"));

		Files.createDirectories(OUTPUT);
		for (Path p : OUTPUTS)
			Files.createDirectories(p);

		// new Generator().generateTestWorld();
		// if (true)
		// return;
		CommandLine cli = new CommandLine(new Generator());
		for (String s : args)
			cli.parseWithHandler(new CommandLine.RunLast(), s.split(";"));

		log.info("Done.");
	}

	/**
	 * Gradle uses its {@code processResources} task to copy all resources from the src folders to the classes folder. Since we are generating
	 * multiple resources at runtime, Gradle has no chance to copy them so we must do this manually every time some files changed that are
	 * needed in the future.
	 */
	public static void processResources() throws IOException {
		log.info("Updating resources");
		// TODO lazy copying will save a lot of time
		for (Path p : OUTPUTS)
			if (p != OUTPUT_INTERNAL_TEST)
				Files.walkFileTree(p, new CopyDirVisitor(p, Paths.get("./build/resources/main")));
		Files.walkFileTree(OUTPUT_INTERNAL_TEST, new CopyDirVisitor(OUTPUT_INTERNAL_TEST, Paths.get("./build/resources/test")));
	}

	/** Java still has no good way to copy directories ... */
	private static class CopyDirVisitor extends SimpleFileVisitor<Path> {
		private Path	sourceDir;
		private Path	targetDir;

		public CopyDirVisitor(Path sourceDir, Path targetDir) {
			this.sourceDir = sourceDir;
			this.targetDir = targetDir;
		}

		@Override
		public FileVisitResult visitFile(Path file,
				BasicFileAttributes attributes) throws IOException {
			Path targetFile = targetDir.resolve(sourceDir.relativize(file));
			if (!Files.exists(targetFile) || Files.getLastModifiedTime(file).compareTo(Files.getLastModifiedTime(targetFile)) > 0)
				Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult preVisitDirectory(Path dir,
				BasicFileAttributes attributes) throws IOException {
			Path newDir = targetDir.resolve(sourceDir.relativize(dir));
			Files.createDirectories(newDir);
			return FileVisitResult.CONTINUE;
		}
	}
}
