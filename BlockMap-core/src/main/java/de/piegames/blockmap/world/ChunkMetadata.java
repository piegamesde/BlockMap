package de.piegames.blockmap.world;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

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

	public static enum ChunkGenerationStatus {
		EMPTY,
		BASE,
		CARVED,
		LIQUID_CARVED,
		DECORATED,
		LIGHTED,
		MOBS_SPAWNED,
		FINALIZED,
		FULLCHUNK,
		POSTPROCESSED;

		/**
		 * Get an enum instance from its name
		 * 
		 * @param name
		 *            The name of the chunk's status, as used in Minecraft saves
		 * @return The corresponding enum value, oder {@code null} if the given string doesn't match
		 */
		public static ChunkGenerationStatus forName(String name) {
			/* Thanks Mojang for simply renaming the values */
			switch (name) {
			case "empty":
				return EMPTY;
			case "base":
				return BASE;
			case "carvers":
			case "carved":
				return CARVED;
			case "liquid_carvers":
			case "liquid_carved":
				return LIQUID_CARVED;
			case "decorated":
				return DECORATED;
			case "light":
			case "lighted":
				return LIGHTED;
			case "mobs_spawned":
				return MOBS_SPAWNED;
			case "finalized":
				return FINALIZED;
			case "full":
			case "fullchunk":
				return FULLCHUNK;
			case "features": // ?
			case "postprocessed":
				return POSTPROCESSED;
			default:
				return null;
			}
		}
	}

	public static interface ChunkMetadataVisitor<T> {
		public T rendered(ChunkMetadataRendered metadata);

		public T failed(ChunkMetadataFailed metadata);

		public T culled(ChunkMetadataCulled metadata);

		public T version(ChunkMetadataVersion metadata);
	}

	public static class ChunkMetadataRendered extends ChunkMetadata {
		/**
		 * May be {@code null} if the chunk didn't get loaded enough to retrieve this information. Must not be {@code null} if {@link #renderState}
		 * is {@link ChunkRenderState#RENDERED}.
		 */
		public final ChunkGenerationStatus	generationStatus;

		/**
		 * Map each structure type to its position. The position is the centroid of the bounding box. There is at most one structure of each type
		 * per chunk.
		 */
		public final Map<String, Vector3ic>	structures;

		public ChunkMetadataRendered(Vector2ic position, ChunkGenerationStatus generationStatus) {
			this(position, generationStatus, Collections.emptyMap());
		}

		public ChunkMetadataRendered(Vector2ic position, ChunkGenerationStatus generationStatus, Map<String, Vector3ic> structures) {
			super(position);
			this.generationStatus = generationStatus;
			this.structures = Collections.unmodifiableMap(structures);
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
