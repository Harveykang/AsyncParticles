package fun.qu_an.minecraft.asyncparticles.client.mixin.conditional;

import net.minecraft.world.level.levelgen.LegacyRandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.concurrent.atomic.AtomicLong;

@Mixin(LegacyRandomSource.class)
public class MixinLegacyRandomSource {
	@Redirect(method = {"setSeed", "next"}, at = @At(value = "INVOKE", target = "Ljava/util/concurrent/atomic/AtomicLong;compareAndSet(JJ)Z"))
	public boolean safeify(AtomicLong instance, long expectedValue, long newValue) {
		instance.accumulateAndGet(newValue, (prev, l1) -> l1);
		return true;
	}
}
