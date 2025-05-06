package fun.qu_an.minecraft.asyncparticles.client.mixin.cloth_config;

import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractConfigListEntry.class)
public interface AbstractConfigListEntryAccessor {
	@Accessor(value = "requiresRestart", remap = false)
	boolean asyncparticles$isRequiresRestart();
}
