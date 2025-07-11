package fun.qu_an.minecraft.asyncparticles.client.mixin.fabric.particular;

import com.chailotl.particular.Main;
import fun.qu_an.minecraft.asyncparticles.client.api.EndTickEvent;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.Event;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;

@Mixin(value = Main.class, remap = false)
public class MixinMain {
	@Redirect(method = "onInitializeClient",
		slice = @Slice(from = @At(value = "FIELD", target = "Lnet/fabricmc/fabric/api/client/event/lifecycle/v1/ClientTickEvents;START_WORLD_TICK:Lnet/fabricmc/fabric/api/event/Event;")),
		at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/fabricmc/fabric/api/event/Event;register(Ljava/lang/Object;)V"))
	private void onRegister(Event<ClientTickEvents.StartWorldTick> instance, Object t) {
		EndTickEvent.register(((ClientTickEvents.StartWorldTick) t)::onStartTick);
	}
}
