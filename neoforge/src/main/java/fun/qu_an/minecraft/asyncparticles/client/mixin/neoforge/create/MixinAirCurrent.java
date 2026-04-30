package fun.qu_an.minecraft.asyncparticles.client.mixin.neoforge.create;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.simibubi.create.content.kinetics.fan.AirCurrent;
import com.simibubi.create.content.kinetics.fan.IAirCurrentSource;
import fun.qu_an.minecraft.asyncparticles.client.util.IterationSafeArrayList;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * To make segments list iteration-safe, we need to wrap it with a delegate list.
 * SO we don't modify the original list directly, we set a new list when we rebuild the segments.
 */
@Mixin(value = AirCurrent.class, remap = false)
public class MixinAirCurrent {
	@Shadow
	public List<?> segments;
	@Unique
	private boolean asyncparticles$isClient = false;

	@Inject(method = "<init>", at = @At("RETURN"))
	public void onInit(IAirCurrentSource source, CallbackInfo ci) {
		if (ThreadUtil.isOnClientTickThread()) {
			asyncparticles$isClient = true;
			segments = new IterationSafeArrayList<>(segments);
		}
	}

	@WrapOperation(method = "rebuild", at = @At(value = "INVOKE", target = "Ljava/util/List;clear()V"))
	public void redirectRebuildClear(List<Object> instance, Operation<Void> original, @Share("localSegs") LocalRef<List<Object>> localSegs) {
		if (!asyncparticles$isClient) {
			original.call(instance);
		} else {
			localSegs.set(new IterationSafeArrayList<>());
		}
	}

	@WrapOperation(method = "rebuild", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z"))
	public boolean redirectRebuildAdd(List<Object> instance, Object e, Operation<Boolean> original, @Share("localSegs") LocalRef<List<Object>> localSegs) {
		if (!asyncparticles$isClient) {
			return original.call(instance, e);
		}
		List<Object> segs = localSegs.get();
		if (segs == null) {
			localSegs.set(segs = new IterationSafeArrayList<>(instance));
		}
		return segs.add(e);
	}

	@Inject(method = "rebuild", at = @At("RETURN"))
	public void onRebuildHead(CallbackInfo ci, @Share("localSegs") LocalRef<List<?>> localSegs) {
		if (!asyncparticles$isClient) {
			return;
		}
		List<?> segs;
		if ((segs = localSegs.get()) != null) {
			segments = segs;
		} else if (!(segments instanceof IterationSafeArrayList)) {
			segments = new IterationSafeArrayList<>(segments);
		}
	}
}
