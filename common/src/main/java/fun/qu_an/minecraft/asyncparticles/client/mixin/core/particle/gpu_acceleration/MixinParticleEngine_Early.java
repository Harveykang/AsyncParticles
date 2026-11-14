package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.gpu_acceleration;

import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.TaskHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.ParticleHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.GpuParticleBehavior;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.IParticleRenderer;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.tick.AsyncTickBehavior;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.Map;
import java.util.Queue;

@Mixin(value = ParticleEngine.class, priority = 500)
public abstract class MixinParticleEngine_Early {
	@Shadow
	protected abstract void tickParticleList(Collection<Particle> collection);

	@SuppressWarnings({"unchecked", "rawtypes"})
	@Inject(method = "tick", at = @At("TAIL"))
	private void tickTail(CallbackInfo ci) {
		if (!ConfigHelper.isGpuParticles()) {
			return;
		}
		int sum = 0;
		AsyncTickBehavior tickBehavior = AsyncTickBehavior.getInstance();
		boolean tickAsync = ConfigHelper.isAsyncParticleTick() && tickBehavior.isParticlePhase();
		TaskHelper taskHelper = tickBehavior.getTickTaskManager();
		Map<ParticleRenderType, Queue<TextureSheetParticle>> gpuParticles = GpuParticleBehavior.getInstance().gpuParticles;
		for (Map.Entry<ParticleRenderType, Queue<TextureSheetParticle>> entry : gpuParticles.entrySet()) {
			Queue<TextureSheetParticle> queue = entry.getValue();
			int size = queue.size();
			if (size == 0) {
				continue;
			}
			sum += size;
			if (!tickAsync || tickBehavior.shouldSyncRenderType(entry.getKey().getClass())) {
				ParticleHelper.tickGpuParticles(() -> tickParticleList((Collection) queue));
			} else {
				ParticleRenderType renderType = entry.getKey();
				ParticleHelper.tickGpuParticles(() -> tickParticleList(tickBehavior.getSyncGpuParticles(renderType)));
				taskHelper.addTask(() -> ParticleHelper.tickGpuParticles(() -> tickParticleList((Collection) queue)));
			}
		}
		if (tickAsync) {
			taskHelper.groupTasks(true);
		}
		if (!tickBehavior.isTailTick()) {
			return;
		}
		GpuParticleBehavior gpuParticleBehavior = GpuParticleBehavior.getInstance();
		gpuParticleBehavior.flushBufferAndSwap();
		gpuParticleBehavior.setUpNextTickRendering(sum);
		IParticleRenderer renderer = gpuParticleBehavior.getOrCreateRenderer();
		renderer.prepareBuffer();
		tickBehavior.getTickTaskManager().addTask(
			() -> renderer.tick(GpuParticleBehavior.getInstance().getPerTickCameraPos(), gpuParticles));
	}
}
