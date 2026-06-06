package fun.qu_an.minecraft.asyncparticles.client.core.particle.tick;

import fun.qu_an.minecraft.asyncparticles.client.compat.Mappings;
import it.unimi.dsi.fastutil.objects.Reference2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.client.particle.ParticleGroup;

import java.util.Set;
import java.util.function.Predicate;

@SuppressWarnings("rawtypes")
public class AsyncTickParticleGroupBehavior {
	public static final String TICK_PARTICLES_METHOD;
	private static final Set<Class<ParticleGroup>> ASYNC_TICKABLE_PARTICLE_GROUP_CLASSES;
	private final static Reference2BooleanOpenHashMap<Class<? extends ParticleGroup>> CAN_TICK_PARTICLES_ASYNC =
		new Reference2BooleanOpenHashMap<>();

	static {
		ASYNC_TICKABLE_PARTICLE_GROUP_CLASSES = new ReferenceOpenHashSet<>(Set.of(ParticleGroup.class));
		TICK_PARTICLES_METHOD = Mappings.getTickParticlesMethod();
	}

	public static boolean canTickAsync(ParticleGroup<?> particleGroup) {
		return CAN_TICK_PARTICLES_ASYNC.computeIfAbsent(particleGroup.getClass(), (Predicate<Class<? extends ParticleGroup>>)
			k -> {
				try {
					Class<?> declaringClass = k.getMethod(TICK_PARTICLES_METHOD).getDeclaringClass();
					return ASYNC_TICKABLE_PARTICLE_GROUP_CLASSES.contains(declaringClass);
				} catch (NoSuchMethodException e) {
					return false;
				}
			});
	}
}
