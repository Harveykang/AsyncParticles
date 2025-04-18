package fun.qu_an.minecraft.asyncparticles.client.util;

import net.minecraft.world.phys.Vec3;

public record CollisionResult(Vec3 clipMotion, Vec3 contactPointMotion) {
}
