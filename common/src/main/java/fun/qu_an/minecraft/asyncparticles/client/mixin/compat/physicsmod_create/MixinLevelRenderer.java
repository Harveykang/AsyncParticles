package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.physicsmod_create;

import com.bawnorton.mixinsquared.TargetHandler;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import fun.qu_an.minecraft.asyncparticles.client.compat.create.CreateCompat;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.particles.ParticleOptions;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = LevelRenderer.class, priority = 1100)
public class MixinLevelRenderer {
	// Fabric
	@Dynamic
	@TargetHandler(
		name = "tickRain",
		mixin = "net.diebuddies.mixins.weather.MixinLevelRenderer"
	)
	@Group(name = "asyncparticles:physicsmod_create$shouldTickRain", min = 3, max = 6)
	@WrapWithCondition(method = "@MixinSquared:Handler", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;addAlwaysVisibleParticle(Lnet/minecraft/core/particles/ParticleOptions;ZDDDDDD)V"))
	private boolean onAddAlwaysVisibleParticle(ClientLevel instance, ParticleOptions particleData, boolean ignoreRange, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
		return CreateCompat.canSpawnWeatherParticle(instance, x, y, z);
	}

	// Forge
	@Dynamic
	@TargetHandler(
		name = "tickRain",
		mixin = "net.diebuddies.mixins.weather.MixinLevelRenderer"
	)
	@Group(name = "asyncparticles:physicsmod_create$shouldTickRain", min = 3, max = 6)
	@Redirect(method = "@MixinSquared:Handler", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/ParticleProvider;createParticle(Lnet/minecraft/core/particles/ParticleOptions;Lnet/minecraft/client/multiplayer/ClientLevel;DDDDDD)Lnet/minecraft/client/particle/Particle;"))
	private <T extends ParticleOptions> Particle onAddAlwaysVisibleParticle(ParticleProvider<T> instance,
																			T t,
																			ClientLevel level,
																			double x,
																			double y,
																			double z,
																			double vx,
																			double vy,
																			double vz) {
		if (CreateCompat.canSpawnWeatherParticle(level, x, y, z)) {
			return instance.createParticle(t, level, x, y, z, vx, vy, vz);
		} else {
			return null;
		}
	}

	// Forge
	@Dynamic
	@TargetHandler(
		name = "tickRain",
		mixin = "net.diebuddies.mixins.weather.MixinLevelRenderer"
	)
	@Group(name = "asyncparticles:physicsmod_create$shouldTickRain", min = 3, max = 6)
	@WrapWithCondition(method = "@MixinSquared:Handler", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/ParticleEngine;add(Lnet/minecraft/client/particle/Particle;)V"))
	private boolean onAddAlwaysVisibleParticle(ParticleEngine instance,
											   Particle particle) {
		return particle != null;
	}
}
