package de.piegames.blockmap.standalone;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;

import com.flowpowered.nbt.regionfile.RegionFile;

import de.piegames.blockmap.Region;
import de.piegames.blockmap.RegionFolder;
import de.piegames.blockmap.renderer.RegionRenderer;
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

@Command(name = "blockmap", subcommands = { CommandRender.class, HelpCommand.class })
public class CommandLineMain implements Runnable {

	private static Log	log	= LogFactory.getLog(RegionRenderer.class);

	@Option(names = { "--verbose", "-v" }, description = "Be chatty")
	boolean				verbose;

	@Command(name = "render")
	public static class CommandRender implements Runnable {

		@ParentCommand
		private CommandLineMain	main;

		@Option(names = { "--output", "-o" },
				description = "The location of the output images. Must not be a file. Non-existant folders will be created.",
				paramLabel = "FOLDER",
				defaultValue = "./",
				showDefaultValue = Visibility.ALWAYS)
		private Path			output;
		@Parameters(index = "0",
				paramLabel = "INPUT",
				description = "Path to the world data. Normally, this should point to a 'region/' of a world.")
		private Path			input;
		@Option(names = "--color-map", description = "Load a custom color map from the specified file")
		private Path			colorMap;
		@Option(names = "--biome-map", description = "Load a custom biome color map from the specified file")
		private Path			biomeMap;

		@Option(names = { "--min-Y", "--min-height" }, description = "Don't draw blocks lower than this height", defaultValue = "0")
		private int				minY;
		@Option(names = { "--max-Y", "--max-height" }, description = "Don't draw blocks higher than this height", defaultValue = "255")
		private int				maxY;
		@Option(names = "--min-X", description = "Don't draw blocks to the east of this coordinate", defaultValue = "-2147483648")
		private int				minX;
		@Option(names = "--max-X", description = "Don't draw blocks to the west of this coordinate", defaultValue = "2147483647")
		private int				maxX;
		@Option(names = "--min-Z", description = "Don't draw blocks to the north of this coordinate", defaultValue = "-2147483648")
		private int				minZ;
		@Option(names = "--max-Z", description = "Don't draw blocks to the south of this coordinate", defaultValue = "2147483647")
		private int				maxZ;

		@Option(names = "--create-tile-html",
				description = "Generate a tiles.html in the output directory that will show all rendered images ona mapin your browsed")
		private boolean			createHtml;
		@Option(names = "--create-big-image", description = "Merge all rendered images into a single file. May require a lot of RAM")
		private boolean			createBigPic;

		@Override
		public void run() {
			if (main.verbose) {
				Configurator.setRootLevel(Level.DEBUG);
			}
			RenderSettings settings = new RenderSettings();
			settings.loadDefaultColors();
			RegionRenderer renderer = new RegionRenderer(settings);
			log.debug("Input " + input.toAbsolutePath());
			log.debug("Output: " + output.toAbsolutePath());
			RegionFolder world = RegionFolder.load(input);
			for (Region r : world.regions.values()) {
				try {
					RegionFile rf = new RegionFile(r.path);
					BufferedImage b = renderer.render(r.position, rf);
					r.renderedPath = output.resolve(r.path.getFileName().toString().replace(".mca", ".png"));
					log.debug("Saving image to " + r.renderedPath.toAbsolutePath());
					ImageIO.write(b, "png", Files.newOutputStream(r.renderedPath));
				} catch (IOException e) {
					log.error("Could not render region file", e);
				}
			}
			if (createBigPic)
				PostProcessing.createBigImage(world, output, settings);
			if (createHtml)
				PostProcessing.createTileHtml(world, output, settings);
		}
	}

	@Override
	public void run() {
		if (verbose) {
			Configurator.setRootLevel(Level.DEBUG);
		}
		/*
		 * Using generics will make sure the class is only loaded now and not before. Loading this class may cause to load JavaFX classes which
		 * might not be on the class path with some java installations. This way, even users without JavaFX can still use the CLI
		 */
		try {
			Class.forName("de.piegames.blockmap.guistandalone.GuiMain").getMethod("main2").invoke(null);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException
				| ClassNotFoundException e) {
			log.fatal("Could not find GUI main class", e);
		}
	}

	public static void main(String... args) {
		CommandLine cli = new CommandLine(new CommandLineMain());
		cli.parseWithHandler(new RunLast(), args);
	}
}
