package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.gpu_acceleration;

import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.IParticleBatchKey;
import net.minecraft.client.particle.ParticleRenderType;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ParticleRenderType.class)
public interface MixinParticleRenderType extends IParticleBatchKey {
}
