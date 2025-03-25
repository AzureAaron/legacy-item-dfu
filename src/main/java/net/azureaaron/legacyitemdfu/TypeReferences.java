package net.azureaaron.legacyitemdfu;

import org.jetbrains.annotations.ApiStatus;

import com.mojang.datafixers.DSL;

public final class TypeReferences {
	public static final DSL.TypeReference LEGACY_ITEM_STACK = () -> "legacy_item_stack";
	@ApiStatus.Internal
	public static final DSL.TypeReference ITEM_NAME = () -> "legacy_dfu_item_name";
}
