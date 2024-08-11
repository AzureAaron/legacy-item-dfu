package net.azureaaron.legacyitemdfu.fixers;

import java.util.Optional;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;

import net.azureaaron.legacyitemdfu.TypeReferences;
import net.azureaaron.legacyitemdfu.schemas.IdentifierNormalizingSchema;

public class BannerIdsFix extends DataFix {

	public BannerIdsFix(Schema outputSchema, boolean changesType) {
		super(outputSchema, changesType);
	}

	@Override
	protected TypeRewriteRule makeRule() {
		Type<?> type = this.getInputSchema().getType(TypeReferences.LEGACY_ITEM_STACK);
		OpticFinder<Pair<String, String>> itemIdFinder = DSL.fieldFinder("id", DSL.named(TypeReferences.ITEM_NAME.typeName(), IdentifierNormalizingSchema.getIdentifierType()));
		OpticFinder<?> nbtTagFinder = type.findField("tag");
		OpticFinder<?> blockEntityTagFinder = nbtTagFinder.type().findField("BlockEntityTag");

		return this.fixTypeEverywhereTyped(
				"BannerIdsFix",
				type,
				itemStackTyped -> {
					Optional<Pair<String, String>> itemId = itemStackTyped.getOptional(itemIdFinder);

					if (itemId.isPresent() && itemId.get().getSecond().equals("minecraft:banner")) {
						Dynamic<?> stack = itemStackTyped.get(DSL.remainderFinder());
						Optional<? extends Typed<?>> nbtTagOpt = itemStackTyped.getOptionalTyped(nbtTagFinder);

						if (nbtTagOpt.isPresent()) {
							Typed<?> nbtTagTyped = nbtTagOpt.get();
							Optional<? extends Typed<?>> blockEntityTagOpt = nbtTagTyped.getOptionalTyped(blockEntityTagFinder);

							if (blockEntityTagOpt.isPresent()) {
								Typed<?> blockEntityTagTyped = blockEntityTagOpt.get();
								Dynamic<?> blockEntityTagDynamic = blockEntityTagTyped.getOrCreate(DSL.remainderFinder());

								//Set base colour as damage
								if (blockEntityTagDynamic.get("Base").asNumber().result().isPresent()) {
									stack = stack.set("Damage", stack.createShort((short) (blockEntityTagDynamic.get("Base").asInt(0) & 15)));
									blockEntityTagDynamic = blockEntityTagDynamic.remove("Base");
								}

								//Update pattern colour ids
								blockEntityTagDynamic = blockEntityTagDynamic.update(
										"Patterns",
										patternsDynamic -> DataFixUtils.orElse(
												patternsDynamic.asStreamOpt()
													.map(stream -> stream.map(patternDynamic -> patternDynamic.update("Color", colourDynamic -> colourDynamic.createInt(15 - colourDynamic.asInt(0)))))
													.map(patternsDynamic::createList)
													.result(),
												patternsDynamic));

								return itemStackTyped.set(DSL.remainderFinder(), stack).set(nbtTagFinder, nbtTagTyped.set(blockEntityTagFinder, blockEntityTagTyped.set(DSL.remainderFinder(), blockEntityTagDynamic)));
							}
						}

						return itemStackTyped.set(DSL.remainderFinder(), stack);
					} else {
						return itemStackTyped;
					}
				});
	}
}
