package fun.qu_an.minecraft.asyncparticles.client.particle.fabric;

import net.fabricmc.loader.api.FabricLoader;

@SuppressWarnings("unused")
public class GpuParticlesImpl {
	public static String getRenderMethod() {
		return FabricLoader.getInstance().getMappingResolver().mapMethodName(
			"intermediary",
			"net.minecraft.class_703",
			"method_3074",
			"(Lnet/minecraft/class_4588;Lnet/minecraft/class_4184;F)V"
		);
	}
}
