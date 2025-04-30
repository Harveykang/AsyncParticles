package fun.qu_an.minecraft.asyncparticles.client.mixin.neoforge;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fun.qu_an.minecraft.asyncparticles.client.util.ConcurrentLong2ObjectMap;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelDataManager;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

// FIXME: Can I remove this?
@Mixin(ModelDataManager.class)
public abstract class MixinModelDataManager {
	@Mutable
	@Shadow(remap = false) @Final private Long2ObjectMap<Set<BlockPos>> needModelDataRefresh;

	@Mutable
	@Shadow(remap = false) @Final private Long2ObjectMap<Long2ObjectMap<ModelData>> modelDataCache;

	@Inject(method = "<init>", at = @At("RETURN"))
	private void onStaticInit(CallbackInfo ci) {
		needModelDataRefresh = new ConcurrentLong2ObjectMap<>(needModelDataRefresh);
		modelDataCache = new ConcurrentLong2ObjectMap<>(modelDataCache);
	}

	@WrapMethod(method = "isOtherThread", remap = false)
	private boolean wrapIsOtherThread(Operation<Boolean> original) {
		return original.call() && !ThreadUtil.isOnParticleTickerThread();
	}
}
