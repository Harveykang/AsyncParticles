package fun.qu_an.minecraft.asyncparticles.client.compat.immersive_portals;

import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.IComputeReuseCondition;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

record ImmPtlComputeReuseCondition(
	ClientLevel level,
	Vec3 position,
	Vector3f leftVector,
	Vector3f upVector,
	float partialTicks
) implements IComputeReuseCondition {
	@Override
	public boolean matches(Camera camera, float partialTicks) {
		return level == Minecraft.getInstance().level
			&& this.position.equals(camera.getPosition())
			&& camera.getLeftVector().equals(leftVector)
			&& camera.getUpVector().equals(upVector)
			&& this.partialTicks == partialTicks;
	}

	static IComputeReuseCondition of(ClientLevel level, Camera camera, float partialTicks) {
		Vector3f leftVector = camera.getLeftVector();
		Vector3f upVector = camera.getUpVector();
		return new ImmPtlComputeReuseCondition(
			level,
			camera.getPosition(),
			new Vector3f(leftVector),
			new Vector3f(upVector),
			partialTicks);
	}
}
