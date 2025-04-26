package fun.qu_an.minecraft.asyncparticles.client.mixin.fabric.particlerain;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import fun.qu_an.minecraft.asyncparticles.client.AsyncTicker;
import fun.qu_an.minecraft.asyncparticles.client.compat.particlerain.ParticleRainCompat;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pigcart.particlerain.ParticleRainClient;

@Mixin(value = ParticleRainClient.class, remap = false)
public class MixinParticleRainClient {
	@ModifyExpressionValue(method = "lambda$onInitializeClient$2", at = @At(value = "FIELD", target = "Lpigcart/particlerain/WeatherParticleManager;particleCount:I"))
	private static int modifyParticleCount(int original) {
		return ParticleRainCompat.asyncparticles$particleCount.get();
	}

	@ModifyExpressionValue(method = "lambda$onInitializeClient$2", at = @At(value = "FIELD", target = "Lpigcart/particlerain/WeatherParticleManager;fogCount:I"))
	private static int modifyFogCount(int original) {
		return ParticleRainCompat.asyncparticles$fogCount.get();
	}

	@Inject(method = "onJoin", at = @At("HEAD"))
	private void onJoin(CallbackInfo ci) {
		ParticleRainCompat.clearCounters();
	}

	@Redirect(method = "onInitializeClient",
		slice = @Slice(from = @At(value = "FIELD", target = "Lnet/fabricmc/fabric/api/client/event/lifecycle/v1/ClientTickEvents;END_CLIENT_TICK:Lnet/fabricmc/fabric/api/event/Event;")),
		at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/fabricmc/fabric/api/event/Event;register(Ljava/lang/Object;)V"))
	private void onRegister(Event<?> instance, Object t) {
		AsyncTicker.registerEndTickEvent(((ClientTickEvents.EndTick) t)::onEndTick);
	}
}
