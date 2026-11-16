package fun.qu_an.minecraft.asyncparticles.client.mixin.off_thread_access;

import fun.qu_an.minecraft.asyncparticles.client.core.Diagnostic;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class MixinEntity {
	@Inject(method = "<init>", at = @At("RETURN"))
	private void injectInit(CallbackInfo ci) {
		if (ThreadUtil.isOnParticleThread()) {
			Diagnostic.wtfEntityLifecycle();
		}
	}
}
