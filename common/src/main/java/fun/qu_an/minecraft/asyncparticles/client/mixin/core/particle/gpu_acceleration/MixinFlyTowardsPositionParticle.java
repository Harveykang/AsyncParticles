package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.gpu_acceleration;

import fun.qu_an.minecraft.asyncparticles.client.addon.GpuParticleAddon;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.FlyTowardsPositionParticle;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ARGB;
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

	public int asyncparticles$getOColor() {
		float alpha = this.lifetimeAlpha.currentAlphaForAge(this.age, this.lifetime, 0f);
		return ARGB.color( // ABGR
			(int) (alpha * 255.0f),
			(int) (bCol * 255.0f),
			(int) (gCol * 255.0f),
			(int) (rCol * 255.0f));
	}

	public int asyncparticles$getColor(int oColor) {
		float alpha = this.lifetimeAlpha.currentAlphaForAge(this.age, this.lifetime, 1f);
		this.setAlpha(alpha);
		return ARGB.color((int) (alpha * 255.0f), oColor);
	}
}
