package fun.qu_an.minecraft.asyncparticles.client.mixin.off_thread_access;

import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LightLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientChunkCache.class, priority = 500)
public abstract class MixinClientChunkCache {
	@Shadow
	public abstract void onLightUpdate(LightLayer lightLayer, SectionPos sectionPos);

	@Inject(method = "onLightUpdate", at = @At("HEAD"), cancellable = true)
	public void onLightUpdateWrap(LightLayer lightLayer, SectionPos sectionPos, CallbackInfo ci) {
		if (ThreadUtil.isOnParticleThread()) {
			ci.cancel();
			ThreadUtil.enqueueClientTask(() -> this.onLightUpdate(lightLayer, sectionPos));
		}
	}
}
