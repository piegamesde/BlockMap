package de.piegames.blockmap;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import de.piegames.blockmap.standalone.CommandLineMain;
import de.piegames.blockmap.standalone.PostProcessing;
import de.piegames.blockmap.world.RegionFolder;

public class CommandLineTest {

	// /** Save out since we'll override it later on */
	// private static PrintStream out = System.out;

	@BeforeClass
	public static void testWorldExists() {
		assertNotNull("Please run regenerate to generate testing data", CommandLineTest.class.getResource("/BlockMapWorld"));
	}

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	/**
	 * A simple test running the CLI with different options to make sure no exception is thrown.
	 * 
	 * @throws IOException
	 */
	@Test
	public void test() throws IOException {
		File out1 = folder.newFolder();
		File out2 = folder.newFolder();

		CommandLineMain.main("-v", "-V");
		CommandLineMain.main("render", "-o=" + out1 + "", "./src/test/resources/Debug/region/");
		CommandLineMain.main("render", "--create-tile-html", "--lazy", "-o=" + out1 + "", "./src/test/resources/Debug/region/");
		CommandLineMain.main("render", "--create-big-image", "-o=" + out1 + "", "--shader=RELIEF", "--color-map=OCEAN_GROUND",
				"./src/test/resources/Debug/region/");

		CommandLineMain.main("-v", "render", "-o=" + out2 + "", "./src/main/resources/BlockMapWorld/region/");
		CommandLineMain.main("-v", "render", "--create-tile-html", "--lazy", "-o=" + out2 + "/", "./src/main/resources/BlockMapWorld/region/");
		CommandLineMain.main("-v", "render", "--create-big-image", "-o=" + out2 + "", "--shader=RELIEF", "--color-map=OCEAN_GROUND",
				"./src/main/resources/BlockMapWorld/region/");

		CommandLineMain.main("-v", "render", "--create-tile-html", "--lazy", "-o=" + out2 + "/", "./src/main/resources/BlockMapWorld/",
				"--dimension=OVERWORLD", "save", "--world-name=testworld", "-p");
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

	// private void testBounds(int minX, int maxX, int minZ, int maxZ, int renderedFiles, int skippedFiles, int minPixelX, int maxPixelX, int
	// minPixelZ,
	// int maxPixelZ) throws IOException {
	// try {
	// File out1 = folder.newFolder();
	// /* Record the log to a byte buffer */
	// ByteArrayOutputStream out = new ByteArrayOutputStream();
	// System.setOut(new PrintStream(out));
	// CommandLineMain.main("-v",
	// "render",
	// "-o=" + out1 + "",
	// "./src/main/resources/BlockMapWorld/region/",
	// "--minX=" + minX,
	// "--maxX=" + maxX,
	// "--minZ=" + minZ,
	// "--maxZ=" + maxZ,
	// "--create-big-image",
	// "--create-tile-html");
	// out.close();
	// /* Read the byte buffer and parse it and check if the messages were correct */
	// BufferedReader in = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(out.toByteArray())));
	// for (String line = in.readLine(); line != null; line = in.readLine()) {
	//
	// }
	// in.close();
	// } finally {
	// System.setOut(out);
	// }
	// }
}
