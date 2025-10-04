package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.ParticlesRenderState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(value = LevelRenderer.class, priority = 1500)
public abstract class MixinLevelRenderer {
	@Shadow
	@Final
	private ParticlesRenderState particlesRenderState;
	@Unique
	private Operation<Void> asyncparticles$extractParticles;

	@Shadow
	protected abstract Frustum prepareCullFrustum(Matrix4f matrix4f, Matrix4f matrix4f3, Vec3 vec3);

	@Inject(method = "renderLevel", at = @At(value = "HEAD"))
	public void extractParticles(GraphicsResourceAllocator graphicsResourceAllocator,
								 DeltaTracker deltaTracker,
								 boolean bl,
								 Camera camera,
								 Matrix4f matrix4f,
								 Matrix4f matrix4f2,
								 Matrix4f matrix4f3,
								 GpuBufferSlice gpuBufferSlice,
								 Vector4f vector4f,
								 boolean bl2,
								 CallbackInfo ci,
								 @Share("earlyFrustum") LocalRef<Frustum> frustumRef) {
		Frustum frustum = prepareCullFrustum(matrix4f, matrix4f3, camera.getPosition());
		frustumRef.set(frustum);
		if (asyncparticles$extractParticles != null) {
			asyncparticles$extractParticles.call(Minecraft.getInstance().particleEngine,
				particlesRenderState, frustum, camera, deltaTracker.getGameTimeDeltaPartialTick(false));
		}
	}

	@Redirect(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;prepareCullFrustum(Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/client/renderer/culling/Frustum;"))
	private Frustum redirectPrepareCullFrustum(LevelRenderer instance,
											   Matrix4f matrix4f,
											   Matrix4f matrix4f2,
											   Vec3 vec3,
											   @Share("earlyFrustum") LocalRef<Frustum> frustumRef) {
		return Objects.requireNonNull(frustumRef.get());
	}

	@WrapOperation(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/ParticleEngine;extract(Lnet/minecraft/client/renderer/state/ParticlesRenderState;Lnet/minecraft/client/renderer/culling/Frustum;Lnet/minecraft/client/Camera;F)V"))
	public void extractParticles(ParticleEngine instance,
								 ParticlesRenderState particlesRenderState,
								 Frustum frustum,
								 Camera camera,
								 float f,
								 Operation<Void> original) {
		if (asyncparticles$extractParticles == null) {
			asyncparticles$extractParticles = original;
			original.call(instance, particlesRenderState, frustum, camera, f);
		}
	}
}
