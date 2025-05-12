package fun.qu_an.minecraft.asyncparticles.client.coremod.mixin_extension.target_modifier;

import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.service.MixinService;

import java.util.*;

/**
 * These codes are from my fork of MixinSquared.<p>
 * <a href="https://github.com/Harveykang/MixinSquared">https://github.com/Harveykang/MixinSquared</a><p>
 * APIs may be removed or change frequently before pull requests are merged.
 */
public class MixinClassAdjusterRegistrar {
	private static final ILogger LOGGER = MixinService.getService().getLogger("mixinsquared");
	private static Map<String, MixinClassAdjuster> pendingAdjusters = new HashMap<>();
	private static Set<MixinClassProvider> pendingProviders = new HashSet<>();

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

	static Set<MixinClassProvider> endProviders() {
		Set<MixinClassProvider> l = pendingProviders;
		pendingProviders = null;
		return l;
	}
}
