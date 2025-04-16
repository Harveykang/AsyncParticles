package fun.qu_an.minecraft.asyncparticles.client.mixin.forge.physicsmod;

import com.bawnorton.mixinsquared.TargetHandler;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import fun.qu_an.minecraft.asyncparticles.client.compat.vs2.VSClientUtils;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.particles.ParticleOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = LevelRenderer.class, priority = 1100)
public class MixinLevelRenderer {
	@SuppressWarnings("MixinExtrasOperationParameters")
	@TargetHandler(
		name = "tickRain",
		mixin = "net.diebuddies.mixins.weather.MixinLevelRenderer"
	)
	@WrapOperation(method = "@MixinSquared:Handler", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/ParticleProvider;createParticle(Lnet/minecraft/core/particles/ParticleOptions;Lnet/minecraft/client/multiplayer/ClientLevel;DDDDDD)Lnet/minecraft/client/particle/Particle;"))
	private <T extends ParticleOptions> Particle onAddAlwaysVisibleParticle(ParticleProvider<?> instance,
																			T t,
																			ClientLevel level,
																			double x,
																			double y,
																			double z,
																			double vx,
																			double vy,
																			double vz,
																			Operation<Particle> original,
																			@Share("shouldAdd") LocalBooleanRef shouldAdd) {
		if (!VSClientUtils.isUnderShipHeightMap(level, x, y, z)) {
			shouldAdd.set(true);
			return original.call(instance, t, level, x, y, z, vx, vy, vz);
		} else {
			shouldAdd.set(false);
			return null;
		}
	}

	@TargetHandler(
		name = "tickRain",
		mixin = "net.diebuddies.mixins.weather.MixinLevelRenderer"
	)
	@WrapWithCondition(method = "@MixinSquared:Handler", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/ParticleEngine;add(Lnet/minecraft/client/particle/Particle;)V"))
	private <T extends ParticleOptions> boolean onAddAlwaysVisibleParticle(ParticleEngine instance,
																		   Particle optional,
																		   @Share("shouldAdd") LocalBooleanRef shouldAdd) {
		return shouldAdd.get();
	}
}
