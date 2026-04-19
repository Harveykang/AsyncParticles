package fun.qu_an.minecraft.asyncparticles.client.compat.physicsmod;

import fun.qu_an.minecraft.asyncparticles.client.compat.create.CreateUtil;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.config.RainEffect;
import fun.qu_an.minecraft.asyncparticles.client.compat.create.CollisionType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class PhysicsModCompat {
	public static CollisionType isCollideWithContraption(ClientLevel level, Vec3 movement, AABB aabb) {
		return CreateUtil.isCollideWithContraption(level, movement, aabb, true);
	}

	public static void onContraptionCollide(ClientLevel level, Vec3 location, Vec3 movement, CollisionType collisionType) {
		RainEffect createRainEffect;
		if ((createRainEffect = ConfigHelper.getCreateRainEffect()) == RainEffect.NONE ||
			level.random.nextFloat() > 0.1) {
			return;
		}
		BlockHitResult hit = CreateUtil.clip(level, location, location.add(movement.scale(2)));
		if (hit == null || hit.getType() != HitResult.Type.BLOCK || hit.isInside() ||
			!collisionType.canSpawnRainEffect(createRainEffect)) {
			return;
		}
		Vec3 spawnPos = hit.location;
		Minecraft.getInstance().level.addParticle(
			ParticleTypes.RAIN,
			spawnPos.x,
			spawnPos.y,
			spawnPos.z,
			0,
			0,
			0);
	}
}
