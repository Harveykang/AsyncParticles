package fun.qu_an.minecraft.asyncparticles.client;

import net.fabricmc.loader.api.FabricLoader;

public class ModListHelper {
	public static final boolean IS_FORGE = isModLoaded("connectormod");
	public static final boolean VS_LOADED = isModLoaded("valkyrienskies");
	public static final boolean SODIUM_LOADED = isModLoaded("sodium") || isModLoaded("embeddium");
	public static final boolean PARTICLERAIN_LOADED = isModLoaded("particlerain");
	public static final boolean IRIS_LOADED = isModLoaded("iris");
	public static final boolean DUMMMMMMY_LOADED = isModLoaded("dummmmmmy");
	public static final boolean EFFECTUAL_LOADED = isModLoaded("effectual");

	private static boolean isModLoaded(String modId) {
		return FabricLoader.getInstance().isModLoaded(modId);
	}
}
