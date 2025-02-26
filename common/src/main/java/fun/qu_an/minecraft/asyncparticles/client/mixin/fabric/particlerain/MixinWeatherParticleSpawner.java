package fun.qu_an.minecraft.asyncparticles.client.mixin.fabric.particlerain;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fun.qu_an.minecraft.asyncparticles.client.AsyncTicker;
import fun.qu_an.minecraft.asyncparticles.client.compat.particlerain.CountManagements;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import pigcart.particlerain.WeatherParticleSpawner;

@Mixin(value = WeatherParticleSpawner.class)
public class MixinWeatherParticleSpawner {
	@ModifyExpressionValue(method = "spawnParticle", at = @At(value = "FIELD", remap = false, target = "Lpigcart/particlerain/ParticleRainClient;particleCount:I"))
	private static int modifyParticleCount(int original) {
		return CountManagements.asyncParticles$particleCount.get();
	}

	@ModifyExpressionValue(method = "spawnParticle", at = @At(value = "FIELD", remap = false, target = "Lpigcart/particlerain/ParticleRainClient;fogCount:I"))
	private static int modifyFogCount(int original) {
		return CountManagements.asyncParticles$fogCount.get();
	}

	@WrapMethod(method = "update")
	private static void wrapUpdate(ClientLevel level, Entity entity, float f, Operation<Void> original){
		if (AsyncTicker.shouldTickParticles && level != null) {
			AsyncTicker.END_TICK_OPERATIONS.add(() -> original.call(level, entity, f));
		}
	}
}
