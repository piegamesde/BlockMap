package de.piegames.blockmap;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
		BlockStateTest.class,
		ColorCompilerTest.class,
		RegionRendererTest.class,
		RegionFolderTest.class,
		CommandLineTest.class
})
public class AllTests {
}