package fun.qu_an.minecraft.asyncparticles.client.compat.neoforge;


import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.neoforgespi.language.IModFileInfo;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.Restriction;

@SuppressWarnings("unused")
public class ModListHelperImpl {
	public static boolean isForge() {
		return true;
	}

	public static boolean isClient() {
		return FMLLoader.getDist().isClient();
	}

	public static boolean isModLoaded(String modId) {
		return FMLLoader.getLoadingModList().getModFileById(modId) != null;
	}

	/**
	 * Basically from <a href="https://github.com/Moulberry/MixinConstraints">MixinConstraints</a>
	 */
	public static boolean versionCheck(String modId, String minInclusive, String maxExclusive) {
		IModFileInfo info = LoadingModList.get().getModFileById(modId);
		String version = info == null || info.getMods().isEmpty() ? null : info.versionString();
		if (version == null) {
			throw new IllegalArgumentException("Mod " + modId + " is not loaded.");
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
}
