package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.neoforge.create;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import fun.qu_an.minecraft.asyncparticles.client.AsyncParticlesClient;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = LevelRenderer.class, priority = 490)
public class MixinLevelRenderer_Rain_Early {
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
