package fun.qu_an.minecraft.asyncparticles.client.coremod;

public class MixinUtil {
	public static String getRefMapperName(String className, String refMapper) {
		if (className.startsWith("forge")) {
			return "forge-" + refMapper;
		}
		if (className.startsWith("fabric")) {
			return "fabric-" + refMapper;
		}
		return refMapper;
	}
}
