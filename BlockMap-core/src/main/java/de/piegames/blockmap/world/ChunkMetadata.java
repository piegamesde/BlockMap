package de.piegames.blockmap.world;

import org.joml.Vector2ic;

public class ChunkMetadata {

	public static enum ChunkRenderState {
		RENDERED, NOT_RENDERED, FAILED, CULLED, TOO_OLD, TOO_NEW, NOT_GENERATED;
	}

	public Vector2ic		position;
	public ChunkRenderState	renderState	= ChunkRenderState.NOT_RENDERED;
	public boolean			slimeChunk;

	public ChunkMetadata() {
	}
}
