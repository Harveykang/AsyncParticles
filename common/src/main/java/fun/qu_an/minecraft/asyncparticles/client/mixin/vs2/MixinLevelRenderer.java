package fun.qu_an.minecraft.asyncparticles.client.mixin.vs2;

import com.bawnorton.mixinsquared.TargetHandler;
import com.llamalad7.mixinextras.sugar.Local;
import fun.qu_an.minecraft.asyncparticles.client.compat.vs2.VSParticleAddon;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.valkyrienskies.core.api.ships.ClientShip;

@Mixin(value = LevelRenderer.class, priority = 1500)
public class MixinLevelRenderer {
	// VS2
	@TargetHandler(
		name = "spawnParticleInWorld",
		mixin = "org.valkyrienskies.mod.mixin.feature.transform_particles.MixinLevelRenderer"
	)
	@Redirect(method = "@MixinSquared:Handler", require = 0, at = @At(value = "INVOKE", remap = false,
		target = "Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable;setReturnValue(Ljava/lang/Object;)V"))
	private <T> void onSpawnParticleInWorld(CallbackInfoReturnable<T> instance,
											T particle,
											@SuppressWarnings("LocalMayBeArgsOnly")
											@Local(ordinal = 0) ClientShip ship) {
		instance.setReturnValue(particle);
		if (particle != null) {
			((VSParticleAddon) particle).asyncParticles$setShip(ship);
		}
	}

	// VS Addition
	@Dynamic
	@TargetHandler(
		name = "spawnParticleInWorld",
		mixin = "io.github.xiewuzhiying.vs_addition.mixin.valkyrienskies.client.MixinMixinLevelRenderer"
	)
	@Inject(method = "@MixinSquared:Handler", require = 0, at = @At(value = "RETURN", ordinal = 2))
	private <T> void onSpawnParticleInWorld(CallbackInfoReturnable<T> cir,
											@SuppressWarnings("LocalMayBeArgsOnly")
											@Local(ordinal = 0) ClientShip ship) {
		T particle = cir.getReturnValue();
		if (particle != null) {
			((VSParticleAddon) particle).asyncParticles$setShip(ship);
		}
	}
}
