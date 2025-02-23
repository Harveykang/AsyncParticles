package fun.qu_an.minecraft.asyncparticles.client.mixin.forge.particlerain;

import com.leclowndu93150.particlerain.WeatherParticleSpawner;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fun.qu_an.minecraft.asyncparticles.client.AsyncTicker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = WeatherParticleSpawner.class, remap = false)
public class MixinWeatherParticleSpawner {
	@ModifyExpressionValue(method = "spawnParticle", at = @At(value = "FIELD", target = "Lcom/leclowndu93150/particlerain/ParticleRainClient;particleCount:I"))
	private static int modifyParticleCount(int original) {
		return 0;
	}

	@WrapMethod(method = "update")
	private static void onUpdate(ClientLevel level, Entity entity, float f, Operation<Void> original){
		if (AsyncTicker.shouldTickParticles && level != null) {
			AsyncTicker.END_TICK_OPERATIONS.add(() -> original.call(level, entity, f));
		}
	}
}
