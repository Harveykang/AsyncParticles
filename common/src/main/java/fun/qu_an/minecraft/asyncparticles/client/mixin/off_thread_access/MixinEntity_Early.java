package fun.qu_an.minecraft.asyncparticles.client.mixin.off_thread_access;

import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Entity.class, priority = 500)
public abstract class MixinEntity_Early {
	@Shadow
	public abstract void setRemoved(Entity.RemovalReason arg);

	@Shadow
	public abstract void remove(Entity.RemovalReason arg);

	@Inject(method = "remove", at = @At("HEAD"), cancellable = true)
	private void wrapRemove(Entity.RemovalReason removalReason, CallbackInfo ci) {
		if (ThreadUtil.isOnParticleThread()) {
			ThreadUtil.enqueueClientTask(() -> remove(removalReason));
			ci.cancel();
		}
	}

	@Inject(method = "setRemoved", at = @At("HEAD"), cancellable = true)
	private void wrapRemove2(Entity.RemovalReason removalReason, CallbackInfo ci) {
		if (ThreadUtil.isOnParticleThread()) {
			ThreadUtil.enqueueClientTask(() -> setRemoved(removalReason));
			ci.cancel();
		}
	}
}
