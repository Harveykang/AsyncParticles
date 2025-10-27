package fun.qu_an.minecraft.asyncparticles.client.mixin.neoforge.prettyrain_vs;

import com.leclowndu93150.particlerain.particle.RippleParticle;
import fun.qu_an.minecraft.asyncparticles.client.compat.particlerain.v3.RippleParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.mixin.neoforge.prettyrain.MixinWeatherParticle;
import net.minecraft.client.multiplayer.ClientLevel;
import org.joml.AxisAngle4d;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = RippleParticle.class)
public abstract class MixinRippleParticle extends MixinWeatherParticle implements RippleParticleAddon {
	@Unique
	private Vector3f asyncparticles$normal;

	protected MixinRippleParticle(ClientLevel level, double x, double y, double z) {
		super(level, x, y, z);
	}

	@Redirect(method = "render", at = @At(value = "NEW", remap = false, target = "(Lorg/joml/AxisAngle4d;)Lorg/joml/Quaternionf;"))
	private Quaternionf redirectNewQuaternionf(AxisAngle4d axisAngle) {
		Vector3f normal = this.asyncparticles$normal;
		if (normal == null) {
			return new Quaternionf(axisAngle);
		}
		return new Quaternionf().rotateTo(0, 0, 1, normal.x, normal.y, normal.z);
	}

	@Override
	public void asycncparticles$setNormal(Vector3f normal) {
		this.asyncparticles$normal = normal;
	}
}
