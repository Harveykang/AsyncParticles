package fun.qu_an.minecraft.asyncparticles.client.core;

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
	private static boolean illegalEntityStorageAccess = false;
	private static boolean illegalBlockEntityStorageAccess = false;
	private static boolean temporaryMark_gpuOnlyAsyncParticleTick = false;
	private static boolean temporaryMark_disableAnimationTick = false;
	private static boolean temporaryMark_disableAsyncRainTick = false;
	private static boolean temporaryMark_disableAsyncParticleTick = false;

	private static void sendChat(Supplier<MutableComponent> component) {
		Minecraft.getInstance().execute(() -> Minecraft.getInstance().gui.getChat()
			.addMessage(component.get().withStyle(ChatFormatting.DARK_RED)));
	}

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
			LOGGER.error("""
				[AsyncParticles] Block entity storage accessed off the main thread!
				Consider enabling 'safeBlockEntityMap'.""", new IllegalStateException(""));
			sendChat(() -> Component.literal("[AsyncParticles] ").append(
				Component.translatable("chat.asyncparticles.warn.get_block_entity_off_main_thread",
					Component.translatable("config.asyncparticles.mixin.safeBlockEntityMap"))));
			illegalBlockEntityStorageAccess = true;
		}
	}

	public static void errorAsyncParticleTick(Exception e) {
		if (!temporaryMark_gpuOnlyAsyncParticleTick) {
			temporaryMark_gpuOnlyAsyncParticleTick = true;
			LOGGER.error("""
					[AsyncParticles] Unexpected error while ticking particles asynchronously.
					This is likely caused by a race condition between the particle tick and render methods.
					Temporarily enabled 'gpuOnlyAsyncParticleTick' internally. You may also want to turn it on manually,\
					 otherwise this error will recur after restarting the game.
					It is highly recommended to report this error to {} to help the author identify and fix potential issues.""",
				AsyncParticlesClient.ISSUE_URL_STR, e);
			sendChat(() -> Component.literal("[AsyncParticles] ").append(
				Component.translatable("chat.asyncparticles.warn.error_during_async_particle_tick",
					e.toString(),
					Component.translatable("config.asyncparticles.tick.gpuOnlyAsyncParticleTick"),
					Component.literal("GitHub Issue")
						.withStyle(Style.EMPTY.withClickEvent(new ClickEvent.OpenUrl(AsyncParticlesClient.ISSUE_URI)))
						.withStyle(ChatFormatting.UNDERLINE))));
		}
	}


	public static boolean isTemporaryGpuOnlyAsyncParticleTick() {
		return temporaryMark_gpuOnlyAsyncParticleTick;
	}

	public static void errorAsyncGpuParticleTick(Exception e) {
		if (!temporaryMark_disableAsyncParticleTick) {
			temporaryMark_disableAsyncParticleTick = true;
			LOGGER.error("""
					[AsyncParticles] Unexpected error while ticking GPU particles asynchronously.
					The cause is currently unknown.
					Temporarily disabled 'Async Particle Tick' internally. You may also want to turn it off manually,\
					 otherwise this error will recur after restarting the game.
					It is highly recommended to report this error to {} to help the author identify and fix potential issues.""",
				AsyncParticlesClient.ISSUE_URL_STR, e);
			sendChat(() -> Component.literal("[AsyncParticles] ").append(
				Component.translatable("chat.asyncparticles.warn.error_during_async_gpu_particle_tick",
					e.toString(),
					Component.translatable("config.asyncparticles.tick.particleAsyncMode"),
					Component.literal("GitHub Issue")
						.withStyle(Style.EMPTY.withClickEvent(new ClickEvent.OpenUrl(AsyncParticlesClient.ISSUE_URI)))
						.withStyle(ChatFormatting.UNDERLINE))));
		}
	}

	public static boolean isTemporaryDisableAsyncParticleTick() {
		return temporaryMark_disableAsyncParticleTick;
	}

	public static void errorAsyncAnimateTick(Exception e) {
		if (!temporaryMark_disableAnimationTick) {
			temporaryMark_disableAnimationTick = true;
			LOGGER.error("""
				[AsyncParticles] Error during animateTick.
				This is likely caused by an incompatible injection into the animateTick method.
					Temporarily disabled 'Async Animation Tick' internally. You may also want to turn it on manually,\
					 otherwise this error will recur after restarting the game.""", e);
			sendChat(() -> Component.literal("[AsyncParticles] ").append(
				Component.translatable("chat.asyncparticles.warn.error_during_async_incompatible_injection",
					e.toString(),
					Component.translatable("config.asyncparticles.tick.animationTickMode"),
					"animateTick")
			));
		}
	}

	public static boolean isTemporaryDisableAnimationTick() {
		return temporaryMark_disableAnimationTick;
	}

	public static void errorAsyncRainTick(Exception e) {
		if (!temporaryMark_disableAsyncRainTick) {
			temporaryMark_disableAsyncRainTick = true;
			LOGGER.error("""
				[AsyncParticles] Error during rain tick.
				This is likely caused by an incompatible injection into the rain tick method.
					Temporarily disabled 'Async Weather Tick' internally. You may also want to turn it off manually,\
					 otherwise this error will recur after restarting the game.""", e);
			sendChat(() -> Component.literal("[AsyncParticles] ").append(
				Component.translatable("chat.asyncparticles.warn.error_during_async_incompatible_injection",
					e.toString(),
					Component.translatable("config.asyncparticles.tick.tickWeatherAsync"),
					"tickRain")
			));
		}
	}

	public static boolean isTemporaryDisableAsyncRainTick() {
		return temporaryMark_disableAsyncRainTick;
	}
}
