package fun.qu_an.minecraft.asyncparticles.client.mixin.core;

import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.jtracy.MemoryPool;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MemoryPool.class)
public class MixinMemoryPool {
	@Expression("this != ?")
	@ModifyExpressionValue(remap = false, method = {"malloc", "free"}, at = @At("MIXINEXTRAS:EXPRESSION"))
	public boolean isAvailable(boolean original) {
		return false;
	}
}
