package fun.qu_an.minecraft.asyncparticles.client.mixin.neoforge.simple_weather;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fun.qu_an.minecraft.asyncparticles.client.AsyncTicker;
import fun.qu_an.minecraft.asyncparticles.client.compat.create.CreateCompat;
import fun.qu_an.minecraft.asyncparticles.client.compat.create.CreateUtil;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import tv.soaryn.simpleweather.SimpleWeather;

@Mixin(targets = "tv.soaryn.simpleweather.SimpleWeather$NeoBus", remap = false)
public interface MixinSimpleWeather$NeoBus {
	@WrapWithCondition(method = {"addRain", "addSnowflake"},
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;addParticle(Lnet/minecraft/core/particles/ParticleOptions;ZDDDDDD)V"))
	private static boolean onAddParticle(ClientLevel level, ParticleOptions particleData, boolean forceAlwaysRender, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
		return CreateCompat.canSpawnWeatherParticle(level, x, y, z);
	}

	@WrapMethod(method = "renderWeather")
	private static void renderWeather(ClientTickEvent.Pre event, Operation<Void> original) {
		if (SimpleWeather.ClientConfig.OverrideWeather.get()) {
			AsyncTicker.addEndTickTask(() -> original.call((Object) null));
		}
	}

	@Redirect(method = "renderWeather",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getDeltaMovement()Lnet/minecraft/world/phys/Vec3;"))
	private static Vec3 getDeltaMovement(LocalPlayer player) {
		Vec3 contraptionMotion = CreateUtil.getContraptionDeltaMovement(player);
		Vec3 deltaMovement = player.getRootVehicle().getDeltaMovement();
		return contraptionMotion != null ? contraptionMotion.add(deltaMovement) : deltaMovement;
	}
}
