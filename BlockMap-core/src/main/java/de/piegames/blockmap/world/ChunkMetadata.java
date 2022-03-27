package de.piegames.blockmap.world;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.joml.Vector2ic;
import org.joml.Vector3ic;

import io.gsonfire.annotations.ExposeMethodResult;

/**
 * When rendering a chunk, in addition to the resulting image there is also some meta data being generated. This includes information about
 * the chunk (generation status and contained structures) as well as about the render process (did it fail?). This will be used later on to
 * create the so-called <i>dynamic pins</i> in the GUI, but may come in handy for other uses as well.
 */
public abstract class ChunkMetadata {
	/**
	 * The render state represents one of the possible outcomes while trying to render a chunk in a region file. Unless the state of a chunk is
	 * {@value #RENDERED}, the resulting image won't contain any rendered pixel data.
	 */
	public static enum ChunkRenderState {
		/** Everything went fine */
		RENDERED(ChunkMetadataRendered.class),
		/** An exception occurred while rendering that chunk */
		FAILED(ChunkMetadataFailed.class),
		/** The chunk is (completely) outside the bounds specified by the render settings */
		CULLED(ChunkMetadataCulled.class),
		/** The chunk has a version id that is either too old or that could not be parsed due to some other reason */
		TOO_OLD(ChunkMetadataVersion.class);

		public final Class<? extends ChunkMetadata> clazz;

		ChunkRenderState(Class<? extends ChunkMetadata> clazz) {
			this.clazz = clazz;
		}
	}

	public static interface ChunkMetadataVisitor<T> {
		public T rendered(ChunkMetadataRendered metadata);

		public T failed(ChunkMetadataFailed metadata);

		public T culled(ChunkMetadataCulled metadata);

		public T version(ChunkMetadataVersion metadata);
	}

	public static class ChunkMetadataRendered extends ChunkMetadata {
		/** If the generation status is one of these, the chunk is empty and should be skipped directly */
		public static final Set<String>		STATUS_EMPTY	= Collections.singleton("empty");
		/**
		 * If the generation status is one of these, the chunk has been generated far enough for it to be fully rendered. There are multiple values
		 * because of Minecraft version changes.
		 */
		public static final Set<String>		STATUS_FINISHED	= Stream.of("full", "postprocessed").collect(Collectors.toUnmodifiableSet());
		public final String					generationStatus;

		/**
		 * Map each structure type to its position. The position is the centroid of the bounding box. There is at most one structure of each type
		 * per chunk.
		 */
		public final Map<String, Vector3ic>	structures;

		public ChunkMetadataRendered(Vector2ic position, String generationStatus) {
			this(position, generationStatus, Collections.emptyMap());
		}

		public ChunkMetadataRendered(Vector2ic position, String generationStatus, Map<String, Vector3ic> structures) {
			super(position);
			this.generationStatus = Objects.requireNonNull(generationStatus);
			this.structures = structures;
		}

		@Override
		public ChunkRenderState getRenderState() {
			return ChunkRenderState.RENDERED;
		}

		@Override
		public <T> T visit(ChunkMetadataVisitor<T> visitor) {
			return visitor.rendered(this);
		}

	}

	public static class ChunkMetadataFailed extends ChunkMetadata {
		public final Exception error;

		public ChunkMetadataFailed(Vector2ic position, Exception error) {
			super(position);
			this.error = error;
		}

		@Override
		public ChunkRenderState getRenderState() {
			return ChunkRenderState.FAILED;
		}

		@Override
		public <T> T visit(ChunkMetadataVisitor<T> visitor) {
			return visitor.failed(this);
		}
	}

	public static class ChunkMetadataCulled extends ChunkMetadata {

		public ChunkMetadataCulled(Vector2ic position) {
			super(position);
		}

		@Override
		public ChunkRenderState getRenderState() {
			return ChunkRenderState.CULLED;
		}

		@Override
		public <T> T visit(ChunkMetadataVisitor<T> visitor) {
			return visitor.culled(this);
		}
	}

	public static class ChunkMetadataVersion extends ChunkMetadata {
		public final int	version;
		public final String	message;

		public ChunkMetadataVersion(Vector2ic position, String message, int version) {
			super(position);
			this.version = version;
			this.message = message;
		}

		@Override
		public ChunkRenderState getRenderState() {
			return ChunkRenderState.TOO_OLD;
		}

		@Override
		public <T> T visit(ChunkMetadataVisitor<T> visitor) {
			return visitor.version(this);
		}
	}

	/** Chunk coordinates relative to the world */
	public final Vector2ic position;

	public ChunkMetadata(Vector2ic position) {
		this.position = Objects.requireNonNull(position);
	}

	@ExposeMethodResult(value = "renderState")
	public abstract ChunkRenderState getRenderState();

	public abstract <T> T visit(ChunkMetadataVisitor<T> visitor);
}
