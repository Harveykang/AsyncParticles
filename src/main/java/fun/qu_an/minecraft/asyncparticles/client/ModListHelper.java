package fun.qu_an.minecraft.asyncparticles.client;

import net.fabricmc.loader.api.FabricLoader;
import org.sinytra.connector.loader.ConnectorEarlyLoader;

public class ModListHelper {
	public static final boolean IS_FORGE = isModLoaded("connectormod");
	public static final boolean VS_LOADED = isModLoaded("valkyrienskies");
	public static final boolean SODIUM_LOADED = isModLoaded("sodium") || isModLoaded("embeddium");
	public static final boolean PARTICLERAIN_LOADED = isModLoaded("particlerain");
	public static final boolean IRIS_LOADED = isModLoaded("iris");
	public static final boolean DUMMMMMMY_LOADED = isModLoaded("dummmmmmy");
	public static final boolean EFFECTUAL_LOADED = isModLoaded("effectual");
	public static final boolean CREATE_LOADED = isModLoaded("create");
	public static final boolean FABRIC_API_LOADED = isModLoaded("fabric");
	public static final boolean FORGE_FLEROVIUM_LOADED = isModLoaded("flerovium");
	public static final boolean EFFECTIVE_LOADED = isModLoaded("effective");
	public static final boolean FORGE_EFFECTIVE_LOADED = isForgeModLoaded("effective");
	public static final boolean FABRIC_EFFECTIVE_LOADED = isFabricModLoaded("effective");
	public static final boolean LODESTONE_LOADED = isModLoaded("lodestone");
	public static final boolean FORGE_LODESTONE_LOADED = isForgeModLoaded("lodestone");
	public static final boolean FABRIC_LODESTONE_LOADED = isFabricModLoaded("lodestone");
	public static final boolean FABRIC_PARTICLERAIN_LOADED = isFabricModLoaded("particlerain");
	public static final boolean FORGE_PARTICLERAIN_LOADED = isForgeModLoaded("particlerain");
	public static final boolean FABRIC_CREATE_LOADED = isFabricModLoaded("create");
	public static final boolean FORGE_CREATE_LOADED = isForgeModLoaded("create");
	public static final boolean FABRIC_EFFECTUAL_LOADED = isFabricModLoaded("effectual");
	public static final boolean TOMBSTONE_LOADED = isModLoaded("tombstone");
	public static final boolean HEXCASTING_LOADED = isModLoaded("hexcasting");
	public static final boolean FLYWHEEL_LOADED = isModLoaded("flywheel");

	public static boolean isModLoaded(String modId) {
		return FabricLoader.getInstance().isModLoaded(modId);
	}

	public static boolean isForgeModLoaded(String modId) {
		return IS_FORGE && isModLoaded(modId) && !ConnectorEarlyLoader.isConnectorMod(modId);
	}

	public static boolean isFabricModLoaded(String modId) {
		return IS_FORGE ? ConnectorEarlyLoader.isConnectorMod(modId) : isModLoaded(modId);
	}
}
