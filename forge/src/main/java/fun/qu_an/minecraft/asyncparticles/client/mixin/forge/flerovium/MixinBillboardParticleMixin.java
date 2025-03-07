package fun.qu_an.minecraft.asyncparticles.client.mixin.forge.flerovium;

import com.bawnorton.mixinsquared.TargetHandler;
import fun.qu_an.minecraft.asyncparticles.client.SingleQuadParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.config.SimplePropertiesConfig;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.SingleQuadParticle;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = SingleQuadParticle.class, priority = 1001)
public abstract class MixinBillboardParticleMixin implements SingleQuadParticleAddon {
	@Dynamic
	@Shadow(remap = false)
	private int flerovium$getLightColorCached(float pt, Camera camera) {
		throw new AssertionError();
	}

	@Dynamic
	@TargetHandler(
		mixin = "com.moepus.flerovium.mixins.Particle.SingleQuadParticleMixin",
		name = "renderFast"
	)
	@Redirect(method = "@MixinSquared:Handler",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/SingleQuadParticle;flerovium$getLightColorCached(FLnet/minecraft/client/Camera;)I"))
	private int redirectGetLightColor(SingleQuadParticle particle, float pt, Camera camera) {
		return SimplePropertiesConfig.particleLightCache() ? asyncParticles$getLight() : flerovium$getLightColorCached(pt, camera);
	}
}
