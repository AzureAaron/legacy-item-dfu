package net.azureaaron.legacyitemdfu.utils;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonWriter;

public final class JsonHelper {

	public static String toSortedString(JsonElement json) {
		StringWriter stringWriter = new StringWriter();
		JsonWriter jsonWriter = new JsonWriter(stringWriter);

		try {
			writeSorted(jsonWriter, json, Comparator.naturalOrder());
		} catch (IOException var4) {
			throw new AssertionError(var4);
		}

		return stringWriter.toString();
	}

	private static void writeSorted(JsonWriter writer, @Nullable JsonElement json, @Nullable Comparator<String> comparator) throws IOException {
		if (json == null || json.isJsonNull()) {
			writer.nullValue();
		} else if (json.isJsonPrimitive()) {
			JsonPrimitive jsonPrimitive = json.getAsJsonPrimitive();
			if (jsonPrimitive.isNumber()) {
				writer.value(jsonPrimitive.getAsNumber());
			} else if (jsonPrimitive.isBoolean()) {
				writer.value(jsonPrimitive.getAsBoolean());
			} else {
				writer.value(jsonPrimitive.getAsString());
			}
		} else if (json.isJsonArray()) {
			writer.beginArray();

			for (JsonElement jsonElement : json.getAsJsonArray()) {
				writeSorted(writer, jsonElement, comparator);
			}

			writer.endArray();
		} else {
			if (!json.isJsonObject()) {
				throw new IllegalArgumentException("Couldn't write " + json.getClass());
			}

			writer.beginObject();

			for (Entry<String, JsonElement> entry : sort(json.getAsJsonObject().entrySet(), comparator)) {
				writer.name((String)entry.getKey());
				writeSorted(writer, (JsonElement)entry.getValue(), comparator);
			}

			writer.endObject();
		}
	}

	private static Collection<Entry<String, JsonElement>> sort(Collection<Entry<String, JsonElement>> entries, @Nullable Comparator<String> comparator) {
		if (comparator == null) {
			return entries;
		} else {
			List<Entry<String, JsonElement>> list = new ArrayList<>(entries);
			list.sort(Entry.comparingByKey(comparator));
			return list;
		}
	}
}
