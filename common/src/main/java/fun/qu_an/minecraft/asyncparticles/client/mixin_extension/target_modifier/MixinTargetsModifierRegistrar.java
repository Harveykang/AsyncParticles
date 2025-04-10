package fun.qu_an.minecraft.asyncparticles.client.mixin_extension.target_modifier;

import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.service.MixinService;

public class MixinTargetsModifierRegistrar {
    private static final ILogger LOGGER = MixinService.getService().getLogger("mixinsquared");

    public static void register(MixinTargetModifier mixinTargetModifier) {
        MixinTargetsModifierApplication.MODIFIERS.put(mixinTargetModifier.getMixinClassName(), mixinTargetModifier);
        LOGGER.debug("Registered target modifier {}", mixinTargetModifier.getClass().getName());
    }
}
