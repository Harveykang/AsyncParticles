package fun.qu_an.minecraft.asyncparticles.client.coremod;

import static fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper.IS_FORGE;

public class MixinUtil {
	public static String getRefMapperName(String className, String refMapper) {
		return IS_FORGE ? null : className.startsWith("fabric") ? "fabric-" + refMapper : refMapper;
	}
}
