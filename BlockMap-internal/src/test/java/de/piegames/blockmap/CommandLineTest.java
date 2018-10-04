package de.piegames.blockmap;

import org.junit.Test;

import de.piegames.blockmap.standalone.CommandLineMain;

public class CommandLineTest {

	/** A simple test running the CLI with different options to make sure no exception is thrown. */
	@Test
	public void test() {
		CommandLineMain.main("-v", "-V");
		CommandLineMain.main("-v", "render", "-o=\"./output/\"", "\"./src/test/resources/Debug/region/\"");
		CommandLineMain.main("-v", "render", "--create-tile-html", "--lazy", "-o=\"./output/\"", "\"./src/test/resources/Debug/region/\"");
		CommandLineMain.main("-v", "render", "--create-big-image", "-o=\"./output/\"", "--shader=RELIEF", "--color-map=OCEAN_GROUND",
				"\"./src/test/resources/Debug/region/\"");
	}
}
