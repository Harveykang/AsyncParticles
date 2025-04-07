package fun.qu_an.minecraft.asyncparticles.client.mixin.off_thread_access;

import net.minecraft.util.profiling.ActiveProfiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ActiveProfiler.class, priority = 499)
public class MixinActiveProfiler {
	@Unique
	private Thread asyncparticles$thread;

	@Inject(method = "<init>", at = @At("RETURN"))
	private void init(CallbackInfo ci) {
		asyncparticles$thread = Thread.currentThread();
	}

	// TODO: 是否会破坏一些性能监测 mod？
	@Inject(method = "push(Ljava/lang/String;)V", at = @At("HEAD"), cancellable = true)
	private void push(String string, CallbackInfo ci) {
		if (asyncparticles$thread != Thread.currentThread()) {
			ci.cancel();
		}
	}

	@Inject(method = "pop()V", at = @At("HEAD"), cancellable = true)
	private void pop(CallbackInfo ci) {
		if (asyncparticles$thread != Thread.currentThread()) {
			ci.cancel();
		}
	}
}
