package fun.qu_an.minecraft.asyncparticles.client.mixin.off_thread_access;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fun.qu_an.minecraft.asyncparticles.client.core.Diagnostic;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Entity.class, priority = 500)
public abstract class MixinEntity {
	@Inject(method = "<init>", at = @At("HEAD"))
	private static void injectInit(CallbackInfo ci) {
		if (ThreadUtil.isOnParticleThread()) {
			Diagnostic.wtfEntityLifecycle();
		}
	}

	@Shadow
	public abstract void setRemoved(Entity.RemovalReason arg);

	@WrapMethod(method = "remove")
	private void wrapRemove(Entity.RemovalReason removalReason, Operation<Void> original) {
		if (ThreadUtil.isOnParticleThread()) {
			ThreadUtil.enqueueClientTask(() -> original.call(removalReason));
			Diagnostic.wtfEntityLifecycle();
		} else {
			original.call(removalReason);
		}
	}

	@Inject(method = "setRemoved", at = @At("HEAD"), cancellable = true)
	private void onSetRemoved(Entity.RemovalReason removalReason, CallbackInfo ci) {
		if (ThreadUtil.isOnParticleThread()) {
			ThreadUtil.enqueueClientTask(() -> setRemoved(removalReason));
			Diagnostic.wtfEntityLifecycle();
			ci.cancel();
		}
	}
}
