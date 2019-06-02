package de.piegames.blockmap.renderer;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import de.piegames.blockmap.MinecraftVersion;
import de.piegames.blockmap.color.BiomeColorMap;
import de.piegames.blockmap.color.BlockColorMap;
import de.piegames.blockmap.color.BlockColorMap.InternalColorMap;
import de.piegames.blockmap.renderer.RegionShader.ReliefShader;

public class RenderSettings {

	public int									minX	= Integer.MIN_VALUE;
	public int									maxX	= Integer.MAX_VALUE;
	public int									minY	= Integer.MIN_VALUE;
	public int									maxY	= Integer.MAX_VALUE;
	public int									minZ	= Integer.MIN_VALUE;
	public int									maxZ	= Integer.MAX_VALUE;

	public Map<MinecraftVersion, BlockColorMap>	blockColors;
	public BiomeColorMap						biomeColors;
	public RegionShader							shader	= new ReliefShader();

	public RenderSettings() {
	}

	public RenderSettings(int minX, int maxX, int minY, int maxY, int minZ, int maxZ, Map<MinecraftVersion, BlockColorMap> blockColors,
			BiomeColorMap biomeColors, RegionShader shader) {
		this.minX = minX;
		this.maxX = maxX;
		this.minY = minY;
		this.maxY = maxY;
		this.minZ = minZ;
		this.maxZ = maxZ;
		this.blockColors = blockColors;
		this.biomeColors = biomeColors;
		this.shader = shader;
	}

	public void loadDefaultColors() {
		blockColors = InternalColorMap.DEFAULT.getColorMap();
		biomeColors = BiomeColorMap.loadDefault();
	}

	@Override
	public int hashCode() {
		/*
		 * The keys of blockColors are an enum type. Sadly, for some reason, there is no possibility to override the hashCode function for enums.
		 * This means, that the keys of the map will always get different hash codes at runtime. To avoid this, we replace them with their ordinal
		 * for the hash calculation.
		 */
		return Objects.hash(
				biomeColors,
				blockColors.entrySet()
						.stream()
						.collect(Collectors.toMap(e -> e.getKey().ordinal(), Map.Entry::getValue)),
				maxX, maxY, maxZ, minX, minY, minZ, shader);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RenderSettings other = (RenderSettings) obj;
		return Objects.equals(biomeColors, other.biomeColors) && Objects.equals(blockColors, other.blockColors) && maxX == other.maxX && maxY == other.maxY
				&& maxZ == other.maxZ && minX == other.minX && minY == other.minY && minZ == other.minZ && Objects.equals(shader, other.shader);
	}
}