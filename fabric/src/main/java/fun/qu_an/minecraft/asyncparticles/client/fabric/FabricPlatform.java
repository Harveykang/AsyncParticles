package fun.qu_an.minecraft.asyncparticles.client.fabric;

import fun.qu_an.minecraft.asyncparticles.client.Platform;
import fun.qu_an.minecraft.asyncparticles.client.compat.fabric.ModListHelperImpl;
import fun.qu_an.minecraft.asyncparticles.client.core.fabric.GameUtilImpl;
import net.minecraft.world.phys.AABB;

@SuppressWarnings("unused")
public class FabricPlatform implements Platform {
	public boolean isForge() {
		return false;
	}

	public boolean isClient() {
		return ModListHelperImpl.isClient();
	}

	public boolean isModLoaded(String modId) {
		return ModListHelperImpl.isModLoaded(modId);
	}

	@Override
	public boolean isForgeModLoaded(String modId) {
		return false;
	}

	@Override
	public boolean isFabricModLoaded(String modId) {
		return isModLoaded(modId);
	}

	public boolean versionCheck(String modId, String minInclusive, String maxExclusive) {
		return ModListHelperImpl.versionCheck(modId, minInclusive, maxExclusive);
	}

	public String versionToString(String modId) {
		return ModListHelperImpl.versionToString(modId);
	}

	public boolean isDevelopmentEnvironment() {
		return ModListHelperImpl.isDevelopmentEnvironment();
	}

	@Override
	public AABB infinityAABB() {
		return GameUtilImpl.infinityAABB();
	}
}
