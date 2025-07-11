package fun.qu_an.minecraft.asyncparticles.client.mixin.neoforge.simple_weather;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fun.qu_an.minecraft.asyncparticles.client.api.EndTickOperation;
import fun.qu_an.minecraft.asyncparticles.client.compat.simpleweather.neoforge.SimpleWeatherCompat;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.spongepowered.asm.mixin.Mixin;
import tv.soaryn.simpleweather.configs.SimpleWeatherConfigs;
import tv.soaryn.simpleweather.weather.WeatherFX;

@Mixin(WeatherFX.class)
public class MixinWeatherFX {
	@WrapMethod(method = "renderWeather", remap = false)
	private static void renderWeather(ClientTickEvent.Pre event, Operation<Void> original) {
		if (SimpleWeatherConfigs.Client.OverrideWeather.get()) {
			EndTickOperation.schedule(SimpleWeatherCompat.SIMPLE_WEATHER$RENDER_WEATHER, () -> original.call((Object) null));
		}
	}
}
