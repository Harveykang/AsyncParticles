
package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.light_cache;

import fun.qu_an.minecraft.asyncparticles.client.addon.LightCachedParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleEngineAddon;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import net.minecraft.client.particle.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleEngine.class)
public abstract class MixinParticleEngine implements ParticleEngineAddon {
	@Inject(method = "add", at = @At(value = "HEAD"))
	public void add(Particle p, CallbackInfo ci) {
		if (ConfigHelper.particleLightCache()) {
			// Enable the light only if the particle is added to the current ParticleEngine instance.
			((LightCachedParticleAddon) p).asyncparticles$enableLightCache();
			// refresh the light cache here since this method can run in other threads.
			// so it can avoid to slower the main thread.
			((LightCachedParticleAddon) p).asyncparticles$refresh();
		}
	}
}
