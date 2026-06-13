package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.cosycritters;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pigcart.cosycritters.particle.BirdParticle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;

@Mixin(value = BirdParticle.class, remap = false)
public class MixinBirdParticle {
	@Mutable
	@Shadow
	@Final
	public static Collection<BirdParticle> birds;

	@WrapOperation(method = "<clinit>", at = @At(value = "NEW", target = "()Ljava/util/ArrayList;"))
	private static ArrayList<?> onInit(Operation<ArrayList<?>> original) {
		return null;
	}

	@Inject(method = "<clinit>", at = @At("RETURN"))
	private static void onInit(CallbackInfo ci) {
		birds = new CopyOnWriteArrayList<>();
	}
}
