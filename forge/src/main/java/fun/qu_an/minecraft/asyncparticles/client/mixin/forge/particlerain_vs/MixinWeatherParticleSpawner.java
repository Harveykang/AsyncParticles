package fun.qu_an.minecraft.asyncparticles.client.mixin.forge.particlerain_vs;

import com.leclowndu93150.particlerain.ParticleRegistry;
import com.leclowndu93150.particlerain.WeatherParticleSpawner;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import fun.qu_an.minecraft.asyncparticles.client.VSClientUtils;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = WeatherParticleSpawner.class)
public class MixinWeatherParticleSpawner {
	@Redirect(method = "spawnParticle", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"))
	private static void onSpawnParticle(ClientLevel level, ParticleOptions particleOptions, double x, double y, double z, double g, double h, double i) {
		if (particleOptions == ParticleRegistry.DUST.get()) {
			if (VSClientUtils.isUnderShipHeightMap(level, x, y, z)) {
				return;
			}
			level.addParticle(particleOptions, x, y, z, g, h, i);
			return;
		}
		if ((particleOptions != ParticleRegistry.RAIN.get() && particleOptions != ParticleRegistry.SNOW.get())
			|| !VSClientUtils.isUnderHeightMap(level, x, y, z)) {
			level.addParticle(particleOptions, x, y, z, g, h, i);
		}
	}
}
