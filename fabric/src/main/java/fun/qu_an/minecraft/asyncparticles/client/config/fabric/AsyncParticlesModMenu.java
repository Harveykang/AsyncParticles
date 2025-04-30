package fun.qu_an.minecraft.asyncparticles.client.config.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import fun.qu_an.minecraft.asyncparticles.client.config.AsyncParticlesConfig;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.network.chat.Component;

public class AsyncParticlesModMenu implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		if (ModListHelper.CLOTH_CONFIG_LOADED){
			return parent -> AsyncParticlesConfig.screenBuilder(parent).build();
		} else {
			return parent -> new DisconnectedScreen(parent,
				Component.translatable("config.asyncparticles.error.menu_unavailable"),
				Component.translatable("config.asyncparticles.error.cloth_config_required"),
				Component.translatable("gui.back"));
		}
	}
}
