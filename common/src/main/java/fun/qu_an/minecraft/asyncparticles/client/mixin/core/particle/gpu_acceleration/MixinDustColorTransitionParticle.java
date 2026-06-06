package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.gpu_acceleration;

import fun.qu_an.minecraft.asyncparticles.client.addon.GpuParticleAddon;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.DustColorTransitionParticle;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
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

	@Override
	public void asyncparticles$postTick(long address) {
		lerpColors(0f);
		long ptr = address + oCOLOR_OFFSET;
		MemoryUtil.memPutByte(ptr, (byte) (rCol * 255f));
		ptr += 1;
		MemoryUtil.memPutByte(ptr, (byte) (gCol * 255f));
		ptr += 1;
		MemoryUtil.memPutByte(ptr, (byte) (bCol * 255f));

		lerpColors(1f);
		ptr = address + COLOR_OFFSET;
		MemoryUtil.memPutByte(ptr, (byte) (rCol * 255f));
		ptr += 1;
		MemoryUtil.memPutByte(ptr, (byte) (gCol * 255f));
		ptr += 1;
		MemoryUtil.memPutByte(ptr, (byte) (bCol * 255f));
	}
}
