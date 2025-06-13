package fun.qu_an.minecraft.asyncparticles.client.compat.forge;

import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.LoadingModList;
import net.minecraftforge.forgespi.language.IModFileInfo;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.Restriction;

@SuppressWarnings("unused")
public class ModListHelperImpl {
	public static boolean isForge() {
		return true;
	}

	public static boolean isModLoaded(String modId) {
		return FMLLoader.getLoadingModList().getModFileById(modId) != null;
	}

	public static boolean isDevelopmentEnvironment() {
		return !FMLLoader.isProduction();
	}

	public static boolean isClient() {
		return FMLLoader.getDist().isClient();
	}

	/**
	 * Basically from <a href="https://github.com/Moulberry/MixinConstraints">MixinConstraints</a>
	 */
	public static boolean versionCheck(String modId, String minInclusive, String maxExclusive) {
		IModFileInfo info = LoadingModList.get().getModFileById(modId);
		String version = info == null || info.getMods().isEmpty() ? null : info.versionString();
		if (version == null) {
			return false;
		}
		ArtifactVersion currentVersion = new DefaultArtifactVersion(version);
		ArtifactVersion min, max;
		try {
			min = minInclusive == null ? null : new DefaultArtifactVersion(minInclusive);
		} catch (Exception e) {
			throw new IllegalArgumentException("Invalid version minInclusive", e);
		}
		try {
			max = maxExclusive == null ? null : new DefaultArtifactVersion(maxExclusive);
		} catch (Exception e) {
			throw new IllegalArgumentException("Invalid version maxExclusive", e);
		}
		if (min != null && max != null && min.compareTo(max) >= 0) {
			throw new IllegalArgumentException("Invalid version range: minInclusive > maxExclusive");
		}
		return new Restriction(min, true, max, false).containsVersion(currentVersion);
	}

	public static String versionToString(String modId) {
		IModFileInfo info = LoadingModList.get().getModFileById(modId);
		if (info == null || info.getMods().isEmpty()) {
			throw new IllegalArgumentException("Mod " + modId + " is not loaded.");
		}
		return info.versionString();
	}
}
