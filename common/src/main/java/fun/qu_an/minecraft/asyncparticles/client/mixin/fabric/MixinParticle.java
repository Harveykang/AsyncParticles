package fun.qu_an.minecraft.asyncparticles.client.mixin.fabric;

import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import net.minecraft.client.particle.Particle;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.*;

@Mixin(Particle.class)
public abstract class MixinParticle implements ParticleAddon {
	@Shadow public abstract AABB getBoundingBox();

	// NeoForge has implemented this method.
	// So we need only to implement for fabric.
	@Override
	public @NotNull AABB getRenderBoundingBox(float partialTicks) {
		return this.getBoundingBox().inflate(1.0);
	}
}
