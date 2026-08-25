package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.render;

import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleEngineAddon;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.culling.Frustum;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.*;

@Mixin(value = ParticleEngine.class, priority = 500)
public abstract class MixinParticleEngine implements ParticleEngineAddon {
	@Unique
	private Frustum asyncparticle$frustum = new Frustum(new Matrix4f(), new Matrix4f());
	@Override
	public void asyncparticle$setFrustum(Frustum asyncparticle$frustum) {
		this.asyncparticle$frustum = asyncparticle$frustum;
	}

	@Override
	public Frustum asyncparticle$getFrustum() {
		return asyncparticle$frustum;
	}
}
