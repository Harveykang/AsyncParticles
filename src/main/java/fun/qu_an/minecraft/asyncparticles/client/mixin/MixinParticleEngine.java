package fun.qu_an.minecraft.asyncparticles.client.mixin;

import com.google.common.collect.EvictingQueue;
import com.google.common.collect.Lists;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
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
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TrackingEmitter;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(value = ParticleEngine.class, priority = 500)
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
				if (AsyncTicker.cancelled) {
					return;
				}
				Particle particle = iterator.next();
				this.tickParticle(particle);
				((TickedParticle) particle).asyncParticles$setTicked();
				if (ModListHelper.VS_LOADED) {
					if (VSClientUtils.isOutOfSight(particle)) {
						particle.remove();
						continue;
					}
				}
				// to prevent break some mixin, trust jit compiler
				//noinspection PointlessBooleanExpression
				if (!particle.isAlive() && false) {
					iterator.remove();
				}
			}
		}
	}

//	@Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/Particle;render(Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/client/Camera;F)V"))
//	public void redirectRender(Particle instance, VertexConsumer vertexConsumer, Camera camera, float v) {
//		if (((TickedParticle) instance).asyncParticles$isTicked()) {
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
		PoseStack poseStack2 = null;
		if (!AsyncRenderer.isStart) {
			lightTexture.turnOnLightLayer();
			RenderSystem.enableDepthTest();
			poseStack2 = RenderSystem.getModelViewStack();
			poseStack2.pushPose();
			poseStack2.mulPoseMatrix(poseStack.last().pose());
			RenderSystem.applyModelViewMatrix();
		}

		for (ParticleRenderType particleRenderType : RENDER_ORDER) {
			Queue<Particle> iterable = this.particles.get(particleRenderType);
			if (iterable == null) {
				continue;
			}
			if (!AsyncRenderer.isStart) {
				RenderSystem.setShader(GameRenderer::getParticleShader);
				BufferBuilder bufferBuilder = AsyncRenderer.getBufferBuilder(particleRenderType);
				try {
					particleRenderType.begin(null, this.textureManager);
				} catch (NullPointerException ignored) {
					// setup RenderSystem with a null bufferbuilder
				}
				BufferUploader.drawWithShader(bufferBuilder.end());
			} else {
				BufferBuilder bufferBuilder = AsyncRenderer.getBufferBuilder(particleRenderType);
//					particleRenderType.begin(bufferBuilder, this.textureManager);
				bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
				Runnable runnable = () -> iterable.forEach(particle -> {
					try {
						if (((TickedParticle) particle).asyncParticles$isTicked()) {
							AsyncRenderer.getBufferBuilder(particleRenderType);
							particle.render(bufferBuilder, camera, f);
						} else {
							particle.render(bufferBuilder, camera, f + 1f);
						}
					} catch (Throwable throwable) {
						CrashReport crashReport = CrashReport.forThrowable(throwable, "Rendering Particle");
						CrashReportCategory crashReportCategory = crashReport.addCategory("Particle being rendered");
						Objects.requireNonNull(particle);
						crashReportCategory.setDetail("Particle", particle::toString);
						Objects.requireNonNull(particleRenderType);
						crashReportCategory.setDetail("Particle Type", particleRenderType::toString);
						throw new ReportedException(crashReport);
					}
				});
				AsyncRenderer.add(particleRenderType, runnable);
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
		if (!AsyncTicker.shouldTickParticles) {
			if (!particlesToAdd.isEmpty()) {
				particlesToAdd = new ArrayDeque<>();
			}
			return;
		}
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

//		particles.values().forEach(particles1 -> {
//			if (particles1.isEmpty()) {
//				return;
//			}
//			particles1.removeIf(particle1 -> ((TickedParticle) particle1).asyncParticles$shouldRemove());
//		});
		if (AsyncTicker.particleCleanup != null) {
			AsyncTicker.particleCleanup.join();
			AsyncTicker.particleCleanup = null;
		}

		Particle particle;
		if (!this.particlesToAdd.isEmpty()) {
			while ((particle = this.particlesToAdd.poll()) != null) {
				this.particles.computeIfAbsent(particle.getRenderType(), (p_107347_) -> EvictingQueue.create(SimplePropertiesConfig.limit)).add(particle);
			}
		}
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
