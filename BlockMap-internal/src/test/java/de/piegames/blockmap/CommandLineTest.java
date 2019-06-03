package de.piegames.blockmap;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import de.piegames.blockmap.standalone.CommandLineMain;
import de.piegames.blockmap.standalone.PostProcessing;
import de.piegames.blockmap.world.RegionFolder;

public class CommandLineTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	/**
	 * A simple test running the CLI with different options to make sure no exception is thrown.
	 * 
	 * @throws IOException
	 */
	@Test
	public void test() throws IOException {
		for (MinecraftVersion version : MinecraftVersion.values()) {
			File out1 = folder.newFolder();
			String path = "./src/test/resources/Debug-" + version.fileSuffix + "/region/";
			CommandLineMain.main("-v", "-V");
			CommandLineMain.main("render", "-o=" + out1 + "", path);
			CommandLineMain.main("render", "--create-tile-html", "--lazy", "-o=" + out1 + "", path);
			CommandLineMain.main("render", "--create-big-image", "-o=" + out1 + "", "--shader=RELIEF", "--color-map=OCEAN_GROUND", path);
		}
		File out2 = folder.newFolder();

		CommandLineMain.main("-v", "render", "-o=" + out2 + "", "--min-X=-1024", "--max-X=1024", "--min-Z=-1024", "--max-Z=1024",
				"./src/main/resources/BlockMapWorld/region/");
		CommandLineMain.main("-v", "render", "--create-tile-html", "--lazy", "-o=" + out2 + "/", "--min-X=-1024", "--max-X=1024", "--min-Z=-1024",
				"--max-Z=1024", "./src/main/resources/BlockMapWorld/region/");
		CommandLineMain.main("-v", "render", "--create-big-image", "-o=" + out2 + "", "--shader=RELIEF", "--color-map=OCEAN_GROUND", "--min-X=-1024",
				"--max-X=1024", "--min-Z=-1024", "--max-Z=1024",
				"./src/main/resources/BlockMapWorld/region/");

		CommandLineMain.main("-v", "render", "--create-tile-html", "--lazy", "-o=" + out2 + "/", "./src/main/resources/BlockMapWorld/", "--min-X=-1024",
				"--max-X=1024", "--min-Z=-1024", "--max-Z=1024",
				"--dimension=OVERWORLD");
	}

	/**
	 * Test the bounds on {@link PostProcessing#createTileHtml(RegionFolder, java.nio.file.Path, de.piegames.blockmap.renderer.RenderSettings)}
	 * and {@link PostProcessing#createBigImage(RegionFolder, java.nio.file.Path, de.piegames.blockmap.renderer.RenderSettings)}.
	 */
	@Test
	public void testBounds() {
		assertTrue(PostProcessing.inBounds(0, 0, 200));
		assertTrue(PostProcessing.inBounds(0, 0, 511));
		assertTrue(PostProcessing.inBounds(0, 0, 512));
		assertTrue(PostProcessing.inBounds(0, 511, 512));
		assertFalse(PostProcessing.inBounds(0, 512, 512));
		assertTrue(PostProcessing.inBounds(1, 512, 512));
		assertFalse(PostProcessing.inBounds(1, -100, 100));
		assertTrue(PostProcessing.inBounds(0, -100, 100));
		assertTrue(PostProcessing.inBounds(-1, -100, 100));

		for (int i = -1000; i < 1000; i++)
			assertEquals("Value " + i + " failed", i >= -1 && i < 1, PostProcessing.inBounds(i, -4, 3));
	}
}
