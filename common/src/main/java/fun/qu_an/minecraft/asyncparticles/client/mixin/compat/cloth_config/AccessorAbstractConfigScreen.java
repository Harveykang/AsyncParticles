package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.cloth_config;

import me.shedaniel.clothconfig2.gui.AbstractConfigScreen;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractConfigScreen.class)
public interface AccessorAbstractConfigScreen {
	@Accessor
	Screen getParent();
}
