package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle;

import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleEngineAddon;
import fun.qu_an.minecraft.asyncparticles.client.config.AsyncParticlesConfig;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.tick.AsyncTickBehavior;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.ParticleHelper;
import fun.qu_an.minecraft.asyncparticles.client.util.*;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.ParticleGroup;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.Queue;

@Mixin(value = ParticleEngine.class, priority = 500)
public abstract class MixinParticleEngine implements ParticleEngineAddon {
	@Shadow
	public Queue<Particle> particlesToAdd;
	@Shadow
	public Map<ParticleRenderType, Queue<Particle>> particles;
	@Shadow
	public Queue<TrackingEmitter> trackingEmitters;
	@Mutable
	@Shadow
	@Final
	private Object2IntOpenHashMap<ParticleGroup> trackedParticleCounts;
	@Unique
	private Queue<Particle> asyncparticles$tickQueue;
	@Unique
	private Queue<Particle> asyncparticles$deferredQueue;

	@Override
	public void asyncparticles$switchToDeferredQueue() {
		particlesToAdd = asyncparticles$deferredQueue;
	}

	@Override
	public void asyncparticles$switchToTickQueue() {
		particlesToAdd = asyncparticles$tickQueue;
	}

	@Override
	public void asyncparticles$addDefers() {
		if (!asyncparticles$deferredQueue.isEmpty()) {
			asyncparticles$tickQueue.addAll(asyncparticles$deferredQueue);
			asyncparticles$deferredQueue.clear();
		}
	}

	public void asyncparticles$discardParticlesToAdd() {
		if (!asyncparticles$tickQueue.isEmpty()) {
			asyncparticles$tickQueue.forEach(ParticleHelper::onEvictIgnoreExceptions);
			asyncparticles$tickQueue.clear();
		}
		if (!asyncparticles$deferredQueue.isEmpty()) {
			asyncparticles$deferredQueue.forEach(ParticleHelper::onEvictIgnoreExceptions);
			asyncparticles$deferredQueue.clear();
		}
	}

	@Inject(method = "<init>", order = 9000, at = @At(value = "RETURN"))
	public void initTail(CallbackInfo ci) {
		trackedParticleCounts = new TrackedParticleCountsMap();
		particlesToAdd = BusyWaitEvictingQueue.newInstance(AsyncParticlesConfig.MIN_PARTICLE_LIMIT, ConfigHelper.getParticleLimit(), ParticleHelper::onEvict);
		asyncparticles$tickQueue = particlesToAdd;
		asyncparticles$deferredQueue = BusyWaitEvictingQueue.newInstance(AsyncParticlesConfig.MIN_PARTICLE_LIMIT, ConfigHelper.getParticleLimit(), ParticleHelper::onEvict);
		trackingEmitters = BusyWaitEvictingQueue.newInstance(AsyncParticlesConfig.MIN_PARTICLE_LIMIT / 4, ConfigHelper.getParticleLimit(), ParticleHelper::onEvict);
	}

	@Inject(method = "add", at = @At(value = "INVOKE", target = "Ljava/util/Queue;add(Ljava/lang/Object;)Z"))
	public void onAdd(Particle particle, CallbackInfo ci) {
		ParticleHelper.doFirstRefresh(particle);
	}

	@Inject(method = "clearParticles", at = @At("HEAD"))
	public void onClearParticles(CallbackInfo ci) {
		AsyncTickBehavior.getInstance().reset();

		asyncparticles$tickQueue.forEach(ParticleHelper::onClearParticle);
		asyncparticles$deferredQueue.forEach(ParticleHelper::onClearParticle);
		particlesToAdd = BusyWaitEvictingQueue.newInstance(AsyncParticlesConfig.MIN_PARTICLE_LIMIT, ConfigHelper.getParticleLimit(), ParticleHelper::onEvict);
		asyncparticles$tickQueue = particlesToAdd;
		asyncparticles$deferredQueue = BusyWaitEvictingQueue.newInstance(AsyncParticlesConfig.MIN_PARTICLE_LIMIT, ConfigHelper.getParticleLimit(), ParticleHelper::onEvict);

		trackingEmitters.forEach(ParticleHelper::onClearParticle);
		trackingEmitters = BusyWaitEvictingQueue.newInstance(AsyncParticlesConfig.MIN_PARTICLE_LIMIT / 4, ConfigHelper.getParticleLimit(), ParticleHelper::onEvict);
		particles.values().forEach(queue -> queue.forEach(ParticleHelper::onClearParticle));
		ParticleHelper.onClearParticles();
	}
}
