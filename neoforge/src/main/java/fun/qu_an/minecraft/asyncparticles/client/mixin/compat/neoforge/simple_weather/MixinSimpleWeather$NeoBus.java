package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.neoforge.simple_weather;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "tv.soaryn.simpleweather.SimpleWeather$NeoBus")
public interface MixinSimpleWeather$NeoBus {
	@Redirect(method = "renderWeather", remap = false, require = 0,
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getDeltaMovement()Lnet/minecraft/world/phys/Vec3;"))
	private static Vec3 redirectDeltaMovement(LocalPlayer player) {
		return player.getRootVehicle().getDeltaMovement();
	}
}
