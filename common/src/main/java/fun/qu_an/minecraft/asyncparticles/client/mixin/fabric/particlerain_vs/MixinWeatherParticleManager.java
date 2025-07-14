package fun.qu_an.minecraft.asyncparticles.client.mixin.fabric.particlerain_vs;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import fun.qu_an.minecraft.asyncparticles.client.compat.vs2.VSCompat;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import pigcart.particlerain.WeatherParticleManager;

@Mixin(WeatherParticleManager.class)
public class MixinWeatherParticleManager {
	@WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lpigcart/particlerain/WeatherParticleManager;spawnParticles(Lnet/minecraft/client/multiplayer/ClientLevel;Lnet/minecraft/core/Holder;DDD)V"))
	private static boolean spawnParticles(ClientLevel level,
										  Holder<Biome> biomeHolder,
										  double x,
										  double y,
										  double z){
		return VSCompat.canSpawnWeatherParticle(level, x, y, z, 1.5);
	}
}
