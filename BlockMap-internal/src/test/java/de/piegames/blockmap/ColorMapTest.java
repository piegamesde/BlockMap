package de.piegames.blockmap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.joml.Vector2i;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.flowpowered.nbt.regionfile.RegionFile;

import de.piegames.blockmap.color.BiomeColorMap;
import de.piegames.blockmap.color.BlockColorMap;
import de.piegames.blockmap.color.BlockColorMap.BlockColor;
import de.piegames.blockmap.color.BlockColorMap.InternalColorMap;
import de.piegames.blockmap.renderer.Block;
import de.piegames.blockmap.renderer.BlockState;
import de.piegames.blockmap.renderer.RegionRenderer;
import de.piegames.blockmap.renderer.RegionShader;
import de.piegames.blockmap.renderer.RenderSettings;

@RunWith(Parameterized.class)
public class ColorMapTest {

	@Parameters(name = "{0}, {1}")
	public static Collection<Object[]> data() {
		return Arrays.stream(MinecraftVersion.values())
				.flatMap(version -> Arrays.stream(BlockColorMap.InternalColorMap.values()).map(internal -> new Object[] { version, internal }))
				.collect(Collectors.toList());
	}

	@Parameter
	public MinecraftVersion	version;
	@Parameter(value = 1)
	public InternalColorMap	colorMap;

	MinecraftBlocks			blocks;
	BlockState				states;
	BlockColorMap			map;

	@Before
	public void load() throws IOException {
		blocks = new MinecraftBlocks(Paths.get(URI.create(getClass().getResource("/data-" + version.fileSuffix + "/reports/blocks.json").toString())));
		states = blocks.generateStates();
		map = colorMap.getColorMap(version);
	}

	/** Test that all blocks (an no more) are covered by the color map */
	@Test
	public void testBlockNames() {
		Map<String, Map<BitSet, BlockColor>> colors = map.getBlockColors();
		Set<String> tmp = new HashSet<>(colors.keySet());
		tmp.removeAll(blocks.states.keySet());
		assertEquals("Some blocks should be removed", Collections.emptySet(), tmp);
		tmp = new HashSet<>(blocks.states.keySet());
		tmp.removeAll(colors.keySet());
		assertEquals("Some blocks are missing", Collections.emptySet(), tmp);
		assertEquals(blocks.states.keySet(), colors.keySet());
	}

	/** Test that all possible states are covered by the color map */
	@Test
	public void testBlockStates() {
		BitSet allBits = new BitSet(states.getSize());
		allBits.set(0, states.getSize());
		Map<String, Map<BitSet, BlockColor>> colors = map.getBlockColors();
		colors.values().stream().flatMap(m -> m.keySet().stream()).forEach(allBits::andNot);
		assertTrue("Colors missing for states: " + allBits, allBits.isEmpty());
	}

	/** Test against each single state specified in Minecraft. */
	@Test
	public void testDetailed() {
		List<Block> missing = new ArrayList<>();
		Map<String, Map<BitSet, BlockColor>> colors = map.getBlockColors();
		for (Map.Entry<String, MinecraftBlocks.Block> block : blocks.states.entrySet()) {
			for (MinecraftBlocks.Block.State state : block.getValue().states) {
				BitSet compiledState = new BitSet(states.getSize());
				if (state.properties != null)
					state.properties.entrySet().forEach(e -> compiledState.set(states.getProperty(e.getKey(), e.getValue())));
				if (!colors.containsKey(block.getKey()) || colors.get(block.getKey()).remove(compiledState) == null)
					missing.add(new Block(block.getKey(), compiledState));
			}
		}
		assertTrue("The following states are missing in the color map: " + missing, missing.isEmpty());
		colors.keySet().removeIf(key -> colors.get(key).isEmpty());
		List<Block> tooMuch = colors.entrySet()
				.stream()
				.flatMap(e -> e.getValue().keySet().stream().map(v -> new Block(e.getKey(), v)))
				.collect(Collectors.toList());
		assertTrue("The following states should be removed from the color map: " + tooMuch, tooMuch.isEmpty());
	}

	/** The debug world contains every single block and block state that exists in the game, so let's test it */
	@Test
	public void testDebugWorld() throws IOException, URISyntaxException, InterruptedException {
		Set<Block> missing = new HashSet<>();
		RenderSettings settings = new RenderSettings();
		settings.version = version;
		settings.shader = RegionShader.DefaultShader.FLAT.getShader();
		settings.blockColors = new BlockColorMap(map.getBlockColors()) {
			@Override
			public BlockColor getBlockColor(String blockName, BitSet blockState) {
				if (!super.hasBlockColor(blockName, blockState))
					missing.add(new Block(blockName, blockState));
				return super.getBlockColor(blockName, blockState);
			}
		};
		settings.biomeColors = BiomeColorMap.loadDefault(version);
		RegionRenderer renderer = RegionRenderer.create(settings);
		String pathPrefix = "/Debug-" + version.fileSuffix + "/region/r.";
		assertNoMissing(renderer.render(new Vector2i(-1, -1), new RegionFile(Paths.get(getClass().getResource(pathPrefix + "-1.-1.mca").toURI()))).getImage());
		assertNoMissing(renderer.render(new Vector2i(-1, 0), new RegionFile(Paths.get(getClass().getResource(pathPrefix + "-1.0.mca").toURI()))).getImage());
		assertNoMissing(renderer.render(new Vector2i(-1, 1), new RegionFile(Paths.get(getClass().getResource(pathPrefix + "-1.1.mca").toURI()))).getImage());
		assertNoMissing(renderer.render(new Vector2i(0, -1), new RegionFile(Paths.get(getClass().getResource(pathPrefix + "0.-1.mca").toURI()))).getImage());
		assertNoMissing(renderer.render(new Vector2i(0, 0), new RegionFile(Paths.get(getClass().getResource(pathPrefix + "0.0.mca").toURI()))).getImage());
		assertNoMissing(renderer.render(new Vector2i(0, 1), new RegionFile(Paths.get(getClass().getResource(pathPrefix + "0.1.mca").toURI()))).getImage());
		assertNoMissing(renderer.render(new Vector2i(1, -1), new RegionFile(Paths.get(getClass().getResource(pathPrefix + "1.-1.mca").toURI()))).getImage());
		assertNoMissing(renderer.render(new Vector2i(1, 0), new RegionFile(Paths.get(getClass().getResource(pathPrefix + "1.0.mca").toURI()))).getImage());
		assertNoMissing(renderer.render(new Vector2i(1, 1), new RegionFile(Paths.get(getClass().getResource(pathPrefix + "1.1.mca").toURI()))).getImage());

		/* Filter out some false positives: Minecraft does not always store the waterlogged property for some reason (performance? bug?) */
		missing.removeIf(block -> {
			int i1 = states.getProperty("waterlogged", "true");
			int i2 = states.getProperty("waterlogged", "false");
			boolean hasWater = block.state.get(i1) || block.state.get(i2);
			BitSet withWater = (BitSet) block.state.clone();
			withWater.set(i1);
			BitSet withoutWater = (BitSet) block.state.clone();
			withoutWater.set(i2);
			/* If the block has no waterlogged property, but would be in the color map if it had one, remove it from the error list. */
			return !hasWater && (map.hasBlockColor(block.name, withWater) || map.hasBlockColor(block.name, withoutWater));
		});
		/* Same thing, but for TNT */
		missing.removeIf(block -> {
			int i1 = states.getProperty("unstable", "true");
			int i2 = states.getProperty("unstable", "false");
			boolean hasWater = block.state.get(i1) || block.state.get(i2);
			BitSet withWater = (BitSet) block.state.clone();
			withWater.set(i1);
			BitSet withoutWater = (BitSet) block.state.clone();
			withoutWater.set(i2);
			/* If the block has no unstable property, but would be in the color map if it had one, remove it from the error list. */
			return !hasWater && (map.hasBlockColor(block.name, withWater) || map.hasBlockColor(block.name, withoutWater));
		});
		assertTrue("Some blocks were missing during rendering: " + missing, missing.isEmpty());
	}

	/** Assert there are no "missing color" pixels in that image */
	private static void assertNoMissing(BufferedImage image) {
		// TODO tolerance
		for (int x = 0; x < image.getWidth(); x++)
			for (int y = 0; y < image.getHeight(); y++)
				assertTrue(0xFFFF00FF != image.getRGB(x, y));
	}

}
