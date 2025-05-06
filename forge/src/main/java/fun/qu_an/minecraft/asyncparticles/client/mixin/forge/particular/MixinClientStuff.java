package fun.qu_an.minecraft.asyncparticles.client.mixin.forge.particular;

import com.leclowndu93150.particular.ClientStuff;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fun.qu_an.minecraft.asyncparticles.client.AsyncTicker;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.TickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ClientStuff.ClientEvents.class)
public class MixinClientStuff {
	@Unique
	private static final ResourceLocation asyncparticles$PARTICULAR$ON_CLIENT_TICK =
		new ResourceLocation("particular", "on_client_tick");
	@WrapMethod(method = "onClientTick", remap = false)
	private static void onClientTick(TickEvent.ClientTickEvent event, Operation<Void> original) {
		if (event.phase == TickEvent.Phase.START) {
			AsyncTicker.addEndTickTask(asyncparticles$PARTICULAR$ON_CLIENT_TICK, () -> original.call(new TickEvent.ClientTickEvent(TickEvent.Phase.START)));
		}
	}

	@Unique
	private static final ResourceLocation asyncparticles$PARTICULAR$ON_CHUNK_LOAD =
		new ResourceLocation("particular", "on_chunk_load");
	@Redirect(method = "lambda$onChunkLoad$4", remap = false,
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;execute(Ljava/lang/Runnable;)V"))
	private static void onChunkLoad(Minecraft mc, Runnable runnable) {
		AsyncTicker.addEndTickTask(asyncparticles$PARTICULAR$ON_CHUNK_LOAD, runnable);
	}
}
