package fun.qu_an.minecraft.asyncparticles.client.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fun.qu_an.minecraft.asyncparticles.client.AsyncTicker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

@Mixin(value = ClientLevel.class, priority = 1001)
public abstract class MixinClientLevel extends Level {
	protected MixinClientLevel(WritableLevelData writableLevelData, ResourceKey<Level> resourceKey, DimensionType dimensionType, Supplier<ProfilerFiller> supplier, boolean bl, boolean bl2, long l) {
		super(writableLevelData, resourceKey, dimensionType, supplier, bl, bl2, l);
	}

	@WrapMethod(method = "animateTick")
	public void animateTick(int i, int j, int k, Operation<Void> original) {
		if (AsyncTicker.shouldTickParticles) {
			AsyncTicker.beforeParticleOperations.add(() -> original.call(i, j, k));
		}
	}

	@Inject(method = "animateTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getItem()Lnet/minecraft/world/item/Item;"), cancellable = true)
	public void onAnimateTick1(int i, int j, int k, CallbackInfo ci) {
		if (AsyncTicker.cancelled) {
			ci.cancel();
		}
	}

	@Inject(method = "animateTick", at = @At(value = "CONSTANT", args = "intValue=16"), cancellable = true)
	public void onAnimateTick2(int i, int j, int k, CallbackInfo ci) {
		if (AsyncTicker.cancelled) {
			ci.cancel();
		}
	}
}
