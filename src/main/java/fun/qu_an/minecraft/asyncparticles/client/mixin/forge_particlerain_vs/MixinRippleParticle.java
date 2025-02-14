package fun.qu_an.minecraft.asyncparticles.client.mixin.forge_particlerain_vs;

import fun.qu_an.minecraft.asyncparticles.client.RippleParticleAddon;
import net.minecraft.client.multiplayer.ClientLevel;
import org.joml.AxisAngle4d;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import com.leclowndu93150.particlerain.particle.RippleParticle;

@Mixin(value = RippleParticle.class, remap = false)
public abstract class MixinRippleParticle extends MixinWeatherPatricle implements RippleParticleAddon {
	@Unique
	private Vector3f normal;

	protected MixinRippleParticle(ClientLevel level, double x, double y, double z) {
		super(level, x, y, z);
	}

	@Redirect(method = "m_5744_", at = @At(value = "NEW", target = "(Lorg/joml/AxisAngle4d;)Lorg/joml/Quaternionf;"))
	private Quaternionf redirectNewQuaternionf(AxisAngle4d axisAngle) {
		Vector3f normal = this.normal;
		if (normal == null) {
			return new Quaternionf(axisAngle);
		}
		return new Quaternionf().rotateTo(0, 0, 1, normal.x, normal.y, normal.z);
	}

	@Override
	public void move(double d, double e, double f) {
		// do nothing
	}

	@Override
	public void asyncedParticles$setNormal(Vector3f normal) {
		this.normal = normal;
	}
}
