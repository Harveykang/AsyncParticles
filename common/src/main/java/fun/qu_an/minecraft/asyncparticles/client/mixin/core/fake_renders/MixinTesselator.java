package fun.qu_an.minecraft.asyncparticles.client.mixin.core.fake_renders;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.Tesselator;
import fun.qu_an.minecraft.asyncparticles.client.util.FakeTesselator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Tesselator.class)
public class MixinTesselator {
	@SuppressWarnings("ConstantValue")
	@WrapOperation(method = "<init>(I)V", at = @At(value = "NEW", target = "(I)Lcom/mojang/blaze3d/vertex/ByteBufferBuilder;"))
	private ByteBufferBuilder createFakeBufferBuilder(int capacity, Operation<ByteBufferBuilder> original) {
		if ((Object) this instanceof FakeTesselator) {
			return null;
		}
		return original.call(capacity);
	}
}
