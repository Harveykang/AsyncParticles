package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.forge.prettyrain;

import com.leclowndu93150.particlerain.particle.SnowParticle;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SnowParticle.class)
public abstract class MixinSnowParticle extends MixinWeatherParticle {
	protected MixinSnowParticle(ClientLevel clientLevel, double d, double e, double f) {
		super(clientLevel, d, e, f);
	}

	@Redirect(method = "tick", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/core/BlockPos$MutableBlockPos;below()Lnet/minecraft/core/BlockPos;"))
	private BlockPos onTick(BlockPos.MutableBlockPos instance) {
		return instance;
	}

	@Inject(method = "<init>", at = @At("RETURN"))
	private void onInit(CallbackInfo ci) {
		setSize(3.8F, 3.8F);
	}
}
