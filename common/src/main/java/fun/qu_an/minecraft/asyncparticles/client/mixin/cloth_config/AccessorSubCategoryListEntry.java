package fun.qu_an.minecraft.asyncparticles.client.mixin.cloth_config;

import me.shedaniel.clothconfig2.gui.entries.SubCategoryListEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SubCategoryListEntry.class)
public interface AccessorSubCategoryListEntry {
	@Accessor(value = "expanded", remap = false)
	boolean asyncparticles$isExpanded();
}
