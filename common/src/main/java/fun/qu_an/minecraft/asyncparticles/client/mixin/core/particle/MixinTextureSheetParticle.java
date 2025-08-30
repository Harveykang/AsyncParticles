package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle;

import fun.qu_an.minecraft.asyncparticles.client.addon.GpuParticleAddon;
import net.minecraft.client.particle.TextureSheetParticle;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(TextureSheetParticle.class)
public class MixinTextureSheetParticle implements GpuParticleAddon {
	@Override
	public void asyncparticles$postTick(long address) {
		// no-op
	}

	@Override
	public boolean asyncparticles$shouldRender() {
		return true;
	}
}
