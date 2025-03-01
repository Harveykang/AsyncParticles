package fun.qu_an.minecraft.asyncparticles.client.mixin.sodium_like;

import com.mojang.blaze3d.vertex.BufferBuilder;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadLocalBufferBuilder;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.caffeinemc.mods.sodium.api.vertex.format.VertexFormatDescription;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ThreadLocalBufferBuilder.class, remap = false)
public class MixinThreadLocalBufferBuilder implements VertexBufferWriter {
	@Shadow
	private ThreadLocal<BufferBuilder> buffer;

	@Override
	public void push(MemoryStack memoryStack, long l, int i, VertexFormatDescription vertexFormatDescription) {
		((VertexBufferWriter) buffer.get()).push(memoryStack, l, i, vertexFormatDescription);
	}
}
