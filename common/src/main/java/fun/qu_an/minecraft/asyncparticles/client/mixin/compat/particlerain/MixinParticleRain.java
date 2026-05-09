package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.particlerain;

import fun.qu_an.minecraft.asyncparticles.client.compat.particlerain.ParticleRainCompat;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import pigcart.particlerain.ParticleRain;

@Mixin(ParticleRain.class)
public class MixinParticleRain {
	@Redirect(method = "getDebugLines", remap = false, at = @At(value = "FIELD", remap = false,
		target = "Lpigcart/particlerain/ParticleSpawner;particleCount:I", opcode = Opcodes.GETSTATIC))
	private static int modifyParticleCount() {
		return ParticleRainCompat.particleCount.get();
	}
}
