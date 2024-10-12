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
import com.mojang.serialization.Dynamic;

import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;
import net.azureaaron.legacyitemdfu.TypeReferences;
import net.azureaaron.legacyitemdfu.schemas.IdentifierNormalizingSchema;

/**
 * Converts spawn eggs to their modern id
 */
public class SpawnEggItemIdFix extends DataFix {
	private static final Short2ObjectMap<String> DAMAGE_2_ID = DataFixUtils.make(new Short2ObjectOpenHashMap<>(), map -> {
		//Polar Bears shouldn't be damage 0 but Hypixel uses this for decoy spawn eggs and others
		map.put((short) 0, "minecraft:polar_bear_spawn_egg");
		map.put((short) 4, "minecraft:elder_guardian_spawn_egg");
		map.put((short) 5, "minecraft:wither_skeleton_spawn_egg");
		map.put((short) 6, "minecraft:stray_spawn_egg");
		map.put((short) 23, "minecraft:husk_spawn_egg");
		map.put((short) 27, "minecraft:zombie_villager_spawn_egg");
		map.put((short) 28, "minecraft:skeleton_horse_spawn_egg");
		map.put((short) 29, "minecraft:zombie_horse_spawn_egg");
		map.put((short) 50, "minecraft:creeper_spawn_egg");
		map.put((short) 51, "minecraft:skeleton_spawn_egg");
		map.put((short) 52, "minecraft:spider_spawn_egg");
		map.put((short) 54, "minecraft:zombie_spawn_egg");
		map.put((short) 55, "minecraft:slime_spawn_egg");
		map.put((short) 56, "minecraft:ghast_spawn_egg");
		map.put((short) 57, "minecraft:zombified_piglin_spawn_egg");
		map.put((short) 58, "minecraft:enderman_spawn_egg");
		map.put((short) 59, "minecraft:cave_spider_spawn_egg");
		map.put((short) 60, "minecraft:silverfish_spawn_egg");
		map.put((short) 61, "minecraft:blaze_spawn_egg");
		map.put((short) 62, "minecraft:magma_cube_spawn_egg");
		map.put((short) 65, "minecraft:bat_spawn_egg");
		map.put((short) 66, "minecraft:witch_spawn_egg");
		map.put((short) 67, "minecraft:endermite_spawn_egg");
		map.put((short) 68, "minecraft:guardian_spawn_egg");
		map.put((short) 69, "minecraft:shulker_spawn_egg");
		map.put((short) 90, "minecraft:pig_spawn_egg");
		map.put((short) 91, "minecraft:sheep_spawn_egg");
		map.put((short) 92, "minecraft:cow_spawn_egg");
		map.put((short) 93, "minecraft:chicken_spawn_egg");
		map.put((short) 94, "minecraft:squid_spawn_egg");
		map.put((short) 95, "minecraft:wolf_spawn_egg");
		map.put((short) 96, "minecraft:mooshroom_spawn_egg");
		map.put((short) 98, "minecraft:ocelot_spawn_egg");
		map.put((short) 100, "minecraft:horse_spawn_egg");
		map.put((short) 101, "minecraft:rabbit_spawn_egg");
		map.put((short) 120, "minecraft:villager_spawn_egg");

		map.defaultReturnValue("minecraft:air");
	});

	public SpawnEggItemIdFix(Schema outputSchema, boolean changesType) {
		super(outputSchema, changesType);
	}

	@Override
	protected TypeRewriteRule makeRule() {
		Type<?> type = this.getInputSchema().getType(TypeReferences.LEGACY_ITEM_STACK);
		OpticFinder<Pair<String, String>> itemIdFinder = DSL.fieldFinder("id", DSL.named(TypeReferences.ITEM_NAME.typeName(), IdentifierNormalizingSchema.getIdentifierType()));

		return this.fixTypeEverywhereTyped(
				"SpawnEggItemIdFix",
				type,
				itemStackTyped -> {
					Optional<Pair<String, String>> itemId = itemStackTyped.getOptional(itemIdFinder);

					if (itemId.isPresent() && itemId.get().getSecond().equals("minecraft:spawn_egg")) {
						Dynamic<?> stack = itemStackTyped.get(DSL.remainderFinder());
						short damage = stack.get("Damage").asShort((short) 0);
						String newId = DAMAGE_2_ID.get(damage);

						itemStackTyped = itemStackTyped.set(itemIdFinder, Pair.of(TypeReferences.ITEM_NAME.typeName(), newId));

						return itemStackTyped.set(DSL.remainderFinder(), stack.remove("Damage"));
					}

					return itemStackTyped;
				});
	}
}
