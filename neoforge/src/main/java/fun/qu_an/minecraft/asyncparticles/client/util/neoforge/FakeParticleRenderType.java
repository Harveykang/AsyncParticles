package fun.qu_an.minecraft.asyncparticles.client.util.neoforge;

import net.minecraft.client.particle.ParticleRenderType;

public class FakeParticleRenderType {
	public static final ParticleRenderType OPAQUE = new ParticleRenderType("opaque", null, false);
	public static final ParticleRenderType TRANSLUCENT = new ParticleRenderType("opaque", null, true);
}
