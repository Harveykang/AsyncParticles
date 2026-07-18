package fun.qu_an.minecraft.asyncparticles.client.compat;

import com.mojang.logging.LogUtils;
import fun.qu_an.minecraft.asyncparticles.client.AsyncParticlesClient;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Unique;

import java.util.function.Supplier;

public class Diagnostic {
	private static final Logger LOGGER = LogManager.getLogger();
	private static volatile boolean illegalEntityStorageAccess = false;
	private static volatile boolean illegalBlockEntityStorageAccess = false;
	private static boolean temporaryMark_gpuOnlyAsyncParticleTick = false;
	private static boolean temporaryMark_disableAnimationTick = false;

	public static void illegalEntityStorageAccess() {
		if (!illegalEntityStorageAccess) {
			LOGGER.error("""
				[AsyncParticles] Entity storage accessed off the main thread!
				Consider enabling 'safeClassInstanceMultiMap'.""", new IllegalStateException(""));
			sendChat(() -> Component.literal("[AsyncParticles] ").append(
				Component.translatable("chat.asyncparticles.warn.get_entities_off_main_thread",
					Component.translatable("config.asyncparticles.mixin.safeClassInstanceMultiMap"))));
			illegalEntityStorageAccess = true;
		}
	}

	@Unique
	public static void illegalBlockEntityStorageAccess() {
		if (ThreadUtil.isOnParticleThread() && !illegalBlockEntityStorageAccess) {
			LogUtils.getLogger().warn("[AsyncParticles] Block entity storage accessed off the main thread!\nConsider enabling 'safeBlockEntityMap'.", new IllegalStateException(""));
			sendChat(() -> Component.literal("[AsyncParticles] ").append(
				Component.translatable("chat.asyncparticles.warn.get_block_entity_off_main_thread",
					Component.translatable("config.asyncparticles.mixin.safeBlockEntityMap"))));
			illegalBlockEntityStorageAccess = true;
		}
	}

	public static void unexpectedRenderErrorOnMainThread(Exception e) {
		temporaryMark_gpuOnlyAsyncParticleTick = true;
		LOGGER.error("""
				[AsyncParticles] Unexpected error while rendering particles on the main thread.
				This is likely caused by a race condition between the particle tick and render methods.
				Temporarily enabled 'gpuOnlyAsyncParticleTick' internally. You may also want to turn it on manually,\
				 otherwise this error will recur after restarting the game.
				It is highly recommended to report this error to {} to help the author identify and fix potential issues.""",
			AsyncParticlesClient.ISSUE_URL, e);
		sendChat(() -> Component.literal("[AsyncParticles] ").append(
			Component.translatable("chat.asyncparticles.warn.temporary_mark_gpuOnlyAsyncParticleTick",
				e.toString(),
				Component.translatable("config.asyncparticles.tick.gpuOnlyAsyncParticleTick"),
				Component.literal("GitHub Issue")
					.withStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, AsyncParticlesClient.ISSUE_URL)))
					.withStyle(ChatFormatting.UNDERLINE))));
	}

	public static boolean isTemporaryGpuOnlyAsyncParticleTick() {
		return temporaryMark_gpuOnlyAsyncParticleTick;
	}

	public static void errorDuringAsyncAnimateTick(Exception e) {
		temporaryMark_disableAnimationTick = true;
		LOGGER.error("""
			[AsyncParticles] Error during animateTick.
			This is likely caused by an incompatible injection into the animateTick method.
				Temporarily disabled 'Async Animation Tick' internally. You may also want to turn it on manually,\
				 otherwise this error will recur after restarting the game.
			You may need to turn off 'Async Animation Tick'""", e);
		sendChat(() -> Component.literal("[AsyncParticles] ").append(
			Component.translatable("chat.asyncparticles.warn.error_during_animate_tick",
				e.toString(),
				Component.translatable("config.asyncparticles.tick.animationTickMode"))
		));
	}

	public static boolean isTemporaryDisableAnimationTick() {
		return temporaryMark_disableAnimationTick;
	}

	private static void sendChat(Supplier<MutableComponent> component) {
		Minecraft.getInstance().execute(() -> Minecraft.getInstance().gui.getChat()
			.addMessage(component.get().withStyle(ChatFormatting.DARK_RED)));
	}
}
