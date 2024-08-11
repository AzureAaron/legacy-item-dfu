package net.azureaaron.legacyitemdfu.fixers;

import java.util.Optional;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;

import net.azureaaron.legacyitemdfu.TypeReferences;
import net.azureaaron.legacyitemdfu.fixers.fixes.UuidFixes;
import net.azureaaron.legacyitemdfu.schemas.IdentifierNormalizingSchema;

public class ItemStackUuidsFix extends DataFix {
	public ItemStackUuidsFix(Schema outputSchema, boolean changesType) {
		super(outputSchema, changesType);
	}

	@Override
	protected TypeRewriteRule makeRule() {
		Type<?> type = this.getInputSchema().getType(TypeReferences.LEGACY_ITEM_STACK);
		OpticFinder<Pair<String, String>> itemIdFinder = DSL.fieldFinder("id", DSL.named(TypeReferences.ITEM_NAME.typeName(), IdentifierNormalizingSchema.getIdentifierType()));
		OpticFinder<?> nbtTagFinder = type.findField("tag");

		return this.fixTypeEverywhereTyped(
				"ItemStack UUIDs Fix",
				type,
				itemStackTyped -> {
					return itemStackTyped.updateTyped(nbtTagFinder, nbtTagTyped -> nbtTagTyped.update(DSL.remainderFinder(), nbtTagDynamic -> {
						nbtTagDynamic = fixAttributeModifiers(nbtTagDynamic);
						Optional<Pair<String, String>> itemId = itemStackTyped.getOptional(itemIdFinder);

						if (itemId.isPresent() && itemId.get().getSecond().equals("minecraft:player_head")) {
							nbtTagDynamic = fixPlayerHeadOwner(nbtTagDynamic);
						}

						return nbtTagDynamic;
					}));
				});
	}

	private static Dynamic<?> fixAttributeModifiers(Dynamic<?> tagDynamic) {
		return tagDynamic.update(
			"AttributeModifiers",
			attributeModifiersDynamic -> tagDynamic.createList(
					attributeModifiersDynamic.asStream()
						.map(attributeModifier -> UuidFixes.updateRegularMostLeast(attributeModifier, "UUID", "UUID").orElse(attributeModifier))
				)
		);
	}

	private Dynamic<?> fixPlayerHeadOwner(Dynamic<?> tagDynamic) {
		return tagDynamic.update("SkullOwner", skullOwner -> UuidFixes.updateStringUuid(skullOwner, "Id", "Id").orElse(skullOwner));
	}
}
