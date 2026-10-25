package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.polytone;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import fun.qu_an.minecraft.asyncparticles.client.compat.polytone.PolytoneParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.compat.polytone.PolytoneParticleTypeAddon;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.GpuParticleBehavior;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.tick.AsyncTickBehavior;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import net.mehvahdjukaar.polytone.content.particle.custom.*;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Queue;

@Mixin(value = CustomParticleType.class, remap = false)
public abstract class MixinCustomParticleType implements PolytoneParticleTypeAddon {
	@Shadow
	@Final
	protected IRotationProvider rotationProvider;
	@Shadow
	@Final
	protected ParticleRenderMode renderType;
	@Shadow
	@Final
	protected int exclusionRadius;
	@Shadow
	@Final
	protected CustomParticleType.RenderOffset offset;

	@Override
	public boolean asyncparticles$isSimpleQuad() {
		return rotationProvider == RotationMode.LOOK_AT_XYZ
			&& offset.camera() == 0.0f
			&& (renderType == ParticleRenderMode.TERRAIN
			|| renderType == ParticleRenderMode.SOLID
			|| renderType == ParticleRenderMode.CUTOUT
			|| renderType == ParticleRenderMode.OPAQUE
			|| renderType == ParticleRenderMode.TRANSLUCENT);
	}

	@Override
	public Vec3 asyncparticles$getWorldOffset() {
		return offset.world();
	}

	@ModifyExpressionValue(method = "createParticle", remap = true, at = @At(value = "INVOKE", remap = false,
		target = "Lnet/mehvahdjukaar/polytone/content/config/OptionHolder;get()Ljava/lang/Object;"))
	private Object initParticlesAsync(Object offThread, @Local(ordinal = 0) CustomParticleInstance particle) {
		// Polytone's pending list is main-thread-only; off-thread creation can initialize in place.
		if (!ThreadUtil.isOnMainThread()) {
			return false;
		}
		if (!ConfigHelper.isPolytoneAsyncTick()) {
			return offThread;
		}
		if (ConfigHelper.isGpuOnlyAsyncParticleTick() || ConfigHelper.isPolytoneGpuOnlyAsyncTick()) {
			return false;
		}
		// GPU particles must finish init before the main thread appends their rendering data.
		if (ConfigHelper.isPolytoneGpuRendering() && ((PolytoneParticleAddon) particle).asyncparticles$canRenderFast()) {
			return false;
		}
		return !AsyncTickBehavior.getInstance().shouldSyncRenderType(particle.getRenderType().getClass());
	}

	@ModifyReturnValue(method = "createParticle", at = @At("RETURN"), remap = true)
	private Particle checkGpuExclusionRadius(Particle particle) {
		if (particle == null) {
			return null;
		}
		if (!ConfigHelper.isPolytoneAsyncTick()) {
			return particle;
		}
		if (exclusionRadius <= 0) {
			return particle;
		}
		Queue<TextureSheetParticle> queue = GpuParticleBehavior.getInstance().gpuParticles.get(particle.getRenderType());
		if (queue == null) {
			return particle;
		}
		// Polytone already checked the CPU queue. GPU particles live in a separate queue.
		double radiusSquared = (double) exclusionRadius * exclusionRadius;
		for (TextureSheetParticle other : queue) {
			if (other instanceof CustomParticleInstance custom && custom.type == (Object) this
				&& Mth.lengthSquared(other.x - particle.x, other.y - particle.y, other.z - particle.z) < radiusSquared) {
				if (custom.hasAgeLeft()) {
					return null;
				}
				other.remove();
			}
		}
		return particle;
	}
}
