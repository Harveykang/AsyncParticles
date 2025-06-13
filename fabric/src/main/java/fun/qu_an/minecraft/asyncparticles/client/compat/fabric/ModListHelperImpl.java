package fun.qu_an.minecraft.asyncparticles.client.compat.fabric;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;

import java.util.Optional;

@SuppressWarnings("unused")
public class ModListHelperImpl {
	public static boolean isForge() {
		return false;
	}

	public static boolean isModLoaded(String modId) {
		return FabricLoader.getInstance().isModLoaded(modId);
	}

	public static boolean isDevelopmentEnvironment() {
		return FabricLoader.getInstance().isDevelopmentEnvironment();
	}

	public static boolean isClient() {
		return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
	}

	/**
	 * Basically from <a href="https://github.com/Moulberry/MixinConstraints">MixinConstraints</a>
	 */
	public static boolean versionCheck(String modId, String minInclusive, String maxExclusive) {
		Optional<Version> optional = FabricLoader.getInstance().getModContainer(modId)
			.map(container -> container.getMetadata()
				.getVersion());
		if (optional.isEmpty()) {
			return false;
		}
		Version currentVersion = optional.get();
		Version min, max;
		try {
			min = minInclusive == null ? null : Version.parse(minInclusive);
		} catch (VersionParsingException e) {
			throw new IllegalArgumentException("Invalid version minInclusive", e);
		}
		try {
			max = maxExclusive == null ? null : Version.parse(maxExclusive);
		} catch (VersionParsingException e) {
			throw new IllegalArgumentException("Invalid version maxExclusive", e);
		}
		if (min != null && max != null && min.compareTo(max) >= 0) {
			throw new IllegalArgumentException("Invalid version range: minInclusive > maxExclusive");
		}
		return (min == null || currentVersion.compareTo(min) >= 0) && (max == null || currentVersion.compareTo(max) < 0);
	}

	public static String versionToString(String modId) {
		return FabricLoader.getInstance().getModContainer(modId)
			.map(container -> container.getMetadata()
				.getVersion().getFriendlyString())
			.orElseThrow(() -> new IllegalArgumentException("Mod " + modId + " is not loaded."));
	}
}
