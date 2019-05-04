package de.piegames.blockmap.color;

import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Base64;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

import de.piegames.blockmap.MinecraftVersion;
import de.piegames.blockmap.renderer.Block;
import io.gsonfire.GsonFireBuilder;
import io.gsonfire.TypeSelector;
import io.gsonfire.annotations.ExcludeByValue;
import io.gsonfire.annotations.PostDeserialize;
import io.gsonfire.annotations.PreSerialize;
import io.gsonfire.gson.ExclusionByValueStrategy;

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
		DEFAULT("default"), CAVES("caves"), NO_FOLIAGE("foliage"), OCEAN_GROUND("water");
		private String fileName;

		InternalColorMap(String fileName) {
			this.fileName = Objects.requireNonNull(fileName);
		}

		public Map<MinecraftVersion, BlockColorMap> getColorMap() {
			return Arrays.stream(MinecraftVersion.values())
					.collect(Collectors.toMap(Function.identity(), version -> BlockColorMap.loadInternal(fileName, version)));
		}
	}

	public static final Gson GSON = new GsonFireBuilder()
			.enableHooks(NormalStateColors.class)
			.enableHooks(BlockColor.class)
			.enableHooks(BlockColorMap.class)
			.enableExclusionByValue()
			.registerTypeSelector(StateColors.class, new TypeSelector<StateColors>() {

				@Override
				public Class<? extends StateColors> getClassForElement(JsonElement readElement) {
					return readElement.getAsJsonObject().has("color") ? SingleStateColors.class
							: NormalStateColors.class;
				}
			})
			.createGsonBuilder()
			.serializeNulls()
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

	public static class BlockColor {
		public static final BlockColor	MISSING		= new BlockColor(Color.MISSING, false, false, false, false);
		public static final BlockColor	TRANSPARENT	= new BlockColor(Color.TRANSPARENT, false, false, false, true);

		public Color					color;
		/** Tell if a given block has a grassy surface which should be tainted according to the biome the block stands in */
		public transient boolean		isGrass;
		/** Tell if a given block represents foliage and should be tainted according to the biome the block stands in */
		public transient boolean		isFoliage;
		/** Tell if a given block contains water and should be tainted according to the biome the block stands in */
		public transient boolean		isWater;
		/** Tell if a given block is letting light through and thus will not count to any height shading calculations */
		public transient boolean		isTranslucent;

		public static class ExcludeZero implements ExclusionByValueStrategy<Integer> {

			@Override
			public boolean shouldSkipField(Integer fieldValue) {
				return fieldValue == 0;
			}
		}

		@ExcludeByValue(value = ExcludeZero.class)
		public int isGFWT;

		public BlockColor() {

		}

		public BlockColor(Color color, boolean isGrass, boolean isFoliage, boolean isWater, boolean isTranslucent) {
			this.color = color;
			this.isGrass = isGrass;
			this.isFoliage = isFoliage;
			this.isWater = isWater;
			this.isTranslucent = isTranslucent;
			preSerialize();
		}

		@PostDeserialize
		private void postDeserialize() {
			isGrass = (isGFWT & 8) != 0;
			isFoliage = (isGFWT & 4) != 0;
			isWater = (isGFWT & 2) != 0;
			isTranslucent = (isGFWT & 1) != 0;
		}

		@PreSerialize
		private void preSerialize() {
			isGFWT = (isGrass ? 8 : 0)
					| (isFoliage ? 4 : 0)
					| (isWater ? 2 : 0)
					| (isTranslucent ? 1 : 0);
		}

		@Override
		public String toString() {
			return "BlockColor [color=" + color + ", isGrass=" + isGrass + ", isFoliage=" + isFoliage + ", isWater=" + isWater + ", isTranslucent="
					+ isTranslucent + "]";
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((color == null) ? 0 : color.hashCode());
			result = prime * result + isGFWT;
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
			BlockColor other = (BlockColor) obj;
			return equals(other);
		}

		public boolean equals(BlockColor other) {
			if (isGFWT != other.isGFWT)
				return false;
			if (color == null) {
				if (other.color != null)
					return false;
			} else if (!color.equals(other.color))
				return false;
			return true;
		}
	}

	public static interface StateColors {
		public abstract BlockColor getColor(BitSet state);

		public default BlockColor getColor(Supplier<BitSet> state) {
			return getColor(state.get());
		};

		public abstract boolean hasColor(BitSet state);
	}

	public static class NormalStateColors implements StateColors {

		transient Map<BitSet, BlockColor>	blockColors;
		Map<String, BlockColor>				blockColors2;

		@SuppressWarnings("unused")
		private NormalStateColors() {

		}

		public NormalStateColors(Map<BitSet, BlockColor> blockColors) {
			this.blockColors = Objects.requireNonNull(blockColors);
		}

		@Override
		public BlockColor getColor(BitSet state) {
			return blockColors.getOrDefault(state, BlockColor.MISSING);
		}

		@Override
		public boolean hasColor(BitSet state) {
			return blockColors.containsKey(state);
		}

		@PostDeserialize
		private void postDeserialize() {
			blockColors = new HashMap<>(blockColors2.size() * 2, 0.51f);
			blockColors2.forEach((k, v) -> blockColors.put(BitSet.valueOf(Base64.getDecoder().decode(k)), v));
			blockColors2 = null;
		}

		@PreSerialize
		private void preSerialize() {
			blockColors2 = new HashMap<>();
			blockColors.forEach((k, v) -> blockColors2.put(Base64.getEncoder().encodeToString(k.toByteArray()), v));
		}

		/** Use for testing only */
		public Map<BitSet, BlockColor> getColors() {
			return new HashMap<>(blockColors);
		}
	}

	public static class SingleStateColors extends BlockColor implements StateColors {

		@SuppressWarnings("unused")
		private SingleStateColors() {
		}

		public SingleStateColors(BlockColor color) {
			super(color.color, color.isGrass, color.isFoliage, color.isWater, color.isTranslucent);
		}

		@Override
		public BlockColor getColor(BitSet state) {
			return this;
		}

		@Override
		public BlockColor getColor(Supplier<BitSet> state) {
			return this;
		}

		@Override
		public boolean hasColor(BitSet state) {
			return true;
		}
	}

	protected Map<String, StateColors>		blockColors;
	protected transient BlockColor			airColor;
	protected transient final StateColors	missing	= new SingleStateColors(BlockColor.MISSING) {
														@Override
														public boolean hasColor(BitSet state) {
															return false;
														}

													};

	public BlockColorMap(Map<String, StateColors> blockColors) {
		this.blockColors = Objects.requireNonNull(blockColors);
	}

	public BlockColor getBlockColor(String blockName, BitSet blockState) {
		return blockColors.getOrDefault(blockName, missing).getColor(blockState);
	}

	public BlockColor getBlockColor(String blockName, Supplier<BitSet> blockState) {
		return blockColors.getOrDefault(blockName, missing).getColor(blockState);
	}

	public boolean hasBlockColor(String blockName, BitSet blockState) {
		return blockColors.containsKey(blockName) && blockColors.get(blockName).hasColor(blockState);
	}

	@PostDeserialize
	private void postDeserialize() {
		HashMap<String, StateColors> blockColors = new HashMap<>(this.blockColors.size() * 2, 0.51f);
		blockColors.putAll(this.blockColors);
		this.blockColors = blockColors;
	}

	/** This is a common operation so avoid retrieving it from the map every time. */
	public BlockColor getAirColor() {
		if (airColor == null)
			return airColor = getBlockColor("minecraft:air", Block.STATE_NONE);
		else
			return airColor;
	}

	/** Use for testing only */
	public Map<String, StateColors> getBlockColors() {
		return new HashMap<>(blockColors);
	}

	public static BlockColorMap load(Reader reader) {
		return GSON.fromJson(reader, BlockColorMap.class);
	}

	public static BlockColorMap loadInternal(String name, MinecraftVersion version) {
		try {
			return load(new InputStreamReader(BlockColorMap.class.getResourceAsStream("/block-colors-" + name + "-" + version.fileSuffix + ".json")));
		} catch (NullPointerException e) {
			throw new IllegalArgumentException("Did not find internal color map " + name, e);
		}
	}
}
