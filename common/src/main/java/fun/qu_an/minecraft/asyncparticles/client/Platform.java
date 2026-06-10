package fun.qu_an.minecraft.asyncparticles.client;

import net.minecraft.world.phys.AABB;

public interface Platform {
	Platform PLATFORM = createPlatform();

	private static Platform createPlatform() {
		try {
			Class<?> c;
			try {
				c = Class.forName("net.neoforged.neoforge.common.NeoForge");
			} catch (ClassNotFoundException ignored) {
				c = null;
			}
			Class<?> c1;
			if (c != null) {
				c1 = Class.forName("fun.qu_an.minecraft.asyncparticles.client.neoforge.NeoForgePlatform");
			} else {
				c1 = Class.forName("fun.qu_an.minecraft.asyncparticles.client.fabric.FabricPlatform");
			}
			return (Platform) c1.getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	boolean isForge();

	boolean isClient();

	boolean isModLoaded(String modId);

	boolean isForgeModLoaded(String modId);

	boolean isFabricModLoaded(String modId);

	boolean versionCheck(String modId, String minInclusive, String maxExclusive);

	String versionToString(String modId);

	boolean isDevelopmentEnvironment();

	AABB infinityAABB();
}
