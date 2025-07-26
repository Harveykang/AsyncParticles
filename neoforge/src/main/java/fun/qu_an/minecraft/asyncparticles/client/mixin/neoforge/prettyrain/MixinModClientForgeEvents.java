package fun.qu_an.minecraft.asyncparticles.client.mixin.neoforge.prettyrain;

import com.leclowndu93150.particlerain.ParticleRainClient;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import fun.qu_an.minecraft.asyncparticles.client.compat.particlerain.v3.ParticleRainCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ParticleRainClient.class, remap = false)
public class MixinModClientForgeEvents {
	@ModifyExpressionValue(method = "lambda$registerClientCommands$0", at = @At(value = "FIELD", target = "Lcom/leclowndu93150/particlerain/ParticleRainClient;particleCount:I"))
	private static int modifyParticleCount(int original) {
		return ParticleRainCompat.INSTANCE.particleCount.get();
	}

	@ModifyExpressionValue(method = "lambda$registerClientCommands$0", at = @At(value = "FIELD", target = "Lcom/leclowndu93150/particlerain/ParticleRainClient;fogCount:I"))
	private static int modifyFogCount(int original) {
		return ParticleRainCompat.INSTANCE.fogCount.get();
	}

	@Inject(method = "onJoin", at = @At("HEAD"))
	private void onPlayerJoin(CallbackInfo ci) {
		ParticleRainCompat.INSTANCE.clearCounters();
	}
}
