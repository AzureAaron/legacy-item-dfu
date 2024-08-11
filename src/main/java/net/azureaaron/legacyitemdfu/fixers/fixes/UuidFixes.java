package net.azureaaron.legacyitemdfu.fixers.fixes;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import com.mojang.serialization.Dynamic;

public class UuidFixes {

	public static Optional<Dynamic<?>> updateStringUuid(Dynamic<?> dynamic, String oldKey, String newKey) {
		return createArrayFromStringUuid(dynamic, oldKey).map(dynamic2 -> dynamic.remove(oldKey).set(newKey, dynamic2));
	}

	private static Optional<Dynamic<?>> createArrayFromStringUuid(Dynamic<?> dynamic, String key) {
		return dynamic.get(key).result().flatMap(dynamic2 -> {
			String string = dynamic2.asString(null);
			if (string != null) {
				try {
					UUID uUID = UUID.fromString(string);
					return createArray(dynamic, uUID.getMostSignificantBits(), uUID.getLeastSignificantBits());
				} catch (IllegalArgumentException var4) {
				}
			}

			return Optional.empty();
		});
	}

	public static Optional<Dynamic<?>> updateRegularMostLeast(Dynamic<?> dynamic, String oldKey, String newKey) {
		String string = oldKey + "Most";
		String string2 = oldKey + "Least";
		return createArrayFromMostLeastTags(dynamic, string, string2).map(dynamic2 -> dynamic.remove(string).remove(string2).set(newKey, dynamic2));
	}

	private static Optional<Dynamic<?>> createArrayFromMostLeastTags(Dynamic<?> dynamic, String mostBitsKey, String leastBitsKey) {
		long l = dynamic.get(mostBitsKey).asLong(0L);
		long m = dynamic.get(leastBitsKey).asLong(0L);
		return l != 0L && m != 0L ? createArray(dynamic, l, m) : Optional.empty();
	}

	private static Optional<Dynamic<?>> createArray(Dynamic<?> dynamic, long mostBits, long leastBits) {
		return Optional.of(dynamic.createIntList(Arrays.stream(new int[]{(int)(mostBits >> 32), (int)mostBits, (int)(leastBits >> 32), (int)leastBits})));
	}
}
