package fun.qu_an.minecraft.asyncparticles.client.mixin.sodium;

import com.bawnorton.mixinsquared.TargetHandler;
import fun.qu_an.minecraft.asyncparticles.client.SingleQuadParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.config.SimplePropertiesConfig;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SingleQuadParticle;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = SingleQuadParticle.class, priority = 1001)
public abstract class MixinBillboardParticleMixin extends Particle implements SingleQuadParticleAddon {
	protected MixinBillboardParticleMixin(ClientLevel level, double x, double y, double z) {
		super(level, x, y, z);
	}

	@Dynamic
	@TargetHandler(
		mixin = "net.caffeinemc.mods.sodium.mixin.features.render.particle.SingleQuadParticleMixin",
		name = "renderRotatedQuad"
	)
	@Redirect(method = "@MixinSquared:Handler", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/SingleQuadParticle;getLightColor(F)I"))
	private int redirectGetLightColor(SingleQuadParticle instance, float v) {
		return SimplePropertiesConfig.particleLightCache() ? asyncParticles$getLight() : instance.getLightColor(v);
	}
}
