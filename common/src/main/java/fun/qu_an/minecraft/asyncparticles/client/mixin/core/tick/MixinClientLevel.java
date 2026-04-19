package fun.qu_an.minecraft.asyncparticles.client.mixin.core.tick;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fun.qu_an.minecraft.asyncparticles.client.particle.AsyncTickBehavior;
import fun.qu_an.minecraft.asyncparticles.client.task.EndTickOperation;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.util.GameUtil;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

@Mixin(value = ClientLevel.class, priority = 1100)
public abstract class MixinClientLevel extends Level {
	protected MixinClientLevel(WritableLevelData writableLevelData, ResourceKey<Level> resourceKey, RegistryAccess registryAccess, Holder<DimensionType> holder, Supplier<ProfilerFiller> supplier, boolean bl, boolean bl2, long l, int i) {
		super(writableLevelData, resourceKey, registryAccess, holder, supplier, bl, bl2, l, i);
	}

	@Unique
	private static final ResourceLocation ANIMATE_TICK = GameUtil.id("animate_tick");
	@WrapMethod(method = "animateTick")
	public void animateTick(int i, int j, int k, Operation<Void> original) {
		if (!AsyncTickBehavior.INSTANCE.shouldTickParticles &&
			ConfigHelper.isTickAsync()) {
			// don't tick animate if the game is lagging
			return;
		}
		if (!ConfigHelper.asyncBlockEntityAnimate()) {
			original.call(i, j, k);
		} else {
			EndTickOperation.schedule(ANIMATE_TICK, () -> original.call(i, j, k));
		}
	}

	@Inject(method = "animateTick", at = @At(value = "CONSTANT", args = "intValue=16"), cancellable = true)
	public void onAnimateTick(int i, int j, int k, CallbackInfo ci) {
		if (AsyncTickBehavior.INSTANCE.isCancelled() && !ConfigHelper.forceDoneBlockAnimateTick()) {
			ci.cancel();
		}
	}
}
