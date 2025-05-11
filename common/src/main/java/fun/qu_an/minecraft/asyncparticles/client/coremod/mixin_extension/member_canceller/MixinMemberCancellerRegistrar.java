package fun.qu_an.minecraft.asyncparticles.client.coremod.mixin_extension.member_canceller;

/**
 * These codes are from my fork of MixinSquared.<p>
 * <a href="https://github.com/Harveykang/MixinSquared">https://github.com/Harveykang/MixinSquared</a><p>
 * APIs may be removed or change frequently before pull request.
 */
@SuppressWarnings("unused")
public class MixinMemberCancellerRegistrar {
    /**
     * Registers a MixinMemberCanceller to be used by the ExtensionCancelMixinMember.
     * @param canceller The MixinMemberCanceller to register.
     */
    public static void register(MixinMemberCanceller canceller) {
        ExtensionMemberCancelApplication.CANCELLERS.add(canceller);
        ExtensionMemberCancelApplication.LOGGER.debug("Registered mixin member canceller {}", canceller.getClass().getName());
    }
}
