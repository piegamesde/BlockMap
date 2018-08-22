package togos.minecraft.maprend.color;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collections;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class BiomeColorMap {

	public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	protected Map<Integer, Color> waterColor;
	// TODO make grass and foliage colors more native
	protected Map<Integer, Color> grassColor;
	protected Map<Integer, Color> foliageColor;

	@SuppressWarnings("unused")
	private BiomeColorMap() {
		// For deserialization purposes
	}

	public BiomeColorMap(Map<Integer, Color> waterColor, Map<Integer, Color> grassColor, Map<Integer, Color> foliageColor) {
		this.waterColor = Collections.unmodifiableMap(waterColor);
		this.grassColor = Collections.unmodifiableMap(grassColor);
		this.foliageColor = Collections.unmodifiableMap(foliageColor);
	}

	public Color getWaterColor(int biome) {
		return waterColor.getOrDefault(biome, Color.MISSING);
	}

	public Color getGrassColor(int biome) {
		return grassColor.getOrDefault(biome, Color.MISSING);
	}

	public Color getFoliageColor(int biome) {
		return foliageColor.getOrDefault(biome, Color.MISSING);
	}

	public static BiomeColorMap load(Reader reader) {
		return GSON.fromJson(reader, BiomeColorMap.class);
	}

	public static BiomeColorMap loadDefault() {
		return load(new InputStreamReader(BiomeColorMap.class.getResourceAsStream("/biome-colors.json")));
	}
}