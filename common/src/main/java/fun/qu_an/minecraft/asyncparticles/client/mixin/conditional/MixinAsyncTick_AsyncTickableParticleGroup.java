package fun.qu_an.minecraft.asyncparticles.client.mixin.conditional;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import fun.qu_an.minecraft.asyncparticles.client.addon.AsyncTickableParticleGroup;
import fun.qu_an.minecraft.asyncparticles.client.addon.LightCachedParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.GpuParticleBehavior;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.tick.AsyncTickBehavior;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.tick.AsyncTickParticleGroupBehavior;
import fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.tick.MixinParticleGroup;
import fun.qu_an.minecraft.asyncparticles.client.util.IterationSafeEvictingQueue;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceSet;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.ParticleGroupRenderState;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Iterator;
import java.util.Queue;
import java.util.function.Consumer;
import java.util.stream.Stream;

@Mixin({
	QuadParticleGroup.class,
	ItemPickupParticleGroup.class,
	ElderGuardianParticleGroup.class
})
public abstract class MixinAsyncTick_AsyncTickableParticleGroup extends MixinParticleGroup implements AsyncTickableParticleGroup {
	@SuppressWarnings("ConstantValue")
	@Unique
	private final boolean asyncparticles$canTickAsync = ConfigHelper.isAsyncTickParticle()
		&& AsyncTickParticleGroupBehavior.canTickAsync((ParticleGroup<?>) (Object) this);
	@Unique
	private ReferenceSet<Particle> asyncparticles$syncParticles;
	@Unique
	private ReferenceSet<Particle> asyncparticles$syncGpuParticles;

	@Inject(method = "extractRenderState", require = 0, at = @At(value = "HEAD"))
	private static void injectExtra(Frustum frustum,
	                                Camera camera,
	                                float partialTickTime,
	                                CallbackInfoReturnable<ParticleGroupRenderState> cir,
	                                @Share("originalPartialTick") LocalFloatRef originalPartialTick) {
		originalPartialTick.set(partialTickTime);
	}

	@Dynamic
	@Coerce
	@ModifyExpressionValue(method = "extractRenderState", require = 0, at = @At(value = "INVOKE", target = "Ljava/util/Iterator;next()Ljava/lang/Object;"))
	private static Object wrapNext(@Coerce Object original,
	                               @Share("originalPartialTick") LocalFloatRef originalPartialTick,
	                               @Local(argsOnly = true, ordinal = 0) LocalFloatRef partialTickTime) {
		if (((ParticleAddon) original).asyncparticles$isTicked()) {
			partialTickTime.set(originalPartialTick.get());
		} else {
			partialTickTime.set(originalPartialTick.get() + 1f);
		}
		return original;
	}

	@Override
	public void asyncparticles$addSync(Particle particle) {
		if (GpuParticleBehavior.getInstance().canRenderFast(particle)) {
			if (asyncparticles$syncGpuParticles == null) {
				asyncparticles$syncGpuParticles = new ReferenceOpenHashSet<>();
			}
			asyncparticles$syncGpuParticles.add(particle);
		} else {
			if (asyncparticles$syncParticles == null) {
				asyncparticles$syncParticles = new ReferenceOpenHashSet<>();
			}
			asyncparticles$syncParticles.add(particle);
		}
	}

	@Dynamic
	@Group(name = "checkAliveBeforeRender", min = 1)
	@Redirect(method = "extractRenderState", require = 0, at = @At(value = "INVOKE", target = "Ljava/util/Queue;stream()Ljava/util/stream/Stream;"))
	private static <T extends Particle> Stream<T> redirectStream(Queue<T> queue) {
		if (queue instanceof IterationSafeEvictingQueue<T>) {
			return queue.stream().filter(Particle::isAlive);
		} else {
			return queue.stream();
		}
	}

	@Dynamic
	@Group(name = "checkAliveBeforeRender", min = 1)
	@Redirect(method = "extractRenderState", require = 0, at = @At(value = "INVOKE", target = "Ljava/util/Queue;forEach(Ljava/util/function/Consumer;)V"))
	private static <T extends Particle> void redirectForEach(Queue<T> queue, Consumer<T> consumer) {
		if (queue instanceof IterationSafeEvictingQueue<T>) {
			for (T t : queue) {
				if (t.isAlive()) {
					consumer.accept(t);
				}
			}
		} else {
			queue.forEach(consumer);
		}
	}

	@Dynamic
	@Group(name = "checkAliveBeforeRender", min = 1)
	@Redirect(method = "extractRenderState", require = 0, at = @At(value = "INVOKE", target = "Ljava/util/Queue;iterator()Ljava/util/Iterator;"))
	private static <T extends Particle> Iterator<T> redirectIterator(Queue<T> queue) {
		if (queue instanceof IterationSafeEvictingQueue<T> iseq) {
			return iseq.conditionalIterator(Particle::isAlive);
		} else {
			return queue.iterator();
		}
	}

	@Override
	public boolean asyncparticles$canTickAsync() {
		return asyncparticles$canTickAsync;
	}

	@Override
	public void asyncparticles$tickSyncParticles(boolean isGpu) {
		ReferenceSet<Particle> syncParticles = isGpu ? asyncparticles$syncGpuParticles : asyncparticles$syncParticles;
		if (syncParticles == null || syncParticles.isEmpty()) {
			return;
		}
		Iterator<Particle> iterator = syncParticles.iterator();
		while (iterator.hasNext()) {
			Particle particle = iterator.next();
			if (!particle.isAlive()) {
				// we manage the count in cleanup task
				iterator.remove();
				continue;
			}
			try {
				tickParticle(particle);
				if (!(particle instanceof TrackingEmitter)) {
					((LightCachedParticleAddon) particle).asyncparticles$tickLightCache();
					((ParticleAddon) particle).asyncparticles$setTicked();
				}
			} catch (Throwable e) {
				throw AsyncTickBehavior.getInstance().getExceptionHandler().constructCrashReport(particle, e);
			}
		}
	}

	@Override
	public boolean asyncparticles$isSyncParticle(Particle particle) {
		return (asyncparticles$syncParticles != null && asyncparticles$syncParticles.contains(particle))
			|| (asyncparticles$syncGpuParticles != null && asyncparticles$syncGpuParticles.contains(particle));
	}
}
