package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.vs2_create;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import fun.qu_an.minecraft.asyncparticles.client.compat.vs2.VSCompat;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 *  before {@link fun.qu_an.minecraft.asyncparticles.client.mixin.compat.create.MixinLevelRenderer_Rain}
 */
@Mixin(value = LevelRenderer.class, priority = 490)
public class MixinLevelRenderer_Rain {
	@ModifyExpressionValue(method = "tickRain", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/LevelReader;getHeightmapPos(Lnet/minecraft/world/level/levelgen/Heightmap$Types;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/core/BlockPos;"))
	private static BlockPos injectOnGetHeightmapPos(BlockPos original,
	                                                @Share(value = "weatherSurfaceLookupPos", namespace = "org.valkyrienskies.mod.mixin.feature.world_weather.MixinLevelRenderer")
													LocalRef<BlockPos> weatherSurfaceLookupPos) {
		if (weatherSurfaceLookupPos.get() != null
			&& original.getY() > VSCompat.shipSurfaceY.get()) {
			weatherSurfaceLookupPos.set(null);
		}
		// compare this with the finally calculated rain height by other mods
		return original;
	}
}
