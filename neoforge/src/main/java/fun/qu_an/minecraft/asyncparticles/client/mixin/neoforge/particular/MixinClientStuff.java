package fun.qu_an.minecraft.asyncparticles.client.mixin.neoforge.particular;

import com.leclowndu93150.particular.Main;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fun.qu_an.minecraft.asyncparticles.client.api.EndTickOperation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Main.ClientEvents.class)
public class MixinClientStuff {
	@Unique
	private static final ResourceLocation PARTICULAR$ON_CLIENT_TICK =
		ResourceLocation.fromNamespaceAndPath("particular", "on_client_tick");
	@WrapMethod(method = "onClientTick", remap = false)
	private static void onClientTick(ClientTickEvent.Pre event, Operation<Void> original) {
		EndTickOperation.schedule(PARTICULAR$ON_CLIENT_TICK, () -> original.call((Object) null));
	}
}
