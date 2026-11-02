package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.particlestorm;

import fun.qu_an.minecraft.asyncparticles.client.compat.particlestorm.ParticleStormParticle;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.TextureSheetParticle;
import org.mesdag.particlestorm.particle.MolangParticleInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = MolangParticleInstance.class, remap = false)
public abstract class MixinMolangParticleInstance extends TextureSheetParticle implements ParticleStormParticle {
	@Shadow protected float[] billboardSize;
	@Shadow protected float xRot;
	@Shadow protected float yRot;
	@Shadow protected float xRotO;
	@Shadow protected float yRotO;
	@Unique private long asyncparticles$gpuGeneration;

	protected MixinMolangParticleInstance(ClientLevel level, double x, double y, double z) {
		super(level, x, y, z);
	}

	@Override
	public long asyncparticles$getGpuGeneration() {
		return asyncparticles$gpuGeneration;
	}

	@Override
	public void asyncparticles$setGpuGeneration(long generation) {
		asyncparticles$gpuGeneration = generation;
	}

	@Override
	public float[] asyncparticles$getBillboardSize() {
		return billboardSize;
	}

	@Override
	public float asyncparticles$getXRot() {
		return xRot;
	}

	@Override
	public float asyncparticles$getYRot() {
		return yRot;
	}

	@Override
	public float asyncparticles$getXRotO() {
		return xRotO;
	}

	@Override
	public float asyncparticles$getYRotO() {
		return yRotO;
	}

	@Override
	public int asyncparticles$getParticleStormLight(float partialTickTime) {
		try {
			return getLightColor(partialTickTime);
		} catch (Exception ignore) {
			return 0;
		}
	}
}
