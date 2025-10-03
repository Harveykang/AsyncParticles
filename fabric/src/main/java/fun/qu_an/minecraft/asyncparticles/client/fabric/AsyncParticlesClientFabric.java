package fun.qu_an.minecraft.asyncparticles.client.fabric;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import fun.qu_an.minecraft.asyncparticles.client.AsyncParticlesClient;
import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.SharedSuggestionProvider;

import java.util.concurrent.CompletableFuture;

public final class AsyncParticlesClientFabric implements ClientModInitializer {
	private static CompletableFuture<Suggestions> suggestModId(CommandContext<FabricClientCommandSource> context, SuggestionsBuilder builder) {
		return SharedSuggestionProvider.suggest(FabricLoader.getInstance().getAllMods().stream().map(modContainer -> modContainer.getMetadata().getId()), builder);
	}

	@Override
	public void onInitializeClient() {
		if (!ModListHelper.IS_CLIENT) {
			return;
		}
		AsyncParticlesClient.init();
		if (ModListHelper.FABRIC_API_LOADED) {
//			ClientCommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
//				dispatcher.register(literal(AsyncParticlesClient.MOD_ID)
//					.then(literal("isfabricmod")
//						.then(argument("modid", StringArgumentType.word())
//							.suggests(AsyncParticlesClientFabric::suggestModId)
//							.executes(context -> {
//								String modId = StringArgumentType.getString(context, "modid");
//								context.getSource().sendFeedback(Component.literal(modId + " is " + (isFabricModLoaded(modId) ? "fabric mod" : isModLoaded(modId) ? "not fabric mod" : "not loaded")));
//								return 1;
//							})))
//					.then(literal("isforgemod")
//						.then(argument("modid", StringArgumentType.word())
//							.suggests(AsyncParticlesClientFabric::suggestModId)
//							.executes(context -> {
//								String modId = StringArgumentType.getString(context, "modid");
//								context.getSource().sendFeedback(Component.literal(modId + " is " + (isForgeModLoaded(modId) ? "forge mod" : isModLoaded(modId) ? "not forge mod" : "not loaded")));
//								return 1;
//							})))
//					.then(literal("debug")
//						.executes(context -> {
//							FabricClientCommandSource source = context.getSource();
//							AsyncTicker.debugLater(s -> source.sendFeedback(Component.literal(s)
//								.withStyle(Style.EMPTY.withClickEvent(new ClickEvent.CopyToClipboard(s))
//									.withHoverEvent(new HoverEvent.ShowText(Component.literal("Copy to clipboard"))))));
//							AsyncRenderer.debugLater(s -> source.sendFeedback(Component.literal(s)
//								.withStyle(Style.EMPTY.withClickEvent(new ClickEvent.CopyToClipboard(s))
//									.withHoverEvent(new HoverEvent.ShowText(Component.literal("Copy to clipboard"))))));
//							return 1;
//						}))
//					.then(literal("dump")
//						.executes(context -> {
//							FabricClientCommandSource source = context.getSource();
//							AsyncTicker.dumpParticles();
//							source.sendFeedback(Component.literal("Particles have been dumped to log."));
//							return 1;
//						}))
//					.then(literal("class_exists")
//						.then(argument("className", StringArgumentType.string())
//							.executes(context -> {
//								String className = StringArgumentType.getString(context, "className");
//								if (ModListHelper.classExists(className)) {
//									context.getSource().sendFeedback(Component.literal("Class " + className + " exists!"));
//								} else {
//									context.getSource().sendFeedback(Component.literal("Class " + className + " not exists."));
//								}
//								return 1;
//							})))
//					.then(literal("load_class")
//						.then(argument("className", StringArgumentType.string())
//							.executes(context -> {
//								String className = StringArgumentType.getString(context, "className");
//								if (ModListHelper.loadClass(className)) {
//									context.getSource().sendFeedback(Component.literal("Class " + className + " exists!"));
//								} else {
//									context.getSource().sendFeedback(Component.literal("Class " + className + " not exists."));
//								}
//								return 1;
//							})))
//					.then(literal("version_check")
//						.then(argument("modid", StringArgumentType.word())
//							.suggests(AsyncParticlesClientFabric::suggestModId)
//							.executes(context -> {
//								String modId = StringArgumentType.getString(context, "modid");
//								if (isModLoaded(modId)) {
//									String version = versionToString(modId);
//									context.getSource().sendFeedback(Component.literal(modId + " version " + version));
//								} else {
//									context.getSource().sendFeedback(Component.literal(modId + " is not loaded"));
//								}
//								return 1;
//							})
//							.then(argument("min_inclusive", StringArgumentType.string())
//								.then(argument("max_exclusive", StringArgumentType.string())
//									.executes(context -> {
//										String modId = StringArgumentType.getString(context, "modid");
//										String minInclusive = StringArgumentType.getString(context, "min_inclusive");
//										String maxExclusive = StringArgumentType.getString(context, "max_exclusive");
//										if (isModLoaded(modId)) {
//											boolean b = versionCheck(modId, minInclusive, maxExclusive);
//											context.getSource().sendFeedback(Component.literal(modId + " version " + (b ? "is within" : "is not within") + " [" + minInclusive + ", " + maxExclusive + ")"));
//										} else {
//											context.getSource().sendFeedback(Component.literal(modId + " is not loaded"));
//										}
//										return 1;
//									})))))
//					.then(literal("config")
//						.executes(context -> {
//							Minecraft mc = Minecraft.getInstance();
//							ThreadUtil.enqueueClientTask(() -> mc.setScreen(AsyncParticlesConfig.newConfigScreen(null)));
//							return 1;
//						}))
//					.then(literal("reload")
//						.executes(context -> {
//							FabricClientCommandSource source = context.getSource();
//							try {
//								ConfigHelper.load();
//							} catch (Exception e) {
//								source.sendFeedback(Component.literal("Failed to reload config"));
//								return 1;
//							}
//							AsyncTicker.reloadLater();
//							source.sendFeedback(Component.literal("AsyncParticles config reloaded"));
//							return 1;
//						})));
//			});
		}
	}
}
