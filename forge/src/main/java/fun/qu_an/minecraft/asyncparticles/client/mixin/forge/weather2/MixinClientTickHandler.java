package fun.qu_an.minecraft.asyncparticles.client.mixin.forge.weather2;

import fun.qu_an.minecraft.asyncparticles.client.AsyncTicker;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import weather2.ClientTickHandler;

@Mixin(value = ClientTickHandler.class, remap = false)
public class MixinClientTickHandler {
	@Shadow @Final public static ClientTickHandler INSTANCE;

	static {
		AsyncTicker.registerEndTickEvent(() -> INSTANCE.onTickInGame());
	}

	/**
	 * @author
	 * @reason
	 */
	@Overwrite
	@SubscribeEvent
	public static void tick(TickEvent.ClientTickEvent event) {
		// do nothing
	}
}
