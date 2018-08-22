package togos.minecraft.maprend;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class DotMinecraft {

	private DotMinecraft() {
	}

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