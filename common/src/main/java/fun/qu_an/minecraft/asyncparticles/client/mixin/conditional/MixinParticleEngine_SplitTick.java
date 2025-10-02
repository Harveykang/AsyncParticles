package fun.qu_an.minecraft.asyncparticles.client.mixin.conditional;

import com.bawnorton.mixinsquared.TargetHandler;
import com.llamalad7.mixinextras.sugar.Local;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.particle.AsyncTickBehavior;
import fun.qu_an.minecraft.asyncparticles.client.particle.GpuParticleBehavior;
import fun.qu_an.minecraft.asyncparticles.client.particle.TickParticleRecursiveAction;
import fun.qu_an.minecraft.asyncparticles.client.particle.render.IParticleRenderer;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.Spliterator;

@Mixin(value = ParticleEngine.class, priority = 600)
public abstract class MixinParticleEngine_SplitTick {
	@Unique
	private static final List<ParticleRenderType> PARALLELLED_RENDER_TYPES = List.of(
		ParticleRenderType.TERRAIN_SHEET,
		ParticleRenderType.PARTICLE_SHEET_OPAQUE,
		ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT,
		ParticleRenderType.PARTICLE_SHEET_LIT,
		ParticleRenderType.NO_RENDER
	);

	@Shadow
	protected abstract void tickParticleList(Collection<Particle> particles);

	@TargetHandler(
		mixin = "fun.qu_an.minecraft.asyncparticles.client.mixin.core.tick.MixinParticleEngine",
		name = "asyncparticles$scheduleGpuParticleTick"
	)
	@Inject(method = "@MixinSquared:Handler", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z"))
	private void redirectScheduleGpuTick(ParticleRenderType particleRenderType,
										 Queue<?> queue,
										 CallbackInfo ci,
										 @Local IParticleRenderer renderer) {
		AsyncTickBehavior.PARTICLE_OPERATIONS.add(() -> {
			if (ConfigHelper.isTickAsync() &&
				PARALLELLED_RENDER_TYPES.contains(particleRenderType)) {
				asyncparticles$splitTickParticleList((Queue) queue);
			} else {
				tickParticleList((Queue) queue);
			}
			AsyncTickBehavior.doRemoveIf((Queue) queue);
			renderer.tick(GpuParticleBehavior.getCameraPos(), (Queue) queue);
		});
	}

	@TargetHandler(
		mixin = "fun.qu_an.minecraft.asyncparticles.client.mixin.core.tick.MixinParticleEngine",
		name = "asyncparticles$scheduleGpuParticleTick"
	)
	@Redirect(method = "@MixinSquared:Handler", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z"))
	private boolean redirectScheduleGpuTick(List<?> instance, Object e) {
		return true;
	}

	@TargetHandler(
		mixin = "fun.qu_an.minecraft.asyncparticles.client.mixin.core.tick.MixinParticleEngine",
		name = "asyncparticles$scheduleParticleTick"
	)
	@Inject(method = "@MixinSquared:Handler", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z"))
	private void redirectScheduleTick(ParticleRenderType particleRenderType,
									  Queue<?> queue,
									  CallbackInfo ci) {
		AsyncTickBehavior.PARTICLE_OPERATIONS.add(() -> {
			if (ConfigHelper.isTickAsync() &&
				PARALLELLED_RENDER_TYPES.contains(particleRenderType)) {
				asyncparticles$splitTickParticleList((Queue) queue);
			} else {
				tickParticleList((Queue) queue);
			}
		});
	}

	@TargetHandler(
		mixin = "fun.qu_an.minecraft.asyncparticles.client.mixin.core.tick.MixinParticleEngine",
		name = "asyncparticles$scheduleParticleTick"
	)
	@Redirect(method = "@MixinSquared:Handler", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z"))
	private boolean redirectScheduleTick(List<?> instance, Object e) {
		return true;
	}

	@Unique
	private void asyncparticles$splitTickParticleList(Collection<Particle> collection) {
		ThreadUtil.assertNotMainThread();
		Spliterator<Particle> spliterator = collection.spliterator();
		new TickParticleRecursiveAction(spliterator).compute();
	}
}
