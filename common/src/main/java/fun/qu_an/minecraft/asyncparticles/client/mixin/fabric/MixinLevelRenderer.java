package fun.qu_an.minecraft.asyncparticles.client.mixin.fabric;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import fun.qu_an.minecraft.asyncparticles.client.AsyncRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LevelRenderer.class, priority = 499)
public abstract class MixinLevelRenderer {
	@Inject(method = "renderLevel",
		at =  @At(value = "FIELD", ordinal = 0, target = "Lnet/minecraft/client/renderer/LevelRenderer;transparencyChain:Lnet/minecraft/client/renderer/PostChain;"))
	private void onRenderLevelTransparencyChain(DeltaTracker deltaTracker,
												boolean renderBlockOutline,
												Camera camera,
												GameRenderer gameRenderer,
												LightTexture lightTexture,
												Matrix4f frustumMatrix,
												Matrix4f projectionMatrix,
												CallbackInfo ci,
												@Local(ordinal = 0) float f) {
		AsyncRenderer.irisOpaque(f, camera, lightTexture, null);
	}

	@WrapOperation(method = "renderLevel", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/particle/ParticleEngine;render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;F)V"))
	private void redirectRenderParticles(ParticleEngine instance,
										 LightTexture lightTexture,
										 Camera camera,
										 float f,
										 Operation<Void> original) {
		AsyncRenderer.irisTranslucent(f, camera, lightTexture, null);
	}
}
