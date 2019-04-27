package de.piegames.blockmap.color;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Modifier;
import java.util.Base64;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import de.piegames.blockmap.MinecraftVersion;
import de.piegames.blockmap.renderer.Block;
import io.gsonfire.GsonFireBuilder;

/**
 * Represents a mapping from block states to properties that are used to calculate this block's color. These properties are: base color, if
 * the block is a grass/foliage/water block and if the block is translucent. The base color usually is the average color of its texture, but
 * this is not a requirement. If the block is a grass/foliage/water block, its color will be multiplied with the respective colors of the
 * biome the block is in. The grass/foliage/water properties are independent from each other, a block may as well be both (even if the
 * result may not look that well).
 * 
 * The translucency property has no correlation with the transparency of that block (which is encoded in its base color) nor with any
 * properties directly related to Minecraft internals. See {@link #isTranslucentBlock(Block)} for more details.
 * 
 * @author piegames
 */
public class BlockColorMap {

	public static enum InternalColorMap {
		DEFAULT("default"), NO_FOLIAGE("foliage"), OCEAN_GROUND("water"), CAVES("caves");
		private String fileName;

		InternalColorMap(String fileName) {
			this.fileName = Objects.requireNonNull(fileName);
		}

		public BlockColorMap getColorMap(MinecraftVersion version) {
			return BlockColorMap.loadInternal(fileName, version);
		}
	}

	private static final TypeAdapter<BlockColorMap>	ADAPTER	= new TypeAdapter<BlockColorMap>() {

																@Override
																public void write(JsonWriter out, BlockColorMap value) throws IOException {
																	out.beginObject();
																	for (Map.Entry<String, Map<BitSet, BlockColor>> blockEntry : value.blockColors.entrySet()) {
																		out.name(blockEntry.getKey());
																		out.beginObject();
																		for (Map.Entry<BitSet, BlockColor> entry : blockEntry.getValue().entrySet()) {
																			out.name(Base64.getEncoder().encodeToString(entry.getKey()
																					.toByteArray()));
																			BlockColor.ADAPTER.write(out, entry.getValue());
																		}
																		out.endObject();
																	}
																	out.endObject();
																}

																@Override
																public BlockColorMap read(JsonReader in) throws IOException {
																	Map<String, Map<BitSet, BlockColor>> colors = new HashMap<>(300, 0.5f);
																	in.beginObject();
																	while (in.hasNext()) {
																		String blockName = in.nextName();
																		Map<BitSet, BlockColor> map = new HashMap<>(20, 0.5f);
																		in.beginObject();
																		while (in.hasNext()) {
																			BitSet state = BitSet.valueOf(Base64.getDecoder().decode(in.nextName()));
																			BlockColor color = BlockColor.ADAPTER.read(in);
																			map.put(state, color);
																		}
																		colors.put(blockName, map);
																		in.endObject();
																	}
																	in.endObject();
																	return new BlockColorMap(colors);
																}
															};

	public static final Gson						GSON	= new GsonFireBuilder()
			.createGsonBuilder()
			.serializeNulls()
			.setPrettyPrinting()
			.addSerializationExclusionStrategy(new ExclusionStrategy() {

																		@Override
																		public boolean shouldSkipField(FieldAttributes f) {
																			return f.hasModifier(Modifier.TRANSIENT);
																		}

																		@Override
																		public boolean shouldSkipClass(Class<?> clazz) {
																			return false;
																		}
																	})
			.registerTypeAdapter(Color.class, Color.ADAPTER)
			.registerTypeAdapter(BlockColor.class, BlockColor.ADAPTER)
			.registerTypeAdapter(BlockColorMap.class, ADAPTER)
			.disableHtmlEscaping()
			.create();
	public static final BlockColor					MISSING	= new BlockColor(Color.MISSING, false, false, false, false);

	public static final class BlockColor {

		private static final TypeAdapter<BlockColor>	ADAPTER	= new TypeAdapter<BlockColorMap.BlockColor>() {

																	@Override
																	public void write(JsonWriter out, BlockColor value) throws IOException {
																		out.beginObject();
																		out.name("color");
																		Color.ADAPTER.write(out, value.color);
																		out.name("isGFWT");
																		out.value((value.isGrass ? 8 : 0) | (value.isFoliage ? 4 : 0) | (value.isWater ? 2 : 0)
																				| (value.isTranslucent ? 1 : 0));
																		out.endObject();
																	}

																	@Override
																	public BlockColor read(JsonReader in) throws IOException {
																		BlockColor color = new BlockColor();
																		in.beginObject();
																		in.nextName();
																		color.color = Color.ADAPTER.read(in);
																		in.nextName();
																		int value = in.nextInt();
																		color.isGrass = (value & 8) != 0;
																		color.isFoliage = (value & 4) != 0;
																		color.isWater = (value & 2) != 0;
																		color.isTranslucent = (value & 1) != 0;
																		in.endObject();
																		return color;
																	}
																};

		public Color									color;
		/** Tell if a given block has a grassy surface which should be tainted according to the biome the block stands in */
		public boolean									isGrass;
		/** Tell if a given block represents foliage and should be tainted according to the biome the block stands in */
		public boolean									isFoliage;
		/** Tell if a given block contains water and should be tainted according to the biome the block stands in */
		public boolean									isWater;
		/** Tell if a given block is letting light through and thus will not count to any height shading calculations */
		public boolean									isTranslucent;

		public BlockColor() {

		}

		public BlockColor(Color color, boolean isGrass, boolean isFoliage, boolean isWater, boolean isTranslucent) {
			this.color = color;
			this.isGrass = isGrass;
			this.isFoliage = isFoliage;
			this.isWater = isWater;
			this.isTranslucent = isTranslucent;
		}

		@Override
		public String toString() {
			return "BlockColor [color=" + color + ", isGrass=" + isGrass + ", isFoliage=" + isFoliage + ", isWater=" + isWater + ", isTranslucent="
					+ isTranslucent + "]";
		}
	}

	protected Map<String, Map<BitSet, BlockColor>>	blockColors;
	protected transient Color						airColor;

	public BlockColorMap(Map<String, Map<BitSet, BlockColor>> blockColors) {
		this.blockColors = Objects.requireNonNull(blockColors);
	}

	public BlockColor getBlockColor(String blockName, BitSet blockState) {
		return blockColors.getOrDefault(blockName, Collections.emptyMap()).getOrDefault(blockState, MISSING);
	}

	public boolean hasBlockColor(String blockName, BitSet blockState) {
		return blockColors.containsKey(blockName) && blockColors.get(blockName).containsKey(blockState);
	}

	/** This is a common operation so avoid retrieving it from the map every time. */
	public Color getAirColor() {
		if (airColor == null)
			return airColor = getBlockColor("minecraft:air", Block.STATE_NONE).color;
		else
			return airColor;
	}

	/** Use for testing only */
	public Map<String, Map<BitSet, BlockColor>> getBlockColors() {
		/* Explode to stream and collect again to create a semi-deep copy */
		return blockColors.entrySet().stream().collect(Collectors.toMap(e -> e.getKey(), e -> new HashMap<>(e.getValue())));
	}

	public static BlockColorMap load(Reader reader) {
		return GSON.fromJson(reader, BlockColorMap.class);
	}

	public static BlockColorMap loadDefault(MinecraftVersion version) {
		return loadInternal("default", version);
	}

	public static BlockColorMap loadInternal(String name, MinecraftVersion version) {
		try {
			return load(new InputStreamReader(BlockColorMap.class.getResourceAsStream("/block-colors-" + name + "-" + version.fileSuffix + ".json")));
		} catch (NullPointerException e) {
			throw new IllegalArgumentException("Did not find internal color map " + name, e);
		}
	}
}
