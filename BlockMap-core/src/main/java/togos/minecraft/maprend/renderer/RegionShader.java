package togos.minecraft.maprend.renderer;

public interface RegionShader {

	// public IntBuffer shadeRegion(
	// // Region data
	// LongBuffer blockIDs,
	// ByteBuffer heightMap,
	// ByteBuffer biomeMap,
	// // Shading/Color data
	// IntBuffer colorMap);

	// public IntBuffer shadeRegion(
	// RenderSettings settings,
	// IntBuffer color,
	// ShortBuffer height);
	//
	// public static class DefaultRegionShader implements RegionShader {
	//
	// @Override
	// public IntBuffer shadeRegion(RenderSettings settings, IntBuffer color, ShortBuffer height) {
	// for (int i = color.capacity() - 1; i >= 0; --i)
	// color.put(i, Color.demultiplyAlpha(color.get(i)));
	//
	// int width = 512, depth = 512;
	//
	// int idx = 0;
	// for (int z = 0; z < depth; ++z) {
	// for (int x = 0; x < width; ++x, ++idx) {
	// float dyx, dyz;
	//
	// if (color.get(idx) == 0)
	// continue;
	//
	// if (x == 0)
	// dyx = height.get(idx + 1) - height.get(idx);
	// else if (x == width - 1)
	// dyx = height.get(idx) - height.get(idx - 1);
	// else
	// dyx = (height.get(idx + 1) - height.get(idx - 1)) * 2;
	//
	// if (z == 0)
	// dyz = height.get(idx + width) - height.get(idx);
	// else if (z == depth - 1)
	// dyz = height.get(idx) - height.get(idx - width);
	// else
	// dyz = (height.get(idx + width) - height.get(idx - width)) * 2;
	//
	// float shade = dyx + dyz;
	// if (shade > 10)
	// shade = 10;
	// if (shade < -10)
	// shade = -10;
	//
	// int altShade = settings.altitudeShadingFactor * (height.get(idx) - settings.shadingReferenceAltitude) / 255;
	// if (altShade < settings.minAltitudeShading)
	// altShade = settings.minAltitudeShading;
	// if (altShade > settings.maxAltitudeShading)
	// altShade = settings.maxAltitudeShading;
	//
	// shade += altShade;
	//
	// color.put(idx, Color.shade(color.get(idx), (int) (shade * 8)));
	// }
	// }
	//
	// return color;
	// }
	// }
	//
	// public static class HeightRegionShader implements RegionShader {
	//
	// @Override
	// public IntBuffer shadeRegion(RenderSettings settings, IntBuffer color, ShortBuffer height) {
	// return null;
	// }
	//
	// }
	//
	// public static class OpenGLRegionShader implements RegionShader {
	//
	// @Override
	// public IntBuffer shadeRegion(RenderSettings settings, IntBuffer color, ShortBuffer height) {
	// return null;
	// }
	// }
}
