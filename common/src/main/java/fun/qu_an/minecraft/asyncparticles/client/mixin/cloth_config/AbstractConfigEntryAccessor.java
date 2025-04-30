package fun.qu_an.minecraft.asyncparticles.client.mixin.cloth_config;

import me.shedaniel.clothconfig2.api.AbstractConfigEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.function.Consumer;

@Mixin(AbstractConfigEntry.class)
public interface AbstractConfigEntryAccessor {
	@Accessor(value = "saveCallback", remap = false)
	<T> Consumer<T> asyncparticles$getSaveCallback();
}
