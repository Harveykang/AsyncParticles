package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle;

import fun.qu_an.minecraft.asyncparticles.client.addon.GpuParticleAddon;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.DustColorTransitionParticle;
import net.minecraft.client.particle.TextureSheetParticle;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(DustColorTransitionParticle.class)
public abstract class MixinDustColorTransitionParticle extends TextureSheetParticle implements GpuParticleAddon {
	@Shadow
	protected abstract void lerpColors(float partialTick);

	protected MixinDustColorTransitionParticle(ClientLevel level, double x, double y, double z) {
		super(level, x, y, z);
	}

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
