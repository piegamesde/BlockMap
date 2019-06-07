package de.piegames.blockmap.world;

import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.Objects;

import org.joml.Vector2ic;

/** Objects of this class represent a rendered Minecraft region. The image data is stored as {@link BufferedImage} in memory. */
public class Region {
	protected Vector2ic									position;
	protected BufferedImage								image;
	protected Map<? extends Vector2ic, ChunkMetadata>	metadata;

	public Region(Vector2ic position, BufferedImage image, Map<? extends Vector2ic, ChunkMetadata> metadata) {
		this.position = Objects.requireNonNull(position);
		this.image = Objects.requireNonNull(image);
		this.metadata = Objects.requireNonNull(metadata);
	}

	public Vector2ic getPosition() {
		return position;
	}

	public BufferedImage getImage() {
		return image;
	}

	public Map<? extends Vector2ic, ChunkMetadata> getChunkMetadata() {
		return metadata;
	}
}
