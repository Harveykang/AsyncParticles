package fun.qu_an.minecraft.asyncparticles.client.mixin;

import net.minecraft.client.gui.Font;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Font.class)
public class MixinFont {
	@Redirect(method = "<init>",
		at = @At(value = "INVOKE", target ="Lnet/minecraft/util/RandomSource;create()Lnet/minecraft/util/RandomSource;"))
	private RandomSource onInit() {
		return RandomSource.createNewThreadLocalInstance();
	}
}
