package fun.qu_an.minecraft.asyncparticles.client.mixin.forge.particular;

import com.leclowndu93150.particular.ClientStuff;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fun.qu_an.minecraft.asyncparticles.client.api.EndTickOperation;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.TickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ClientStuff.ClientEvents.class)
public class MixinClientStuff {
	@Unique
	private static final ResourceLocation PARTICULAR$ON_CLIENT_TICK =
		new ResourceLocation("particular", "on_client_tick");
	@WrapMethod(method = "onClientTick", remap = false)
	private static void onClientTick(TickEvent.ClientTickEvent event, Operation<Void> original) {
		if (event.phase == TickEvent.Phase.START) {
			EndTickOperation.schedule(PARTICULAR$ON_CLIENT_TICK, () -> original.call(new TickEvent.ClientTickEvent(TickEvent.Phase.START)));
		}
	}
}
