package fun.qu_an.minecraft.asyncparticles.client.mixin.neoforge.create;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.logistics.depot.DepotBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.item.ItemHelper;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.Containers;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

@Mixin(value = DepotBehaviour.class, remap = false)
public abstract class MixinDepotBehaviour extends BlockEntityBehaviour {
	@Shadow
	List<TransportedItemStack> incoming;

	@Shadow protected abstract boolean tick(TransportedItemStack heldItem);

	@Shadow
	TransportedItemStack heldItem;

	public MixinDepotBehaviour(SmartBlockEntity be) {
		super(be);
	}

	@Redirect(method = "tick()V", at = @At(value = "INVOKE", target = "Ljava/util/List;iterator()Ljava/util/Iterator;"))
	private Iterator<TransportedItemStack> onTick(List<TransportedItemStack> instance, @Local(name = "world") Level world) {
		if (!world.isClientSide) {
			return instance.iterator();
		}
		Set<TransportedItemStack> toRemove = new HashSet<>();
		for (TransportedItemStack ts : incoming) {
			if (!tick(ts))
				continue;
			if (!blockEntity.isVirtual())
				continue;
			if (heldItem == null) {
				heldItem = ts;
			} else {
				if (!ItemHelper.canItemStackAmountsStack(heldItem.stack, ts.stack)) {
					Vec3 vec = VecHelper.getCenterOf(blockEntity.getBlockPos());
					Containers.dropItemStack(blockEntity.getLevel(), vec.x, vec.y + .5f, vec.z, ts.stack);
				} else {
					heldItem.stack.grow(ts.stack.getCount());
				}
			}
			toRemove.add(ts);
			blockEntity.notifyUpdate();
		}
		incoming.removeAll(toRemove);
		return Collections.emptyIterator();
	}

	@Inject(method = "<init>", at = @At(value = "RETURN"))
	private void onInit(SmartBlockEntity be, CallbackInfo ci) {
		Level level = be.getLevel();
		// 这个列表很小，不会过于影响性能
		if (level == null) {
			if (ThreadUtil.isOnClientTickThread()) {
				incoming = new CopyOnWriteArrayList<>(incoming);
			}
		} else if (level.isClientSide) {
			incoming = new CopyOnWriteArrayList<>(incoming);
		}
	}

	@WrapOperation(method = "read", at = @At(value = "INVOKE", target = "Lnet/createmod/catnip/nbt/NBTHelper;readCompoundList(Lnet/minecraft/nbt/ListTag;Ljava/util/function/Function;)Ljava/util/List;"))
	private <T> List<T> readCompoundList(ListTag listNBT, Function<CompoundTag, T> deserializer, Operation<List<T>> original) {
		Level level = blockEntity.getLevel();
		// 这个列表很小，不会过于影响性能
		if (level == null) {
			if (ThreadUtil.isOnClientTickThread()) {
				return new CopyOnWriteArrayList<>(original.call(listNBT, deserializer));
			}
		} else if (level.isClientSide) {
			return new CopyOnWriteArrayList<>(original.call(listNBT, deserializer));
		}
		return original.call(listNBT, deserializer);
	}
}
