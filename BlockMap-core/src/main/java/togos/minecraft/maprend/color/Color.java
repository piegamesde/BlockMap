package togos.minecraft.maprend.color;

public class Color {

	public static final Color MISSING = new Color(1f, 1f, 0f, 1f);
	public static final Color TRANSPARENT = new Color(0, 0, 0, 0);

	public final float r, g, b, a;

	public Color(float a, float r, float g, float b) {
		this.r = r;
		this.g = g;
		this.b = b;
		this.a = a;
	}

	/** Converts this color to sRGB8 with linear alpha component on bit 24-31 */
	public int toRGB() {
		return ((0xFF & (int) (a * 255)) << 24) |
				((linearRGBTosRGB(r) & 0xFF) << 16) |
				((linearRGBTosRGB(g) & 0xFF) << 8) |
				((linearRGBTosRGB(b) & 0xFF));
	}

	/** Take in a sRGB color with linear alpha component */
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

	static int linearRGBTosRGB(float component) {
		double tempComponent = 0.0f;
		if (component <= 0.00318308)
			tempComponent = 12.92 * component;
		else
			tempComponent = 1.055 * Math.pow(component, 1.0 / 2.4) - 0.055;
		return (int) (tempComponent * 255.0);
	}

	public static final int component(int color, int shift) {
		return (color >> shift) & 0xFF;
	}

	public static final int alpha(int color) {
		return component(color, 24);
	}

	// public static final int shade(int color, int amt) {
	// return color(
	// component(color, 24),
	// component(color, 16) + amt,
	// component(color, 8) + amt,
	// component(color, 0) + amt);
	// }
	//
	// /**
	// * Return the color resulting from overlaying frontColor over backColor + Front color's RGB should *not* be pre-multiplied by alpha. -
	// Back
	// * color must have RGB components pre-multiplied by alpha. - Resulting color will be pre-multiplied by alpha.
	// */
	// public static final int overlay(int color, int overlayColor) {
	// final int overlayOpacity = component(overlayColor, 24);
	// final int overlayTransparency = 255 - overlayOpacity;
	//
	// return color(
	// overlayOpacity + (component(color, 24) * overlayTransparency) / 255,
	// (component(overlayColor, 16) * overlayOpacity + component(color, 16) * overlayTransparency) / 255,
	// (component(overlayColor, 8) * overlayOpacity + component(color, 8) * overlayTransparency) / 255,
	// (component(overlayColor, 0) * overlayOpacity + component(color, 0) * overlayTransparency) / 255);
	// }

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

	// public static final int demultiplyAlpha(int color) {
	// final int alpha = component(color, 24);
	//
	// return alpha == 0 ? 0
	// : color(
	// alpha,
	// component(color, 16) * 255 / alpha,
	// component(color, 8) * 255 / alpha,
	// component(color, 0) * 255 / alpha);
	// }
}
