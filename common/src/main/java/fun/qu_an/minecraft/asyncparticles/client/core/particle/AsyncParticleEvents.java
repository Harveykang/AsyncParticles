package fun.qu_an.minecraft.asyncparticles.client.core.particle;

import fun.qu_an.minecraft.asyncparticles.client.core.particle.async_extract.AsyncRenderBehavior;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.tick.AsyncTickBehavior;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.GpuParticleBehavior;

public class AsyncParticleEvents {
	public static void onClearParticles() {
		AsyncRenderBehavior.getInstance().reset();
		AsyncTickBehavior.getInstance().reset();
		GpuParticleBehavior.INSTANCE.onClearParticles();
	}
}
