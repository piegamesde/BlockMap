package de.piegames.blockmap.renderer;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.channels.ClosedByInterruptException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joml.Vector2i;
import org.joml.Vector2ic;

import de.piegames.blockmap.MinecraftVersion;
import de.piegames.blockmap.color.Color;
import de.piegames.blockmap.world.ChunkMetadata;
import de.piegames.blockmap.world.ChunkMetadata.ChunkMetadataCulled;
import de.piegames.blockmap.world.ChunkMetadata.ChunkMetadataFailed;
import de.piegames.blockmap.world.ChunkMetadata.ChunkMetadataVersion;
import de.piegames.blockmap.world.Region;
import de.piegames.nbt.CompoundTag;
import de.piegames.nbt.Tag;
import de.piegames.nbt.regionfile.Chunk;
import de.piegames.nbt.regionfile.RegionFile;
import de.piegames.nbt.stream.NBTInputStream;

public class RegionRenderer {

	private static Log			log	= LogFactory.getLog(RegionRenderer.class);

	public final RenderSettings	settings;
	private final ChunkRenderer	renderer13, renderer14;

	public RegionRenderer(RenderSettings settings) {
		this.settings = Objects.requireNonNull(settings);
		renderer13 = new ChunkRenderer_1_13(settings);
		renderer14 = new ChunkRenderer_1_14(settings);
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
	public Region render(Vector2ic regionPos, RegionFile file) {
		log.info("Rendering region file " + regionPos.x() + " " + regionPos.y());
		BufferedImage image = new BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB);
		Map<Vector2ic, ChunkMetadata> metadata = new HashMap<>();
		Color[] colors = renderRaw(regionPos, file, metadata);
		// image.setRGB(0, 0, 512, 512, colors, 0, 512);
		for (int x = 0; x < 512; x++)
			for (int z = 0; z < 512; z++)
				if (colors[x | (z << 9)] != null)
					image.setRGB(x, z, colors[x | (z << 9)].toRGB());
		return new Region(regionPos, image, metadata);
	}

	/**
	 * Render a given {@link RegionFile} to an image, represented as color array.
	 * 
	 * @param file
	 *            The file to render. Should not be {@code null}
	 * @param regionPos
	 *            The position of the region file in region coordinates. Used to check if blocks are within the bounds of the area to render.
	 * @return An array of colors representing the final image. The image is square and 512x512 wide. The array sorted in XZ order.
	 * @see #render(Vector2ic, RegionFile)
	 * @see Color
	 * @see RegionFile
	 */
	protected Color[] renderRaw(Vector2ic regionPos, RegionFile file, Map<Vector2ic, ChunkMetadata> metadata) {
		/* The final map of the chunk, 512*512 pixels, XZ */
		Color[] map = new Color[512 * 512];
		/* If nothing is set otherwise, the height map is set to the minimum height. */
		int[] height = new int[512 * 512];
		int[] regionBiomes = new int[512 * 512];
		Arrays.fill(height, settings.minY);
		Arrays.fill(regionBiomes, -1);

		for (int chunkIndex : file.listChunks()) {
			Chunk chunk = null;
			try {
				chunk = file.loadChunk(chunkIndex);
			} catch (ClosedByInterruptException e) {
				log.info("Got interrupted while rendering, stopping");
				break;
			} catch (IOException e) {
				int x = chunkIndex & 0xF, z = chunkIndex >> 4;
				log.warn("Failed to load chunk (" + x + ", " + z + ")", e);
				Vector2ic chunkPos = new Vector2i(((regionPos.x() << 5) | x), ((regionPos.y() << 5) | z));
				metadata.put(chunkPos, new ChunkMetadataFailed(chunkPos, e));
				continue;
			}
			int chunkX = ((regionPos.x() << 5) | chunk.x);
			int chunkZ = ((regionPos.y() << 5) | chunk.z);
			Vector2ic chunkPosRegion = new Vector2i(chunk.x, chunk.z);
			Vector2ic chunkPos = new Vector2i(chunkX, chunkZ);

			if ((chunkX + 16 < settings.minX || chunkX > settings.maxX)
					&& (chunkZ + 16 < settings.minZ || chunkZ > settings.maxZ)) {
				metadata.put(chunkPos, new ChunkMetadataCulled(chunkPos));
				continue;
			}

			CompoundTag root;
			try (NBTInputStream nbtIn = new NBTInputStream(new ByteArrayInputStream(chunk.getData().array(), 5, chunk.getRealLength()), chunk
					.getCompression(), true);) {
				root = new CompoundTag("chunk", ((CompoundTag) nbtIn.readTag()).getValue());
			} catch (IOException e) {
				log.warn("Failed to load chunk " + chunkPosRegion, e);
				metadata.put(chunkPos, new ChunkMetadataFailed(chunkPos, e));
				continue;
			}
			CompoundTag level = root.getAsCompoundTag("Level").get();

			/* Check data version */
			Optional<Integer> dataVersion = root.getAsIntTag("DataVersion").map(Tag::getValue);
			if (dataVersion.isPresent()) {
				int version = dataVersion.get();
				if (version < MinecraftVersion.MC_1_13.minVersion) {
					log.warn("Skipping chunk because it is too old");
					metadata.put(chunkPos, new ChunkMetadataVersion(chunkPos, "This chunk was written from Minecraft <1.13, which is not supported", version));
					continue;
				} else if (version <= MinecraftVersion.MC_1_13.maxVersion) {
					metadata.put(chunkPos, renderer13.renderChunk(chunkPosRegion, chunkPos, level, map, height, regionBiomes));
				} else if (version >= MinecraftVersion.MC_1_14.minVersion && version < MinecraftVersion.MC_1_14.maxVersion) {
					metadata.put(chunkPos, renderer14.renderChunk(chunkPosRegion, chunkPos, level, map, height, regionBiomes));
				} else {
					metadata.put(chunkPos, new ChunkMetadataVersion(chunkPos, "Could not find a chunk rendering engine for this version", version));
				}
			} else {
				log.warn("Skipping chunk because it is way too old (pre 1.9)");
				metadata.put(chunkPos, new ChunkMetadataVersion(chunkPos, "This chunk was written from Minecraft <1.9, which is not supported", 0));
				continue;
			}
		}
		settings.shader.shade(map, height, regionBiomes, settings.biomeColors);
		return map;
	}
}