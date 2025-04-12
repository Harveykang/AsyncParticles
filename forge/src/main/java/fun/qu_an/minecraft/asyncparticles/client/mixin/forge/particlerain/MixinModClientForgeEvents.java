package fun.qu_an.minecraft.asyncparticles.client.mixin.forge.particlerain;

import com.leclowndu93150.particlerain.ClientStuff;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import fun.qu_an.minecraft.asyncparticles.client.compat.particlerain.ParticleRainCompat;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientStuff.ModClientForgeEvents.class, remap = false)
public class MixinModClientForgeEvents {
	@ModifyExpressionValue(method = "lambda$registerClientCommands$0", at = @At(value = "FIELD", target = "Lcom/leclowndu93150/particlerain/ParticleRainClient;particleCount:I"))
	private static int modifyParticleCount(int original) {
		return ParticleRainCompat.asyncParticles$particleCount.get();
	}

	@ModifyExpressionValue(method = "lambda$registerClientCommands$0", at = @At(value = "FIELD", target = "Lcom/leclowndu93150/particlerain/ParticleRainClient;fogCount:I"))
	private static int modifyFogCount(int original) {
		return ParticleRainCompat.asyncParticles$fogCount.get();
	}

	@Dynamic
	@Inject(method = "onPlayerJoin", require = 0, at = @At("HEAD"))
	private void onClearCounters(CallbackInfo ci) {
		ParticleRainCompat.clearCounters();
	}

	@Dynamic
	@Inject(method = {"onPlayerJoin", "onPlayerClone"}, require = 0, at = @At("HEAD"))
	private static void onClearCounters2(CallbackInfo ci) {
		ParticleRainCompat.clearCounters();
	}
}
