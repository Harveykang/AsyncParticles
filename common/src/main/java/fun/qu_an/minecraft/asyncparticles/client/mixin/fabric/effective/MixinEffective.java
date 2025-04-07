//package fun.qu_an.minecraft.asyncparticles.client.mixin.fabric.effective;
//
//import fun.qu_an.minecraft.asyncparticles.client.AsyncTicker;
//import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
//import net.fabricmc.fabric.api.event.Event;
//import org.ladysnake.effective.Effective;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Redirect;
//import org.spongepowered.asm.mixin.injection.Slice;
//
//@Mixin(Effective.class)
//public class MixinEffective {
//	@Redirect(method = "onInitializeClient", remap = false,
//		slice = @Slice(from = @At(value = "FIELD", ordinal = 0, target = "Lnet/fabricmc/fabric/api/client/event/lifecycle/v1/ClientTickEvents;END_CLIENT_TICK:Lnet/fabricmc/fabric/api/event/Event;")),
//		at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/fabricmc/fabric/api/event/Event;register(Ljava/lang/Object;)V"))
//	private void onRegister(Event<ClientTickEvents.EndTick> instance, Object t) {
//		AsyncTicker.registerEndTickEvent(((ClientTickEvents.EndTick) t)::onEndTick);
//	}
//}
