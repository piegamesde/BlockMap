package de.piegames.blockmap.standalone;

import java.io.IOException;
import java.io.Writer;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.joml.Vector2ic;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import de.piegames.blockmap.MinecraftDimension;
import de.piegames.blockmap.color.BiomeColorMap;
import de.piegames.blockmap.color.BlockColorMap.InternalColorMap;
import de.piegames.blockmap.renderer.RegionRenderer;
import de.piegames.blockmap.renderer.RegionShader.DefaultShader;
import de.piegames.blockmap.renderer.RenderSettings;
import de.piegames.blockmap.standalone.CommandLineMain.CommandRender;
import de.piegames.blockmap.standalone.CommandLineMain.CommandServer;
import de.piegames.blockmap.world.LevelMetadata;
import de.piegames.blockmap.world.RegionFolder.CachedRegionFolder;
import de.piegames.blockmap.world.RegionFolder.WorldRegionFolder;
import de.piegames.blockmap.world.ServerMetadata;
import io.gsonfire.GsonFireBuilder;
import net.dongliu.gson.GsonJava8TypeAdapterFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Visibility;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Spec;

@Command(name = "blockmap",
		versionProvider = VersionProvider.class,
		synopsisSubcommandLabel = "COMMAND",
		subcommands = { CommandRender.class, CommandServer.class },
		footerHeading = "%n",
		footer = "This is the command line interface of blockmap. To access the GUI (if installed), run `blockmap-gui`.")
public class CommandLineMain implements Callable<Integer> {

	private static Log			log		= null;

	public static final Gson	GSON	= new GsonFireBuilder()
			.enableExposeMethodParam()
			.registerPostProcessor(ServerSettings.class, new DeserializeNullChecker())
			.registerPostProcessor(ServerSettings.RegionFolderSettings.class, new DeserializeNullChecker())
			.createGsonBuilder()
			.registerTypeAdapterFactory(new GsonJava8TypeAdapterFactory())
			.registerTypeHierarchyAdapter(Path.class, new TypeAdapter<Path>() {

													@Override
													public void write(JsonWriter out, Path value) throws IOException {
														out.value(value.toString());
													}

													@Override
													public Path read(JsonReader in) throws IOException {
														return Paths.get(in.nextString());
													}
												})
			.disableHtmlEscaping()
			.setPrettyPrinting()
			.create();

	/** Lazily initialize the logger to avoid loading Log4j too early (startup performance). */
	private static void checkLogger() {
		if (log == null)
			log = LogFactory.getLog(CommandLineMain.class);
	}

	@Option(names = { "-V", "--version" },
			versionHelp = true,
			description = "Print version information and exit.")
	boolean		versionRequested;
	@Option(names = { "-h", "--help" }, usageHelp = true, description = "Print this help message and exit")
	boolean		usageHelpRequested;

	@Option(names = { "--verbose", "-v" }, description = "Be chatty")
	boolean		verbose;

	@Spec
	CommandSpec	spec;

	public void runAll() {
		if (verbose) {
			Configurator.setRootLevel(Level.DEBUG);
		}
	}

	@Override
	public Integer call() {
		runAll();
		throw new ParameterException(spec.commandLine(), "Missing required subcommand");
	}

	@Command(name = "render",
			sortOptions = false,
			description = "Render a folder containing region files to another folder through the command line interface",
			footerHeading = "%n",
			footer = "Please don't forget that you can use global options too, which can be listed through `blockmap --help`."
					+ " These have to be put before the render command.")
	public static class CommandRender implements Callable<Integer> {

		@ParentCommand
		private CommandLineMain		main;
		@Option(names = { "-h", "--help" }, usageHelp = true, description = "Print this help message and exit")
		boolean						usageHelpRequested;

		@Option(names = { "--output", "-o" },
				description = "The location of the output images. Must not be a file. Non-existant folders will be created.",
				paramLabel = "<FOLDER>",
				defaultValue = "./",
				showDefaultValue = Visibility.ALWAYS)
		private Path				output;
		@Parameters(index = "0",
				paramLabel = "INPUT",
				description = "Path to the world data. This should be a folder containing a level.dat. A path to the level.dat itself is valid too.")
		private Path				input;
		@Option(names = { "-c", "--color-map" },
				paramLabel = "{DEFAULT|CAVES|NO_FOLIAGE|OCEAN_GROUND|RAILS}",
				description = "Load a built-in color map.",
				defaultValue = "DEFAULT")
		private InternalColorMap	colorMap;
		@Option(names = { "-s", "--shader" },
				paramLabel = "{FLAT|RELIEF|BIOMES|HEIGHTMAP}",
				description = "The height shading to use in post processing.",
				showDefaultValue = Visibility.ALWAYS,
				defaultValue = "RELIEF")
		private DefaultShader		shader;
		@Option(names = { "-d", "--dim", "--dimension" },
				paramLabel = "{OVERWORLD|NETHER|END}",
				defaultValue = "OVERWORLD",
				showDefaultValue = Visibility.ALWAYS,
				description = "The dimension of the world to render.")
		private MinecraftDimension	dimension;

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
		/**
		 * Lazy is the default now. This option will thus be ignored.
		 * 
		 * @see CommandLineMain#force
		 */
		@Option(names = { "-l", "--lazy" },
				description = "Don't render region files if there is already an up to date. This saves time when rendering the same world regularly with the same settings.",
				hidden = true)
		@Deprecated
		private boolean				lazy;
		@Option(names = { "-f", "--force" },
				description = "Re-render region files even if they are up to date (based on their timestamp)")
		private boolean				force;
		@Option(names = { "-p", "--pins" }, description = "Load pin data from the world. This requires the use of the --dimension option")
		private boolean				pins;

		@Option(names = "--create-tile-html",
				description = "Generate a tiles.html in the output directory that will show all rendered images ona mapin your browsed.")
		private boolean				createHtml;
		@Option(names = "--create-big-image",
				description = "Merge all rendered images into a single file. May require a lot of RAM.")
		private boolean				createBigPic;

		@Override
		public Integer call() {
			main.runAll();
			checkLogger();

			/* Initialize settings */

			RenderSettings settings = new RenderSettings();
			settings.minX = minX;
			settings.maxX = maxX;
			settings.minY = minY;
			settings.maxY = maxY;
			settings.minZ = minZ;
			settings.maxZ = maxZ;
			settings.blockColors = colorMap.getColorMap();
			settings.biomeColors = BiomeColorMap.loadDefault();
			settings.regionShader = shader.getShader();

			RegionRenderer renderer = new RegionRenderer(settings);
			Path input = this.input;
			if (Files.isDirectory(input)) {
				if (!Files.exists(input.resolve("level.dat")))
					/* Don't exit, this is fine as long as the region folders are present */
					log.warn("World folders normally contain a file called `level.dat`");
			} else {
				if (input.getFileName().toString().equals("level.dat"))
					input = input.getParent();
				else {
					log.error("Input path must either point to a folder or to the `level.dat`");
					return 2;
				}
			}
			Path inputRegion = input.resolve(dimension.getRegionPath());
			log.debug("Input: " + input.normalize().toAbsolutePath());
			log.debug("Output: " + output.normalize().toAbsolutePath());
			if (!Files.exists(inputRegion)) {
				log.error("Specified region folder does not exist");
				return 2;
			}
			if (!Files.isDirectory(inputRegion)) {
				log.error("Specified region folder is not a directory");
				return 2;
			}
			WorldRegionFolder world;
			CachedRegionFolder cached;
			try {
				world = WorldRegionFolder.load(inputRegion, renderer);
				cached = CachedRegionFolder.create(world, !force, output);
			} catch (IOException e) {
				log.error("Could not load region folder", e);
				return 1;
			}

			/* Actual rendering */

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

			/* Post-processing, saving */

			if (pins) {
				world.setPins(LevelMetadata.loadFromWorld(input, dimension));
			}

			try {
				cached.save();
			} catch (IOException e) {
				log.error("Could not save the rendered world", e);
				return 1;
			}

			if (createBigPic)
				return PostProcessing.createBigImage(cached, output, settings);
			if (createHtml)
				return PostProcessing.createTileHtml(cached, output, settings);
			return 0;
		}
	}

	@Command(name = "render-many",
			sortOptions = false,
			description = "Render multiple worlds using a configuration file for usage in servers",
			footerHeading = "%n",
			footer = "Please don't forget that you can use global options too, which can be listed through `blockmap --help`."
					+ " These have to be put before the render command.")
	/* TODO configuration file man page / help */
	public static class CommandServer implements Callable<Integer> {
		@ParentCommand
		private CommandLineMain	main;

		@Parameters(index = "0",
				paramLabel = "CONFIG",
				description = "Path to the config.json")
		private Path			input;

		@Option(names = { "--server-name" })
		private String			name;
		@Option(names = { "--server-description" })
		private String			description;
		@Option(names = { "--server-address" })
		private String			ipAddress;
		@Option(names = { "--server-icon" })
		private String			iconLocation;
		@Option(names = { "--online-players" }, description = "The UUIDs of all players to be shown as 'online'")
		private Set<String> online = Collections.emptySet();
		@Option(names = { "--max-players" }, description = "The number of total slots on the servers")
		private int maxPlayers;

		@Override
		public Integer call() {
			main.runAll();
			checkLogger();

			ServerSettings settings;
			try {
				/* TODO add more error handling regarding missing values (Required values will simply throw a NullPointerException) */
				settings = GSON.fromJson(new String(Files.readAllBytes(input.toAbsolutePath())), ServerSettings.class);
			} catch (JsonParseException | IOException e) {
				log.error("Could not parse the settings file", e);
				return 2;
			}

			ServerMetadata metadata = new ServerMetadata(name, description, ipAddress, iconLocation, online, maxPlayers);
			metadata.levels = new ArrayList<>(settings.worlds.length);

			/* Render all worlds */
			for (ServerSettings.RegionFolderSettings folderSettings : settings.worlds) {
				log.info("Rendering world " + folderSettings.name);
				metadata.levels.add(new ServerMetadata.ServerLevel(folderSettings.name,
						/* https://stackoverflow.com/questions/4737841/urlencoder-not-able-to-translate-space-character TODO use Guava */
						URLEncoder.encode(folderSettings.name, Charset.defaultCharset()).replace("+", "%20") + "/rendered.json.gz"));

				RegionRenderer renderer = new RegionRenderer(folderSettings.renderSettings);

				Path input = folderSettings.inputDir;
				if (Files.isDirectory(input)) {
					if (!Files.exists(input.resolve("level.dat")))
						/* Don't exit, this is fine as long as the region folders are present */
						log.warn("World folders normally contain a file called `level.dat`");
				} else {
					if (input.getFileName().toString().equals("level.dat"))
						input = input.getParent();
					else {
						log.error("Input path must either point to a folder or to the `level.dat`");
						return 2;
					}
				}
				Path inputRegion = input.resolve(folderSettings.dimension.getRegionPath());
				if (!Files.exists(inputRegion)) {
					log.error("Specified region folder does not exist");
					return 2;
				}
				if (!Files.isDirectory(inputRegion)) {
					log.error("Specified region folder is not a directory");
					return 2;
				}
				WorldRegionFolder world;
				CachedRegionFolder cached;
				try {
					world = WorldRegionFolder.load(inputRegion, renderer);
					cached = CachedRegionFolder.create(world, !folderSettings.force, settings.outputDir.resolve(folderSettings.name));
				} catch (IOException e) {
					log.error("Could not load region folder", e);
					return 1;
				}

				/* Actual rendering */

				for (Vector2ic pos : world.listRegions()) {
					if (!PostProcessing.inBounds(pos.x(), folderSettings.renderSettings.minX, folderSettings.renderSettings.maxX)
							|| !PostProcessing.inBounds(pos.y(), folderSettings.renderSettings.minZ, folderSettings.renderSettings.maxZ))
						continue;
					try {
						cached.render(pos);
					} catch (IOException e) {
						log.error("Could not render region file", e);
					}
				}

				/* Post-processing, saving */

				var levelMetadata = LevelMetadata.loadFromWorld(input, folderSettings.dimension);
				if (settings.hideOfflinePlayers)
					levelMetadata.getPlayers().ifPresent(
							players -> players.removeIf(
									/*
									 * Remove all player pins that have a UUID but are not marked as online. Players without UUID won't
									 * be removed
									 */
									player -> !player.getUUID().map(online::contains).orElse(true)));
				world.setPins(levelMetadata);

				try {
					cached.save();
				} catch (IOException e) {
					log.error("Could not save the rendered world", e);
					return 1;
				}
			}

			/* Save the index file */
			// TODO sanitize user input
			try (Writer writer = Files.newBufferedWriter(settings.outputDir.resolve("index.json"));) {
				GSON.toJson(metadata, ServerMetadata.class, writer);
			} catch (IOException e) {
				log.error("Could not save the index file");
				return 1;
			}

			return 0;
		}
	}

	/** Separate method for testing the exit code without quitting the application */
	public static int mainWithoutQuit(String... args) {
		/* Without this, JOML will print vectors out in scientific notation which isn't the most human readable thing in the world */
		System.setProperty("joml.format", "false");
		return new CommandLine(new CommandLineMain()).execute(args);
	}

	public static void main(String... args) {
		System.exit(mainWithoutQuit(args));
	}
}
