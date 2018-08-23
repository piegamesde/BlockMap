package togos.minecraft.maprend;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;

import org.junit.Test;

import com.google.gson.stream.JsonReader;

import togos.minecraft.maprend.renderer.BlockState;

public class BlockStateTest {
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
		try (JsonReader reader = new JsonReader(new InputStreamReader(getClass().getResourceAsStream("/blocks.json")))) {
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