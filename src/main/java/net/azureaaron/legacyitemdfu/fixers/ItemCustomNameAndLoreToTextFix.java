package net.azureaaron.legacyitemdfu.fixers;

import java.util.Optional;
import java.util.stream.Stream;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;

import net.azureaaron.legacyitemdfu.TypeReferences;
import net.azureaaron.legacyitemdfu.fixers.fixes.TextFixes;

public class ItemCustomNameAndLoreToTextFix extends DataFix {
	public ItemCustomNameAndLoreToTextFix(Schema outputSchema, boolean changesType) {
		super(outputSchema, changesType);
	}

	@Override
	protected TypeRewriteRule makeRule() {
		Type<?> type = this.getInputSchema().getType(TypeReferences.LEGACY_ITEM_STACK);
		OpticFinder<?> nbtTagFinder = type.findField("tag");

		TypeRewriteRule customNameFix = this.fixTypeEverywhereTyped(
				"ItemStack Custom Name to Text",
				type,
				itemStackTyped -> itemStackTyped.updateTyped(
						nbtTagFinder,
						nbtTagTyped -> nbtTagTyped.update(DSL.remainderFinder(), ItemCustomNameAndLoreToTextFix::fixCustomName)));

		TypeRewriteRule loreFix = this.fixTypeEverywhereTyped(
				"ItemStack Lore to Text",
				type,
				itemStackTyped -> itemStackTyped.updateTyped(
						nbtTagFinder,
						nbtTagTyped -> nbtTagTyped.update(DSL.remainderFinder(), nbtTagDynamic -> nbtTagDynamic.update(
								"display",
								displayTagDynamic -> displayTagDynamic.update(
										"Lore",
										loreDynamic -> DataFixUtils.orElse(loreDynamic.asStreamOpt().map(ItemCustomNameAndLoreToTextFix::fixLore).map(loreDynamic::createList).result(), loreDynamic))))));

		return TypeRewriteRule.seq(customNameFix, loreFix);
	}

	private static Dynamic<?> fixCustomName(Dynamic<?> tagDynamic) {
		Optional<? extends Dynamic<?>> displayDynamicOpt = tagDynamic.get("display").result();

		if (displayDynamicOpt.isPresent()) {
			Dynamic<?> displayDynamic = displayDynamicOpt.get();
			Optional<String> customNameOpt = displayDynamic.get("Name").asString().result();

			if (customNameOpt.isPresent()) {
				displayDynamic = displayDynamic.set("Name", TextFixes.text(displayDynamic.getOps(), customNameOpt.get()));
			}

			return tagDynamic.set("display", displayDynamic);
		} else {
			return tagDynamic;
		}
	}

	private static <T> Stream<Dynamic<T>> fixLore(Stream<Dynamic<T>> nbt) {
		return nbt.map(TextFixes::fixText);
	}
}
