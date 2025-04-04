package fun.qu_an.minecraft.asyncparticles.client.mixin.forge.weather2_create;

import extendedrenderer.particle.entity.EntityRotFX;
import extendedrenderer.particle.entity.ParticleTexExtraRender;
import fun.qu_an.minecraft.asyncparticles.client.compat.create.CreateCompat;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRotFX.class)
public abstract class MixinEntityRotFX extends TextureSheetParticle {
	@Shadow
	public abstract void remove();

	@Shadow(remap = false) public abstract AABB getBoundingBoxForRender(float partialTicks);

	protected MixinEntityRotFX(ClientLevel level, double x, double y, double z) {
		super(level, x, y, z);
	}

	@Inject(method = "spawnAsWeatherEffect", remap = false, at = @At(value = "HEAD"), cancellable = true)
	private void spawnAsWeatherEffect(CallbackInfo ci) {
		if (CreateCompat.isUnderContraption(level, x, y, z)) {
			remove();
			ci.cancel();
		}
	}

	@Inject(method = "move", at = @At("HEAD"), cancellable = true)
	private void collideBoundingBox(double xd, double yd, double zd, CallbackInfo ci) {
		// we do it in another thread, so we don't need to worry about costly collision checks
		AABB boundingBox = getBoundingBoxForRender(0);
		if ((Object) this instanceof ParticleTexExtraRender) {
			double xsize = boundingBox.getXsize();
			double ysize = boundingBox.getYsize();
			double zsize = boundingBox.getZsize();
			boundingBox = boundingBox.inflate(xsize >= 10 ? 0.0 : 10 - xsize, ysize >= 10 ? 0.0 : 10 - ysize, zsize >= 10 ? 0.0 : 10 - zsize);
		}
		boolean b = CreateCompat.isCollideWithContraption(level, new Vec3(x, y, z), new Vec3(xd, yd, zd), boundingBox);
		if (b) {
			remove();
			ci.cancel();
		}
	}
}
