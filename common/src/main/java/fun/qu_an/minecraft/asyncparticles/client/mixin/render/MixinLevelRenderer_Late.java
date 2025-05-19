package fun.qu_an.minecraft.asyncparticles.client.mixin.render;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import fun.qu_an.minecraft.asyncparticles.client.AsyncRenderer;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.*;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LevelRenderer.class, priority = 1500)
public abstract class MixinLevelRenderer_Late {
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
			AsyncRenderer.endAll(poseStack, f, camera, lightTexture);
			stack.poseStack.addLast(pose);
		}
	}
}
