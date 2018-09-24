package de.piegames.blockmap.color;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import de.piegames.blockmap.renderer.Block;

public class BlockColorMap {

	public static final TypeAdapter<BlockColorMap>	ADAPTER	= new TypeAdapter<BlockColorMap>() {

																@Override
																public void write(JsonWriter out, BlockColorMap value) throws IOException {
																	out.beginObject();
																	out.name("blockColors");
																	out.beginObject();
																	for (Entry<Block, Color> entry : value.blockColors.entrySet()) {
																		out.name(entry.getKey().toString());
																		out.beginArray();
																		out.value(Float.floatToIntBits(entry.getValue().a));
																		out.value(Float.floatToIntBits(entry.getValue().r));
																		out.value(Float.floatToIntBits(entry.getValue().g));
																		out.value(Float.floatToIntBits(entry.getValue().b));
																		out.endArray();
																	}
																	out.endObject();
																	out.name("grassBlocks");
																	out.beginArray();
																	for (Block block : value.grassBlocks)
																		out.value(block.toString());
																	out.endArray();
																	out.name("foliageBlocks");
																	out.beginArray();
																	for (Block block : value.foliageBlocks)
																		out.value(block.toString());
																	out.endArray();
																	out.name("waterBlocks");
																	out.beginArray();
																	for (Block block : value.waterBlocks)
																		out.value(block.toString());
																	out.endArray();
																	out.name("translucentBlocks");
																	out.beginArray();
																	for (Block block : value.translucentBlocks)
																		out.value(block.toString());
																	out.endArray();

																	out.endObject();
																}

																@Override
																public BlockColorMap read(JsonReader in) throws IOException {
																	Map<Block, Color> blockColors = new HashMap<>();
																	Set<Block> grassBlocks = new HashSet<>();
																	Set<Block> foliageBlocks = new HashSet<>();
																	Set<Block> waterBlocks = new HashSet<>();
																	Set<Block> translucentBlocks = new HashSet<>();

																	in.beginObject();
																	// blockColors
																	in.skipValue();
																	in.beginObject();
																	while (in.hasNext()) {
																		String key = in.nextName();
																		in.beginArray();
																		Color color = new Color(
																				Float.intBitsToFloat(in.nextInt()),
																				Float.intBitsToFloat(in.nextInt()),
																				Float.intBitsToFloat(in.nextInt()),
																				Float.intBitsToFloat(in.nextInt()));
																		Block block = Block.byCompactForm(key).get(0);
																		blockColors.put(block, color);
																		in.endArray();
																	}
																	in.endObject();
																	// gassBlocks
																	in.skipValue();
																	in.beginArray();
																	while (in.hasNext())
																		grassBlocks.add(Block.byCompactForm(in.nextString()).get(0));
																	in.endArray();
																	// foliageBlocks
																	in.skipValue();
																	in.beginArray();
																	while (in.hasNext())
																		foliageBlocks.add(Block.byCompactForm(in.nextString()).get(0));
																	in.endArray();
																	// waterBlocks
																	in.skipValue();
																	in.beginArray();
																	while (in.hasNext())
																		waterBlocks.add(Block.byCompactForm(in.nextString()).get(0));
																	in.endArray();
																	// translucentBlocks
																	in.skipValue();
																	in.beginArray();
																	while (in.hasNext())
																		translucentBlocks.add(Block.byCompactForm(in.nextString()).get(0));
																	in.endArray();

																	in.endObject();
																	return new BlockColorMap(blockColors, grassBlocks, foliageBlocks, waterBlocks,
																			translucentBlocks);
																}
															};
	public static final Gson						GSON	= new GsonBuilder().registerTypeAdapter(BlockColorMap.class, ADAPTER).disableHtmlEscaping()
			.create();

	// TODO combine into new class
	protected Map<Block, Color>						blockColors;
	protected Set<Block>							grassBlocks;
	protected Set<Block>							foliageBlocks;
	protected Set<Block>							waterBlocks;
	protected Set<Block>							translucentBlocks;

	@SuppressWarnings("unused")
	private BlockColorMap() {
		// For deserialization purposes
	}

	public BlockColorMap(Map<Block, Color> blockColors, Set<Block> grassBlocks, Set<Block> foliageBlocks, Set<Block> waterBlocks,
			Set<Block> translucentBlocks) {
		this.blockColors = Collections.unmodifiableMap(blockColors);
		this.grassBlocks = Collections.unmodifiableSet(grassBlocks);
		this.foliageBlocks = Collections.unmodifiableSet(foliageBlocks);
		this.waterBlocks = Collections.unmodifiableSet(waterBlocks);
		this.translucentBlocks = Collections.unmodifiableSet(translucentBlocks);
	}

	public Color getBlockColor(Block block) {
		return blockColors.getOrDefault(block, Color.MISSING);
	}

	public boolean hasBlockColor(Block block) {
		return blockColors.containsKey(block);
	}

	/** Tell if a given block has a grassy surface which should be tainted according to the biome the block stands in */
	public boolean isGrassBlock(Block block) {
		return grassBlocks.contains(block);
	}

	/** Tell if a given block represents foliage and should be tainted according to the biome the block stands in */
	public boolean isFoliageBlock(Block block) {
		return foliageBlocks.contains(block);
	}

	/** Tell if a given block contains water and should be tainted according to the biome the block stands in */
	public boolean isWaterBlock(Block block) {
		return waterBlocks.contains(block);
	}

	/** Tell if a given block is letting light through and thus will not count to any height shading calculations */
	public boolean isTranslucentBlock(Block block) {
		return translucentBlocks.contains(block);
	}

	public static BlockColorMap load(Reader reader) {
		return GSON.fromJson(reader, BlockColorMap.class);
	}

	public static BlockColorMap loadDefault() {
		return loadInternal("default");
	}

	public static BlockColorMap loadInternal(String name) {
		try {
			return load(new InputStreamReader(BlockColorMap.class.getResourceAsStream("/block-colors-" + name + ".json")));
		} catch (NullPointerException e) {
			throw new IllegalArgumentException("Did not find internal color map " + name, e);
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((blockColors == null) ? 0 : blockColors.hashCode());
		result = prime * result + ((foliageBlocks == null) ? 0 : foliageBlocks.hashCode());
		result = prime * result + ((grassBlocks == null) ? 0 : grassBlocks.hashCode());
		result = prime * result + ((waterBlocks == null) ? 0 : waterBlocks.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BlockColorMap other = (BlockColorMap) obj;
		if (blockColors == null) {
			if (other.blockColors != null)
				return false;
		} else if (!blockColors.equals(other.blockColors))
			return false;
		if (foliageBlocks == null) {
			if (other.foliageBlocks != null)
				return false;
		} else if (!foliageBlocks.equals(other.foliageBlocks))
			return false;
		if (grassBlocks == null) {
			if (other.grassBlocks != null)
				return false;
		} else if (!grassBlocks.equals(other.grassBlocks))
			return false;
		if (waterBlocks == null) {
			if (other.waterBlocks != null)
				return false;
		} else if (!waterBlocks.equals(other.waterBlocks))
			return false;
		return true;
	}
}
