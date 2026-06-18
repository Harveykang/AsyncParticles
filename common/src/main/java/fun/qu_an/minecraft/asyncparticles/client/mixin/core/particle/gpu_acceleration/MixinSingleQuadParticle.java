package fun.qu_an.minecraft.asyncparticles.client.mixin.core.particle.gpu_acceleration;

import fun.qu_an.minecraft.asyncparticles.client.addon.GpuParticleAddon;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.util.ARGB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(SingleQuadParticle.class)
public abstract class MixinSingleQuadParticle extends Particle implements GpuParticleAddon {
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

	@Override
	public void asyncparticles$postTick(long address) {
		// no-op
	}

	@Override
	public boolean asyncparticles$shouldRender() {
		return true;
	}

	public float asyncparticles$getQuadSize(float deltaPartialTick) {
		return getQuadSize(deltaPartialTick);
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

	public int asyncparticles$getLightCoords(float deltaPartialTick) {
		return getLightCoords(deltaPartialTick);
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
		return ARGB.color( // ABGR
			(int) (alpha * 255.0f),
			(int) (bCol * 255.0f),
			(int) (gCol * 255.0f),
			(int) (rCol * 255.0f));
	}

	public int asyncparticles$getColor(int oColor) {
		return oColor;
	}
}
