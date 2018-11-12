package de.piegames.blockmap.generate;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
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

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.joml.Vector2d;
import org.joml.Vector2i;

import com.flowpowered.nbt.regionfile.RegionFile;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import de.piegames.blockmap.BlockStateHelper;
import de.piegames.blockmap.BlockStateHelper.BlockStateHelperBlock;
import de.piegames.blockmap.BlockStateHelper.BlockStateHelperState;
import de.piegames.blockmap.color.BiomeColorMap;
import de.piegames.blockmap.color.BlockColorMap;
import de.piegames.blockmap.color.Color;
import de.piegames.blockmap.guistandalone.GuiMain;
import de.piegames.blockmap.guistandalone.RegionFolderProvider;
import de.piegames.blockmap.renderer.RegionRenderer;
import de.piegames.blockmap.renderer.RegionShader;
import de.piegames.blockmap.renderer.RenderSettings;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.WritableImage;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command
public class Generator {

	private static Log			log						= LogFactory.getLog(Generator.class);

	static final Path			OUTPUT					= Paths.get("./build/generated-resources");
	static final Path			OUTPUT_CORE				= OUTPUT.resolve("BlockMap-core/generated-resources-main");
	static final Path			OUTPUT_INTERNAL_MAIN	= OUTPUT.resolve("BlockMap-internal/generated-resources-main");
	static final Path			OUTPUT_INTERNAL_TEST	= OUTPUT.resolve("BlockMap-internal/generated-resources-test");
	static final Path			OUTPUT_STANDALONE		= OUTPUT.resolve("BlockMap-standalone/generated-resources-main");
	static final Path			OUTPUT_GUI				= OUTPUT.resolve("BlockMap-gui/generated-resources-main");
	static final Path			OUTPUT_OTHER			= OUTPUT.resolve("other");
	static final Path			OUTPUT_SCREENSHOTS		= Paths.get("../screenshots");
	private static final Path[]	OUTPUTS					= { OUTPUT_CORE, OUTPUT_INTERNAL_MAIN, OUTPUT_INTERNAL_TEST, OUTPUT_STANDALONE, OUTPUT_GUI,
			OUTPUT_OTHER, OUTPUT_SCREENSHOTS };

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
	public void generateTestWorld() throws IOException {
		log.info("Generating test world");
		// FileUtils.copyDirectory(new File(Generator.class), OUTPUT_INTERNAL_TEST.toFile());

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
		builder.append("\tMAP_TRUE(\"map\", \"true\"),");
		builder.append(System.getProperty("line.separator"));
		builder.append("\tMAP_FALSE(\"map\", \"false\");");
		// Load template file and replace original
		// String original = new String(Files.readAllBytes(Paths.get(URI.create(Generator.class.getResource("/BlockState.java").toString()))));
		// original = original.replace("$REPLACE", builder.toString());
		// Files.write(Paths.get("./src/main/java", "togos/minecraft/maprend/renderer", "BlockState.java"), original.getBytes());
		Files.write(OUTPUT_OTHER.resolve("BlockState.java"), builder.toString().getBytes());

		processResources();
	}

	@Command
	public void generateScreenshots() throws IOException {
		log.info("Generating screenshots");
		RenderSettings settings = new RenderSettings();
		settings.loadDefaultColors();
		RegionRenderer renderer = new RegionRenderer(settings);
		{ /* Color maps */
			settings.maxY = 50;
			BufferedImage img1 = generateScreenshot(renderer, settings, new Vector2i(-1, 1), BlockColorMap.InternalColorMap.CAVES);
			settings.maxY = 255;
			BufferedImage img2 = generateScreenshot(renderer, settings, new Vector2i(0, 1), BlockColorMap.InternalColorMap.NO_FOLIAGE);
			BufferedImage img3 = generateScreenshot(renderer, settings, new Vector2i(-1, 2), BlockColorMap.InternalColorMap.OCEAN_GROUND);
			BufferedImage img4 = generateScreenshot(renderer, settings, new Vector2i(0, 2), BlockColorMap.InternalColorMap.DEFAULT);
			BufferedImage img = new BufferedImage(1024, 1024, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = img.createGraphics();
			g.drawImage(img1, 0, 0, null);
			g.drawImage(img2, 512, 0, null);
			g.drawImage(img3, 0, 512, null);
			g.drawImage(img4, 512, 512, null);
			g.setFont(g.getFont().deriveFont(0, 32.0f));
			g.drawString("Caves", 0 + 32, 512 - 32);
			g.drawString("No foliage", 1024 - 32 - g.getFontMetrics().stringWidth("No foliage"), 512 - 32);
			g.drawString("Ocean ground", 0 + 32, 1024 - 32);
			g.drawString("Default", 1024 - 32 - g.getFontMetrics().stringWidth("Default"), 1024 - 32);
			g.dispose();
			try (OutputStream out = Files.newOutputStream(OUTPUT_SCREENSHOTS.resolve("screenshot-1.png"))) {
				ImageIO.write(img, "png", out);
			}
		}
		{ /* Shaders */
			BufferedImage img1 = generateScreenshot(renderer, settings, new Vector2i(-1, 1), BlockColorMap.InternalColorMap.DEFAULT);
			settings.shader = RegionShader.DefaultShader.RELIEF.getShader();
			settings.shader = RegionShader.DefaultShader.FLAT.getShader();
			BufferedImage img2 = generateScreenshot(renderer, settings, new Vector2i(0, 1), BlockColorMap.InternalColorMap.DEFAULT);
			settings.shader = RegionShader.DefaultShader.HEIGHTMAP.getShader();
			BufferedImage img3 = generateScreenshot(renderer, settings, new Vector2i(-1, 2), BlockColorMap.InternalColorMap.OCEAN_GROUND);
			settings.shader = RegionShader.DefaultShader.BIOMES.getShader();
			BufferedImage img4 = generateScreenshot(renderer, settings, new Vector2i(0, 2), BlockColorMap.InternalColorMap.DEFAULT);
			BufferedImage img = new BufferedImage(1024, 1024, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = img.createGraphics();
			g.drawImage(img1, 0, 0, null);
			g.drawImage(img2, 512, 0, null);
			g.drawImage(img3, 0, 512, null);
			g.drawImage(img4, 512, 512, null);
			g.setFont(g.getFont().deriveFont(0, 32.0f));
			g.drawString("Relief", 0 + 32, 512 - 32);
			g.drawString("Flat", 1024 - 32 - g.getFontMetrics().stringWidth("Flat"), 512 - 32);
			g.drawString("Heightmap", 0 + 32, 1024 - 32);
			g.drawString("Biomes", 1024 - 32 - g.getFontMetrics().stringWidth("Biomes"), 1024 - 32);
			g.dispose();
			try (OutputStream out = Files.newOutputStream(OUTPUT_SCREENSHOTS.resolve("screenshot-2.png"))) {
				ImageIO.write(img, "png", out);
			}
		}
		{ /* GUI */
			Thread th = new Thread(() -> GuiMain.main());
			th.start();
			while (GuiMain.instance == null)
				Thread.yield();
			Platform.runLater(() -> {
				GuiMain.instance.stage.setWidth(1280);
				GuiMain.instance.stage.setHeight(720);
				GuiMain.instance.stage.hide();
				GuiMain.instance.stage.show();
				GuiMain.instance.controller.load(
						RegionFolderProvider.byPath(
								Paths.get(URI.create(Generator.class.getResource("/BlockMapWorld/").toString()))));
				GuiMain.instance.controller.renderer.viewport.translationProperty.set(new Vector2d(512, -512));
			});
			while (GuiMain.instance.controller.renderer.getStatus().get().equals("No regions loaded"))
				Thread.yield();
			while (GuiMain.instance.controller.renderer.getProgress().get() < 1)
				Thread.yield();
			Platform.runLater(() -> {
				WritableImage img = GuiMain.instance.stage.getScene().snapshot(null);
				try (OutputStream out = Files.newOutputStream(OUTPUT_SCREENSHOTS.resolve("screenshot-3.png"))) {
					ImageIO.write(SwingFXUtils.fromFXImage(img, null), "png", out);
				} catch (IOException e) {
					log.error(e);
				}
				GuiMain.instance.controller.exit();
			});
			try {
				th.join();
			} catch (InterruptedException e) {
				log.error(e);
			}
		}

		processResources();
	}

	private static BufferedImage generateScreenshot(RegionRenderer renderer, RenderSettings settings, Vector2i toRender, BlockColorMap.InternalColorMap colors)
			throws IOException {
		try (RegionFile file = new RegionFile(Paths.get(URI.create(Generator.class.getResource("/BlockMapWorld/region/r." + toRender.x + "." + toRender.y
				+ ".mca")
				.toString())))) {
			settings.blockColors = colors.getColorMap();
			return renderer.render(toRender, file);
		}
	}

	public static void main(String[] args) throws Exception {
		Configurator.setRootLevel(Level.DEBUG);
		log.info("Output path " + OUTPUT.toAbsolutePath());
		log.debug("Local resources path: " + Generator.class.getResource("/"));

		Files.createDirectories(OUTPUT);
		for (Path p : OUTPUTS)
			Files.createDirectories(p);

		CommandLine cli = new CommandLine(new Generator());
		for (String s : args)
			cli.parseWithHandler(new CommandLine.RunLast(), new String[] { s });

		log.info("Done.");
	}

	/**
	 * Gradle uses its {@code processResources} task to copy all resources from the src folders to the classes folder. Since we are generating
	 * multiple resources at runtime, Gradle has no chance to copy them so we must do this manually every time some files changed that are
	 * needed in the future.
	 */
	public static void processResources() throws IOException {
		log.info("Updating resources");
		for (Path p : OUTPUTS)
			if (p != OUTPUT_INTERNAL_TEST)
				FileUtils.copyDirectory(p.toFile(), Paths.get("./build/resources/main").toFile());
		FileUtils.copyDirectory(OUTPUT_INTERNAL_TEST.toFile(), Paths.get("./build/resources/test").toFile());
	}
}
