package fun.qu_an.minecraft.asyncparticles.client.mixin;

import com.google.common.collect.EvictingQueue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import fun.qu_an.minecraft.asyncparticles.client.*;
import fun.qu_an.minecraft.asyncparticles.client.config.SimplePropertiesConfig;
import fun.qu_an.minecraft.asyncparticles.client.util.FakeBeginBufferBuilder;
import fun.qu_an.minecraft.asyncparticles.client.util.FakeEndTesselator;
import fun.qu_an.minecraft.asyncparticles.client.util.TrackedParticleCountsMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.particles.ParticleGroup;
import org.slf4j.Logger;
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

	@Shadow
	@Final
	private TextureManager textureManager;

	@Shadow
	@Final
	private static List<ParticleRenderType> RENDER_ORDER;

	@Shadow
	@Final
	private static Logger LOGGER;

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
	public void render(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, LightTexture lightTexture, Camera camera, float f) {
		// TODO: culling
		PoseStack poseStack2 = null;
		Frustum frustum = AsyncRenderer.frustum;
		if (!AsyncRenderer.isStart) {
			lightTexture.turnOnLightLayer();
			RenderSystem.enableDepthTest();
			poseStack2 = RenderSystem.getModelViewStack();
			poseStack2.pushPose();
			poseStack2.mulPoseMatrix(poseStack.last().pose());
			RenderSystem.applyModelViewMatrix();
		}

		for (ParticleRenderType particleRenderType : (ModListHelper.IS_FORGE ? particles.keySet() : RENDER_ORDER)) {
			Queue<Particle> iterable = this.particles.get(particleRenderType);
			if (iterable == null || iterable.isEmpty()) {
				continue;
			}
			BufferBuilder bufferBuilder = AsyncRenderer.getBufferBuilder(particleRenderType);
			if (!AsyncRenderer.isStart) {
				List<? extends Particle> particles1 = AsyncRenderer.getSync(particleRenderType);
				if (!particles1.isEmpty()) {
					if (!bufferBuilder.building()) {
						bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
					}
					for (Particle particle : particles1) {
						if (!frustum.isVisible(particle.getBoundingBox())) {
							continue;
						}
						float g = ((ParticleAddon) particle).asyncParticles$isTicked() ? f : f + 1f;
						try {
							particle.render(bufferBuilder, camera, g);
						} catch (Throwable throwable) {
							CrashReport crashReport = CrashReport.forThrowable(throwable, "Rendering Particle");
							CrashReportCategory crashReportCategory = crashReport.addCategory("Particle being rendered");
							Objects.requireNonNull(particle);
							crashReportCategory.setDetail("Particle", particle::toString);
							Objects.requireNonNull(particleRenderType);
							crashReportCategory.setDetail("Particle Type", particleRenderType::toString);
							throw new ReportedException(crashReport);
						}
					}
				}
				if (bufferBuilder.building()) {
					RenderSystem.setShader(GameRenderer::getParticleShader);
					particleRenderType.begin(FakeBeginBufferBuilder.INSTANCE, this.textureManager);
					BufferUploader.drawWithShader(bufferBuilder.end());
					particleRenderType.end(FakeEndTesselator.INSTANCE);
				}
			} else {
				if (!bufferBuilder.building()) {
					bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
				}
				Runnable runnable = () -> iterable.forEach(particle -> {
					if (particle == null // might be null because ArrayDeque is not thread-safe
						|| !frustum.isVisible(particle.getBoundingBox())) {
						return;
					}
					if (((ParticleAddon) particle).asyncedParticles$isRenderSync()) {
						AsyncRenderer.recordSync(particleRenderType, particle);
						return;
					}
					float g = ((ParticleAddon) particle).asyncParticles$isTicked() ? f : f + 1f;
					try {
						particle.render(bufferBuilder, camera, g);
					} catch (Throwable throwable) {
						LOGGER.error("Exception while rendering particle, marking as sync", throwable);
						((ParticleAddon) particle).asyncedParticles$setRenderSync();
						AsyncRenderer.markAsSync(particle.getClass());
						AsyncRenderer.recordSync(particleRenderType, particle);
					}
				});
				AsyncRenderer.add(runnable);
			}
		}

		if (!AsyncRenderer.isStart) {
			poseStack2.popPose();
			RenderSystem.applyModelViewMatrix();
			RenderSystem.depthMask(true);
			RenderSystem.disableBlend();
			lightTexture.turnOffLightLayer();
		}
	}

	/**
	 * @author
	 * @reason
	 */
	@Overwrite
	public void tick() {
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
				Queue<Particle> queue = this.particles.computeIfAbsent(p.getRenderType(), (p_107347_) -> EvictingQueue.create(SimplePropertiesConfig.limit));
				if (queue.size() < SimplePropertiesConfig.limit) { // TODO: 能不能取消粒子组检查？
					p.getParticleGroup().ifPresent(g -> updateCount(g, 1));
				}
				queue.add(p);
			});
			particlesToAdd.clear();
			// TODO: 实现线程安全的低锁开销队列，目前会因为一些粒子在tick时添加新的粒子导致并发访问
			//  不会抛异常，因为遍历的时候不会检查为空性，无明显影响
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
//			Minecraft.getInstance().gui.getChat().addMessage(Component.nullToEmpty(t.getMessage()));
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
