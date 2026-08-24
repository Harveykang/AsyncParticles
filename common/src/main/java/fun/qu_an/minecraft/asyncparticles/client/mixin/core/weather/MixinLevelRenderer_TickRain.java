package fun.qu_an.minecraft.asyncparticles.client.mixin.core.weather;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fun.qu_an.minecraft.asyncparticles.client.core.Diagnostic;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.Phase;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.tick.AsyncTickBehavior;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LevelRenderer.class)
public class MixinLevelRenderer_TickRain {
	@WrapMethod(method = "tickRain")
	private void wrapTickRain(Camera camera, Operation<Void> original) {
		if (ConfigHelper.isTickWeatherAsync()
			&& AsyncTickBehavior.getInstance().isTailTick()) {
			AsyncTickBehavior.getInstance().addTaskEnsureLevelRunning(() -> original.call(camera), Diagnostic::errorAsyncWeatherTick, Phase.WEATHER_TICK);
		} else {
			original.call(camera);
		}
	}
}
