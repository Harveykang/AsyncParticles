package fun.qu_an.minecraft.asyncparticles.client.mixin.core.render;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import fun.qu_an.minecraft.asyncparticles.client.particle.AsyncRenderBehavior;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static fun.qu_an.minecraft.asyncparticles.client.compat.InternalRenderingMode.DELAYED_ASYNC;

@Mixin(value = LevelRenderer.class, priority = 1500)
public abstract class MixinLevelRenderer_Late {
	@Inject(method = "renderLevel", // inject later
		at = @At(value = "FIELD", ordinal = 2, target = "Lnet/minecraft/client/renderer/LevelRenderer;transparencyChain:Lnet/minecraft/client/renderer/PostChain;"))
	private void onRenderLevelTail2(PoseStack poseStack,
									float partialTick,
									long l,
									boolean bl,
									Camera camera,
									GameRenderer gameRenderer,
									LightTexture lightTexture,
									Matrix4f matrix4f,
									CallbackInfo ci,
									@Share(namespace = "asyncparticles", value = "internalRenderingMode")
									LocalIntRef irm) {
		if (irm.get() == DELAYED_ASYNC) {
			PoseStack stack = RenderSystem.getModelViewStack();
			// so that we don't need to change the behavior of ParticleEngine.render()
			PoseStack.Pose pose = stack.poseStack.removeLast();
			AsyncRenderBehavior.endAll(poseStack, partialTick, camera, lightTexture, true);
			stack.poseStack.addLast(pose);
			RenderSystem.applyModelViewMatrix();
		}
	}
}
