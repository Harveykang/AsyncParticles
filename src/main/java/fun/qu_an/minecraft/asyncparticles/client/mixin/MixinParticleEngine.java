package fun.qu_an.minecraft.asyncparticles.client.mixin;

import com.google.common.collect.EvictingQueue;
import com.google.common.collect.Lists;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import fun.qu_an.minecraft.asyncparticles.client.*;
import fun.qu_an.minecraft.asyncparticles.client.config.SimplePropertiesConfig;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

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

//	@Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/Particle;render(Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/client/Camera;F)V"))
//	public void redirectRender(Particle instance, VertexConsumer vertexConsumer, Camera camera, float v) {
//		if (((ParticleAddon) instance).asyncParticles$isTicked()) {
//			instance.render(vertexConsumer, camera, v);
//		} else {
//			instance.render(vertexConsumer, camera, v + 1f);
//		}
//	}

	/**
	 * @author
	 * @reason
	 */
	@Overwrite
	public void render(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, LightTexture lightTexture, Camera camera, float f) {
		// TODO: culling
		PoseStack poseStack2 = null;
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
			if (iterable == null) {
				continue;
			}
			BufferBuilder bufferBuilder = AsyncRenderer.getBufferBuilder(particleRenderType);
			if (!AsyncRenderer.isStart) {
				List<? extends Particle> particles1 = AsyncRenderer.pollSync(particleRenderType);
				if (!particles1.isEmpty()){
					if (!bufferBuilder.building()) {
						bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
					}
					for (Particle particle : particles1) {
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
				if (bufferBuilder.building()){
					RenderSystem.setShader(GameRenderer::getParticleShader);
					try {
						// FIXME: 这样快还是查表快？
						particleRenderType.begin(null, this.textureManager);
					} catch (NullPointerException ignored) {
						// setup RenderSystem with a null bufferbuilder
					}
					BufferUploader.drawWithShader(bufferBuilder.end());
				}
			} else {
				if (!bufferBuilder.building()) {
					bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
				}
				Runnable runnable = () -> iterable.forEach(particle -> {
					if (((ParticleAddon) particle).asyncedParticles$isRenderSync()) {
						AsyncRenderer.recordSync(particleRenderType, particle);
						return;
					}
					float g = ((ParticleAddon) particle).asyncParticles$isTicked() ? f : f + 1f;
					try {
						particle.render(bufferBuilder, camera, g);
					} catch (Throwable throwable) {
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

	/**
	 * @author
	 * @reason
	 */
	@Overwrite
	public void tick() {
		particles.forEach((particleRenderType, queue) -> {
			this.level.getProfiler().push(particleRenderType.toString());
			AsyncTicker.particleOperations.add(() -> tickParticleList(queue));
			this.level.getProfiler().pop();
		});

		if (!this.trackingEmitters.isEmpty()) {
			AsyncTicker.particleOperations.add(() -> {
				List<TrackingEmitter> list = Lists.newArrayList();
				for (TrackingEmitter emitter : this.trackingEmitters) {
					if (AsyncTicker.cancelled) {
						this.trackingEmitters.removeAll(list);
						return;
					}
					emitter.tick();
					if (ModListHelper.VS_LOADED) {
						if (VSClientUtils.isOutOfSight(emitter)) {
							emitter.remove();
						}
					}
					if (!emitter.isAlive()) {
						list.add(emitter);
					}
				}
				this.trackingEmitters.removeAll(list);
			});
		}

		if (AsyncTicker.particleCleanup != null) {
			AsyncTicker.particleCleanup.join();
			AsyncTicker.particleCleanup = null;
		}

		if (!this.particlesToAdd.isEmpty()) {
			Particle particle;
			while ((particle = this.particlesToAdd.poll()) != null) {
				// TODO: 实现可分割无锁队列
				this.particles.computeIfAbsent(particle.getRenderType(), (p_107347_) -> EvictingQueue.create(SimplePropertiesConfig.limit)).add(particle);
			}
		}

//		particles.forEach((particleRenderType, queue) -> {
//			this.level.getProfiler().push(particleRenderType.toString());
//			tickParticleList(queue); // TODO: 实现可分割队列
//			this.level.getProfiler().pop();
//		});
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
			if (AsyncTicker.cancelled) {
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

	@Unique
	private Runnable combineTasks(Particle... task) {
		int size = task.length;
		return () -> {
			//noinspection ForLoopReplaceableByForEach
			for (int i = 0; i < size; i++) {
				if (AsyncTicker.cancelled) {
					return;
				}
				Particle particle = task[i];
				if (particle == null) {
					return;
				}
				this.tickParticle(particle);
				((ParticleAddon) particle).asyncParticles$setTicked();
				if (ModListHelper.VS_LOADED) {
					if (VSClientUtils.isOutOfSight(particle)) {
						particle.remove();
					}
				}
			}
		};
	}

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

	@Inject(method = "reload", at = @At(value = "RETURN"), cancellable = true)
	public void reload(CallbackInfoReturnable<CompletableFuture<Void>> cir) {
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
}
