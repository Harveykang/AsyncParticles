package fun.qu_an.minecraft.asyncparticles.client.mixin.neoforge.simple_weather;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fun.qu_an.minecraft.asyncparticles.client.api.EndTickOperation;
import fun.qu_an.minecraft.asyncparticles.client.compat.simpleweather.neoforge.SimpleWeatherCompat;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.spongepowered.asm.mixin.Mixin;
import tv.soaryn.simpleweather.SimpleWeather;

@Mixin(targets = "tv.soaryn.simpleweather.SimpleWeather$GameBus", remap = false)
public interface MixinSimpleWeather$GameBus {
	@WrapMethod(method = "renderWeather")
	private static void renderWeather(ClientTickEvent.Pre event, Operation<Void> original) {
		if (SimpleWeather.ClientConfig.OverrideWeather.get()) {
			EndTickOperation.schedule(SimpleWeatherCompat.SIMPLE_WEATHER$RENDER_WEATHER, () -> original.call((Object) null));
		}
	}
}
