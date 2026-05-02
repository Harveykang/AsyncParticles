package fun.qu_an.minecraft.asyncparticles.client.mixin.core.off_thread_access;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = Level.class, priority = 1500)
public abstract class MixinLevel {
	@WrapOperation(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;create()Lnet/minecraft/util/RandomSource;"))
	private RandomSource redirectRandomSource(Operation<RandomSource> original) {
		RandomSource source = original.call();
		if (((Object) this) instanceof ClientLevel) {
			return new SingleThreadedRandomSource(RandomSupport.generateUniqueSeed());
		}
		return source;
	}
}
