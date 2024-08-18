package net.azureaaron.legacyitemdfu.fixers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

import com.google.common.base.Splitter;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.OptionalDynamic;

import net.azureaaron.legacyitemdfu.TypeReferences;
import net.azureaaron.legacyitemdfu.fixers.fixes.TextFixes;
import net.azureaaron.legacyitemdfu.schemas.IdentifierNormalizingSchema;

public class ItemStackComponentizationFix extends DataFix {
	private static final int HIDE_ENCHANTMENTS_FLAG = 1;
	private static final int HIDE_MODIFIERS_FLAG = 2;
	private static final int HIDE_UNBREAKABLE_FLAG = 4;
	private static final int HIDE_CAN_DESTROY_FLAG = 8;
	private static final int HIDE_CAN_PLACE_FLAG = 16;
	private static final int HIDE_ADDITIONAL_FLAG = 32;
	private static final int HIDE_DYED_FLAG = 64;
	private static final int HIDE_UPGRADE_FLAG = 128;
	private static final Set<String> POTION_ITEM_IDS = Set.of(
			"minecraft:potion", "minecraft:splash_potion", "minecraft:lingering_potion", "minecraft:tipped_arrow"
			);
	private static final Set<String> ENTITY_BUCKET_ITEM_IDS = Set.of(
			"minecraft:pufferfish_bucket",
			"minecraft:salmon_bucket",
			"minecraft:cod_bucket",
			"minecraft:tropical_fish_bucket",
			"minecraft:axolotl_bucket",
			"minecraft:tadpole_bucket"
			);
	private static final List<String> RELEVANT_ENTITY_NBT_KEYS = List.of(
			"NoAI", "Silent", "NoGravity", "Glowing", "Invulnerable", "Health", "Age", "Variant", "HuntingCooldown", "BucketVariantTag"
			);
	private static final Set<String> BOOLEAN_BLOCK_STATE_PROPERTIES = Set.of(
			"attached",
			"bottom",
			"conditional",
			"disarmed",
			"drag",
			"enabled",
			"extended",
			"eye",
			"falling",
			"hanging",
			"has_bottle_0",
			"has_bottle_1",
			"has_bottle_2",
			"has_record",
			"has_book",
			"inverted",
			"in_wall",
			"lit",
			"locked",
			"occupied",
			"open",
			"persistent",
			"powered",
			"short",
			"signal_fire",
			"snowy",
			"triggered",
			"unstable",
			"waterlogged",
			"berries",
			"bloom",
			"shrieking",
			"can_summon",
			"up",
			"down",
			"north",
			"east",
			"south",
			"west",
			"slot_0_occupied",
			"slot_1_occupied",
			"slot_2_occupied",
			"slot_3_occupied",
			"slot_4_occupied",
			"slot_5_occupied",
			"cracked",
			"crafting"
			);
	private static final Splitter COMMA_SPLITTER = Splitter.on(',');

	public ItemStackComponentizationFix(Schema outputSchema, boolean changesType) {
		super(outputSchema, changesType);
	}

	private static void fixStack(StackData data, Dynamic<?> dynamic) {
		int hideFlags = data.getAndRemove("HideFlags").asInt(0);

		data.moveToComponent("Damage", "minecraft:damage", dynamic.createInt(0));
		data.moveToComponent("RepairCost", "minecraft:repair_cost", dynamic.createInt(0));
		data.moveToComponent("CustomModelData", "minecraft:custom_model_data");

		//Block, Entity and Block Entity tag fixes
		data.getAndRemove("BlockStateTag")
				.result()
				.ifPresent(blockStateTagDynamic -> data.setComponent("minecraft:block_state", fixBlockStateTag(blockStateTagDynamic)));
		data.moveToComponent("EntityTag", "minecraft:entity_data");
		data.applyFixer("BlockEntityTag", false, blockEntityTagDynamic -> {
			String id = IdentifierNormalizingSchema.normalize(blockEntityTagDynamic.get("id").asString(data.itemId));
			blockEntityTagDynamic = fixBlockEntityData(data, blockEntityTagDynamic, id);
			Dynamic<?> newBlockEntityTagDynamic = blockEntityTagDynamic.remove("id");

			return newBlockEntityTagDynamic.equals(blockEntityTagDynamic.emptyMap()) ? newBlockEntityTagDynamic : blockEntityTagDynamic;
		});
		data.moveToComponent("BlockEntityTag", "minecraft:block_entity_data");

		//Unbreakable
		if (data.getAndRemove("Unbreakable").asBoolean(false)) {
			Dynamic<?> unbreakableComponent = dynamic.emptyMap();
			if ((hideFlags & HIDE_UNBREAKABLE_FLAG) != 0) {
				unbreakableComponent = unbreakableComponent.set("show_in_tooltip", dynamic.createBoolean(false));
			}

			data.setComponent("minecraft:unbreakable", unbreakableComponent);
		}

		//Enchantments (applied to items and enchanted books
		fixEnchantments(data, dynamic, "Enchantments", "minecraft:enchantments", (hideFlags & HIDE_ENCHANTMENTS_FLAG) != 0);
		if (data.itemEquals("minecraft:enchanted_book")) {
			fixEnchantments(data, dynamic, "StoredEnchantments", "minecraft:stored_enchantments", (hideFlags & HIDE_ADDITIONAL_FLAG) != 0);
		}

		//Fix display - item custom name, lore, item dye colour
		data.applyFixer("display", false, displayDynamic -> fixDisplay(data, displayDynamic, hideFlags));

		//Fix can place/destroy predicates and attribute modifiers
		fixAdventureModePredicates(data, dynamic, hideFlags);
		fixAttributeModifiers(data, dynamic, hideFlags);

		//Armour Trims
		Optional<? extends Dynamic<?>> trimOpt = data.getAndRemove("Trim").result();
		if (trimOpt.isPresent()) {
			Dynamic<?> trimComponentDynamic = trimOpt.get();

			if ((hideFlags & HIDE_UPGRADE_FLAG) != 0) {
				trimComponentDynamic = trimComponentDynamic.set("show_in_tooltip", trimComponentDynamic.createBoolean(false));
			}

			data.setComponent("minecraft:trim", trimComponentDynamic);
		}

		if ((hideFlags & HIDE_ADDITIONAL_FLAG) != 0) {
			data.setComponent("minecraft:hide_additional_tooltip", dynamic.emptyMap());
		}

		//Item Specific Data Fixes
		if (data.itemEquals("minecraft:crossbow")) {
			data.getAndRemove("Charged");
			data.moveToComponent("ChargedProjectiles", "minecraft:charged_projectiles", dynamic.createList(Stream.empty()));
		}

		if (data.itemEquals("minecraft:bundle")) {
			data.moveToComponent("Items", "minecraft:bundle_contents", dynamic.createList(Stream.empty()));
		}

		if (data.itemEquals("minecraft:filled_map")) {
			data.moveToComponent("map", "minecraft:map_id");
			Map<? extends Dynamic<?>, ? extends Dynamic<?>> map = data.getAndRemove("Decorations").asStream()
					.map(ItemStackComponentizationFix::fixMapDecorations)
					.collect(Collectors.toMap(Pair::getFirst, Pair::getSecond, (dynamicx, dynamic2) -> dynamicx));
			if (!map.isEmpty()) {
				data.setComponent("minecraft:map_decorations", dynamic.createMap(map));
			}
		}

		if (data.itemMatches(POTION_ITEM_IDS)) {
			fixPotionContents(data, dynamic);
		}

		if (data.itemEquals("minecraft:writable_book")) {
			fixWritableBookContent(data, dynamic);
		}

		if (data.itemEquals("minecraft:written_book")) {
			fixWrittenBookContent(data, dynamic);
		}

		if (data.itemEquals("minecraft:suspicious_stew")) {
			data.moveToComponent("effects", "minecraft:suspicious_stew_effects");
		}

		if (data.itemEquals("minecraft:debug_stick")) {
			data.moveToComponent("DebugProperty", "minecraft:debug_stick_state");
		}
		
		if (data.itemMatches(ENTITY_BUCKET_ITEM_IDS)) {
			fixBucketEntityData(data, dynamic);
		}

		if (data.itemEquals("minecraft:goat_horn")) {
			data.moveToComponent("instrument", "minecraft:instrument");
		}

		if (data.itemEquals("minecraft:knowledge_book")) {
			data.moveToComponent("Recipes", "minecraft:recipes");
		}

		if (data.itemEquals("minecraft:compass")) {
			fixLodestoneTarget(data, dynamic);
		}

		if (data.itemEquals("minecraft:firework_rocket")) {
			fixFireworks(data);
		}

		if (data.itemEquals("minecraft:firework_star")) {
			fixExplosion(data);
		}

		if (data.itemEquals("minecraft:player_head")) {
			data.getAndRemove("SkullOwner").result().ifPresent(skullOwnerDynamic -> data.setComponent("minecraft:profile", createProfileDynamic(skullOwnerDynamic)));
		}
	}

	private static Dynamic<?> fixBlockStateTag(Dynamic<?> dynamic) {
		return DataFixUtils.orElse(dynamic.asMapOpt().result().map(stream -> stream.collect(Collectors.toMap(Pair::getFirst, pair -> {
				String string = pair.getFirst().asString("");
				Dynamic<?> dynamicx = pair.getSecond();
				if (BOOLEAN_BLOCK_STATE_PROPERTIES.contains(string)) {
					Optional<Boolean> optional = dynamicx.asBoolean().result();
					if (optional.isPresent()) {
						return dynamicx.createString(String.valueOf(optional.get()));
					}
				}

				Optional<Number> optional = dynamicx.asNumber().result();
				return optional.isPresent() ? dynamicx.createString(optional.get().toString()) : dynamicx;
			}))).map(dynamic::createMap), dynamic);
	}

	private static <T> Dynamic<T> fixBlockEntityData(ItemStackComponentizationFix.StackData data, Dynamic<T> dynamic, String blockEntityId) {
		data.setComponent("minecraft:lock", dynamic.get("Lock"));
		dynamic = dynamic.remove("Lock");
		Optional<Dynamic<T>> optional = dynamic.get("LootTable").result();
		if (optional.isPresent()) {
			Dynamic<T> dynamic2 = dynamic.emptyMap().set("loot_table", (Dynamic<?>)optional.get());
			long l = dynamic.get("LootTableSeed").asLong(0L);
			if (l != 0L) {
				dynamic2 = dynamic2.set("seed", dynamic.createLong(l));
			}

			data.setComponent("minecraft:container_loot", dynamic2);
			dynamic = dynamic.remove("LootTable").remove("LootTableSeed");
		}
		return switch (blockEntityId) {
			case "minecraft:skull" -> {
				data.setComponent("minecraft:note_block_sound", dynamic.get("note_block_sound"));
				yield dynamic.remove("note_block_sound");
			}

			case "minecraft:decorated_pot" -> {
				data.setComponent("minecraft:pot_decorations", dynamic.get("sherds"));
				Optional<Dynamic<T>> optional2 = dynamic.get("item").result();
				if (optional2.isPresent()) {
					data.setComponent(
						"minecraft:container", dynamic.createList(Stream.of(dynamic.emptyMap().set("slot", dynamic.createInt(0)).set("item", (Dynamic<?>)optional2.get())))
					);
				}

				yield dynamic.remove("sherds").remove("item");
			}

			//Modify this so that it matches all possible banner ids
			case String s when s.startsWith("minecraft:") && s.endsWith("banner")  -> {
				data.setComponent("minecraft:banner_patterns", dynamic.get("patterns"));
				Optional<Number> baseOpt = dynamic.get("Base").asNumber().result();
				if (baseOpt.isPresent()) {
					data.setComponent("minecraft:base_color", dynamic.createString(BannerPatternFormatFix.getColourFromInt(((Number)baseOpt.get()).intValue())));
				}

				yield dynamic.remove("patterns").remove("Base"); //TODO see if this matters that we removed base earlier, prob not
			}

			case "minecraft:shulker_box", "minecraft:chest", "minecraft:trapped_chest", "minecraft:furnace", "minecraft:ender_chest", "minecraft:dispenser", "minecraft:dropper", "minecraft:brewing_stand", "minecraft:hopper", "minecraft:barrel", "minecraft:smoker", "minecraft:blast_furnace", "minecraft:campfire", "minecraft:chiseled_bookshelf", "minecraft:crafter" -> {
				List<Dynamic<T>> items = dynamic.get("Items")
					.asList(
						itemsDynamic -> itemsDynamic.emptyMap()
								.set("slot", itemsDynamic.createInt(itemsDynamic.get("Slot").asByte((byte)0) & 255))
								.set("item", itemsDynamic.remove("Slot"))
					);
				if (!items.isEmpty()) {
					data.setComponent("minecraft:container", dynamic.createList(items.stream()));
				}

				yield dynamic.remove("Items");
			}

			case "minecraft:beehive" -> {
				data.setComponent("minecraft:bees", dynamic.get("bees"));
				yield dynamic.remove("bees");
			}

			default -> dynamic;
		};
	}

	private static void fixEnchantments(ItemStackComponentizationFix.StackData data, Dynamic<?> dynamic, String nbtKey, String componentId, boolean hideInTooltip) {
		OptionalDynamic<?> enchantmentsListDynamic = data.getAndRemove(nbtKey);
		List<Pair<String, Integer>> enchantmentsApplied = enchantmentsListDynamic.asList(Function.identity())
			.stream()
			.flatMap(enchantmentsDynamic -> getEnchantmentAndLevelPair(enchantmentsDynamic).stream())
			.toList();
		if (!enchantmentsApplied.isEmpty() || hideInTooltip) {
			Dynamic<?> componentDynamic = dynamic.emptyMap();
			Dynamic<?> levelsDynamic = dynamic.emptyMap();

			for (Pair<String, Integer> pair : enchantmentsApplied) {
				levelsDynamic = levelsDynamic.set(pair.getFirst(), dynamic.createInt(pair.getSecond()));
			}

			componentDynamic = componentDynamic.set("levels", levelsDynamic);
			if (hideInTooltip) {
				componentDynamic = componentDynamic.set("show_in_tooltip", dynamic.createBoolean(false));
			}

			data.setComponent(componentId, componentDynamic);
		}

		if (enchantmentsListDynamic.result().isPresent() && enchantmentsApplied.isEmpty()) {
			data.setComponent("minecraft:enchantment_glint_override", dynamic.createBoolean(true));
		}
	}

	private static Optional<Pair<String, Integer>> getEnchantmentAndLevelPair(Dynamic<?> dynamic) {
		return dynamic.get("id")
			.asString()
			.apply2stable(
				(enchantmentId, level) -> Pair.of(enchantmentId, Math.clamp(level.intValue(), 0, 255)), dynamic.get("lvl").asNumber()
			)
			.result();
	}

	private static Dynamic<?> fixDisplay(ItemStackComponentizationFix.StackData data, Dynamic<?> dynamic, int hideFlags) {
		data.setComponent("minecraft:custom_name", dynamic.get("Name"));
		data.setComponent("minecraft:lore", dynamic.get("Lore"));

		Optional<Integer> dyeColourOptional = dynamic.get("color").asNumber().result().map(Number::intValue);
		boolean hideDyeColour = (hideFlags & HIDE_DYED_FLAG) != 0;

		if (dyeColourOptional.isPresent() || hideDyeColour) {
			Dynamic<?> dyeColourComponentDynamic = dynamic.emptyMap().set("rgb", dynamic.createInt(dyeColourOptional.orElse(10511680)));
			if (hideDyeColour) {
				dyeColourComponentDynamic = dyeColourComponentDynamic.set("show_in_tooltip", dynamic.createBoolean(false));
			}

			data.setComponent("minecraft:dyed_color", dyeColourComponentDynamic);
		}

		Optional<String> localizedNameOpt = dynamic.get("LocName").asString().result();
		if (localizedNameOpt.isPresent()) {
			data.setComponent("minecraft:item_name", TextFixes.translate(dynamic.getOps(), localizedNameOpt.get()));
		}

		if (data.itemEquals("minecraft:filled_map")) {
			data.setComponent("minecraft:map_color", dynamic.get("MapColor"));
			dynamic = dynamic.remove("MapColor");
		}

		return dynamic.remove("Name").remove("Lore").remove("color").remove("LocName");
	}
	
	private static void fixAdventureModePredicates(ItemStackComponentizationFix.StackData data, Dynamic<?> dynamic, int hideFlags) {
		fixBlockPredicateList(data, dynamic, "CanDestroy", "minecraft:can_break", (hideFlags & HIDE_CAN_DESTROY_FLAG) != 0);
		fixBlockPredicateList(data, dynamic, "CanPlaceOn", "minecraft:can_place_on", (hideFlags & HIDE_CAN_PLACE_FLAG) != 0);
	}

	private static void fixBlockPredicateList(ItemStackComponentizationFix.StackData data, Dynamic<?> dynamic, String nbtKey, String componentId, boolean hideInTooltip) {
		Optional<? extends Dynamic<?>> optional = data.getAndRemove(nbtKey).result();
		if (!optional.isEmpty()) {
			Dynamic<?> componentDynamic = dynamic.emptyMap()
				.set(
					"predicates",
					dynamic.createList(
						(optional.get())
							.asStream()
							.map(
								predicatesDynamic -> DataFixUtils.orElse(
										predicatesDynamic.asString().map(string -> createBlockPredicateListDynamic(predicatesDynamic, string)).result(), predicatesDynamic
									)
							)
					)
				);
			if (hideInTooltip) {
				componentDynamic = componentDynamic.set("show_in_tooltip", dynamic.createBoolean(false));
			}

			data.setComponent(componentId, componentDynamic);
		}
	}

	private static Dynamic<?> createBlockPredicateListDynamic(Dynamic<?> dynamic, String listAsString) {
		int i = listAsString.indexOf(91);
		int j = listAsString.indexOf(123);
		int k = listAsString.length();
		if (i != -1) {
			k = i;
		}

		if (j != -1) {
			k = Math.min(k, j);
		}

		String string = listAsString.substring(0, k);
		Dynamic<?> dynamic2 = dynamic.emptyMap().set("blocks", dynamic.createString(string.trim()));
		int l = listAsString.indexOf(93);
		if (i != -1 && l != -1) {
			Dynamic<?> dynamic3 = dynamic.emptyMap();

			for (String string2 : COMMA_SPLITTER.split(listAsString.substring(i + 1, l))) {
				int m = string2.indexOf(61);
				if (m != -1) {
					String string3 = string2.substring(0, m).trim();
					String string4 = string2.substring(m + 1).trim();
					dynamic3 = dynamic3.set(string3, dynamic.createString(string4));
				}
			}

			dynamic2 = dynamic2.set("state", dynamic3);
		}

		int n = listAsString.indexOf(125);
		if (j != -1 && n != -1) {
			dynamic2 = dynamic2.set("nbt", dynamic.createString(listAsString.substring(j, n + 1)));
		}

		return dynamic2;
	}

	private static void fixAttributeModifiers(ItemStackComponentizationFix.StackData data, Dynamic<?> dynamic, int hideFlags) {
		OptionalDynamic<?> oldAttributeModifiers = data.getAndRemove("AttributeModifiers");

		if (!oldAttributeModifiers.result().isEmpty()) {
			boolean hideModifiersFromTooltip = (hideFlags & HIDE_MODIFIERS_FLAG) != 0;
			List<? extends Dynamic<?>> modifiers = oldAttributeModifiers.asList(ItemStackComponentizationFix::fixAttributeModifier);
			Dynamic<?> attributeModifiersComponentDynamic = dynamic.emptyMap().set("modifiers", dynamic.createList(modifiers.stream()));

			if (hideModifiersFromTooltip) {
				attributeModifiersComponentDynamic = attributeModifiersComponentDynamic.set("show_in_tooltip", dynamic.createBoolean(false));
			}

			data.setComponent("minecraft:attribute_modifiers", attributeModifiersComponentDynamic);
		}
	}

	private static Dynamic<?> fixAttributeModifier(Dynamic<?> dynamic) {
		Dynamic<?> modifier = dynamic.emptyMap()
			.set("name", dynamic.createString(""))
			.set("amount", dynamic.createDouble(0.0))
			.set("operation", dynamic.createString("add_value"));
		modifier = Dynamic.copyField(dynamic, "AttributeName", modifier, "type");
		modifier = Dynamic.copyField(dynamic, "Slot", modifier, "slot");
		modifier = Dynamic.copyField(dynamic, "UUID", modifier, "uuid");
		modifier = Dynamic.copyField(dynamic, "Name", modifier, "name");
		modifier = Dynamic.copyField(dynamic, "Amount", modifier, "amount");

		return Dynamic.copyAndFixField(dynamic, "Operation", modifier, "operation", operationDynamic -> {
			return operationDynamic.createString(switch (operationDynamic.asInt(0)) {
				case 1 -> "add_multiplied_base";
				case 2 -> "add_multiplied_total";
				default -> "add_value";
			});
		});
	}

	private static Pair<Dynamic<?>, Dynamic<?>> fixMapDecorations(Dynamic<?> dynamic) {
		Dynamic<?> idDynamic = DataFixUtils.orElseGet(dynamic.get("id").result(), () -> dynamic.createString(""));
		Dynamic<?> dynamic3 = dynamic.emptyMap()
			.set("type", dynamic.createString(getMapDecorationName(dynamic.get("type").asInt(0))))
			.set("x", dynamic.createDouble(dynamic.get("x").asDouble(0.0)))
			.set("z", dynamic.createDouble(dynamic.get("z").asDouble(0.0)))
			.set("rotation", dynamic.createFloat((float)dynamic.get("rot").asDouble(0.0)));
		return Pair.of(idDynamic, dynamic3);
	}

	private static String getMapDecorationName(int index) {
		return switch (index) {
			case 1 -> "frame";
			case 2 -> "red_marker";
			case 3 -> "blue_marker";
			case 4 -> "target_x";
			case 5 -> "target_point";
			case 6 -> "player_off_map";
			case 7 -> "player_off_limits";
			case 8 -> "mansion";
			case 9 -> "monument";
			case 10 -> "banner_white";
			case 11 -> "banner_orange";
			case 12 -> "banner_magenta";
			case 13 -> "banner_light_blue";
			case 14 -> "banner_yellow";
			case 15 -> "banner_lime";
			case 16 -> "banner_pink";
			case 17 -> "banner_gray";
			case 18 -> "banner_light_gray";
			case 19 -> "banner_cyan";
			case 20 -> "banner_purple";
			case 21 -> "banner_blue";
			case 22 -> "banner_brown";
			case 23 -> "banner_green";
			case 24 -> "banner_red";
			case 25 -> "banner_black";
			case 26 -> "red_x";
			case 27 -> "village_desert";
			case 28 -> "village_plains";
			case 29 -> "village_savanna";
			case 30 -> "village_snowy";
			case 31 -> "village_taiga";
			case 32 -> "jungle_temple";
			case 33 -> "swamp_hut";
			default -> "player";
		};
	}

	private static void fixPotionContents(ItemStackComponentizationFix.StackData data, Dynamic<?> dynamic) {
		Dynamic<?> potionContentsComponent = dynamic.emptyMap();
		Optional<String> potion = data.getAndRemove("Potion").asString().result().filter(potionId -> !potionId.equals("minecraft:empty"));

		if (potion.isPresent()) {
			potionContentsComponent = potionContentsComponent.set("potion", dynamic.createString((String)potion.get()));
		}

		potionContentsComponent = data.moveToComponent("CustomPotionColor", potionContentsComponent, "custom_color");
		potionContentsComponent = data.moveToComponent("custom_potion_effects", potionContentsComponent, "custom_effects");
		if (!potionContentsComponent.equals(dynamic.emptyMap())) {
			data.setComponent("minecraft:potion_contents", potionContentsComponent);
		}
	}

	private static void fixWritableBookContent(ItemStackComponentizationFix.StackData data, Dynamic<?> dynamic) {
		Dynamic<?> writableBookContentComponent = fixBookPages(data, dynamic);

		if (writableBookContentComponent != null) {
			data.setComponent("minecraft:writable_book_content", dynamic.emptyMap().set("pages", writableBookContentComponent));
		}
	}

	private static void fixWrittenBookContent(ItemStackComponentizationFix.StackData data, Dynamic<?> dynamic) {
		Dynamic<?> pages = fixBookPages(data, dynamic);
		String title = data.getAndRemove("title").asString("");
		Optional<String> filteredTitleOpt = data.getAndRemove("filtered_title").asString().result();
		Dynamic<?> writtenBookContentComponent = dynamic.emptyMap();

		writtenBookContentComponent = writtenBookContentComponent.set("title", createFilterableTextDynamic(dynamic, title, filteredTitleOpt));
		writtenBookContentComponent = data.moveToComponent("author", writtenBookContentComponent, "author");
		writtenBookContentComponent = data.moveToComponent("resolved", writtenBookContentComponent, "resolved");
		writtenBookContentComponent = data.moveToComponent("generation", writtenBookContentComponent, "generation");

		if (pages != null) {
			writtenBookContentComponent = writtenBookContentComponent.set("pages", pages);
		}

		data.setComponent("minecraft:written_book_content", writtenBookContentComponent);
	}

	@Nullable
	private static Dynamic<?> fixBookPages(ItemStackComponentizationFix.StackData data, Dynamic<?> dynamic) {
		List<String> pages = data.getAndRemove("pages").asList(pagesDynamic -> pagesDynamic.asString(""));
		Map<String, String> filteredPages = data.getAndRemove("filtered_pages")
			.asMap(filteredPagesKeyDynamic -> filteredPagesKeyDynamic.asString("0"), filteredPagesValueDynamic -> filteredPagesValueDynamic.asString(""));
		if (pages.isEmpty()) {
			return null;
		} else {
			List<Dynamic<?>> list2 = new ArrayList<>(pages.size());

			for (int i = 0; i < pages.size(); i++) {
				String string = pages.get(i);
				String string2 = filteredPages.get(String.valueOf(i));
				list2.add(createFilterableTextDynamic(dynamic, string, Optional.ofNullable(string2)));
			}

			return dynamic.createList(list2.stream());
		}
	}

	private static Dynamic<?> createFilterableTextDynamic(Dynamic<?> dynamic, String unfiltered, Optional<String> filtered) {
		Dynamic<?> dynamic2 = dynamic.emptyMap().set("raw", dynamic.createString(unfiltered));
		if (filtered.isPresent()) {
			dynamic2 = dynamic2.set("filtered", dynamic.createString(filtered.get()));
		}

		return dynamic2;
	}

	private static void fixBucketEntityData(ItemStackComponentizationFix.StackData data, Dynamic<?> dynamic) {
		Dynamic<?> bucketEntityDataComponentDynamic = dynamic.emptyMap();

		for (String string : RELEVANT_ENTITY_NBT_KEYS) {
			bucketEntityDataComponentDynamic = data.moveToComponent(string, bucketEntityDataComponentDynamic, string);
		}

		if (!bucketEntityDataComponentDynamic.equals(dynamic.emptyMap())) {
			data.setComponent("minecraft:bucket_entity_data", bucketEntityDataComponentDynamic);
		}
	}

	private static void fixLodestoneTarget(ItemStackComponentizationFix.StackData data, Dynamic<?> dynamic) {
		Optional<? extends Dynamic<?>> lodestonePosOpt = data.getAndRemove("LodestonePos").result();
		Optional<? extends Dynamic<?>> lodestoneDimensionOpt = data.getAndRemove("LodestoneDimension").result();

		if (!lodestonePosOpt.isEmpty() || !lodestoneDimensionOpt.isEmpty()) {
			boolean lodestoneTracked = data.getAndRemove("LodestoneTracked").asBoolean(true);
			Dynamic<?> lodestoneTrackerComponentDynamic = dynamic.emptyMap();

			if (lodestonePosOpt.isPresent() && lodestoneDimensionOpt.isPresent()) {
				lodestoneTrackerComponentDynamic = lodestoneTrackerComponentDynamic.set("target", dynamic.emptyMap().set("pos", (Dynamic<?>)lodestonePosOpt.get()).set("dimension", (Dynamic<?>)lodestoneDimensionOpt.get()));
			}

			if (!lodestoneTracked) {
				lodestoneTrackerComponentDynamic = lodestoneTrackerComponentDynamic.set("tracked", dynamic.createBoolean(false));
			}

			data.setComponent("minecraft:lodestone_tracker", lodestoneTrackerComponentDynamic);
		}
	}

	private static void fixExplosion(ItemStackComponentizationFix.StackData data) {
		data.applyFixer("Explosion", true, explosionDynamic -> {
			data.setComponent("minecraft:firework_explosion", fixExplosion(explosionDynamic));
			return explosionDynamic.remove("Type").remove("Colors").remove("FadeColors").remove("Trail").remove("Flicker");
		});
	}

	private static void fixFireworks(ItemStackComponentizationFix.StackData data) {
		data.applyFixer(
			"Fireworks",
			true,
			fireworksDynamic -> {
				Stream<? extends Dynamic<?>> stream = fireworksDynamic.get("Explosions").asStream().map(ItemStackComponentizationFix::fixExplosion);
				int flightDuration = fireworksDynamic.get("Flight").asInt(0);
				data.setComponent(
					"minecraft:fireworks",
					fireworksDynamic.emptyMap().set("explosions", fireworksDynamic.createList(stream)).set("flight_duration", fireworksDynamic.createByte((byte) flightDuration))
				);
				return fireworksDynamic.remove("Explosions").remove("Flight");
			}
		);
	}

	private static Dynamic<?> fixExplosion(Dynamic<?> dynamic) {
		dynamic = dynamic.set("shape", dynamic.createString(switch (dynamic.get("Type").asInt(0)) {
			case 1 -> "large_ball";
			case 2 -> "star";
			case 3 -> "creeper";
			case 4 -> "burst";
			default -> "small_ball";
		})).remove("Type");
		dynamic = dynamic.renameField("Colors", "colors");
		dynamic = dynamic.renameField("FadeColors", "fade_colors");
		dynamic = dynamic.renameField("Trail", "has_trail");
		return dynamic.renameField("Flicker", "has_twinkle");
	}

	public static Dynamic<?> createProfileDynamic(Dynamic<?> dynamic) {
		Optional<String> optional = dynamic.asString().result();
		if (optional.isPresent()) {
			return isValidUsername(optional.get()) ? dynamic.emptyMap().set("name", dynamic.createString(optional.get())) : dynamic.emptyMap();
		} else {
			String name = dynamic.get("Name").asString("");
			Optional<? extends Dynamic<?>> optional2 = dynamic.get("Id").result();
			Dynamic<?> propertiesDynamic = createPropertiesDynamic(dynamic.get("Properties"));
			Dynamic<?> profileComponentDynamic = dynamic.emptyMap();
			if (isValidUsername(name)) {
				profileComponentDynamic = profileComponentDynamic.set("name", dynamic.createString(name));
			}

			if (optional2.isPresent()) {
				profileComponentDynamic = profileComponentDynamic.set("id", optional2.get());
			}

			if (propertiesDynamic != null) {
				profileComponentDynamic = profileComponentDynamic.set("properties", propertiesDynamic);
			}

			return profileComponentDynamic;
		}
	}

	private static boolean isValidUsername(String username) {
		return username.length() > 16 ? false : username.chars().filter(c -> c <= 32 || c >= 127).findAny().isEmpty();
	}

	@Nullable
	private static Dynamic<?> createPropertiesDynamic(OptionalDynamic<?> propertiesDynamic) {
		Map<String, List<Pair<String, Optional<String>>>> map = propertiesDynamic.asMap(dynamic -> dynamic.asString(""), dynamic -> dynamic.asList(dynamicx -> {
				String string = dynamicx.get("Value").asString("");
				Optional<String> optional = dynamicx.get("Signature").asString().result();
				return Pair.of(string, optional);
			}));
		return map.isEmpty()
			? null
			: propertiesDynamic.createList(
				map.entrySet()
					.stream()
					.flatMap(
						entry -> (entry.getValue())
								.stream()
								.map(
									pair -> {
										Dynamic<?> propertyDynamic = propertiesDynamic.emptyMap()
											.set("name", propertiesDynamic.createString(entry.getKey()))
											.set("value", propertiesDynamic.createString(pair.getFirst()));
										Optional<String> optional = pair.getSecond();
										return optional.isPresent() ? propertyDynamic.set("signature", propertiesDynamic.createString(optional.get())) : propertyDynamic;
									}
								)
					)
			);
	}

	@Override
	protected TypeRewriteRule makeRule() {
		return this.writeFixAndRead(
				"ItemStack componentization", 
				this.getInputSchema().getType(TypeReferences.LEGACY_ITEM_STACK),
				this.getOutputSchema().getType(TypeReferences.LEGACY_ITEM_STACK),
				itemStackDynamic -> {
					Optional<? extends Dynamic<?>> optional = StackData.fromDynamic(itemStackDynamic).map(data -> {
						fixStack(data, data.nbt);

						return data.finalizeFix();
					});

					return DataFixUtils.orElse(optional, itemStackDynamic);
				});
	}

	private static class StackData {
		private final String itemId;
		private final int count;
		private Dynamic<?> components;
		private Dynamic<?> nbt;
		private final Dynamic<?> leftoverNbt;

		StackData(String itemId, int count, Dynamic<?> dynamic) {
			this.itemId = IdentifierNormalizingSchema.normalize(itemId);
			this.count = count;
			this.components = dynamic.emptyMap();
			this.nbt = dynamic.get("tag").orElseEmptyMap();
			this.leftoverNbt = dynamic.remove("tag");
		}

		static Optional<StackData> fromDynamic(Dynamic<?> dynamic) {
			return dynamic.get("id")
					.asString()
					.apply2stable(
							(itemId, count) -> new StackData(itemId, count.intValue(), dynamic.remove("id").remove("Count")),
							dynamic.get("Count").asNumber())
					.result();
		}

		OptionalDynamic<?> getAndRemove(String key) {
			OptionalDynamic<?> element = this.nbt.get(key);
			this.nbt = this.nbt.remove(key);
			return element;
		}

		void setComponent(String key, Dynamic<?> value) {
			this.components = this.components.set(key, value);
		}

		void setComponent(String key, OptionalDynamic<?> optionalValue) {
			optionalValue.result().ifPresent(value -> this.components = this.components.set(key, value));
		}

		void moveToComponent(String nbtKey, String componentId, Dynamic<?> defaultValue) {
			Optional<? extends Dynamic<?>> optional = this.getAndRemove(nbtKey).result();

			if (optional.isPresent() && !optional.get().equals(defaultValue)) {
				this.setComponent(componentId, optional.get());
			}
		}

		void moveToComponent(String nbtKey, String componentId) {
			this.getAndRemove(nbtKey).result().ifPresent(nbt -> this.setComponent(componentId, nbt));
		}

		void applyFixer(String nbtKey, boolean removeIfEmpty, UnaryOperator<Dynamic<?>> fixer) {
			OptionalDynamic<?> optionalDynamic = this.nbt.get(nbtKey);
			if (!removeIfEmpty || !optionalDynamic.result().isEmpty()) {
				Dynamic<?> dynamic = optionalDynamic.orElseEmptyMap();
				dynamic = fixer.apply(dynamic);
				if (dynamic.equals(dynamic.emptyMap())) {
					this.nbt = this.nbt.remove(nbtKey);
				} else {
					this.nbt = this.nbt.set(nbtKey, dynamic);
				}
			}
		}


		Dynamic<?> moveToComponent(String nbtKey, Dynamic<?> components, String componentId) {
			Optional<? extends Dynamic<?>> optional = this.getAndRemove(nbtKey).result();
			return optional.isPresent() ? components.set(componentId, optional.get()) : components;
		}

		Dynamic<?> finalizeFix() {
			Dynamic<?> stack = this.nbt.emptyMap()
					.set("id", this.nbt.createString(this.itemId))
					.set("count", this.nbt.createInt(this.count));

			if (!this.nbt.equals(this.nbt.emptyMap())) {
				this.components = this.components.set("minecraft:custom_data", this.nbt);
			}

			if (!this.components.equals(this.nbt.emptyMap())) {
				stack = stack.set("components", this.components);
			}

			return mergeLeftoverNbt(stack, this.leftoverNbt);
		}

		static <T> Dynamic<T> mergeLeftoverNbt(Dynamic<T> data, Dynamic<?> leftoverNbt) {
			DynamicOps<T> ops = data.getOps();

			return ops.getMap(data.getValue())
					.flatMap(mapLike -> ops.mergeToMap(leftoverNbt.convert(ops).getValue(), mapLike))
					.map(object -> new Dynamic<>(ops, object))
					.result()
					.orElse(data);
		}

		boolean itemEquals(String itemId) {
			return this.itemId.equals(itemId);
		}

		boolean itemMatches(Set<String> itemIds) {
			return itemIds.contains(this.itemId);
		}

		@SuppressWarnings("unused")
		boolean itemContains(String componentId) {
			return this.components.get(componentId).result().isPresent();
		}
	}
}
