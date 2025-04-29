package fun.qu_an.minecraft.asyncparticles.client.mixin.lodestone;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import team.lodestar.lodestone.handlers.RenderHandler;

@Mixin(RenderHandler.class)
public class MixinRenderHandler {
	@WrapMethod(method = "addRenderType", remap = false)
	private static synchronized void addRenderType(RenderType renderType, Operation<Void> original) {
		original.call(renderType);
	}
}
