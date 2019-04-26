package de.piegames.blockmap.world;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import org.joml.Vector2ic;
import org.joml.Vector3ic;

/**
 * When rendering a chunk, in addition to the resulting image there is also some meta data being generated. This includes information about
 * the chunk (generation status and contained structures) as well as about the render process (did it fail?). This will be used later on to
 * create the so-called <i>dynamic pins</i> in the GUI, but may come in handy for other uses as well.
 */
public class ChunkMetadata {

	/**
	 * The render state represents one of the possible outcomes while trying to render a chunk in a region file. Unless the state of a chunk is
	 * {@value #RENDERED}, the resulting image won't contain any rendered pixel data.
	 */
	public static enum ChunkRenderState {
		/** Everything went fine */
		RENDERED,
		/** An exception occurred while rendering that chunk */
		FAILED,
		/** The chunk is outside the bounds specified by the render settings */
		CULLED,
		/** The chunk still has the old pre-1.13 format */
		TOO_OLD;
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
			switch (name) {
			case "empty":
				return EMPTY;
			case "base":
				return BASE;
			case "carved":
				return CARVED;
			case "liquid_carved":
				return LIQUID_CARVED;
			case "decorated":
				return DECORATED;
			case "lighted":
				return LIGHTED;
			case "mobs_spawned":
				return MOBS_SPAWNED;
			case "finalized":
				return FINALIZED;
			case "fullchunk":
				return FULLCHUNK;
			case "postprocessed":
				return POSTPROCESSED;
			default:
				return null;
			}
		}
	}

	/** Chunk coordinates relative to the world */
	public final Vector2ic				position;
	public final ChunkRenderState		renderState;
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

	public ChunkMetadata(Vector2ic position, ChunkRenderState renderState, ChunkGenerationStatus generationStatus) {
		this(position, renderState, generationStatus, Collections.emptyMap());
	}

	public ChunkMetadata(Vector2ic position, ChunkRenderState renderState, ChunkGenerationStatus generationStatus, Map<String, Vector3ic> structures) {
		this.position = Objects.requireNonNull(position);
		this.renderState = Objects.requireNonNull(renderState);
		this.generationStatus = generationStatus;
		this.structures = Collections.unmodifiableMap(structures);
	}
}
