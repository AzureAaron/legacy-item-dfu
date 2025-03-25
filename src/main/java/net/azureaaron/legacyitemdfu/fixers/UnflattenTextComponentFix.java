package net.azureaaron.legacyitemdfu.fixers;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.OptionalDynamic;

import net.azureaaron.legacyitemdfu.TypeReferences;

/**
 * Text components in 1.21.5 were moved to a structure where they were no longer flattened into a singular json string, so we need
 * to unflatten them.
 */
public class UnflattenTextComponentFix extends DataFix {

	public UnflattenTextComponentFix(Schema outputSchema, boolean changesType) {
		super(outputSchema, changesType);
	}

	@Override
	protected TypeRewriteRule makeRule() {
		Type<?> type = this.getInputSchema().getType(TypeReferences.LEGACY_ITEM_STACK);
		OpticFinder<?> componentsFinder = type.findField("components");

		return this.fixTypeEverywhereTyped(
				"Unflatten Text Component Fix",
				type,
				itemStackTyped -> itemStackTyped.updateTyped(componentsFinder, this::fix));
	}

	private Typed<?> fix(Typed<?> componentsTyped) {
		return componentsTyped.update(DSL.remainderFinder(), dynamic -> {
			OptionalDynamic<?> itemName = dynamic.get("minecraft:item_name");

			if (itemName.result().isPresent()) {
				dynamic = dynamic.set("minecraft:item_name", unflattenTextComponent(itemName.getOps(), itemName.asString("")));
			}

			OptionalDynamic<?> customName = dynamic.get("minecraft:custom_name");

			if (customName.result().isPresent()) {
				dynamic = dynamic.set("minecraft:custom_name", unflattenTextComponent(customName.getOps(), customName.asString("")));
			}

			OptionalDynamic<?> lore = dynamic.get("minecraft:lore");

			if (lore.result().isPresent()) {
				dynamic = dynamic.set("minecraft:lore", dynamic.createList(lore.result().get().asStream()
						.map(lineDynamic -> unflattenTextComponent(lineDynamic.getOps(), lineDynamic.asString(""))))
						);
			}

			return dynamic;
		});
	}

	private static <T> Dynamic<T> unflattenTextComponent(DynamicOps<T> outOps, String jsonText) {
		try {
			JsonElement json = JsonParser.parseString(jsonText);

			if (!json.isJsonNull()) {
				return new Dynamic<>(outOps, JsonOps.INSTANCE.convertTo(outOps, json));
			}
		} catch (Exception e) {
			//TODO add better logging later - I don't know why this would get fed non-json text to begin with anyways
			System.out.println(String.format("Failed to unflatten text component json: %s", jsonText));
			e.printStackTrace();
		}

		return new Dynamic<>(outOps, outOps.createString(jsonText));
	}
}
