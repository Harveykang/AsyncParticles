package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.sable;

import com.bawnorton.mixinsquared.TargetHandler;
import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Particle.class, priority = 1500)
public abstract class MixinMixinParticle_LightScan {
	@Shadow
	@Final
	public ClientLevel level;

	@TargetHandler(
		mixin = "dev.ryanhcode.sable.mixin.particle.ParticleMixin",
		name = "sable$checkSubLevelLightColor"
	)
	@Inject(method = "@MixinSquared:Handler", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/core/BlockPos$MutableBlockPos;setWithOffset(Lnet/minecraft/core/Vec3i;III)Lnet/minecraft/core/BlockPos$MutableBlockPos;"),
		require = 2, allow = 2)
	private void resetScanBudget(CallbackInfo ci, @Share("scanStepsRemaining") LocalIntRef stepsRemaining) {
		stepsRemaining.set(level.getHeight());
	}

	@TargetHandler(
		mixin = "dev.ryanhcode.sable.mixin.particle.ParticleMixin",
		name = "sable$checkSubLevelLightColor"
	)
	@Definition(id = "getY", method = "Lnet/minecraft/core/BlockPos$MutableBlockPos;getY()I")
	@Expression("?.getY() >= ?")
	@ModifyExpressionValue(method = "@MixinSquared:Handler", at = @At("MIXINEXTRAS:EXPRESSION"),
		require = 2, allow = 2)
	private boolean limitHeightScan(boolean original,
	                                @Share("scanStepsRemaining") LocalIntRef stepsRemaining) {
		int remaining = stepsRemaining.get();
		if (!original || remaining <= 0) {
			return false;
		}
		stepsRemaining.set(remaining - 1);
		return true;
	}
}
