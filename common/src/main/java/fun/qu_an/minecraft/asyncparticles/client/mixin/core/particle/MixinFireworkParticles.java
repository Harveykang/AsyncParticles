package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle;

import fun.qu_an.minecraft.asyncparticles.client.addon.GpuParticleAddon;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.FireworkParticles;
import net.minecraft.client.particle.TextureSheetParticle;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

public class MixinFireworkParticles {
	@Mixin(targets = "net.minecraft.client.particle.FireworkParticles.SparkParticle")
	public static abstract class SparkParticle extends TextureSheetParticle implements GpuParticleAddon {
		@Shadow
		private boolean flicker;

		protected SparkParticle(ClientLevel level, double x, double y, double z) {
			super(level, x, y, z);
		}

		@Override
		public boolean asyncparticles$shouldRender() {
			return !this.flicker || this.age < this.lifetime / 3 || (this.age + this.lifetime) / 3 % 2 == 0;
		}
	}

	@Mixin(FireworkParticles.OverlayParticle.class)
	public static abstract class OverlayParticle extends TextureSheetParticle implements GpuParticleAddon {
		protected OverlayParticle(ClientLevel level, double x, double y, double z) {
			super(level, x, y, z);
		}

		@Override
		public void asyncparticles$postTick(long address) {
   			float alpha = 0.6F - this.age * 0.125f;
			this.setAlpha(alpha);
			MemoryUtil.memPutByte(address + oCOLOR_ALPHA_OFFSET, (byte) (0.6f * 255f - (this.age - 1) * (0.125f * 255f)));
			MemoryUtil.memPutByte(address + COLOR_ALPHA_OFFSET, (byte) (alpha * 255f));
		}
	}
}
