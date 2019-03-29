package de.piegames.blockmap.standalone;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.joml.Vector2ic;

import de.piegames.blockmap.RegionFolder.CachedRegionFolder;
import de.piegames.blockmap.RegionFolder.WorldRegionFolder;
import de.piegames.blockmap.color.BiomeColorMap;
import de.piegames.blockmap.color.BlockColorMap;
import de.piegames.blockmap.color.BlockColorMap.InternalColorMap;
import de.piegames.blockmap.renderer.RegionRenderer;
import de.piegames.blockmap.renderer.RegionShader.DefaultShader;
import de.piegames.blockmap.renderer.RenderSettings;
import de.piegames.blockmap.standalone.CommandLineMain.CommandRender;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Visibility;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.RunLast;

@Command(name = "blockmap",
		versionProvider = VersionProvider.class,
		footer = "To access the GUI, omit the [COMMAND].",
		subcommands = { CommandRender.class, HelpCommand.class })
public class CommandLineMain implements Runnable {

	private static Log	log	= LogFactory.getLog(RegionRenderer.class);

	@Option(names = { "-V", "--version" },
			versionHelp = true,
			description = "Print version information and exit.")
	boolean				versionRequested;

	@Option(names = { "--verbose", "-v" }, description = "Be chatty")
	boolean				verbose;

	@Command(name = "render",
			sortOptions = false,
			description = "Render a folder containing region files to another folder through the command line interface",
			footer = "Please don't forget that you can use global options too, which can be accessed through `BlockMap help`."
					+ " These have to be put before the render command.",
			subcommands = { CommandSaveRendered.class, HelpCommand.class })
	public static class CommandRender implements Callable<CachedRegionFolder> {

		@ParentCommand
		private CommandLineMain		main;

		@Option(names = { "--output", "-o" },
				description = "The location of the output images. Must not be a file. Non-existant folders will be created.",
				paramLabel = "<FOLDER>",
				defaultValue = "./",
				showDefaultValue = Visibility.ALWAYS)
		private Path				output;
		@Parameters(index = "0",
				paramLabel = "INPUT",
				description = "Path to the world data. Normally, this should point to a 'region/' of a world.")
		private Path				input;
		@Option(names = { "-c", "--color-map" },
				paramLabel = "{DEFAULT|CAVES|NO_FOLIAGE|OCEAN_GROUND}",
				description = "Load a built-in color map.",
				defaultValue = "DEFAULT")
		private InternalColorMap	colorMap;
		@Option(names = { "-s", "--shader" },
				paramLabel = "{FLAT|RELIEF|BIOMES|HEIGHTMAP}",
				description = "The height shading to use in post processing.",
				showDefaultValue = Visibility.ALWAYS,
				defaultValue = "RELIEF")
		private DefaultShader		shader;
		@Option(names = "--custom-color-map", description = "Load a custom color map from the specified file. Overrides --color-map.")
		private Path				customColorMap;
		@Option(names = "--custom-biome-map", description = "Load a custom biome color map from the specified file.")
		private Path				customBiomeMap;

		@Option(names = { "--min-Y", "--min-height" }, description = "Don't draw blocks lower than this height.", defaultValue = "0")
		private int					minY;
		@Option(names = { "--max-Y", "--max-height" }, description = "Don't draw blocks higher than this height.", defaultValue = "255")
		private int					maxY;
		@Option(names = "--min-X", description = "Don't draw blocks to the east of this coordinate.", defaultValue = "-2147483648")
		private int					minX;
		@Option(names = "--max-X", description = "Don't draw blocks to the west of this coordinate.", defaultValue = "2147483647")
		private int					maxX;
		@Option(names = "--min-Z", description = "Don't draw blocks to the north of this coordinate.", defaultValue = "-2147483648")
		private int					minZ;
		@Option(names = "--max-Z", description = "Don't draw blocks to the south of this coordinate.", defaultValue = "2147483647")
		private int					maxZ;
		@Option(names = { "-l", "--lazy" },
				description = "Don't render region files if there is already an up to date. This saves time when rendering the same world regularly with the same settings.")
		private boolean				lazy;

		@Option(names = "--create-tile-html",
				description = "Generate a tiles.html in the output directory that will show all rendered images ona mapin your browsed.")
		private boolean				createHtml;
		@Option(names = "--create-big-image",
				description = "Merge all rendered images into a single file. May require a lot of RAM.")
		private boolean				createBigPic;

		@Override
		public CachedRegionFolder call() {
			main.runAll();
			RenderSettings settings = new RenderSettings();
			settings.minX = minX;
			settings.maxX = maxX;
			settings.minY = minY;
			settings.maxY = maxY;
			settings.minZ = minZ;
			settings.maxZ = maxZ;
			if (customColorMap == null)
				settings.blockColors = colorMap.getColorMap();
			else
				try (Reader r = Files.newBufferedReader(customColorMap)) {
					settings.blockColors = BlockColorMap.load(r);
				} catch (IOException e) {
					log.error("Could not load custom block color map", e);
					return null;
				}
			if (customBiomeMap == null)
				settings.biomeColors = BiomeColorMap.loadDefault();
			else
				try (Reader r = Files.newBufferedReader(customBiomeMap)) {
					settings.biomeColors = BiomeColorMap.load(r);
				} catch (IOException e) {
					log.error("Could not load custom block color map", e);
					return null;
				}
			settings.shader = shader.getShader();

			RegionRenderer renderer = new RegionRenderer(settings);
			log.debug("Input " + input.normalize().toAbsolutePath());
			log.debug("Output: " + output.normalize().toAbsolutePath());
			WorldRegionFolder world;
			try {
				world = WorldRegionFolder.load(input, renderer);
			} catch (IOException e) {
				log.error("Could not load region folder", e);
				return null;
			}
			CachedRegionFolder cached = new CachedRegionFolder(world, lazy, output);

			for (Vector2ic pos : world.listRegions()) {
				if (!PostProcessing.inBounds(pos.x(), settings.minX, settings.maxX)
						|| !PostProcessing.inBounds(pos.y(), settings.minZ, settings.maxZ))
					continue;
				try {
					cached.render(pos);
				} catch (IOException e) {
					log.error("Could not render region file", e);
				}
			}
			if (createBigPic)
				PostProcessing.createBigImage(cached.save(), output, settings);
			if (createHtml)
				PostProcessing.createTileHtml(cached.save(), output, settings);
			return cached;
		}

	}

	@Command(name = "save",
			description = "Save the rendering information to a file for later use. If the file already exists, the rendering's data will be appended, preserving the existing ones. "
					+ "The file is in json format and contains the paths of the rendered images, along with additional data.")
	public static class CommandSaveRendered implements Runnable {
		@ParentCommand
		CommandRender	parent;

		@Option(names = "--world-name",
				required = true,
				description = "A saved rendering can contain multiple rendered worlds, no matter if they are of the same world but with different settings or multiple actually distinct "
						+ "worlds. This name is used as identifier to distinguish between them. It is recommended to encode useful information like the world's name or distinct render "
						+ "settings into it.")
		private String	name;
		@Option(names = "--file",
				description = "The path of the file to write. If relative, it will be resolved against the output directory.",
				defaultValue = "./rendered.json",
				showDefaultValue = Visibility.ALWAYS)
		private Path	file;
		@Option(names = "--absolute",
				description = "By default, all paths are relativized to this file so moving the whole folder won't break the paths. This is useful if you want to put this file on a server, "
						+ "but if you want to be able to move this file and keep all images in place, use this option instead.")
		private boolean	absolute;

		@Override
		public void run() {
			CachedRegionFolder rendered = parent.call();
			if (rendered != null)
				try {
					Path out = file;
					if (out == null)
						out = Paths.get("rendered.json");
					if (!out.isAbsolute())
						out = parent.output.resolve(out);
					log.info("Saving rendering information to " + out.normalize());
					rendered.save(out, name, !absolute);
				} catch (IOException e) {
					log.error(e);
				}
			else
				log.warn("Could not save the world's information to a file since it didn't get rendered correctly");
		}
	}

	public void runAll() {
		if (verbose) {
			Configurator.setRootLevel(Level.DEBUG);
		}
	}

	@Override
	public void run() {
		runAll();
		/*
		 * Using generics will make sure the class is only loaded now and not before. Loading this class may cause to load JavaFX classes which
		 * might not be on the class path with some java installations. This way, even users without JavaFX can still use the CLI
		 */
		try {
			Class.forName("de.piegames.blockmap.guistandalone.GuiMain").getMethod("main", String[].class).invoke(null, (Object) new String[0]);
		} catch (NoClassDefFoundError e) {
			log.fatal("Could not load GUI classes. Please make sure you have JavaFX loaded and on your class path. "
					+ "Alternatively, use Java 8 which includes JavaFX. You can use the BlockMap CLI anyway with `BlockMap help` or `BlockMap render`.",
					e);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException
				| ClassNotFoundException e) {
			log.fatal("Could not load GUI main class", e);
		}
	}

	public static void main(String... args) {
		/* Without this, JOML will print vectors out in scientific notation which isn't the most human readable thing in the world */
		System.setProperty("joml.format", "false");

		CommandLine cli = new CommandLine(new CommandLineMain());
		cli.parseWithHandler(new RunLast(), args);
	}

}
