package togos.minecraft.maprend.standalone;

import java.nio.file.Path;
import joptsimple.OptionParser;

public class CommandLineMain2 {

	public static void main(String[] args) {
		OptionParser parser = new OptionParser();
		parser.accepts("help").forHelp();
		parser.accepts("output").withRequiredArg().defaultsTo(".").ofType(Path.class);
		parser.accepts("input").withRequiredArg().ofType(Path.class);
		parser.accepts("verbose");

		parser.accepts("minX").withOptionalArg().ofType(Integer.class);
		parser.accepts("maxX").withOptionalArg().ofType(Integer.class);
		parser.accepts("minY").withOptionalArg().ofType(Integer.class);
		parser.accepts("maxY").withOptionalArg().ofType(Integer.class);
		parser.accepts("minZ").withOptionalArg().ofType(Integer.class);
		parser.accepts("maxZ").withOptionalArg().ofType(Integer.class);

	}
}