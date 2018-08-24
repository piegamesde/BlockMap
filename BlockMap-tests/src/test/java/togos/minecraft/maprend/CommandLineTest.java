package togos.minecraft.maprend;

import java.nio.file.Path;

import org.junit.Test;

import picocli.CommandLine.Help.Visibility;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import togos.minecraft.maprend.standalone.CommandLineMain;

public class CommandLineTest {

	@Test
	public void test() {
		CommandLineMain.main("-o=\"./output/\"", "-v", "\"./src/test/resources/Debug/region/\"");
		// CommandLineMain.main("./Debug/region -v");

		// CommandLine cli = new CommandLine(new CommandLineMain());
		// cli.parseWithHandler(new RunLast(), new String[] {});
	}

	public static class PicocliTest implements Runnable {
		@Option(names = { "--output",
				"-o" }, defaultValue = "./", showDefaultValue = Visibility.ALWAYS)
		private Path output;
		@Parameters(index = "0", paramLabel = "INPUT")
		private Path input;

		@Override
		public void run() {
			System.out.println("Input: " + input);
			System.out.println("Output: " + output);
		}
	}
}
