package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.vulkanmod;

import net.vulkanmod.gl.VkGlFramebuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = VkGlFramebuffer.class, remap = false)
public interface AccessorVkGlFramebuffer {
	@Accessor("boundFramebuffer")
	static void setBoundFramebuffer(VkGlFramebuffer framebuffer) {
		throw new AssertionError("Implemented via mixin");
	}
}
