package fun.qu_an.minecraft.asyncparticles.client.mixin.core.off_thread_access;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LegacyRandomSource.class)
public class MixinLegacyRandomSource {
	@ModifyExpressionValue(method = {"setSeed", "next"}, at = @At(value = "INVOKE", target = "Ljava/util/concurrent/atomic/AtomicLong;compareAndSet(JJ)Z"))
	public boolean safeify(boolean original) {
		return ThreadUtil.isOnParticleThread() || original;
	}
}
