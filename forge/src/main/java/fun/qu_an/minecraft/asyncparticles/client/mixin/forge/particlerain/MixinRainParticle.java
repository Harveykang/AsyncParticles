package fun.qu_an.minecraft.asyncparticles.client.mixin.forge.particlerain;

import com.leclowndu93150.particlerain.particle.RainParticle;
import fun.qu_an.minecraft.asyncparticles.client.ModListHelper;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RainParticle.class)
public class MixinRainParticle {
	@Redirect(method = "tick", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/core/BlockPos$MutableBlockPos;below(I)Lnet/minecraft/core/BlockPos;"))
	private BlockPos modifyMaxCount(BlockPos.MutableBlockPos instance, int i) {
		return ModListHelper.VS_LOADED ? instance : instance.below(i);
	}
}
