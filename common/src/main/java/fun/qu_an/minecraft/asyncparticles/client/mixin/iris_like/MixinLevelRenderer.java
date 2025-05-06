package fun.qu_an.minecraft.asyncparticles.client.mixin.iris_like;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.mojang.blaze3d.vertex.PoseStack;
import fun.qu_an.minecraft.asyncparticles.client.AsyncRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LevelRenderer.class, priority = 599) // After mixin.render.MixinLevelRenderer
public abstract class MixinLevelRenderer {
	@Inject(method = "renderLevel",
		at = @At(value = "FIELD", ordinal = 0, target = "Lnet/minecraft/client/renderer/LevelRenderer;transparencyChain:Lnet/minecraft/client/renderer/PostChain;"))
	private void onRenderLevelTransparencyChain(PoseStack poseStack,
												float f,
												long l,
												boolean bl,
												Camera camera,
												GameRenderer gameRenderer,
												LightTexture lightTexture,
												Matrix4f projectionMatrix,
												CallbackInfo ci,
												@Share(namespace = "asyncparticles", value = "isRenderAsync")
												LocalBooleanRef isRenderAsync,
												@Share(namespace = "asyncparticles", value = "isMixedParticleRendering")
													LocalBooleanRef isMixedParticleRendering) {
		if (isRenderAsync.get() && isMixedParticleRendering.get()) {
			AsyncRenderer.irisOpaque(poseStack, f, camera, lightTexture);
		}
	}
}
