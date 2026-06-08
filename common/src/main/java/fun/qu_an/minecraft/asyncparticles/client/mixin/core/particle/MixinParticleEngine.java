package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle;

import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleEngineAddon;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleGroupAddition;
import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.async_tick.AsyncTickBehavior;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.async_tick.TrackedParticleCountsMap;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.GpuParticleBehavior;
import fun.qu_an.minecraft.asyncparticles.client.util.BusyWaitEvictingQueue;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.ParticleLimit;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

@Mixin(ParticleEngine.class)
public class MixinParticleEngine implements ParticleEngineAddon {
	@Mutable
	@Shadow
	@Final
	private static List<ParticleRenderType> RENDER_ORDER;
	@Mutable
	@Shadow
	@Final
	private Object2IntOpenHashMap<ParticleLimit> trackedParticleCounts;
	@Mutable
	@Final
	@Shadow
	private Queue<Particle> particlesToAdd;
	@Mutable
	@Final
	@Shadow
	private Queue<TrackingEmitter> trackingEmitters;
	@Shadow
	@Final
	public Map<ParticleRenderType, ParticleGroup<?>> particles;

	@Inject(order = 1500, method = "<init>", at = @At(value = "RETURN"))
	public void initTail(CallbackInfo ci) {
		trackedParticleCounts = new TrackedParticleCountsMap();
		particlesToAdd = BusyWaitEvictingQueue.newInstance(1024, ConfigHelper.getParticleLimit(), AsyncTickBehavior.getInstance()::onEvict);
		trackingEmitters = BusyWaitEvictingQueue.newInstance(256, ConfigHelper.getParticleLimit(), AsyncTickBehavior.getInstance()::onEvict);
	}

	@Inject(method = "clearParticles", at = @At("HEAD"))
	private void clearParticles(CallbackInfo ci) {
		this.particles.values().forEach(group -> ((ParticleGroupAddition) group).asyncparticles$removeDeadParticles());
	}

	@Override
	public void asyncparticle$addRenderType(ParticleRenderType particleRenderType) {
		if (!ModListHelper.IS_FORGE && !RENDER_ORDER.contains(particleRenderType)) {
			List<ParticleRenderType> renderOrder = RENDER_ORDER;
			if (!(renderOrder instanceof ArrayList<ParticleRenderType>)) {
				renderOrder = RENDER_ORDER = new ArrayList<>(RENDER_ORDER);
			}
			if (particleRenderType == GpuParticleBehavior.INSTANCE.getRenderType(ParticleRenderType.SINGLE_QUADS)) {
				renderOrder.add(renderOrder.indexOf(ParticleRenderType.SINGLE_QUADS), particleRenderType);
			} else {
				renderOrder.add(particleRenderType);
			}
		}
	}
}
