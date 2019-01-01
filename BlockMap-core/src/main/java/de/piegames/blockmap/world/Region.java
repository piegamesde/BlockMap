package de.piegames.blockmap.world;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.imageio.ImageIO;

import org.joml.Vector2i;
import org.joml.Vector2ic;

public abstract class Region {
	protected Vector2ic position;

	protected Region(Vector2ic position) {
		this.position = position;
	}

	public Vector2ic getPosition() {
		return position;
	}

	public abstract BufferedImage getImage() throws IOException;

	public abstract Map<Vector2ic, ChunkMetadata> getChunkMetadata();

	public static class BufferedRegion extends Region {

		protected BufferedImage					image;
		protected Map<Vector2ic, ChunkMetadata>	metadata;

		public BufferedRegion(Vector2ic position, BufferedImage image, Map<Vector2ic, ChunkMetadata> metadata) {
			super(position);
			this.image = Objects.requireNonNull(image);
			this.metadata = Objects.requireNonNull(metadata);
		}

		@Override
		public BufferedImage getImage() throws IOException {
			return image;
		}

		@Override
		public Map<Vector2ic, ChunkMetadata> getChunkMetadata() {
			return metadata;
		}

		public SavedRegion save(Path out) throws IOException {
			ImageIO.write(image, "png", Files.newOutputStream(out));
			return new SavedRegion(position, out.toUri(), metadata);
		}
	}

	public static class SavedRegion extends Region {

		protected URI							path;
		protected Map<Vector2ic, ChunkMetadata>	metadata;

		public SavedRegion(Vector2ic position, URI path, ChunkMetadata[] metadata) {
			super(position);
			this.path = path;
			this.metadata = IntStream.range(0, 32 * 32)
					.mapToObj(Integer::valueOf)
					.collect(Collectors.toMap(
							i -> new Vector2i(position.x() + (i & 15), position.y() + (i >> 4)),
							i -> metadata[i]));

		}

		public SavedRegion(Vector2ic position, URI path, Map<Vector2ic, ChunkMetadata> metadata) {
			super(position);
			this.path = Objects.requireNonNull(path);
			this.metadata = Objects.requireNonNull(metadata);

		}

		@Override
		public BufferedImage getImage() throws IOException {
			return ImageIO.read(path.toURL().openStream());
		}

		@Override
		public Map<Vector2ic, ChunkMetadata> getChunkMetadata() {
			return metadata;
		}

		public URI getPath() {
			return path;
		}
	}
}
