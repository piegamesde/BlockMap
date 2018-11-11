package de.piegames.blockmap.color;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Represents a mapping from biome IDs to their actual color.
 * 
 * @author piegames
 */
public class BiomeColorMap {

	public static final Gson		GSON	= new GsonBuilder().registerTypeAdapter(Color.class, Color.ADAPTER).setPrettyPrinting().create();
	public static final BiomeColor	MISSING	= new BiomeColor(Color.MISSING, Color.MISSING, Color.MISSING, Color.MISSING);

	/** Holds all distinct colors a biome has: its water color, grass color, foliage color and universal color. */
	public static class BiomeColor {
		public Color waterColor, grassColor, foliageColor, biomeColor;

		public BiomeColor() {

		}

		public BiomeColor(Color waterColor, Color grassColor, Color foliageColor, Color biomeColor) {
			this.waterColor = waterColor;
			this.grassColor = grassColor;
			this.foliageColor = foliageColor;
			this.biomeColor = biomeColor;
		}
	}

	protected Map<Integer, BiomeColor> biomeColors;

	@SuppressWarnings("unused")
	private BiomeColorMap() {
		// For deserialization purposes
	}

	public BiomeColorMap(Map<Integer, BiomeColor> biomeColors) {
		this.biomeColors = Objects.requireNonNull(biomeColors);
	}

	@Deprecated
	public BiomeColorMap(Map<Integer, Color> waterColor, Map<Integer, Color> grassColor, Map<Integer, Color> foliageColor, Map<Integer, Color> biomeColor) {
		biomeColors = new HashMap<>();
		for (int i : waterColor.keySet())
			biomeColors.put(i, new BiomeColor(waterColor.get(i), grassColor.get(i), foliageColor.get(i), biomeColor.get(i)));
	}

	/** Returns the water color in that biome. */
	public Color getWaterColor(int biome) {
		return biomeColors.getOrDefault(biome, MISSING).waterColor;
	}

	/** Returns the grass color in that biome. */
	public Color getGrassColor(int biome) {
		return biomeColors.getOrDefault(biome, MISSING).grassColor;
	}

	/** Returns the foliage color in that biome. */
	public Color getFoliageColor(int biome) {
		return biomeColors.getOrDefault(biome, MISSING).foliageColor;
	}

	/** This color does not actually exist in Minecraft. It is used to represent a biome on a map and not to just tint a block's texture. */
	public Color getBiomeColor(int biome) {
		return biomeColors.getOrDefault(biome, MISSING).biomeColor;
	}

	public static BiomeColorMap load(Reader reader) {
		return GSON.fromJson(reader, BiomeColorMap.class);
	}

	public static BiomeColorMap loadDefault() {
		return load(new InputStreamReader(BiomeColorMap.class.getResourceAsStream("/biome-colors.json")));
	}
}