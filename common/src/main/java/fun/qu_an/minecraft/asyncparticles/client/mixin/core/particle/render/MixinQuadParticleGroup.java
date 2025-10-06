package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.render;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.particle.QuadParticleGroup;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.state.QuadParticleRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(QuadParticleGroup.class)
public abstract class MixinQuadParticleGroup extends ParticleGroup<SingleQuadParticle> {
	public MixinQuadParticleGroup(ParticleEngine particleEngine) {
		super(particleEngine);
	}

	@WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/SingleQuadParticle;extract(Lnet/minecraft/client/renderer/state/QuadParticleRenderState;Lnet/minecraft/client/Camera;F)V"))
	private void wrapExtract(SingleQuadParticle instance, QuadParticleRenderState quadParticleRenderState, Camera camera, float f, Operation<Void> original) {
		if (!((ParticleAddon) instance).asyncparticles$isTicked()) {
			f += 1.0F;
		}
		original.call(instance, quadParticleRenderState, camera, f);
	}
}
