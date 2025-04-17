package fun.qu_an.minecraft.asyncparticles.client.mixin.physicsmod;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.qu_an.minecraft.asyncparticles.client.addon.ParticleAddon;
import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import fun.qu_an.minecraft.asyncparticles.client.compat.physicsmod.PhysicsModCompat;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import net.diebuddies.minecraft.weather.FastTextureSheetParticle;
import net.diebuddies.minecraft.weather.RainParticle;
import net.diebuddies.minecraft.weather.WeatherParticle;
import net.diebuddies.physics.ocean.OceanWorld;
import net.diebuddies.physics.snow.math.AABB3D;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WeatherParticle.class)
public abstract class MixinWeatherParticle implements ParticleAddon {
	@Shadow(remap = false)
	protected AABB3D aabb;

	@Redirect(method = "tick", at = @At(value = "INVOKE", remap = false, target = "Lnet/diebuddies/physics/ocean/OceanWorld;spawnRainRipple(IFDDD)V"))
	private void onSpawnRainRipple(OceanWorld instance, int lifetime, float scale, double x, double y, double z) {
		if (RenderSystem.isOnRenderThread()){
			instance.spawnRainRipple(lifetime, scale, x, y, z);
		} else {
			ThreadUtil.enqueueClientTask(() -> instance.spawnRainRipple(lifetime, scale, x, y, z));
		}
	}

	@Override
	public @NotNull AABB getRenderBoundingBox(float partialTicks) {
		Vector3d min = aabb.getMin();
		Vector3d max = aabb.getMax();
		return new AABB(min.x, min.y, min.z, max.x, max.y, max.z);
	}
}
