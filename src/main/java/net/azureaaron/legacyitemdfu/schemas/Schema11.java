package net.azureaaron.legacyitemdfu.schemas;

import java.util.Map;
import java.util.function.Supplier;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;

import net.azureaaron.legacyitemdfu.TypeReferences;

/**
 * Schema that represents an {@code ItemStack} with components.
 */
public final class Schema11 extends ItemStackOnlySchema {
	public Schema11(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	@Override
	public void registerTypes(Schema schema, Map<String, Supplier<TypeTemplate>> entityTypes, Map<String, Supplier<TypeTemplate>> blockEntityTypes) {
		schema.registerType(false, TypeReferences.ITEM_NAME, () -> DSL.constType(IdentifierNormalizingSchema.getIdentifierType()));
		schema.registerType(true, TypeReferences.LEGACY_ITEM_STACK, () -> DSL.optionalFields(
				"id",
				DSL.constType(IdentifierNormalizingSchema.getIdentifierType()),
				"components",
				DSL.remainder()));
	}
}
