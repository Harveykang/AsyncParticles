package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fun.qu_an.minecraft.asyncparticles.client.addon.LightCachedParticleAddon;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = Particle.class, priority = 5050)
public abstract class MixinParticle_LightCache_Late implements LightCachedParticleAddon {
	@WrapMethod(method = "getLightColor")
	private int wrapGetLightColor(float partialTick, Operation<Integer> original) {
		return asyncparticles$isEnabledLightCache()
			? asyncparticles$getCachedLight()
			: original.call(partialTick);
	}
}
