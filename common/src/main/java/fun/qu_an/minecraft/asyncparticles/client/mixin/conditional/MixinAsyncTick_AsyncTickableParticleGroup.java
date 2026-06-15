package fun.qu_an.minecraft.asyncparticles.client.mixin.conditional;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import fun.qu_an.minecraft.asyncparticles.client.addon.AsyncTickableParticleGroup;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.util.IterationSafeEvictingQueue;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.ParticleGroupRenderState;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collections;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

@Mixin({
	QuadParticleGroup.class,
	ItemPickupParticleGroup.class,
	ElderGuardianParticleGroup.class
})
public abstract class MixinAsyncTick_AsyncTickableParticleGroup implements AsyncTickableParticleGroup {
	@Unique
	private final Set<Particle> asyncparticles$syncParticles = new ReferenceOpenHashSet<>();

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
		if (partialTickTime.get() <= 1.0f && !((ParticleAddon) original).asyncparticles$isTicked()) {
			partialTickTime.set(originalPartialTick.get() + 1f);
		}
		return original;
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
}
