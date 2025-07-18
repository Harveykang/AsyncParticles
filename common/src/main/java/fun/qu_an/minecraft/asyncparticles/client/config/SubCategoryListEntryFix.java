package fun.qu_an.minecraft.asyncparticles.client.config;

import fun.qu_an.minecraft.asyncparticles.client.mixin.cloth_config.AccessorSubCategoryListEntry;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.gui.entries.SubCategoryListEntry;

public class SubCategoryListEntryFix extends SubCategoryListEntry {
	public SubCategoryListEntryFix(SubCategoryListEntry entry) {
		super(entry.getCategoryName(), entry.getValue(), ((AccessorSubCategoryListEntry) entry).asyncparticles$isExpanded());
	}

	@Override
	public boolean isRequiresRestart() {
		for (AbstractConfigListEntry<?> entry : getValue())
			if (entry.isRequiresRestart() && entry.isEdited())
				return true;
		return false;
	}

}
