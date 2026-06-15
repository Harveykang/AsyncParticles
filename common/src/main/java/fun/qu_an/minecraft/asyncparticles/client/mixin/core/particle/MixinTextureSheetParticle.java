package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle;

import fun.qu_an.minecraft.asyncparticles.client.addon.GpuParticleAddon;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.FastColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(TextureSheetParticle.class)
public abstract class MixinTextureSheetParticle extends SingleQuadParticle implements GpuParticleAddon {
	protected MixinTextureSheetParticle(ClientLevel clientLevel, double d, double e, double f) {
		super(clientLevel, d, e, f);
	}

	@Shadow
	public abstract float getU0();

	@Shadow
	public abstract float getV0();

	@Shadow
	public abstract float getU1();

	@Shadow
	public abstract float getV1();

	@Override
	public void asyncparticles$postTick(long address) {
		// no-op
	}

	@Override
	public boolean asyncparticles$shouldRender() {
		return true;
	}

	public float asyncparticles$getQuadSize(float partialTickTime) {
		return getQuadSize(partialTickTime);
	}

	public float asyncparticles$getU0() {
		return getU0();
	}

	public float asyncparticles$getV0() {
		return getV0();
	}

	public float asyncparticles$getU1() {
		return getU1();
	}

	public float asyncparticles$getV1() {
		return getV1();
	}

	public int asyncparticles$getLightCoords(float partialTickTime) {
		return getLightColor(partialTickTime);
	}

	public double asyncparticles$getXo() {
		return xo;
	}

	public double asyncparticles$getYo() {
		return yo;
	}

	public double asyncparticles$getZo() {
		return zo;
	}

	public double asyncparticles$getX() {
		return x;
	}

	public double asyncparticles$getY() {
		return y;
	}

	public double asyncparticles$getZ() {
		return z;
	}

	public float asyncparticles$getORoll() {
		return oRoll;
	}

	public float asyncparticles$getRoll() {
		return roll;
	}

	public int asyncparticles$getOColor() {
		return FastColor.ABGR32.color( // ABGR
			(int) (alpha * 255.0f),
			(int) (bCol * 255.0f),
			(int) (gCol * 255.0f),
			(int) (rCol * 255.0f));
	}

	public int asyncparticles$getColor(int oColor) {
		return oColor;
	}
}
