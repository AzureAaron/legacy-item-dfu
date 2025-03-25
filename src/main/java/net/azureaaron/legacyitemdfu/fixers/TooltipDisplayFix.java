package net.azureaaron.legacyitemdfu.fixers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;

import net.azureaaron.legacyitemdfu.TypeReferences;

public class TooltipDisplayFix extends DataFix {
	/**
	 * Components whose tooltips were hidden by the pre-1.21.5 {@code hide_additional_tooltip} component.
	 */
	private static final List<String> HIDE_ADDITIONAL_TOOLTIP_COMPONENTS = List.of(
			"minecraft:banner_patterns",
			"minecraft:bees",
			"minecraft:block_entity_data",
			"minecraft:block_state",
			"minecraft:bundle_contents",
			"minecraft:charged_projectiles",
			"minecraft:container",
			"minecraft:container_loot",
			"minecraft:firework_explosion",
			"minecraft:fireworks",
			"minecraft:instrument",
			"minecraft:map_id",
			"minecraft:painting/variant",
			"minecraft:pot_decorations",
			"minecraft:potion_contents",
			"minecraft:tropical_fish/pattern",
			"minecraft:written_book_content"
		);

	public TooltipDisplayFix(Schema outputSchema, boolean changesType) {
		super(outputSchema, changesType);
	}

	@Override
	protected TypeRewriteRule makeRule() {
		Type<?> type = this.getInputSchema().getType(TypeReferences.LEGACY_ITEM_STACK);
		OpticFinder<?> componentsFinder = type.findField("components");

		return this.fixTypeEverywhereTyped(
				"Tooltip Display Fix",
				type,
				itemStackTyped -> itemStackTyped.updateTyped(componentsFinder, this::fix));
	}

	private Typed<?> fix(Typed<?> componentsTyped) {
		return componentsTyped.update(DSL.remainderFinder(), dynamic -> {
			List<String> hiddenComponents = new ArrayList<>(); //Lists ensures a consistent order

			dynamic = fixComponent(dynamic, "minecraft:can_break", hiddenComponents);
			dynamic = fixComponent(dynamic, "minecraft:can_place_on", hiddenComponents);
			dynamic = fixComponent(dynamic, "minecraft:trim", hiddenComponents);
			dynamic = fixComponent(dynamic, "minecraft:unbreakable", hiddenComponents);
			dynamic = fixAndInlineComponent(dynamic, "minecraft:dyed_color", "rgb", hiddenComponents);
			dynamic = fixAndInlineComponent(dynamic, "minecraft:attribute_modifiers", "modifiers", hiddenComponents);
			dynamic = fixAndInlineComponent(dynamic, "minecraft:enchantments", "levels", hiddenComponents);
			dynamic = fixAndInlineComponent(dynamic, "minecraft:stored_enchantments", "levels", hiddenComponents);

			boolean shouldHideEntireTooltip = dynamic.get("minecraft:hide_tooltip").result().isPresent();
			dynamic = dynamic.remove("minecraft:hide_tooltip");

			boolean shouldHideAdditionalTooltips = dynamic.get("minecraft:hide_additional_tooltip").result().isPresent();
			dynamic = dynamic.remove("minecraft:hide_additional_tooltip");

			//Handle all the components hidden by the legacy hide_additional_tooltip component
			if (shouldHideAdditionalTooltips) {
				for (String id : HIDE_ADDITIONAL_TOOLTIP_COMPONENTS) {
					if (dynamic.get(id).result().isPresent()) {
						hiddenComponents.add(id);
					}
				}
			}

			//Construct the new tooltip_display component if necessary
			return hiddenComponents.isEmpty() && !shouldHideEntireTooltip ? dynamic : dynamic.set(
					"minecraft:tooltip_display",
					dynamic.createMap(
							Map.of(
									dynamic.createString("hide_tooltip"),
									dynamic.createBoolean(shouldHideEntireTooltip),
									dynamic.createString("hidden_components"),
									dynamic.createList(hiddenComponents.stream().map(dynamic::createString))
									)
					));
		});
	}

	private static Dynamic<?> fixComponent(Dynamic<?> dynamic, String id, List<String> hiddenComponents) {
		return fixComponent(dynamic, id, hiddenComponents, UnaryOperator.identity());
	}

	private static Dynamic<?> fixAndInlineComponent(Dynamic<?> dynamic, String id, String fieldToInline, List<String> hiddenComponents) {
		return fixComponent(dynamic, id, hiddenComponents, component -> DataFixUtils.orElse(component.get(fieldToInline).result(), component));
	}

	private static Dynamic<?> fixComponent(Dynamic<?> dynamic, String id, List<String> hiddenComponents, UnaryOperator<Dynamic<?>> fixer) {
		return dynamic.update(id, component -> {
			boolean showTooltip = component.get("show_in_tooltip").asBoolean(true);

			if (!showTooltip) {
				hiddenComponents.add(id);
			}

			return fixer.apply(component.remove("show_in_tooltip"));
		});
	}
}
