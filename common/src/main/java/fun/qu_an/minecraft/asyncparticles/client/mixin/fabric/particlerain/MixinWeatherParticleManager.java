package fun.qu_an.minecraft.asyncparticles.client.mixin.fabric.particlerain;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fun.qu_an.minecraft.asyncparticles.client.AsyncTicker;
import fun.qu_an.minecraft.asyncparticles.client.compat.particlerain.ParticleRainCompat;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pigcart.particlerain.WeatherParticleManager;

@Mixin(value = WeatherParticleManager.class, remap = false)
public class MixinWeatherParticleManager {
	@ModifyExpressionValue(method = "spawnParticle", at = @At(value = "FIELD", target = "Lpigcart/particlerain/WeatherParticleManager;particleCount:I"))
	private static int modifyParticleCount(int original) {
		return ParticleRainCompat.asyncParticles$particleCount.get();
	}

	@ModifyExpressionValue(method = "spawnParticle", at = @At(value = "FIELD", target = "Lpigcart/particlerain/WeatherParticleManager;fogCount:I"))
	private static int modifyFogCount(int original) {
		return ParticleRainCompat.asyncParticles$fogCount.get();
	}

	@Inject(method = "resetParticleCount", at = @At("HEAD"))
	private static void resetParticleCount(CallbackInfo ci) {
		ParticleRainCompat.clearCounters();
	}
}
