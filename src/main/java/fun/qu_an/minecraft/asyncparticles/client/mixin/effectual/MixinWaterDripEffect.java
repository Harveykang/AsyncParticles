package fun.qu_an.minecraft.asyncparticles.client.mixin.effectual;

import com.imeetake.effects.WaterDrip.WaterDripEffect;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

@Mixin(value = WaterDripEffect.class, remap = false)
public class MixinWaterDripEffect {
	@Mutable
	@Shadow
	@Final
	private static Map<Player, Long> lastFullySubmergedTicks;

	@Inject(method = "<clinit>", at = @At("TAIL"))
	private static void onInit(CallbackInfo ci) {
		lastFullySubmergedTicks = new WeakHashMap<>(); // Use WeakHashMap to avoid memory leak
	}
}
