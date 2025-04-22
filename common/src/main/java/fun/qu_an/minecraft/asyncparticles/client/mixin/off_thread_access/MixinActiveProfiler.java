package fun.qu_an.minecraft.asyncparticles.client.mixin.off_thread_access;

import net.minecraft.util.profiling.ActiveProfiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ActiveProfiler.class)
public class MixinActiveProfiler {
	@Unique
	private Thread asyncparticles$thread;

	@Inject(method = "startTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiling/ActiveProfiler;push(Ljava/lang/String;)V"))
	private void startTick(CallbackInfo ci) {
		asyncparticles$thread = Thread.currentThread();
	}

	@Inject(method = "endTick", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/util/profiling/ActiveProfiler;pop()V"))
	private void endTick(CallbackInfo ci) {
		asyncparticles$thread = null;
	}

	@Inject(method = {
		"push(Ljava/lang/String;)V",
		"pop()V",
		"incrementCounter*",
		"markForCharting"
	}, at = @At("HEAD"), cancellable = true)
	private void push(CallbackInfo ci) {
		if (asyncparticles$thread != Thread.currentThread()) {
			ci.cancel();
		}
	}
}
