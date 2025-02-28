package fun.qu_an.minecraft.asyncparticles.client.mixin.forge.particlerain;

import com.leclowndu93150.particlerain.particle.SnowParticle;
import com.leclowndu93150.particlerain.particle.WeatherParticle;
import fun.qu_an.minecraft.asyncparticles.client.ModListHelper;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SnowParticle.class)
public abstract class MixinSnowParticle extends WeatherParticle {
	protected MixinSnowParticle(ClientLevel level, double x, double y, double z) {
		super(level, x, y, z);
	}

	@Redirect(method = "tick", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/core/BlockPos$MutableBlockPos;below()Lnet/minecraft/core/BlockPos;"))
	private BlockPos onTick(BlockPos.MutableBlockPos instance) {
		return ModListHelper.VS_LOADED ? instance : instance.below();
	}
}
