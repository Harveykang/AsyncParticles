package fun.qu_an.minecraft.asyncparticles.client.core;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.qu_an.minecraft.asyncparticles.client.util.ParticleThreadLocal;

public class AnimateTickBehavior {
	public static final ParticleThreadLocal<Boolean> CULL_UNDERWATER_PARTICLE_TYPE = ParticleThreadLocal.withInitial(RenderSystem::isOnRenderThread, () -> false);
}
