package togos.minecraft.maprend.renderer;

import java.io.File;

public class RenderSettings {

	public int		minX						= Integer.MIN_VALUE;
	public int		maxX						= Integer.MAX_VALUE;
	public int		minY						= Integer.MIN_VALUE;
	public int		maxY						= Integer.MAX_VALUE;
	public int		minZ						= Integer.MIN_VALUE;
	public int		maxYZ						= Integer.MAX_VALUE;

	/**
	 * Above = brighter, below = darker TODO affect grass color in a more native way
	 */
	public int		shadingReferenceAltitude	= 64;
	/** Maximum brightness difference through shading */
	public int		altitudeShadingFactor		= 50;

	public int		minAltitudeShading			= -20;
	public int		maxAltitudeShading			= +20;

	public String	mapTitle					= "Regions";
	public int[]	mapScales					= { 1 };

	public RenderSettings() {
	}

	public RenderSettings(
			File colorMapFile, File biomeMapFile, boolean debug, int minHeight, int maxHeight,
			int shadingRefAlt, int minAltShading, int maxAltShading, int altShadingFactor,
			String mapTitle, int[] mapScales) {

		// this.colorMapFile = colorMapFile;
		// this.biomeMapFile = biomeMapFile;
		// this.debug = debug;
		//
		// this.minHeight = minHeight;
		// this.maxHeight = maxHeight;
		this.shadingReferenceAltitude = shadingRefAlt;
		this.minAltitudeShading = minAltShading;
		this.maxAltitudeShading = maxAltShading;
		this.altitudeShadingFactor = altShadingFactor;

		this.mapTitle = mapTitle;
		this.mapScales = mapScales;
	}
}