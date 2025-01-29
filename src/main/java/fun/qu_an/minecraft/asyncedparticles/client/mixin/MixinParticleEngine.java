package fun.qu_an.minecraft.asyncedparticles.client.mixin;

import com.google.common.collect.EvictingQueue;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import fun.qu_an.minecraft.asyncedparticles.client.Caches;
import fun.qu_an.minecraft.asyncedparticles.client.config.SimplePropertiesConfig;
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
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;

@Mixin(ParticleEngine.class)
public abstract class MixinParticleEngine {
	@Shadow
	@Final
	private Queue<Particle> particlesToAdd;

	@Shadow
	@Final
	private Map<ParticleRenderType, Queue<Particle>> particles;

	@Shadow protected ClientLevel level;

	@Shadow protected abstract void tickParticleList(Collection<Particle> collection);

	@Shadow @Final private Queue<TrackingEmitter> trackingEmitters;

	/**
	 * @author
	 * @reason
	 */
	@Overwrite
	public void tick() {
		for (Map.Entry<ParticleRenderType, Queue<Particle>> entry : particles.entrySet()) {
			if (Caches.cancelled) {
				return;
			}
			ParticleRenderType particleRenderType = entry.getKey();
			Queue<Particle> queue = entry.getValue();
			this.level.getProfiler().push(particleRenderType.toString());
			Caches.addParticleOperation(() -> tickParticleList(queue));
			this.level.getProfiler().pop();
		}

		if (!this.trackingEmitters.isEmpty()) {
			Caches.addParticleOperation(() -> {
				if (Caches.cancelled) {
					return;
				}
				for (TrackingEmitter trackingEmitter : this.trackingEmitters) {
					if (Caches.cancelled) {
						return;
					}
					trackingEmitter.tick();
				}
			});
		}

		particles.values().forEach(particles1 -> {
			if (particles1.isEmpty()) {
				return;
			}
			particles1.removeIf(particle1 -> !particle1.isAlive());
		});

		if (!this.trackingEmitters.isEmpty()) {
			trackingEmitters.removeIf(trackingEmitter -> !trackingEmitter.isAlive());
		}

		Particle particle;
		if (!this.particlesToAdd.isEmpty()) {
			while ((particle = this.particlesToAdd.poll()) != null) {
				this.particles.computeIfAbsent(particle.getRenderType(), (p_107347_) -> EvictingQueue.create(SimplePropertiesConfig.limit)).add(particle);
			}
		}
	}

	@Redirect(method = "tickParticleList", at = @At(value = "INVOKE", target = "Ljava/util/Iterator;remove()V"))
	public void redirectRemove(Iterator instance) {
	}

	@Inject(method = "tickParticleList", at = @At(value = "INVOKE", target = "Ljava/util/Iterator;next()Ljava/lang/Object;"), cancellable = true)
	public void tickParticleListNext(CallbackInfo ci) {
		if (Caches.cancelled) {
			ci.cancel();
		}
	}

	@Inject(method = "tickParticleList", at = @At(value = "HEAD"), cancellable = true)
	public void tickParticleListHead(CallbackInfo ci) {
		if (Caches.cancelled) {
			ci.cancel();
		}
	}

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
