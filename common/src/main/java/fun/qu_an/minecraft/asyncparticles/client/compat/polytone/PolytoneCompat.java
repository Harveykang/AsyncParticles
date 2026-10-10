package fun.qu_an.minecraft.asyncparticles.client.compat.polytone;

import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.tick.LevelBundle;
import fun.qu_an.minecraft.asyncparticles.client.mixin.compat.polytone.AccessorPolytoneAsyncParticleHandler;
import fun.qu_an.minecraft.asyncparticles.client.util.ParticleThreadLocal;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import net.mehvahdjukaar.polytone.content.particle.custom.CustomParticleInstance;
import net.mehvahdjukaar.polytone.content.particle.custom.PolytoneAsyncParticleHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class PolytoneCompat {
	@SuppressWarnings("Convert2MethodRef")
	public static final ParticleThreadLocal<BlockState> SPAWN_STATE = ParticleThreadLocal.withInitial(
		ThreadUtil::isOnMainThread, () -> Blocks.AIR.defaultBlockState());

	public static void awaitTicks() {
		PolytoneAsyncParticleHandler.awaitTicks();
	}

	public static void deferToMain(Runnable action) {
		ClientLevel level = Minecraft.getInstance().level;
		ThreadUtil.enqueueClientTask(() -> {
			if (LevelBundle.isLevelAvailable(level)) {
				action.run();
			}
		});
	}

	public static void onParticleEngineClear() {
		PolytoneAsyncParticleHandler.awaitTicks();
		AccessorPolytoneAsyncParticleHandler.getPENDING().clear();
		SPAWN_STATE.set(Blocks.AIR.defaultBlockState());
	}

	public static boolean shouldSync(Particle particle) {
		if (!PolytoneEarlyCompat.isAvailable()) {
			return particle instanceof CustomParticleInstance;
		}
		return particle instanceof PolytoneParticleAddon polytone
			&& (!ConfigHelper.isPolytoneAsyncTick()
			|| (ConfigHelper.isPolytoneGpuOnlyAsyncTick() && !polytone.asyncparticles$canRenderFast()));
	}
}
