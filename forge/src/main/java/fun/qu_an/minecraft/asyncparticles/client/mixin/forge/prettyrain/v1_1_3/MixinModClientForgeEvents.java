package fun.qu_an.minecraft.asyncparticles.client.mixin.forge.prettyrain.v1_1_3;

import com.leclowndu93150.particlerain.ClientStuff;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import fun.qu_an.minecraft.asyncparticles.client.compat.particlerain.ParticleRainCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientStuff.ModClientForgeEvents.class, remap = false)
public class MixinModClientForgeEvents {
	@ModifyExpressionValue(method = "lambda$registerClientCommands$0", at = @At(value = "FIELD", target = "Lcom/leclowndu93150/particlerain/ParticleRainClient;particleCount:I"))
	private static int modifyParticleCount(int original) {
		return ParticleRainCompat.asyncparticles$particleCount.get();
	}

	@ModifyExpressionValue(method = "lambda$registerClientCommands$0", at = @At(value = "FIELD", target = "Lcom/leclowndu93150/particlerain/ParticleRainClient;fogCount:I"))
	private static int modifyFogCount(int original) {
		return ParticleRainCompat.asyncparticles$fogCount.get();
	}

	@Group(name = "asyncparticles$particlerain$ClientStuff$onClearCounters", min = 2, max = 2)
	@Inject(method = {"onPlayerJoin", "onPlayerClone", "onPlayerChangeDimension"}, at = @At("HEAD"))
	private static void onClearCounters1(CallbackInfo ci) {
		ParticleRainCompat.clearCounters();
	}
}
