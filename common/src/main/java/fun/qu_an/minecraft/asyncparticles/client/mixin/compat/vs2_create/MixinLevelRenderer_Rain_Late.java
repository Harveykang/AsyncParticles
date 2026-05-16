package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.vs2_create;

import com.bawnorton.mixinsquared.TargetHandler;
import fun.qu_an.minecraft.asyncparticles.client.compat.vs2.VSCompat;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LevelRenderer.class, priority = 1500)
public class MixinLevelRenderer_Rain_Late {
	@TargetHandler(
		mixin = "org.valkyrienskies.mod.mixin.feature.world_weather.MixinLevelRenderer",
		name = "includeShipsInWeatherTickOcclusion"
	)
	@ModifyArg(method = "@MixinSquared:Handler", index = 1,
		at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;<init>(III)V"))
	private int modifyY(int y) {
		VSCompat.shipSurfaceY.set(y);
		return y;
	}

	@TargetHandler(
		mixin = "org.valkyrienskies.mod.mixin.feature.world_weather.MixinLevelRenderer",
		name = "includeShipsInWeatherTickOcclusion"
	)
	@Inject(method = "@MixinSquared:Handler",
		at = @At(value = "HEAD"))
	private void inject(CallbackInfoReturnable<BlockPos> cir) {
		VSCompat.shipSurfaceY.set(Integer.MIN_VALUE);
	}
}
