package net.azureaaron.legacyitemdfu;

import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.DataFixerBuilder;
import com.mojang.datafixers.schemas.Schema;

import net.azureaaron.legacyitemdfu.fixers.AttributeIdFix;
import net.azureaaron.legacyitemdfu.fixers.BannerIdsFix;
import net.azureaaron.legacyitemdfu.fixers.BannerPatternFormatFix;
import net.azureaaron.legacyitemdfu.fixers.EnchantmentsFix;
import net.azureaaron.legacyitemdfu.fixers.ItemCustomNameAndLoreToTextFix;
import net.azureaaron.legacyitemdfu.fixers.ItemStackComponentizationFix;
import net.azureaaron.legacyitemdfu.fixers.ItemStackUuidsFix;
import net.azureaaron.legacyitemdfu.fixers.NumericItemIdFix;
import net.azureaaron.legacyitemdfu.fixers.PostFlatteningItemIdsFix;
import net.azureaaron.legacyitemdfu.fixers.SpawnEggItemIdFix;
import net.azureaaron.legacyitemdfu.fixers.TheFlatteningFix;
import net.azureaaron.legacyitemdfu.fixers.TooltipDisplayFix;
import net.azureaaron.legacyitemdfu.fixers.UnflattenTextComponentFix;
import net.azureaaron.legacyitemdfu.schemas.Schema1;
import net.azureaaron.legacyitemdfu.schemas.Schema11;
import net.azureaaron.legacyitemdfu.schemas.Schema2;

public final class LegacyItemStackFixer {
	private static final int FIRST_VERSION = 1;
	private static final int LATEST_VERSION = 14;
	private static final DataFixer FIXER = build();

	private static DataFixer build() {
		DataFixerBuilder builder = new DataFixerBuilder(LATEST_VERSION);

		builder.addSchema(1, Schema1::new);

		Schema schema2 = builder.addSchema(2, Schema2::new);
		builder.addFixer(new NumericItemIdFix(schema2, true));

		Schema schema3 = builder.addSchema(3, Schema::new);
		builder.addFixer(new SpawnEggItemIdFix(schema3, true));

		Schema schema4 = builder.addSchema(4, Schema::new);
		builder.addFixer(new BannerIdsFix(schema4, true));

		Schema schema5 = builder.addSchema(5, Schema::new);
		builder.addFixer(new TheFlatteningFix(schema5, true));

		Schema schema6 = builder.addSchema(6, Schema::new);
		builder.addFixer(new PostFlatteningItemIdsFix(schema6, true));

		Schema schema7 = builder.addSchema(7, Schema::new);
		builder.addFixer(new ItemCustomNameAndLoreToTextFix(schema7, true));

		Schema schema8 = builder.addSchema(8, Schema::new);
		builder.addFixer(new EnchantmentsFix(schema8, true));

		Schema schema9 = builder.addSchema(9, Schema::new);
		builder.addFixer(new ItemStackUuidsFix(schema9, true));

		Schema schema10 = builder.addSchema(10, Schema::new);
		builder.addFixer(new BannerPatternFormatFix(schema10, true));

		Schema schema11 = builder.addSchema(11, Schema11::new);
		builder.addFixer(new ItemStackComponentizationFix(schema11, true));

		Schema schema12 = builder.addSchema(12, Schema::new);
		builder.addFixer(new AttributeIdFix(schema12, true));

		Schema schema13 = builder.addSchema(13, Schema::new);
		builder.addFixer(new UnflattenTextComponentFix(schema13, true));

		Schema schema14 = builder.addSchema(14, Schema::new);
		builder.addFixer(new TooltipDisplayFix(schema14, true));

		return builder.build().fixer();
	}

	public static DataFixer getFixer() {
		return FIXER;
	}

	public static int getFirstVersion() {
		return FIRST_VERSION;
	}

	public static int getLatestVersion() {
		return LATEST_VERSION;
	}
}
