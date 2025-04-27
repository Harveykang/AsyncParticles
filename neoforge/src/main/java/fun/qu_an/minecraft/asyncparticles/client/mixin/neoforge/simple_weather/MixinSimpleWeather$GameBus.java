package fun.qu_an.minecraft.asyncparticles.client.mixin.neoforge.simple_weather;

import fun.qu_an.minecraft.asyncparticles.client.AsyncTicker;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import tv.soaryn.simpleweather.SimpleWeatherClient;

import java.util.function.Consumer;

@Mixin(value = SimpleWeatherClient.class, remap = false)
public class MixinSimpleWeather$GameBus {
	@Redirect(method = "<init>", at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/neoforged/bus/api/IEventBus;addListener(Ljava/util/function/Consumer;)V"))
	private void renderWeather(IEventBus instance, Consumer<ClientTickEvent.Pre> tConsumer) {
		AsyncTicker.registerEndTickEvent(() -> tConsumer.accept(null));
	}
}
