package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.cloth_config;
import me.shedaniel.clothconfig2.gui.entries.BaseListEntry;
import net.minecraft.client.gui.components.AbstractWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import java.util.function.Supplier;

@Mixin(BaseListEntry.class)
public interface AccessorBaseListEntry {
	@Accessor(value = "defaultValue", remap = false)
	<T> Supplier<List<T>> asyncparticles$getDefaultValue();

	@Accessor(value = "resetWidget", remap = false)
	AbstractWidget asyncparticles$getResetWidget();
}
