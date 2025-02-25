package fun.qu_an.minecraft.asyncparticles.client.fabric;

import net.fabricmc.loader.api.FabricLoader;

@SuppressWarnings("unused")
public class ModListHelperImpl {
	public static boolean isForge() {
		return false;
	}

	public static boolean isModLoaded(String modId) {
		return FabricLoader.getInstance().isModLoaded(modId);
	}
}
