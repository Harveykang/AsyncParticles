package fun.qu_an.minecraft.asyncparticles.client.mixin.legacy.flywheel;

import com.bawnorton.mixinsquared.TargetHandler;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientLevel.class)
public class MixinClientLevelMixin {
	@Dynamic
	@TargetHandler(
		mixin = "com.jozufozu.flywheel.mixin.ClientLevelMixin",
		name = "filterEntities"
	)
	@Inject(method = "@MixinSquared:Handler", at = @At("HEAD"), cancellable = true)
	private void filterEntities(CallbackInfoReturnable<Iterable<Entity>> ignored, CallbackInfo ci) {
		if (!RenderSystem.isOnRenderThread()) {
			ci.cancel();
		}
	}
}
