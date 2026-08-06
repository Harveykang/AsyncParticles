
package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.gpu_acceleration;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleEngineAddon;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.TaskHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.ParticleHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.GpuParticleBehavior;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.IParticleRenderer;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.tick.AsyncTickBehavior;
import net.minecraft.client.particle.*;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

@Mixin(value = ParticleEngine.class, priority = 1500)
public abstract class MixinParticleEngine implements ParticleEngineAddon {
	@Shadow
	public Map<ParticleRenderType, Queue<Particle>> particles;

	@Shadow
	protected abstract void tickParticleList(Collection<Particle> collection);

	@ModifyExpressionValue(method = "countParticles", at = @At(value = "INVOKE", target = "Ljava/util/stream/IntStream;sum()I"))
	private int modifyCount(int i) {
		return i + GpuParticleBehavior.getInstance().gpuParticles.values().stream().mapToInt(Collection::size).sum();
	}

	@WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Ljava/util/Queue;add(Ljava/lang/Object;)Z"))
	public boolean wrapAdd(Queue<?> instance, Object e, Operation<Boolean> original) {
		if (ConfigHelper.isGpuParticles()
			&& e instanceof TextureSheetParticle tsp
			&& GpuParticleBehavior.getInstance().canRenderFast(tsp)) {
			GpuParticleBehavior.getInstance().onAddGpu(tsp);
			GpuParticleBehavior.getInstance().gpuParticles.computeIfAbsent(tsp.getRenderType(), k -> ParticleHelper.newParticleQueue()).add(tsp);
		} else {
			original.call(instance, e);
		}
		return true;
	}

	@Dynamic
	@WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Ljava/util/Set;add(Ljava/lang/Object;)Z"))
	public boolean wrapAdd(Set<?> instance, Object e, Operation<Boolean> original) {
		if (ConfigHelper.isGpuParticles()
			&& e instanceof TextureSheetParticle tsp
			&& GpuParticleBehavior.getInstance().canRenderFast(tsp)) {
			AsyncTickBehavior.getInstance().getSyncGpuParticles(tsp.getRenderType()).add(tsp);
		} else {
			original.call(instance, e);
		}
		return false;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	@Inject(method = "tick", order = 500, at = @At("TAIL"))
	private void tickTail(CallbackInfo ci) {
		if (!ConfigHelper.isGpuParticles()) {
			return;
		}
		AsyncTickBehavior tickBehavior = AsyncTickBehavior.getInstance();
		if (tickBehavior.isTailTick()) {
			GpuParticleBehavior.getInstance().flushBufferAndSwap();
		}
		int sum = 0;
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
			if (!tickAsync) {
				ParticleHelper.tickGpuParticles(() -> tickParticleList((Collection) queue));
				tickBehavior.doParticlesRemoveIf(queue);
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
		gpuParticleBehavior.setUpNextTickRendering(sum);
		IParticleRenderer renderer = gpuParticleBehavior.getOrCreateRenderer();
		renderer.prepareBuffer();
		tickBehavior.getTickTaskManager().addTask(
			() -> renderer.tick(GpuParticleBehavior.getInstance().getPerTickCameraPos(), gpuParticles));
	}
}
