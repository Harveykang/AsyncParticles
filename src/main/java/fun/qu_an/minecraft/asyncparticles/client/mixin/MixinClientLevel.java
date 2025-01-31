package fun.qu_an.minecraft.asyncparticles.client.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fun.qu_an.minecraft.asyncparticles.client.Caches;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientLevel.class, priority = 1001)
public class MixinClientLevel {
	@WrapMethod(method = "animateTick")
	public void animateTick(int i, int j, int k, Operation<Void> original) {
		if (Caches.shouldTickParticles) {
			Caches.animateOperation = () -> original.call(i, j, k);
		}
	}

	@Inject(method = "animateTick", require = 0, at = @At(value = "INVOKE", target = "Ljava/util/Iterator;next()Ljava/lang/Object;"), cancellable = true)
	public void onAnimateTickStart(int i, int j, int k, CallbackInfo ci) {
		if (Caches.cancelled) {
			ci.cancel();
		}
	}

	@Inject(method = "animateTick", at = @At(value = "CONSTANT", args = "intValue=16"), cancellable = true)
	public void onAnimateTick(int i, int j, int k, CallbackInfo ci) {
		if (Caches.cancelled) {
			ci.cancel();
		}
	}
}
