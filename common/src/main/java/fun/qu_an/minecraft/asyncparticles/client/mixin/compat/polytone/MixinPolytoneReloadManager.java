package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.polytone;

import fun.qu_an.minecraft.asyncparticles.client.compat.polytone.PolytoneCompat;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.tick.AsyncTickBehavior;
import net.mehvahdjukaar.polytone.common.reloader.PolytoneReloadManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PolytoneReloadManager.class, remap = false)
public abstract class MixinPolytoneReloadManager {
	@Inject(method = "resetWithLevel", at = @At("HEAD"))
	private void joinBeforeReset(boolean isLogOff, CallbackInfo ci) {
		AsyncTickBehavior.getInstance().reset();
		PolytoneCompat.onParticleEngineClear();
	}
}
