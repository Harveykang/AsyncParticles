package fun.qu_an.minecraft.asyncparticles.client.mixin.shimmer;

import com.lowdragmc.shimmer.client.postprocessing.PostParticle;
import fun.qu_an.minecraft.asyncparticles.client.addon.LightCachedParticleAddon;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(PostParticle.class)
public abstract class MixinPostParticle implements LightCachedParticleAddon {
	@Shadow @Final public Particle parent;

	@Override
	public void asyncparticles$refresh() {
		((LightCachedParticleAddon) parent).asyncparticles$refresh();
	}
}
