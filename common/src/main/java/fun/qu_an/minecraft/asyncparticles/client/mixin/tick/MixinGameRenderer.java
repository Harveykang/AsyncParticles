package fun.qu_an.minecraft.asyncparticles.client.mixin.tick;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import fun.qu_an.minecraft.asyncparticles.client.AsyncTicker;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {
	@WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;tickRain(Lnet/minecraft/client/Camera;)V"))
	private void wrapTickParticles(LevelRenderer instance, Camera camera, Operation<Void> original) {
		if (ConfigHelper.isTickWeatherAsync()) {
			AsyncTicker.PARTICLE_OPERATIONS.add(() -> original.call(instance, camera));
		} else {
			original.call(instance, camera);
		}
	}
}
