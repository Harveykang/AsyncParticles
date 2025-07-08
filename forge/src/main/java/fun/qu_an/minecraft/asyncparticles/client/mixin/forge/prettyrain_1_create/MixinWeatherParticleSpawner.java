package fun.qu_an.minecraft.asyncparticles.client.mixin.forge.prettyrain_1_create;

import com.leclowndu93150.particlerain.WeatherParticleSpawner;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import fun.qu_an.minecraft.asyncparticles.client.compat.create.CreateCompat;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = WeatherParticleSpawner.class)
public class MixinWeatherParticleSpawner {
	@WrapWithCondition(method = "spawnParticle", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"))
	private static boolean onSpawnParticle(ClientLevel instance, ParticleOptions particleData, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
		return CreateCompat.canSpawnWeatherParticle(instance, x, y, z, 2.0);
	}
}
