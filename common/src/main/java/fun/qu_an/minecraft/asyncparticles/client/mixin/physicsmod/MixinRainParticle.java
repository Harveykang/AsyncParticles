package fun.qu_an.minecraft.asyncparticles.client.mixin.physicsmod;

import net.diebuddies.minecraft.weather.RainParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RainParticle.class)
public class MixinRainParticle {
	@Redirect(method = "render", at = @At(value = "INVOKE", remap = false, target = "Lcom/mojang/blaze3d/systems/RenderSystem;disableCull()V"))
	private void redirectDisableCull() {
		// do nothing
	}
}
