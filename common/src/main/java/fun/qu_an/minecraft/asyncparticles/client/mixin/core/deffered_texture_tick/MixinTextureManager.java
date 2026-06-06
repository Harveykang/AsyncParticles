package fun.qu_an.minecraft.asyncparticles.client.mixin.core.deffered_texture_tick;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.tick.AsyncTickBehavior;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import net.minecraft.client.renderer.texture.TextureManager;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(TextureManager.class)
public class MixinTextureManager {
	@WrapMethod(method = "tick")
	public void wrapTick(Operation<Void> original) {
		if (ConfigHelper.isDeferredTextureTick() &&
			AsyncTickBehavior.getInstance().isTailTick()) {
			// execute at the first frame after tick
			ThreadUtil.enqueueClientTask(original::call);
		} else {
			original.call();
		}
	}
}
