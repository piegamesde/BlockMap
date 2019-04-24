package de.piegames.blockmap.world;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

import javax.imageio.ImageIO;

import org.joml.Vector2ic;

/** Objects of this class represent a rendered Minecraft region. */
public abstract class Region {
	protected Vector2ic position;

	protected Region(Vector2ic position) {
		this.position = position;
	}

	public Vector2ic getPosition() {
		return position;
	}

	/** Retrieve the rendered image. The image may already be in memory or it might be loaded lazily from a resource location. */
	public abstract BufferedImage getImage() throws IOException;

	/**
	 * Get the metadata for all existing chunks indexed by their location in world space. Does not need to contain an entry for every chunk in
	 * the region. Must not contain entries for chunks outside of the region. Must not be <code>null</code>.
	 * 
	 * @see ChunkMetadata
	 */
	public abstract Map<? extends Vector2ic, ChunkMetadata> getChunkMetadata();

	public static class BufferedRegion extends Region {

		protected BufferedImage								image;
		protected Map<? extends Vector2ic, ChunkMetadata>	metadata;

		public BufferedRegion(Vector2ic position, BufferedImage image, Map<? extends Vector2ic, ChunkMetadata> metadata) {
			super(position);
			this.image = Objects.requireNonNull(image);
			this.metadata = Objects.requireNonNull(metadata);
		}

		@Override
		public BufferedImage getImage() throws IOException {
			return image;
		}

		@Override
		public Map<? extends Vector2ic, ChunkMetadata> getChunkMetadata() {
			return metadata;
		}

		public LocalSavedRegion save(Path out) throws IOException {
			ImageIO.write(image, "png", Files.newOutputStream(out));
			return new LocalSavedRegion(position, out, metadata);
		}
	}

	public static class SavedRegion extends Region {

		protected URI										path;
		protected Map<? extends Vector2ic, ChunkMetadata>	metadata;

		public SavedRegion(Vector2ic position, URI path, Map<? extends Vector2ic, ChunkMetadata> metadata) {
			super(position);
			this.path = Objects.requireNonNull(path);
			this.metadata = Objects.requireNonNull(metadata);

		}

		@Override
		public BufferedImage getImage() throws IOException {
			return ImageIO.read(path.toURL().openStream());
		}

		@Override
		public Map<? extends Vector2ic, ChunkMetadata> getChunkMetadata() {
			return metadata;
		}

		public URI getURI() {
			return path;
		}
	}

	public static class LocalSavedRegion extends SavedRegion {

		protected Path path;

		public LocalSavedRegion(Vector2ic position, Path path, Map<? extends Vector2ic, ChunkMetadata> metadata) {
			super(position, path.toUri(), metadata);
			this.path = path;
		}

		public Path getPath() {
			return path;
		}
	}
}
