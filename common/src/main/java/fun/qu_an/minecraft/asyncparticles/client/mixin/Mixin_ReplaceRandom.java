package fun.qu_an.minecraft.asyncparticles.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.particle.Particle;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(Particle.class) // Will be replaced with the actual targets
public abstract class Mixin_ReplaceRandom {
	@WrapOperation(method = "*", require = 0, at = @At(value = "INVOKE",
		target = "Lnet/minecraft/util/RandomSource;create()Lnet/minecraft/util/RandomSource;"))
	private static RandomSource onCreateRandomSource(Operation<RandomSource> original) {
		return new SingleThreadedRandomSource(RandomSupport.generateUniqueSeed());
	}
}
