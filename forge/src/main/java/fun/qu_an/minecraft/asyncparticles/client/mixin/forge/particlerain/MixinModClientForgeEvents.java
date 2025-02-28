package fun.qu_an.minecraft.asyncparticles.client.mixin.forge.particlerain;

import com.leclowndu93150.particlerain.ClientStuff;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import fun.qu_an.minecraft.asyncparticles.client.compat.particlerain.CountManagements;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientStuff.ModClientForgeEvents.class, remap = false)
public class MixinModClientForgeEvents {
	@ModifyExpressionValue(method = "lambda$registerClientCommands$0", at = @At(value = "FIELD", target = "Lcom/leclowndu93150/particlerain/ParticleRainClient;particleCount:I"))
	private static int modifyParticleCount(int original) {
		return CountManagements.asyncParticles$particleCount.get();
	}

	@ModifyExpressionValue(method = "lambda$registerClientCommands$0", at = @At(value = "FIELD", target = "Lcom/leclowndu93150/particlerain/ParticleRainClient;fogCount:I"))
	private static int modifyFogCount(int original) {
		return CountManagements.asyncParticles$fogCount.get();
	}

	@Inject(method = "onPlayerJoin", at = @At("HEAD"))
	private void onPlayerJoin(CallbackInfo ci) {
		CountManagements.asyncParticles$particleCount.set(0);
		CountManagements.asyncParticles$fogCount.set(0);
	}
}
