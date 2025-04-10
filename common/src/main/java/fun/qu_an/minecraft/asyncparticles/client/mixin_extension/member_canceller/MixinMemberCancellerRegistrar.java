package fun.qu_an.minecraft.asyncparticles.client.mixin_extension.member_canceller;


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
