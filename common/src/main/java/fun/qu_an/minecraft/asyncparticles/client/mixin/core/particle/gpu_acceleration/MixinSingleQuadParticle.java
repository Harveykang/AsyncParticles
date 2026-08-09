package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.gpu_acceleration;

import fun.qu_an.minecraft.asyncparticles.client.addon.GpuParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.util.ARGB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(SingleQuadParticle.class)
public abstract class MixinSingleQuadParticle extends Particle implements GpuParticleAddon, ParticleAddon {
	@Unique
	private boolean asyncparticles$isGpuLightGot = false;

	protected MixinSingleQuadParticle(ClientLevel level, double x, double y, double z) {
		super(level, x, y, z);
	}

	@Shadow
	public abstract float getQuadSize(float a);

	@Shadow
	public abstract float getU0();

	@Shadow
	public abstract float getV0();

	@Shadow
	public abstract float getU1();

	@Shadow
	public abstract float getV1();

	@Shadow
	public float oRoll;

	@Shadow
	public float roll;

	@Shadow
	public float alpha;

	@Shadow
	public float bCol;

	@Shadow
	public float gCol;

	@Shadow
	public float rCol;

	@Shadow
	public abstract SingleQuadParticle.Layer getLayer();

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
		return ARGB.color( // ABGR
			(int) (alpha * 255.0f),
			(int) (bCol * 255.0f),
			(int) (gCol * 255.0f),
			(int) (rCol * 255.0f));
	}

	@Override
	public int asyncparticles$getColor(int oColor) {
		return oColor;
	}

	@Override
	public SingleQuadParticle.Layer asyncparticles$getLayer() {
		return getLayer();
	}

	public float asyncparticles$getAlpha() {
		return alpha;
	}

	public float asyncparticles$getRed() {
		return rCol;
	}

	public float asyncparticles$getGreen() {
		return gCol;
	}

	public float asyncparticles$getBlue() {
		return bCol;
	}
}
