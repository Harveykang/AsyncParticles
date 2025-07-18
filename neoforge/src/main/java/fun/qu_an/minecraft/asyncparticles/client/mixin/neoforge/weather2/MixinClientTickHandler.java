package fun.qu_an.minecraft.asyncparticles.client.mixin.neoforge.weather2;

import fun.qu_an.minecraft.asyncparticles.client.api.EndTickEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
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
	public static void tick(ClientTickEvent.Pre event) {
		// do nothing
	}
}
