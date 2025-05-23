package net.azureaaron.legacyitemdfu.schemas;

import java.util.Map;
import java.util.function.Supplier;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import com.mojang.datafixers.util.Pair;

import net.azureaaron.legacyitemdfu.TypeReferences;

/**
 * Schema that represents an {@code ItemStack} with either a numeric or string id, and an NBT tag.
 */
public final class Schema1 extends ItemStackOnlySchema {
	public Schema1(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	@Override
	public void registerTypes(Schema schema, Map<String, Supplier<TypeTemplate>> entityTypes, Map<String, Supplier<TypeTemplate>> blockEntityTypes) {
		schema.registerType(false, TypeReferences.ITEM_NAME, () -> DSL.constType(IdentifierNormalizingSchema.getIdentifierType()));
		schema.registerType(true, TypeReferences.LEGACY_ITEM_STACK, () -> DSL.optionalFields(
				"id",
				DSL.or(DSL.constType(DSL.intType()), TypeReferences.ITEM_NAME.in(schema)),
				"tag",
				DSL.optionalFields(Pair.of("BlockEntityTag", DSL.remainder()))
				)
				);
	}
}
