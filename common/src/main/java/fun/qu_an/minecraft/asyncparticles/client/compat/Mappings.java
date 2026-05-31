package fun.qu_an.minecraft.asyncparticles.client.compat;

public class Mappings {
	public static String getTickParticlesMethod() {
		return "tickParticles";
	}

	public static String getRenderMethod() {
		return "extract";
	}

	public static String getRenderRotatedQuadMethod1() {
		return "extractRotatedQuad";
	}

	public static String getRenderRotatedQuadMethod2() {
		return "extractRotatedQuad";
	}

	public static String getFireworkSparkClass() {
		return "net.minecraft.client.particle.FireworkParticles$SparkParticle";
	}
}
