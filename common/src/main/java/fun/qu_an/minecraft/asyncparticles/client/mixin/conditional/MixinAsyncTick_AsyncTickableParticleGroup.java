package fun.qu_an.minecraft.asyncparticles.client.mixin.conditional;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.addon.AsyncTickableParticleGroup;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceSets;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Collections;
import java.util.Set;

@Mixin({
	QuadParticleGroup.class,
	ItemPickupParticleGroup.class,
	ElderGuardianParticleGroup.class
})
public abstract class MixinAsyncTick_AsyncTickableParticleGroup implements AsyncTickableParticleGroup {
	@Unique
	private final Set<Particle> asyncparticles$syncParticles = new ReferenceOpenHashSet<>();

	@Dynamic
	@WrapOperation(method = "extractRenderState", require = 0, at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/particle/SingleQuadParticle;extract(Lnet/minecraft/client/renderer/state/level/QuadParticleRenderState;Lnet/minecraft/client/Camera;F)V"))
	private static void wrapExtract(SingleQuadParticle instance, QuadParticleRenderState quadParticleRenderState, Camera camera, float f, Operation<Void> original) {
		if (!instance.isAlive()) {
			return;
		}
		if (!((ParticleAddon) instance).asyncparticles$isTicked() && f <= 1.0f) {
			f += 1.0F;
		}
		original.call(instance, quadParticleRenderState, camera, f);
	}

	public Set<Particle> asyncparticles$getSyncParticles() {
		return Collections.unmodifiableSet(asyncparticles$syncParticles);
	}

	@Override
	public void asyncparticles$recordSync(Particle particle) {
		synchronized (asyncparticles$syncParticles) {
			asyncparticles$syncParticles.add(particle);
		}
	}
}
