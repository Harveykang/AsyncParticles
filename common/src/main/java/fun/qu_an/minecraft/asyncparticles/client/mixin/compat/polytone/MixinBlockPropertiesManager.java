package fun.qu_an.minecraft.asyncparticles.client.mixin.compat.polytone;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import fun.qu_an.minecraft.asyncparticles.client.compat.polytone.PolytoneCompat;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import fun.qu_an.minecraft.asyncparticles.client.util.Utils;
import net.mehvahdjukaar.polytone.content.block.BlockClientTickable;
import net.mehvahdjukaar.polytone.content.block.BlockPropertiesManager;
import net.mehvahdjukaar.polytone.content.block.TickSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Iterator;
import java.util.List;

@Mixin(value = BlockPropertiesManager.class, remap = false)
public abstract class MixinBlockPropertiesManager {
	@WrapOperation(method = "runTickers", at = @At(value = "INVOKE", target = "Ljava/util/List;iterator()Ljava/util/Iterator;"))
	private Iterator<BlockClientTickable> tickEmitterOnMainThread(
		List<BlockClientTickable> instance,
		Operation<Iterator<BlockClientTickable>> original,
		@Local(argsOnly = true) Level level,
		@Local(argsOnly = true) BlockPos pos,
		@Local(argsOnly = true) BlockState state,
		@Local(argsOnly = true) TickSource source) {
		if (ThreadUtil.isOnMainThread()) {
			return original.call(instance);
		}
		BlockPos posCopy = pos.immutable();
		PolytoneCompat.deferToMain(() -> instance.forEach(p -> p.tick(level, posCopy, state, source)));
		return Utils.dummyIterator();
	}
}
