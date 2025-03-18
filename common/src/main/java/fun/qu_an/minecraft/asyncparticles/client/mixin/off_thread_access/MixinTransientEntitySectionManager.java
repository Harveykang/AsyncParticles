package fun.qu_an.minecraft.asyncparticles.client.mixin.off_thread_access;

import fun.qu_an.minecraft.asyncparticles.client.addon.IsClientAddon;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.level.entity.LevelCallback;
import net.minecraft.world.level.entity.TransientEntitySectionManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TransientEntitySectionManager.class)
public class MixinTransientEntitySectionManager {
	@Shadow @Final
	EntitySectionStorage<?> sectionStorage;

	@Inject(method = "<init>", at = @At("RETURN"))
	private void onInit(Class<?> clazz, LevelCallback<?> callbacks, CallbackInfo ci) {
		((IsClientAddon) sectionStorage).asyncparticles$setClientSide();
	}

}
