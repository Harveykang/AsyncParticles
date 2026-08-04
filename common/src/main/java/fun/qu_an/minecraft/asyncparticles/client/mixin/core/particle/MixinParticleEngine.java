package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle;

import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleEngineAddon;
import fun.qu_an.minecraft.asyncparticles.client.config.AsyncParticlesConfig;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.tick.AsyncTickBehavior;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.GpuParticleBehavior;
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

	@Inject(method = "<init>", order = 9000, at = @At(value = "RETURN"))
	public void initTail(CallbackInfo ci) {
		trackedParticleCounts = new TrackedParticleCountsMap();
		particlesToAdd = BusyWaitEvictingQueue.newInstance(AsyncParticlesConfig.MIN_PARTICLE_LIMIT, ConfigHelper.getParticleLimit(), ParticleHelper::onEvict);
		trackingEmitters = BusyWaitEvictingQueue.newInstance(AsyncParticlesConfig.MIN_PARTICLE_LIMIT / 4, ConfigHelper.getParticleLimit(), ParticleHelper::onEvict);
	}

	@Inject(method = "add", at = @At(value = "INVOKE", target = "Ljava/util/Queue;add(Ljava/lang/Object;)Z"))
	public void onAdd(Particle particle, CallbackInfo ci) {
		ParticleHelper.doFirstRefresh(particle);
	}

	@Inject(method = "clearParticles", at = @At("HEAD"))
	public void onClearParticles(CallbackInfo ci) {
		particlesToAdd.forEach(ParticleHelper::onClearParticle);
		particlesToAdd = BusyWaitEvictingQueue.newInstance(AsyncParticlesConfig.MIN_PARTICLE_LIMIT, ConfigHelper.getParticleLimit(), ParticleHelper::onEvict);
		trackingEmitters.forEach(ParticleHelper::onClearParticle);
		trackingEmitters = BusyWaitEvictingQueue.newInstance(AsyncParticlesConfig.MIN_PARTICLE_LIMIT / 4, ConfigHelper.getParticleLimit(), ParticleHelper::onEvict);
		particles.values().forEach(queue -> queue.forEach(ParticleHelper::onClearParticle));
		ParticleHelper.onClearParticles();
	}
}
