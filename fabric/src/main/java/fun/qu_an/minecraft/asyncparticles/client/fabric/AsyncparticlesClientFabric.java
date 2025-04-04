package fun.qu_an.minecraft.asyncparticles.client.fabric;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import fun.qu_an.minecraft.asyncparticles.client.AsyncRenderer;
import fun.qu_an.minecraft.asyncparticles.client.AsyncTicker;
import fun.qu_an.minecraft.asyncparticles.client.AsyncparticlesClient;
import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import fun.qu_an.minecraft.asyncparticles.client.config.SimplePropertiesConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import static fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper.*;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public final class AsyncparticlesClientFabric implements ClientModInitializer {
	private static CompletableFuture<Suggestions> suggestModId(CommandContext<FabricClientCommandSource> context, SuggestionsBuilder builder) {
		return SharedSuggestionProvider.suggest(FabricLoader.getInstance().getAllMods().stream().map(modContainer -> modContainer.getMetadata().getId()), builder);
	}

	@Override
	public void onInitializeClient() {
		if (!ModListHelper.IS_CLIENT) {
			return;
		}
		AsyncparticlesClient.init();
		if (ModListHelper.FABRIC_API_LOADED) {
			ClientCommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
				dispatcher.register(literal(AsyncparticlesClient.MOD_ID)
					.then(literal("isfabricmod")
						.then(argument("modid", StringArgumentType.word())
							.suggests(AsyncparticlesClientFabric::suggestModId)
							.executes(context -> {
								String modId = StringArgumentType.getString(context, "modid");
								context.getSource().sendFeedback(Component.literal(modId + " is " + (isFabricModLoaded(modId) ? "fabric mod" : isModLoaded(modId) ? "not fabric mod" : "not loaded")));
								return 1;
							})))
					.then(literal("isforgemod")
						.then(argument("modid", StringArgumentType.word())
							.suggests(AsyncparticlesClientFabric::suggestModId)
							.executes(context -> {
								String modId = StringArgumentType.getString(context, "modid");
								context.getSource().sendFeedback(Component.literal(modId + " is " + (isForgeModLoaded(modId) ? "forge mod" : isModLoaded(modId) ? "not forge mod" : "not loaded")));
								return 1;
							})))
					.then(literal("debug")
						.executes(context -> {
							FabricClientCommandSource source = context.getSource();
							AsyncTicker.debugLater(s -> source.sendFeedback(Component.literal(s)
								.withStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, s))
									.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Copy to clipboard"))))));
							AsyncRenderer.debugLater(s -> source.sendFeedback(Component.literal(s)
								.withStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, s))
									.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Copy to clipboard"))))));
							return 1;
						}))
					.then(literal("dump")
						.executes(context -> {
							FabricClientCommandSource source = context.getSource();
							AsyncTicker.dumpParticles();
							source.sendFeedback(Component.literal("Particles have been dumped to log."));
							return 1;
						}))
					.then(literal("class_exists")
						.then(argument("className", StringArgumentType.string())
							.executes(context -> {
								String className = StringArgumentType.getString(context, "className");
								Class<?> aClass = ModListHelper.getClass(className);
								if (aClass == null) {
									context.getSource().sendFeedback(Component.literal("Class " + className + " not found."));
								} else {
									context.getSource().sendFeedback(Component.literal("Class " + className + " found!"));
								}
								return 1;
							})))
					.then(literal("version_check")
						.then(argument("modid", StringArgumentType.word())
							.suggests(AsyncparticlesClientFabric::suggestModId)
							.executes(context -> {
								String modId = StringArgumentType.getString(context, "modid");
								if (isModLoaded(modId)) {
									String version = versionToString(modId);
									context.getSource().sendFeedback(Component.literal(modId + " version " + version));
								} else {
									context.getSource().sendFeedback(Component.literal(modId + " is not loaded"));
								}
								return 1;
							})
							.then(argument("min_inclusive", StringArgumentType.string())
								.then(argument("max_exclusive", StringArgumentType.string())
									.executes(context -> {
										String modId = StringArgumentType.getString(context, "modid");
										String minInclusive = StringArgumentType.getString(context, "min_inclusive");
										String maxExclusive = StringArgumentType.getString(context, "max_exclusive");
										if (isModLoaded(modId)) {
											boolean b = versionCheck(modId, minInclusive, maxExclusive);
											context.getSource().sendFeedback(Component.literal(modId + " version " + (b ? "is within" : "is not within") + " [" + minInclusive + ", " + maxExclusive + ")"));
										} else {
											context.getSource().sendFeedback(Component.literal(modId + " is not loaded"));
										}
										return 1;
									})))))
					.then(literal("reload")
						.executes(context -> {
							FabricClientCommandSource source = context.getSource();
							try {
								SimplePropertiesConfig.load();
							} catch (IOException e) {
								source.sendFeedback(Component.literal("Failed to reload config"));
								return 1;
							}
							AsyncTicker.reloadLater();
							source.sendFeedback(Component.literal("AsyncParticles config reloaded"));
							return 1;
						})));
			});
		}
	}
}
