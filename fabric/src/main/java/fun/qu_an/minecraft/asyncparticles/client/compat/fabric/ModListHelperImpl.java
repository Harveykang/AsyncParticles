package fun.qu_an.minecraft.asyncparticles.client.compat.fabric;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.api.Version;

@SuppressWarnings("unused")
public class ModListHelperImpl {
	public static boolean isForge() {
		return false;
	}

	public static boolean isModLoaded(String modId) {
		return FabricLoader.getInstance().isModLoaded(modId);
	}

	public static int versionMajor(String modId) {
		return FabricLoader.getInstance().getModContainer(modId).map(modContainer -> {
			Version version = modContainer.getMetadata().getVersion();
			if (version instanceof SemanticVersion) {
				return ((SemanticVersion) version).getVersionComponent(0);
			}
			try {
				return Integer.parseInt(version.getFriendlyString().split("\\.")[0].replaceAll("[^0-9]", ""));
			} catch (Exception e) {
				return -1;
			}
		}).orElse(-1);
	}

	public static boolean isDevelopmentEnvironment() {
		return FabricLoader.getInstance().isDevelopmentEnvironment();
	}
}
