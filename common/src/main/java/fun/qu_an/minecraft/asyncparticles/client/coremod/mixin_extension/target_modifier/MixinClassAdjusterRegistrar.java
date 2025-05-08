package fun.qu_an.minecraft.asyncparticles.client.coremod.mixin_extension.target_modifier;

import fun.qu_an.minecraft.asyncparticles.client.coremod.PreLaunch;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.service.MixinService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@PreLaunch
public class MixinClassAdjusterRegistrar {
	private static final ILogger LOGGER = MixinService.getService().getLogger("mixinsquared");
	static Map<String, MixinClassAdjuster> pendingAdjusters = new HashMap<>();
	static List<MixinClassProvider> pendingProviders = new ArrayList<>();

	public static void register(MixinClassAdjuster mixinClassAdjuster) {
		if (pendingAdjusters == null) {
			throw new IllegalStateException("Cannot register class adjuster after pre-launch!");
		}
		pendingAdjusters.put(mixinClassAdjuster.getMixinClassName(), mixinClassAdjuster);
		LOGGER.debug("Registered target modifier {}", mixinClassAdjuster.getClass().getName());
	}

	public static void register(MixinClassProvider mixinClassProvider) {
		if (pendingProviders == null) {
			throw new IllegalStateException("Cannot register class provider after pre-launch!");
		}
		pendingProviders.add(mixinClassProvider);
		LOGGER.debug("Registered target modifier {}", mixinClassProvider.getClass().getName());
	}

	static Map<String, MixinClassAdjuster> endAdjusters() {
		Map<String, MixinClassAdjuster> m = pendingAdjusters;
		pendingAdjusters = null;
		return m;
	}

	static List<MixinClassProvider> endProviders() {
		List<MixinClassProvider> l = pendingProviders;
		pendingProviders = null;
		return l;
	}
}
