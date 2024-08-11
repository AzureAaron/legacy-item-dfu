package net.azureaaron.legacyitemdfu.fixers;

import java.util.Optional;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.azureaaron.legacyitemdfu.TypeReferences;
import net.azureaaron.legacyitemdfu.schemas.IdentifierNormalizingSchema;

/**
 * Fixes item ids that changed after the flattening
 */
public class PostFlatteningItemIdsFix extends DataFix {
	private static final Object2ObjectMap<String, String> NEW_IDS = DataFixUtils.make(new Object2ObjectOpenHashMap<>(), map -> {
		//1.13
		map.put("minecraft:melon_block", "minecraft:melon");
		map.put("minecraft:melon", "minecraft:melon_slice");
		map.put("minecraft:speckled_melon", "minecraft:glistering_melon_slice");
		map.put("minecraft:prismarine_bricks_slab", "minecraft:prismarine_brick_slab");
		map.put("minecraft:prismarine_bricks_stairs", "minecraft:prismarine_brick_stairs");

		//1.14
		map.put("minecraft:cactus_green", "minecraft:green_dye");
		map.put("minecraft:rose_red", "minecraft:red_dye");
		map.put("minecraft:dandelion_yellow", "minecraft:yellow_dye");
		map.put("minecraft:clownfish", "minecraft:tropical_fish");
		map.put("minecraft:stone_slab", "minecraft:smooth_stone_slab");
		map.put("minecraft:sign", "minecraft:oak_sign");

		//1.20.5
		map.put("minecraft:grass", "minecraft:short_grass");
		map.put("minecraft:scute", "minecraft:turtle_scute");
	});

	public PostFlatteningItemIdsFix(Schema outputSchema, boolean changesType) {
		super(outputSchema, changesType);
	}

	@Override
	protected TypeRewriteRule makeRule() {
		Type<?> type = this.getInputSchema().getType(TypeReferences.LEGACY_ITEM_STACK);
		OpticFinder<Pair<String, String>> itemIdFinder = DSL.fieldFinder("id", DSL.named(TypeReferences.ITEM_NAME.typeName(), IdentifierNormalizingSchema.getIdentifierType()));

		return this.fixTypeEverywhereTyped(
				"PostFlatteningItemIdsFix",
				type,
				itemStackTyped -> {
					Optional<Pair<String, String>> itemId = itemStackTyped.getOptional(itemIdFinder);

					if (itemId.isPresent() && NEW_IDS.containsKey(itemId.get().getSecond())) {
						String newId = NEW_IDS.get(itemId.get().getSecond());
						itemStackTyped = itemStackTyped.set(itemIdFinder, Pair.of(TypeReferences.ITEM_NAME.typeName(), newId));
					}

					return itemStackTyped;
				});
	}
}
