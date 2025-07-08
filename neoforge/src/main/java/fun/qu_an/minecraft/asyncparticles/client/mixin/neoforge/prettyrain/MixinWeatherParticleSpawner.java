package fun.qu_an.minecraft.asyncparticles.client.mixin.neoforge.prettyrain;

import com.leclowndu93150.particlerain.WeatherParticleSpawner;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import fun.qu_an.minecraft.asyncparticles.client.compat.particlerain.ParticleRainCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = WeatherParticleSpawner.class, remap = false)
public class MixinWeatherParticleSpawner {
	@ModifyExpressionValue(method = "spawnParticle", at = @At(value = "FIELD", remap = false, target = "Lcom/leclowndu93150/particlerain/ParticleRainClient;particleCount:I"))
	private static int modifyParticleCount(int original) {
		return ParticleRainCompat.asyncparticles$particleCount.get();
	}

	@ModifyExpressionValue(method = "spawnParticle", at = @At(value = "FIELD", remap = false, target = "Lcom/leclowndu93150/particlerain/ParticleRainClient;fogCount:I"))
	private static int modifyFogCount(int original) {
		return ParticleRainCompat.asyncparticles$fogCount.get();
	}
}
