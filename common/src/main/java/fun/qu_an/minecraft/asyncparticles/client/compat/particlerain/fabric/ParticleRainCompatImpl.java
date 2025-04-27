package fun.qu_an.minecraft.asyncparticles.client.compat.particlerain.fabric;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class ParticleRainCompatImpl {
	public static void onCreateCollision(@NotNull ClientLevel level, Vec3 originalMotion, @NotNull Vec3 clipMotion, @NotNull AABB aabb) {
		throw new UnsupportedOperationException();
	}
}
