package togos.minecraft.maprend;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;

import org.joml.Vector2i;
import org.junit.Before;
import org.junit.Test;

import com.flowpowered.nbt.regionfile.RegionFile;
import com.google.gson.stream.JsonReader;

import togos.minecraft.maprend.color.BlockColorMap;
import togos.minecraft.maprend.generate.ColorCompiler;
import togos.minecraft.maprend.renderer.Block;
import togos.minecraft.maprend.renderer.BlockState;
import togos.minecraft.maprend.renderer.RegionRenderer;
import togos.minecraft.maprend.renderer.RenderSettings;

public class ColorCompilerTest {

	Path minecraftJarfile;
	FileSystem minecraftJar;

	@Before
	public void testMinecraftJar() throws URISyntaxException, IOException {
		assertNotNull("Minecraft jar missing. Please copy or link the 1.13.jar from the versions folder to ./src/test/resources/minecraft.jar",
				getClass().getResource("/minecraft.jar"));
		minecraftJarfile = Paths.get(getClass().getResource("/minecraft.jar").toURI());
		minecraftJar = FileSystems.newFileSystem(minecraftJarfile, null);
	}

	/*
	 * This test is not accurate enough since the blockstate data is missing some important block states liked waterlogged and powered because
	 * those are handled by the game and not by the resource pack
	 */
	// /**
	// * Go through all files of {@code /assets/minecraft/blockstates} and test if they are present in the color file
	// *
	// * @throws IOException
	// * @throws URISyntaxException
	// */
	// @Test
	// public void testAllResources() throws IOException, URISyntaxException {
	// BlockColorMap map = ColorCompiler.compileBlockColors(minecraftJarfile,
	// Paths.get(getClass().getResource("/block-color-instructions.json").toURI()));
	//
	// try (DirectoryStream<Path> stream = Files.newDirectoryStream(minecraftJar.getPath("assets/minecraft/blockstates"))) {
	// for (Path p : stream) {
	// String blockName = p.getFileName().toString().split("\\.")[0];
	// System.out.println("Testing block " + blockName);
	// blockName = "minecraft:" + blockName;
	// try (JsonReader reader = new JsonReader(Files.newBufferedReader(p))) {
	// reader.beginObject();
	// String type = reader.nextName();
	// if (type.equals("variants")) {
	// reader.beginObject();
	// while (reader.hasNext()) {
	// for (Block block : Block.byCompactForm(blockName + "," + reader.nextName())) {
	// boolean b = map.hasBlockColor(block);
	// if (!b)
	// b = map.hasBlockColor(Block.block+",waterlogged=false");
	// if (!b)
	// b = map.hasBlockColor(block)
	// assertTrue("Block " + block + " should exist in color map", map.hasBlockColor(block));
	// }
	// reader.skipValue();
	// }
	// reader.endObject();
	// } else if (type.equals("multipart")) {
	// reader.skipValue();
	// // TODO
	//
	// // reader.beginArray();
	// // reader.endArray();
	// } else {
	// throw new AssertionError("Type '" + type + "' is not valid");
	// }
	//
	// reader.endObject();
	// }
	// }
	// }
	// }

	/** The debug world contains every single block and block state that exists in the game, so lets test it */
	@Test
	public void testDebugWorld() throws IOException, URISyntaxException, InterruptedException {
		// TODO make sure no pixel has the "missing" color
		RegionRenderer renderer = new RegionRenderer(new RenderSettings());
		renderer.render(new Vector2i(-1, -1), new RegionFile(Paths.get(getClass().getResource("/Debug/region/r.-1.-1.mca").toURI())));
		renderer.render(new Vector2i(-1, 0), new RegionFile(Paths.get(getClass().getResource("/Debug/region/r.-1.0.mca").toURI())));
		renderer.render(new Vector2i(-1, 1), new RegionFile(Paths.get(getClass().getResource("/Debug/region/r.-1.1.mca").toURI())));
		renderer.render(new Vector2i(0, -1), new RegionFile(Paths.get(getClass().getResource("/Debug/region/r.0.-1.mca").toURI())));
		renderer.render(new Vector2i(0, 0), new RegionFile(Paths.get(getClass().getResource("/Debug/region/r.0.0.mca").toURI())));
		renderer.render(new Vector2i(0, 1), new RegionFile(Paths.get(getClass().getResource("/Debug/region/r.0.1.mca").toURI())));
		renderer.render(new Vector2i(1, -1), new RegionFile(Paths.get(getClass().getResource("/Debug/region/r.1.-1.mca").toURI())));
		renderer.render(new Vector2i(1, 0), new RegionFile(Paths.get(getClass().getResource("/Debug/region/r.1.0.mca").toURI())));
		renderer.render(new Vector2i(1, 1), new RegionFile(Paths.get(getClass().getResource("/Debug/region/r.1.1.mca").toURI())));
	}

	/**
	 * Use Minecraft generated data to test if every single block state existing is present here. Should do exactly the same thing as
	 * {@link #testDebugWorld()}, but just in case
	 */
	@Test
	public void testExisting() throws IOException, URISyntaxException {
		BlockColorMap map = ColorCompiler.compileBlockColors(minecraftJarfile, Paths.get(getClass().getResource("/block-color-instructions.json").toURI()));

		try (JsonReader reader = new JsonReader(Files.newBufferedReader(Paths.get(getClass().getResource("/blocks.json").toURI())))) {
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
									assertTrue("Block " + block + " should exist in color map", map.hasBlockColor(block));

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
	}
}
