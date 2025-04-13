package fun.qu_an.minecraft.asyncparticles.client.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fun.qu_an.minecraft.asyncparticles.client.AsyncTicker;
import fun.qu_an.minecraft.asyncparticles.client.config.SimplePropertiesConfig;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import net.minecraft.client.renderer.texture.TextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TextureManager.class)
public class MixinTextureManager {
	@WrapMethod(method = "tick")
	public void wrapTick(Operation<Void> original) {
		if (AsyncTicker.shouldTickParticles) {
			// execute at the first frame after tick
			ThreadUtil.submitClientTask(original::call);
		} else {
			original.call();
		}
	}
}
