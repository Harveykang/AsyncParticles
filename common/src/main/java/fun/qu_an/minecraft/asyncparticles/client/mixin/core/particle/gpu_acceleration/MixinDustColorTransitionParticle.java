package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.gpu_acceleration;

import fun.qu_an.minecraft.asyncparticles.client.addon.GpuParticleAddon;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.DustColorTransitionParticle;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ARGB;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(DustColorTransitionParticle.class)
public abstract class MixinDustColorTransitionParticle extends SingleQuadParticle implements GpuParticleAddon {
	protected MixinDustColorTransitionParticle(ClientLevel level, double x, double y, double z, TextureAtlasSprite sprite) {
		super(level, x, y, z, sprite);
	}

	@Shadow
	protected abstract void lerpColors(float partialTickTime);

	public int asyncparticles$getOColor() {
		lerpColors(0f);
		return ARGB.color( // ABGR
			(int) (alpha * 255.0f),
			(int) (bCol * 255.0f),
			(int) (gCol * 255.0f),
			(int) (rCol * 255.0f));
	}

	public int asyncparticles$getColor(int oColor) {
		lerpColors(1f);
		return ARGB.color( // ABGR
			oColor >>> 24,
			(int) (bCol * 255.0f),
			(int) (gCol * 255.0f),
			(int) (rCol * 255.0f));
	}
}
