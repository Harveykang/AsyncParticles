package fun.qu_an.minecraft.asyncparticles.client;

import fun.qu_an.minecraft.asyncparticles.client.config.SimplePropertiesConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

import java.io.IOException;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.*;

public class AsyncparticlesClient implements ClientModInitializer {
	public static final String MOD_ID = "asyncparticles";

	@Override
	public void onInitializeClient() {
		if (ModListHelper.FABRIC_API_LOADED) {
			ClientCommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
				dispatcher.register(literal(MOD_ID)
					.then(literal("debug")
						.executes(context -> {
							FabricClientCommandSource source = context.getSource();
							AsyncTicker.debugLater(s -> source.sendFeedback(Component.nullToEmpty(s)));
							return 1;
						}))
					.then(literal("reload")
						.executes(context -> {
							FabricClientCommandSource source = context.getSource();
							try {
								SimplePropertiesConfig.load();
							} catch (IOException e) {
								source.sendFeedback(Component.nullToEmpty("Failed to reload config"));

								return 1;
							}
							AsyncTicker.reload();
							source.sendFeedback(Component.nullToEmpty("AsyncParticles config reloaded"));
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
