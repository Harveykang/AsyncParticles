package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle;

import fun.qu_an.minecraft.asyncparticles.client.core.AsyncQuadParticleGroup;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.QuadParticleGroup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ParticleEngine.class)
public class MixinParticleEngine {
	@Redirect(method = "createParticleGroup", at = @At(value = "NEW", target = "(Lnet/minecraft/client/particle/ParticleEngine;Lnet/minecraft/client/particle/ParticleRenderType;)Lnet/minecraft/client/particle/QuadParticleGroup;"))
	private QuadParticleGroup createParticleGroup(ParticleEngine particleEngine, ParticleRenderType particleRenderType) {
		return new AsyncQuadParticleGroup(particleEngine, particleRenderType);
	}
}
