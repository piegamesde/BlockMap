package togos.minecraft.maprend.renderer;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public enum BlockState {

$REPLACE


	public final String name, value;
	public final String[]	allowedBlocks;
	public final Set<String>	allowedBlocks2;

	BlockState(String name, String value, String... allowedBlocks) {
		this.name = name;
		this.value = value;
		this.allowedBlocks = allowedBlocks;
		allowedBlocks2 = new HashSet<>();
		allowedBlocks2.addAll(Arrays.asList(allowedBlocks));
	}

	private static Map<String, EnumSet<BlockState>>	values	= new HashMap<>();
	public static final EnumSet<BlockState> NONE = EnumSet.noneOf(BlockState.class);

	static {
		for (BlockState state : values()) {
			values.putIfAbsent(state.name, EnumSet.noneOf(BlockState.class));
			values.get(state.name).add(state);
		}
	}

	public static BlockState valueOf(String name, String value) {
		try {
			return valueOf(name.toUpperCase() + "_" + value.toUpperCase());
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("State " + name + "=" + value + " does not exist", e);
		}
	}

	/**
	 * Returns all the possible states the given property may have in this block. For example the property "age" will contain the values 0-7 on most crops, 0-15 on
	 * fire and 0-25 on kelp (among others)
	 */
	public static EnumSet<BlockState> allowedStates(String blockName, String propertyName) {
		if (!values.containsKey(propertyName))
			return null;
		EnumSet<BlockState> ret = EnumSet.copyOf(values.get(propertyName));
		ret.removeIf(b -> !b.allowedBlocks2.contains(blockName));
		return ret;
	}
}
