package de.piegames.blockmap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.codepoetics.protonpack.StreamUtils;
import com.google.common.reflect.TypeToken;

import de.piegames.blockmap.color.BlockColorMap;
import de.piegames.blockmap.renderer.BlockState;

/**
 * A class representing all blocks with their states and properties in Minecraft. It is a direct representation of the {@code blocks.json}
 * that can be generated from the CLI of the Minecraft server.
 */
public class MinecraftBlocks {
	public Map<String, Block> states;

	@SuppressWarnings("serial")
	public MinecraftBlocks(Path path) throws IOException {
		states = BlockColorMap.GSON.fromJson(Files.newBufferedReader(path), new TypeToken<Map<String, Block>>() {
		}.getType());
	}

	public BlockState generateStates() {
		class KeyValue {

			String key, value;

			KeyValue(String key, String value) {
				this.key = key;
				this.value = value;
			}

			@Override
			public int hashCode() {
				final int prime = 31;
				int result = 1;
				result = prime * result + ((key == null) ? 0 : key.hashCode());
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
				KeyValue other = (KeyValue) obj;
				if (key == null) {
					if (other.key != null)
						return false;
				} else if (!key.equals(other.key))
					return false;
				if (value == null) {
					if (other.value != null)
						return false;
				} else if (!value.equals(other.value))
					return false;
				return true;
			}
		}

		return new BlockState(StreamUtils.zipWithIndex(states.values()
				.stream()
				.map(Block::getProperties)
				.flatMap(p -> p.entrySet().stream())
				.flatMap(e -> e.getValue().stream().map(v -> new KeyValue(e.getKey(), v)))
				.distinct()
		/* .sorted(Comparator.<KeyValue, String>comparing(kv -> kv.key).thenComparing(Comparator.comparing(kv -> kv.value))) */)
				.collect(Collectors.groupingBy(
						zip -> zip.getValue().key,
						Collectors.toMap(
								zip -> zip.getValue().value,
								zip -> (int) zip.getIndex()))));
	}

	public static class Block {
		private Map<String, Set<String>>	properties;
		public Set<State>					states;

		public Map<String, Set<String>> getProperties() {
			return properties == null ? Collections.emptyMap() : properties;
		}

		public static class State {
			public int					id;
			public Map<String, String>	properties;
		}
	}
}
