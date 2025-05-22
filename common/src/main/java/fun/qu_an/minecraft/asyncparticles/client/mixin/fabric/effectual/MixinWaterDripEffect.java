package fun.qu_an.minecraft.asyncparticles.client.mixin.fabric.effectual;

import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.WeakHashMap;

@Pseudo
@Mixin(targets = {
	"com.imeetake.effects.WaterDrip.WaterDripEffect",
	"com.imeetake.effectual.effects.WaterDrip.WaterDripEffect",
})
public class MixinWaterDripEffect {
	@Mutable
	@Shadow(remap = false)
	@Final
	private static Map<Player, Long> lastFullySubmergedTicks;

	@Inject(method = "<clinit>", remap = false, at = @At("TAIL"))
	private static void onInit(CallbackInfo ci) {
		lastFullySubmergedTicks = new WeakHashMap<>(); // Use WeakHashMap to avoid memory leak
	}
}
