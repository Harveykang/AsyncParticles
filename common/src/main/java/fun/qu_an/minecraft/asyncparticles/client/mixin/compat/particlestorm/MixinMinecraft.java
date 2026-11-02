package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.particlestorm;

import fun.qu_an.minecraft.asyncparticles.client.compat.particlestorm.ParticleStormCompat;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MixinMinecraft {
	@Inject(method = "close", at = @At("HEAD"), remap = false)
	private void releaseRenderer(CallbackInfo ci) {
		ParticleStormCompat.close();
	}
}
