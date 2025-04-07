package fun.qu_an.minecraft.asyncparticles.client.mixin.render;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import fun.qu_an.minecraft.asyncparticles.client.AsyncRenderer;
import fun.qu_an.minecraft.asyncparticles.client.config.SimplePropertiesConfig;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LevelRenderer.class, priority = 500)
public abstract class MixinLevelRenderer {
	// TODO: 有没有更好的方法？
	@Inject(method = "renderLevel", at = @At("HEAD"))
	private void renderLevel(GraphicsResourceAllocator graphicsResourceAllocator,
							 DeltaTracker deltaTracker,
							 boolean renderBlockOutline,
							 Camera camera,
							 GameRenderer gameRenderer,
							 Matrix4f frustumMatrix,
							 Matrix4f projectionMatrix,
							 CallbackInfo ci) {
		// as early as possible
		AsyncRenderer.start(deltaTracker.getGameTimeDeltaPartialTick(false), camera, frustumMatrix, projectionMatrix);
	}

	@Inject(method = "renderLevel", at = @At(value = "CONSTANT", args = "stringValue=captureFrustum"))
	private void renderLevel1(CallbackInfo ci, @Local Frustum frustum){
		if (!SimplePropertiesConfig.isRenderAsync()) {
			AsyncRenderer.frustum = frustum;
		}
	}
}
