package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.create;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import fun.qu_an.minecraft.asyncparticles.client.AsyncParticlesClient;
import fun.qu_an.minecraft.asyncparticles.client.compat.create.ContraptionRainBlocking;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LevelRenderer.class, priority = 490)
public class MixinLevelRenderer_Rain_Early {
	@Shadow
	private @Nullable ClientLevel level;

	@Inject(method = "tickRain", order = 100, at = @At("HEAD"))
	private void onTickRain(Camera camera,
	                        CallbackInfo ci) {
		BlockPos blockPos = camera.getBlockPosition();
		ContraptionRainBlocking.tickRainBlocking(
			level,
			blockPos.getX(),
			blockPos.getZ(),
			ConfigHelper.getTickRainBlockingRange());
	}

	@ModifyExpressionValue(method = "tickRain", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/level/LevelReader;getHeightmapPos(Lnet/minecraft/world/level/levelgen/Heightmap$Types;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/core/BlockPos;"))
	private BlockPos wrapHeightmapPos(BlockPos original,
	                                  @Share(value = "hasNoContraption", namespace = AsyncParticlesClient.MOD_ID) LocalBooleanRef hasNoContraption,
	                                  @Share(value = "contraptionSurfaceY", namespace = AsyncParticlesClient.MOD_ID) LocalIntRef contraptionSurfaceY) {
		boolean b = original.getY() > contraptionSurfaceY.get();
		hasNoContraption.set(b); // must set after all other mods' injections.
		return original;
	}
}
