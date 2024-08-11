package net.azureaaron.legacyitemdfu.fixers;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

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

public class BannerPatternFormatFix extends DataFix {
	private static final Set<String> BANNER_IDS = Set.of(
			"minecraft:white_banner",
			"minecraft:light_gray_banner",
			"minecraft:gray_banner",
			"minecraft:black_banner",
			"minecraft:brown_banner",
			"minecraft:red_banner",
			"minecraft:orange_banner",
			"minecraft:yellow_banner",
			"minecraft:lime_banner",
			"minecraft:green_banner",
			"minecraft:cyan_banner",
			"minecraft:light_blue_banner",
			"minecraft:blue_banner",
			"minecraft:purple_banner",
			"minecraft:magenta_banner",
			"minecraft:pink_banner"
			);
	private static final Map<String, String> OLD_TO_NEW_PATTERNS = Map.ofEntries(
			Map.entry("b", "minecraft:base"),
			Map.entry("bl", "minecraft:square_bottom_left"),
			Map.entry("br", "minecraft:square_bottom_right"),
			Map.entry("tl", "minecraft:square_top_left"),
			Map.entry("tr", "minecraft:square_top_right"),
			Map.entry("bs", "minecraft:stripe_bottom"),
			Map.entry("ts", "minecraft:stripe_top"),
			Map.entry("ls", "minecraft:stripe_left"),
			Map.entry("rs", "minecraft:stripe_right"),
			Map.entry("cs", "minecraft:stripe_center"),
			Map.entry("ms", "minecraft:stripe_middle"),
			Map.entry("drs", "minecraft:stripe_downright"),
			Map.entry("dls", "minecraft:stripe_downleft"),
			Map.entry("ss", "minecraft:small_stripes"),
			Map.entry("cr", "minecraft:cross"),
			Map.entry("sc", "minecraft:straight_cross"),
			Map.entry("bt", "minecraft:triangle_bottom"),
			Map.entry("tt", "minecraft:triangle_top"),
			Map.entry("bts", "minecraft:triangles_bottom"),
			Map.entry("tts", "minecraft:triangles_top"),
			Map.entry("ld", "minecraft:diagonal_left"),
			Map.entry("rd", "minecraft:diagonal_up_right"),
			Map.entry("lud", "minecraft:diagonal_up_left"),
			Map.entry("rud", "minecraft:diagonal_right"),
			Map.entry("mc", "minecraft:circle"),
			Map.entry("mr", "minecraft:rhombus"),
			Map.entry("vh", "minecraft:half_vertical"),
			Map.entry("hh", "minecraft:half_horizontal"),
			Map.entry("vhr", "minecraft:half_vertical_right"),
			Map.entry("hhb", "minecraft:half_horizontal_bottom"),
			Map.entry("bo", "minecraft:border"),
			Map.entry("cbo", "minecraft:curly_border"),
			Map.entry("gra", "minecraft:gradient"),
			Map.entry("gru", "minecraft:gradient_up"),
			Map.entry("bri", "minecraft:bricks"),
			Map.entry("glb", "minecraft:globe"),
			Map.entry("cre", "minecraft:creeper"),
			Map.entry("sku", "minecraft:skull"),
			Map.entry("flo", "minecraft:flower"),
			Map.entry("moj", "minecraft:mojang"),
			Map.entry("pig", "minecraft:piglin")
		);

	public BannerPatternFormatFix(Schema outputSchema, boolean changesType) {
		super(outputSchema, changesType);
	}

	@Override
	protected TypeRewriteRule makeRule() {
		Type<?> type = this.getInputSchema().getType(TypeReferences.LEGACY_ITEM_STACK);
		OpticFinder<Pair<String, String>> itemIdFinder = DSL.fieldFinder("id", DSL.named(TypeReferences.ITEM_NAME.typeName(), IdentifierNormalizingSchema.getIdentifierType()));
		OpticFinder<?> nbtTagFinder = type.findField("tag");
		OpticFinder<?> blockEntityTagFinder = nbtTagFinder.type().findField("BlockEntityTag");

		return this.fixTypeEverywhereTyped(
				"BannerPatternFormatFix",
				type,
				itemStackTyped -> {
					Optional<Pair<String, String>> itemId = itemStackTyped.getOptional(itemIdFinder);

					if (itemId.isPresent() && BANNER_IDS.contains(itemId.get().getSecond())) {
						Dynamic<?> stack = itemStackTyped.get(DSL.remainderFinder());
						Optional<? extends Typed<?>> nbtTagOpt = itemStackTyped.getOptionalTyped(nbtTagFinder);

						if (nbtTagOpt.isPresent()) {
							Typed<?> nbtTagTyped = nbtTagOpt.get();
							Optional<? extends Typed<?>> blockEntityTagOpt = nbtTagTyped.getOptionalTyped(blockEntityTagFinder);

							if (blockEntityTagOpt.isPresent()) {
								Typed<?> blockEntityTagTyped = blockEntityTagOpt.get();
								Dynamic<?> blockEntityTagDynamic = blockEntityTagTyped.getOrCreate(DSL.remainderFinder());
								blockEntityTagDynamic = blockEntityTagDynamic.renameAndFixField(
										"Patterns",
										"patterns",
										patternsDynamic -> patternsDynamic.createList(patternsDynamic.asStream().map(BannerPatternFormatFix::replacePatternAndColour)));

								return itemStackTyped.set(DSL.remainderFinder(), stack).set(nbtTagFinder, nbtTagTyped.set(blockEntityTagFinder, blockEntityTagTyped.set(DSL.remainderFinder(), blockEntityTagDynamic)));
							}
						}
					}

					return itemStackTyped;
				});
	}

	private static Dynamic<?> replacePatternAndColour(Dynamic<?> dynamic) {
		dynamic = dynamic.renameAndFixField(
			"Pattern",
			"pattern",
			patternDynamic -> DataFixUtils.orElse(
					patternDynamic.asString().map(patternId -> OLD_TO_NEW_PATTERNS.getOrDefault(patternId, patternId)).map(patternDynamic::createString).result(),
					patternDynamic
				)
		);
		dynamic = dynamic.set("color", dynamic.createString(getColourFromInt(dynamic.get("Color").asInt(0))));
		return dynamic.remove("Color");
	}

	static String getColourFromInt(int color) {
		return switch (color) {
			case 1 -> "orange";
			case 2 -> "magenta";
			case 3 -> "light_blue";
			case 4 -> "yellow";
			case 5 -> "lime";
			case 6 -> "pink";
			case 7 -> "gray";
			case 8 -> "light_gray";
			case 9 -> "cyan";
			case 10 -> "purple";
			case 11 -> "blue";
			case 12 -> "brown";
			case 13 -> "green";
			case 14 -> "red";
			case 15 -> "black";
			default -> "white";
		};
	}
}
