package de.piegames.blockmap.world;

import org.joml.Vector2ic;

public class ChunkMetadata {

	public static enum ChunkRenderState {
		RENDERED, FAILED, CULLED, TOO_OLD, TOO_NEW, NOT_GENERATED;
	}

	/** Relative to the world */
	public Vector2ic		position;
	public ChunkRenderState	renderState;
	public boolean			slimeChunk;

	public ChunkMetadata(Vector2ic position, ChunkRenderState renderState) {
		this.position = position;
		this.renderState = renderState;
		this.slimeChunk = false;
	}
}
