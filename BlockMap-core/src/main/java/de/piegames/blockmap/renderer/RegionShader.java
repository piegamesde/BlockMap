package de.piegames.blockmap.renderer;

import java.io.InputStreamReader;
import java.util.Objects;

import com.google.gson.stream.JsonReader;

import de.piegames.blockmap.color.BiomeColorMap;
import de.piegames.blockmap.color.BlockColorMap;
import de.piegames.blockmap.color.Color;

/**
 * After having rendered a map, it will be shaded by an implementation of this interface. It has total access to the rendered image and to
 * some useful additional information like block height and biome.
 * 
 * @author piegames
 */
public interface RegionShader {

	/** All built-in shaders as enum for the CLI interface */
	public static enum DefaultShader {
		FLAT, RELIEF, BIOMES, HEIGHTMAP;

		public RegionShader getShader() {
			return DEFAULT_SHADERS[ordinal()];
		}
	}

	/** All built-in shaders */
	public static final RegionShader[] DEFAULT_SHADERS = new RegionShader[] {
			new FlatShader(), new ReliefShader(), new BiomeShader(), new HeightShader()
	};

	/**
	 * Shade a rendered region file to its final form.
	 * 
	 * @param map
	 *            The image to shade. Will always be contain 512*512 elements. Changes made in this array will affect the final image
	 * @param height
	 *            A height map, always containing 512*512 entries. Bedrock begins at height 0, the sea level is at height 64 and the build
	 *            height is at 255. You may assume all values are in this range.
	 * @param biome
	 *            The biome IDs at each position as a 512*512 array. Currently, biome IDs are bytes where values, but this will likely change in
	 *            the future.
	 * @param biomeColors
	 *            Use this to retrieve the color of a biome from its ID.
	 */
	public void shade(Color[] map, int[] height, int[] biome, BiomeColorMap biomeColors);

	/** A simple shader that does nothing. */
	public class FlatShader implements RegionShader {

		@Override
		public void shade(Color[] map, int[] height, int[] biome, BiomeColorMap biomeColors) {
		}

		@Override
		public int hashCode() {
			return Objects.hash(getClass().getName());
		}
	}

	/** This shader does some classic relief shading with a fictional light source coming from the north-west. */
	public class ReliefShader implements RegionShader {

		@Override
		public void shade(Color[] map, int[] height, int[] biome, BiomeColorMap biomeColors) {
			for (int z = 0; z < 512; z++)
				for (int x = 0; x < 512; x++) {
					if (map[z << 9 | x] == null)
						continue;
					int centerHeight = height[z << 9 | x];
					int westHeight = height[z << 9 | Math.max(x - 1, 0)];
					int eastHeight = height[z << 9 | Math.min(x + 1, 511)];
					int northHeight = height[Math.max(z - 1, 0) << 9 | x];
					int southHeight = height[Math.min(z + 1, 511) << 9 | x];
					int northWestHeight = height[Math.max(z - 1, 0) << 9 | Math.max(x - 1, 0)];
					int northEastHeight = height[Math.max(z - 1, 0) << 9 | Math.min(x + 1, 511)];
					int southWestHeight = height[Math.min(z + 1, 511) << 9 | Math.max(x - 1, 0)];
					int southEastHeight = height[Math.min(z + 1, 511) << 9 | Math.min(x + 1, 511)];
					double gX = (northWestHeight * 1 + 2 * westHeight + southWestHeight * 1 - eastHeight * 1 - 2 * northEastHeight - southEastHeight * 1);
					double gY = (northWestHeight * 1 + 2 * northHeight + northEastHeight * 1 - southWestHeight * 1 - 2 * southHeight - southEastHeight * 1);
					double g = Math.sqrt(gX * gX + gY * gY);
					// Vector3f gradientX = new Vector3f(2, (northWestHeight + westHeight + southWestHeight - eastHeight - northEastHeight -
					// southEastHeight) /
					// 3,
					// 0);
					// Vector3f gradientZ = new Vector3f(0,
					// (northHeight + northWestHeight + northEastHeight - southHeight - southWestHeight - southEastHeight) / 3, 2);
					// Vector3f gradient = gradientZ.cross(gradientX, new Vector3f());
					// Vector3f sunlight = new Vector3f(0, 1, 0);
					// double angle = (sunlight.angle(gradient) / (2 * Math.PI));

					// Calculate the angle of the slope [0..2PI]
					// double factor = ((Math.atan2(gY, gX) + Math.PI + 7 / 4d * Math.PI) % (2 * Math.PI)) - Math.PI;
					// Offset it by 135° (3/4 PI)
					// factor = ((factor + 3 / 4f * Math.PI) % (2 * Math.PI));
					// Map to [-1..1]
					// factor /= 2 * Math.PI;
					// Map to [0..1]
					// factor = (Math.abs(factor));
					// factor = Color.linearRGBTosRGB(factor);
					// factor = 2 * (factor - 0.5);

					// factor *= Math.tanh(g / 5);
					// Map to [-1..1]
					// factor = (factor + 1) / 2;

					double factor = -Math.tanh((gX + gY) / 10);
					factor *= 0.3;

					// if (g == 0)
					// map[z << 9 | x] = Color.fromRGB(java.awt.Color.HSBtoRGB((float) factor, 1, 0));
					// else
					// map[z << 9 | x] = Color.fromRGB(java.awt.Color.HSBtoRGB((float) factor, 1, 1));

					// map[z << 9 | x] = Color.fromRGB(java.awt.Color.HSBtoRGB(0, 0, (float) factor));

					// factor = (1.0 * (centerHeight - settings.minY) / (settings.maxY - settings.minY));
					// map[z << 9 | x] = new Color(1, (float) factor, (float) factor, (float) factor);
					// map[z << 9 | x] = new Color(1, (float) factor, (float) factor, (float) factor);
					// map[z << 9 | x] = Color.shade(map[z << 9 | x], (float) (x / 512d - 0.5) * 2f);

					map[z << 9 | x] = Color.shade(map[z << 9 | x], (float) factor);

					// Test 3
					// int maxHeight = Math.max(Math.max(northHeight, southHeight), Math.max(westHeight, eastHeight));
					// int minHeight = Math.min(Math.min(northHeight, southHeight), Math.min(westHeight, eastHeight));
					// if (maxHeight >>> 3 != minHeight >>> 3)
					// map[z << 9 | x] = Color.alphaOver(map[z << 9 | x],
					// new Color((float) Color.sRGBToLinear(Math.min(((maxHeight - minHeight + 8) >>> 3) / 10.0, 1)), 0, 0, 0));

					// Test 1
					// map[z << 9 | x] = Color.alphaOver(map[z << 9 | x], new Color((float) Math.tanh(g / 20) * 0.6f, 0, 0, 0));

					// Test 2
					// if ((height[z << 9 | x] & 7) == 0 && g > 0.05)
					// map[z << 9 | x] = Color.alphaOver(map[z << 9 | x], new Color(0.1f, 0, 0, 0));
				}
		}

		@Override
		public int hashCode() {
			return Objects.hash(getClass().getName());
		}
	}

	/** This shader will discard all color information and replace it by the color of the biome the block is in. */
	public static class BiomeShader implements RegionShader {

		@Override
		public void shade(Color[] map, int[] height, int[] biome, BiomeColorMap biomeColors) {
			for (int i = 0; i < 512 * 512; i++)
				if (biome[i] != -1)
					map[i] = biomeColors.getBiomeColor(biome[i] & 0xFF).biomeColor;
		}

		@Override
		public int hashCode() {
			return Objects.hash(getClass().getName());
		}
	}

	/** This shader will discard all color information and replace it by a color gradient representing the height of each block. */
	public static class HeightShader implements RegionShader {

		private static final Color[] colors;

		static {
			colors = BlockColorMap.GSON.fromJson(new JsonReader(new InputStreamReader(
					HeightShader.class.getResourceAsStream("/heightmap.json"))), Color[].class);
		}

		@Override
		public void shade(Color[] map, int[] height, int[] biome, BiomeColorMap biomeColors) {
			for (int i = 0; i < 512 * 512; i++)
				if (biome[i] != -1)
					map[i] = colors[height[i] + 64];
		}

		@Override
		public int hashCode() {
			return Objects.hash(getClass().getName());
		}
	}
}
