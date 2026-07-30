package fun.qu_an.minecraft.asyncparticles.client.compat;

import fun.qu_an.minecraft.asyncparticles.client.Platform;

public class Mappings {
	public static String getTickParticlesMethod() {
		return Platform.PLATFORM.getMappings().getTickParticlesMethod();
	}

	public static String getRenderMethod() {
		return Platform.PLATFORM.getMappings().getRenderMethod();
	}

	public static String getRenderRotatedQuadMethod1() {
		return Platform.PLATFORM.getMappings().getRenderRotatedQuadMethod1();
	}

	public static String getRenderRotatedQuadMethod2() {
		return Platform.PLATFORM.getMappings().getRenderRotatedQuadMethod2();
	}

	public interface IMappings {
		String getTickParticlesMethod();

		String getRenderMethod();

		String getRenderRotatedQuadMethod1();

		String getRenderRotatedQuadMethod2();
	}
}
