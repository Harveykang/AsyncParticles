package fun.qu_an.minecraft.asyncparticles.client.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.systems.RenderSystem;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LightLayer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ClientChunkCache.class)
public abstract class MixinClientChunkCache {
	@WrapMethod(method = "onLightUpdate")
	public void onLightUpdateWrap(LightLayer layer, SectionPos pos, Operation<Void> original) {
		if (RenderSystem.isOnRenderThread()) {
			original.call(layer, pos);
		} else {
			ThreadUtil.submitClientTask(() -> original.call(layer, pos));
		}
	}
}
