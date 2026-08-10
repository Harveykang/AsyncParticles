package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.cloth_config;

import fun.qu_an.minecraft.asyncparticles.client.compat.cloth_config.AbstractConfigEntryAddon;
import me.shedaniel.clothconfig2.gui.entries.TextFieldListEntry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(TextFieldListEntry.class)
public class MixinTextFieldListEntry<T> implements AbstractConfigEntryAddon<T> {
	@Final
	@Shadow(remap = false)
	@Mutable
	protected T original;

	@Override
	public void asyncparticles$setOriginal(T original) {
		this.original = original;
	}
}
