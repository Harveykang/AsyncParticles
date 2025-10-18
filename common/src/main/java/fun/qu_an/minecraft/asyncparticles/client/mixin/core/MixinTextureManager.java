package fun.qu_an.minecraft.asyncparticles.client.mixin.core;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import net.minecraft.client.renderer.texture.TextureManager;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(TextureManager.class)
public class MixinTextureManager {
	@WrapMethod(method = "tick")
	public void wrapTick(Operation<Void> original) {
		if (ConfigHelper.isDeferredTextureTick()) {
			ThreadUtil.enqueueClientTask(original::call);
		} else {
			original.call();
		}
	}
}
