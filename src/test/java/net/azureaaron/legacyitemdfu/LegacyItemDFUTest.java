package net.azureaaron.legacyitemdfu;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;

public class LegacyItemDFUTest {
	private final DataFixer FIXER = LegacyItemStackFixer.getFixer();
	private final int FIRST_VERSION = LegacyItemStackFixer.getFirstVersion();
	private final int LATEST_VERSION = LegacyItemStackFixer.getLatestVersion();

	@Test
	void testSpawnEggItemIdFix() {
		JsonElement spawnEgg = JsonParser.parseString("{\"id\":383,\"Damage\":0,\"Count\":1}");
		Dynamic<JsonElement> fixed = FIXER.update(TypeReferences.LEGACY_ITEM_STACK, new Dynamic<>(JsonOps.INSTANCE, spawnEgg), FIRST_VERSION, LATEST_VERSION);

		System.out.println("Spawn Egg Fixed: " + fixed.getValue());
		Assertions.assertEquals("{\"id\":\"minecraft:polar_bear_spawn_egg\",\"count\":1}", fixed.getValue().toString());
	}

	@Test
	void testBannerFixes() {
		//Totem of Corruption
		//{"id":425,"Count":1,"tag":{"BlockEntityTag":{"Patterns":[{"Pattern":"b","Color":0},{"Pattern":"cr","Color":13},{"Pattern":"cbo","Color":5},{"Pattern":"mc","Color":5},{"Pattern":"flo","Color":0},{"Pattern":"bts","Color":0},{"Pattern":"tts","Color":0}],"Base":15}}}
		JsonElement banner = JsonParser.parseString("{\"id\":425,\"Count\":1,\"tag\":{\"BlockEntityTag\":{\"Patterns\":[{\"Pattern\":\"b\",\"Color\":0},{\"Pattern\":\"cr\",\"Color\":13},{\"Pattern\":\"cbo\",\"Color\":5},{\"Pattern\":\"mc\",\"Color\":5},{\"Pattern\":\"flo\",\"Color\":0},{\"Pattern\":\"bts\",\"Color\":0},{\"Pattern\":\"tts\",\"Color\":0}],\"Base\":15}}}");
		Dynamic<JsonElement> fixed = FIXER.update(TypeReferences.LEGACY_ITEM_STACK, new Dynamic<>(JsonOps.INSTANCE, banner), FIRST_VERSION, LATEST_VERSION);

		System.out.println("Banner Fixed: " + fixed.getValue());
		Assertions.assertEquals("{\"id\":\"minecraft:white_banner\",\"count\":1,\"components\":{\"minecraft:banner_patterns\":[{\"pattern\":\"minecraft:base\",\"color\":\"black\"},{\"pattern\":\"minecraft:cross\",\"color\":\"magenta\"},{\"pattern\":\"minecraft:curly_border\",\"color\":\"purple\"},{\"pattern\":\"minecraft:circle\",\"color\":\"purple\"},{\"pattern\":\"minecraft:flower\",\"color\":\"black\"},{\"pattern\":\"minecraft:triangles_bottom\",\"color\":\"black\"},{\"pattern\":\"minecraft:triangles_top\",\"color\":\"black\"}]}}", fixed.getValue().toString());
	}

	@Test
	void testShortGrassIdFix() {
		JsonElement shortGrass = JsonParser.parseString("{\"id\":31,\"Damage\":1,\"Count\":1}");
		Dynamic<JsonElement> fixed = FIXER.update(TypeReferences.LEGACY_ITEM_STACK, new Dynamic<>(JsonOps.INSTANCE, shortGrass), FIRST_VERSION, LATEST_VERSION);

		System.out.println("Short Grass Fixed: " + fixed.getValue());
		Assertions.assertEquals("{\"id\":\"minecraft:short_grass\",\"count\":1}", fixed.getValue().toString());
	}

	@Test
	void testDarkClaymore() {
		//{"id":272,"Count":1,"Damage":0,"tag":{"ExtraAttributes":{"id":"DARK_CLAYMORE"},"display":{"Name":"§dWithered Dark Claymore §6✪§6✪§6✪§6✪§6✪§c➎","Lore":["§7§7§oThat thing was too big to be called a","§7§osword, it was more like a large hunk","§7§oof stone."]},"Unbreakable":true,"HideFlags":254,"ench":[]}}
		JsonElement darkClaymore = JsonParser.parseString("{\"id\":272,\"Count\":1,\"Damage\":0,\"tag\":{\"ExtraAttributes\":{\"id\":\"DARK_CLAYMORE\"},\"display\":{\"Name\":\"§dWithered Dark Claymore §6✪§6✪§6✪§6✪§6✪§c➎\",\"Lore\":[\"§7§7§oThat thing was too big to be called a\",\"§7§osword, it was more like a large hunk\",\"§7§oof stone.\"]},\"Unbreakable\":true,\"HideFlags\":254,\"ench\":[]}}");
		Dynamic<JsonElement> fixed = FIXER.update(TypeReferences.LEGACY_ITEM_STACK, new Dynamic<>(JsonOps.INSTANCE, darkClaymore), FIRST_VERSION, LATEST_VERSION);

		System.out.println("Dark Claymore Fixed: " + fixed.getValue());
		Assertions.assertEquals("{\"id\":\"minecraft:stone_sword\",\"count\":1,\"components\":{\"minecraft:unbreakable\":{\"show_in_tooltip\":false},\"minecraft:enchantment_glint_override\":true,\"minecraft:custom_name\":\"{\\\"text\\\":\\\"§dWithered Dark Claymore §6✪§6✪§6✪§6✪§6✪§c➎\\\"}\",\"minecraft:lore\":[\"{\\\"text\\\":\\\"§7§7§oThat thing was too big to be called a\\\"}\",\"{\\\"text\\\":\\\"§7§osword, it was more like a large hunk\\\"}\",\"{\\\"text\\\":\\\"§7§oof stone.\\\"}\"],\"minecraft:dyed_color\":{\"rgb\":10511680,\"show_in_tooltip\":false},\"minecraft:hide_additional_tooltip\":{},\"minecraft:custom_data\":{\"ExtraAttributes\":{\"id\":\"DARK_CLAYMORE\"}}}}", fixed.getValue().toString());
	}

	@Test
	void testDiamondNecronHead() {
		//{"id":397,"Count":1,"Damage":3,"tag":{"SkullOwner":{"Id":"efa160b9-7f84-301d-abb6-818f7b1c0964","Properties":{"textures":[{"Value":"ewogICJ0aW1lc3RhbXAiIDogMTYwNTU1MjgyNDM1MiwKICAicHJvZmlsZUlkIiA6ICJhMmY4MzQ1OTVjODk0YTI3YWRkMzA0OTcxNmNhOTEwYyIsCiAgInByb2ZpbGVOYW1lIiA6ICJiUHVuY2giLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOGIxMTVjZGM0NWZkODRmMjFmYmE3YWMwZjJiYzc3YmMzYjYzMDJiZTY3MDg0MmY2ZTExZjY2ZWI1NTdmMTNlZSIKICAgIH0KICB9Cn0="}]}},"ExtraAttributes":{"id":"DIAMOND_NECRON_HEAD"},"display":{"Name":"§cAncient Diamond Necron Head §6✪§6✪§6✪§6✪§6✪§c➎"},"ench":[{"lvl":3,"id":5},{"lvl":1,"id":6}]}}
		JsonElement a = JsonParser.parseString("{\"id\":397,\"Count\":1,\"Damage\":3,\"tag\":{\"SkullOwner\":{\"Id\":\"efa160b9-7f84-301d-abb6-818f7b1c0964\",\"Properties\":{\"textures\":[{\"Value\":\"ewogICJ0aW1lc3RhbXAiIDogMTYwNTU1MjgyNDM1MiwKICAicHJvZmlsZUlkIiA6ICJhMmY4MzQ1OTVjODk0YTI3YWRkMzA0OTcxNmNhOTEwYyIsCiAgInByb2ZpbGVOYW1lIiA6ICJiUHVuY2giLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOGIxMTVjZGM0NWZkODRmMjFmYmE3YWMwZjJiYzc3YmMzYjYzMDJiZTY3MDg0MmY2ZTExZjY2ZWI1NTdmMTNlZSIKICAgIH0KICB9Cn0=\"}]}},\"ExtraAttributes\":{\"id\":\"DIAMOND_NECRON_HEAD\"},\"display\":{\"Name\":\"§cAncient Diamond Necron Head §6✪§6✪§6✪§6✪§6✪§c➎\"},\"ench\":[{\"lvl\":3,\"id\":5},{\"lvl\":1,\"id\":6}]}}");
		Dynamic<JsonElement> fixed = FIXER.update(TypeReferences.LEGACY_ITEM_STACK, new Dynamic<>(JsonOps.INSTANCE, a), FIRST_VERSION, LATEST_VERSION);

		System.out.println("Diamond Necron Head Fixed: " + fixed.getValue());
		Assertions.assertEquals("{\"id\":\"minecraft:player_head\",\"count\":1,\"components\":{\"minecraft:enchantments\":{\"levels\":{\"minecraft:respiration\":3,\"minecraft:aqua_affinity\":1}},\"minecraft:custom_name\":\"{\\\"text\\\":\\\"§cAncient Diamond Necron Head §6✪§6✪§6✪§6✪§6✪§c➎\\\"}\",\"minecraft:profile\":{\"name\":\"\",\"id\":[-274636615,2139369501,-1414102641,2065434980],\"properties\":[{\"name\":\"textures\",\"value\":\"ewogICJ0aW1lc3RhbXAiIDogMTYwNTU1MjgyNDM1MiwKICAicHJvZmlsZUlkIiA6ICJhMmY4MzQ1OTVjODk0YTI3YWRkMzA0OTcxNmNhOTEwYyIsCiAgInByb2ZpbGVOYW1lIiA6ICJiUHVuY2giLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOGIxMTVjZGM0NWZkODRmMjFmYmE3YWMwZjJiYzc3YmMzYjYzMDJiZTY3MDg0MmY2ZTExZjY2ZWI1NTdmMTNlZSIKICAgIH0KICB9Cn0=\"}]},\"minecraft:custom_data\":{\"ExtraAttributes\":{\"id\":\"DIAMOND_NECRON_HEAD\"}}}}", fixed.getValue().toString());
	}

	@Test
	void testTreeCap() {
		//{"id":286,"Damage":0,"Count":1,"tag":{"ench":[{"id":32,"lvl":5}],"Unbreakable":1,"HideFlags":254,"ExtraAttributes":{"id":"TREECAPITATOR_AXE"}}}
		JsonElement a = JsonParser.parseString("{\"id\":286,\"Damage\":0,\"Count\":1,\"tag\":{\"ench\":[{\"id\":32,\"lvl\":5}],\"Unbreakable\":1,\"HideFlags\":254,\"ExtraAttributes\":{\"id\":\"TREECAPITATOR_AXE\"}}}");
		Dynamic<JsonElement> fixed = FIXER.update(TypeReferences.LEGACY_ITEM_STACK, new Dynamic<>(JsonOps.INSTANCE, a), FIRST_VERSION, LATEST_VERSION);

		System.out.println("Treecap Fixed: " + fixed.getValue());
		Assertions.assertEquals("{\"id\":\"minecraft:golden_axe\",\"count\":1,\"components\":{\"minecraft:enchantments\":{\"levels\":{\"minecraft:efficiency\":5}},\"minecraft:dyed_color\":{\"rgb\":10511680,\"show_in_tooltip\":false},\"minecraft:hide_additional_tooltip\":{},\"minecraft:custom_data\":{\"ExtraAttributes\":{\"id\":\"TREECAPITATOR_AXE\"}}}}", fixed.getValue().toString());
	}
}
