package togos.minecraft.maprend;

import static org.junit.Assert.assertEquals;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import org.junit.Test;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import togos.minecraft.maprend.BlockStateHelper.BlockStateHelperBlock;
import togos.minecraft.maprend.BlockStateHelper.BlockStateHelperState;
import togos.minecraft.maprend.renderer.BlockState;

public class BlockStateTest {

	private Map<String, BlockStateHelperBlock> blocks;

	@Test
	public void generateClass() throws IOException {
		Type type = new TypeToken<Map<String, BlockStateHelperBlock>>() {}.getType();
		blocks = new Gson().fromJson(new InputStreamReader(getClass().getResourceAsStream("/blocks.json")), type);

		Comparator<BlockStateHelper> c = Comparator.comparing(s -> s.name);

		// Map each block id to a set of possible block states
		Map<String, Set<BlockStateHelper>> statesByBlock = new HashMap<>();

		for (Entry<String, BlockStateHelperBlock> e : blocks.entrySet()) {
			if (!statesByBlock.containsKey(e.getKey()))
				statesByBlock.put(e.getKey(), new HashSet<>());
			Set<BlockStateHelper> states = statesByBlock.get(e.getKey());
			for (BlockStateHelperState state : e.getValue().states) {
				if (state.properties != null)
				state.properties.forEach((k, v) -> states.add(new BlockStateHelper(k, v)));
			}
		}

		// Reverse map each block state to the blocks that are allowed to use it (maybe a BiMap would help?)
		Map<BlockStateHelper, Set<String>> blocksByState = new TreeMap<>(c.thenComparing(Comparator.comparing(s -> s.value)));
		for (Entry<String, Set<BlockStateHelper>> e : statesByBlock.entrySet()) {
			for (BlockStateHelper state : e.getValue()) {
				if (blocksByState.containsKey(state)) {
					blocksByState.get(state).add(e.getKey());
				} else {
					Set<String> allowed = new HashSet<>();
					allowed.add(e.getKey());
					blocksByState.put(state, allowed);
				}
			}
		}

		// We can now start generating our actual enum code
		StringBuilder builder = new StringBuilder();
		for (Entry<BlockStateHelper, Set<String>> e : blocksByState.entrySet()) {
			String key = e.getKey().name;
			String value = e.getKey().value;
			builder.append('\t');
			builder.append(key.toUpperCase());
			builder.append("_");
			builder.append(value.toUpperCase());
			builder.append("(\"");
			builder.append(key);
			builder.append("\", \"");
			builder.append(value);
			builder.append("\"");
			if (!e.getValue().isEmpty()) {
				for (String s : e.getValue()) {
					builder.append(", \"");
					builder.append(s);
					builder.append('"');
				}
			}
			builder.append("),");
			builder.append(System.getProperty("line.separator"));
		}
		// Hardcode the map property of item frames since it does not show up in this list
		builder.append("MAP_TRUE(\"map\", \"true\"),");
		builder.append(System.getProperty("line.separator"));
		builder.append("MAP_FALSE(\"map\", \"false\");");

		// Load template file and replace original
		String original = new String(Files.readAllBytes(Paths.get(URI.create(getClass().getResource("/BlockState.java").toString()))));
		original = original.replace("$REPLACE", builder.toString());
		Files.write(Paths.get("./src/main/java", "togos/minecraft/maprend/renderer", "BlockState.java"), original.getBytes());
	}

	@Test
	public void testName() {
		for (BlockState state : BlockState.values())
			assertEquals(state.name(), state.name.toUpperCase() + "_" + state.value.toUpperCase());
	}

	@Test
	public void testFind() {
		for (BlockState state : BlockState.values())
			assertEquals(state, BlockState.valueOf(state.name, state.value));
	}

	/**
	 * Use Minecraft generated data to test if every single block state existing is present here.
	 *
	 * @throws URISyntaxException
	 * @throws IOException
	 */
	@Test
	public void testExisting() throws IOException, URISyntaxException {
		try (JsonReader reader = new JsonReader(Files.newBufferedReader(Paths.get(URI.create(getClass().getResource("/blocks.json").toString()))))) {
			reader.beginObject();
			while (reader.hasNext()) {
				reader.skipValue();
				reader.beginObject();
				while (reader.hasNext()) {
					String name = reader.nextName();
					if (name.equals("properties")) {
						reader.beginObject();
						while (reader.hasNext()) {
							String propertyName = reader.nextName();
							reader.beginArray();
							while (reader.hasNext()) {
								String propertyValue = reader.nextString();
								try {
									BlockState.valueOf(propertyName, propertyValue);
								} catch (Exception e) {
									throw new AssertionError("Property " + propertyName + "=" + propertyValue + " should exist, but doesn't");
								}
							}
							reader.endArray();
						}
						reader.endObject();
					} else
						reader.skipValue();
				}
				reader.endObject();
			}
			reader.endObject();
		}
	}
}