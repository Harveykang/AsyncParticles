package fun.qu_an.minecraft.asyncparticles.client.mixin.render;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = LevelRenderer.class, priority = 500)
public abstract class MixinLevelRenderer_WeatherCulling_Early {
	// It's ok if this redirect is not applied, just reduce a little of performance.
	@Redirect(method = "renderSnowAndRain", require = 0, at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/level/Level;getHeight(Lnet/minecraft/world/level/levelgen/Heightmap$Types;II)I"))
	public int onGetHeight(Level instance,
						   Heightmap.Types types,
						   int i,
						   int j,
						   @Share(value = "enableCull") LocalBooleanRef enableCull,
						   @Share(value = "height_map") LocalIntRef qRef) {
		return enableCull.get() ? qRef.get() : instance.getHeight(types, i, j);
	}
}
