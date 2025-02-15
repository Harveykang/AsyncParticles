package fun.qu_an.minecraft.asyncparticles.client.mixin.particlerain;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fun.qu_an.minecraft.asyncparticles.client.AsyncTicker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import pigcart.particlerain.WeatherParticleSpawner;

@Mixin(value = WeatherParticleSpawner.class, remap = false)
public class MixinWeatherParticleSpawner {
	@WrapMethod(method = "update")
	private static void onUpdate(ClientLevel level, Entity entity, float f, Operation<Void> original){
		if (AsyncTicker.shouldTickParticles) {
			AsyncTicker.beforeParticleOperations.add(() -> original.call(level, entity, f));
		}
	}
}
