package de.piegames.blockmap;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import de.piegames.blockmap.standalone.CommandLineMain;

public class CommandLineTest {

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
		CommandLineMain.main("-v", "render", "-o=" + out1 + "", "./src/test/resources/Debug/region/");
		CommandLineMain.main("-v", "render", "--create-tile-html", "--lazy", "-o=" + out1 + "", "./src/test/resources/Debug/region/");
		CommandLineMain.main("-v", "render", "--create-big-image", "-o=" + out1 + "", "--shader=RELIEF", "--color-map=OCEAN_GROUND",
				"./src/test/resources/Debug/region/");

		CommandLineMain.main("-v", "render", "-o=" + out2 + "", "./src/main/resources/BlockMapWorld/region/");
		CommandLineMain.main("-v", "render", "--create-tile-html", "--lazy", "-o=" + out2 + "/", "./src/main/resources/BlockMapWorld/region/");
		CommandLineMain.main("-v", "render", "--create-big-image", "-o=" + out2 + "", "--shader=RELIEF", "--color-map=OCEAN_GROUND",
				"./src/main/resources/BlockMapWorld/region/");
	}
}
