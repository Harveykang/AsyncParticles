package fun.qu_an.minecraft.asyncparticles.client.mixin.off_thread_access;

import fun.qu_an.minecraft.asyncparticles.client.addon.IsClientAddon;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.entity.EntitySectionStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntitySectionStorage.class)
public class MixinEntitySectionStorage implements IsClientAddon {
	@Unique
	boolean asyncparticles$isClientSide = false;

	@Override
	public boolean asyncparticles$isClientSide() {
		return asyncparticles$isClientSide;
	}

	@Override
	public void asyncparticles$setClientSide() {
		this.asyncparticles$isClientSide = true;
	}

	@Inject(method = "createSection", at = @At(value = "RETURN"))
	private void onSectionCreated(long sectionPos, CallbackInfoReturnable<EntitySection<?>> cir) {
		if (asyncparticles$isClientSide) {
			((IsClientAddon) cir.getReturnValue()).asyncparticles$setClientSide();
		}
	}
}
