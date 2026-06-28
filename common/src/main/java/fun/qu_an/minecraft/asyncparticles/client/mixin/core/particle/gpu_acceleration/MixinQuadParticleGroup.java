package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.gpu_acceleration;

import fun.qu_an.minecraft.asyncparticles.client.addon.GpuParticleGroup;
import fun.qu_an.minecraft.asyncparticles.client.addon.GpuQuadParticleRenderState;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.ParticleHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.tick.AsyncTickBehavior;
import fun.qu_an.minecraft.asyncparticles.client.util.IterationSafeEvictingQueue;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Queue;

@Mixin(QuadParticleGroup.class)
public abstract class MixinQuadParticleGroup implements GpuParticleGroup {
	@Shadow
	@Final
	private QuadParticleRenderState particleTypeRenderState;
	@Unique
	private final Queue<SingleQuadParticle> asyncparticles$gpuParticles = ParticleHelper.newParticleQueue();

	@Inject(method = "<init>", at = @At("RETURN"))
	private void onInit(CallbackInfo ci) {
		((GpuQuadParticleRenderState) particleTypeRenderState).asyncparticles$setGroup((QuadParticleGroup) (Object) this);
	}

	@Override
	public Queue<SingleQuadParticle> asyncparticles$getGpuParticles() {
		return asyncparticles$gpuParticles;
	}

	@Override
	public void asyncparticles$removeDeadGpuParticles() {
		if (ConfigHelper.isParallelQueueRemoval()) {
			((IterationSafeEvictingQueue<? extends Particle>) asyncparticles$gpuParticles)
				.parallelRemoveIf(particle ->
						AsyncTickBehavior.getInstance().shouldRemove(particle),
					ConfigHelper.isParallelQueueEviction(),
					AsyncTickBehavior.THREADS,
					AsyncTickBehavior.getInstance().getExecutor());
		} else {
			asyncparticles$gpuParticles.removeIf(particle ->
				AsyncTickBehavior.getInstance().shouldRemove(particle));
		}
	}
}
