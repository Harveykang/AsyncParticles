package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.gpu_acceleration;

import fun.qu_an.minecraft.asyncparticles.client.addon.GpuParticleAddon;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.FlyTowardsPositionParticle;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(FlyTowardsPositionParticle.class)
public abstract class MixinFlyTowardsPositionParticle extends SingleQuadParticle implements GpuParticleAddon {
	@Shadow
	@Final
	private LifetimeAlpha lifetimeAlpha;

	protected MixinFlyTowardsPositionParticle(ClientLevel level, double x, double y, double z, TextureAtlasSprite sprite) {
		super(level, x, y, z, sprite);
	}

	public void asyncparticles$postTick(long address) {
		this.setAlpha(this.lifetimeAlpha.currentAlphaForAge(this.age, this.lifetime, 0f));
		MemoryUtil.memPutByte(address + oCOLOR_ALPHA_OFFSET, (byte) (alpha * 255f));

		this.setAlpha(this.lifetimeAlpha.currentAlphaForAge(this.age, this.lifetime, 1f));
		MemoryUtil.memPutByte(address + COLOR_ALPHA_OFFSET, (byte) (alpha * 255f));
	}
}
