package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.vulkanmod;

import net.vulkanmod.vulkan.Drawer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = Drawer.class, remap = false)
public interface AccessorDrawer {
	@Accessor
	long getPBuffers();

	@Accessor
	long getPOffsets();
}
