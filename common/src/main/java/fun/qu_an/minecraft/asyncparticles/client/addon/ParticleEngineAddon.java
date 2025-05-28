package fun.qu_an.minecraft.asyncparticles.client.addon;

import net.minecraft.client.particle.ParticleRenderType;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public interface ParticleEngineAddon {
	void asyncparticle$addRenderType(ParticleRenderType particleRenderType);
}
