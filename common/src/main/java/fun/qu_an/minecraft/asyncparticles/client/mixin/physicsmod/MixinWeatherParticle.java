package fun.qu_an.minecraft.asyncparticles.client.mixin.physicsmod;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import net.diebuddies.minecraft.weather.FastTextureSheetParticle;
import net.diebuddies.minecraft.weather.WeatherParticle;
import net.diebuddies.physics.ocean.OceanWorld;
import net.diebuddies.physics.snow.math.AABB3D;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(WeatherParticle.class)
public abstract class MixinWeatherParticle extends FastTextureSheetParticle {
	@Shadow(remap = false)
	protected AABB3D aabb;

	protected MixinWeatherParticle(ClientLevel clientLevel, double d, double e, double f) {
		super(clientLevel, d, e, f);
	}

	@Redirect(method = "tick", at = @At(value = "INVOKE", remap = false, target = "Lnet/diebuddies/physics/ocean/OceanWorld;spawnRainRipple(IFDDD)V"))
	private void onSpawnRainRipple(OceanWorld instance, int lifetime, float scale, double x, double y, double z) {
		if (ThreadUtil.isOnMainThread()){
			instance.spawnRainRipple(lifetime, scale, x, y, z);
		} else {
			ThreadUtil.enqueueClientTask(() -> instance.spawnRainRipple(lifetime, scale, x, y, z));
		}
	}

	@Override
	public @NotNull AABB getBoundingBox() {
		if (aabb == null) {
			return INITIAL_AABB;
		}
		Vector3d min = aabb.getMin();
		Vector3d max = aabb.getMax();
		return new AABB(min.x, min.y, min.z, max.x, max.y, max.z);
	}
}
