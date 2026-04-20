package fun.qu_an.minecraft.asyncparticles.client.mixin.neoforge.create;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalDoubleRef;
import fun.qu_an.minecraft.asyncparticles.client.compat.create.neoforge.CollideUtil;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Particle.class)
public class MixinParticle {
	@Shadow
	@Final
	public ClientLevel level;

	@Shadow
	private AABB bb;

	@Shadow
	protected boolean hasPhysics;

	/**
	 * See {@link fun.qu_an.minecraft.asyncparticles.client.mixin.vs2.MixinParticle#collideBoundingBox}
	 * See {@link fun.qu_an.minecraft.asyncparticles.client.mixin.neoforge.weather2_vs.MixinEntityRotFX#collideBoundingBox}
	 */
	@Inject(method = "move", at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
		target = "Lnet/minecraft/client/particle/Particle;stoppedByCollision:Z"))
	private void collideBoundingBox(CallbackInfo ci,
	                                @Local(ordinal = 0) LocalDoubleRef d,
	                                @Local(ordinal = 1) LocalDoubleRef e,
	                                @Local(ordinal = 2) LocalDoubleRef f) {
		if (!hasPhysics) {
			return;
		}
		Vec3 mov = CollideUtil.collideMotionWithContraptions(level, new Vec3(d.get(), e.get(), f.get()), bb);
		if (mov == null) {
			return;
		}
		d.set(mov.x);
		e.set(mov.y);
		f.set(mov.z);
	}
}
