package de.piegames.blockmap.color;

import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;

import de.piegames.blockmap.renderer.Block;
import io.gsonfire.GsonFireBuilder;
import io.gsonfire.annotations.PostDeserialize;
import io.gsonfire.annotations.PreSerialize;

public class BlockColorMap {
	public static final Gson		GSON	= new GsonFireBuilder()
			.enableHooks(BlockColorMap.class)
			.createGsonBuilder()
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
			.disableHtmlEscaping()
			.create();
	public static final BlockColor	MISSING	= new BlockColor(Color.MISSING, false, false, false, false);

	public static final class BlockColor {

		public Color	color;
		public boolean	isGrass, isFoliage, isWater, isTranslucent;

		public BlockColor() {

		}

		public BlockColor(Color color, boolean isGrass, boolean isFoliage, boolean isWater, boolean isTranslucent) {
			this.color = color;
			this.isGrass = isGrass;
			this.isFoliage = isFoliage;
			this.isWater = isWater;
			this.isTranslucent = isTranslucent;
		}
	}

	protected transient Map<Block, BlockColor>	blockColors;
	/*
	 * Use this map instead of blockColors for serialization, because the keys are primitive (String). A serialization hook will convert between
	 * the two map forms if needed.
	 */
	protected Map<String, BlockColor>			blockSerialize;

	@SuppressWarnings("unused")
	private BlockColorMap() {
		// For deserialization purposes
	}

	public BlockColorMap(Map<Block, BlockColor> blockColors) {
		this.blockColors = Objects.requireNonNull(blockColors);
	}

	@Deprecated
	public BlockColorMap(Map<Block, Color> blockColors, Set<Block> grassBlocks, Set<Block> foliageBlocks, Set<Block> waterBlocks,
			Set<Block> translucentBlocks) {
		this.blockColors = new HashMap<>();
		blockColors.forEach((b, c) -> {
			this.blockColors.put(b, new BlockColor(c,
					grassBlocks.contains(b),
					foliageBlocks.contains(b),
					waterBlocks.contains(b),
					translucentBlocks.contains(b)));
		});
	}

	@PreSerialize
	public void preSerializeLogic() {
		blockSerialize = blockColors.entrySet().stream().collect(Collectors.toMap(e -> e.getKey().toString(), e -> e.getValue()));
	}

	@PostDeserialize
	public void postDeserializeLogic() {
		blockColors = blockSerialize.entrySet().stream().collect(Collectors.toMap(e -> Block.byCompactForm(e.getKey()).get(0), e -> e.getValue()));
		blockSerialize = null;
	}

	public Color getBlockColor(Block block) {
		return blockColors.getOrDefault(block, MISSING).color;
	}

	public boolean hasBlockColor(Block block) {
		return blockColors.containsKey(block);
	}

	/** Tell if a given block has a grassy surface which should be tainted according to the biome the block stands in */
	public boolean isGrassBlock(Block block) {
		return blockColors.getOrDefault(block, MISSING).isGrass;
	}

	/** Tell if a given block represents foliage and should be tainted according to the biome the block stands in */
	public boolean isFoliageBlock(Block block) {
		return blockColors.getOrDefault(block, MISSING).isFoliage;
	}

	/** Tell if a given block contains water and should be tainted according to the biome the block stands in */
	public boolean isWaterBlock(Block block) {
		return blockColors.getOrDefault(block, MISSING).isWater;
	}

	/** Tell if a given block is letting light through and thus will not count to any height shading calculations */
	public boolean isTranslucentBlock(Block block) {
		return blockColors.getOrDefault(block, MISSING).isTranslucent;
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
}
