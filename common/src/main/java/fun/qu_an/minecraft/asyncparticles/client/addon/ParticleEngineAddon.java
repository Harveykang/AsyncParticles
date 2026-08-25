package fun.qu_an.minecraft.asyncparticles.client.addon;

import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.culling.Frustum;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public interface ParticleEngineAddon {
	void asyncparticle$addRenderType(ParticleRenderType particleRenderType);

	void asyncparticle$setFrustum(Frustum asyncparticle$frustum);

	Frustum asyncparticle$getFrustum();
}
