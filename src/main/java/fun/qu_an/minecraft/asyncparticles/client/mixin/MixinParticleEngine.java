package fun.qu_an.minecraft.asyncparticles.client.mixin;

import com.google.common.collect.EvictingQueue;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fun.qu_an.minecraft.asyncparticles.client.Caches;
import fun.qu_an.minecraft.asyncparticles.client.TickedParticle;
import fun.qu_an.minecraft.asyncparticles.client.config.SimplePropertiesConfig;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TrackingEmitter;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(ParticleEngine.class)
public abstract class MixinParticleEngine {
	@Mutable
	@Shadow
	@Final
	private Queue<Particle> particlesToAdd;

	@Shadow
	@Final
	private Map<ParticleRenderType, Queue<Particle>> particles;

	@Shadow
	protected ClientLevel level;

	/**
	 * @author
	 * @reason
	 */
	@Overwrite
	private void tickParticleList(Collection<Particle> collection) {
		if (!collection.isEmpty()) {
			Iterator<Particle> iterator = collection.iterator();
			while (iterator.hasNext()) {
				if (Caches.cancelled) {
					return;
				}
				Particle particle = iterator.next();
				this.tickParticle(particle);
				((TickedParticle) particle).setTicked();
				// to prevent break some mixin, trust jit compiler
				//noinspection PointlessBooleanExpression
				if (!particle.isAlive() && false) {
					iterator.remove();
				}
			}
		}
	}

	@Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/Particle;render(Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/client/Camera;F)V"))
	public void redirectRender(Particle instance, VertexConsumer vertexConsumer, Camera camera, float v) {
		if (((TickedParticle) instance).isTicked()) {
			instance.render(vertexConsumer, camera, v);
		} else {
			instance.render(vertexConsumer, camera, v + 1f);
		}
	}

	@Shadow
	@Final
	private Queue<TrackingEmitter> trackingEmitters;

	@Shadow
	protected abstract void tickParticle(Particle particle);

	/**
	 * @author
	 * @reason
	 */
	@Overwrite
	public void tick() {
		if (!Caches.shouldTickParticles) {
			if (!particlesToAdd.isEmpty()) {
				particlesToAdd = new ArrayDeque<>();
			}
			return;
		}
		particles.forEach((particleRenderType, queue) -> {
			this.level.getProfiler().push(particleRenderType.toString());
			Caches.parallelOperations.add(() -> tickParticleList(queue));
			this.level.getProfiler().pop();
		});

		if (!this.trackingEmitters.isEmpty()) {
			Caches.parallelOperations.add(() -> {
				Iterator<TrackingEmitter> iterator = this.trackingEmitters.iterator();
				while (iterator.hasNext()) {
					if (Caches.cancelled) {
						return;
					}
					TrackingEmitter trackingEmitter = iterator.next();
					trackingEmitter.tick();
					if (!trackingEmitter.isAlive()) {
						iterator.remove();
					}
				}
			});
		}

		particles.values().forEach(particles1 -> {
			if (particles1.isEmpty()) {
				return;
			}
			particles1.removeIf(particle1 -> ((TickedParticle) particle1).resetTicked());
		});

		Particle particle;
		if (!this.particlesToAdd.isEmpty()) {
			while ((particle = this.particlesToAdd.poll()) != null) {
				this.particles.computeIfAbsent(particle.getRenderType(), (p_107347_) -> EvictingQueue.create(SimplePropertiesConfig.limit)).add(particle);
			}
		}
	}

	@Inject(method = "add", at = @At(value = "HEAD"), cancellable = true)
	public void add(Particle particle, CallbackInfo ci) {
		if (!Caches.shouldTickParticles) {
			ci.cancel();
		}
	}

//	@Redirect(method = "tickParticleList", at = @At(value = "INVOKE", target = "Ljava/util/Iterator;remove()V"))
//	public void redirectRemove(Iterator instance) {
//	}

//	@Inject(method = "tickParticleList", at = @At(value = "INVOKE", target = "Ljava/util/Iterator;next()Ljava/lang/Object;"), cancellable = true)
//	public void tickParticleListNext(CallbackInfo ci) {
//		if (Caches.cancelled) {
//			ci.cancel();
//		}
//	}

	@Inject(method = "reload", at = @At(value = "RETURN"), cancellable = true)
	public void reload(PreparableReloadListener.PreparationBarrier preparationBarrier, ResourceManager resourceManager, ProfilerFiller profilerFiller, ProfilerFiller profilerFiller2, Executor executor, Executor executor2, CallbackInfoReturnable<CompletableFuture<Void>> cir) {
		cir.setReturnValue(cir.getReturnValue().thenRun(() -> {
			try {
				SimplePropertiesConfig.load();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}).exceptionally(t -> {
			Minecraft.getInstance().gui.getChat().addMessage(Component.nullToEmpty(t.getMessage()));
			return null;
		}));
	}

//	@Redirect(method = "tick", at = @At(value = "INVOKE", target = "Ljava/util/Map;forEach(Ljava/util/function/BiConsumer;)V"))
//	public void redirectForEach(Map<ParticleRenderType, Queue<Particle>> instance, BiConsumer<ParticleRenderType, Queue<Particle>> v) {
//		// do nothing
//	}
//
//	@ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Ljava/util/Queue;isEmpty()Z", ordinal = 1))
//	public boolean modifyIsEmpty(boolean original) {
//		return true;
//	}

//	@Inject(method = "tick", at = @At(value = "INVOKE", target = "Ljava/util/Queue;isEmpty()Z", ordinal = 0), cancellable = true)
//	public void tickTrackingEmitterEmpty(CallbackInfo ci) {
//		if (Caches.cancelled) {
//			ci.cancel();
//		}
//	}

//	@Inject(method = "tick", at = @At(value = "INVOKE", target = "Ljava/util/Iterator;next()Ljava/lang/Object;"), cancellable = true)
//	public void tickTrackingEmitter(CallbackInfo ci) {
//		if (Caches.cancelled) {
//			ci.cancel();
//		}
//	}
}
