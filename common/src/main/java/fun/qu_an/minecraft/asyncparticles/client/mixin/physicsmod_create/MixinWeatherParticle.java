package fun.qu_an.minecraft.asyncparticles.client.mixin.physicsmod_create;

import fun.qu_an.minecraft.asyncparticles.client.compat.physicsmod.PhysicsModCompat;
import net.diebuddies.minecraft.weather.FastTextureSheetParticle;
import net.diebuddies.minecraft.weather.RainParticle;
import net.diebuddies.minecraft.weather.WeatherParticle;
import net.diebuddies.physics.snow.math.AABB3D;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WeatherParticle.class)
public abstract class MixinWeatherParticle extends FastTextureSheetParticle {
	@Shadow(remap = false)
	protected AABB3D aabb;

	protected MixinWeatherParticle(ClientLevel clientLevel, double d, double e, double f) {
		super(clientLevel, d, e, f);
	}

	@Inject(method = "tick",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"),
		cancellable = true)
	private void onTick(CallbackInfo ci) {
		Vec3 movement = new Vec3(xd, yd, zd);
		if (PhysicsModCompat.isCollideWithContraptions(level, movement, aabb)) {
			if ((Object) this instanceof RainParticle) {
				PhysicsModCompat.onContraptionCollision(level, movement, aabb);
			}
			remove();
			ci.cancel();
		}
	}
}
