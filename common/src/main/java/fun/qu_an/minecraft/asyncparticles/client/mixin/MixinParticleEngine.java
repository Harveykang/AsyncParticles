package fun.qu_an.minecraft.asyncparticles.client.mixin;

import com.google.common.collect.EvictingQueue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fun.qu_an.minecraft.asyncparticles.client.*;
import fun.qu_an.minecraft.asyncparticles.client.config.SimplePropertiesConfig;
import fun.qu_an.minecraft.asyncparticles.client.util.TrackedParticleCountsMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.ParticleGroup;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

// TODO: 分为两个 Mixin
@Mixin(value = ParticleEngine.class, priority = 500)
public abstract class MixinParticleEngine {
	@Mutable
	@Shadow
	@Final
	public Queue<Particle> particlesToAdd;

	@Shadow
	@Final
	public Map<ParticleRenderType, Queue<Particle>> particles;

	@Shadow
	protected ClientLevel level;

	@Mutable
	@Shadow
	@Final
	private Queue<TrackingEmitter> trackingEmitters;

	@Shadow
	protected abstract void tickParticle(Particle particle);

	@Mutable
	@Shadow
	@Final
	private Object2IntOpenHashMap<ParticleGroup> trackedParticleCounts;

	@Inject(method = "<init>", at = @At(value = "RETURN"))
	public void init(CallbackInfo ci) {
		trackedParticleCounts = new TrackedParticleCountsMap();
	}

	@Shadow
	public abstract void updateCount(ParticleGroup group, int count);

	/**
	 * @author
	 * @reason
	 */
	@Overwrite
	public void tick() {
		if (!AsyncTicker.shouldTickParticles) {
			if (!this.particlesToAdd.isEmpty()) {
				particlesToAdd.forEach(p -> {
					if (p == null) { // might be null because ArrayDeque is not thread-safe
						return;
					}
					p.remove();
					// this will fix some mod's particle count management bug
				});
				particlesToAdd.clear();
			}
			if (AsyncTicker.particleCleanup != null) {
				AsyncTicker.particleCleanup.join();
				AsyncTicker.particleCleanup = null;
			}
			return;
		}

		particles.forEach((particleRenderType, queue) -> {
			this.level.getProfiler().push(particleRenderType.toString());
			AsyncTicker.PARTICLE_OPERATIONS.add(() -> tickParticleList(queue));
			this.level.getProfiler().pop();
		});

		if (!this.trackingEmitters.isEmpty()) {
			AsyncTicker.PARTICLE_OPERATIONS.add(() -> {
				HashSet<TrackingEmitter> set = null;
				for (TrackingEmitter emitter : this.trackingEmitters) {
					if (AsyncTicker.isCancelled() && !AsyncTicker.forceDoneParticleTick()) {
						if (set != null) {
							this.trackingEmitters.removeAll(set);
						}
						return;
					}
					emitter.tick();
					if (ModListHelper.VS_LOADED) {
						if (VSClientUtils.isOutOfSight(emitter)) {
							emitter.remove();
						}
					}
					if (!emitter.isAlive()) {
						if (set == null) {
							set = new HashSet<>();
						}
						set.add(emitter);
					}
				}
				if (set != null) {
					this.trackingEmitters.removeAll(set);
				}
			});
		}

		if (AsyncTicker.particleCleanup != null) {
			AsyncTicker.particleCleanup.join();
			AsyncTicker.particleCleanup = null;
		}

		if (!this.particlesToAdd.isEmpty()) {
			particlesToAdd.forEach(p -> {
				if (p == null) { // might be null because ArrayDeque is not thread-safe
					return;
				}
				Queue<Particle> queue = this.particles.computeIfAbsent(p.getRenderType(),
					(p_107347_) -> EvictingQueue.create(SimplePropertiesConfig.limit));
				while (queue.size() >= SimplePropertiesConfig.limit) {
					Particle removed = queue.remove();
					removed.remove(); // remove if the limit exceeded
					removed.getParticleGroup().ifPresent(g -> updateCount(g, -1));
					// this will fix some mod's particle count management bug
					// TODO: 这样的话就不需要 EvictingQueue 了
				}
				p.getParticleGroup().ifPresent(g -> updateCount(g, 1));
				queue.add(p);
			});
			particlesToAdd.clear();
			// FIXME: 实现线程安全的低锁开销队列，目前会因为一些粒子在tick时添加新的粒子导致并发访问
			//  不会抛异常，因为遍历的时候不会检查为空性，无明显影响，但可能导致一些模组的粒子计数出现偏差
		}
	}

	/**
	 * @author
	 * @reason
	 */
	@Overwrite
	private void tickParticleList(Collection<Particle> collection) {
		if (collection.isEmpty()) {
			return;
		}
		Iterator<Particle> iterator = collection.iterator();
		//noinspection WhileLoopReplaceableByForEach
		while (iterator.hasNext()) {
			if (AsyncTicker.isCancelled() && !AsyncTicker.forceDoneParticleTick()) {
				return;
			}
			Particle particle = iterator.next();
			this.tickParticle(particle);
			((ParticleAddon) particle).asyncParticles$setTicked();
			if (ModListHelper.VS_LOADED) {
				if (VSClientUtils.isOutOfSight(particle)) {
					particle.remove();
				}
			}
		}
//		int size = Math.min(16384, collection.size());
//		Iterator<Particle> iterator = collection.iterator();
//		while (iterator.hasNext()) {
//			var particles = new Particle[size];
//			for (int j = 0; j < size && iterator.hasNext(); j++) {
//				particles[j] = iterator.next();
//			}
//			AsyncTicker.particleOperations.add(combineTasks(particles));
//		}
	}

//	@Unique
//	private Runnable combineTasks(Particle... task) {
//		int size = task.length;
//		return () -> {
//			//noinspection ForLoopReplaceableByForEach
//			for (int i = 0; i < size; i++) {
//				if (AsyncTicker.cancelled) {
//					return;
//				}
//				Particle particle = task[i];
//				if (particle == null) {
//					return;
//				}
//				this.tickParticle(particle);
//				((ParticleAddon) particle).asyncParticles$setTicked();
//				if (ModListHelper.VS_LOADED) {
//					if (VSClientUtils.isOutOfSight(particle)) {
//						particle.remove();
//					}
//				}
//			}
//		};
//	}

	@WrapMethod(method = "tick")
	public void wrapTick(Operation<Void> original) {
		AsyncTicker.tickParticleEngine = original;
	}

	@Inject(method = "add", at = @At(value = "HEAD"), cancellable = true)
	public void add(Particle particle, CallbackInfo ci) {
		if (!AsyncTicker.shouldTickParticles) {
			particle.remove(); // to compatible with some mods...
			ci.cancel();
		}
	}

	@Redirect(method = "add", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/ParticleEngine;updateCount(Lnet/minecraft/core/particles/ParticleGroup;I)V"))
	public void redirectUpdateCount(ParticleEngine instance, ParticleGroup group, int count) {
		// do nothing
		// we check this later in tick()
	}

//	@Inject(method = "reload", at = @At(value = "RETURN"), cancellable = true)
//	public void reload(CallbackInfoReturnable<CompletableFuture<Void>> cir) {
//		cir.setReturnValue(cir.getReturnValue().thenRun(() -> {
//			try {
//				SimplePropertiesConfig.load();
//			} catch (IOException e) {
//				throw new RuntimeException(e);
//			}
//		}).exceptionally(t -> {
//			Minecraft.getInstance().gui.getChat().addMessage(Component.literal(t.getMessage()));
//			return null;
//		}));
//	}

//	@Redirect(method = "clearParticles",
//		slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/client/particle/ParticleEngine;particlesToAdd:Ljava/util/Queue;")),
//		at = @At(value = "INVOKE", target = "Ljava/util/Queue;clear()V"))
//	public void redirectClearParticles(Queue<Particle> queue) {
//		particlesToAdd = new ArrayBlockingQueue<>(SimplePropertiesConfig.limit);
//	}
}
