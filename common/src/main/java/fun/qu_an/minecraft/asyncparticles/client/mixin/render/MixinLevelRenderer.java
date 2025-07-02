package fun.qu_an.minecraft.asyncparticles.client.mixin.render;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import fun.qu_an.minecraft.asyncparticles.client.AsyncRenderer;
import fun.qu_an.minecraft.asyncparticles.client.compat.InternalRenderingMode;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = LevelRenderer.class, priority = 500)
public abstract class MixinLevelRenderer {
	@Shadow
	public Frustum capturedFrustum;

	@Shadow
	public boolean captureFrustum;

	@Shadow
	public Frustum cullingFrustum;

	@Shadow @Final private WeatherEffectRenderer weatherEffectRenderer;

	@Shadow private int ticks;

	// TODO: 有没有更好的方法？
	@Inject(method = "renderLevel", at = @At("HEAD"))
	private void renderLevel(GraphicsResourceAllocator graphicsResourceAllocator,
							 DeltaTracker deltaTracker,
							 boolean bl,
							 Camera camera,
							 GameRenderer gameRenderer,
							 Matrix4f matrix4f,
							 Matrix4f matrix4f2,
							 CallbackInfo ci,
							 @Share(namespace = "asyncparticles", value = "internalRenderingMode")
							 LocalIntRef irm) {
		float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
		boolean b = this.capturedFrustum != null;
		Frustum frustum = AsyncRenderer.frustum = b ? this.capturedFrustum : this.cullingFrustum;
		Vec3 cameraPos = camera.getPosition();
		if (this.captureFrustum) {
			this.capturedFrustum = b ? new Frustum(matrix4f, matrix4f2) : frustum;
			this.capturedFrustum.prepare(cameraPos.x, cameraPos.y, cameraPos.z);
			this.captureFrustum = false;
		}
		int irmValue = InternalRenderingMode.updateInternalMode(ConfigHelper.getParticleRenderingMode());
		irm.set(irmValue);
		AsyncRenderer.begin(partialTick, camera, irmValue, weatherEffectRenderer, ticks);
	}

	@ModifyExpressionValue(method = "renderLevel", at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
		target = "Lnet/minecraft/client/renderer/LevelRenderer;captureFrustum:Z"))
	private static boolean redirectPrepare(boolean original) {
		return false;
	}
}
