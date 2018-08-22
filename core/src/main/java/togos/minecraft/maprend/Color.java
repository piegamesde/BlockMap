package togos.minecraft.maprend;

public class Color
{
	protected static final int clampByte( int component ) {
		if( component < 0 ) return 0;
		if( component > 255 ) return 255;
		return component;
	}

	public static final int color( int a, int r, int g, int b ) {
		return
			(clampByte(a) << 24) |
			(clampByte(r) << 16) |
			(clampByte(g) <<  8) |
			(clampByte(b) <<  0);
	}

	public static final int component( int color, int shift ) {
		return (color >> shift) & 0xFF;
	}

	public static final int alpha( int color ) { return component(color,24); }

	public static final int shade( int color, int amt ) {
		return color(
			component( color, 24 ),
			component( color, 16 ) + amt,
			component( color,  8 ) + amt,
			component( color,  0 ) + amt
		);
	}

	/**
	 * Return the color resulting from overlaying frontColor over backColor
	 * + Front color's RGB should *not* be pre-multiplied by alpha.
	 * - Back color must have RGB components pre-multiplied by alpha.
	 * - Resulting color will be pre-multiplied by alpha.
	 */
	public static final int overlay( int color, int overlayColor ) {
		final int overlayOpacity = component( overlayColor, 24 );
		final int overlayTransparency = 255-overlayOpacity;

		return color(
			overlayOpacity                            + (component( color, 24 )*overlayTransparency)/255,
			(component(overlayColor,16)*overlayOpacity + component( color, 16 )*overlayTransparency)/255,
			(component(overlayColor, 8)*overlayOpacity + component( color,  8 )*overlayTransparency)/255,
			(component(overlayColor, 0)*overlayOpacity + component( color,  0 )*overlayTransparency)/255
		);
	}

	public static final int overlay( int color, int frontColor, int repeat ) {
		for( int i=0; i<repeat; ++i ) color = overlay(color,frontColor);
		return color;
	}

	public static final int alpha_over(int dst, int src) {
		final int srcA = component(src, 24);
		final int src1A = 255 - srcA;
		final int dstA = component(dst, 24);
		int outA = srcA + dstA * src1A;

		if (outA == 0)
			return 0;
		return color(
				outA / 255,
				(component(src, 16) * srcA + component(dst, 16) * dstA * src1A) / outA,
				(component(src, 8) * srcA + component(dst, 8) * dstA * src1A) / outA,
				(component(src, 0) * srcA + component(dst, 0) * dstA * src1A) / outA);
	}

	public static final int demultiplyAlpha( int color ) {
		final int alpha = component(color, 24);

		return alpha == 0 ? 0 : color(
			alpha,
			component(color, 16) * 255 / alpha,
			component(color,  8) * 255 / alpha,
			component(color,  0) * 255 / alpha
		);
	}

	public static final int multiply( int c1, int c2 ) {
		return color(
			component(c1,24) * component(c2,24) / (255),
			component(c1,16) * component(c2,16) / (255),
			component(c1, 8) * component(c2, 8) / (255),
			component(c1, 0) * component(c2, 0) / (255)
		);
	}

	/**
	 * Multiply 2 colors together by multiplying their R, G, and B components.
	 * Alpha is taken from only the first color.
	 * This way, the 2nd color acts as a filter.
	 */
	public static final int multiplySolid( int c1, int c2 ) {
	    return color(
	            component(c1,24),
	            component(c1,16) * component(c2,16) / (255),
	            component(c1, 8) * component(c2, 8) / (255),
	            component(c1, 0) * component(c2, 0) / (255)
	        );
	}
}
