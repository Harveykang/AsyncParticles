package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle;

import fun.qu_an.minecraft.asyncparticles.client.core.AsyncQuadParticleRenderState;
import net.minecraft.client.particle.QuadParticleGroup;
import net.minecraft.client.renderer.state.QuadParticleRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(QuadParticleGroup.class)
public class MixinQuadParticleGroup {
	@Redirect(method = "<init>", at = @At(value = "NEW", target = "()Lnet/minecraft/client/renderer/state/QuadParticleRenderState;"))
	private QuadParticleRenderState redirectNewQuadParticleRenderState() {
		return new AsyncQuadParticleRenderState();
	}
}
