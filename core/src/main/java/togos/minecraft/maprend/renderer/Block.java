package togos.minecraft.maprend.renderer;

import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** Each object of this class represents an existing Minecraft block, with its ID and block state, but without its position or object data. */
public class Block {

	/** The name/id of the block including the namespace, like in Minecraft. Example: minecraft:air */
	public final String					name;
	/** A set of all block states this block has. The usage of an EnumSet is supposed to increase performance */
	public final EnumSet<BlockState>	state;

	public Block(String name) {
		this(name, BlockState.NONE);
	}

	public Block(String name, EnumSet<BlockState> state) {
		this.name = Objects.requireNonNull(name);
		this.state = state;
	}

	public static List<Block> byCompactForm(String name) {
		String[] subs = name.split(",");
		List<EnumSet<BlockState>> blocks = new LinkedList<>();
		blocks.add(EnumSet.noneOf(BlockState.class));
		for (int i = 1; i < subs.length; i++) {
			String key = subs[i].split("=")[0];
			String val = subs[i].substring(subs[i].indexOf("=") + 1);
			if (val.equals("*")) {
				List<EnumSet<BlockState>> oldBlocks = blocks;
				blocks = new LinkedList<>();
				for (BlockState newState : BlockState.allowedStates(subs[0], key)) {
					for (EnumSet<BlockState> oldState : oldBlocks) {
						EnumSet<BlockState> toAdd = EnumSet.copyOf(oldState);
						toAdd.add(newState);
						blocks.add(toAdd);
					}
				}
			} else
				blocks.stream().forEach(set -> set.add(BlockState.valueOf(key, val)));
		}
		return blocks.stream().map(state -> new Block(subs[0], state)).collect(Collectors.toList());
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(name);
		for (BlockState b : state)
			builder.append("," + b.name + "=" + b.value);
		return builder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((state == null) ? 0 : state.hashCode());
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
		Block other = (Block) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (state == null) {
			if (other.state != null)
				return false;
		} else if (!state.equals(other.state))
			return false;
		return true;
	}
}