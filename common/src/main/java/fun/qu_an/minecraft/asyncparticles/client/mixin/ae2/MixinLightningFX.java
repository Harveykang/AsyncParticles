package fun.qu_an.minecraft.asyncparticles.client.mixin.ae2;

import appeng.client.render.effects.LightningArcFX;
import appeng.client.render.effects.LightningFX;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({LightningFX.class, LightningArcFX.class})
public class MixinLightningFX {
	@Redirect(method = "<clinit>", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;create()Lnet/minecraft/util/RandomSource;"))
	private static RandomSource createRandomSource() {
		return new SingleThreadedRandomSource(RandomSupport.generateUniqueSeed());
	}
}
