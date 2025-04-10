package fun.qu_an.minecraft.asyncparticles.client.mixin_extension.member_canceller;

import java.util.List;

public interface MixinMemberCanceller {
    /**
     * Pre-test method to check if the canceller should be applied to the given mixin class.
     * @return true if the canceller should be tested for the given mixin class, false otherwise
     */
    boolean preCancel(List<String> targetClassNames, String mixinClassName);

    /**
     * Prevents the certain mixin method from being applied to the target classes.
     * This method is called before shouldCancelField().
     * @return true if the given method should be cancelled, false otherwise
     */
    boolean shouldCancelMethod(List<String> targetClassNames,
                               String mixinClassName,
                               List<String> targetMethodDescs,
                               String mixinMethodName,
                               String mixinMethodDesc);

    /**
     * Prevents the certain mixin field from being applied to the target classes.
     * This method is called after shouldCancelMethod().
     * Make sure that when this method returns true, the mixin methods accessing the field have also been cancelled.
     * Note: Currently, we can only cancel fields that are not be accessed by Opcodes.GETFIELD or Opcodes.GETSTATIC.
     * @return true if the given field should be cancelled, false otherwise
     */
    default boolean shouldCancelField(List<String> targetClassNames,
                                      String mixinClassName,
                                      String mixinFieldName,
                                      String mixinFieldDesc) {
        return false;
    }
}
