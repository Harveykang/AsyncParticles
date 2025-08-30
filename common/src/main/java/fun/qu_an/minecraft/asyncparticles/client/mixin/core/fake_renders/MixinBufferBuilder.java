package fun.qu_an.minecraft.asyncparticles.client.mixin.core.fake_renders;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.BufferBuilder;
import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import fun.qu_an.minecraft.asyncparticles.client.util.FakeBufferBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.nio.ByteBuffer;

@Mixin(BufferBuilder.class)
public class MixinBufferBuilder {
	@SuppressWarnings("ConstantValue")
	@WrapOperation(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/MemoryTracker;create(I)Ljava/nio/ByteBuffer;"))
	private ByteBuffer wrapInit(int capacity, Operation<ByteBuffer> original) {
		if ((Object) this instanceof FakeBufferBuilder &&
			!ModListHelper.VULKAN_MOD_LOADED) { // otherwise jvm internal error raised
			return null;
		}
		return original.call(capacity);
	}
}
