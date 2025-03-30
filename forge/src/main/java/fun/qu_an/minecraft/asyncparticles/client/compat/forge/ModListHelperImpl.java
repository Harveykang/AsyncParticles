package fun.qu_an.minecraft.asyncparticles.client.compat.forge;

import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.moddiscovery.ModFileInfo;

@SuppressWarnings("unused")
public class ModListHelperImpl {
	public static boolean isForge() {
		return true;
	}

	public static boolean isModLoaded(String modId) {
		return FMLLoader.getLoadingModList().getModFileById(modId) != null;
	}

	public static int versionMajor(String modId) {
		ModFileInfo modFileById = FMLLoader.getLoadingModList().getModFileById(modId);
		if (modFileById == null) {
			return -1;
		}
		return modFileById.getMods().get(0).getVersion().getMajorVersion();
	}

	public static boolean isDevelopmentEnvironment() {
		return !FMLLoader.isProduction();
	}

	public static boolean isClient() {
		return FMLLoader.getDist().isClient();
	}
}
