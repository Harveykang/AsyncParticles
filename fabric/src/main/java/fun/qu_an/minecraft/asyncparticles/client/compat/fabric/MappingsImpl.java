package fun.qu_an.minecraft.asyncparticles.client.compat.fabric;

import fun.qu_an.minecraft.asyncparticles.client.compat.Mappings;
import net.fabricmc.loader.api.FabricLoader;

@SuppressWarnings("unused")
public class MappingsImpl implements Mappings.IMappings {
	public String getTickParticlesMethod() {
		return FabricLoader.getInstance().getMappingResolver().mapMethodName(
			"intermediary",
			"net.minecraft.class_11938",
			"method_74287",
			"()V"
		);
	}

	// iku	net/minecraft/class_11944	net/minecraft/client/renderer/state/QuadParticleRenderState
	// ger	net/minecraft/class_4184	net/minecraft/client/Camera
	public String getRenderMethod() {
		return FabricLoader.getInstance().getMappingResolver().mapMethodName(
			"intermediary",
			"net.minecraft.class_3940",
			"method_3074",
			"(Lnet/minecraft/class_11944;Lnet/minecraft/class_4184;F)V"
		);
	}

	public String getRenderRotatedQuadMethod1() {
		return FabricLoader.getInstance().getMappingResolver().mapMethodName(
			"intermediary",
			"net.minecraft.class_3940",
			"method_60373",
			"(Lnet/minecraft/class_11944;Lnet/minecraft/class_4184;Lorg/joml/Quaternionf;F)V"
		);
	}

	public String getRenderRotatedQuadMethod2() {
		return FabricLoader.getInstance().getMappingResolver().mapMethodName(
			"intermediary",
			"net.minecraft.class_3940",
			"method_60375",
			"(Lnet/minecraft/class_11944;Lorg/joml/Quaternionf;FFFF)V"
		);
	}
}
