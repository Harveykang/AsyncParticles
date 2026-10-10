package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.polytone;

import fun.qu_an.minecraft.asyncparticles.client.compat.polytone.PolytoneCompat;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import net.mehvahdjukaar.polytone.content.particle.custom.CustomParticleInstance;
import net.mehvahdjukaar.polytone.content.particle.custom.PolytoneAsyncParticleHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PolytoneAsyncParticleHandler.class, remap = false)
public abstract class MixinPolytoneAsyncParticleHandler {
	@Inject(method = "enqueue", at = @At("HEAD"), cancellable = true)
	private static void tickInCurrentBatch(CustomParticleInstance particle, CallbackInfo ci) {
		if (ConfigHelper.isAsyncParticleTick() && ConfigHelper.isPolytoneAsyncTick()) {
			particle.tickSync();
			ci.cancel();
		}
	}

	@Inject(method = "deferToMain", at = @At(value = "INVOKE", target = "Ljava/util/Queue;add(Ljava/lang/Object;)Z"), cancellable = true)
	private static void deferToClient(Runnable action, CallbackInfo ci) {
		if (ConfigHelper.isAsyncParticleTick() && ConfigHelper.isPolytoneAsyncTick()) {
			PolytoneCompat.deferToMain(action);
			ci.cancel();
		}
	}
}
