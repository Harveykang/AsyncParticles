package fun.qu_an.minecraft.asyncparticles.client.mixin.particlerain;

import fun.qu_an.minecraft.asyncparticles.client.compat.particlerain.v4.ParticleRainAddon;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pigcart.particlerain.config.ModConfig;
import pigcart.particlerain.particle.CustomParticle;

@Mixin(CustomParticle.class)
public abstract class MixinCustomParticle implements ParticleRainAddon {
	@Unique
	private boolean asyncparticles$isRainParticle;

	@Inject(method = "<init>", at = @At("RETURN"))
	private void onInit(ClientLevel level,
						double x,
						double y,
						double z,
						ModConfig.ParticleOptions opts,
						CallbackInfo ci) {
		asyncparticles$isRainParticle = !opts.onGround && opts.gravity > 0.0f && opts.precipitation.contains(Biome.Precipitation.RAIN);
	}

	@Override
	public boolean asyncparticles$isRainParticle() {
		return asyncparticles$isRainParticle;
	}
}
