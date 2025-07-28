package fun.qu_an.minecraft.asyncparticles.client.mixin.particlerain_vs;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import fun.qu_an.minecraft.asyncparticles.client.compat.vs2.VSCompat;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pigcart.particlerain.WeatherParticleManager;
import pigcart.particlerain.config.ModConfig;

@Mixin(WeatherParticleManager.class)
public class MixinWeatherParticleManager {
	@Shadow(remap = false)
	@Final
	private static BlockPos.MutableBlockPos heightmapPos;

	@Inject(method = "spawnParticles", at = @At("HEAD"))
	private static void spawnParticlesHead(ClientLevel level,
										   Holder<Biome> biome,
										   double x,
										   double y,
										   double z,
										   CallbackInfo ci,
										   @Share("canSpawnGroundParticles") LocalBooleanRef canSpawnGroundParticles,
										   @Share("canSpawnRainParticles") LocalBooleanRef canSpawnRainParticles) {
		boolean value = VSCompat.canSpawnWeatherParticle(level, x, y, z);
		canSpawnRainParticles.set(value);
		canSpawnGroundParticles.set(value && VSCompat.canSpawnWeatherParticle(level, x, heightmapPos.getY(), z));
	}

	@ModifyExpressionValue(method = "spawnParticles", at = @At(value = "FIELD", remap = false,
		target = "Lpigcart/particlerain/config/ModConfig$ParticleOptions;enabled:Z"))
	private static boolean modifyEnabled(boolean original,
										 @Local ModConfig.ParticleOptions particleOptions,
										 @Share("canSpawnRainParticles") LocalBooleanRef canSpawnRainParticles,
										 @Share("canSpawnGroundParticles") LocalBooleanRef canSpawnGroundParticles) {
		return original && (particleOptions.onGround ? canSpawnGroundParticles.get() : canSpawnRainParticles.get());
	}
}
