package fun.qu_an.minecraft.asyncparticles.client.mixin.legacy.flywheel;

import com.jozufozu.flywheel.light.LightListener;
import com.jozufozu.flywheel.light.LightUpdater;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.systems.RenderSystem;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import net.minecraft.world.level.LightLayer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LightUpdater.class)
public class MixinLightUpdater {
	@WrapMethod(method = {"addListener", "removeListener"})
	public void addListener(LightListener listener, Operation<Void> original) {
		if (RenderSystem.isOnRenderThread()) {
			original.call(listener);
		} else {
			ThreadUtil.submitClientTask(() -> original.call(listener));
		}
	}

	@WrapMethod(method = "onLightUpdate")
	public void onLightUpdate(LightLayer type, long sectionPos, Operation<Void> original) {
		if (RenderSystem.isOnRenderThread()) {
			original.call(type, sectionPos);
		} else {
			ThreadUtil.submitClientTask(() -> original.call(type, sectionPos));
		}
	}

	@WrapMethod(method = "onLightPacket")
	public void onLightPacket(int chunkX, int chunkZ, Operation<Void> original) {
		if (RenderSystem.isOnRenderThread()) {
			original.call(chunkX, chunkZ);
		} else {
			ThreadUtil.submitClientTask(() -> original.call(chunkX, chunkZ));
		}
	}
}
