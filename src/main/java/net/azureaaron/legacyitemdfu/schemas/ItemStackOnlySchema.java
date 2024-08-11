package net.azureaaron.legacyitemdfu.schemas;

import java.util.Map;
import java.util.function.Supplier;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;

/**
 * Abstract class to quickly setup schemas that only care about {@code ItemStack}s.
 */
public abstract class ItemStackOnlySchema extends Schema {

	public ItemStackOnlySchema(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	@Override
	public abstract void registerTypes(Schema schema, Map<String, Supplier<TypeTemplate>> entityTypes, Map<String, Supplier<TypeTemplate>> blockEntityTypes);

	@Override
	public Map<String, Supplier<TypeTemplate>> registerEntities(final Schema schema) {
		return Map.of();
	}

	@Override
	public Map<String, Supplier<TypeTemplate>> registerBlockEntities(final Schema schema) {
		return Map.of();
	}
}
