package fun.qu_an.minecraft.asyncparticles.client.mixin.neoforge.particular;

import com.leclowndu93150.particular.Main;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fun.qu_an.minecraft.asyncparticles.client.api.EndTickOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Main.ClientEvents.class)
public class MixinClientStuff {
	@Unique
	private static final ResourceLocation PARTICULAR$ON_CLIENT_TICK =
		ResourceLocation.fromNamespaceAndPath("particular", "on_client_tick");
	@WrapMethod(method = "onClientTick", remap = false)
	private static void onClientTick(ClientTickEvent.Pre event, Operation<Void> original) {
		EndTickOperation.schedule(PARTICULAR$ON_CLIENT_TICK, false, () -> original.call((Object) null));
	}

	@Unique
	private static final ResourceLocation PARTICULAR$ON_CHUNK_LOAD =
		ResourceLocation.fromNamespaceAndPath("particular", "on_chunk_load");
	@Redirect(method = "lambda$onChunkLoad$3", remap = false,
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;execute(Ljava/lang/Runnable;)V"))
	private static void onChunkLoad(Minecraft mc, Runnable runnable) {
		EndTickOperation.schedule(PARTICULAR$ON_CHUNK_LOAD, false, runnable);
	}
}
