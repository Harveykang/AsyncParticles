package fun.qu_an.minecraft.asyncparticles.client;

import com.chailotl.particular.Main;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.fabricmc.loader.api.FabricLoader;
import org.sinytra.connector.loader.ConnectorEarlyLoader;

public class ModListHelper {
	public static final boolean IS_FORGE = isForge();
	public static final boolean FABRIC_API_LOADED = isModLoaded("fabric");
	public static final boolean CONNECTORMOD_LOADED = isModLoaded("connectormod");
	/* Valkyrien Skies */
	public static final boolean VS_LOADED = isModLoaded("valkyrienskies");
	/* Sodium */
	public static final boolean SODIUM_LOADED = isModLoaded("sodium") || isModLoaded("embeddium");
	/* IRIS */
	public static final boolean IRIS_LOADED = isModLoaded("iris");
	/* Dummmmmmy */
	public static final boolean DUMMMMMMY_LOADED = isModLoaded("dummmmmmy");
	/* Effectual */
	public static final boolean FABRIC_EFFECTUAL_LOADED = isFabricModLoaded("effectual");
	/* Flerovium */
	public static final boolean FORGE_FLEROVIUM_LOADED = isForgeModLoaded("flerovium");
	/* Effective */
	public static final boolean FORGE_EFFECTIVE_LOADED = isForgeModLoaded("effective");
	public static final boolean FABRIC_EFFECTIVE_LOADED = isFabricModLoaded("effective");
	/* Lodestone */
	public static final boolean LODESTONE_LOADED = isModLoaded("lodestone");
	/* Particle Rain */
	public static final boolean FABRIC_PARTICLERAIN_LOADED = isFabricModLoaded("particlerain");
	public static final boolean FORGE_PARTICLERAIN_LOADED = isForgeModLoaded("particlerain");
	/* Flywheel */
	public static final boolean FLYWHEEL_LOADED = isModLoaded("flywheel");
	/* Create */
	public static final boolean FABRIC_CREATE_LOADED = isFabricModLoaded("create");
	public static final boolean FORGE_CREATE_LOADED = isForgeModLoaded("create");
	/* Tombstone */
	public static final boolean TOMBSTONE_LOADED = isModLoaded("tombstone");
	/* Hexcasting */
	public static final boolean HEXCASTING_LOADED = isModLoaded("hexcasting");
	/* Enhanced Block Entities */
	public static final boolean ENHANCEDBLOCKENTITIES_LOADED = isModLoaded("enhancedblockentities");
	/* Particular */
	public static final boolean FABRIC_PARTICULAR_LOADED = isFabricModLoaded("particular");

	@ExpectPlatform
	private static boolean isForge() {
		throw new AssertionError();
	}

	@ExpectPlatform
	public static boolean isModLoaded(String modId) {
		throw new AssertionError();
	}

	@SuppressWarnings("ConstantValue")
	public static boolean isForgeModLoaded(String modId) {
		return IS_FORGE && isModLoaded(modId) && (!CONNECTORMOD_LOADED || !ConnectorEarlyLoader.isConnectorMod(modId));
	}

	public static boolean isFabricModLoaded(String modId) {
		return IS_FORGE ? CONNECTORMOD_LOADED && ConnectorEarlyLoader.isConnectorMod(modId) : isModLoaded(modId);
	}
}
