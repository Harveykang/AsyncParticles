package fun.qu_an.minecraft.asyncparticles.client.mixin.off_thread_access;

import fun.qu_an.minecraft.asyncparticles.client.addon.IsClientAddon;
import fun.qu_an.minecraft.asyncparticles.client.util.IterationSafeArrayList;
import net.minecraft.util.ClassInstanceMultiMap;
import net.minecraft.world.level.entity.EntitySection;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.concurrent.ConcurrentHashMap;

@Mixin(EntitySection.class)
public class MixinEntitySection implements IsClientAddon {
	@Shadow @Final private ClassInstanceMultiMap<?> storage;
	@Unique
	private boolean asyncparticles$isClientSide;

	@Override
	public boolean asyncparticles$isClientSide() {
		return asyncparticles$isClientSide;
	}

	@Override
	public void asyncparticles$setClientSide() {
		if (!asyncparticles$isClientSide) {
			asyncparticles$isClientSide = true;
			((IsClientAddon) storage).asyncparticles$setClientSide();
		}
	}
}
