package fun.qu_an.minecraft.asyncparticles.client.mixin.create;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.logistics.depot.DepotBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
		Level level = be.getLevel();
		if (level == null) { // god-damn it, WHY!?!?!
			String threadName = Thread.currentThread().getName().toLowerCase(Locale.ROOT);
			// TODO: Dimensional threading 兼容，但是写成这样太丑了，有更好的方法吗？
			if (!threadName.contains("server")){
				incoming = new CopyOnWriteArrayList<>(incoming);
			}
		} else if (!level.isClientSide) {
			incoming = new CopyOnWriteArrayList<>(incoming);
		}
	}

//	@Inject(method = "tick()V", at = @At(value = "HEAD"))
//	private void onTick(CallbackInfo ci) {
//		List<TransportedItemStack> incoming = this.incoming;
//		if (incoming instanceof ArrayList) {
//			this.incoming = new CopyOnWriteArrayList<>(incoming);
//		}
//	}
}
