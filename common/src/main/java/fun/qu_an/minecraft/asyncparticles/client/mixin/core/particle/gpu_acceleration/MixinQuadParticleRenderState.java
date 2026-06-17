package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.gpu_acceleration;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import fun.qu_an.minecraft.asyncparticles.client.addon.GpuParticleGroup;
import fun.qu_an.minecraft.asyncparticles.client.addon.GpuQuadParticleRenderState;
import net.minecraft.client.particle.QuadParticleGroup;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(QuadParticleRenderState.class)
public class MixinQuadParticleRenderState implements GpuQuadParticleRenderState {
	@Unique
	private GpuParticleGroup asyncparticles$gpuParticleGroup;

	@Definition(id = "particleCount", field = "Lnet/minecraft/client/renderer/state/level/QuadParticleRenderState;particleCount:I")
	@Expression("this.particleCount > 0")
	@ModifyExpressionValue(method = "submit", at = @At("MIXINEXTRAS:EXPRESSION"))
	private boolean submit(boolean original) {
		return original || !asyncparticles$gpuParticleGroup.asyncparticles$getGpuParticles().isEmpty();
	}

	@Override
	public GpuParticleGroup asyncparticles$gpuParticleGroup() {
		return asyncparticles$gpuParticleGroup;
	}

	@Override
	public void asyncparticles$setGroup(QuadParticleGroup quadParticleGroup) {
		this.asyncparticles$gpuParticleGroup = (GpuParticleGroup) quadParticleGroup;
	}
}
