package de.piegames.blockmap.standalone;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;

import io.gsonfire.PostProcessor;

public class DeserializeNullChecker implements PostProcessor<Object> {

	@Documented
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.FIELD })
	public @interface DeserializeNonNull {
	}

	@Override
	public void postDeserialize(Object result, JsonElement src, Gson gson) {
		check(result, src);
	}

	@Override
	public void postSerialize(JsonElement result, Object src, Gson gson) {
	}

	private void check(Object result, JsonElement src) {
		try {
			List<String> nullFields = new ArrayList<>();
			for (Field f : result.getClass().getDeclaredFields()) {
				if (f.isAnnotationPresent(DeserializeNonNull.class)) {
					String name = f.getName();
					if (f.isAnnotationPresent(SerializedName.class))
						name = f.getAnnotation(SerializedName.class).value();
					if (f.get(result) == null)
						nullFields.add('"' + name + '"');
				}
			}
			if (!nullFields.isEmpty()) {
				if (nullFields.size() == 1)
					throw new JsonParseException("Field " + nullFields.get(0) + " in object '"
							+ ellipsisString(src.toString())
							+ "' must be specified");
				else
					throw new JsonParseException("Fields " + nullFields + " in object '"
							+ ellipsisString(src.toString())
							+ "' must be specified");
			}
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new JsonParseException("Could not check all null fields", e);
		}
	}

	private String ellipsisString(String text) {
		if (text.length() < 40)
			return text;
		else
			return text.substring(0, 30) + "[…]" + text.substring(text.length() - 10, text.length());
	}
}