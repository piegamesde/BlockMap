package de.piegames.blockmap.renderer;

import java.util.BitSet;
import java.util.Objects;

/**
 * Each object of this class represents an existing Minecraft block, with its ID and block state, but without its position or object data.
 * 
 * @author piegames
 */
public class Block {

	/** The plain, empty state. DO NOT MODIFY! (There are no immutable bit sets) */
	public static final BitSet	STATE_NONE	= new BitSet(0);

	public static final Block			AIR	= new Block("minecraft:air");

	/** The name/id of the block including the namespace, like in Minecraft. Example: {@code minecraft:air} */
	public final String					name;
	/**
	 * The block's state. Each set bit represents one {@code key=value} property the block has. The actual names are not important any more,
	 * only a unique identification of each state is required for rendering. <br/>
	 * Note that through the way of encoding, there is the possibility to create invalid values (e.g. assigning the same property key multiple
	 * times or assigning properties to block that can't have it).
	 */
	public final BitSet			state;

	public Block(String name) {
		this(name, STATE_NONE);
	}

	public Block(String name, BitSet state) {
		this.name = Objects.requireNonNull(name);
		this.state = state;
	}

	/**
	 * Serialized this block to its compact form
	 * 
	 * @see #byCompactForm(String)
	 */
	@Override
	public String toString() {
		return name + ":" + state.toString();
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