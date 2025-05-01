package fun.qu_an.minecraft.asyncparticles.client.mixin.tick;

import fun.qu_an.minecraft.asyncparticles.client.AsyncTicker;
import fun.qu_an.minecraft.asyncparticles.client.config.SimplePropertiesConfig;
import fun.qu_an.minecraft.asyncparticles.client.util.BusyWaitEvictingQueue;
import fun.qu_an.minecraft.asyncparticles.client.util.TrackedParticleCountsMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.TrackingEmitter;
import net.minecraft.core.particles.ParticleGroup;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Mixin(value = ParticleEngine.class, priority = 9000)
public abstract class MixinParticleEngine_Late {
	@Mutable
	@Shadow
	@Final
	private Object2IntOpenHashMap<ParticleGroup> trackedParticleCounts;

	@Shadow
	public Queue<Particle> particlesToAdd;

	@Shadow
	public Queue<TrackingEmitter> trackingEmitters;

	@Mutable
	@Shadow
	@Final
	private RandomSource random;

	@Inject(method = "<init>", at = @At(value = "RETURN"))
	public void initTail(CallbackInfo ci) {
		trackedParticleCounts = new TrackedParticleCountsMap();
		particlesToAdd = BusyWaitEvictingQueue.newInstance(1024, SimplePropertiesConfig.getLimit(), AsyncTicker::onEvicted);
		trackingEmitters = BusyWaitEvictingQueue.newInstance(256, SimplePropertiesConfig.getLimit(), AsyncTicker::onEvicted);
		random = new SingleThreadedRandomSource(ThreadLocalRandom.current().nextInt());
	}
}
