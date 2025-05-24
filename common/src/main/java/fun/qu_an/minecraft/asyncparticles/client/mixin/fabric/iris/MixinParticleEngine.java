package fun.qu_an.minecraft.asyncparticles.client.mixin.fabric.iris;

import com.google.common.collect.ImmutableList;
import fun.qu_an.minecraft.asyncparticles.client.compat.iris.IrisParticleEngineAddon;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import org.spongepowered.asm.mixin.*;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.util.List;

@Mixin(ParticleEngine.class)
public class MixinParticleEngine implements IrisParticleEngineAddon {
	@SuppressWarnings({"unused", "MissingUnique"})
	private static List<ParticleRenderType> OPAQUE_PARTICLE_RENDER_TYPES;

	@Override
	public void asyncparticle$addOpaqueRenderType(ParticleRenderType particleRenderType) {
		try {
			Field field = ParticleEngine.class.getDeclaredField("OPAQUE_PARTICLE_RENDER_TYPES");
			if (!field.trySetAccessible()) {
				throw new RuntimeException("Failed to make field accessible");
			}
			field.set(null, ImmutableList.<ParticleRenderType>builder()
				.addAll(OPAQUE_PARTICLE_RENDER_TYPES)
				.add(particleRenderType)
				.build());
		} catch (NoSuchFieldException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
}
