package de.piegames.blockmap;

import org.junit.Test;

import de.piegames.blockmap.standalone.CommandLineMain;

public class CommandLineTest {

	@Test
	public void test() {
		CommandLineMain.main("-o=\"./output/\"", "-v", "\"./src/test/resources/Debug/region/\"");
	}
}
