package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.gpu_acceleration;

import fun.qu_an.minecraft.asyncparticles.client.addon.GpuParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.FastColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(TextureSheetParticle.class)
public abstract class MixinTextureSheetParticle extends SingleQuadParticle implements GpuParticleAddon, ParticleAddon {
	@Unique
	private boolean asyncparticles$isGpuLightGot = false;

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

	@Override
	public float asyncparticles$getQuadSize(float partialTickTime) {
		return getQuadSize(partialTickTime);
	}

	@Override
	public float asyncparticles$getU0() {
		return getU0();
	}

	@Override
	public float asyncparticles$getV0() {
		return getV0();
	}

	@Override
	public float asyncparticles$getU1() {
		return getU1();
	}

	@Override
	public float asyncparticles$getV1() {
		return getV1();
	}

	@Override
	public int asyncparticles$getGpuLightCoords(float partialTickTime) {
		if (!asyncparticles$isGpuLightGot) {
			// A workaround for level light update latency
			asyncparticles$isGpuLightGot = true;
			return asyncparticles$getCachedLight();
		}
		try {
			return getLightColor(partialTickTime);
		} catch (Exception ignore) {
			return 0;
		}
	}

	@Override
	public double asyncparticles$getXo() {
		return xo;
	}

	@Override
	public double asyncparticles$getYo() {
		return yo;
	}

	@Override
	public double asyncparticles$getZo() {
		return zo;
	}

	@Override
	public double asyncparticles$getX() {
		return x;
	}

	@Override
	public double asyncparticles$getY() {
		return y;
	}

	@Override
	public double asyncparticles$getZ() {
		return z;
	}

	@Override
	public float asyncparticles$getORoll() {
		return oRoll;
	}

	@Override
	public float asyncparticles$getRoll() {
		return roll;
	}

	@Override
	public int asyncparticles$getOColor() {
		return FastColor.ABGR32.color( // ABGR
			(int) (alpha * 255.0f) & 255,
			(int) (bCol * 255.0f) & 255,
			(int) (gCol * 255.0f) & 255,
			(int) (rCol * 255.0f) & 255);
	}

	@Override
	public int asyncparticles$getColor(int oColor) {
		return oColor;
	}

	@Override
	public float asyncparticles$getAlpha() {
		return alpha;
	}

	@Override
	public float asyncparticles$getRed() {
		return rCol;
	}

	@Override
	public float asyncparticles$getGreen() {
		return gCol;
	}

	@Override
	public float asyncparticles$getBlue() {
		return bCol;
	}
}
