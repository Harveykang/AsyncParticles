package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.sable;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = LevelPlot.class, remap = false)
public abstract class MixinLevelPlot {
	@ModifyReturnValue(method = "getBoundingBox", at = @At("RETURN"))
	private BoundingBox3ic fallbackIfUnloaded(BoundingBox3ic bounds) {
		// Unloading can clear localBounds between the getter's null check and its second read.
		return bounds != null ? bounds : BoundingBox3i.EMPTY;
	}
}
