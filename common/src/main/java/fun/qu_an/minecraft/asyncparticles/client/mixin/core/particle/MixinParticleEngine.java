package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle;

import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleEngineAddon;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.ParticleHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.tick.TrackedParticleCountsMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.ParticleLimit;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.Queue;

@Mixin(ParticleEngine.class)
public class MixinParticleEngine implements ParticleEngineAddon {
	@Mutable
	@Shadow
	@Final
	private Object2IntOpenHashMap<ParticleLimit> trackedParticleCounts;
	@Mutable
	@Final
	@Shadow
	private Queue<Particle> particlesToAdd;
	@Mutable
	@Final
	@Shadow
	private Queue<TrackingEmitter> trackingEmitters;
	@Shadow
	@Final
	public Map<ParticleRenderType, ParticleGroup<?>> particles;

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

	@Override
	public void asyncparticles$discardParticlesToAdd() {
		if (!asyncparticles$tickQueue.isEmpty()) {
			asyncparticles$tickQueue.forEach(ParticleHelper::onEvict);
			asyncparticles$tickQueue.clear();
		}
		if (!asyncparticles$deferredQueue.isEmpty()) {
			asyncparticles$deferredQueue.forEach(ParticleHelper::onEvict);
			asyncparticles$deferredQueue.clear();
		}
	}

	@Inject(order = 1500, method = "<init>", at = @At(value = "RETURN"))
	public void initTail(CallbackInfo ci) {
		trackedParticleCounts = new TrackedParticleCountsMap();
		particlesToAdd = ParticleHelper.newBusyWaitParticleQueue(1024);
		asyncparticles$tickQueue = particlesToAdd;
		asyncparticles$deferredQueue = ParticleHelper.newBusyWaitParticleQueue(1024);
		trackingEmitters = ParticleHelper.newBusyWaitParticleQueue(256);
	}

	@Inject(method = "clearParticles", at = @At("HEAD"))
	public void clearParticles(CallbackInfo ci) {
		ParticleHelper.onClearParticles();
		particlesToAdd = asyncparticles$tickQueue;
		asyncparticles$deferredQueue.clear();
	}
}
