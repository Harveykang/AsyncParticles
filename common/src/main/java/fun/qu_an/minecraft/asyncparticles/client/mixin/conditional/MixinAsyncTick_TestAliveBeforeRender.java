package fun.qu_an.minecraft.asyncparticles.client.mixin.conditional;

import fun.qu_an.minecraft.asyncparticles.client.util.IterationSafeEvictingQueue;
import net.minecraft.client.particle.ElderGuardianParticleGroup;
import net.minecraft.client.particle.ItemPickupParticleGroup;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Iterator;
import java.util.Queue;
import java.util.function.Consumer;
import java.util.stream.Stream;

@Mixin({
	ItemPickupParticleGroup.class,
	ElderGuardianParticleGroup.class
})
public class MixinAsyncTick_TestAliveBeforeRender {
	@Dynamic
	@Group(name = "checkAliveBeforeRender", min = 1)
	@Redirect(method = "*", require = 0, at = @At(value = "INVOKE", target = "Ljava/util/Queue;stream()Ljava/util/stream/Stream;"))
	private static <T extends Particle> Stream<T> redirectStream(Queue<T> queue) {
		if (queue instanceof IterationSafeEvictingQueue<T>) {
			return queue.stream().filter(Particle::isAlive);
		} else {
			return queue.stream();
		}
	}

	@Dynamic
	@Group(name = "checkAliveBeforeRender", min = 1)
	@Redirect(method = "*", require = 0, at = @At(value = "INVOKE", target = "Ljava/util/Queue;forEach(Ljava/util/function/Consumer;)V"))
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
	@Redirect(method = "*", require = 0, at = @At(value = "INVOKE", target = "Ljava/util/Queue;iterator()Ljava/util/Iterator;"))
	private static <T extends Particle> Iterator<T> redirectIterator(Queue<T> queue) {
		if (queue instanceof IterationSafeEvictingQueue<T> iseq) {
			return iseq.conditionalIterator(Particle::isAlive);
		} else {
			return queue.iterator();
		}
	}
}
