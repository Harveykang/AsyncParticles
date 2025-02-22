package fun.qu_an.minecraft.asyncparticles.client.mixin;

import fun.qu_an.minecraft.asyncparticles.client.AsyncRenderer;
import fun.qu_an.minecraft.asyncparticles.client.AsyncTicker;
import net.minecraft.client.Minecraft;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraft {
	@Shadow
	private ProfilerFiller profiler;

	@Inject(method = "runTick", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/util/profiling/ProfilerFiller;push(Ljava/lang/String;)V"))
	private void onRunAllTasks(boolean bl, CallbackInfo ci) {
		profiler.push("tick");
		AsyncTicker.onRunAllTasks();
		profiler.pop();
	}

	@Inject(method = "setLevel", at = @At(value = "FIELD", ordinal = 0,
		target = "Lnet/minecraft/client/Minecraft;level:Lnet/minecraft/client/multiplayer/ClientLevel;"))
	private void onSetLevel(CallbackInfo ci) {
		// TODO: 这玩意到底有没有用？？
		AsyncTicker.destroy();
		AsyncRenderer.destroy();
	}
}
