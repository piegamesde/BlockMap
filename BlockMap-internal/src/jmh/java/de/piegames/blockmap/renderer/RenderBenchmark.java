package de.piegames.blockmap.renderer;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.joml.Vector2i;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import de.piegames.blockmap.MinecraftVersion;
import de.piegames.blockmap.color.BiomeColorMap;
import de.piegames.blockmap.color.BlockColorMap;
import de.piegames.blockmap.color.BlockColorMap.InternalColorMap;
import de.piegames.blockmap.world.Region;
import de.piegames.nbt.regionfile.RegionFile;

@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(2)
@Warmup(iterations = 3)
@Threads(4)
@Measurement(iterations = 3)
public class RenderBenchmark {

	@Param({ "DEFAULT", "OCEAN_GROUND", "NO_FOLIAGE", "CAVES" })
	private String									colorMap;

	private Map<MinecraftVersion, BlockColorMap>	blockColors;
	private BiomeColorMap							biomeColors;
	private Path									resourcePath;

	@Setup
	public void loadColors() {
		biomeColors = BiomeColorMap.loadDefault();
		blockColors = InternalColorMap.valueOf(colorMap).getColorMap();
		resourcePath = Paths.get(System.getProperty("benchmark.resources"));
	}

	@Benchmark
	public void benchmark(Blackhole hole) throws IOException {
		RenderSettings settings = new RenderSettings();
		settings.blockColors = blockColors;
		settings.biomeColors = biomeColors;

		RegionRenderer renderer = new RegionRenderer(settings);
		hole.consume(render(renderer, new Vector2i(-1, 1)));
		hole.consume(render(renderer, new Vector2i(0, 1)));
		hole.consume(render(renderer, new Vector2i(-1, 2)));
		hole.consume(render(renderer, new Vector2i(0, 2)));
	}

	private Region render(RegionRenderer renderer, Vector2i position) throws IOException {
		return renderer.render(position, new RegionFile(resourcePath.resolve("BlockMapWorld/region/r." + position.x + "." + position.y + ".mca")));
	}
}
