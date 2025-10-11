package fun.qu_an.minecraft.asyncparticles.client.compat;

import dev.architectury.injectables.annotations.ExpectPlatform;

public class Mappings {
	@ExpectPlatform
	public static String getTickParticlesMethod() {
		throw new AssertionError();
	}
}
