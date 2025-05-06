package fun.qu_an.minecraft.asyncparticles.client.mixin.forge;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.mojang.blaze3d.vertex.PoseStack;
import fun.qu_an.minecraft.asyncparticles.client.AsyncRenderer;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.culling.Frustum;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * @implNote Suppressed if Iris mod loaded.
 */
@Mixin(value = LevelRenderer.class, priority = 1600) // After mixin.render.MixinLevelRenderer_Late
public abstract class MixinLevelRenderer_Late {
	//	@WrapWithCondition(method = "renderLevel",
	//		slice = @Slice(from = @At(value = "FIELD", remap = false, ordinal = 0,
	//			target = "Lnet/minecraftforge/client/event/RenderLevelStageEvent$Stage;AFTER_PARTICLES:Lnet/minecraftforge/client/event/RenderLevelStageEvent$Stage;")),
	//		at = {
	//			@At(value = "INVOKE", remap = false, ordinal = 0,
	//				target = "Lnet/minecraftforge/client/ForgeHooksClient;dispatchRenderStage(Lnet/minecraftforge/client/event/RenderLevelStageEvent$Stage;Lnet/minecraft/client/renderer/LevelRenderer;Lcom/mojang/blaze3d/vertex/PoseStack;Lorg/joml/Matrix4f;ILnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/culling/Frustum;)V"),
	//			@At(value = "INVOKE", remap = false, ordinal = 1,
	//				target = "Lnet/minecraftforge/client/ForgeHooksClient;dispatchRenderStage(Lnet/minecraftforge/client/event/RenderLevelStageEvent$Stage;Lnet/minecraft/client/renderer/LevelRenderer;Lcom/mojang/blaze3d/vertex/PoseStack;Lorg/joml/Matrix4f;ILnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/culling/Frustum;)V")
	//		})
	//	private boolean redirectRenderStage(RenderLevelStageEvent.Stage stage,
	//										LevelRenderer levelRenderer,
	//										PoseStack poseStack,
	//										Matrix4f projectionMatrix,
	//										int renderTick,
	//										Camera camera,
	//										Frustum frustum,
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
	//	@Inject(method = "renderLevel", // inject later
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
	//			ForgeHooksClient.dispatchRenderStage(
	//				RenderLevelStageEvent.Stage.AFTER_PARTICLES,
	//				(LevelRenderer) (Object) this,
	//				poseStack,
	//				matrix4f,
	//				this.ticks,
	//				camera,
	//				frustum);
	//			stack.poseStack.addLast(pose);
	//		}
	//	}
	//
	//	@Inject(method = "renderLevel", // inject later
	//		at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/client/renderer/LevelRenderer;renderDebug(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/Camera;)V"))
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
	//			ForgeHooksClient.dispatchRenderStage(
	//				RenderLevelStageEvent.Stage.AFTER_PARTICLES,
	//				(LevelRenderer) (Object) this,
	//				poseStack,
	//				matrix4f,
	//				this.ticks,
	//				camera,
	//				frustum);
	//		}
	//	}
}
