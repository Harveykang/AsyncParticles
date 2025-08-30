package fun.qu_an.minecraft.asyncparticles.client.mixin.core.off_thread_access;

import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import net.minecraft.util.profiling.ActiveProfiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ActiveProfiler.class)
public class MixinActiveProfiler {
	@Inject(method = {
		"push(Ljava/lang/String;)V",
		"pop()V",
		"incrementCounter*",
		"markForCharting"
	}, at = @At("HEAD"), cancellable = true)
	private void onThreadAccess(CallbackInfo ci) {
		if (ThreadUtil.isOnParticleThread()) {
			ci.cancel();
		}
	}
}
