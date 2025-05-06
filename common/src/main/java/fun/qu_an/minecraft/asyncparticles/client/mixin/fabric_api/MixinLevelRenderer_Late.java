package fun.qu_an.minecraft.asyncparticles.client.mixin.fabric_api;

import net.minecraft.client.renderer.*;
import org.spongepowered.asm.mixin.Mixin;

/**
 * @implNote Suppressed if Iris mod loaded.
 */
@Mixin(value = LevelRenderer.class, priority = 1600) // After mixin.render.MixinLevelRenderer_Late
public abstract class MixinLevelRenderer_Late {
//	@Shadow
//	@Nullable
//	public PostChain transparencyChain;
//
//	// Fabric's context
//	@SuppressWarnings({"unused", "AddedMixinMembersNamePattern", "MissingUnique"})
//	private WorldRenderContext context;
//
//	@TargetHandler(
//		mixin = "net.fabricmc.fabric.mixin.client.rendering.WorldRendererMixin",
//		name = "beforeClouds"
//	)
//	@WrapWithCondition(method = "@MixinSquared:Handler", at = @At(value = "INVOKE", remap = false,
//		target = "Lnet/fabricmc/fabric/api/client/rendering/v1/WorldRenderEvents$AfterTranslucent;afterTranslucent(Lnet/fabricmc/fabric/api/client/rendering/v1/WorldRenderContext;)V"))
//	private boolean redirectRenderStage(WorldRenderEvents.AfterTranslucent instance,
//										WorldRenderContext worldRenderContext,
//										@Share(namespace = "asyncparticles", value = "isRenderAsync")
//										LocalBooleanRef isRenderAsync,
//										@Share("isDeferForgeAfterParticlesStage")
//										LocalBooleanRef isDeferForgeAfterParticlesStage) {
//		boolean b = isRenderAsync.get() &&
//					!AsyncRenderer.isMixedParticleRenderingSetting() &&
//					ConfigHelper.isDeferAfterParticlesStage();
//		isDeferForgeAfterParticlesStage.set(b);
//		return !b;
//	}
//
//	@Inject(method = "renderLevel",
//		slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/RenderStateShard;WEATHER_TARGET:Lnet/minecraft/client/renderer/RenderStateShard$OutputStateShard;")),
//		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/PostChain;process(F)V"))
//	private void onRenderLevelTail2(PoseStack poseStack,
//									float f,
//									long l,
//									boolean bl,
//									Camera camera,
//									GameRenderer gameRenderer,
//									LightTexture lightTexture,
//									Matrix4f matrix4f,
//									CallbackInfo ci,
//									@Local(ordinal = 0) Frustum frustum,
//									@Share("isDeferForgeAfterParticlesStage")
//									LocalBooleanRef isDeferForgeAfterParticlesStage) {
//		if (isDeferForgeAfterParticlesStage.get()) {
//			PoseStack stack = RenderSystem.getModelViewStack();
//			// so that we don't need to change the behavior of ParticleEngine.render()
//			PoseStack.Pose pose = stack.poseStack.removeLast();
//			WorldRenderEvents.AFTER_TRANSLUCENT.invoker().afterTranslucent(context);
//			stack.poseStack.addLast(pose);
//		}
//	}
//
//	@Inject(method = "renderLevel",
//		at = @At(value = "INVOKE", shift = At.Shift.AFTER,
//			target = "Lnet/minecraft/client/renderer/LevelRenderer;renderDebug(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/Camera;)V"))
//	private void onRenderLevelTail(PoseStack poseStack,
//								   float f,
//								   long l,
//								   boolean bl,
//								   Camera camera,
//								   GameRenderer gameRenderer,
//								   LightTexture lightTexture,
//								   Matrix4f matrix4f,
//								   CallbackInfo ci,
//								   @Local(ordinal = 0) Frustum frustum,
//								   @Share("isDeferForgeAfterParticlesStage")
//								   LocalBooleanRef isDeferForgeAfterParticlesStage) {
//		if (isDeferForgeAfterParticlesStage.get() &&
//			transparencyChain == null) {
//			WorldRenderEvents.AFTER_TRANSLUCENT.invoker().afterTranslucent(context);
//		}
//	}
}
