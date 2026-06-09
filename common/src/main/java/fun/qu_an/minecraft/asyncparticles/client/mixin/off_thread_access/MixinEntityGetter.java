package fun.qu_an.minecraft.asyncparticles.client.mixin.off_thread_access;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.EntityGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.UUID;

@Mixin(EntityGetter.class)
public interface MixinEntityGetter {
	@Inject(method = "getPlayerByUUID", at = @At("HEAD"))
	default void injectHead(CallbackInfoReturnable<Player> cir, @Share("isClientLevel") LocalBooleanRef isClientLevel) {
		isClientLevel.set(this instanceof ClientLevel);
	}

	@WrapOperation(method = "getPlayerByUUID", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getUUID()Ljava/util/UUID;"))
	default UUID wrapPlayerGetUUID(Player player, Operation<UUID> original, @Share("isClientLevel") LocalBooleanRef isClientLevel) {
		return isClientLevel.get() && player == null ? null : original.call(player);
	}

	@WrapOperation(method = "getPlayerByUUID", at = @At(value = "INVOKE", target = "Ljava/util/List;get(I)Ljava/lang/Object;"))
	default <E> E wrapPlayerGet(List<E> list, int index, Operation<E> original, @Share("isClientLevel") LocalBooleanRef isClientLevel) {
		try {
			return original.call(list, index);
		} catch (IndexOutOfBoundsException exception) {
			if (isClientLevel.get()) {
				return null;
			}
			throw exception;
		}
	}
}
