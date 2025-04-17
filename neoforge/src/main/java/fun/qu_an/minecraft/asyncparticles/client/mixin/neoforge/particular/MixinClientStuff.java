package fun.qu_an.minecraft.asyncparticles.client.mixin.neoforge.particular;

import com.leclowndu93150.particular.Main;
import com.leclowndu93150.particular.utils.TextureCache;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fun.qu_an.minecraft.asyncparticles.client.AsyncTicker;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.TextureAtlasStitchedEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Main.ClientEvents.class)
public class MixinClientStuff {
	@WrapMethod(method = "onClientTick", remap = false)
	private static void onClientTick(ClientTickEvent.Pre event, Operation<Void> original) {
		AsyncTicker.addEndTickTask(() -> original.call((Object) null));
	}

	@Redirect(method = "lambda$onChunkLoad$3", remap = false,
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;execute(Ljava/lang/Runnable;)V"))
	private static void onChunkLoad(Minecraft mc, Runnable runnable) {
		AsyncTicker.addEndTickTask(runnable);
	}
}
