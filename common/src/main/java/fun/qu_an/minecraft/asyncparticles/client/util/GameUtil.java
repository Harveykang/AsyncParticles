package fun.qu_an.minecraft.asyncparticles.client.util;

import fun.qu_an.minecraft.asyncparticles.client.AsyncParticlesClient;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import static java.lang.Math.abs;

public class GameUtil {
	public static ResourceLocation id(String path) {
		return new ResourceLocation(AsyncParticlesClient.MOD_ID, path);
	}

	public static double manhattanLength(Vec3 vec3) {
		return abs(vec3.x) + abs(vec3.y) + abs(vec3.z);
	}
}
