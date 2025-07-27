package fun.qu_an.minecraft.asyncparticles.client.mixin.fabric.effective;

import fun.qu_an.minecraft.asyncparticles.client.util.SyncArrayList;
import org.ladysnake.effective.index.EffectiveLights;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;

@Mixin(EffectiveLights.class)
public class MixinEffectiveLights {
	@Redirect(method = "<clinit>", at = @At(value = "NEW", target = "()Ljava/util/ArrayList;"))
	private static ArrayList<?> redirectArrayList() {
		return new SyncArrayList<>();
	}
}
