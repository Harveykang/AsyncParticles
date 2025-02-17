package fun.qu_an.minecraft.asyncparticles.client.mixin.forge_create;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.logistics.depot.EjectorBlockEntity;
import com.simibubi.create.foundation.utility.IntAttached;
import com.simibubi.create.foundation.utility.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

@Mixin(value = EjectorBlockEntity.class, remap = false)
public abstract class MixinEjectorBlockEntity extends KineticBlockEntity {

	@Shadow
	List<IntAttached<ItemStack>> launchedItems;

	@Shadow
	ItemStack trackedItem;

	@Shadow
	float earlyTargetTime;

	@Shadow @Nullable Pair<Vec3, BlockPos> earlyTarget;

	@Shadow protected abstract void placeItemAtTarget(boolean doLogic, float maxTime, IntAttached<ItemStack> intAttached);

	@Shadow protected abstract boolean scanTrajectoryForObstacles(int time);

	public MixinEjectorBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
		super(typeIn, pos, state);
	}

	@Redirect(method = "tick()V", at = @At(value = "INVOKE", target = "Ljava/util/List;iterator()Ljava/util/Iterator;"))
	private Iterator<IntAttached<ItemStack>> onTick(List<IntAttached<ItemStack>> instance,
													 @Local(name = "totalTime") float totalTime,
													 @Local(name = "doLogic") boolean doLogic) {
		if (!level.isClientSide) {
			return instance.iterator();
		}
		Set<IntAttached<ItemStack>> toRemove = new HashSet<>();
		IntAttached<ItemStack> intAttached;
		for (Iterator<IntAttached<ItemStack>> iterator = launchedItems.iterator(); iterator.hasNext(); intAttached.increment()) {
			intAttached = iterator.next();
			boolean hit = false;
			if (intAttached.getSecond() == this.trackedItem) {
				hit = this.scanTrajectoryForObstacles(intAttached.getFirst());
			}

			float maxTime = this.earlyTarget != null ? Math.min(this.earlyTargetTime, totalTime) : totalTime;
			if (hit || intAttached.exceeds((int) maxTime)) {
				this.placeItemAtTarget(doLogic, maxTime, intAttached);
				toRemove.add(intAttached);
			}
		}
		launchedItems.removeAll(toRemove);
		return Collections.emptyIterator();
	}

	@Inject(method = "<init>", at = @At("RETURN"))
	private void onInit(BlockEntityType<?> typeIn, BlockPos pos, BlockState state, CallbackInfo ci) {
		Level level = getLevel();
		// 这个列表很小，不会过于影响性能
		if (level == null) { // god-damn it, WHY!?!?!
			String threadName = Thread.currentThread().getName().toLowerCase(Locale.ROOT);
			// TODO: Dimensional threading 兼容，但是写成这样太丑了，有更好的方法吗？
			if (!threadName.contains("server")){
				launchedItems = new CopyOnWriteArrayList<>(launchedItems);
			}
		} else if (level.isClientSide) {
			launchedItems = new CopyOnWriteArrayList<>(launchedItems);
		}
	}

	@WrapOperation(method = "read", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/foundation/utility/NBTHelper;readCompoundList(Lnet/minecraft/nbt/ListTag;Ljava/util/function/Function;)Ljava/util/List;"))
	private <T> List<T> readCompoundList(ListTag listNBT, Function<CompoundTag, T> deserializer, Operation<List<T>> original) {
		Level level = getLevel();
		// 这个列表很小，不会过于影响性能
		if (level == null) { // god-damn it, WHY!?!?!
			String threadName = Thread.currentThread().getName().toLowerCase(Locale.ROOT);
			// TODO: Dimensional threading 兼容，但是写成这样太丑了，有更好的方法吗？
			if (!threadName.contains("server")) {
				return new CopyOnWriteArrayList<>(original.call(listNBT, deserializer));
			}
		} else if (level.isClientSide) {
			return new CopyOnWriteArrayList<>(original.call(listNBT, deserializer));
		}
		return original.call(listNBT, deserializer);
	}
}
