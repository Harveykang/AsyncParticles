package fun.qu_an.minecraft.asyncparticles.client.mixin.conditional;

import net.minecraft.world.level.levelgen.LegacyRandomSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.atomic.AtomicLong;

@Mixin(LegacyRandomSource.class)
public class MixinLegacyRandomSource {
	@Shadow
	@Final
	private AtomicLong seed;

	@Inject(method = "setSeed", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/ThreadingDetector;makeThreadingException(Ljava/lang/String;Ljava/lang/Thread;)Lnet/minecraft/ReportedException;"),
		cancellable = true)
	public void safeify(long seed, CallbackInfo ci) {
		if (MixinConfigHelper.isSafeLegacyRandomSource()) {
			this.seed.accumulateAndGet(0L, (prev, l1) -> (prev ^ 25214903917L) & 281474976710655L);
			ci.cancel();
		}
	}

	@Inject(method = "next", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/ThreadingDetector;makeThreadingException(Ljava/lang/String;Ljava/lang/Thread;)Lnet/minecraft/ReportedException;"),
		cancellable = true)
	public void safeify(int size, CallbackInfoReturnable<Integer> cir) {
		if (MixinConfigHelper.isSafeLegacyRandomSource()) {
			long l = this.seed.accumulateAndGet(0L, (prev, l1) -> prev * 25214903917L + 11L & 281474976710655L);
			cir.setReturnValue((int) (l >> 48 - size));
		}
	}
}
