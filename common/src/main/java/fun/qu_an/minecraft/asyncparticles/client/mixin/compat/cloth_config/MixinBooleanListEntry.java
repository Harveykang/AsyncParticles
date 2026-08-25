package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.cloth_config;

import fun.qu_an.minecraft.asyncparticles.client.compat.cloth_config.AbstractConfigEntryAddon;
import me.shedaniel.clothconfig2.gui.entries.BooleanListEntry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BooleanListEntry.class)
public class MixinBooleanListEntry implements AbstractConfigEntryAddon<Boolean> {
	@Final
	@Shadow(remap = false)
	@Mutable
	private boolean original;

	@Override
	public void asyncparticles$setOriginal(Boolean original) {
		this.original = original;
	}
}
