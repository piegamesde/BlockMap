package de.piegames.blockmap.renderer;

import java.awt.image.BufferedImage;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joml.Vector2ic;

import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.StringTag;
import com.flowpowered.nbt.Tag;
import com.flowpowered.nbt.regionfile.RegionFile;

import de.piegames.blockmap.color.Color;
import de.piegames.blockmap.world.ChunkMetadata;
import de.piegames.blockmap.world.Region.BufferedRegion;

public abstract class RegionRenderer {

	private static Log			log						= LogFactory.getLog(RegionRenderer.class);

	public final RenderSettings	settings;
	protected Set<Block>		blocksWithMissingColor	= new HashSet<>();

	public RegionRenderer(RenderSettings settings) {
		this.settings = Objects.requireNonNull(settings);
	}

	/**
	 * Render a given {@link RegionFile} to a {@link BufferedImage}. The image will always have a width and height of 512 pixels.
	 * 
	 * @param file
	 *            The file to render. Should not be {@code null}
	 * @param regionPos
	 *            The position of the region file in region coordinates. Used to check if blocks are within the bounds of the area to render.
	 * @return An array of colors representing the final image. The image is square and 512x512 wide. The array sorted in XZ order.
	 */
	public BufferedRegion render(Vector2ic regionPos, RegionFile file) {
		log.info("Rendering region file " + regionPos.x() + " " + regionPos.y());
		BufferedImage image = new BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB);
		Map<Vector2ic, ChunkMetadata> metadata = new HashMap<>();
		Color[] colors = renderRaw(regionPos, file, metadata);
		// image.setRGB(0, 0, 512, 512, colors, 0, 512);
		for (int x = 0; x < 512; x++)
			for (int z = 0; z < 512; z++)
				if (colors[x | (z << 9)] != null)
					image.setRGB(x, z, colors[x | (z << 9)].toRGB());
		return new BufferedRegion(regionPos, image, metadata);
	}

	protected abstract Color[] renderRaw(Vector2ic regionPos, RegionFile file, Map<Vector2ic, ChunkMetadata> metadata);

	protected static BitSet parseBlockState(CompoundTag properties, BlockState state) {
		BitSet ret = new BitSet(state.getSize());
		if (properties != null)
			for (Entry<String, Tag<?>> entry : properties.getValue().entrySet())
				ret.set(state.getProperty(entry.getKey(), ((StringTag) entry.getValue()).getValue()));
		return ret;
	}

	public static RegionRenderer create(RenderSettings settings) {
		switch (settings.version) {
		case MC_1_13:
			return new RegionRenderer_1_13(settings);
		case MC_1_14:
			return new RegionRenderer_1_14(settings);
		default:
			throw new UnsupportedOperationException("Did not find a RegionRenderer for version " + settings.version);
		}
	}
}