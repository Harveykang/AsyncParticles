package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle;

import fun.qu_an.minecraft.asyncparticles.client.addon.GpuParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.FastColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(TextureSheetParticle.class)
public abstract class MixinTextureSheetParticle extends SingleQuadParticle implements GpuParticleAddon, ParticleAddon {
	protected MixinTextureSheetParticle(ClientLevel clientLevel, double d, double e, double f) {
		super(clientLevel, d, e, f);
	}

	@Shadow
	protected abstract float getU0();

	@Shadow
	protected abstract float getV0();

	@Shadow
	protected abstract float getU1();

	@Shadow
	protected abstract float getV1();

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
		if (asyncparticles$isFirstGpuLightGet()) {
			// A workaround for level light update latency
			asyncparticles$setGpuLightGot();
			return asyncparticles$getCachedLight();
		}
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
			(int) (alpha * 255.0f) & 255,
			(int) (bCol * 255.0f) & 255,
			(int) (gCol * 255.0f) & 255,
			(int) (rCol * 255.0f) & 255);
	}

	public int asyncparticles$getColor(int oColor) {
		return oColor;
	}
}
