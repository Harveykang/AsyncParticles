package fun.qu_an.minecraft.asyncparticles.client.compat.fabric;

import net.fabricmc.loader.api.FabricLoader;

@SuppressWarnings("unused")
public class MappingsImpl {
	public static String getTickParticlesMethod() {
		return FabricLoader.getInstance().getMappingResolver().mapMethodName(
			"intermediary",
			"net.minecraft.class_11938",
			"method_74287",
			"()V"
		);
	}
}
