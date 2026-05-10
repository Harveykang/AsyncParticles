package fun.qu_an.minecraft.asyncparticles.client.config;

import fun.qu_an.minecraft.asyncparticles.client.mixin.compat.cloth_config.AbstractConfigEntryAccessor;
import fun.qu_an.minecraft.asyncparticles.client.mixin.compat.cloth_config.AbstractConfigListEntryAccessor;
import fun.qu_an.minecraft.asyncparticles.client.mixin.compat.cloth_config.BaseListEntryAccessor;
import me.shedaniel.clothconfig2.gui.entries.StringListListEntry;

public class StringListListEntryFixRestart extends StringListListEntry {
	private final boolean requiresRestart;

	public StringListListEntryFixRestart(StringListListEntry toFix) {
		super(toFix.getFieldName(),
			toFix.getValue(),
			toFix.isExpanded(),
			toFix.getTooltipSupplier(),
			((AbstractConfigEntryAccessor) toFix).asyncparticles$getSaveCallback(),
			((BaseListEntryAccessor) toFix).asyncparticles$getDefaultValue(),
			((BaseListEntryAccessor) toFix).asyncparticles$getResetWidget().getMessage(),
			false,
			toFix.isDeleteButtonEnabled(),
			toFix.insertInFront());
		this.requiresRestart = ((AbstractConfigListEntryAccessor) toFix).asyncparticles$isRequiresRestart();
	}

	@Override
	public boolean isRequiresRestart() {
		return requiresRestart ||
			   super.isRequiresRestart();
	}
}
