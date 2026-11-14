package fun.qu_an.minecraft.asyncparticles.client.mixin.off_thread_access;

import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.TransientEntitySectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TransientEntitySectionManager.class)
public abstract class MixinTransientEntitySectionManager<T extends EntityAccess> {
	@Shadow
	public abstract void addEntity(T entity);

	@Inject(method = "addEntity", at = @At("HEAD"), cancellable = true)
	private void onAddEntity(T entity, CallbackInfo ci) {
		if (ThreadUtil.isOnParticleThread()) {
			ci.cancel();
			ThreadUtil.enqueueClientTask(() -> {
				if (!(entity instanceof Entity e) || !e.isRemoved()) {
					this.addEntity(entity);
				}
			});
		}
	}
}
