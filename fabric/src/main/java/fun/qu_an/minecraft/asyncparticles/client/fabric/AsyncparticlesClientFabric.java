package fun.qu_an.minecraft.asyncparticles.client.fabric;

import com.mojang.brigadier.arguments.StringArgumentType;
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

import static fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper.*;
import static fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper.isModLoaded;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public final class AsyncparticlesClientFabric implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		AsyncparticlesClient.init();
		if (ModListHelper.FABRIC_API_LOADED) {
			ClientCommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
				dispatcher.register(literal(AsyncparticlesClient.MOD_ID)
					.then(literal("isfabricmod")
						.then(argument("modid", StringArgumentType.word())
							.suggests((context, builder)
								-> SharedSuggestionProvider.suggest(FabricLoader.getInstance().getAllMods().stream().map(modContainer -> modContainer.getMetadata().getId()), builder))
							.executes(context -> {
								String modId = StringArgumentType.getString(context, "modid");
								context.getSource().sendFeedback(Component.literal(modId + " is " + (isFabricModLoaded(modId) ? "fabric mod" : isModLoaded(modId) ? "not fabric mod" : "not loaded")));
								return 1;
							})))
					.then(literal("isforgemod")
						.then(argument("modid", StringArgumentType.word())
							.suggests((context, builder)
								-> SharedSuggestionProvider.suggest(FabricLoader.getInstance().getAllMods().stream().map(modContainer -> modContainer.getMetadata().getId()), builder))
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
