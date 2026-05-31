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
	public static String getRenderMethod() {
		return FabricLoader.getInstance().getMappingResolver().mapMethodName(
			"intermediary",
			"net.minecraft.class_703",
			"method_3074",
			"(Lnet/minecraft/class_4588;Lnet/minecraft/class_4184;F)V"
		);
	}

	public static String getRenderRotatedQuadMethod1() {
		return FabricLoader.getInstance().getMappingResolver().mapMethodName(
			"intermediary",
			"net.minecraft.class_3940",
			"method_60373",
			"(Lnet/minecraft/class_4588;Lnet/minecraft/class_4184;Lorg/joml/Quaternionf;F)V"
		);
	}

	public static String getRenderRotatedQuadMethod2() {
		return FabricLoader.getInstance().getMappingResolver().mapMethodName(
			"intermediary",
			"net.minecraft.class_3940",
			"method_60374",
			"(Lnet/minecraft/class_4588;Lorg/joml/Quaternionf;FFFF)V"
		);
	}

	public static String getFireworkSparkClass() {
		return FabricLoader.getInstance().getMappingResolver().mapClassName(
			"intermediary",
			"net.minecraft.class_677$class_680"
		);
	}
}
