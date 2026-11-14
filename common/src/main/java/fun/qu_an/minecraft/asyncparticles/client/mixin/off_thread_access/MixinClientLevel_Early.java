package fun.qu_an.minecraft.asyncparticles.client.mixin.off_thread_access;

import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientLevel.class, priority = 500)
public abstract class MixinClientLevel_Early {
	@Shadow
	private void addEntity(int id, Entity entity) {
		throw new AssertionError();
	}

	@Shadow
	public abstract void removeEntity(int id, Entity.RemovalReason removalReason);

	@Inject(method = "addEntity", at = @At("HEAD"), cancellable = true)
	private void onAddEntity(int id, Entity entity, CallbackInfo ci) {
		if (ThreadUtil.isOnParticleThread()) {
			ci.cancel();
			ThreadUtil.enqueueClientTask(() -> {
				// The particle may have been discarded before its entity is added.
				if (!entity.isRemoved()) {
					this.addEntity(id, entity);
				}
			});
		}
	}

	@Inject(method = "removeEntity", at = @At("HEAD"), cancellable = true)
	private void onRemoveEntity(int id, Entity.RemovalReason removalReason, CallbackInfo ci) {
		if (ThreadUtil.isOnParticleThread()) {
			ci.cancel();
			ThreadUtil.enqueueClientTask(() -> this.removeEntity(id, removalReason));
		}
	}
}
