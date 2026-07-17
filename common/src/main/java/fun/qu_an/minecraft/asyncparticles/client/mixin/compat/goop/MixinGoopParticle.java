package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.goop;

import absolutelyaya.goop.particles.GoopParticle;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Mixin(GoopParticle.class)
public class MixinGoopParticle {
	@Mutable
	@Final
	@Shadow(remap = false)
	static Queue<GoopParticle> GOOP_QUEUE;

	@Inject(method = "<clinit>", at = @At("RETURN"))
	private static void onInit(CallbackInfo ci) {
		GOOP_QUEUE = new ConcurrentLinkedQueue<>(GOOP_QUEUE);
	}
}
