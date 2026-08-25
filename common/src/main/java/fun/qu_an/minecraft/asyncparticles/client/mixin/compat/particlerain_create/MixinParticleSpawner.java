package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.particlerain_create;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import fun.qu_an.minecraft.asyncparticles.client.compat.create.CreateCompat;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pigcart.particlerain.ParticleSpawner;
import pigcart.particlerain.config.ParticleData;
import pigcart.particlerain.config.Whitelist;
import pigcart.particlerain.particle.CustomParticle;
import pigcart.particlerain.particle.StreakParticle;

@Mixin(ParticleSpawner.class)
public class MixinParticleSpawner {
	@Group(name = "createCompat_tickSkyFX", min = 1, max = 1)
	@Inject(method = "tickSkyFX", at = @At(value = "FIELD", remap = false,
		target = "Lpigcart/particlerain/config/ConfigData$CompatibilityOptions;doSpawnHeightLimit:Z"))
	private static void onTickSkyFX(ClientLevel level,
	                                Vec3 cameraPos,
	                                CallbackInfo ci,
	                                @Local(name = "x") float x,
	                                @Local(name = "y") float y,
	                                @Local(name = "z") float z,
	                                @Share("canSpawn") LocalBooleanRef canSpawn) {
		boolean value = CreateCompat.canSpawnWeatherParticleFloorToInt(level, x, y, z);
		canSpawn.set(value);
	}

	@Dynamic
	@Group(name = "createCompat_tickSkyFX", min = 1, max = 1)
	@Inject(method = "tickSkyFX", at = @At(value = "FIELD", remap = false,
		target = "Lpigcart/particlerain/config/ConfigData$CompatibilityOptions;doSpawnHeightLimit:Z"))
	private static void onTickSkyFX(ClientLevel level,
	                                Vec3 cameraPos,
	                                CallbackInfo ci,
	                                @Local(name = "x") double x,
	                                @Local(name = "y") double y,
	                                @Local(name = "z") double z,
	                                @Share("canSpawn") LocalBooleanRef canSpawn) {
		boolean value = CreateCompat.canSpawnWeatherParticleFloorToInt(level, x, y, z);
		canSpawn.set(value);
	}

	@Dynamic
	@Group(name = "createCompat_tickSkyFX", min = 1, max = 1)
	@Inject(method = "tickSkyFX", at = @At(value = "FIELD", remap = false,
		target = "Lpigcart/particlerain/config/ConfigData$CompatibilityOptions;doSpawnHeightLimit:Z"))
	private static void onTickSkyFX(ClientLevel level,
	                                Vec3 cameraPos,
	                                CallbackInfo ci,
	                                @Local(name = "x") double x,
	                                @Local(name = "y") float y,
	                                @Local(name = "z") double z,
	                                @Share("canSpawn") LocalBooleanRef canSpawn) {
		boolean value = CreateCompat.canSpawnWeatherParticleFloorToInt(level, x, y, z);
		canSpawn.set(value);
	}

	@Definition(id = "getHeight", method = "Lpigcart/particlerain/ParticleSpawner;getHeight(Lnet/minecraft/client/multiplayer/ClientLevel;II)I")
	@Expression("? = getHeight(?, ?, ?)")
	@Inject(method = "tickSurfaceFX", at = @At(value = "MIXINEXTRAS:EXPRESSION", shift = At.Shift.AFTER))
	private static void onTickSurfaceFX(ClientLevel level,
	                                    Vec3 cameraPos,
	                                    CallbackInfo ci,
	                                    @Local(name = "x") double x,
	                                    @Local(name = "y") int y,
	                                    @Local(name = "z") double z,
	                                    @Share("canSpawn") LocalBooleanRef canSpawn) {
		boolean value = CreateCompat.canSpawnWeatherParticleFloorToInt(level, x, y, z);
		canSpawn.set(value);
	}

	@ModifyExpressionValue(method = {"tickSurfaceFX", "tickSkyFX"}, at = @At(value = "FIELD", remap = false,
		target = "Lpigcart/particlerain/config/ParticleData;enabled:Ljava/lang/Boolean;"))
	private static Boolean modifyEnabled(Boolean original,
	                                     @Local ParticleData data,
	                                     @Share("canSpawn") LocalBooleanRef canSpawn) {
		return original && (!data.needsSkyAccess || canSpawn.get());
	}

	@WrapOperation(method = "tickBlockFX", at = @At(value = "NEW", target = "(Lnet/minecraft/client/multiplayer/ClientLevel;DDDLpigcart/particlerain/config/ParticleData;)Lpigcart/particlerain/particle/CustomParticle;"))
	private static CustomParticle onTickBlockFX(ClientLevel level,
	                                            double x,
	                                            double y,
	                                            double z,
	                                            ParticleData data,
	                                            Operation<CustomParticle> original) {
		if (!data.needsSkyAccess || CreateCompat.canSpawnWeatherParticleFloorToInt(level, x, y, z)) {
			return original.call(level, x, y, z, data);
		} else {
			return null;
		}
	}

	@WrapOperation(method = "tickBlockFX", at = @At(value = "NEW",
		target = "(Lnet/minecraft/client/multiplayer/ClientLevel;DDDLnet/minecraft/core/Direction;Lpigcart/particlerain/config/Whitelist$BlockList;)Lpigcart/particlerain/particle/StreakParticle;"))
	private static StreakParticle onTickBlockFX(ClientLevel level,
	                                            double x,
	                                            double y,
	                                            double z,
	                                            Direction direction,
	                                            Whitelist.BlockList blockList,
	                                            Operation<StreakParticle> original,
	                                            @Local(ordinal = 0) ParticleData opts) {
		if (!opts.needsSkyAccess || CreateCompat.canSpawnWeatherParticleFloorToInt(level, x, y, z)) {
			return original.call(level, x, y, z, direction, blockList);
		} else {
			return null;
		}
	}

	/**
	 * @see fun.qu_an.minecraft.asyncparticles.client.mixin.compat.particlerain.MixinParticleSpawner#onTickBlockFX
	 */
//	@WrapWithCondition(method = "tickBlockFX", at = @At(value = "INVOKE",
//		target = "Lnet/minecraft/client/particle/ParticleEngine;add(Lnet/minecraft/client/particle/Particle;)V"))
//	private static boolean onTickBlockFX(ParticleEngine instance, Particle particle) {
//		return particle != null;
//	}

	@WrapWithCondition(method = "tickBlockFX", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/multiplayer/ClientLevel;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"))
	private static boolean onTickBlockFX(ClientLevel instance,
	                                     ParticleOptions particleOptions,
	                                     double x,
	                                     double y,
	                                     double z,
	                                     double g,
	                                     double h,
	                                     double i,
	                                     @Local(ordinal = 0) ClientLevel level,
	                                     @Local(ordinal = 0) ParticleData opts) {
		return !opts.needsSkyAccess || CreateCompat.canSpawnWeatherParticleFloorToInt(level, x, y, z);
	}
}
