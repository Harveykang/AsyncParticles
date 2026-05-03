package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.neoforge.simple_weather_create;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import fun.qu_an.minecraft.asyncparticles.client.compat.create.neoforge.CreateUtilImpl;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "tv.soaryn.simpleweather.SimpleWeather$NeoBus")
public interface MixinSimpleWeather$NeoBus {
//	@WrapWithCondition(method = {"addRain", "addSnowflake"}, remap = false,
//		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;addParticle(Lnet/minecraft/core/particles/ParticleOptions;ZDDDDDD)V"))
//	private static boolean onAddParticle(ClientLevel level, ParticleOptions particleData, boolean forceAlwaysRender, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
//		return CreateCompat.canSpawnWeatherParticle(level, x, y, z);
//	}

	@Group(name = "asyncparticles:redirectDeltaMovement", min = 1, max = 1)
	@WrapOperation(method = "renderWeather", remap = false,
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getDeltaMovement()Lnet/minecraft/world/phys/Vec3;"))
	private static Vec3 redirectDeltaMovement(LocalPlayer player, Operation<Vec3> original) {
		Vec3 contraptionMotion = CreateUtilImpl.getContraptionDeltaMovement(player);
		Vec3 deltaMovement = original.call(player);
		return contraptionMotion != null ? contraptionMotion.add(deltaMovement) : deltaMovement;
	}

	@Dynamic
	@Group(name = "asyncparticles:redirectDeltaMovement", min = 1, max = 1)
	@Redirect(method = "renderWeather", remap = false,
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getDeltaMovement()Lnet/minecraft/world/phys/Vec3;"))
	private static Vec3 redirectDeltaMovement2(Entity entity) {
		Vec3 contraptionMotion = CreateUtilImpl.getContraptionDeltaMovement(entity);
		Vec3 deltaMovement = entity.getDeltaMovement();
		return contraptionMotion != null ? contraptionMotion.add(deltaMovement) : deltaMovement;
	}
}
