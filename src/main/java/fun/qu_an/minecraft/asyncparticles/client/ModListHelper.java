package fun.qu_an.minecraft.asyncparticles.client;

import com.google.common.base.Suppliers;
import net.fabricmc.loader.api.FabricLoader;

import java.util.function.Supplier;

public class ModListHelper {
	public static final boolean IS_FORGE = isModLoaded("connectormod");
	public static final boolean VS_LOADED = isModLoaded("valkyrienskies");
	public static final boolean SODIUM_LOADED = isModLoaded("sodium") || isModLoaded("embeddium");
	public static final boolean PARTICLERAIN_LOADED = isModLoaded("particlerain");

	private static boolean isModLoaded(String modId) {
		return FabricLoader.getInstance().isModLoaded(modId);
	}
}
