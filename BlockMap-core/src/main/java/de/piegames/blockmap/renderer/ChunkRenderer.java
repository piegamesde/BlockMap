package de.piegames.blockmap.renderer;

import java.util.BitSet;
import java.util.Map.Entry;

import org.joml.Vector2ic;

import de.piegames.blockmap.MinecraftVersion;
import de.piegames.blockmap.color.BlockColorMap;
import de.piegames.blockmap.color.Color;
import de.piegames.blockmap.world.ChunkMetadata;
import de.piegames.nbt.CompoundTag;
import de.piegames.nbt.StringTag;
import de.piegames.nbt.Tag;

abstract class ChunkRenderer {

	final MinecraftVersion	version;
	final RenderSettings	settings;
	BlockColorMap			blockColors;

	ChunkRenderer(MinecraftVersion version, RenderSettings settings) {
		this.version = version;
		this.settings = settings;
	}

	abstract ChunkMetadata renderChunk(Vector2ic chunkPosRegion, Vector2ic chunkPosWorld, CompoundTag level, Color[] map, int[] height, int[] regionBiomes);

	protected static BitSet parseBlockState(CompoundTag properties, BlockState state) {
		BitSet ret = new BitSet(state.getSize());
		if (properties != null)
			for (Entry<String, Tag<?>> entry : properties.getValue().entrySet())
				ret.set(state.getProperty(entry.getKey(), ((StringTag) entry.getValue()).getValue()));
		return ret;
	}

}
