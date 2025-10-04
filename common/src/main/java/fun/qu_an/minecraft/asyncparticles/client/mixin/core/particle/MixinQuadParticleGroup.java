package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import fun.qu_an.minecraft.asyncparticles.client.core.AsyncQuadParticleGroup;
import fun.qu_an.minecraft.asyncparticles.client.core.render.AsyncQuadParticleRenderState;
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

	@WrapOperation(method = "<init>", at = @At(value = "NEW", target = "()Lnet/minecraft/client/renderer/state/QuadParticleRenderState;"))
	private QuadParticleRenderState redirectNewQuadParticleRenderState(Operation<QuadParticleRenderState> original) {
		return (Object) this instanceof AsyncQuadParticleGroup ? new AsyncQuadParticleRenderState() : original.call();
	}
}
