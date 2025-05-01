package fun.qu_an.minecraft.asyncparticles.client.config.forge;

import fun.qu_an.minecraft.asyncparticles.client.compat.ModListHelper;
import fun.qu_an.minecraft.asyncparticles.client.config.AsyncParticlesConfig;
import net.minecraft.client.gui.screens.Screen;

public class Menus {
	public static Screen newConfigScreen(Screen parent) {
		if (ModListHelper.CLOTH_CONFIG_LOADED) {
			return AsyncParticlesConfig.screenBuilder(parent).build();
		} else {
			return AsyncParticlesConfig.fallBackScreen(parent);
		}
	}
}
