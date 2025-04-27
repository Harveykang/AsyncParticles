package fun.qu_an.minecraft.asyncparticles.client.mixin.neoforge.simple_weather_create;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import fun.qu_an.minecraft.asyncparticles.client.compat.create.CreateCompat;
import fun.qu_an.minecraft.asyncparticles.client.compat.create.CreateUtil;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "tv.soaryn.simpleweather.SimpleWeather$NeoBus")
public interface MixinSimpleWeather$NeoBus {
	@WrapWithCondition(method = {"addRain", "addSnowflake"}, remap = false,
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;addParticle(Lnet/minecraft/core/particles/ParticleOptions;ZDDDDDD)V"))
	private static boolean onAddParticle(ClientLevel level, ParticleOptions particleData, boolean forceAlwaysRender, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
		return CreateCompat.canSpawnWeatherParticle(level, x, y, z);
	}

	@Redirect(method = "renderWeather", remap = false,
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getDeltaMovement()Lnet/minecraft/world/phys/Vec3;"))
	private static Vec3 getDeltaMovement(LocalPlayer player) {
		Vec3 contraptionMotion = CreateUtil.getContraptionDeltaMovement(player);
		Vec3 deltaMovement = player.getRootVehicle().getDeltaMovement();
		return contraptionMotion != null ? contraptionMotion.add(deltaMovement) : deltaMovement;
	}
}
