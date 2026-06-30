package fun.qu_an.minecraft.asyncparticles.client.mixin.core.weather;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.tick.AsyncTickBehavior;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.WeatherEffectRenderer;
import net.minecraft.server.level.ParticleStatus;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(WeatherEffectRenderer.class)
public class MixinWeatherEffectRenderer {
	@WrapMethod(method = "tickRainParticles")
	private void wrapTickRain(ClientLevel level,
	                          Camera camera,
	                          int ticks,
	                          ParticleStatus particleStatus,
	                          int weatherRadius,
	                          Operation<Void> original) {
		if (ConfigHelper.isTickWeatherAsync()) {
			AsyncTickBehavior.getInstance().getTickTaskManager().addTask(() -> original.call(level, camera, ticks, particleStatus, weatherRadius));
		} else {
			original.call(level, camera, ticks, particleStatus, weatherRadius);
		}
	}
}
