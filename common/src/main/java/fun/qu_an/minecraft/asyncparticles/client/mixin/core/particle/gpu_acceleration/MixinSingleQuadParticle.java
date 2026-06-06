package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.gpu_acceleration;

import fun.qu_an.minecraft.asyncparticles.client.addon.GpuParticleAddon;
import net.minecraft.client.particle.SingleQuadParticle;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SingleQuadParticle.class)
public class MixinSingleQuadParticle implements GpuParticleAddon {
	@Override
	public void asyncparticles$postTick(long address) {
		// no-op
	}

	@Override
	public boolean asyncparticles$shouldRender() {
		return true;
	}
}
