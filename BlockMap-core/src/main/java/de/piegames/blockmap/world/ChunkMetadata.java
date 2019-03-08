package de.piegames.blockmap.world;

import java.util.Collections;
import java.util.Map;

import org.joml.Vector2ic;
import org.joml.Vector3ic;

public class ChunkMetadata {

	public static enum ChunkRenderState {
		RENDERED, FAILED, CULLED, TOO_OLD, NOT_GENERATED;
	}

	/** Relative to the world */
	public final Vector2ic				position;
	public final ChunkRenderState		renderState;
	public final Map<String, Vector3ic>	structures;

	public ChunkMetadata(Vector2ic position, ChunkRenderState renderState) {
		this(position, renderState, Collections.emptyMap());
	}

	public ChunkMetadata(Vector2ic position, ChunkRenderState renderState, Map<String, Vector3ic> structures) {
		this.position = position;
		this.renderState = renderState;
		this.structures = Collections.unmodifiableMap(structures);
	}
}
