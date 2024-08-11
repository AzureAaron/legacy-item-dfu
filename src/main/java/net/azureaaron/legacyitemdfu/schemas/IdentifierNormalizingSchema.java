package net.azureaaron.legacyitemdfu.schemas;

import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.Const.PrimitiveType;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.PrimitiveCodec;

import net.azureaaron.legacyitemdfu.utils.IdentifierUtils;

public class IdentifierNormalizingSchema extends Schema {
	public static final PrimitiveCodec<String> CODEC = new PrimitiveCodec<String>() {
		@Override
		public <T> DataResult<String> read(DynamicOps<T> ops, T input) {
			return ops.getStringValue(input).map(IdentifierNormalizingSchema::normalize);
		}

		public <T> T write(DynamicOps<T> dynamicOps, String string) {
			return dynamicOps.createString(string);
		}

		public String toString() {
			return "NamespacedString";
		}
	};

	private static final Type<String> IDENTIFIER_TYPE = new PrimitiveType<>(CODEC);

	public IdentifierNormalizingSchema(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	public static String normalize(String id) {
		String identifier = IdentifierUtils.tryParse(id);
		return identifier != null ? identifier.toString() : id;
	}

	public static Type<String> getIdentifierType() {
		return IDENTIFIER_TYPE;
	}

	@Override
	public Type<?> getChoiceType(TypeReference type, String choiceName) {
		return super.getChoiceType(type, normalize(choiceName));
	}
}
