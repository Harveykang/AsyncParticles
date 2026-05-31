package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.async_render;

import fun.qu_an.minecraft.asyncparticles.client.core.particle.async_render.DualAsyncQuadParticleRenderState;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.particle.QuadParticleGroup;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(QuadParticleGroup.class)
public abstract class MixinQuadParticleGroup extends ParticleGroup<SingleQuadParticle> {
	@Redirect(method = "<init>", at = @At(value = "NEW",
		target = "()Lnet/minecraft/client/renderer/state/level/QuadParticleRenderState;"))
	private QuadParticleRenderState redirectNewQuadParticleRenderState() {
		if (((QuadParticleGroup) (Object) this).getClass() == QuadParticleGroup.class) {
			return new DualAsyncQuadParticleRenderState(this);
		} else {
			return new QuadParticleRenderState();
		}
	}

	public MixinQuadParticleGroup(ParticleEngine particleEngine) {
		super(particleEngine);
	}
}
