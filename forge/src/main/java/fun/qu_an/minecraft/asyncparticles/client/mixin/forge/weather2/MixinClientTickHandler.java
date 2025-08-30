package fun.qu_an.minecraft.asyncparticles.client.mixin.forge.weather2;

import fun.qu_an.minecraft.asyncparticles.client.task.EndTickEvent;
import net.minecraftforge.event.TickEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import weather2.ClientTickHandler;

@Mixin(value = ClientTickHandler.class, remap = false)
public class MixinClientTickHandler {
	@Shadow @Final public static ClientTickHandler INSTANCE;

	static {
		EndTickEvent.register(() -> INSTANCE.onTickInGame());
	}

	/**
	 * @author
	 * @reason
	 */
	@SuppressWarnings("OverwriteModifiers")
	// Remove @SubscribeEvent annotation
	@Overwrite
	public static void tick(TickEvent.ClientTickEvent event) {
		// do nothing
	}
}
