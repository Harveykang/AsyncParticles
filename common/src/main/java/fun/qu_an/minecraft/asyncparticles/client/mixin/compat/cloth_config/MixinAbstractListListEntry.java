package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.cloth_config;

import fun.qu_an.minecraft.asyncparticles.client.compat.cloth_config.AbstractListListEntryAddon;
import me.shedaniel.clothconfig2.gui.entries.AbstractListListEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@SuppressWarnings("UnstableApiUsage")
@Mixin(AbstractListListEntry.class)
public class MixinAbstractListListEntry<T> implements AbstractListListEntryAddon<T> {
	@Shadow(remap = false)
	protected List<T> original;

	@Override
	public void asyncparticles$setOriginal(List<T> list) {
		this.original = list;
	}
}
