package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.vulkanmod;

import net.vulkanmod.vulkan.memory.buffer.Buffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = Buffer.class, remap = false)
public interface AccessorBuffer {
	@Accessor
	void setOffset(long offset);
}
