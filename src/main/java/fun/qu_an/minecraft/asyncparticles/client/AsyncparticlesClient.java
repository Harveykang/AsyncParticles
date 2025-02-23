package fun.qu_an.minecraft.asyncparticles.client;

import com.mojang.brigadier.arguments.StringArgumentType;
import fun.qu_an.minecraft.asyncparticles.client.config.SimplePropertiesConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

import java.io.IOException;

import static fun.qu_an.minecraft.asyncparticles.client.ModListHelper.*;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.*;

public class AsyncparticlesClient implements ClientModInitializer {
	public static final String MOD_ID = "asyncparticles";

	@Override
	public void onInitializeClient() {
		if (ModListHelper.FABRIC_API_LOADED) {
			ClientCommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
				dispatcher.register(literal(MOD_ID)
						.then(literal("isfabricmod")
							.then(argument("modid", StringArgumentType.word())
								.suggests((context, builder)
									-> SharedSuggestionProvider.suggest(FabricLoader.getInstance().getAllMods().stream().map(modContainer -> modContainer.getMetadata().getId()), builder))
								.executes(context -> {
									String modId = StringArgumentType.getString(context, "modid");
									context.getSource().sendFeedback(Component.literal(modId + " is " + (isFabricModLoaded(modId)? "fabric mod" : isModLoaded(modId) ? "not fabric mod": "not loaded")));
									return 1;
								})))
						.then(literal("isforgemod")
							.then(argument("modid", StringArgumentType.word())
								.suggests((context, builder)
									-> SharedSuggestionProvider.suggest(FabricLoader.getInstance().getAllMods().stream().map(modContainer -> modContainer.getMetadata().getId()), builder))
								.executes(context -> {
									String modId = StringArgumentType.getString(context, "modid");
									context.getSource().sendFeedback(Component.literal(modId + " is " + (isForgeModLoaded(modId)? "forge mod" : isModLoaded(modId) ? "not forge mod": "not loaded")));
									return 1;
								})))
					.then(literal("debug")
						.executes(context -> {
							FabricClientCommandSource source = context.getSource();
							AsyncTicker.debugLater(s -> source.sendFeedback(Component.literal(s)));
							AsyncRenderer.debugLater(s -> source.sendFeedback(Component.literal(s)));
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

		try {
			SimplePropertiesConfig.load();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
