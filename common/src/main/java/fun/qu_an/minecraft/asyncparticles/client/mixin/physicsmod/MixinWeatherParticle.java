package fun.qu_an.minecraft.asyncparticles.client.mixin.physicsmod;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import fun.qu_an.minecraft.asyncparticles.client.compat.physicsmod.PhysicsModCompat;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import net.diebuddies.minecraft.weather.FastTextureSheetParticle;
import net.diebuddies.minecraft.weather.RainParticle;
import net.diebuddies.minecraft.weather.WeatherParticle;
import net.diebuddies.physics.ocean.OceanWorld;
import net.diebuddies.physics.snow.math.AABB3D;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
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
		if (ModListHelper.VS_LOADED &&
			PhysicsModCompat.isCollideWithShip(level, xd, yd, zd, aabb)) {
			if ((Object) this instanceof RainParticle) {
				PhysicsModCompat.onShipCollide(level, new Vec3(x, y, z), new Vec3(xd, yd, zd));
			}
			remove();
			ci.cancel();
		}
	}

	@Redirect(method = "tick", at = @At(value = "INVOKE", remap = false, target = "Lnet/diebuddies/physics/ocean/OceanWorld;spawnRainRipple(IFDDD)V"))
	private void onSpawnRainRipple(OceanWorld instance, int lifetime, float scale, double x, double y, double z) {
		if (RenderSystem.isOnRenderThread()){
			instance.spawnRainRipple(lifetime, scale, x, y, z);
		} else {
			ThreadUtil.enqueueClientTask(() -> instance.spawnRainRipple(lifetime, scale, x, y, z));
		}
	}
}
