package de.piegames.blockmap.renderer;

import java.util.BitSet;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

public class BlockState {

	Map<String, Map<String, Integer>> states;

	@SuppressWarnings("unused")
	private BlockState() {
		/* Used by GSON */
	}

	public BlockState(Map<String, Map<String, Integer>> states) {
		this.states = Objects.requireNonNull(states);
	}

	public int getProperty(String key, String value) {
		return states.getOrDefault(key, Collections.emptyMap()).get(value);
	}

	public BitSet getState(Map<String, String> properties) {
		BitSet state = new BitSet(states.size());
		for (Entry<String, String> entry : properties.entrySet())
			state.set(getProperty(entry.getKey(), entry.getValue()));
		return state;
	}

	public int getSize() {
		return states.size();
	}

	@Override
	public int hashCode() {
		return Objects.hash(states);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BlockState other = (BlockState) obj;
		return Objects.equals(states, other.states);
	}
}
