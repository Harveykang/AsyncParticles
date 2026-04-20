package fun.qu_an.minecraft.asyncparticles.client.mixin.core.tick;

import fun.qu_an.minecraft.asyncparticles.client.particle.GpuParticleBehavior;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {
	static {
		GpuParticleBehavior.INSTANCE.init();
	}
}
