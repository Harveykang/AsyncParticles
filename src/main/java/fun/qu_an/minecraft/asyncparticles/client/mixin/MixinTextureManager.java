package fun.qu_an.minecraft.asyncparticles.client.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fun.qu_an.minecraft.asyncparticles.client.AsyncTicker;
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
			AsyncTicker.endTickOperations.add(original::call);
		}
	}

	@Inject(method = "tick", at = @At(value = "INVOKE", target = "Ljava/util/Iterator;next()Ljava/lang/Object;"), cancellable = true)
	public void onTickIteratorNext(CallbackInfo ci) {
		if (AsyncTicker.isCancelled() && !AsyncTicker.forceDoneTextureTick()) {
			ci.cancel();
		}
	}
}
