package de.piegames.blockmap;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedList;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.joml.Vector2i;
import org.junit.BeforeClass;
import org.junit.Test;

import com.flowpowered.nbt.regionfile.RegionFile;
import com.google.gson.stream.JsonReader;

import de.piegames.blockmap.color.BlockColorMap;
import de.piegames.blockmap.generate.ColorCompiler;
import de.piegames.blockmap.renderer.Block;
import de.piegames.blockmap.renderer.BlockState;
import de.piegames.blockmap.renderer.RegionRenderer;
import de.piegames.blockmap.renderer.RegionShader;
import de.piegames.blockmap.renderer.RenderSettings;

public class ColorCompilerTest {

	static Path			minecraftJarfile;
	static FileSystem	minecraftJar;

	@BeforeClass
	public static void testMinecraftJar() throws URISyntaxException, IOException {
		Configurator.setRootLevel(Level.DEBUG);

		assertNotNull("Minecraft jar missing. Please copy or link the 1.13.jar from the versions folder to ./src/test/resources/minecraft.jar",
				ColorCompilerTest.class.getResource("/client.jar"));
		minecraftJarfile = Paths.get(ColorCompilerTest.class.getResource("/client.jar").toURI());
		minecraftJar = FileSystems.newFileSystem(minecraftJarfile, null);
	}

	/** The debug world contains every single block and block state that exists in the game, so let's test it */
	@Test
	public void testDebugWorld() throws IOException, URISyntaxException, InterruptedException {
		RenderSettings settings = new RenderSettings();
		settings.shader = RegionShader.DefaultShader.FLAT.getShader();
		settings.blockColors = ColorCompiler.compileBlockColors(minecraftJarfile, Paths.get(getClass().getResource("/block-color-instructions.json").toURI()))
				.get("default");
		settings.biomeColors = ColorCompiler.compileBiomeColors(minecraftJarfile, Paths.get(getClass().getResource("/biome-color-instructions.json").toURI()));
		RegionRenderer renderer = new RegionRenderer(settings);
		assertNoMissing(renderer.render(new Vector2i(-1, -1), new RegionFile(Paths.get(getClass().getResource("/Debug/region/r.-1.-1.mca").toURI()))));
		assertNoMissing(renderer.render(new Vector2i(-1, 0), new RegionFile(Paths.get(getClass().getResource("/Debug/region/r.-1.0.mca").toURI()))));
		assertNoMissing(renderer.render(new Vector2i(-1, 1), new RegionFile(Paths.get(getClass().getResource("/Debug/region/r.-1.1.mca").toURI()))));
		assertNoMissing(renderer.render(new Vector2i(0, -1), new RegionFile(Paths.get(getClass().getResource("/Debug/region/r.0.-1.mca").toURI()))));
		assertNoMissing(renderer.render(new Vector2i(0, 0), new RegionFile(Paths.get(getClass().getResource("/Debug/region/r.0.0.mca").toURI()))));
		assertNoMissing(renderer.render(new Vector2i(0, 1), new RegionFile(Paths.get(getClass().getResource("/Debug/region/r.0.1.mca").toURI()))));
		assertNoMissing(renderer.render(new Vector2i(1, -1), new RegionFile(Paths.get(getClass().getResource("/Debug/region/r.1.-1.mca").toURI()))));
		assertNoMissing(renderer.render(new Vector2i(1, 0), new RegionFile(Paths.get(getClass().getResource("/Debug/region/r.1.0.mca").toURI()))));
		assertNoMissing(renderer.render(new Vector2i(1, 1), new RegionFile(Paths.get(getClass().getResource("/Debug/region/r.1.1.mca").toURI()))));
	}

	/**
	 * Use Minecraft generated data to test if every single block state existing is present here. Should do exactly the same thing as
	 * {@link #testDebugWorld()}, but just in case
	 */
	@Test
	public void testExisting() throws IOException, URISyntaxException {
		BlockColorMap map = ColorCompiler.compileBlockColors(minecraftJarfile, Paths.get(getClass().getResource("/block-color-instructions.json").toURI()))
				.get("default");

		/*
		 * Collect all missing blocks and fail at the end so that if a lot things are missing it isn't required to re-run the test after each
		 * change.
		 */
		Collection<Block> missing = new LinkedList<>();
		try (JsonReader reader = new JsonReader(Files.newBufferedReader(Paths.get(getClass().getResource("/data/reports/blocks.json").toURI())))) {
			reader.beginObject();
			while (reader.hasNext()) {
				String blockName = reader.nextName();
				reader.beginObject();
				while (reader.hasNext()) {
					String name = reader.nextName();
					if (name.equals("states")) {
						reader.beginArray();
						while (reader.hasNext()) {
							reader.beginObject();
							while (reader.hasNext()) {
								if (reader.nextName().equals("properties")) {
									reader.beginObject();

									EnumSet<BlockState> properties = EnumSet.noneOf(BlockState.class);
									while (reader.hasNext())
										properties.add(BlockState.valueOf(reader.nextName(), reader.nextString()));
									Block block = new Block(blockName, properties);
									if (!map.hasBlockColor(block))
										missing.add(block);

									reader.endObject();
								} else
									reader.skipValue();
							}
							reader.endObject();

						}
						reader.endArray();
					} else
						reader.skipValue();
				}
				reader.endObject();
			}
			reader.endObject();
		}
		assertTrue("Blocks " + missing.toString() + " should exist in color map", missing.isEmpty());
	}

	/** Assert there are no "missing color" pixels in that image */
	private static void assertNoMissing(BufferedImage image) {
		// TODO tolerance
		for (int x = 0; x < image.getWidth(); x++)
			for (int y = 0; y < image.getHeight(); y++)
				assertTrue(0xFFFF00FF != image.getRGB(x, y));
	}
}
