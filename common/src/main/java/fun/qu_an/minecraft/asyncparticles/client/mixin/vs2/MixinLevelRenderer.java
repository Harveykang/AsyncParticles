package fun.qu_an.minecraft.asyncparticles.client.mixin.vs2;

import com.bawnorton.mixinsquared.TargetHandler;
import com.llamalad7.mixinextras.sugar.Local;
import fun.qu_an.minecraft.asyncparticles.client.addon.LightCachedParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.compat.vs2.VSParticleAddon;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.valkyrienskies.core.api.ships.ClientShip;

// FIXME: This is unstable
@Mixin(value = LevelRenderer.class, priority = 1500)
public class MixinLevelRenderer {
	// VS2
	@TargetHandler(
		name = "spawnParticleInWorld",
		// this mixin will be cancelled by vs_addition
		mixin = "org.valkyrienskies.mod.mixin.feature.transform_particles.MixinLevelRenderer"
	)
	@Group(name = "asyncparticles:vs2$spawnParticleInWorld", min = 1, max = 1)
	@Redirect(method = "@MixinSquared:Handler", at = @At(value = "INVOKE", remap = false,
		target = "Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable;setReturnValue(Ljava/lang/Object;)V"))
	private <T> void onSpawnParticleInWorld(CallbackInfoReturnable<T> instance,
											T particle,
											@SuppressWarnings("LocalMayBeArgsOnly")
											@Local(ordinal = 0) ClientShip ship) {
		instance.setReturnValue(particle);
		if (!(particle instanceof VSParticleAddon vsp) ||
			vsp.asyncparticles$isOnShip()) {
			return;
		}
		vsp.asyncparticles$setShip(ship);
		if (vsp instanceof LightCachedParticleAddon lp) {
			lp.asyncparticles$refresh();
		}
	}

	// VS Addition
	@Dynamic
	@TargetHandler(
		name = "spawnParticleInWorld",
		// forgix modifies the package name
		mixin = "fabric.io.github.xiewuzhiying.vs_addition.mixin.valkyrienskies.client.MixinMixinLevelRenderer"
	)
	@Group(name = "asyncparticles:vs2$spawnParticleInWorld", min = 1, max = 1)
	@Inject(method = "@MixinSquared:Handler", at = @At(value = "RETURN", ordinal = 2))
	private <T> void onSpawnParticleInWorld1(CallbackInfoReturnable<T> cir,
											 @SuppressWarnings("LocalMayBeArgsOnly")
											 @Local(ordinal = 0) ClientShip ship) {
		T particle = cir.getReturnValue();
		if (!(particle instanceof VSParticleAddon vsp) ||
			vsp.asyncparticles$isOnShip()) {
			return;
		}
		vsp.asyncparticles$setShip(ship);
		if (vsp instanceof LightCachedParticleAddon lp) {
			lp.asyncparticles$refresh();
		}
	}

	@Dynamic
	@TargetHandler(
		name = "spawnParticleInWorld",
		// forgix modifies the package name
		mixin = "forge.io.github.xiewuzhiying.vs_addition.mixin.valkyrienskies.client.MixinMixinLevelRenderer"
	)
	@Group(name = "asyncparticles:vs2$spawnParticleInWorld", min = 1, max = 1)
	@Inject(method = "@MixinSquared:Handler", at = @At(value = "RETURN", ordinal = 2))
	private <T> void onSpawnParticleInWorld2(CallbackInfoReturnable<T> cir,
											 @SuppressWarnings("LocalMayBeArgsOnly")
											 @Local(ordinal = 0) ClientShip ship) {
		T particle = cir.getReturnValue();
		if (!(particle instanceof VSParticleAddon vsp) ||
			vsp.asyncparticles$isOnShip()) {
			return;
		}
		vsp.asyncparticles$setShip(ship);
		if (vsp instanceof LightCachedParticleAddon lp) {
			lp.asyncparticles$refresh();
		}
	}
}
