package de.piegames.blockmap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.joml.Vector2i;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.google.gson.reflect.TypeToken;

import de.piegames.blockmap.color.BiomeColorMap;
import de.piegames.blockmap.color.BlockColorMap;
import de.piegames.blockmap.color.BlockColorMap.BlockColor;
import de.piegames.blockmap.color.BlockColorMap.InternalColorMap;
import de.piegames.blockmap.color.BlockColorMap.NormalStateColors;
import de.piegames.blockmap.color.BlockColorMap.SingleStateColors;
import de.piegames.blockmap.color.BlockColorMap.StateColors;
import de.piegames.blockmap.generate.Generator;
import de.piegames.blockmap.generate.ColorCompiler.BiomeInstructions;
import de.piegames.blockmap.renderer.Block;
import de.piegames.blockmap.renderer.BlockState;
import de.piegames.blockmap.renderer.RegionRenderer;
import de.piegames.blockmap.renderer.RegionShader;
import de.piegames.blockmap.renderer.RenderSettings;
import de.piegames.nbt.regionfile.RegionFile;

public class BiomesTest {

	/** Make sure that all biomes are covered in the biome-color-instructions.json */
	@Test
	public void testBiomeInstructions() throws IOException {
		var biomeDataDirectory = Paths.get(URI.create(getClass().getResource(
				"/data-" + MinecraftVersion.LATEST.fileSuffix + "/reports/minecraft/worldgen/biome")
				.toString()));
		var instructionsPath = Paths.get(URI.create(Generator.class.getResource("/biome-color-instructions.json").toString()));

		Map<String, BiomeInstructions> instructions = BiomeColorMap.GSON.fromJson(
			Files.newBufferedReader(instructionsPath),
			new TypeToken<Map<String, BiomeInstructions>>() {
			}.getType()
		);

		for (Path biomePath : Files.walk(biomeDataDirectory).collect(Collectors.toList())) {
			if (Files.isDirectory(biomePath))
				continue;
			String biomeName = biomePath.getFileName().toString().replace(".json", "");
			String biomeId = "minecraft:" + biomeName;
			
			assertTrue("Biomes instructions file is missing " + biomeId, instructions.containsKey(biomeId));
			assertTrue("Biome " + biomeId + " should not be marked as removed", !instructions.get(biomeId).legacy);
		}
		
		for (var e : instructions.entrySet()) {
			String biomeId = e.getKey();
			String biomeName = biomeId.substring("minecraft:".length());
			BiomeInstructions inst = e.getValue();
			
			if (inst.legacy) {
				assertTrue("Biome " + biomeId + " has been removed, ", !Files.exists(biomeDataDirectory.resolve(biomeName + ".json")));
			}
		}
	}
}
