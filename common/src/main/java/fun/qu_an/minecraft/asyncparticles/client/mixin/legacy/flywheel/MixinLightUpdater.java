package fun.qu_an.minecraft.asyncparticles.client.mixin.legacy.flywheel;

import com.jozufozu.flywheel.light.LightListener;
import com.jozufozu.flywheel.light.LightUpdater;
import com.jozufozu.flywheel.util.box.ImmutableBox;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.systems.RenderSystem;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import net.minecraft.world.level.LightLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.stream.Stream;

@Mixin(value = LightUpdater.class, remap = false)
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

	@Inject(method = "getAllBoxes", at = @At("HEAD"))
	public void getAllBoxes(CallbackInfoReturnable<Stream<ImmutableBox>> cir) {
		RenderSystem.assertOnRenderThread();
	}
}
