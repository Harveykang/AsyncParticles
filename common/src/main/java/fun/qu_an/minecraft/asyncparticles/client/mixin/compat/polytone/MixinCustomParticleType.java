package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.polytone;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import fun.qu_an.minecraft.asyncparticles.client.compat.polytone.PolytoneParticleTypeAddon;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.GpuParticleBehavior;
import net.mehvahdjukaar.polytone.content.particle.custom.CustomParticleInstance;
import net.mehvahdjukaar.polytone.content.particle.custom.CustomParticleType;
import net.mehvahdjukaar.polytone.content.particle.custom.IRotationProvider;
import net.mehvahdjukaar.polytone.content.particle.custom.ParticleRenderMode;
import net.mehvahdjukaar.polytone.content.particle.custom.RotationMode;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;
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
			&& CustomParticleType.RenderOffset.NONE.equals(offset)
			&& (renderType == ParticleRenderMode.TERRAIN
			|| renderType == ParticleRenderMode.SOLID
			|| renderType == ParticleRenderMode.CUTOUT
			|| renderType == ParticleRenderMode.OPAQUE
			|| renderType == ParticleRenderMode.TRANSLUCENT);
	}

	@ModifyReturnValue(method = "createParticle", at = @At("RETURN"), remap = true)
	private Particle checkGpuExclusionRadius(Particle particle) {
		if (particle == null) {
			return null;
		}
		if (!ConfigHelper.isPolytoneTakeover()) {
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
				custom.remove();
			}
		}
		return particle;
	}
}
