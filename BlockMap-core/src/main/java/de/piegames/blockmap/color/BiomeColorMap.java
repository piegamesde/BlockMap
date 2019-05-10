package de.piegames.blockmap.color;

import java.io.InputStreamReader;
import java.io.Reader;
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
		/** The water color in that biome. */
		public Color	waterColor;
		/** The grass color in that biome. */
		public Color	grassColor;
		/** The foliage color in that biome. */
		public Color	foliageColor;
		/** This color does not actually exist in Minecraft. It is used to represent a biome on a map and not to just tint a block's texture. */
		public Color	biomeColor;

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

	public BiomeColor getBiomeColor(int biome) {
		return biomeColors.getOrDefault(biome, MISSING);
	}

	public BiomeColorMap(Map<Integer, BiomeColor> biomeColors) {
		this.biomeColors = Objects.requireNonNull(biomeColors);
	}

	public static BiomeColorMap load(Reader reader) {
		return GSON.fromJson(reader, BiomeColorMap.class);
	}

	public static BiomeColorMap loadDefault() {
		return load(new InputStreamReader(BiomeColorMap.class.getResourceAsStream("/biome-colors.json")));
	}
}