package fun.qu_an.minecraft.asyncparticles.client.mixin.off_thread_access;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LightLayer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ClientChunkCache.class)
public abstract class MixinClientChunkCache {
	@WrapMethod(method = "onLightUpdate")
	public void onLightUpdateWrap(LightLayer layer, SectionPos pos, Operation<Void> original) {
		if (ThreadUtil.isOnMainThread()) {
			original.call(layer, pos);
		} else {
			ThreadUtil.enqueueClientTask(() -> original.call(layer, pos));
		}
	}
}
