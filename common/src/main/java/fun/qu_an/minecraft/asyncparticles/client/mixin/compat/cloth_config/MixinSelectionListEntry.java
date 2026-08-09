package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.cloth_config;

import com.google.common.collect.ImmutableList;
import fun.qu_an.minecraft.asyncparticles.client.compat.cloth_config.AbstractConfigEntryAddon;
import me.shedaniel.clothconfig2.gui.entries.SelectionListEntry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(SelectionListEntry.class)
public class MixinSelectionListEntry<T> implements AbstractConfigEntryAddon<T> {
	@Mutable
	@Shadow(remap = false)
	@Final
	private int original;

	@Shadow(remap = false)
	@Final
	private ImmutableList<T> values;

	@Override
	public void asyncparticles$setOriginal(T original) {
		this.original = this.values.indexOf(original);
	}
}
