package fun.qu_an.minecraft.asyncparticles.client.forge;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import fun.qu_an.minecraft.asyncparticles.client.AsyncRenderer;
import fun.qu_an.minecraft.asyncparticles.client.AsyncTicker;
import fun.qu_an.minecraft.asyncparticles.client.AsyncparticlesClient;
import fun.qu_an.minecraft.asyncparticles.client.config.SimplePropertiesConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;

import java.io.IOException;

import static fun.qu_an.minecraft.asyncparticles.client.ModListHelper.*;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

@Mod(AsyncparticlesClient.MOD_ID)
public final class AsyncparticlesClientForge {
	public AsyncparticlesClientForge() {
		// Run our common setup.
		AsyncparticlesClient.init();
		MinecraftForge.EVENT_BUS.addListener(this::registerClientCommands);
	}

	private void registerClientCommands(RegisterClientCommandsEvent event) {
		CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
		dispatcher.register(literal(AsyncparticlesClient.MOD_ID)
			.then(literal("isfabricmod")
				.then(argument("modid", StringArgumentType.word())
					.suggests((context, builder)
						-> SharedSuggestionProvider.suggest(FMLLoader.getLoadingModList().getMods().stream().map(ModInfo::getModId), builder))
					.executes(context -> {
						String modId = StringArgumentType.getString(context, "modid");
						context.getSource().sendSystemMessage(Component.literal(modId + " is " + (isFabricModLoaded(modId) ? "fabric mod" : isModLoaded(modId) ? "not fabric mod" : "not loaded")));
						return 1;
					})))
			.then(literal("isforgemod")
				.then(argument("modid", StringArgumentType.word())
					.suggests((context, builder)
						-> SharedSuggestionProvider.suggest(FMLLoader.getLoadingModList().getMods().stream().map(ModInfo::getModId), builder))
					.executes(context -> {
						String modId = StringArgumentType.getString(context, "modid");
						context.getSource().sendSystemMessage(Component.literal(modId + " is " + (isForgeModLoaded(modId) ? "forge mod" : isModLoaded(modId) ? "not forge mod" : "not loaded")));
						return 1;
					})))
			.then(literal("debug")
				.executes(context -> {
					CommandSourceStack source = context.getSource();
					AsyncTicker.debugLater(s -> source.sendSystemMessage(Component.literal(s)
						.withStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, s))
							.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Copy to clipboard"))))));
					AsyncRenderer.debugLater(s -> source.sendSystemMessage(Component.literal(s)
						.withStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, s))
							.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Copy to clipboard"))))));
					return 1;
				}))
			.then(literal("reload")
				.executes(context -> {
					CommandSourceStack source = context.getSource();
					try {
						SimplePropertiesConfig.load();
					} catch (IOException e) {
						source.sendSystemMessage(Component.literal("Failed to reload config"));
						return 1;
					}
					AsyncTicker.reloadLater();
					source.sendSystemMessage(Component.literal("AsyncParticles config reloaded"));
					return 1;
				})));
	}
}
