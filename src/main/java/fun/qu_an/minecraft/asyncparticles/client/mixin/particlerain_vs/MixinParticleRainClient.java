package fun.qu_an.minecraft.asyncparticles.client.mixin.particlerain_vs;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import fun.qu_an.minecraft.asyncparticles.client.AsyncTicker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import pigcart.particlerain.ParticleRainClient;

@Mixin(ParticleRainClient.class)
public class MixinParticleRainClient {
	@WrapOperation(method = "onTick", at = @At(value = "INVOKE", target = "Lpigcart/particlerain/WeatherParticleSpawner;update(Lnet/minecraft/client/multiplayer/ClientLevel;Lnet/minecraft/world/entity/Entity;F)V"))
	private void onTick(ClientLevel level, Entity entity, float f, Operation<Void> original) {
		if (AsyncTicker.shouldTickParticles) {
			AsyncTicker.beforeParticleOperations.add(() -> original.call(level, entity, f));
		}
	}
}
