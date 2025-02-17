package fun.qu_an.minecraft.asyncparticles.client.mixin.create;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.logistics.depot.DepotBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Mixin(value = DepotBehaviour.class, remap = false)
public abstract class MixinDepotBehaviour extends BlockEntityBehaviour {
	@Shadow
	List<TransportedItemStack> incoming;

	public MixinDepotBehaviour(SmartBlockEntity be) {
		super(be);
	}

	@Inject(method = "<init>", at = @At(value = "RETURN"))
	private void onInit(SmartBlockEntity be, CallbackInfo ci) {
		Level level = blockEntity.getLevel();
		if (level != null && level.isClientSide) {
			incoming = new CopyOnWriteArrayList<>(incoming);
		}
	}
}
