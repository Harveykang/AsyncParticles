package fun.qu_an.minecraft.asyncparticles.client.mixin.render;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import fun.qu_an.minecraft.asyncparticles.client.AsyncRenderer;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.*;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LevelRenderer.class, priority = 1500)
public abstract class MixinLevelRenderer_Late {
	@Shadow
	@Nullable
	public PostChain transparencyChain;

	@Inject(method = "renderLevel", // inject later
		slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/RenderStateShard;WEATHER_TARGET:Lnet/minecraft/client/renderer/RenderStateShard$OutputStateShard;")),
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/PostChain;process(F)V"))
	private void onRenderLevelTail2(PoseStack poseStack,
									float f,
									long l,
									boolean bl,
									Camera camera,
									GameRenderer gameRenderer,
									LightTexture lightTexture,
									Matrix4f matrix4f,
									CallbackInfo ci,
									@Share(namespace = "asyncparticles", value = "isRenderAsync")
									LocalBooleanRef isRenderAsync,
									@Share(namespace = "asyncparticles", value = "isMixedParticleRendering")
									LocalBooleanRef isMixedParticleRendering) {
		if (isRenderAsync.get() &&
			!isMixedParticleRendering.get() &&
			!ConfigHelper.isCompatibilityRendering()) {
			PoseStack stack = RenderSystem.getModelViewStack();
			// so that we don't need to change the behavior of ParticleEngine.render()
			PoseStack.Pose pose = stack.poseStack.removeLast();
			AsyncRenderer.join(poseStack, f, camera, lightTexture);
			stack.poseStack.addLast(pose);
		}
	}

	@Inject(method = "renderLevel", // inject later
		at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/client/renderer/LevelRenderer;renderDebug(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/Camera;)V"))
	private void onRenderLevelTail(PoseStack poseStack,
								   float f,
								   long l,
								   boolean bl,
								   Camera camera,
								   GameRenderer gameRenderer,
								   LightTexture lightTexture,
								   Matrix4f matrix4f,
								   CallbackInfo ci,
								   @Share(namespace = "asyncparticles", value = "isRenderAsync")
								   LocalBooleanRef isRenderAsync,
								   @Share(namespace = "asyncparticles", value = "isMixedParticleRendering")
								   LocalBooleanRef isMixedParticleRendering) {
		if (isRenderAsync.get() &&
			!isMixedParticleRendering.get() &&
			!ConfigHelper.isCompatibilityRendering() &&
			transparencyChain == null) {
			AsyncRenderer.join(poseStack, f, camera, lightTexture);
		}
	}
}
