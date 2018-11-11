package de.piegames.blockmap;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Helper class to find the default {@code .minecraft} folder on different operating systems.
 * 
 * @author piegames
 */
public final class DotMinecraft {

	private DotMinecraft() {
	}

	/**
	 * The default location of the {@code .minecraft} folder. May not actually point to that folder. May point to a non-existing folder or to a
	 * non-folder. Should point to the actual .minecraft folder for most users.
	 */
	public static final Path DOTMINECRAFT;

	static {
		Path mc = null;
		String OS = System.getProperty("os.name").toUpperCase();
		if (OS.contains("WIN")) {
			mc = Paths.get(System.getenv("APPDATA")).resolve(".minecraft");
		} else if (OS.contains("MAC")) {
			mc = Paths.get(System.getProperty("user.home") + "/Library/Application Support").resolve("minecraft");
		} else if (OS.contains("NUX")) {
			mc = Paths.get(System.getProperty("user.home"), ".minecraft");
		} else {
			mc = Paths.get(System.getProperty("user.dir"));
		}
		DOTMINECRAFT = mc;
	}
}