package fun.qu_an.minecraft.asyncparticles.client.compat.neoforge;

import fun.qu_an.minecraft.asyncparticles.client.compat.Mappings;

public class MappingsImpl implements Mappings.IMappings {
	public String getTickParticlesMethod() {
		return "tickParticles";
	}

	public String getRenderMethod() {
		return "extract";
	}

	public String getRenderRotatedQuadMethod1() {
		return "extractRotatedQuad";
	}

	public String getRenderRotatedQuadMethod2() {
		return "extractRotatedQuad";
	}
}
