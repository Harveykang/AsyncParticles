package fun.qu_an.minecraft.asyncparticles.client.coremod.mixin_extension.target_modifier;

import fun.qu_an.minecraft.asyncparticles.client.coremod.PreLaunch;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.service.MixinService;

@PreLaunch
public class MixinTargetsModifierRegistrar {
    private static final ILogger LOGGER = MixinService.getService().getLogger("mixinsquared");

    public static void register(MixinClassAdjuster mixinClassAdjuster) {
        MixinClassAdjusterApplication.ADJUSTERS.put(mixinClassAdjuster.getMixinClassName(), mixinClassAdjuster);
        LOGGER.debug("Registered target modifier {}", mixinClassAdjuster.getClass().getName());
    }
}
