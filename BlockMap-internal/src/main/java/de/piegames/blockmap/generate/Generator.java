package de.piegames.blockmap.generate;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.joml.Vector2i;

import de.piegames.blockmap.MinecraftBlocks;
import de.piegames.blockmap.MinecraftVersion;
import de.piegames.blockmap.color.BiomeColorMap;
import de.piegames.blockmap.color.BlockColorMap;
import de.piegames.blockmap.color.Color;
import de.piegames.blockmap.renderer.BlockState;
import de.piegames.mlg.MinecraftLandGenerator;
import de.piegames.mlg.Server;
import de.piegames.mlg.World;
import de.piegames.mlg.World.Dimension;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command
public class Generator {

	private static Log			log							= LogFactory.getLog(Generator.class);

	/*
	 * All output paths are listed here. They are split into three categories: main-resources, test-resources, other. The resources get copied
	 * to the runtime resources folder through #processResources(). Those are in OUTPUTS.
	 */

	static final Path			OUTPUT						= Paths.get("./build/generated-resources");
	static final Path			OUTPUT_CORE					= OUTPUT.resolve("BlockMap-core/generated-resources-main");
	static final Path			OUTPUT_INTERNAL_MAIN		= OUTPUT.resolve("BlockMap-internal/generated-resources-main");
	static final Path			OUTPUT_INTERNAL_TEST		= OUTPUT.resolve("BlockMap-internal/generated-resources-test");
	static final Path			OUTPUT_INTERNAL_CACHE		= OUTPUT.resolve("BlockMap-internal/generated-resources-cache");
	static final Path			OUTPUT_STANDALONE			= OUTPUT.resolve("BlockMap-cli/generated-resources-main");
	static final Path			OUTPUT_GUI					= OUTPUT.resolve("BlockMap-gui/generated-resources-main");
	static final Path			OUTPUT_SCREENSHOTS			= Paths.get("../screenshots");
	private static final Path[]	OUTPUTS					= { OUTPUT_CORE, OUTPUT_INTERNAL_MAIN, OUTPUT_INTERNAL_TEST, OUTPUT_STANDALONE, OUTPUT_GUI };

	@Command
	public void checkMinecraftVersions() throws Exception {
		log.info("Checking if all information in the `MinecraftVersion.java` are up to date");
		Downloader.checkMinecraftVersions();
	}

	@Command
	public void generateData() throws Exception {
		var versions = Arrays.asList(MinecraftVersion.values());
		Collections.reverse(versions); // Cause there is no better way to reverse arrays in Java -.-
		for (MinecraftVersion version : versions) {

			Downloader.downloadMinecraft(version);

			Path extractedDataDirectory = OUTPUT_INTERNAL_TEST.resolve("data-" + version.fileSuffix);

			{ /* Call the Minecraft data report tool */
				Path serverFile = OUTPUT_INTERNAL_CACHE.resolve("server-" + version.fileSuffix + ".jar");

				/* The hash of the current existing server.jar file */
				String serverHash;
				try {
					serverHash = Downloader.getHashSHA256(serverFile);
				} finally {
				}

				/* The hash of the server.jar at the time the data was generated. */
				String cachedServerHash = null;
				try {
					cachedServerHash = Files.readString(extractedDataDirectory.resolve("server-sha256"));
				} catch (RuntimeException | IOException e) {
				}

				/* If the cached hash does not exist or does not match the current one, re-generate the data. */
				if (cachedServerHash == null || !serverHash.equals(cachedServerHash)) {
					log.info("Extracting Minecraft data for version " + version);

					FileUtils.deleteDirectory(extractedDataDirectory.toFile());
					try (URLClassLoader loader = new URLClassLoader(
							new URL[] { serverFile.toUri().toURL() },
							Generator.class.getClassLoader()
					)) {
						Class<?> MinecraftMain;
						/* For Minecraft 1.18 and up, how things work changed */
						if (version.ordinal() >= MinecraftVersion.MC_1_18.ordinal()) {
							System.setProperty("bundlerMainClass", "net.minecraft.data.Main");
							MinecraftMain = Class.forName("net.minecraft.bundler.Main", true, loader);
						} else {
							MinecraftMain = Class.forName("net.minecraft.data.Main", true, loader);
						}
						Method main = MinecraftMain.getDeclaredMethod("main", String[].class);
						main.invoke(null, new Object[] { new String[] { "--reports", "--output=" + extractedDataDirectory } });
						
						/* The main method may fork, thus wait for the forked thread to exit */
						/* Also, who the fuck defined this API? */
						Thread[] threads = new Thread[100];
						Thread.currentThread().getThreadGroup().enumerate(threads);
						for (Thread thread : threads)
							if (thread != null && thread.getName().equals("ServerMain"))
								thread.join();
					}

					/* Write the hash last. Thus the presence of this file implies the existence of the data. */
					Files.writeString(extractedDataDirectory.resolve("server-sha256"), serverHash);
				} else {
					log.info("Extracted Minecraft data for version " + version +  " is up to date");
				}
			}

			log.info("Generating BlockState information for version " + version);

			MinecraftBlocks blocks = new MinecraftBlocks(extractedDataDirectory.resolve("reports/blocks.json"));

			BlockState states = blocks.generateStates();
			try (BufferedWriter writer = Files.newBufferedWriter(OUTPUT_CORE.resolve("block-states-" + version.fileSuffix + ".json"))) {
				BlockColorMap.GSON.toJson(states, writer);
				writer.flush();
			}

			Path minecraftJarfile = OUTPUT_INTERNAL_CACHE.resolve("client-" + version.fileSuffix + ".jar");

			log.info("Generating block colors for version " + version);

			for (Entry<String, BlockColorMap> map : ColorCompiler.compileBlockColors(minecraftJarfile,
					Paths.get(URI.create(Generator.class.getResource("/block-color-instructions-" + version.fileSuffix + ".json").toString())), blocks, states)
					.entrySet()) {
				Path outputPath = OUTPUT_CORE.resolve("block-colors-" + map.getKey() + "-" + version.fileSuffix + ".json");
				log.debug("Writing block-colors-" + map.getKey() + ".json to " + outputPath);
				try (BufferedWriter writer = Files.newBufferedWriter(outputPath)) {
					BlockColorMap.GSON.toJson(map.getValue(), writer);
					writer.flush();
				}
			}
		}

		{
			/* Until now, biomes never had any backwards-incompatible changes, so simple use latest version for all */
			log.info("Generating biome colors");

			Path minecraftJarfile = OUTPUT_INTERNAL_CACHE.resolve("client-" + MinecraftVersion.LATEST.fileSuffix + ".jar");
			BiomeColorMap map = ColorCompiler.compileBiomeColors(minecraftJarfile,
					Paths.get(URI.create(Generator.class.getResource("/biome-color-instructions.json").toString())),
					OUTPUT_INTERNAL_TEST.resolve("data-" + MinecraftVersion.LATEST.fileSuffix).resolve("reports/minecraft/worldgen/biome"));
			try (BufferedWriter writer = Files.newBufferedWriter(OUTPUT_CORE.resolve("biome-colors.json"))) {
				BlockColorMap.GSON.toJson(map, writer);
				writer.flush();
			}
		}

		{
			log.info("Generating heightmap colors");
			List<Color> colors = ColorCompiler.compileHeightMap(Paths.get(URI.create(Generator.class.getResource("/heightmap.png").toString())));

			try (BufferedWriter writer = Files.newBufferedWriter(OUTPUT_CORE.resolve("heightmap.json"))) {
				BlockColorMap.GSON.toJson(colors, writer);
				writer.flush();
			}
		}

		processResources();
	}
	
	@Command
	public void generateVersion(String version) throws Exception {
		log.info("Generating VersionProvider for version " + version);
		generateVersion(
				Paths.get("../BlockMap-cli/", "src/main/java", "de/piegames/blockmap/standalone", "VersionProvider.java"),
				version);
		generateVersion(
				Paths.get("../BlockMap-gui/", "src/main/java", "de/piegames/blockmap/gui", "VersionProvider.java"),
				version);
	}

	private void generateVersion(Path path, String version) throws IOException {
		List<String> file = Files.readAllLines(path);
		int line = 0;
		while (!file.get(line).contains("$REPLACE_START"))
			line++;
		line++;
		while (!file.get(line).contains("$REPLACE_END"))
			file.remove(line);
		file.add(line, "\tpublic static final String VERSION = \"" + version + "\";");
		Files.write(path, file);
	}

	@Command
	public void generateTestWorld() throws IOException, InterruptedException {
		log.info("Generating test world");

		Path worldPath = OUTPUT_INTERNAL_CACHE.resolve("BlockMapWorld");

		FileUtils.copyDirectory(new File(URI.create(Generator.class.getResource("/BlockMapWorld").toString())), worldPath
				.toFile());

		Server server = new Server(OUTPUT_INTERNAL_CACHE.resolve("server-" + MinecraftVersion.LATEST.fileSuffix + ".jar"), null);
		World world = server.initWorld(worldPath, true);

		int SIZE = 256;// 256;
		ArrayList<Vector2i> chunks = new ArrayList<>(SIZE * SIZE * 4);
		for (int z = -SIZE; z < SIZE; z++)
			for (int x = -SIZE; x < SIZE; x++)
				chunks.add(new Vector2i(x, z));
		MinecraftLandGenerator.forceloadChunks(server, world, chunks, Dimension.OVERWORLD, true, 1024, true);
		world.resetChanges();

		processResources();
	}

	@Command
	public void generateScreenshots() throws Exception {
		log.info("Generating screenshots");
		// TODO remove and place somewhere else
		Files.createDirectories(Generator.OUTPUT_SCREENSHOTS);
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
		Files.createDirectories(OUTPUT_INTERNAL_CACHE);
		Files.createDirectories(OUTPUT_SCREENSHOTS);
		for (Path p : OUTPUTS)
			Files.createDirectories(p);

		if (args.length == 0)
			args = new String[] { "generateData" };

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
