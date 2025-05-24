package fun.qu_an.minecraft.asyncparticles.client.mixin.neoforge.simple_weather;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fun.qu_an.minecraft.asyncparticles.client.api.EndTickOperation;
import fun.qu_an.minecraft.asyncparticles.client.compat.simpleweather.neoforge.SimpleWeatherCompat;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import tv.soaryn.simpleweather.SimpleWeather;

@Mixin(targets = "tv.soaryn.simpleweather.SimpleWeather$NeoBus")
public interface MixinSimpleWeather$NeoBus {
	@WrapMethod(method = "renderWeather", remap = false)
	private static void renderWeather(ClientTickEvent.Pre event, Operation<Void> original) {
		if (SimpleWeather.ClientConfig.OverrideWeather.get()) {
			EndTickOperation.schedule(SimpleWeatherCompat.SIMPLE_WEATHER$RENDER_WEATHER, false, () -> original.call((Object) null));
		}
	}

	@Redirect(method = "renderWeather", remap = false, require = 0,
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getDeltaMovement()Lnet/minecraft/world/phys/Vec3;"))
	private static Vec3 redirectDeltaMovement(LocalPlayer player) {
		return player.getRootVehicle().getDeltaMovement();
	}
}
