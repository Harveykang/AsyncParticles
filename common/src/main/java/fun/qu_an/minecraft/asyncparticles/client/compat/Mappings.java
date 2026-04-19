package fun.qu_an.minecraft.asyncparticles.client.compat;

import dev.architectury.injectables.annotations.ExpectPlatform;

public class Mappings {
	@ExpectPlatform
	public static String getRenderMethod() {
		throw new AssertionError();
	}

	@ExpectPlatform
	public static String getFireworkSparkClass() {
		throw new AssertionError();
	}
}
