package fun.qu_an.minecraft.asyncparticles.client.mixin.fabric.vulkanmod;

import fun.qu_an.minecraft.asyncparticles.client.mixin.conditional.MixinParticle_LightCache;
import net.minecraft.client.particle.SingleQuadParticle;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = SingleQuadParticle.class, priority = 1500)
public abstract class MixinSingleQuadParticle extends MixinParticle_LightCache {
	@Override
	public int getLightColor(float partialTick) {
		return super.getLightColor(partialTick); // override vulkan mod's injection
	}
}
