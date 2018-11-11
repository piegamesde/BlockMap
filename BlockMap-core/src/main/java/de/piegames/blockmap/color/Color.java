package de.piegames.blockmap.color;

import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

/**
 * A class to represent an RGBA color using floats in a linear color space. Each object is immutable. Static helper methods do the
 * calculations.
 * 
 * @author piegames
 */
public class Color {
	/** JSON serialization adapter that minimizes the resulting string size without precision loss. */
	public static final TypeAdapter<Color>	ADAPTER		= new TypeAdapter<Color>() {

															@Override
															public void write(JsonWriter out, Color color) throws IOException {
																out.beginArray();
																out.value(Float.floatToIntBits(color.a));
																out.value(Float.floatToIntBits(color.r));
																out.value(Float.floatToIntBits(color.g));
																out.value(Float.floatToIntBits(color.b));
																out.endArray();
															}

															@Override
															public Color read(JsonReader in) throws IOException {
																in.beginArray();
																Color color = new Color(
																		Float.intBitsToFloat(in.nextInt()),
																		Float.intBitsToFloat(in.nextInt()),
																		Float.intBitsToFloat(in.nextInt()),
																		Float.intBitsToFloat(in.nextInt()));
																in.endArray();
																return color;
															}
														};
	public static final Gson				GSON		= new GsonBuilder().registerTypeAdapter(Color.class, ADAPTER).setPrettyPrinting().create();

	/** The default fallback color for missing things. It is a pure and eye-hurting pink color */
	public static final Color				MISSING		= new Color(1f, 1f, 0f, 1f);
	/** Transparent black */
	public static final Color				TRANSPARENT	= new Color(0, 0, 0, 0);

	public final float						r, g, b, a;

	public Color(float a, float r, float g, float b) {
		this.r = r;
		this.g = g;
		this.b = b;
		this.a = a;
	}

	public Color(double a, double r, double g, double b) {
		this.r = (float) r;
		this.g = (float) g;
		this.b = (float) b;
		this.a = (float) a;
	}

	/**
	 * Converts this color to sRGB8 with linear alpha component on bit 24-31. This is the same representation as
	 * {@link java.awt.Color#getRGB()}.
	 */
	public int toRGB() {
		return ((0xFF & (int) (a * 255)) << 24) |
				((linearRGBTosRGBi(r) & 0xFF) << 16) |
				((linearRGBTosRGBi(g) & 0xFF) << 8) |
				((linearRGBTosRGBi(b) & 0xFF));
	}

	/** Take in an sRGB color with linear alpha component */
	public static Color fromRGB(int color) {
		return new Color(
				component(color, 24) / 255f,
				sRGBToLinear(component(color, 16)),
				sRGBToLinear(component(color, 8)),
				sRGBToLinear(component(color, 0)));
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Float.floatToIntBits(a);
		result = prime * result + Float.floatToIntBits(b);
		result = prime * result + Float.floatToIntBits(g);
		result = prime * result + Float.floatToIntBits(r);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Color other = (Color) obj;
		if (Float.floatToIntBits(a) != Float.floatToIntBits(other.a))
			return false;
		if (Float.floatToIntBits(b) != Float.floatToIntBits(other.b))
			return false;
		if (Float.floatToIntBits(g) != Float.floatToIntBits(other.g))
			return false;
		if (Float.floatToIntBits(r) != Float.floatToIntBits(other.r))
			return false;
		return true;
	}

	@Override
	public String toString() {
		// return "0x" + Integer.toHexString(toRGB());
		return a + " " + r + " " + g + " " + b;
	}

	/** Multiplies the RGB colors component-wise. The alpha of the resulting color is taken from A. */
	public static Color multiplyRGB(Color a, Color b) {
		return new Color(a.a, a.r * b.r, a.g * b.g, a.b * b.b);
	}

	/** Multiplies the RGBA colors component-wise. */
	public static Color multiplyRGBA(Color a, Color b) {
		return new Color(a.a * b.a, a.r * b.r, a.g * b.g, a.b * b.b);
	}

	// https://computergraphics.stackexchange.com/a/7947/6092

	static float sRGBToLinear(int component) {
		double tempComponent = component / 255.0;
		if (tempComponent <= 0.04045f)
			tempComponent = tempComponent / 12.92;
		else
			tempComponent = Math.pow((tempComponent + 0.055) / (1.055), 2.4);
		return (float) tempComponent;
	}

	public static double sRGBToLinear(double component) {
		double tempComponent = component;
		if (tempComponent <= 0.04045f)
			tempComponent = tempComponent / 12.92;
		else
			tempComponent = Math.pow((tempComponent + 0.055) / (1.055), 2.4);
		return tempComponent;
	}

	static int linearRGBTosRGBi(float component) {
		double tempComponent = 0.0f;
		if (component <= 0.00318308)
			tempComponent = 12.92 * component;
		else
			tempComponent = 1.055 * Math.pow(component, 1.0 / 2.4) - 0.055;
		return (int) (tempComponent * 255.0);
	}

	public static double linearRGBTosRGB(double component) {
		double tempComponent = 0.0f;
		if (component <= 0.00318308)
			tempComponent = 12.92 * component;
		else
			tempComponent = 1.055 * Math.pow(component, 1.0 / 2.4) - 0.055;
		return tempComponent;
	}

	public static final int component(int color, int shift) {
		return (color >> shift) & 0xFF;
	}

	public static final int alpha(int color) {
		return component(color, 24);
	}

	public static final Color alphaOver(Color dst, Color src) {
		float src1A = 1 - src.a;
		float outA = src.a + dst.a * src1A;

		if (outA == 0)
			return Color.TRANSPARENT;
		return new Color(
				outA,
				(src.r * src.a + dst.r * dst.a * src1A) / outA,
				(src.g * src.a + dst.g * dst.a * src1A) / outA,
				(src.b * src.a + dst.b * dst.a * src1A) / outA);
	}

	public static final Color alphaOver(Color dst, Color src, int times) {
		double pow = Math.pow(1 - src.a, times);
		double alpha = 1 - (1 - dst.a) * pow;
		double alphaDst = dst.a * pow;
		double alphaSrc = alpha - alphaDst;
		alphaSrc /= alpha;
		alphaDst /= alpha;

		if (alpha == 0)
			return Color.TRANSPARENT;
		return new Color(
				alpha,
				(src.r * alphaSrc + dst.r * alphaDst),
				(src.g * alphaSrc + dst.g * alphaDst),
				(src.b * alphaSrc + dst.b * alphaDst));
	}

	public static final Color alphaUnder(Color dst, Color src) {
		return alphaOver(src, dst);
	}

	public static final Color alphaUnder(Color dst, Color src, int times) {
		if (times == 1)
			return alphaUnder(dst, src);
		double pow = Math.pow(1 - src.a, times);
		double alpha = 1 - (1 - dst.a) * pow;
		double alphaDst = dst.a * pow;
		double alphaSrc = (1 - dst.a) * (alpha - alphaDst);
		alphaSrc /= alpha;
		alphaDst = dst.a / alpha;

		if (alpha == 0)
			return Color.TRANSPARENT;
		return new Color(
				alpha,
				(src.r * alphaSrc + dst.r * alphaDst),
				(src.g * alphaSrc + dst.g * alphaDst),
				(src.b * alphaSrc + dst.b * alphaDst));
	}

	/** factor=-1 -> black, factor=0 -> color, factor=1 -> white */
	public static final Color shade(Color color, float factor) {
		if (factor < 0) {
			factor = 1 + factor;
			factor = (float) sRGBToLinear(factor);
			return new Color(color.a, color.r * factor, color.g * factor, color.b * factor);
		} else if (factor > 0) {
			factor = sRGBToLinear((int) (factor * 255));
			factor = 1 - factor;
			return new Color(color.a, (1 - (1 - color.r) * factor), (1 - (1 - color.g) * factor), (1 - (1 - color.b) * factor));
		} else
			return color;
	}
}
