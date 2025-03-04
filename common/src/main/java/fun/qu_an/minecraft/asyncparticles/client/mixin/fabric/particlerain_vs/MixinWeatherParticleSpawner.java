package fun.qu_an.minecraft.asyncparticles.client.mixin.fabric.particlerain_vs;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import fun.qu_an.minecraft.asyncparticles.client.compat.vs2.VSClientUtils;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import pigcart.particlerain.ParticleRainClient;
import pigcart.particlerain.WeatherParticleSpawner;

@Mixin(value = WeatherParticleSpawner.class)
public class MixinWeatherParticleSpawner {
	@WrapOperation(method = "spawnParticle", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"))
	private static void onSpawnParticle(ClientLevel instance, ParticleOptions particleOptions, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, Operation<Void> original) {
		if (particleOptions == ParticleRainClient.DUST) {
			if (VSClientUtils.isUnderShipHeightMap(instance, x, y, z)) {
				return;
			}
			original.call(instance, particleOptions, x, y, z, xSpeed, ySpeed, zSpeed);
			return;
		}
		if ((particleOptions != ParticleRainClient.RAIN && particleOptions != ParticleRainClient.SNOW)
			|| !VSClientUtils.isUnderHeightMapIncludeShips(instance, x, y, z)) {
			original.call(instance, particleOptions, x, y, z, xSpeed, ySpeed, zSpeed);
		}
	}
}
