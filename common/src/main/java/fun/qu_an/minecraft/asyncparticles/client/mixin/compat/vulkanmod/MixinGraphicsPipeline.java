package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.vulkanmod;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.gpu_acceleration.GpuParticlePipelines;
import net.vulkanmod.interfaces.VertexFormatMixed;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(GraphicsPipeline.class)
public class MixinGraphicsPipeline {
	@Inject(method = "getAttributeDescriptions", at = @At("HEAD"), cancellable = true)
	private static void getAttributeDescriptions(VertexFormat vertexFormat, CallbackInfoReturnable<VkVertexInputAttributeDescription.Buffer> cir) {
		if (vertexFormat != GpuParticlePipelines.IDENTITY_PARTICLE) {
			return;
		}

		List<VertexFormatElement> elements = vertexFormat.getElements();
		int size = elements.size();
		VkVertexInputAttributeDescription.Buffer attributeDescriptions = VkVertexInputAttributeDescription.calloc(size);
		int offset = 0;

		for(int i = 0; i < size; ++i) {
			VkVertexInputAttributeDescription posDescription;
			posDescription = attributeDescriptions.get(i);
			posDescription.binding(0);
			posDescription.location(i);
			VertexFormatElement formatElement = elements.get(i);
			VertexFormatElement.Type type = formatElement.type();
			int count = formatElement.count();
			label62:
			switch (type) {
				case FLOAT:
					switch (count) {
						case 1:
							posDescription.format(100);
							posDescription.offset(offset);
							offset += 4;
							break label62;
						case 2:
							posDescription.format(103);
							posDescription.offset(offset);
							offset += 8;
							break label62;
						case 3:
							posDescription.format(106);
							posDescription.offset(offset);
							offset += 12;
						case 4:
							posDescription.format(VK10.VK_FORMAT_R32G32B32A32_SFLOAT);
							posDescription.offset(offset);
							offset += 16;
							break label62;
						default:
							throw new IllegalStateException("Unexpected value: " + count);
					}
				case UBYTE:
					switch (count) {
						case 4:
							if (formatElement.normalized()) {
								posDescription.format(37);
							} else {
								posDescription.format(41);
							}

							posDescription.offset(offset);
							offset += 4;
							break label62;
						default:
							throw new IllegalStateException("Unexpected value: " + count);
					}
				case BYTE:
					switch (count) {
						case 3:
							if (formatElement.normalized()) {
								posDescription.format(38);
							} else {
								posDescription.format(42);
							}

							posDescription.offset(offset);
							offset += 4;
							break label62;
						case 4:
							if (formatElement.normalized()) {
								posDescription.format(38);
							} else {
								posDescription.format(42);
							}

							posDescription.offset(offset);
							offset += 4;
							break label62;
						default:
							throw new IllegalStateException("Unexpected value: " + count);
					}
				case USHORT:
					switch (count) {
						case 2:
							posDescription.format(81);
							posDescription.offset(offset);
							offset += 4;
							break label62;
						case 4:
							posDescription.format(95);
							posDescription.offset(offset);
							offset += 8;
							break label62;
						default:
							throw new IllegalStateException("Unexpected value: " + count);
					}
				case SHORT:
					switch (count) {
						case 1:
							posDescription.format(75);
							posDescription.offset(offset);
							offset += 2;
							break label62;
						case 2:
							posDescription.format(82);
							posDescription.offset(offset);
							offset += 8;
							break label62;
						case 3:
						default:
							throw new IllegalStateException("Unexpected value: " + count);
						case 4:
							posDescription.format(96);
							posDescription.offset(offset);
							offset += 4;
							break label62;
					}
				case UINT:
					switch (count) {
						case 1:
							posDescription.format(98);
							posDescription.offset(offset);
							offset += 4;
							break label62;
						default:
							throw new IllegalStateException("Unexpected value: " + count);
					}
				case INT:
					switch (count) {
						case 1:
							posDescription.format(99);
							posDescription.offset(offset);
							offset += 4;
							break;
						case 2:
							posDescription.format(VK10.VK_FORMAT_R32G32_SINT);
							posDescription.offset(offset);
							offset += 8;
							break;
						default:
							throw new IllegalStateException("Unexpected value: " + count);
					}
			}

			posDescription.offset(((VertexFormatMixed)vertexFormat).getOffset(i));
		}

		cir.setReturnValue(attributeDescriptions.rewind());
	}
}
