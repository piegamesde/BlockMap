package togos.minecraft.maprend;

import java.util.List;
import java.util.Map;
import com.google.gson.annotations.SerializedName;

/** Deserialization help to get the generated data from the Minecraft server */
public class BlockStateHelper {

	public String		name, value;
	// public List<String> allowedBlocks = new ArrayList<>();

	public BlockStateHelper() {

	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
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
		BlockStateHelper other = (BlockStateHelper) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}

	public BlockStateHelper(String name, String value) {
		this.name = name;
		this.value = value;
	}

	public static class BlockStateHelperBlock {

		public List<BlockStateHelperState>	states;
		public Map<String, List<String>>	properties;
	}

	public static class BlockStateHelperState {

		public int					id;
		@SerializedName("default")
		public boolean				def	= false;
		public Map<String, String>	properties;
	}
}
