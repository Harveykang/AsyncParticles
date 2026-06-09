package fun.qu_an.minecraft.asyncparticles.client.neoforge;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import fun.qu_an.minecraft.asyncparticles.client.AsyncParticlesClient;
import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import fun.qu_an.minecraft.asyncparticles.client.config.AsyncParticlesConfig;
import fun.qu_an.minecraft.asyncparticles.client.config.ConfigHelper;
import fun.qu_an.minecraft.asyncparticles.client.core.particle.tick.AsyncTickBehavior;
import fun.qu_an.minecraft.asyncparticles.client.util.ThreadUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.moddiscovery.ModInfo;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

import java.util.concurrent.CompletableFuture;

import static fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper.*;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

@Mod(value = AsyncParticlesClient.MOD_ID, dist = Dist.CLIENT)
public final class AsyncParticlesClientNeoForge {
	public AsyncParticlesClientNeoForge(ModContainer container) {
		if (!ModListHelper.IS_CLIENT) {
			return;
		}
		// Run our common setup.
		AsyncParticlesClient.init();
		NeoForge.EVENT_BUS.addListener(this::registerClientCommands);
		container.registerExtensionPoint(IConfigScreenFactory.class, (container1, parent)-> AsyncParticlesConfig.newConfigScreen(parent));
	}

	private static CompletableFuture<Suggestions> suggestModId(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
		return SharedSuggestionProvider.suggest(FMLLoader.getCurrent().getLoadingModList().getMods().stream().map(ModInfo::getModId), builder);
	}

	private void registerClientCommands(RegisterClientCommandsEvent event) {
		CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
		dispatcher.register(literal(AsyncParticlesClient.MOD_ID)
			.then(literal("isfabricmod")
				.then(argument("modid", StringArgumentType.word())
					.suggests(AsyncParticlesClientNeoForge::suggestModId)
					.executes(context -> {
						String modId = StringArgumentType.getString(context, "modid");
						context.getSource().sendSystemMessage(Component.literal(modId + " is " + (isFabricModLoaded(modId) ? "fabric mod" : isModLoaded(modId) ? "not fabric mod" : "not loaded")));
						return 1;
					})))
			.then(literal("isforgemod")
				.then(argument("modid", StringArgumentType.word())
					.suggests(AsyncParticlesClientNeoForge::suggestModId)
					.executes(context -> {
						String modId = StringArgumentType.getString(context, "modid");
						context.getSource().sendSystemMessage(Component.literal(modId + " is " + (isForgeModLoaded(modId) ? "forge mod" : isModLoaded(modId) ? "not forge mod" : "not loaded")));
						return 1;
					})))
			.then(literal("debug")
				.executes(context -> {
					CommandSourceStack source = context.getSource();
					AsyncTickBehavior.getInstance().debugLater(s -> source.sendSystemMessage(Component.literal(s)
						.withStyle(Style.EMPTY.withClickEvent(new ClickEvent.CopyToClipboard(s))
							.withHoverEvent(new HoverEvent.ShowText(Component.literal("Copy to clipboard"))))));
//					AsyncRenderBehavior.getInstance().debugLater(s -> source.sendSystemMessage(Component.literal(s)
//						.withStyle(Style.EMPTY.withClickEvent(new ClickEvent.CopyToClipboard(s))
//							.withHoverEvent(new HoverEvent.ShowText(Component.literal("Copy to clipboard"))))));
					return 1;
				}))
			.then(literal("dump")
				.executes(context -> {
					CommandSourceStack source = context.getSource();
//					AsyncTickBehavior.dumpParticles();
//					source.sendSystemMessage(Component.literal("Particles have been dumped to log."));
					return 1;
				}))
			.then(literal("class_exists")
				.then(argument("className", StringArgumentType.string())
					.executes(context -> {
						String className = StringArgumentType.getString(context, "className");
						if (ModListHelper.classExists(className)) {
							context.getSource().sendSystemMessage(Component.literal("Class " + className + " exists!"));
						} else {
							context.getSource().sendSystemMessage(Component.literal("Class " + className + " not exists."));
						}
						return 1;
					})))
			.then(literal("load_class")
				.then(argument("className", StringArgumentType.string())
					.executes(context -> {
						String className = StringArgumentType.getString(context, "className");
						if (ModListHelper.loadClass(className)) {
							context.getSource().sendSystemMessage(Component.literal("Class " + className + " exists!"));
						} else {
							context.getSource().sendSystemMessage(Component.literal("Class " + className + " not exists."));
						}
						return 1;
					})))
			.then(literal("version_check")
				.then(argument("modid", StringArgumentType.word())
					.suggests(AsyncParticlesClientNeoForge::suggestModId)
					.executes(context -> {
						String modId = StringArgumentType.getString(context, "modid");
						if (isModLoaded(modId)) {
							String version = versionToString(modId);
							context.getSource().sendSystemMessage(Component.literal(modId + " version " + version));
						} else {
							context.getSource().sendSystemMessage(Component.literal(modId + " is not loaded"));
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
									context.getSource().sendSystemMessage(Component.literal(modId + " version " + (b ? "is within" : "is not within") + " [" + minInclusive + ", " + maxExclusive + ")"));
								} else {
									context.getSource().sendSystemMessage(Component.literal(modId + " is not loaded"));
								}
								return 1;
							})))))
			.then(literal("config")
				.executes(context -> {
					Minecraft mc = Minecraft.getInstance();
					ThreadUtil.enqueueClientTask(() -> mc.setScreen(AsyncParticlesConfig.newConfigScreen(null)));
					return 1;
				}))
			.then(literal("reload")
				.executes(context -> {
					CommandSourceStack source = context.getSource();
					try {
						ConfigHelper.load();
					} catch (Exception e) {
						source.sendSystemMessage(Component.literal("Failed to reload config"));
						return 1;
					}
					AsyncTickBehavior.getInstance().reloadLater();
					source.sendSystemMessage(Component.literal("AsyncParticles config reloaded"));
					return 1;
				})));
	}
}
