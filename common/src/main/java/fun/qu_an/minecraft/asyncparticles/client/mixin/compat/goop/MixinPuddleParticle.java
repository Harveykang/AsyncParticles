package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.goop;

import absolutelyaya.goop.client.particle.PuddleParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Mixin(PuddleParticle.class)
public class MixinPuddleParticle {
	@Shadow(remap = false)
	static Queue<PuddleParticle> GOOP_QUEUE;

	@Inject(method = "<clinit>", at = @At("RETURN"))
	private static void onInit(CallbackInfo ci) {
		GOOP_QUEUE = new ConcurrentLinkedQueue<>(GOOP_QUEUE);
	}
}
