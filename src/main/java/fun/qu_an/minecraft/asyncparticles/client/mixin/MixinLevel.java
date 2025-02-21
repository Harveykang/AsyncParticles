package fun.qu_an.minecraft.asyncparticles.client.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(Level.class)
public class MixinLevel {
	@Shadow @Final public boolean isClientSide;

	@Shadow @Final public List<TickingBlockEntity> pendingBlockEntityTickers;

	@Shadow @Final protected List<TickingBlockEntity> blockEntityTickers;

	// Redirect the method to synchronize on the pendingBlockEntityTickers list to avoid concurrent modification
	// This is only necessary on the client side
	@SuppressWarnings("SynchronizeOnNonFinalField")
	@Redirect(method = "tickBlockEntities", at = @At(value = "INVOKE", target = "Ljava/util/List;isEmpty()Z"))
	private boolean onTickBlockEntities(List instance) {
		if (!isClientSide) {
			return instance.isEmpty();
		}
		if (!this.pendingBlockEntityTickers.isEmpty()) {
			synchronized (pendingBlockEntityTickers) {
				if (!this.pendingBlockEntityTickers.isEmpty()) {
					this.blockEntityTickers.addAll(this.pendingBlockEntityTickers);
					this.pendingBlockEntityTickers.clear();
				}
			}
		}
		return true;
	}
}
