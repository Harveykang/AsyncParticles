package fun.qu_an.minecraft.asyncparticles.client.mixin.fabric.particlerain;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import fun.qu_an.minecraft.asyncparticles.client.compat.particlerain.CountManagements;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import pigcart.particlerain.ParticleRainClient;

@Mixin(value = ParticleRainClient.class, remap = false)
public class MixinParticleRainClient {
	@ModifyExpressionValue(method = "lambda$onInitializeClient$0", at = @At(value = "FIELD", target = "Lpigcart/particlerain/ParticleRainClient;particleCount:I"))
	private static int modifyParticleCount(int original) {
		return CountManagements.asyncParticles$particleCount.get();
	}

	@ModifyExpressionValue(method = "lambda$onInitializeClient$0", at = @At(value = "FIELD", target = "Lpigcart/particlerain/ParticleRainClient;fogCount:I"))
	private static int modifyFogCount(int original) {
		return CountManagements.asyncParticles$fogCount.get();
	}
}
