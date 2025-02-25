package fun.qu_an.minecraft.asyncparticles.client.forge;

import net.minecraftforge.fml.loading.FMLLoader;

@SuppressWarnings("unused")
public class ModListHelperImpl {
	public static boolean isForge() {
		return true;
	}

	public static boolean isModLoaded(String modId) {
		return FMLLoader.getLoadingModList().getModFileById(modId) != null;
	}
}
