package fun.qu_an.minecraft.asyncparticles.client.compat.fabric;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.MappingResolver;

@SuppressWarnings("unused")
public class MappingsImpl {
	public static String getRenderMethod() {
		return FabricLoader.getInstance().getMappingResolver().mapMethodName(
			"intermediary",
			"net.minecraft.class_703",
			"method_3074",
			"(Lnet/minecraft/class_4588;Lnet/minecraft/class_4184;F)V"
		);
	}

	public static String getFireworkSparkClass() {
		return FabricLoader.getInstance().getMappingResolver().mapClassName(
			"intermediary",
			"net.minecraft.class_677$class_680"
		);
	}
}
